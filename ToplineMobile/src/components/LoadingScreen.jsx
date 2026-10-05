import React from 'react';
import {
  View,
  Text,
  Image,
  ActivityIndicator,
  StyleSheet,
} from 'react-native';
import { spacing, typography } from '../theme';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import useThemeStyles from '../theme/useThemeStyles';

const LoadingScreen = ({ message = 'Loading...' }) => {
  const styles = useThemeStyles(createStyles);
  const { colors, mode } = useTheme();
  const { t } = useLanguage();

  return (
    <View style={styles.container}>
      <Image
        source={mode === 'dark'
          ? require('../../assets/logo1.png')
          : require('../../assets/logo.png')}
        style={styles.logoImage}
        resizeMode="contain"
        accessibilityLabel="Topline"
      />

      <ActivityIndicator
        size="large"
        color={colors.primary}
        style={styles.loader}
      />

      <Text style={styles.message}>{t(message)}</Text>
    </View>
  );
};

const createStyles = (colors) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
    alignItems: 'center',
    justifyContent: 'center',
    padding: spacing.lg,
  },

  logoImage: {
    width: 170,
    height: 52,
  },

  loader: {
    marginTop: spacing.xl,
  },

  message: {
    ...typography.body,
    color: colors.textSecondary,
    marginTop: spacing.md,
  },
});

export default LoadingScreen;