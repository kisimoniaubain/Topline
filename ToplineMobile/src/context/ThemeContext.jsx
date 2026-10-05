import React, {
  createContext,
  useContext,
  useEffect,
  useState,
} from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';

import { darkColors, lightColors } from '../theme/colors';

const ThemeContext = createContext(null);
const THEME_KEY = 'topline_theme';

export function ThemeProvider({ children }) {
  const [mode, setMode] = useState('light');
  const [ready, setReady] = useState(false);

  useEffect(() => {
    AsyncStorage.getItem(THEME_KEY)
      .then((savedMode) => {
        if (savedMode === 'dark' || savedMode === 'light') {
          setMode(savedMode);
        }
      })
      .catch((error) => {
        console.warn('Could not load saved theme:', error);
      })
      .finally(() => setReady(true));
  }, []);

  const updateMode = async (nextMode) => {
    const validMode = nextMode === 'dark' ? 'dark' : 'light';
    setMode(validMode);
    try {
      await AsyncStorage.setItem(THEME_KEY, validMode);
    } catch (error) {
      console.warn('Could not save theme:', error);
    }
  };

  const toggleTheme = () => updateMode(mode === 'dark' ? 'light' : 'dark');
  const colors = mode === 'dark' ? darkColors : lightColors;
  const value = { mode, isDark: mode === 'dark', colors, ready, setMode: updateMode, toggleTheme };

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used inside ThemeProvider');
  }
  return context;
}
