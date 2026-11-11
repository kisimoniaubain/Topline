export const videoUrlPattern = /\/video\/upload\/|\.(3gp|m3u8|m4v|mkv|mov|mp4|webm)(?:[?#/]|$)/i;
export const videoMimeTypePattern = /^video(?:\/|$)/i;

export const looksLikeVideoUrl = (url) =>
	typeof url === "string" && videoUrlPattern.test(url);

export const isVideoFile = (file) =>
	videoMimeTypePattern.test(file.mimetype || "") ||
	/\.(3gp|m3u8|m4v|mkv|mov|mp4|webm)$/i.test(file.originalname || "");

export const getPostVideoUrl = (post) => {
	const mediaType = post.mediaType || post.media_type || "";
	const declaredVideoUrl = videoMimeTypePattern.test(mediaType)
		? post.mediaUrl || post.image
		: "";

	return post.video || post.videoUrl || post.video_url ||
		declaredVideoUrl ||
		[post.mediaUrl, post.image].find(looksLikeVideoUrl) ||
		"";
};
