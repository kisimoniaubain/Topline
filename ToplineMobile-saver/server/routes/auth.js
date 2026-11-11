import crypto from "node:crypto";
import express from "express";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import nodemailer from "nodemailer";
import AuthChallenge from "../models/AuthChallenge.js";
import QrAuthSession from "../models/QrAuthSession.js";
import User from "../models/User.js";

const router = express.Router();
const QR_SESSION_LIFETIME_MS = 2 * 60 * 1000;
const EMAIL_CODE_LIFETIME_MS = 10 * 60 * 1000;
const EMAIL_CODE_RESEND_DELAY_MS = 60 * 1000;
const MAX_EMAIL_CODE_ATTEMPTS = 5;

const normalizeEmail = (email) => String(email || "").trim().toLowerCase();
const emailDeliveryConfigured = () =>
  Boolean(process.env.SMTP_EMAIL && process.env.SMTP_APP_PASSWORD);

const getChallengeSecret = () => {
  const secret = process.env.AUTH_CHALLENGE_SECRET || process.env.JWT_SECRET;
  if (!secret) {
    throw new Error("AUTH_CHALLENGE_SECRET or JWT_SECRET must be configured.");
  }
  return secret;
};

const hashValue = (value) =>
  crypto.createHmac("sha256", getChallengeSecret()).update(value).digest("hex");

const safeEqual = (left, right) => {
  const leftBuffer = Buffer.from(left, "hex");
  const rightBuffer = Buffer.from(right, "hex");
  return leftBuffer.length === rightBuffer.length &&
    crypto.timingSafeEqual(leftBuffer, rightBuffer);
};

const publicUser = (user) => ({
  id: String(user._id),
  name: user.name,
  username: user.username,
  email: user.email,
  phone: user.phone,
  dateOfBirth: user.dateOfBirth,
  gender: user.gender,
  profilePicture: user.profilePicture,
  bio: user.bio,
  location: user.location,
  privateAccount: user.privateAccount,
});

const createSession = (user) => {
  if (!process.env.JWT_SECRET) {
    throw new Error("JWT_SECRET is not configured.");
  }
  return {
    success: true,
    token: jwt.sign({ userId: user._id }, process.env.JWT_SECRET, { expiresIn: "7d" }),
    user: publicUser(user),
  };
};

const getAuthenticatedUserId = (req) => {
  const token = req.headers.authorization?.split(" ")[1];
  if (!token || !process.env.JWT_SECRET) return null;
  try {
    return jwt.verify(token, process.env.JWT_SECRET).userId;
  } catch {
    return null;
  }
};

const sendEmailCode = async ({ email, purpose, subject, text, providerProfile }) => {
  const { SMTP_EMAIL, SMTP_APP_PASSWORD } = process.env;
  if (!SMTP_EMAIL || !SMTP_APP_PASSWORD) {
    throw new Error("Email delivery is not configured.");
  }

  const now = new Date();
  const identifierHash = hashValue(`${purpose}:${email}`);
  const existing = await AuthChallenge.findOne({ purpose, identifierHash });
  if (existing && now - existing.sentAt < EMAIL_CODE_RESEND_DELAY_MS) {
    return {
      sent: false,
      retryAfterSeconds: Math.ceil((EMAIL_CODE_RESEND_DELAY_MS - (now - existing.sentAt)) / 1000),
    };
  }

  const code = String(crypto.randomInt(0, 1_000_000)).padStart(6, "0");
  const challenge = await AuthChallenge.findOneAndUpdate(
    { purpose, identifierHash },
    {
      $set: {
        codeHash: hashValue(`${purpose}:${email}:${code}`),
        expiresAt: new Date(now.getTime() + EMAIL_CODE_LIFETIME_MS),
        sentAt: now,
        attempts: 0,
        providerSubject: providerProfile?.subject || "",
        displayName: providerProfile?.name || "",
        profilePicture: providerProfile?.profilePicture || "",
      },
    },
    { upsert: true, new: true, setDefaultsOnInsert: true },
  );

  try {
    const transporter = nodemailer.createTransport({
      host: "smtp.gmail.com",
      port: 465,
      secure: true,
      auth: {
        user: SMTP_EMAIL,
        pass: SMTP_APP_PASSWORD.replace(/\s/g, ""),
      },
    });
    await transporter.sendMail({
      to: email,
      from: SMTP_EMAIL,
      subject,
      text: text(code),
    });
  } catch (error) {
    await AuthChallenge.deleteOne({ _id: challenge._id });
    throw error;
  }
  return { sent: true, retryAfterSeconds: EMAIL_CODE_RESEND_DELAY_MS / 1000 };
};

