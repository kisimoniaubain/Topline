import { apiRequest } from './api';

const login = async (identifier, password) => {
  return apiRequest('/auth/login', {
    method: 'POST',
    body: JSON.stringify({
      identifier,
      password,
    }),
  });
};

const register = async ({
  name,
  username,
  email,
  password,
  dateOfBirth,
}) => {
  return apiRequest('/auth/register', {
    method: 'POST',
    body: JSON.stringify({
      name,
      username,
      email,
      password,
      dateOfBirth,
    }),
  });
};

const sendConfirmationCode = async ({ email, phone }) => {
  return apiRequest('/auth/send-code', {
    method: 'POST',
    body: JSON.stringify({
      email,
      phone,
    }),
  });
};

export {
  login,
  register,
  sendConfirmationCode,
};