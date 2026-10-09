import express from "express";
import jwt from "jsonwebtoken";
import mongoose from "mongoose";
import Notification from "../models/Notification.js";
import PushDevice from "../models/PushDevice.js";

const router = express.Router();

const getAuthenticatedUserId = (req) => {
  const token = req.headers.authorization?.split(" ")[1];
  if (!token) {
    const error = new Error("Sign in to view notifications.");
    error.status = 401;
    throw error;
  }

  try {
    return jwt.verify(token, process.env.JWT_SECRET).userId;
  } catch {
    const error = new Error("Your session has expired.");
    error.status = 401;
    throw error;
  }
};

router.post("/devices", async (req, res) => {
  try {
    const userId = getAuthenticatedUserId(req);
    const token = typeof req.body.token === "string" ? req.body.token.trim() : "";
    if (token.length < 20 || token.length > 4096) {
      return res.status(400).json({ message: "Invalid push notification device token." });
    }

    await PushDevice.findOneAndUpdate(
      { token },
      {
        $set: {
          user: userId,
          token,
          platform: "android",
          lastRegisteredAt: new Date(),
        },
      },
      { upsert: true, new: true, runValidators: true }
    );
    return res.status(200).json({ success: true, message: "This device is registered for notifications." });
  } catch (error) {
    if (!error.status) {
      console.error("Register push device error:", error);
    }
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to register this device for notifications.",
    });
  }
});

router.delete("/devices", async (req, res) => {
  try {
    const userId = getAuthenticatedUserId(req);
    const token = typeof req.body.token === "string" ? req.body.token.trim() : "";
    if (!token) {
      return res.status(400).json({ message: "Device token is required." });
    }

    await PushDevice.deleteOne({ user: userId, token });
    return res.json({ success: true, message: "This device was unregistered for notifications." });
  } catch (error) {
    if (!error.status) {
      console.error("Unregister push device error:", error);
    }
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to unregister this device.",
    });
  }
});

router.get("/", async (req, res) => {
  try {
    const userId = getAuthenticatedUserId(req);
    const notifications = await Notification.find({ recipient: userId })
      .sort({ createdAt: -1 })
      .limit(200)
      .populate("actor", "name username profilePicture")
      .lean();

    return res.json({ notifications });
  } catch (error) {
    if (!error.status) {
      console.error("Load notifications error:", error);
    }
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to load notifications.",
    });
  }
});

router.patch("/:notificationId/read", async (req, res) => {
  try {
    const userId = getAuthenticatedUserId(req);
    const { notificationId } = req.params;
    if (!mongoose.Types.ObjectId.isValid(notificationId)) {
      return res.status(400).json({ message: "Invalid notification ID." });
    }

    const notification = await Notification.findOneAndUpdate(
      { _id: notificationId, recipient: userId },
      { $set: { isRead: true } },
      { returnDocument: "after" }
    );
    if (!notification) {
      return res.status(404).json({ message: "Notification not found." });
    }
    return res.json({ success: true, notification });
  } catch (error) {
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to update notification.",
    });
  }
});

export default router;