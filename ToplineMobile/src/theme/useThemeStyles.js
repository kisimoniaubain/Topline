import { useMemo } from 'react';
import { useTheme } from '../context/ThemeContext';

export default function useThemeStyles(createStyles) {
  const { colors } = useTheme();
  return useMemo(() => createStyles(colors), [colors, createStyles]);
}