const sendPasswordResetCode = async (email) => sendEmailCode({
  email,
  purpose: "password-reset",
  subject: "Reset your Topline password",
  text: (code) => `Your Topline password reset code is ${code}. It expires in 10 minutes.`,
});

const sendGoogleSignupCode = async (profile) => sendEmailCode({
  email: profile.email,
  purpose: "google-signup",
  subject: "Confirm your Topline account",
  text: (code) => `Your Topline confirmation code is ${code}. It expires in 10 minutes. If you did not start this sign-up, you can ignore this email.`,
  providerProfile: {
    subject: profile.identity.subject,
    name: profile.name,
    profilePicture: profile.profilePicture,
  },
});

const verifyGoogleIdToken = async (idToken) => {
  const allowedClientIds = (process.env.GOOGLE_ALLOWED_CLIENT_IDS || "")
    .split(",")
    .map((id) => id.trim())
    .filter(Boolean);
  if (!allowedClientIds.length) {
    return { error: "Google sign-in is not configured on the server." };
  }

  const response = await fetch(
    `https://oauth2.googleapis.com/tokeninfo?id_token=${encodeURIComponent(idToken)}`,
  );
  if (!response.ok) return { error: "Google could not verify this sign-in." };
  const claims = await response.json();
  if (
    !allowedClientIds.includes(claims.aud) ||
    !["accounts.google.com", "https://accounts.google.com"].includes(claims.iss) ||
    claims.email_verified !== "true" ||
    !claims.sub ||
    !claims.email
  ) {
    return { error: "Google sign-in token is invalid or not verified." };
  }
  return {
    identity: { provider: "google", subject: claims.sub },
    email: normalizeEmail(claims.email),
    name: claims.name || claims.email.split("@")[0],
    profilePicture: claims.picture || "",
  };
};

const verifyFacebookAccessToken = async (accessToken) => {
  const { FACEBOOK_APP_ID, FACEBOOK_APP_SECRET } = process.env;
  if (!FACEBOOK_APP_ID || !FACEBOOK_APP_SECRET) {
    return { error: "Facebook sign-in is not configured on the server." };
  }
  const graphVersion = process.env.FACEBOOK_GRAPH_VERSION || "v23.0";
  const appToken = `${FACEBOOK_APP_ID}|${FACEBOOK_APP_SECRET}`;
  const debugUrl = new URL(`https://graph.facebook.com/${graphVersion}/debug_token`);
  debugUrl.searchParams.set("input_token", accessToken);
  debugUrl.searchParams.set("access_token", appToken);
  const debugResponse = await fetch(debugUrl);
  if (!debugResponse.ok) return { error: "Facebook could not verify this sign-in." };
  const debugData = (await debugResponse.json()).data;
  if (!debugData?.is_valid || debugData.app_id !== FACEBOOK_APP_ID) {
    return { error: "Facebook sign-in token is invalid." };
  }

  const profileUrl = new URL(`https://graph.facebook.com/${graphVersion}/me`);
  profileUrl.searchParams.set("fields", "id,name,email,picture.type(large)");
  profileUrl.searchParams.set("access_token", accessToken);
  const profileResponse = await fetch(profileUrl);
  if (!profileResponse.ok) return { error: "Unable to load the Facebook profile." };
  const profile = await profileResponse.json();
  if (!profile.id || !profile.email) {
    return { error: "A verified email address is required for Facebook sign-in." };
  }
  return {
    identity: { provider: "facebook", subject: profile.id },
    email: normalizeEmail(profile.email),
    name: profile.name || profile.email.split("@")[0],
    profilePicture: profile.picture?.data?.url || "",
  };
};

const makeUsername = async (name, email) => {
  const base = (name || email.split("@")[0])
    .normalize("NFKD")
    .replace(/[^\w.-]/g, "")
    .toLowerCase()
    .slice(0, 18) || "toplineuser";
  for (let attempt = 0; attempt < 8; attempt += 1) {
    const suffix = crypto.randomBytes(3).toString("hex");
    const username = `${base.slice(0, 24 - suffix.length - 1)}_${suffix}`;
    if (!(await User.exists({ username }))) return username;
  }
  throw new Error("Could not allocate a unique username.");
};

