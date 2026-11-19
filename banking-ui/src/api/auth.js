import api from './axios';

export const loginApi = async (email, password) => {
  const response = await api.post('/api/auth/login', { email, password });
  return response.data;
};

export const registerApi = async ({ fullName, email, password }) => {
  const response = await api.post('/api/auth/register', { fullName, email, password });
  return response.data;
};
