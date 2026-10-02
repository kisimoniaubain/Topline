import { Platform } from 'react-native';

const API_URL =
  Platform.OS === 'android'
    ? 'http://10.0.2.2:5000/api'
    : 'http://localhost:5000/api';

const apiRequest = async (endpoint, options = {}) => {
  const isMultipart =
    typeof FormData !== 'undefined' &&
    options.body instanceof FormData;

  const response = await fetch(`${API_URL}${endpoint}`, {
    headers: {
      ...(!isMultipart && {
        'Content-Type': 'application/json',
      }),
      ...(options.headers || {}),
    },
    ...options,
  });

  const data = await response.json();

  if (!response.ok) {
    throw new Error(data.message || 'Something went wrong');
  }

  return data;
};

export { API_URL, apiRequest };