router.post("/password/reset/request", async (req, res) => {
  const email = normalizeEmail(req.body?.email);
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    return res.status(400).json({ success: false, message: "Enter a valid email address." });
  }
  if (!emailDeliveryConfigured()) {
    return res.status(503).json({ success: false, message: "Password recovery email is not configured on the server." });
  }
  try {
    const user = await User.findOne({ email }).select("_id");
    if (user) await sendPasswordResetCode(email);
    return res.json({
      success: true,
      message: "If an account exists for this email, a reset code has been sent.",
    });
  } catch (error) {
    console.error("Password reset request failed:", error.message);
    return res.status(502).json({ success: false, message: "Unable to send a reset code right now." });
  }
});

router.post("/password/reset/confirm", async (req, res) => {
  const email = normalizeEmail(req.body?.email);
  const code = String(req.body?.code || "").trim();
  const password = String(req.body?.password || "");
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || !/^\d{6}$/.test(code) || password.length < 8) {
    return res.status(400).json({
      success: false,
      message: "Enter a valid email, six-digit code, and password with at least eight characters.",
    });
  }
  try {
    const identifierHash = hashValue(`password-reset:${email}`);
    const challenge = await AuthChallenge.findOne({
      purpose: "password-reset",
      identifierHash,
      expiresAt: { $gt: new Date() },
    });
    if (!challenge || challenge.attempts >= MAX_EMAIL_CODE_ATTEMPTS) {
      return res.status(400).json({ success: false, message: "The reset code is invalid or expired. Request a new code." });
    }

    const candidateHash = hashValue(`password-reset:${email}:${code}`);
    if (!safeEqual(challenge.codeHash, candidateHash)) {
      challenge.attempts += 1;
      await challenge.save();
      return res.status(400).json({ success: false, message: "The reset code is invalid or expired." });
    }

    const passwordHash = await bcrypt.hash(password, 12);
    const user = await User.findOneAndUpdate(
      { email },
      { $set: { password: passwordHash } },
      { new: true },
    );
    await AuthChallenge.deleteOne({ _id: challenge._id });
    if (!user) {
      return res.status(400).json({ success: false, message: "The reset code is invalid or expired." });
    }
    return res.json({ success: true, message: "Your password has been reset. You can now sign in." });
  } catch (error) {
    console.error("Password reset confirmation failed:", error.message);
    return res.status(500).json({ success: false, message: "Unable to reset the password right now." });
  }
});

router.post("/oauth/google/verify", async (req, res) => {
  const email = normalizeEmail(req.body?.email);
  const code = String(req.body?.code || "").trim();
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || !/^\d{6}$/.test(code)) {
    return res.status(400).json({ success: false, message: "Enter the email address and six-digit code." });
  }

  try {
    const identifierHash = hashValue(`google-signup:${email}`);
    const challenge = await AuthChallenge.findOne({
      purpose: "google-signup",
      identifierHash,
      expiresAt: { $gt: new Date() },
    });
    if (!challenge || challenge.attempts >= MAX_EMAIL_CODE_ATTEMPTS) {
      return res.status(400).json({ success: false, message: "The confirmation code is invalid or expired. Sign in with Google again to request a new code." });
    }

    const candidateHash = hashValue(`google-signup:${email}:${code}`);
    if (!safeEqual(challenge.codeHash, candidateHash)) {
      challenge.attempts += 1;
      await challenge.save();
      return res.status(400).json({ success: false, message: "The confirmation code is invalid or expired." });
    }

    if (!challenge.providerSubject || !challenge.displayName) {
      await AuthChallenge.deleteOne({ _id: challenge._id });
      return res.status(400).json({ success: false, message: "This confirmation request is invalid. Sign in with Google again." });
    }

    const identity = { provider: "google", subject: challenge.providerSubject };
    let user = await User.findOne({
      federatedIdentities: { $elemMatch: identity },
    });
    if (!user) {
      const emailOwner = await User.findOne({ email }).select("_id");
      if (emailOwner) {
        await AuthChallenge.deleteOne({ _id: challenge._id });
        return res.status(409).json({
          success: false,
          message: "An account already uses this email. Sign in with that account first before linking Google.",
        });
      }

      const randomPassword = await bcrypt.hash(crypto.randomBytes(32).toString("hex"), 12);
      user = await User.create({
        name: challenge.displayName,
        username: await makeUsername(challenge.displayName, email),
        email,
        password: randomPassword,
        dateOfBirth: null,
        profilePicture: challenge.profilePicture,
        federatedIdentities: [identity],
      });
    }

    await AuthChallenge.deleteOne({ _id: challenge._id });
    return res.json(createSession(user));
  } catch (error) {
    console.error("Google email confirmation failed:", error.message);
    return res.status(500).json({ success: false, message: "Unable to confirm this Google sign-in right now." });
  }
});

