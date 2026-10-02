
import React, {
  createContext,
  useContext,
  useEffect,
  useState,
} from 'react';

import AsyncStorage from '@react-native-async-storage/async-storage';

import { apiRequest } from '../services/api';

const AuthContext = createContext(null);

const TOKEN_KEY = 'topline_token';
const USER_KEY = 'topline_user';

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);

  // =========================
  // LOGIN
  // =========================

  const login = async (identifier, password) => {
    const data = await apiRequest('/auth/login', {
      method: 'POST',
      body: JSON.stringify({
        identifier,
        password,
      }),
    });

    await AsyncStorage.setItem(
      TOKEN_KEY,
      data.token
    );

    await AsyncStorage.setItem(
      USER_KEY,
      JSON.stringify(data.user)
    );

    setToken(data.token);
    setUser(data.user);

    return data;
  };

  // =========================
  // REGISTER
  // =========================

  const register = async (userData) => {
    const data = await apiRequest('/auth/register', {
      method: 'POST',
      body: JSON.stringify(userData),
    });

    await AsyncStorage.setItem(
      TOKEN_KEY,
      data.token
    );

    await AsyncStorage.setItem(
      USER_KEY,
      JSON.stringify(data.user)
    );

    setToken(data.token);
    setUser(data.user);

    return data;
  };

  // =========================
  // UPDATE PROFILE
  // =========================

  const updateProfile = async (updates) => {
    if (!token) {
      throw new Error('You must be logged in to update your profile.');
    }

    const data = await apiRequest('/user/me', {
      method: 'PUT',
      headers: {
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(updates),
    });

    if (data.user) {
      setUser(data.user);
      await AsyncStorage.setItem(
        USER_KEY,
        JSON.stringify(data.user)
      );
    }

    return data;
  };

  // =========================
  // LOGOUT
  // =========================

  const logout = async () => {
    await AsyncStorage.multiRemove([
      TOKEN_KEY,
      USER_KEY,
    ]);

    setToken(null);
    setUser(null);
  };

  // =========================
  // LOAD SAVED AUTH
  // =========================

  const loadStoredAuth = async () => {
    try {
      const storedToken =
        await AsyncStorage.getItem(TOKEN_KEY);

      const storedUser =
        await AsyncStorage.getItem(USER_KEY);

      if (!storedToken) {
        return;
      }

      setToken(storedToken);

      if (storedUser) {
        setUser(JSON.parse(storedUser));
      }

      try {
        const data = await apiRequest('/user/me', {
          headers: {
            Authorization: `Bearer ${storedToken}`,
          },
        });

        if (data.user) {
          setUser(data.user);

          await AsyncStorage.setItem(
            USER_KEY,
            JSON.stringify(data.user)
          );
        }
      } catch (error) {
        await AsyncStorage.multiRemove([
          TOKEN_KEY,
          USER_KEY,
        ]);

        setToken(null);
        setUser(null);
      }
    } catch (error) {
      console.error(
        'Failed to load authentication:',
        error
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadStoredAuth();
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        loading,
        isLoggedIn: !!token && !!user,
        login,
        register,
        updateProfile,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error(
      'useAuth must be used inside AuthProvider'
    );
  }

  return context;
}
