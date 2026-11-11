import assert from "node:assert/strict";
import crypto from "node:crypto";
import test from "node:test";
import express from "express";
import nodemailer from "nodemailer";
import AuthChallenge from "../models/AuthChallenge.js";
import User from "../models/User.js";
import authRoutes from "./auth.js";

test("first Google sign-in verifies email before creating an account", async (t) => {
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
      user = users.find((item) => item.email === query.email) || null;
    }
    const result = Promise.resolve(user);
    result.select = () => result;
    return result;
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
      return new Response(JSON.stringify({
        aud: "test-google-client",
        iss: "https://accounts.google.com",
        email_verified: "true",
        sub: identity.subject,
        email,
        name: "New Google User",
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

  const url = `http://127.0.0.1:${server.address().port}/api/auth/oauth/google`;
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
  assert.equal(users.length, 1);
  assert.equal(users[0].email, email);

  response = await post("", { credential: "valid-google-id-token" });
  assert.equal(response.status, 200);
  const returning = await response.json();
  assert.equal(returning.verificationRequired, undefined);
  assert.ok(returning.token);
  assert.equal(sentEmails.length, 1);
});
