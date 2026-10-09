const API_URL = (
  process.env.EXPO_PUBLIC_API_URL || 'https://topline-api.onrender.com/api'
).replace(/\/+$/, '');

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