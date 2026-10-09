import { createSign } from "node:crypto";
import PushDevice from "../models/PushDevice.js";

const TOKEN_URL = "https://oauth2.googleapis.com/token";
let cachedAccessToken = null;
let firebaseSetupWarningLogged = false;

const base64Url = (value) => Buffer.from(value).toString("base64url");

const getServiceAccount = () => {
  const projectId = process.env.FIREBASE_PROJECT_ID;
  const clientEmail = process.env.FIREBASE_CLIENT_EMAIL;
  const privateKey = process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, "\n");

  if (!projectId || !clientEmail || !privateKey) {
    if (!firebaseSetupWarningLogged) {
      console.warn("Push notifications are disabled: Firebase service-account environment variables are not configured.");
      firebaseSetupWarningLogged = true;
    }
    return null;
  }

  return { projectId, clientEmail, privateKey };
};

const getAccessToken = async ({ clientEmail, privateKey }) => {
  if (cachedAccessToken && cachedAccessToken.expiresAt > Date.now() + 60_000) {
    return cachedAccessToken.value;
  }

  const issuedAt = Math.floor(Date.now() / 1000);
  const header = base64Url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claims = base64Url(JSON.stringify({
    iss: clientEmail,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: TOKEN_URL,
    iat: issuedAt,
    exp: issuedAt + 3600,
  }));
  const unsignedAssertion = `${header}.${claims}`;
  const signer = createSign("RSA-SHA256");
  signer.update(unsignedAssertion);
  const assertion = `${unsignedAssertion}.${signer.sign(privateKey, "base64url")}`;

  const response = await fetch(TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  const result = await response.json();

  if (!response.ok || typeof result.access_token !== "string") {
    throw new Error(`Firebase OAuth token request failed (${response.status}): ${JSON.stringify(result)}`);
  }

  cachedAccessToken = {
    value: result.access_token,
    expiresAt: Date.now() + Number(result.expires_in || 3600) * 1000,
  };
  return cachedAccessToken.value;
};

const sendToDevice = async ({ projectId, accessToken, device, title, body, data }) => {
  const response = await fetch(
    `https://fcm.googleapis.com/v1/projects/${encodeURIComponent(projectId)}/messages:send`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        message: {
          token: device.token,
          notification: { title, body },
          data,
          android: {
            priority: "high",
            notification: { channel_id: "topline_activity_notifications" },
          },
        },
      }),
    }
  );
  const result = await response.json();

  if (response.ok) {
    return;
  }

  const errorCodes = result.error?.details
    ?.map((detail) => detail.errorCode)
    .filter(Boolean) || [];
  if (errorCodes.includes("UNREGISTERED")) {
    await PushDevice.deleteOne({ _id: device._id });
    return;
  }

  throw new Error(`FCM delivery failed (${response.status}): ${JSON.stringify(result)}`);
};

export const sendPushNotification = async ({ userId, title, body, data }) => {
  const serviceAccount = getServiceAccount();
  if (!serviceAccount) {
    return;
  }

  const devices = await PushDevice.find({ user: userId })
    .select("token")
    .limit(100)
    .lean();
  if (devices.length === 0) {
    return;
  }

  const accessToken = await getAccessToken(serviceAccount);
  const outcomes = await Promise.allSettled(
    devices.map((device) =>
      sendToDevice({
        ...serviceAccount,
        accessToken,
        device,
        title,
        body,
        data,
      })
    )
  );

  for (const outcome of outcomes) {
    if (outcome.status === "rejected") {
      console.error("Push notification delivery error:", outcome.reason);
    }
  }
};
