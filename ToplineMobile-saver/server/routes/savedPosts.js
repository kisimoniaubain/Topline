import express from "express";
import jwt from "jsonwebtoken";
import mongoose from "mongoose";
import Post from "../models/Post.js";
import SavedPost from "../models/SavedPost.js";
import SavedCollection from "../models/SavedCollection.js";

const router = express.Router();

const getAuthenticatedUserId = (req) => {
  const token = req.headers.authorization?.split(" ")[1];
  if (!token) {
    const error = new Error("Sign in to save posts.");
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

router.get("/collections", async (req, res) => {
  try {
    const userId = getAuthenticatedUserId(req);
    const { postId } = req.query;
    if (postId && !mongoose.Types.ObjectId.isValid(postId)) {
      return res.status(400).json({ message: "Invalid post ID." });
    }
    const [collections, counts] = await Promise.all([
      SavedCollection.find({ user: userId }).sort({ createdAt: -1 }).lean(),
      SavedPost.aggregate([
        { $match: { user: new mongoose.Types.ObjectId(userId) } },
        { $unwind: "$collections" },
        { $group: { _id: "$collections", postCount: { $sum: 1 } } },
      ]),
    ]);
    const savedPost = postId
      ? await SavedPost.findOne({ user: userId, post: postId }).select("collections").lean()
      : null;
    const includedIds = new Set((savedPost?.collections || []).map(String));
    const countById = new Map(counts.map(({ _id, postCount }) => [String(_id), postCount]));
    return res.json({
      collections: collections.map(({ _id, name }) => ({
        id: String(_id),
        name,
        postCount: countById.get(String(_id)) || 0,
        isIncluded: includedIds.has(String(_id)),
      })),
    });
  } catch (error) {
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to load saved collections.",
    });
  }
});

router.get("/collections/:collectionId", async (req, res) => {
  try {
    const userId = getAuthenticatedUserId(req);
    const { collectionId } = req.params;
    if (!mongoose.Types.ObjectId.isValid(collectionId)) {
      return res.status(400).json({ message: "Invalid collection ID." });
    }
    const collection = await SavedCollection.findOne({ _id: collectionId, user: userId }).lean();
    if (!collection) return res.status(404).json({ message: "Collection not found." });
    const saved = await SavedPost.find({ user: userId, collections: collectionId })
      .sort({ createdAt: -1 })
      .populate({
        path: "post",
        populate: { path: "author", select: "name username profilePicture" },
      })
      .lean();
    return res.json({
      collection: { id: String(collection._id), name: collection.name },
      posts: saved.map(({ post }) => post).filter(Boolean),
    });
  } catch (error) {
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to load this collection.",
    });
  }
});

router.post("/collections", async (req, res) => {
  try {
    const userId = getAuthenticatedUserId(req);
    const name = typeof req.body.name === "string" ? req.body.name.trim() : "";
    if (!name || name.length > 50) {
      return res.status(400).json({ message: "Collection names must be 1 to 50 characters." });
    }
    const collection = await SavedCollection.create({ user: userId, name });
    return res.status(201).json({
      collection: { id: String(collection._id), name: collection.name, postCount: 0 },
    });
  } catch (error) {
    if (error.code === 11000) {
      return res.status(409).json({ message: "You already have a collection with that name." });
    }
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to create collection.",
    });
  }
});

router.put("/collections/:collectionId/posts/:postId", async (req, res) => {
  try {
    const userId = getAuthenticatedUserId(req);
    const { collectionId, postId } = req.params;
    if (!mongoose.Types.ObjectId.isValid(collectionId) || !mongoose.Types.ObjectId.isValid(postId)) {
      return res.status(400).json({ message: "Invalid collection or post ID." });
    }
    if (typeof req.body.included !== "boolean") {
      return res.status(400).json({ message: "The included field must be a boolean." });
    }
    const collection = await SavedCollection.exists({ _id: collectionId, user: userId });
    if (!collection) return res.status(404).json({ message: "Collection not found." });
    if (!(await Post.exists({ _id: postId }))) {
      return res.status(404).json({ message: "Post not found." });
    }
    if (req.body.included) {
      await SavedPost.updateOne(
        { user: userId, post: postId },
        { $setOnInsert: { user: userId, post: postId }, $addToSet: { collections: collectionId } },
        { upsert: true },
      );
    } else {
      await SavedPost.updateOne(
        { user: userId, post: postId },
        { $pull: { collections: collectionId } },
      );
    }
    return res.json({ success: true, included: req.body.included });
  } catch (error) {
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to update collection.",
    });
  }
});

router.delete("/collections/:collectionId", async (req, res) => {
  try {
    const userId = getAuthenticatedUserId(req);
    const { collectionId } = req.params;
    if (!mongoose.Types.ObjectId.isValid(collectionId)) {
      return res.status(400).json({ message: "Invalid collection ID." });
    }
    const deleted = await SavedCollection.findOneAndDelete({ _id: collectionId, user: userId });
    if (!deleted) return res.status(404).json({ message: "Collection not found." });
    await SavedPost.updateMany(
      { user: userId, collections: collectionId },
      { $pull: { collections: collectionId } },
    );
    return res.json({ success: true });
  } catch (error) {
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to delete collection.",
    });
  }
});

router.get("/mine", async (req, res) => {
  try {
    const userId = getAuthenticatedUserId(req);
    const saved = await SavedPost.find({ user: userId })
      .select("post")
      .lean();
    return res.json({ savedPostIds: saved.map(({ post }) => String(post)) });
  } catch (error) {
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to load saved posts.",
    });
  }
});

router.get("/", async (req, res) => {
  try {
    const userId = getAuthenticatedUserId(req);
    const saved = await SavedPost.find({ user: userId })
      .sort({ createdAt: -1 })
      .populate({
        path: "post",
        populate: { path: "author", select: "name username profilePicture" },
      })
      .lean();
    const posts = saved.map(({ post }) => post).filter(Boolean);
    return res.json({ posts });
  } catch (error) {
    if (!error.status) {
      console.error("Load saved posts error:", error);
    }
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to load saved posts.",
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
    if (!(await Post.exists({ _id: postId }))) {
      return res.status(404).json({ message: "Post not found." });
    }

    const existing = await SavedPost.findOneAndDelete({ user: userId, post: postId });
    if (existing) {
      return res.json({ success: true, isSaved: false, message: "Post removed from your saved posts." });
    }

    try {
      await SavedPost.create({ user: userId, post: postId });
    } catch (error) {
      if (error.code !== 11000) throw error;
    }
    return res.status(201).json({ success: true, isSaved: true, message: "Post saved." });
  } catch (error) {
    if (!error.status) {
      console.error("Save post error:", error);
    }
    return res.status(error.status || 500).json({
      message: error.status ? error.message : "Unable to save this post.",
    });
  }
});

export default router;
