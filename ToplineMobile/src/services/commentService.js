import { apiRequest } from './api';

export const getComments = (postId) =>
  apiRequest(`/comments/${encodeURIComponent(postId)}`);

export const addComment = (postId, text, token) =>
  apiRequest(`/comments/${encodeURIComponent(postId)}`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ text }),
  });

export const deleteComment = (commentId, token) =>
  apiRequest(`/comments/${encodeURIComponent(commentId)}`, {
    method: 'DELETE',
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
