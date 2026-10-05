import React from 'react';
import { DarkTheme, DefaultTheme, NavigationContainer } from '@react-navigation/native';

import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';

import AuthNavigator from './AuthNavigator';
import MainNavigator from './MainNavigator';

export default function AppNavigator() {
  const { isLoggedIn, loading } = useAuth();
  const { mode, colors } = useTheme();

  if (loading) {
    return null;
  }

  const baseTheme = mode === 'dark' ? DarkTheme : DefaultTheme;
  const navigationTheme = {
    ...baseTheme,
    colors: {
      ...baseTheme.colors,
      primary: colors.primary,
      background: colors.background,
      card: colors.surface,
      text: colors.text,
      border: colors.border,
      notification: colors.primary,
    },
  };

  return (
    <NavigationContainer theme={navigationTheme}>
      {isLoggedIn ? (
        <MainNavigator />
      ) : (
        <AuthNavigator />
      )}
    </NavigationContainer>
  );
}