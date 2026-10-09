import express from "express";
import jwt from "jsonwebtoken";
import mongoose from "mongoose";
import Like from "../models/Like.js";
import Notification from "../models/Notification.js";
import Post from "../models/Post.js";
import { createNotification } from "../utils/createNotification.js";

const router = express.Router();

const getAuthenticatedUserId = (req) => {
  const token = req.headers.authorization?.split(" ")[1];
  if (!token) {
    const error = new Error("Sign in to like posts.");
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

router.get("/mine", async (req, res) => {
  try {
    const userId = getAuthenticatedUserId(req);
    const likes = await Like.find({ user: userId }).select("post").lean();
    return res.json({ likedPostIds: likes.map((like) => String(like.post)) });
  } catch (error) {
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to load liked posts.",
    });
  }
});

router.get("/:postId", async (req, res) => {
  try {
    const { postId } = req.params;
    if (!mongoose.Types.ObjectId.isValid(postId)) {
      return res.status(400).json({ message: "Invalid post ID." });
    }

    const post = await Post.findById(postId).select("_id");
    if (!post) {
      return res.status(404).json({ message: "Post not found." });
    }

    const token = req.headers.authorization?.split(" ")[1];
    let likedByCurrentUser = false;
    if (token) {
      try {
        const decoded = jwt.verify(token, process.env.JWT_SECRET);
        likedByCurrentUser = !!(await Like.exists({ post: postId, user: decoded.userId }));
      } catch {
        likedByCurrentUser = false;
      }
    }

    const likesCount = await Like.countDocuments({ post: postId });
    return res.json({
      success: true,
      postId,
      likedByCurrentUser,
      likesCount,
    });
  } catch (error) {
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to load likes.",
    });
  }
});

router.post("/:postId", async (req, res) => {
  try {
    const userId = getAuthenticatedUserId(req);
    const { postId } = req.params;

    if (!mongoose.Types.ObjectId.isValid(postId)) {
      return res.status(400).json({ message: "Invalid post ID." });
    }

    const post = await Post.findById(postId).select("_id author");
    if (!post) {
      return res.status(404).json({ message: "Post not found." });
    }

    const existingLike = await Like.findOne({ user: userId, post: postId });
    if (existingLike) {
      await existingLike.deleteOne();
      try {
        await Notification.deleteOne({
          recipient: post.author,
          actor: userId,
          type: "like",
          entityId: postId,
        });
      } catch (error) {
        console.error("Remove unliked-post notification error:", error);
      }
      const likesCount = await Like.countDocuments({ post: postId });
      return res.json({
        success: true,
        likedByCurrentUser: false,
        likesCount,
        message: "Post like removed.",
      });
    }

    await Like.create({ user: userId, post: postId });
    await createNotification({
      recipient: post.author,
      actor: userId,
      type: "like",
      entityId: postId,
      text: "liked your post.",
    });
    const likesCount = await Like.countDocuments({ post: postId });
    return res.status(201).json({
      success: true,
      likedByCurrentUser: true,
      likesCount,
      message: "Post liked.",
    });
  } catch (error) {
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to update like.",
    });
  }
});

export default router;
