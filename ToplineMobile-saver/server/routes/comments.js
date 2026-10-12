import express from "express";
import fs from "node:fs/promises";
import jwt from "jsonwebtoken";
import mongoose from "mongoose";
import multer from "multer";
import { v2 as cloudinary } from "cloudinary";
import Comment from "../models/Comment.js";
import Post from "../models/Post.js";
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
		callback(new Error("Choose an image to add to your comment."));
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

const getOptionalAuthenticatedUserId = (req) => {
	const token = req.headers.authorization?.split(" ")[1];
	if (!token) return null;
	try {
		return jwt.verify(token, process.env.JWT_SECRET).userId;
	} catch {
		return null;
	}
};

router.get("/:postId", async (req, res) => {
	try {
		const { postId } = req.params;
		if (!mongoose.Types.ObjectId.isValid(postId)) {
			return res.status(400).json({ message: "Invalid post ID." });
		}

		const post = await Post.findById(postId).select("commentsCount allowComments");
		if (!post) {
			return res.status(404).json({ message: "Post not found." });
		}
		if (!post.allowComments) {
			return res.json({ comments: [], commentsCount: 0 });
		}

		const currentUserId = getOptionalAuthenticatedUserId(req);
		const comments = await Comment.find({ post: postId })
			.sort({ createdAt: 1 })
			.limit(100)
			.populate("author", "name username profilePicture");
		const commentsCount = await Comment.countDocuments({ post: postId });
		if (post.commentsCount !== commentsCount) {
			await Post.updateOne({ _id: postId }, { $set: { commentsCount } });
		}

		return res.json({
			comments: comments.map((comment) => {
				const { likes, ...commentData } = comment.toObject();
				return {
					...commentData,
					likesCount: likes.length,
					likedByCurrentUser: currentUserId
						? likes.some((userId) => String(userId) === String(currentUserId))
						: false,
				};
			}),
			commentsCount,
		});
	} catch (error) {
		console.error("Load comments error:", error);
		return res.status(500).json({ message: "Unable to load comments." });
	}
});

router.post("/:postId", imageUpload.single("image"), async (req, res) => {
	try {
		const authorId = getAuthenticatedUserId(req);
		const { postId } = req.params;
		const text = typeof req.body?.text === "string" ? req.body.text.trim() : "";
		const parentCommentId = typeof req.body?.parentCommentId === "string"
			? req.body.parentCommentId.trim()
			: "";

		if (!mongoose.Types.ObjectId.isValid(postId)) {
			return res.status(400).json({ message: "Invalid post ID." });
		}
		if (!text && !req.file) {
			return res.status(400).json({ message: "Write a comment or attach an image." });
		}
		if (text.length > 1000) {
			return res.status(400).json({ message: "Comments must be 1,000 characters or fewer." });
		}

		const post = await Post.findById(postId).select("author allowComments");
		if (!post) {
			return res.status(404).json({ message: "Post not found." });
		}
		if (!post.allowComments) {
			return res.status(403).json({ message: "Comments are turned off for this post." });
		}

		let parentComment = null;
		if (parentCommentId) {
			if (!mongoose.Types.ObjectId.isValid(parentCommentId)) {
				return res.status(400).json({ message: "Invalid reply target." });
			}
			parentComment = await Comment.findOne({ _id: parentCommentId, post: postId })
				.select("author parentComment");
			if (!parentComment) {
				return res.status(404).json({ message: "The comment you are replying to was not found." });
			}
			if (parentComment.parentComment) {
				return res.status(400).json({ message: "Replies can only be added to a post comment." });
			}
		}

		let image = "";
		if (req.file) {
			if (!process.env.CLOUDINARY_API_KEY || !process.env.CLOUDINARY_API_SECRET) {
				return res.status(503).json({ message: "Comment images are not configured on the server." });
			}
			const uploaded = await cloudinary.uploader.upload(req.file.path, {
				folder: "topline/comments",
				resource_type: "image",
			});
			image = uploaded.secure_url;
		}

		const comment = await Comment.create({
			post: postId,
			author: authorId,
			text,
			image,
			parentComment: parentComment?._id || null,
		});
		await comment.populate("author", "name username profilePicture");
		const updatedPost = await Post.findByIdAndUpdate(
			postId,
			{ $inc: { commentsCount: 1 } },
			{ new: true }
		).select("commentsCount");
		const notificationRecipient = parentComment?.author || post.author;
		if (String(notificationRecipient) !== String(authorId)) {
			await createNotification({
			recipient: notificationRecipient,
			actor: authorId,
			type: "comment",
			entityId: String(comment._id),
			text: parentComment
				? `replied to your comment: ${text.slice(0, 350) || "sent an image"}`
				: `commented on your post: ${text.slice(0, 350) || "sent an image"}`,
			});
		}

		return res.status(201).json({
			success: true,
			comment: {
				...comment.toObject(),
				likesCount: 0,
				likedByCurrentUser: false,
			},
			commentsCount: updatedPost?.commentsCount || 0,
		});
	} catch (error) {
		console.error("Create comment error:", error);
		return res.status(error.status || 500).json({
			message: error.status ? error.message : "Unable to add comment.",
		});
	} finally {
		if (req.file?.path) {
			await fs.unlink(req.file.path).catch((error) => {
				console.warn("Unable to remove temporary comment image:", error.message);
			});
		}
	}
});

router.post("/:commentId/like", async (req, res) => {
	try {
		const userId = getAuthenticatedUserId(req);
		const { commentId } = req.params;
		if (!mongoose.Types.ObjectId.isValid(commentId)) {
			return res.status(400).json({ message: "Invalid comment ID." });
		}

		const comment = await Comment.findById(commentId).select("likes");
		if (!comment) {
			return res.status(404).json({ message: "Comment not found." });
		}
		const alreadyLiked = comment.likes.some((id) => String(id) === String(userId));
		const updatedComment = await Comment.findByIdAndUpdate(
			commentId,
			alreadyLiked ? { $pull: { likes: userId } } : { $addToSet: { likes: userId } },
			{ new: true },
		).select("likes");

		return res.json({
			success: true,
			commentId,
			likesCount: updatedComment.likes.length,
			likedByCurrentUser: updatedComment.likes.some((id) => String(id) === String(userId)),
		});
	} catch (error) {
		return res.status(error.status || 500).json({
			message: error.status ? error.message : "Unable to like this comment.",
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

		const comment = await Comment.findOne({ _id: commentId, author: authorId });
		if (!comment) {
			return res.status(404).json({ message: "Comment not found or you cannot delete it." });
		}
		const deletedIds = [comment._id];
		if (!comment.parentComment) {
			const replies = await Comment.find({ parentComment: comment._id }).select("_id");
			deletedIds.push(...replies.map((reply) => reply._id));
		}
		const deletedCommentsCount = await Comment.deleteMany({ _id: { $in: deletedIds } });

		const post = await Post.findByIdAndUpdate(
			comment.post,
			{ $inc: { commentsCount: -deletedCommentsCount.deletedCount } },
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
