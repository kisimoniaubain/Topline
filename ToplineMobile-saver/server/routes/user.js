import express from "express";
import fs from "node:fs/promises";
import jwt from "jsonwebtoken";
import multer from "multer";
import { v2 as cloudinary } from "cloudinary";
import Comment from "../models/Comment.js";
import Friend from "../models/Friend.js";
import Like from "../models/Like.js";
import Message from "../models/Message.js";
import Notification from "../models/Notification.js";
import Post from "../models/Post.js";
import User from "../models/User.js";

const router = express.Router();
const avatarUpload = multer({
  dest: "uploads/",
  limits: { fileSize: 10 * 1024 * 1024 },
  fileFilter: (_req, file, callback) => {
    if (file.mimetype.startsWith("image/")) {
      callback(null, true);
      return;
    }

    callback(new Error("Choose an image file for your profile photo."));
  },
});

cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME || "drqqahmxt",
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET,
});

const authenticateAvatarUpload = (req, res, next) => {
  const token = req.headers.authorization?.split(" ")[1];
  if (!token) {
    return res.status(401).json({ message: "Sign in to update your profile photo." });
  }

  try {
    req.userId = jwt.verify(token, process.env.JWT_SECRET).userId;
    return next();
  } catch {
    return res.status(401).json({ message: "Your session has expired." });
  }
};

const handleAvatarUpload = (req, res, next) => {
  avatarUpload.single("avatar")(req, res, (error) => {
    if (error) {
      return res.status(400).json({ message: error.message || "Invalid profile photo." });
    }

    return next();
  });
};

router.get("/me", async (req, res) => {
  try {
    const token = req.headers.authorization?.split(" ")[1];
    if (!token) return res.status(401).json({ message: "No token" });

    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    const user = await User.findById(decoded.userId).select("-password");

    if (!user) {
      return res.status(404).json({ message: "User not found" });
    }

    res.json({ user });
  } catch (e) {
    res.status(401).json({ message: "Invalid token" });
  }
});

router.put("/me/avatar", authenticateAvatarUpload, handleAvatarUpload, async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ message: "Choose a profile photo to upload." });
    }

    if (!process.env.CLOUDINARY_API_KEY || !process.env.CLOUDINARY_API_SECRET) {
      return res.status(503).json({ message: "Profile photo uploads are not configured on the server." });
    }

    const uploaded = await cloudinary.uploader.upload(req.file.path, {
      folder: "topline/avatars",
      resource_type: "image",
      transformation: [{ width: 512, height: 512, crop: "fill", gravity: "auto" }],
    });

    const user = await User.findByIdAndUpdate(
      req.userId,
      { $set: { profilePicture: uploaded.secure_url } },
      { new: true, runValidators: true }
    ).select("-password");

    if (!user) {
      return res.status(404).json({ message: "User account was not found." });
    }

    return res.json({ success: true, user });
  } catch (error) {
    console.error("Profile photo upload error:", error);
    return res.status(500).json({ message: "Unable to update your profile photo." });
  } finally {
    if (req.file?.path) {
      await fs.unlink(req.file.path).catch(() => {});
    }
  }
});

router.put("/me", async (req, res) => {
  try {
    const token = req.headers.authorization?.split(" ")[1];
    if (!token) return res.status(401).json({ message: "No token" });

    const decoded = jwt.verify(token, process.env.JWT_SECRET);

    const allowedFields = {
      name: 1,
      username: 1,
      email: 1,
      phone: 1,
      bio: 1,
      location: 1,
      profilePicture: 1,
    };

    const updates = {};

    for (const key of Object.keys(req.body || {})) {
      if (allowedFields[key] !== undefined && req.body[key] !== undefined && req.body[key] !== null) {
        updates[key] = req.body[key];
      }
    }

    if (!Object.keys(updates).length) {
      return res.status(400).json({ message: "No valid profile fields provided" });
    }

    if (updates.username) {
      updates.username = updates.username.trim().toLowerCase();
    }

    if (updates.email) {
      updates.email = updates.email.trim().toLowerCase();
    }

    if (updates.name) {
      updates.name = updates.name.trim();
    }

    const user = await User.findByIdAndUpdate(
      decoded.userId,
      { $set: updates },
      { new: true, runValidators: true }
    ).select("-password");

    if (!user) {
      return res.status(404).json({ message: "User not found" });
    }

    res.json({
      success: true,
      message: "Profile updated successfully",
      user,
    });
  } catch (e) {
    console.error("Update profile error:", e);
    res.status(400).json({ message: e.message || "Profile update failed" });
  }
});

router.delete("/me", async (req, res) => {
  try {
    const token = req.headers.authorization?.split(" ")[1];
    if (!token) {
      return res.status(401).json({ message: "Sign in to delete your account." });
    }

    let userId;
    try {
      userId = jwt.verify(token, process.env.JWT_SECRET).userId;
    } catch {
      return res.status(401).json({ message: "Your session has expired." });
    }

    const user = await User.findById(userId).select("_id");
    if (!user) {
      return res.status(404).json({ message: "Account not found." });
    }

    const posts = await Post.find({ author: userId }).select("_id").lean();
    const postIds = posts.map(({ _id }) => _id);
    await Promise.all([
      Post.deleteMany({ author: userId }),
      Comment.deleteMany({ $or: [{ author: userId }, { post: { $in: postIds } }] }),
      Like.deleteMany({ $or: [{ user: userId }, { post: { $in: postIds } }] }),
      Friend.deleteMany({ $or: [{ follower: userId }, { following: userId }] }),
      Message.deleteMany({ $or: [{ sender: userId }, { receiver: userId }] }),
      Notification.deleteMany({ $or: [{ recipient: userId }, { actor: userId }] }),
    ]);
    await user.deleteOne();

    return res.json({ success: true, message: "Account and associated app data deleted." });
  } catch (error) {
    console.error("Delete account error:", error);
    return res.status(500).json({ message: "Unable to delete your account and its app data." });
  }
});

export default router;
