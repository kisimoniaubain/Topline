import express from "express";
import fs from "node:fs/promises";
import jwt from "jsonwebtoken";
import mongoose from "mongoose";
import multer from "multer";
import { v2 as cloudinary } from "cloudinary";
import Comment from "../models/Comment.js";
import Friend from "../models/Friend.js";
import Like from "../models/Like.js";
import Post from "../models/Post.js";
import SavedPost from "../models/SavedPost.js";
import User from "../models/User.js";
import { createNotification } from "../utils/createNotification.js";

const router = express.Router();

const looksLikeVideoUrl = (url) =>
	typeof url === "string" &&
	(/\/video\/upload\//i.test(url) || /\.(3gp|m3u8|m4v|mkv|mov|mp4|webm)(?:[?#]|$)/i.test(url));

const toClientPost = (post) => {
	const value = typeof post.toObject === "function" ? post.toObject() : post;
	const mediaType = value.mediaType || value.media_type || "";
	const declaresVideo = mediaType.toLowerCase() === "video" ||
		mediaType.toLowerCase().startsWith("video/");
	const videoUrl = value.video || value.videoUrl || value.video_url ||
		(declaresVideo ? value.mediaUrl || value.image : "") ||
		(looksLikeVideoUrl(value.mediaUrl) ? value.mediaUrl : "") ||
		(looksLikeVideoUrl(value.image) ? value.image : "");

	return {
		...value,
		video: videoUrl,
		videoUrl,
		mediaUrl: value.mediaUrl || videoUrl || value.image || "",
		mediaType: videoUrl ? "video/mp4" : mediaType,
	};
};

const getEngagementCounts = async (posts) => {
	const postIds = posts.map((post) => post._id);
	if (!postIds.length) return [];

	const [comments, likes, saves] = await Promise.all([
		Comment.aggregate([
			{ $match: { post: { $in: postIds } } },
			{ $group: { _id: "$post", count: { $sum: 1 } } },
		]),
		Like.aggregate([
			{ $match: { post: { $in: postIds } } },
			{ $group: { _id: "$post", count: { $sum: 1 } } },
		]),
		SavedPost.aggregate([
			{ $match: { post: { $in: postIds } } },
			{ $group: { _id: "$post", count: { $sum: 1 } } },
		]),
	]);
	const commentCounts = new Map(comments.map(({ _id, count }) => [String(_id), count]));
	const likeCounts = new Map(likes.map(({ _id, count }) => [String(_id), count]));
	const saveCounts = new Map(saves.map(({ _id, count }) => [String(_id), count]));

	return posts.map((post) => ({
		...toClientPost(post),
		commentsCount: commentCounts.get(String(post._id)) || 0,
		likesCount: likeCounts.get(String(post._id)) || 0,
		sharesCount: post.sharesCount || 0,
		savesCount: saveCounts.get(String(post._id)) || 0,
	}));
};
const upload = multer({
	dest: "uploads/",
	limits: { fileSize: 500 * 1024 * 1024 },
	fileFilter: (_req, file, callback) => {
		if (file.fieldname === "cover" && !file.mimetype.startsWith("image/")) {
			callback(new Error("Choose an image for the video cover."));
			return;
		}
		if (file.mimetype.startsWith("image/") || file.mimetype.startsWith("video/")) {
			callback(null, true);
			return;
		}

		callback(new Error("Only image and video files can be posted."));
	},
}).fields([
	{ name: "file", maxCount: 1 },
	{ name: "cover", maxCount: 1 },
]);

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
		const [posts, followersCount, followingCount] = await Promise.all([
			getEngagementCounts(records),
			Friend.countDocuments({ following: decoded.userId }),
			Friend.countDocuments({ follower: decoded.userId }),
		]);
		const toPerformance = (post) => ({
			_id: post._id,
			text: post.text,
			image: post.image,
			video: post.video,
			coverImage: post.coverImage,
			createdAt: post.createdAt,
			likes: post.likesCount,
			comments: post.commentsCount,
		});
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
			followersCount,
			followingCount,
			recentPosts: posts.slice(0, 5).map(toPerformance),
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

		let visiblePostFilter = {
			$or: [
				{ visibility: "public" },
				{ visibility: { $exists: false } },
			],
		};
		const token = req.headers.authorization?.split(" ")[1];
		if (token) {
			try {
				const viewerId = jwt.verify(token, process.env.JWT_SECRET).userId;
				if (String(viewerId) === String(authorId)) {
					visiblePostFilter = {};
				} else if (await Friend.exists({ follower: viewerId, following: authorId })) {
					visiblePostFilter.$or.push({ visibility: "followers" });
				}
			} catch {
				// Invalid optional credentials do not grant access to follower-only posts.
			}
		}
		const records = await Post.find({ author: authorId, ...visiblePostFilter })
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
			$or: [{ visibility: { $in: ["public", "followers"] } }, { visibility: { $exists: false } }],
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
		const token = req.headers.authorization?.split(" ")[1];
		let currentUserId = null;
		let visibleAuthorIds = [];
		if (token) {
			try {
				currentUserId = jwt.verify(token, process.env.JWT_SECRET).userId;
				visibleAuthorIds = await Friend.distinct("following", { follower: currentUserId });
			} catch {
				currentUserId = null;
			}
		}
		const visibilityFilter = currentUserId
			? {
				$or: [
					{ visibility: "public" },
					{ visibility: { $exists: false } },
					{ author: currentUserId },
					{ author: { $in: visibleAuthorIds }, visibility: "followers" },
				],
			}
			: { $or: [{ visibility: "public" }, { visibility: { $exists: false } }] };
		const records = await Post.find(visibilityFilter)
			.sort({ createdAt: -1 })
			.limit(50)
			.populate("author", "name username profilePicture");
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

router.post("/:postId/share", async (req, res) => {
	try {
		const token = req.headers.authorization?.split(" ")[1];
		if (!token) {
			return res.status(401).json({ message: "Sign in to count post shares." });
		}

		let decoded;
		try {
			decoded = jwt.verify(token, process.env.JWT_SECRET);
		} catch {
			return res.status(401).json({ message: "Your session has expired." });
		}

		if (!mongoose.Types.ObjectId.isValid(decoded.userId)) {
			return res.status(401).json({ message: "User account was not found." });
		}
		const { postId } = req.params;
		if (!mongoose.Types.ObjectId.isValid(postId)) {
			return res.status(400).json({ message: "Invalid post ID." });
		}
		if (!(await User.exists({ _id: decoded.userId }))) {
			return res.status(401).json({ message: "User account was not found." });
		}

		const post = await Post.findByIdAndUpdate(
			postId,
			{ $inc: { sharesCount: 1 } },
			{ new: true, runValidators: true },
		).select("sharesCount");
		if (!post) {
			return res.status(404).json({ message: "Post not found." });
		}

		return res.json({ success: true, sharesCount: post.sharesCount });
	} catch (error) {
		console.error("Record post share error:", error);
		return res.status(500).json({ message: "Unable to count this post share." });
	}
});

router.post("/", upload, async (req, res) => {
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
		const file = req.files?.file?.[0];
		const cover = req.files?.cover?.[0];
		const visibility = req.body.visibility === "followers" ? "followers" : "public";
		const allowComments = req.body.allowComments !== "false";
		if (!text && !file) {
			return res.status(400).json({ message: "Add text, a photo, or a video." });
		}

		let image = "";
		let video = "";
		let coverImage = "";

		if (file) {
			if (!process.env.CLOUDINARY_API_KEY || !process.env.CLOUDINARY_API_SECRET) {
				return res.status(503).json({ message: "Media uploads are not configured on the server." });
			}

			const resourceType = file.mimetype.startsWith("video/") ? "video" : "image";
			let uploaded;
			try {
				uploaded = resourceType === "video"
					? await cloudinary.uploader.upload_large(file.path, {
						folder: "topline/posts",
						resource_type: "video",
						chunk_size: 6 * 1024 * 1024,
						eager: [{ format: "mp4", video_codec: "h264", audio_codec: "aac" }],
						eager_async: false,
					})
					: await cloudinary.uploader.upload(file.path, {
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

		if (cover) {
			try {
				const uploadedCover = await cloudinary.uploader.upload(cover.path, {
					folder: "topline/posts/covers",
					resource_type: "image",
				});
				coverImage = uploadedCover.secure_url;
			} catch (error) {
				console.error("Cloudinary cover upload error:", error);
				return res.status(502).json({ message: "The cover image could not be uploaded." });
			}
		}

		const post = await Post.create({
			author: decoded.userId,
			text,
			image,
			video,
			coverImage,
			visibility,
			allowComments,
		});

		await post.populate("author", "name username profilePicture");
		try {
			const followers = await Friend.find({ following: decoded.userId })
				.select("follower")
				.lean();
			if (followers.length) {
				await Promise.all(
					followers.map(({ follower }) => createNotification({
						recipient: follower,
						actor: decoded.userId,
						type: "post",
						entityId: String(post._id),
						text: "shared a new post.",
					}))
				);
			}
		} catch (notificationError) {
			console.error("Notify followers about new post error:", notificationError);
		}

		return res.status(201).json({
			success: true,
			message: "Post created successfully.",
			post: toClientPost(post),
		});
	} catch (error) {
		console.error("Create post error:", error);
		return res.status(500).json({ message: "Unable to create post." });
	} finally {
		for (const file of Object.values(req.files || {}).flat()) {
			if (file.path) await fs.unlink(file.path).catch(() => {});
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
