import express from "express";
import fs from "node:fs/promises";
import jwt from "jsonwebtoken";
import mongoose from "mongoose";
import multer from "multer";
import { v2 as cloudinary } from "cloudinary";
import Comment from "../models/Comment.js";
import Friend from "../models/Friend.js";
import Like from "../models/Like.js";
import Notification from "../models/Notification.js";
import Post from "../models/Post.js";
import User from "../models/User.js";

const router = express.Router();

const getEngagementCounts = async (posts) => {
	const postIds = posts.map((post) => post._id);
	if (!postIds.length) return [];

	const [comments, likes] = await Promise.all([
		Comment.aggregate([
			{ $match: { post: { $in: postIds } } },
			{ $group: { _id: "$post", count: { $sum: 1 } } },
		]),
		Like.aggregate([
			{ $match: { post: { $in: postIds } } },
			{ $group: { _id: "$post", count: { $sum: 1 } } },
		]),
	]);
	const commentCounts = new Map(comments.map(({ _id, count }) => [String(_id), count]));
	const likeCounts = new Map(likes.map(({ _id, count }) => [String(_id), count]));

	return posts.map((post) => ({
		...post.toObject(),
		commentsCount: commentCounts.get(String(post._id)) || 0,
		likesCount: likeCounts.get(String(post._id)) || 0,
	}));
};
const upload = multer({
	dest: "uploads/",
	limits: { fileSize: 500 * 1024 * 1024 },
	fileFilter: (_req, file, callback) => {
		if (file.mimetype.startsWith("image/") || file.mimetype.startsWith("video/")) {
			callback(null, true);
			return;
		}

		callback(new Error("Only image and video files can be posted."));
	},
});

cloudinary.config({
	cloud_name: process.env.CLOUDINARY_CLOUD_NAME || "drqqahmxt",
	api_key: process.env.CLOUDINARY_API_KEY,
	api_secret: process.env.CLOUDINARY_API_SECRET,
});

router.get("/mine", async (req, res) => {
	try {
		const token = req.headers.authorization?.split(" ")[1];
		if (!token) {
			return res.status(401).json({ message: "Sign in to view your posts." });
		}

		const decoded = jwt.verify(token, process.env.JWT_SECRET);
		const records = await Post.find({ author: decoded.userId })
			.sort({ createdAt: -1 })
			.populate("author", "name username profilePicture");
		const posts = await getEngagementCounts(records);

		return res.json({ posts, postCount: posts.length });
	} catch (error) {
		return res.status(401).json({ message: error.message || "Unable to load your posts." });
	}
});

router.get("/mine/analytics", async (req, res) => {
	try {
		const token = req.headers.authorization?.split(" ")[1];
		if (!token) {
			return res.status(401).json({ message: "Sign in to view your analytics." });
		}

		const decoded = jwt.verify(token, process.env.JWT_SECRET);
		const records = await Post.find({ author: decoded.userId }).sort({ createdAt: -1 });
		const posts = await getEngagementCounts(records);
		const topPosts = posts
			.map((post) => ({
				_id: post._id,
				text: post.text,
				likes: post.likesCount,
				comments: post.commentsCount,
			}))
			.sort((left, right) => right.likes + right.comments - left.likes - left.comments)
			.slice(0, 5);

		return res.json({
			postsCount: posts.length,
			likesCount: posts.reduce((total, post) => total + post.likesCount, 0),
			commentsCount: posts.reduce((total, post) => total + post.commentsCount, 0),
			topPosts,
		});
	} catch (error) {
		return res.status(401).json({ message: error.message || "Unable to load your analytics." });
	}
});

router.get("/author/:authorId", async (req, res) => {
	try {
		const { authorId } = req.params;
		if (!mongoose.Types.ObjectId.isValid(authorId)) {
			return res.status(400).json({ message: "Invalid user ID." });
		}

		const author = await User.findById(authorId)
			.select("name username profilePicture bio location createdAt")
			.lean();
		if (!author) {
			return res.status(404).json({ message: "User not found." });
		}

		const records = await Post.find({ author: authorId })
			.sort({ createdAt: -1 })
			.populate("author", "name username profilePicture");
		const [posts, followersCount, followingCount] = await Promise.all([
			getEngagementCounts(records),
			Friend.countDocuments({ following: authorId }),
			Friend.countDocuments({ follower: authorId }),
		]);

		return res.json({ user: author, posts, followersCount, followingCount });
	} catch (error) {
		console.error("Load author posts error:", error);
		return res.status(500).json({ message: "Unable to load profile posts." });
	}
});

