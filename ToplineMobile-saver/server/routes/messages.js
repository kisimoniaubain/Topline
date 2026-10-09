import express from "express";
import fs from "node:fs/promises";
import jwt from "jsonwebtoken";
import mongoose from "mongoose";
import multer from "multer";
import { v2 as cloudinary } from "cloudinary";
import Friend from "../models/Friend.js";
import Message from "../models/Message.js";
import User from "../models/User.js";
import { createNotification } from "../utils/createNotification.js";

const router = express.Router();
const imageUpload = multer({
  dest: "uploads/",
  limits: { fileSize: 10 * 1024 * 1024 },
  fileFilter: (_req, file, callback) => {
    if (file.mimetype.startsWith("image/")) {
      callback(null, true);
      return;
    }
    callback(new Error("Choose an image to send."));
  },
});

cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME || "drqqahmxt",
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET,
});

const getAuthenticatedUserId = (req) => {
  const token = req.headers.authorization?.split(" ")[1];
  if (!token) {
    const error = new Error("Sign in to view messages.");
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

router.post("/presence", async (req, res) => {
  try {
    const userId = getAuthenticatedUserId(req);
    await User.findByIdAndUpdate(userId, { $set: { lastActiveAt: new Date() } });
    return res.json({ success: true });
  } catch (error) {
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to update online status.",
    });
  }
});

router.get("/inbox", async (req, res) => {
  try {
    const userId = getAuthenticatedUserId(req);
    const [followingRelations, messages] = await Promise.all([
      Friend.find({ follower: userId })
        .sort({ createdAt: -1 })
        .limit(100)
        .populate("following", "name username profilePicture lastActiveAt"),
      Message.find({ $or: [{ sender: userId }, { receiver: userId }] })
        .sort({ createdAt: -1 })
        .limit(1000)
        .populate("sender", "name username profilePicture lastActiveAt")
        .populate("receiver", "name username profilePicture lastActiveAt"),
    ]);

    const now = Date.now();
    const contact = (person) => ({
      _id: person._id,
      name: person.name,
      username: person.username,
      profilePicture: person.profilePicture,
      isOnline: person.lastActiveAt != null && now - person.lastActiveAt.getTime() < 120_000,
    });
    const following = followingRelations
      .map(({ following: person }) => person)
      .filter(Boolean)
      .map(contact);
    const latestByContact = new Map();
    for (const message of messages) {
      const person = String(message.sender._id) === String(userId) ? message.receiver : message.sender;
      const personId = String(person._id);
      if (!latestByContact.has(personId)) {
        latestByContact.set(personId, {
          user: contact(person),
          lastMessage: message.text || (message.image ? "Photo" : ""),
          updatedAt: message.createdAt,
        });
      }
    }

    return res.json({ following, conversations: [...latestByContact.values()] });
  } catch (error) {
    if (!error.status) {
      console.error("Load inbox error:", error);
    }
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to load your inbox.",
    });
  }
});

router.get("/:userId", async (req, res) => {
  try {
    const currentUserId = getAuthenticatedUserId(req);
    const { userId } = req.params;
    if (!mongoose.Types.ObjectId.isValid(userId)) {
      return res.status(400).json({ message: "Invalid account ID." });
    }

    const messages = await Message.find({
      $or: [
        { sender: currentUserId, receiver: userId },
        { sender: userId, receiver: currentUserId },
      ],
    })
      .sort({ createdAt: -1 })
      .limit(100)
      .populate("sender", "name username profilePicture")
      .populate("receiver", "name username profilePicture");

    return res.json({ messages: messages.reverse() });
  } catch (error) {
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to load this conversation.",
    });
  }
});

router.post("/:userId", imageUpload.single("file"), async (req, res) => {
  try {
    const senderId = getAuthenticatedUserId(req);
    const { userId } = req.params;
    const text = typeof req.body.text === "string" ? req.body.text.trim() : "";

    if (!mongoose.Types.ObjectId.isValid(userId)) {
      return res.status(400).json({ message: "Invalid account ID." });
    }
    if (String(senderId) === String(userId)) {
      return res.status(400).json({ message: "You cannot message yourself." });
    }
    if ((!text && !req.file) || text.length > 2000) {
      return res.status(400).json({ message: "Add a message or image. Text must be 2,000 characters or fewer." });
    }
    if (!(await User.exists({ _id: userId }))) {
      return res.status(404).json({ message: "Account not found." });
    }

    let image = "";
    if (req.file) {
      if (!process.env.CLOUDINARY_API_KEY || !process.env.CLOUDINARY_API_SECRET) {
        return res.status(503).json({ message: "Image messages are not configured on the server." });
      }
      const uploaded = await cloudinary.uploader.upload(req.file.path, {
        folder: "topline/messages",
        resource_type: "image",
      });
      image = uploaded.secure_url;
    }

    const message = await Message.create({ sender: senderId, receiver: userId, text, image });
    await message.populate("sender receiver", "name username profilePicture");
    await createNotification({
      recipient: userId,
      actor: senderId,
      type: "message",
      entityId: String(message._id),
      text: text ? `sent you a message: ${text.slice(0, 350)}` : "sent you a photo.",
    });
    return res.status(201).json({ messages: [message] });
  } catch (error) {
    console.error("Send message error:", error);
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to send your message.",
    });
  } finally {
    if (req.file?.path) {
      await fs.unlink(req.file.path).catch(() => {});
    }
  }
});

router.patch("/:messageId", async (req, res) => {
  try {
    const senderId = getAuthenticatedUserId(req);
    const { messageId } = req.params;
    const text = typeof req.body.text === "string" ? req.body.text.trim() : "";
    if (!mongoose.Types.ObjectId.isValid(messageId)) {
      return res.status(400).json({ message: "Invalid message ID." });
    }
    if (!text || text.length > 2000) {
      return res.status(400).json({ message: "Messages must be between 1 and 2,000 characters." });
    }

    const message = await Message.findOneAndUpdate(
      { _id: messageId, sender: senderId },
      { $set: { text } },
      { returnDocument: "after", runValidators: true },
    ).populate("sender receiver", "name username profilePicture");
    if (!message) {
      return res.status(404).json({ message: "Message not found or you cannot edit it." });
    }
    return res.json({ message });
  } catch (error) {
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to edit this message.",
    });
  }
});

router.delete("/:messageId", async (req, res) => {
  try {
    const senderId = getAuthenticatedUserId(req);
    const { messageId } = req.params;
    if (!mongoose.Types.ObjectId.isValid(messageId)) {
      return res.status(400).json({ message: "Invalid message ID." });
    }
    const message = await Message.findOneAndDelete({ _id: messageId, sender: senderId });
    if (!message) {
      return res.status(404).json({ message: "Message not found or you cannot delete it." });
    }
    return res.json({ success: true, messageId });
  } catch (error) {
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to delete this message.",
    });
  }
});

router.use((error, _req, res, _next) => {
  if (error instanceof multer.MulterError && error.code === "LIMIT_FILE_SIZE") {
    return res.status(413).json({ message: "Message images must be 10 MB or smaller." });
  }
  return res.status(400).json({ message: error.message || "Invalid message attachment." });
});

export default router;