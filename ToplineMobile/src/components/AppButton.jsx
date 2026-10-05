import React from 'react';
import {
  TouchableOpacity,
  Text,
  StyleSheet,
  ActivityIndicator,
} from 'react-native';
import { spacing, typography } from '../theme';
import { useTheme } from '../context/ThemeContext';
import useThemeStyles from '../theme/useThemeStyles';

const AppButton = ({
  title,
  onPress,
  loading = false,
  disabled = false,
  variant = 'primary',
}) => {
  const isOutline = variant === 'outline';
  const styles = useThemeStyles(createStyles);
  const { colors } = useTheme();

  return (
    <TouchableOpacity
      style={[
        styles.button,
        isOutline ? styles.outlineButton : styles.primaryButton,
        disabled && styles.disabledButton,
      ]}
      onPress={onPress}
      disabled={disabled || loading}
      activeOpacity={0.8}
    >
      {loading ? (
        <ActivityIndicator
          color={isOutline ? colors.primary : colors.white}
        />
      ) : (
        <Text
          style={[
            styles.text,
            isOutline ? styles.outlineText : styles.primaryText,
          ]}
        >
          {title}
        </Text>
      )}
    </TouchableOpacity>
  );
};

const createStyles = (colors) => StyleSheet.create({
  button: {
    minHeight: 52,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: spacing.lg,
    marginVertical: spacing.sm,
  },

  primaryButton: {
    backgroundColor: colors.primary,
  },

  outlineButton: {
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.primary,
  },

  disabledButton: {
    opacity: 0.5,
  },

  text: {
    ...typography.button,
  },

  primaryText: {
    color: colors.textOnPrimary,
  },

  outlineText: {
    color: colors.primary,
  },
});

export default AppButton;