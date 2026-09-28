import express from "express";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import User from "../models/User.js";

const router = express.Router();

router.post("/send-code", async (req, res) => {
  try {
    const { email, phone } = req.body;
    if (!email && !phone) {
      return res.status(400).json({ success: false, message: "Email or phone required." });
    }
    const code = "123456";
    if (phone) {
      const twilio = (await import("twilio"))(process.env.TWILIO_SID, process.env.TWILIO_AUTH);
      await twilio.messages.create({
        body: `Your Topline confirmation code is: ${code}`,
        messagingServiceSid: process.env.TWILIO_MESSAGING_SERVICE_SID || "MGd797bf205771aad14d2e980697a4b6d5",
        to: phone,
      });
    } else {
      const sgMail = (await import("@sendgrid/mail")).default;
      sgMail.setApiKey(process.env.SENDGRID_API_KEY || "");
      await sgMail.send({
        to: email,
        from: "noreply@topline.com",
        subject: "Your Topline confirmation code",
        text: `Your confirmation code is: ${code}`,
      });
    }
    res.json({ success: true, message: "Confirmation code sent.", code });
  } catch (e) {
    res.status(500).json({ success: false, message: e.message });
  }
});

/* =========================
   REGISTER
========================= */

router.post("/register", async (req, res) => {
  try {
    const {
      name,
      username,
      email,
      phone,
      confirmationCode,
      password,
      dateOfBirth,
    } = req.body;

    if (
      !name ||
      !username ||
      !email ||
      !password ||
      !dateOfBirth
    ) {
      return res.status(400).json({
        success: false,
        message: "All fields are required.",
      });
    }

    const normalizedUsername = username
      .trim()
      .toLowerCase();

    const normalizedEmail = email
      .trim()
      .toLowerCase();

    // Allow duplicate accounts — no uniqueness check
    const existingUser = null;

    // Allow duplicate accounts — skip uniqueness check
    // if (existingUser) { ... }

    const hashedPassword = await bcrypt.hash(
      password,
      12
    );

    const user = await User.create({
      name: name.trim(),
      username: normalizedUsername,
      email: normalizedEmail,
      password: hashedPassword,
      dateOfBirth,
    });

    const token = jwt.sign(
      {
        userId: user._id,
      },
      process.env.JWT_SECRET,
      {
        expiresIn: "7d",
      }
    );

    res.status(201).json({
      success: true,
      message: "Account created successfully.",
      token,
      user: {
        id: user._id,
        name: user.name,
        username: user.username,
        email: user.email,
        dateOfBirth: user.dateOfBirth,
        profilePicture: user.profilePicture,
        bio: user.bio,
      },
    });
  } catch (error) {
    console.error("Register error:", error);

    res.status(500).json({
      success: false,
      message: "Something went wrong while creating the account.",
    });
  }
});

/* =========================
   LOGIN
========================= */

router.post("/login", async (req, res) => {
  try {
    const { identifier, password } = req.body;

    if (!identifier || !password) {
      return res.status(400).json({
        success: false,
        message: "Email/username and password are required.",
      });
    }

    const loginValue = identifier.trim().toLowerCase();

    const user = await User.findOne({
      $or: [
        { email: loginValue },
        { username: loginValue },
        { phone: loginValue },
      ],
    });

    if (!user) {
      return res.status(401).json({
        success: false,
        message: "Incorrect email/username or password.",
      });
    }

    const passwordMatches = await bcrypt.compare(
      password,
      user.password
    );

    if (!passwordMatches) {
      return res.status(401).json({
        success: false,
        message: "Incorrect email/username or password.",
      });
    }

    const token = jwt.sign(
      {
        userId: user._id,
      },
      process.env.JWT_SECRET,
      {
        expiresIn: "7d",
      }
    );

    res.json({
      success: true,
      message: "Login successful.",
      token,
      user: {
        id: user._id,
        name: user.name,
        username: user.username,
        email: user.email,
        dateOfBirth: user.dateOfBirth,
        profilePicture: user.profilePicture,
        bio: user.bio,
      },
    });
  } catch (error) {
    console.error("Login error:", error);

    res.status(500).json({
      success: false,
      message: "Something went wrong while logging in.",
    });
  }
});

export default router;