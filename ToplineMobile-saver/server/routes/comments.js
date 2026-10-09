import express from "express";
import jwt from "jsonwebtoken";
import mongoose from "mongoose";
import Comment from "../models/Comment.js";
import Post from "../models/Post.js";
import { createNotification } from "../utils/createNotification.js";

const router = express.Router();

const getAuthenticatedUserId = (req) => {
	const token = req.headers.authorization?.split(" ")[1];
	if (!token) {
		const error = new Error("Sign in to comment.");
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

router.get("/:postId", async (req, res) => {
	try {
		const { postId } = req.params;
		if (!mongoose.Types.ObjectId.isValid(postId)) {
			return res.status(400).json({ message: "Invalid post ID." });
		}

		const post = await Post.findById(postId).select("commentsCount");
		if (!post) {
			return res.status(404).json({ message: "Post not found." });
		}

		const comments = await Comment.find({ post: postId })
			.sort({ createdAt: 1 })
			.limit(100)
			.populate("author", "name username profilePicture");
		const commentsCount = await Comment.countDocuments({ post: postId });
		if (post.commentsCount !== commentsCount) {
			await Post.updateOne({ _id: postId }, { $set: { commentsCount } });
		}

		return res.json({ comments, commentsCount });
	} catch (error) {
		console.error("Load comments error:", error);
		return res.status(500).json({ message: "Unable to load comments." });
	}
});

router.post("/:postId", async (req, res) => {
	try {
		const authorId = getAuthenticatedUserId(req);
		const { postId } = req.params;
		const text = typeof req.body.text === "string" ? req.body.text.trim() : "";

		if (!mongoose.Types.ObjectId.isValid(postId)) {
			return res.status(400).json({ message: "Invalid post ID." });
		}
		if (!text) {
			return res.status(400).json({ message: "Write a comment first." });
		}
		if (text.length > 1000) {
			return res.status(400).json({ message: "Comments must be 1,000 characters or fewer." });
		}

		const post = await Post.findById(postId).select("author");
		if (!post) {
			return res.status(404).json({ message: "Post not found." });
		}

		const comment = await Comment.create({ post: postId, author: authorId, text });
		await comment.populate("author", "name username profilePicture");
		const updatedPost = await Post.findByIdAndUpdate(
			postId,
			{ $inc: { commentsCount: 1 } },
			{ new: true }
		).select("commentsCount");
		await createNotification({
			recipient: post.author,
			actor: authorId,
			type: "comment",
			entityId: String(comment._id),
			text: `commented on your post: ${text.slice(0, 350)}`,
		});

		return res.status(201).json({
			success: true,
			comment,
			commentsCount: updatedPost?.commentsCount || 0,
		});
	} catch (error) {
		return res.status(error.status || 500).json({
			message: error.status ? error.message : "Unable to add comment.",
		});
	}
});

router.delete("/:commentId", async (req, res) => {
	try {
		const authorId = getAuthenticatedUserId(req);
		const { commentId } = req.params;
		if (!mongoose.Types.ObjectId.isValid(commentId)) {
			return res.status(400).json({ message: "Invalid comment ID." });
		}

		const comment = await Comment.findOneAndDelete({ _id: commentId, author: authorId });
		if (!comment) {
			return res.status(404).json({ message: "Comment not found or you cannot delete it." });
		}

		const post = await Post.findByIdAndUpdate(
			comment.post,
			{ $inc: { commentsCount: -1 } },
			{ new: true }
		).select("commentsCount");

		return res.json({
			success: true,
			commentId,
			commentsCount: Math.max(0, post?.commentsCount || 0),
		});
	} catch (error) {
		return res.status(error.status || 500).json({
			message: error.status ? error.message : "Unable to delete comment.",
		});
	}
});

export default router;
