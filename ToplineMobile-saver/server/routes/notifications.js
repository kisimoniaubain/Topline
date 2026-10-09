import express from "express";
import jwt from "jsonwebtoken";
import mongoose from "mongoose";
import Notification from "../models/Notification.js";

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