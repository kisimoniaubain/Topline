import React from 'react';
import { StatusBar, View } from 'react-native';

import AppNavigator from './src/navigation/AppNavigator';
import { AuthProvider } from './src/context/AuthContext';
import { ThemeProvider, useTheme } from './src/context/ThemeContext';
import { LanguageProvider, useLanguage } from './src/context/LanguageContext';

function AppContent() {
  const { mode, colors, ready } = useTheme();
  const { ready: languageReady, language } = useLanguage();

  if (!ready || !languageReady) return null;

  return (
    <View
      style={{
        flex: 1,
        backgroundColor: colors.background,
        direction: language === 'ar' ? 'rtl' : 'ltr',
      }}
    >
      <StatusBar
        barStyle={mode === 'dark' ? 'light-content' : 'dark-content'}
        backgroundColor={colors.background}
      />
      <AuthProvider>
        <AppNavigator />
      </AuthProvider>
    </View>
  );
}

export default function App() {
  return (
    <ThemeProvider>
      <LanguageProvider>
        <AppContent />
      </LanguageProvider>
    </ThemeProvider>
  );
}