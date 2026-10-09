import express from "express";
import jwt from "jsonwebtoken";
import mongoose from "mongoose";
import Friend from "../models/Friend.js";
import Post from "../models/Post.js";
import User from "../models/User.js";
import Notification from "../models/Notification.js";
import { createNotification } from "../utils/createNotification.js";

const router = express.Router();

const getAuthenticatedUserId = (req) => {
  const token = req.headers.authorization?.split(" ")[1];
  if (!token) {
    const error = new Error("Sign in to follow users.");
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

router.get("/me/counts", async (req, res) => {
  try {
    const userId = getAuthenticatedUserId(req);
    const [followersCount, followingCount, postsCount] = await Promise.all([
      Friend.countDocuments({ following: userId }),
      Friend.countDocuments({ follower: userId }),
      Post.countDocuments({ author: userId }),
    ]);

    return res.json({ followersCount, followingCount, postsCount });
  } catch (error) {
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to load profile counts.",
    });
  }
});

router.get("/me/following", async (req, res) => {
  try {
    const userId = getAuthenticatedUserId(req);
    const relations = await Friend.find({ follower: userId })
      .sort({ createdAt: -1 })
      .populate("following", "name username profilePicture lastActiveAt");
    const now = Date.now();

    return res.json({
      following: relations
        .map(({ following }) => following)
        .filter(Boolean)
        .map((person) => ({
          _id: person._id,
          name: person.name,
          username: person.username,
          profilePicture: person.profilePicture,
          isOnline: person.lastActiveAt != null && now - person.lastActiveAt.getTime() < 120_000,
        })),
    });
  } catch (error) {
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to load followed accounts.",
    });
  }
});

router.get("/me/followers", async (req, res) => {
  try {
    const userId = getAuthenticatedUserId(req);
    const relations = await Friend.find({ following: userId })
      .sort({ createdAt: -1 })
      .populate("follower", "name username profilePicture lastActiveAt");
    const now = Date.now();

    return res.json({
      followers: relations
        .map(({ follower }) => follower)
        .filter(Boolean)
        .map((person) => ({
          _id: person._id,
          name: person.name,
          username: person.username,
          profilePicture: person.profilePicture,
          isOnline: person.lastActiveAt != null && now - person.lastActiveAt.getTime() < 120_000,
        })),
    });
  } catch (error) {
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to load followers.",
    });
  }
});

router.get("/people", async (req, res) => {
  try {
    const userId = getAuthenticatedUserId(req);
    const followingIds = await Friend.distinct("following", { follower: userId });
    const people = await User.find({ _id: { $ne: userId, $nin: followingIds } })
        .select("name username profilePicture bio location")
        .sort({ username: 1 })
        .lean();

    return res.json({
      people: people.map((person) => ({
        ...person,
        _id: String(person._id),
        isFollowing: false,
      })),
    });
  } catch (error) {
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to load people.",
    });
  }
});

router.get("/:userId", async (req, res) => {
  try {
    const { userId } = req.params;
    if (!mongoose.Types.ObjectId.isValid(userId)) {
      return res.status(400).json({ message: "Invalid user ID." });
    }

    const targetUser = await User.exists({ _id: userId });
    if (!targetUser) {
      return res.status(404).json({ message: "User not found." });
    }

    const token = req.headers.authorization?.split(" ")[1];
    let currentUserId = null;
    if (token) {
      try {
        currentUserId = jwt.verify(token, process.env.JWT_SECRET).userId;
      } catch {
        currentUserId = null;
      }
    }

    const [isFollowing, followersCount, followingCount] = await Promise.all([
      currentUserId ? Friend.exists({ follower: currentUserId, following: userId }) : false,
      Friend.countDocuments({ following: userId }),
      currentUserId ? Friend.countDocuments({ follower: currentUserId }) : 0,
    ]);

    return res.json({
      success: true,
      userId,
      isFollowing: !!isFollowing,
      followersCount,
      followingCount,
    });
  } catch (error) {
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to load follow status.",
    });
  }
});

router.post("/:userId", async (req, res) => {
  try {
    const followerId = getAuthenticatedUserId(req);
    const { userId } = req.params;

    if (!mongoose.Types.ObjectId.isValid(userId)) {
      return res.status(400).json({ message: "Invalid user ID." });
    }
    if (String(followerId) === String(userId)) {
      return res.status(400).json({ message: "You cannot follow yourself." });
    }

    const targetUser = await User.exists({ _id: userId });
    if (!targetUser) {
      return res.status(404).json({ message: "User not found." });
    }

    const existingFollow = await Friend.findOne({ follower: followerId, following: userId });
    if (existingFollow) {
      await existingFollow.deleteOne();
      await Notification.deleteOne({
        recipient: userId,
        actor: followerId,
        type: "follow",
      });
      const [followersCount, followingCount] = await Promise.all([
        Friend.countDocuments({ following: userId }),
        Friend.countDocuments({ follower: followerId }),
      ]);

      return res.json({
        success: true,
        isFollowing: false,
        followersCount,
        followingCount,
        message: "User unfollowed.",
      });
    }

    await Friend.create({ follower: followerId, following: userId });
    await createNotification({
      recipient: userId,
      actor: followerId,
      type: "follow",
      entityId: String(followerId),
      text: "started following you.",
    });
    const [followersCount, followingCount] = await Promise.all([
      Friend.countDocuments({ following: userId }),
      Friend.countDocuments({ follower: followerId }),
    ]);

    return res.status(201).json({
      success: true,
      isFollowing: true,
      followersCount,
      followingCount,
      message: "User followed.",
    });
  } catch (error) {
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to update follow status.",
    });
  }
});

export default router;
