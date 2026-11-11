import assert from "node:assert/strict";
import crypto from "node:crypto";
import test from "node:test";
import express from "express";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import nodemailer from "nodemailer";
import AuthChallenge from "../models/AuthChallenge.js";
import User from "../models/User.js";
import authRoutes from "./auth.js";

test("Google sign-in verifies email, creates new accounts, and links existing accounts", async (t) => {
  const originalEnvironment = {
    JWT_SECRET: process.env.JWT_SECRET,
    AUTH_CHALLENGE_SECRET: process.env.AUTH_CHALLENGE_SECRET,
    GOOGLE_ALLOWED_CLIENT_IDS: process.env.GOOGLE_ALLOWED_CLIENT_IDS,
    SMTP_EMAIL: process.env.SMTP_EMAIL,
    SMTP_APP_PASSWORD: process.env.SMTP_APP_PASSWORD,
  };
  const originalFetch = globalThis.fetch;
  const originalUserMethods = {
    findOne: User.findOne,
    findOneAndUpdate: User.findOneAndUpdate,
    create: User.create,
    exists: User.exists,
  };
  const originalChallengeMethods = {
    findOne: AuthChallenge.findOne,
    findOneAndUpdate: AuthChallenge.findOneAndUpdate,
    deleteOne: AuthChallenge.deleteOne,
  };
  const originalCreateTransport = nodemailer.createTransport;

  process.env.JWT_SECRET = "test-jwt-secret";
  process.env.AUTH_CHALLENGE_SECRET = "test-challenge-secret";
  process.env.GOOGLE_ALLOWED_CLIENT_IDS = "test-google-client";
  process.env.SMTP_EMAIL = "verified@example.test";
  process.env.SMTP_APP_PASSWORD = " test-app-password ";

  const users = [];
  const challenges = new Map();
  const sentEmails = [];
  let challengeId = 0;
  let userId = 0;
  const identity = { provider: "google", subject: "google-subject-1" };
  const email = "new-google-user@example.test";
  const codeKey = (purpose, identifierHash) => `${purpose}:${identifierHash}`;

  User.findOne = (query) => {
    let user = null;
    if (query.federatedIdentities) {
      const expected = query.federatedIdentities.$elemMatch;
      user = users.find((item) =>
        item.federatedIdentities.some(
          (entry) => entry.provider === expected.provider && entry.subject === expected.subject,
        ),
      ) || null;
    } else if (query.email) {
      user = users.find((item) => item.email.toLowerCase() === query.email.toLowerCase()) || null;
    } else if (query.$or) {
      user = users.find((item) => query.$or.some((condition) =>
        Object.entries(condition).some(([field, value]) =>
          String(item[field] || "").toLowerCase() === String(value).toLowerCase(),
        ),
      )) || null;
    }
    const result = Promise.resolve(user);
    result.select = () => result;
    result.collation = () => result;
    return result;
  };
  User.findOneAndUpdate = async ({ _id }, update) => {
    const user = users.find((item) => item._id === _id) || null;
    if (user && update.$addToSet?.federatedIdentities) {
      user.federatedIdentities.push(update.$addToSet.federatedIdentities);
    }
    return user;
  };
  User.exists = async () => false;
  User.create = async (data) => {
    const user = { ...data, _id: `test-user-${++userId}` };
    users.push(user);
    return user;
  };
  AuthChallenge.findOne = async (query) => {
    const challenge = challenges.get(codeKey(query.purpose, query.identifierHash)) || null;
    if (challenge && query.expiresAt?.$gt && challenge.expiresAt <= query.expiresAt.$gt) {
      return null;
    }
    return challenge;
  };
  AuthChallenge.findOneAndUpdate = async (query, update) => {
    const key = codeKey(query.purpose, query.identifierHash);
    const challenge = {
      ...update.$set,
      _id: challenges.get(key)?._id || `test-challenge-${++challengeId}`,
      save: async () => {},
    };
    challenges.set(key, challenge);
    return challenge;
  };
  AuthChallenge.deleteOne = async ({ _id }) => {
    for (const [key, challenge] of challenges) {
      if (challenge._id === _id) challenges.delete(key);
    }
    return { deletedCount: 1 };
  };
  nodemailer.createTransport = (options) => {
    assert.equal(options.host, "smtp.gmail.com");
    assert.equal(options.port, 465);
    assert.equal(options.secure, true);
    assert.equal(options.auth.user, process.env.SMTP_EMAIL);
    assert.equal(options.auth.pass, "test-app-password");
    return {
      sendMail: async (message) => {
        sentEmails.push(message);
        return { messageId: "test-message" };
      },
    };
  };
  globalThis.fetch = async (input, options) => {
    if (String(input).startsWith("https://oauth2.googleapis.com/tokeninfo")) {
      const token = new URL(String(input)).searchParams.get("id_token");
      const existingAccount = token === "existing-google-id-token";
      const expiredToken = token === "expired-google-id-token";
      return new Response(JSON.stringify({
        aud: "test-google-client",
        iss: "https://accounts.google.com",
        exp: String(Math.floor(Date.now() / 1000) + (expiredToken ? -3600 : 3600)),
        email_verified: "true",
        sub: existingAccount ? "existing-google-subject" : identity.subject,
        email: existingAccount ? "existing-account@example.test" : email,
        name: existingAccount ? "Google Profile Name" : "New Google User",
        picture: "https://example.test/avatar.png",
      }), { status: 200 });
    }
    return originalFetch(input, options);
  };

  const app = express();
  app.use(express.json());
  app.use("/api/auth", authRoutes);
  const server = app.listen(0, "127.0.0.1");
  await new Promise((resolve) => server.once("listening", resolve));
  t.after(async () => {
    await new Promise((resolve) => server.close(resolve));
    globalThis.fetch = originalFetch;
    Object.assign(User, originalUserMethods);
    Object.assign(AuthChallenge, originalChallengeMethods);
    nodemailer.createTransport = originalCreateTransport;
    for (const [key, value] of Object.entries(originalEnvironment)) {
      if (value === undefined) delete process.env[key];
      else process.env[key] = value;
    }
  });

  const authUrl = `http://127.0.0.1:${server.address().port}/api/auth`;
  const url = `${authUrl}/oauth/google`;
  const post = (path, body) => originalFetch(`${url}${path}`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body),
  });

  let response = await post("", { credential: "valid-google-id-token" });
  assert.equal(response.status, 200);
  const pending = await response.json();
  assert.equal(pending.verificationRequired, true);
  assert.equal(pending.email, email);
  assert.equal(users.length, 0);
  assert.equal(sentEmails.length, 1);
  assert.equal(sentEmails[0].to, email);

  const code = sentEmails[0].text.match(/confirmation code is (\d{6})/i)?.[1];
  assert.ok(code);
  const identifierHash = crypto
    .createHmac("sha256", process.env.AUTH_CHALLENGE_SECRET)
    .update(`google-signup:${email}`)
    .digest("hex");
  const challenge = challenges.get(`google-signup:${identifierHash}`);
  assert.ok(challenge);
  assert.notEqual(challenge.codeHash, code);

  const incorrectCode = code === "000000" ? "000001" : "000000";
  response = await post("/verify", { email, code: incorrectCode });
  assert.equal(response.status, 400);

  response = await post("/verify", { email, code });
  assert.equal(response.status, 200);
  const confirmed = await response.json();
  assert.equal(confirmed.success, true);
  assert.ok(confirmed.token);
  assert.equal(jwt.verify(confirmed.token, process.env.JWT_SECRET).userId, users[0]._id);
  assert.equal(users.length, 1);
  assert.equal(users[0].email, email);

  response = await post("", { credential: "valid-google-id-token" });
  assert.equal(response.status, 200);
  const returning = await response.json();
  assert.equal(returning.verificationRequired, undefined);
  assert.ok(returning.token);
  assert.equal(sentEmails.length, 1);

  const existingUser = {
    _id: "existing-user",
    email: "Existing-Account@Example.Test",
    name: "Existing Topline Profile",
    username: "existing-profile",
    password: await bcrypt.hash("correct-password", 4),
    bio: "Keep my existing profile",
    profilePicture: "https://example.test/existing-profile.png",
    federatedIdentities: [],
  };
  users.push(existingUser);
  response = await post("", { credential: "existing-google-id-token" });
  assert.equal(response.status, 200);
  const existingPending = await response.json();
  assert.equal(existingPending.verificationRequired, true);
  assert.equal(existingPending.email, "existing-account@example.test");
  assert.equal(users.length, 2);
  assert.equal(sentEmails.length, 2);

  const existingCode = sentEmails[1].text.match(/confirmation code is (\d{6})/i)?.[1];
  assert.ok(existingCode);
  response = await post("/verify", {
    email: existingPending.email,
    code: existingCode,
  });
  assert.equal(response.status, 200);
  const linked = await response.json();
  assert.equal(linked.user.id, existingUser._id);
  assert.ok(linked.token);
  assert.equal(jwt.verify(linked.token, process.env.JWT_SECRET).userId, existingUser._id);
  assert.equal(users.length, 2);
  assert.equal(existingUser.name, "Existing Topline Profile");
  assert.equal(existingUser.username, "existing-profile");
  assert.equal(existingUser.bio, "Keep my existing profile");
  assert.equal(existingUser.profilePicture, "https://example.test/existing-profile.png");
  assert.deepEqual(existingUser.federatedIdentities, [{
    provider: "google",
    subject: "existing-google-subject",
  }]);

  response = await post("", { credential: "existing-google-id-token" });
  assert.equal(response.status, 200);
  const linkedReturning = await response.json();
  assert.equal(linkedReturning.user.id, existingUser._id);
  assert.equal(linkedReturning.verificationRequired, undefined);
  assert.equal(sentEmails.length, 2);

  response = await originalFetch(`${authUrl}/login`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
    identifier: existingUser.email.toUpperCase(),
    password: "correct-password",
    }),
  });
  assert.equal(response.status, 200);
  const passwordLogin = await response.json();
  assert.equal(passwordLogin.user.id, existingUser._id);
  assert.equal(jwt.verify(passwordLogin.token, process.env.JWT_SECRET).userId, existingUser._id);

  response = await post("", { credential: "expired-google-id-token" });
  assert.equal(response.status, 503);
  assert.equal((await response.json()).message, "Google sign-in token is invalid or not verified.");
});
