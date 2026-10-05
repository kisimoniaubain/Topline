import React from 'react';
import {
  View,
  Text,
  TextInput,
  StyleSheet,
} from 'react-native';
import { spacing, typography } from '../theme';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import useThemeStyles from '../theme/useThemeStyles';

const AppInput = ({
  label,
  placeholder,
  value,
  onChangeText,
  secureTextEntry = false,
  keyboardType = 'default',
  autoCapitalize = 'none',
  error,
}) => {
  const styles = useThemeStyles(createStyles);
  const { colors } = useTheme();
  const { language } = useLanguage();

  return (
    <View style={styles.container}>
      {label && <Text style={styles.label}>{label}</Text>}

      <TextInput
        style={[
          styles.input,
          error && styles.errorInput,
          language === 'ar' && { textAlign: 'right', writingDirection: 'rtl' },
        ]}
        placeholder={placeholder}
        placeholderTextColor={colors.textLight}
        value={value}
        onChangeText={onChangeText}
        secureTextEntry={secureTextEntry}
        keyboardType={keyboardType}
        autoCapitalize={autoCapitalize}
      />

      {error && <Text style={styles.errorText}>{error}</Text>}
    </View>
  );
};

const createStyles = (colors) => StyleSheet.create({
  container: {
    marginBottom: spacing.md,
  },

  label: {
    ...typography.bodyMedium,
    color: colors.text,
    marginBottom: spacing.sm,
  },

  input: {
    height: 52,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
    backgroundColor: colors.inputBackground,
    paddingHorizontal: spacing.md,
    color: colors.text,
    fontSize: 16,
  },

  errorInput: {
    borderColor: colors.error,
  },

  errorText: {
    color: colors.error,
    fontSize: 12,
    marginTop: spacing.xs,
  },
});

export default AppInput;