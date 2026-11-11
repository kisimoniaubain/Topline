import { createReadStream } from "node:fs";
import path from "node:path";
import { randomUUID } from "node:crypto";
import { S3Client } from "@aws-sdk/client-s3";
import { Upload } from "@aws-sdk/lib-storage";

let s3Client;

const getS3Configuration = () => ({
	bucket: process.env.S3_BUCKET?.trim(),
	region: process.env.S3_REGION?.trim() || "us-east-1",
	accessKeyId: process.env.S3_ACCESS_KEY_ID?.trim(),
	secretAccessKey: process.env.S3_SECRET_ACCESS_KEY?.trim(),
	publicBaseUrl: process.env.S3_PUBLIC_BASE_URL?.trim().replace(/\/$/, ""),
});

const isPlaceholderValue = (value) =>
	/(?:your[-_ ]|example|placeholder|replace|changeme|<|>|\bxxx\b|^test)/i.test(value || "");

export const isS3StorageConfigured = () => {
	const { bucket, region, accessKeyId, secretAccessKey } = getS3Configuration();
	const bucketFormatIsValid = Boolean(
		bucket &&
		bucket.length >= 3 &&
		bucket.length <= 63 &&
		/^[a-z0-9][a-z0-9.-]*[a-z0-9]$/.test(bucket) &&
		!bucket.includes(".."),
	);
	const regionFormatIsValid = /^[a-z]{2}(?:-[a-z0-9]+)+-[0-9]+$/.test(region);

	return Boolean(
		bucketFormatIsValid &&
		regionFormatIsValid &&
		accessKeyId &&
		secretAccessKey &&
		![bucket, region, accessKeyId, secretAccessKey].some(isPlaceholderValue),
	);
};

const getS3Client = ({ region, accessKeyId, secretAccessKey }) => {
	if (!s3Client) {
		s3Client = new S3Client({
			region,
			credentials: { accessKeyId, secretAccessKey },
		});
	}
	return s3Client;
};

export const buildS3PublicUrl = (bucket, region, publicBaseUrl, objectKey) => {
	const encodedKey = objectKey.split("/").map(encodeURIComponent).join("/");
	const normalizedPublicBaseUrl = publicBaseUrl?.replace(/\/+$/, "");
	return normalizedPublicBaseUrl
		? `${normalizedPublicBaseUrl}/${encodedKey}`
		: `https://${bucket}.s3.${region}.amazonaws.com/${encodedKey}`;
};

export const uploadMediaToS3 = async (file, folder) => {
	const configuration = getS3Configuration();
	if (!isS3StorageConfigured()) {
		throw new Error("S3 media storage is not configured.");
	}

	const extension = path.extname(file.originalname || "")
		.toLowerCase()
		.replace(/[^a-z0-9.]/g, "");
	const objectKey = `${folder}/${randomUUID()}${extension}`;
	const upload = new Upload({
		client: getS3Client(configuration),
		params: {
			Bucket: configuration.bucket,
			Key: objectKey,
			Body: createReadStream(file.path),
			ContentLength: file.size,
			ContentType: file.mimetype,
			CacheControl: "public, max-age=31536000, immutable",
		},
		leavePartsOnError: false,
	});

	await upload.done();

	return {
		publicUrl: buildS3PublicUrl(
			configuration.bucket,
			configuration.region,
			configuration.publicBaseUrl,
			objectKey,
		),
		objectKey,
	};
};
