import { apiRequest } from './api';

export const createPost = async ({ text, media, token }) => {
	const body = new FormData();
	body.append('text', text);

	if (media) {
		const isVideo = media.type === 'video';
		const extension = media.fileName?.split('.').pop();

		body.append('file', {
			uri: media.uri,
			name: media.fileName || `post.${extension || (isVideo ? 'mp4' : 'jpg')}`,
			type: media.mimeType || (isVideo ? 'video/mp4' : 'image/jpeg'),
		});
	}

	return apiRequest('/posts', {
		method: 'POST',
		headers: {
			Authorization: `Bearer ${token}`,
		},
		body,
	});
};

export const getPosts = async (token) =>
	apiRequest('/posts', {
		headers: token
			? { Authorization: `Bearer ${token}` }
			: {},
	});
