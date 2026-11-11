import assert from "node:assert/strict";
import test from "node:test";
import {
	getPostVideoUrl,
	isVideoFile,
	looksLikeVideoUrl,
} from "./postMedia.js";

test("classifies video uploads from MIME type or filename", () => {
	assert.equal(isVideoFile({ mimetype: "video/mp4", originalname: "clip.bin" }), true);
	assert.equal(isVideoFile({ mimetype: "application/octet-stream", originalname: "clip.MOV" }), true);
	assert.equal(isVideoFile({ mimetype: "image/jpeg", originalname: "photo.jpg" }), false);
});

test("recognizes video URLs and legacy video records", () => {
	assert.equal(looksLikeVideoUrl("https://res.cloudinary.com/demo/video/upload/clip"), true);
	assert.equal(looksLikeVideoUrl("https://cdn.example/clip.mp4?download=1"), true);
	assert.equal(looksLikeVideoUrl("https://cdn.example/photo.jpg"), false);
	assert.equal(
		getPostVideoUrl({ mediaUrl: "https://cdn.example/legacy.mp4" }),
		"https://cdn.example/legacy.mp4",
	);
	assert.equal(
		getPostVideoUrl({ mediaType: "video/quicktime", image: "https://cdn.example/legacy.mov" }),
		"https://cdn.example/legacy.mov",
	);
	assert.equal(
		getPostVideoUrl({ video: "https://cdn.example/current.mp4" }),
		"https://cdn.example/current.mp4",
	);
	assert.equal(getPostVideoUrl({ image: "https://cdn.example/photo.jpg" }), "");
});
