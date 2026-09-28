import React from 'react';
import {
  View,
  Text,
  ActivityIndicator,
  StyleSheet,
} from 'react-native';
import { colors, spacing, typography } from '../theme';

const LoadingScreen = ({ message = 'Loading...' }) => {
  return (
    <View style={styles.container}>
      <Text style={styles.logo}>TOPLINE</Text>

      <ActivityIndicator
        size="large"
        color={colors.primary}
        style={styles.loader}
      />

      <Text style={styles.message}>{message}</Text>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
    alignItems: 'center',
    justifyContent: 'center',
    padding: spacing.lg,
  },

  logo: {
    fontSize: 32,
    fontWeight: '800',
    letterSpacing: 2,
    color: colors.primary,
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