router.post("/oauth/:provider", async (req, res) => {
  const provider = String(req.params.provider || "").toLowerCase();
  const credential = String(req.body?.credential || "");
  if (!["google", "facebook"].includes(provider)) {
    return res.status(404).json({ success: false, message: "This sign-in provider is not supported." });
  }
  if (!credential) {
    return res.status(400).json({ success: false, message: "A provider sign-in token is required." });
  }
  try {
    const profile = provider === "google"
      ? await verifyGoogleIdToken(credential)
      : await verifyFacebookAccessToken(credential);
    if (profile.error) return res.status(503).json({ success: false, message: profile.error });

    let user = await User.findOne({
      federatedIdentities: {
        $elemMatch: { provider, subject: profile.identity.subject },
      },
    });
    if (!user && provider === "google") {
      const emailOwner = await User.findOne({ email: profile.email }).select("_id");
      if (emailOwner) {
        return res.status(409).json({
          success: false,
          message: "An account already uses this email. Sign in with that account first before linking Google.",
        });
      }
      const delivery = await sendGoogleSignupCode(profile);
      return res.json({
        success: true,
        verificationRequired: true,
        email: profile.email,
        retryAfterSeconds: delivery.retryAfterSeconds,
        message: delivery.sent
          ? "A confirmation code was sent to your Google email address."
          : "A confirmation code was sent recently. Check your email or wait before requesting another.",
      });
    }
    if (!user) {
      const emailOwner = await User.findOne({ email: profile.email }).select("_id");
      if (emailOwner) {
        return res.status(409).json({
          success: false,
          message: "An account already uses this email. Sign in with that account first before linking this provider.",
        });
      }
      const randomPassword = await bcrypt.hash(crypto.randomBytes(32).toString("hex"), 12);
      user = await User.create({
        name: profile.name,
        username: await makeUsername(profile.name, profile.email),
        email: profile.email,
        password: randomPassword,
        dateOfBirth: null,
        profilePicture: profile.profilePicture,
        federatedIdentities: [profile.identity],
      });
    }
    return res.json(createSession(user));
  } catch (error) {
    console.error(`${provider} sign-in failed:`, error.message);
    if (error.message === "Email delivery is not configured.") {
      return res.status(503).json({
        success: false,
        message: "Google account confirmation email is not configured on the server.",
      });
    }
    return res.status(502).json({ success: false, message: "Unable to complete provider sign-in right now." });
  }
});

router.post("/qr/start", async (_req, res) => {
  try {
    const token = crypto.randomBytes(32).toString("base64url");
    const tokenHash = hashValue(`qr-login:${token}`);
    const expiresAt = new Date(Date.now() + QR_SESSION_LIFETIME_MS);
    await QrAuthSession.create({ tokenHash, expiresAt });
    return res.json({
      success: true,
      token,
      qrPayload: `topline://auth/qr?token=${encodeURIComponent(token)}`,
      expiresAt,
    });
  } catch (error) {
    console.error("QR login session creation failed:", error.message);
    return res.status(500).json({ success: false, message: "Unable to start QR sign-in." });
  }
});

router.post("/qr/approve", async (req, res) => {
  const userId = getAuthenticatedUserId(req);
  const token = String(req.body?.token || "");
  if (!userId) return res.status(401).json({ success: false, message: "Sign in on this device to approve a login." });
  if (!token) return res.status(400).json({ success: false, message: "QR sign-in token is required." });
  try {
    const session = await QrAuthSession.findOneAndUpdate(
      {
        tokenHash: hashValue(`qr-login:${token}`),
        status: "pending",
        expiresAt: { $gt: new Date() },
      },
      { $set: { status: "approved", approvedBy: userId } },
      { new: true },
    );
    if (!session) return res.status(410).json({ success: false, message: "This QR sign-in has expired or was already handled." });
    return res.json({ success: true, message: "Sign-in approved. Return to the other device." });
  } catch (error) {
    console.error("QR login approval failed:", error.message);
    return res.status(500).json({ success: false, message: "Unable to approve this sign-in." });
  }
});

