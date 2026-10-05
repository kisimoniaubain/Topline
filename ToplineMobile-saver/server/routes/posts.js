import express from "express";
import fs from "node:fs/promises";
import jwt from "jsonwebtoken";
import mongoose from "mongoose";
import multer from "multer";
import { v2 as cloudinary } from "cloudinary";
import Post from "../models/Post.js";
import User from "../models/User.js";

const router = express.Router();
const upload = multer({
	dest: "uploads/",
	limits: { fileSize: 100 * 1024 * 1024 },
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
		const posts = await Post.find({ author: decoded.userId })
			.sort({ createdAt: -1 })
			.populate("author", "name username profilePicture");

		return res.json({ posts });
	} catch (error) {
		return res.status(401).json({ message: error.message || "Unable to load your posts." });
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

		const posts = await Post.find({ author: authorId })
			.sort({ createdAt: -1 })
			.populate("author", "name username profilePicture");

		return res.json({ user: author, posts });
	} catch (error) {
		console.error("Load author posts error:", error);
		return res.status(500).json({ message: "Unable to load profile posts." });
	}
});

router.get("/", async (_req, res) => {
	try {
		const posts = await Post.find()
			.sort({ createdAt: -1 })
			.limit(50)
			.populate("author", "name username profilePicture");

		res.json({ posts });
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
			const uploaded = await cloudinary.uploader.upload(req.file.path, {
				folder: "topline/posts",
				resource_type: resourceType,
			});

			if (resourceType === "video") {
				video = uploaded.secure_url;
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
		return res.status(413).json({ message: "Media files must be 100 MB or smaller." });
	}

	return res.status(400).json({ message: error.message || "Invalid media upload." });
});

export default router;