router.get("/following", async (req, res) => {
	try {
		const token = req.headers.authorization?.split(" ")[1];
		if (!token) {
			return res.status(401).json({ message: "Sign in to view videos from accounts you follow." });
		}

		let userId;
		try {
			userId = jwt.verify(token, process.env.JWT_SECRET).userId;
		} catch {
			return res.status(401).json({ message: "Your session has expired." });
		}

		const followingIds = await Friend.distinct("following", { follower: userId });
		if (!followingIds.length) {
			return res.json({ posts: [], postCount: 0 });
		}

		const records = await Post.find({
			author: { $in: followingIds },
			video: { $nin: ["", null] },
		})
			.sort({ createdAt: -1 })
			.limit(50)
			.populate("author", "name username profilePicture");
		const [posts, likedPostIds] = await Promise.all([
			getEngagementCounts(records),
			Like.distinct("post", { user: userId, post: { $in: records.map((post) => post._id) } }),
		]);
		const likedIds = new Set(likedPostIds.map(String));

		return res.json({
			posts: posts.map((post) => ({
				...post,
				likedByCurrentUser: likedIds.has(String(post._id)),
			})),
			postCount: posts.length,
		});
	} catch (error) {
		console.error("Load following videos error:", error);
		return res.status(500).json({ message: "Unable to load videos from accounts you follow." });
	}
});

router.get("/", async (req, res) => {
	try {
		const records = await Post.find()
			.sort({ createdAt: -1 })
			.limit(50)
			.populate("author", "name username profilePicture");
		const token = req.headers.authorization?.split(" ")[1];
		let currentUserId = null;
		if (token) {
			try {
				currentUserId = jwt.verify(token, process.env.JWT_SECRET).userId;
			} catch {
				currentUserId = null;
			}
		}
		const [posts, likedPostIds] = await Promise.all([
			getEngagementCounts(records),
			currentUserId
				? Like.distinct("post", { user: currentUserId })
				: Promise.resolve([]),
		]);
		const likedIds = new Set(likedPostIds.map(String));
		const personalizedPosts = posts.map((post) => ({
			...post,
			likedByCurrentUser: likedIds.has(String(post._id)),
		}));

		res.json({ posts: personalizedPosts });
	} catch (error) {
		console.error("Load posts error:", error);
		res.status(500).json({ message: "Unable to load posts." });
	}
});

router.post("/", upload.single("file"), async (req, res) => {
	try {
		const token = req.headers.authorization?.split(" ")[1];
		if (!token) {
			return res.status(401).json({ message: "Sign in to create a post." });
		}

		let decoded;
		try {
			decoded = jwt.verify(token, process.env.JWT_SECRET);
		} catch {
			return res.status(401).json({ message: "Your session has expired." });
		}

		const authorExists = await User.exists({ _id: decoded.userId });
		if (!authorExists) {
			return res.status(401).json({ message: "User account was not found." });
		}

		const text = typeof req.body.text === "string" ? req.body.text.trim() : "";
		if (!text && !req.file) {
			return res.status(400).json({ message: "Add text, a photo, or a video." });
		}

		let image = "";
		let video = "";

		if (req.file) {
			if (!process.env.CLOUDINARY_API_KEY || !process.env.CLOUDINARY_API_SECRET) {
				return res.status(503).json({ message: "Media uploads are not configured on the server." });
			}

			const resourceType = req.file.mimetype.startsWith("video/") ? "video" : "image";
			let uploaded;
			try {
				uploaded = resourceType === "video"
					? await cloudinary.uploader.upload_large(req.file.path, {
						folder: "topline/posts",
						resource_type: "video",
						chunk_size: 6 * 1024 * 1024,
						eager: [{ format: "mp4", video_codec: "h264", audio_codec: "aac" }],
						eager_async: false,
					})
					: await cloudinary.uploader.upload(req.file.path, {
						folder: "topline/posts",
						resource_type: "image",
					});
			} catch (error) {
				console.error("Cloudinary media upload error:", error);
				return res.status(error.http_code === 413 ? 413 : 502).json({
					message: error.message || "The media upload or video conversion failed.",
				});
			}

			if (resourceType === "video") {
				video = uploaded.eager?.find((format) => format.format === "mp4")?.secure_url
					|| uploaded.secure_url;
			} else {
				image = uploaded.secure_url;
			}
		}

		const post = await Post.create({
			author: decoded.userId,
			text,
			image,
			video,
		});

		await post.populate("author", "name username profilePicture");
		try {
			const followers = await Friend.find({ following: decoded.userId })
				.select("follower")
				.lean();
			if (followers.length) {
				await Notification.insertMany(
					followers.map(({ follower }) => ({
						recipient: follower,
						actor: decoded.userId,
						type: "post",
						entityId: String(post._id),
						text: "shared a new post.",
					})),
					{ ordered: false }
				);
			}
		} catch (notificationError) {
			console.error("Notify followers about new post error:", notificationError);
		}

		return res.status(201).json({
			success: true,
			message: "Post created successfully.",
			post,
		});
	} catch (error) {
		console.error("Create post error:", error);
		return res.status(500).json({ message: "Unable to create post." });
	} finally {
		if (req.file?.path) {
			await fs.unlink(req.file.path).catch(() => {});
		}
	}
});

router.use((error, _req, res, _next) => {
	if (error instanceof multer.MulterError && error.code === "LIMIT_FILE_SIZE") {
		return res.status(413).json({ message: "Media files must be 500 MB or smaller." });
	}

	return res.status(400).json({ message: error.message || "Invalid media upload." });
});

export default router;