router.post("/qr/deny", async (req, res) => {
  const userId = getAuthenticatedUserId(req);
  const token = String(req.body?.token || "");
  if (!userId) return res.status(401).json({ success: false, message: "Sign in on this device to deny a login." });
  if (!token) return res.status(400).json({ success: false, message: "QR sign-in token is required." });
  try {
    const session = await QrAuthSession.findOneAndUpdate(
      {
        tokenHash: hashValue(`qr-login:${token}`),
        status: "pending",
        expiresAt: { $gt: new Date() },
      },
      { $set: { status: "denied", approvedBy: userId } },
      { new: true },
    );
    if (!session) return res.status(410).json({ success: false, message: "This QR sign-in has expired or was already handled." });
    return res.json({ success: true, message: "Sign-in request denied." });
  } catch (error) {
    console.error("QR login denial failed:", error.message);
    return res.status(500).json({ success: false, message: "Unable to deny this sign-in." });
  }
});

router.post("/qr/poll", async (req, res) => {
  const token = String(req.body?.token || "");
  if (!token) return res.status(400).json({ success: false, message: "QR sign-in token is required." });
  try {
    const tokenHash = hashValue(`qr-login:${token}`);
    const now = new Date();
    const session = await QrAuthSession.findOne({ tokenHash });
    if (!session || session.expiresAt <= now || session.status === "consumed") {
      return res.status(410).json({ success: false, status: "expired", message: "This QR sign-in has expired." });
    }
    if (session.status === "pending") return res.json({ success: true, status: "pending" });
    if (session.status === "denied") return res.json({ success: true, status: "denied" });

    const claimed = await QrAuthSession.findOneAndUpdate(
      { _id: session._id, status: "approved", expiresAt: { $gt: now } },
      { $set: { status: "consumed" } },
      { new: false },
    );
    if (!claimed?.approvedBy) {
      return res.status(410).json({ success: false, status: "expired", message: "This QR sign-in has already been used." });
    }
    const user = await User.findById(claimed.approvedBy);
    if (!user) return res.status(410).json({ success: false, message: "The approving account is no longer available." });
    return res.json({ ...createSession(user), status: "approved" });
  } catch (error) {
    console.error("QR login polling failed:", error.message);
    return res.status(500).json({ success: false, message: "Unable to check this sign-in." });
  }
});

/* =========================
   REGISTER
========================= */

router.post("/register", async (req, res) => {
  try {
    const { name, username, email, phone, password, dateOfBirth, gender } = req.body;
    if (!name || !username || !email || !password || !dateOfBirth) {
      return res.status(400).json({ success: false, message: "All fields are required." });
    }

    const normalizedUsername = username.trim().toLowerCase();
    const normalizedEmail = normalizeEmail(email);
    const parsedDateOfBirth = new Date(dateOfBirth);
    if (Number.isNaN(parsedDateOfBirth.getTime())) {
      return res.status(400).json({ success: false, message: "Please provide a valid date of birth." });
    }

    const existingUser = await User.findOne({
      $or: [{ email: normalizedEmail }, { username: normalizedUsername }],
    }).select("_id email username");
    if (existingUser) {
      return res.status(409).json({ success: false, message: "That email or username is already in use." });
    }

    const hashedPassword = await bcrypt.hash(password, 12);
    const user = await User.create({
      name: name.trim(),
      username: normalizedUsername,
      email: normalizedEmail,
      phone: typeof phone === "string" ? phone.trim() : "",
      gender: typeof gender === "string" ? gender.trim() : "",
      password: hashedPassword,
      dateOfBirth: parsedDateOfBirth,
    });
    return res.status(201).json({ ...createSession(user), message: "Account created successfully." });
  } catch (error) {
    console.error("Register error:", error.message);
    return res.status(500).json({ success: false, message: "Something went wrong while creating the account." });
  }
});

/* =========================
   PASSWORD LOGIN
========================= */

router.post("/login", async (req, res) => {
  try {
    const { identifier, password } = req.body;
    if (!identifier || !password) {
      return res.status(400).json({ success: false, message: "Email/username and password are required." });
    }

    const loginValue = String(identifier).trim().toLowerCase();
    const user = await User.findOne({
      $or: [{ email: loginValue }, { username: loginValue }],
    });
    const passwordMatches = user?.password
      ? await bcrypt.compare(password, user.password)
      : false;
    if (!passwordMatches) {
      return res.status(401).json({ success: false, message: "Incorrect email/username or password." });
    }
    return res.json({ ...createSession(user), message: "Login successful." });
  } catch (error) {
    console.error("Login error:", error.message);
    return res.status(500).json({ success: false, message: "Something went wrong while logging in." });
  }
});

export default router;
