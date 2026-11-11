import assert from "node:assert/strict";
import test from "node:test";
import { buildS3PublicUrl, isS3StorageConfigured } from "./s3MediaStorage.js";

test("S3 media storage requires a bucket and both credentials", () => {
	const originalValues = {
		bucket: process.env.S3_BUCKET,
		region: process.env.S3_REGION,
		accessKeyId: process.env.S3_ACCESS_KEY_ID,
		secretAccessKey: process.env.S3_SECRET_ACCESS_KEY,
	};

	try {
		process.env.S3_BUCKET = "topline-media";
		process.env.S3_REGION = "us-east-1";
		process.env.S3_ACCESS_KEY_ID = "AKIAIOSFODNN7FGHIJ";
		delete process.env.S3_SECRET_ACCESS_KEY;
		assert.equal(isS3StorageConfigured(), false);

		process.env.S3_SECRET_ACCESS_KEY = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOP";
		assert.equal(isS3StorageConfigured(), true);
	} finally {
		if (originalValues.bucket === undefined) delete process.env.S3_BUCKET;
		else process.env.S3_BUCKET = originalValues.bucket;
		if (originalValues.region === undefined) delete process.env.S3_REGION;
		else process.env.S3_REGION = originalValues.region;
		if (originalValues.accessKeyId === undefined) delete process.env.S3_ACCESS_KEY_ID;
		else process.env.S3_ACCESS_KEY_ID = originalValues.accessKeyId;
		if (originalValues.secretAccessKey === undefined) delete process.env.S3_SECRET_ACCESS_KEY;
		else process.env.S3_SECRET_ACCESS_KEY = originalValues.secretAccessKey;
	}
});

test("S3 media storage rejects placeholder credentials and regions", () => {
	const originalValues = {
		bucket: process.env.S3_BUCKET,
		region: process.env.S3_REGION,
		accessKeyId: process.env.S3_ACCESS_KEY_ID,
		secretAccessKey: process.env.S3_SECRET_ACCESS_KEY,
	};

	try {
		process.env.S3_BUCKET = "your-bucket-name";
		process.env.S3_REGION = "your-real-region";
		process.env.S3_ACCESS_KEY_ID = "your-access-key";
		process.env.S3_SECRET_ACCESS_KEY = "your-secret-key";
		assert.equal(isS3StorageConfigured(), false);
	} finally {
		if (originalValues.bucket === undefined) delete process.env.S3_BUCKET;
		else process.env.S3_BUCKET = originalValues.bucket;
		if (originalValues.region === undefined) delete process.env.S3_REGION;
		else process.env.S3_REGION = originalValues.region;
		if (originalValues.accessKeyId === undefined) delete process.env.S3_ACCESS_KEY_ID;
		else process.env.S3_ACCESS_KEY_ID = originalValues.accessKeyId;
		if (originalValues.secretAccessKey === undefined) delete process.env.S3_SECRET_ACCESS_KEY;
		else process.env.S3_SECRET_ACCESS_KEY = originalValues.secretAccessKey;
	}
});

test("S3 media URLs use the bucket region and encode object key segments", () => {
	assert.equal(
		buildS3PublicUrl("topline-media", "eu-west-1", "", "topline/posts/hello world.mp4"),
		"https://topline-media.s3.eu-west-1.amazonaws.com/topline/posts/hello%20world.mp4",
	);
});

test("S3 media URLs honor a configured public base URL", () => {
	assert.equal(
		buildS3PublicUrl("topline-media", "eu-west-1", "https://cdn.example/media/", "posts/clip.mp4"),
		"https://cdn.example/media/posts/clip.mp4",
	);
});
