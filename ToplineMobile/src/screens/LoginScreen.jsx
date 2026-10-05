import React, { useState } from 'react';
import {
  View,
  Text,
  Image,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  Alert,
} from 'react-native';

import { useNavigation } from '@react-navigation/native';

import AppInput from '../components/AppInput';
import AppButton from '../components/AppButton';

import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import useThemeStyles from '../theme/useThemeStyles';

export default function LoginScreen() {
  const styles = useThemeStyles(createStyles);
  const { mode } = useTheme();
  const { t } = useLanguage();
  const navigation = useNavigation();

  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const { login } = useAuth();

  const handleLogin = async () => {
    if (!identifier.trim() || !password) {
      Alert.alert(
        t('Missing information'),
        t('Please enter your email/username and password.')
      );
      return;
    }

    try {
      setSubmitting(true);

      await login(
        identifier.trim(),
        password
      );
    } catch (error) {
      Alert.alert(
        t('Login failed'),
        t(error.message || 'Unable to log in.')
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={
        Platform.OS === 'ios'
          ? 'padding'
          : undefined
      }
    >
      <ScrollView
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
      >
        <View style={styles.logoContainer}>
          <Image
            source={mode === 'dark'
              ? require('../../assets/icon1.png')
              : require('../../assets/icon.png')}
            style={styles.logoImage}
            resizeMode="contain"
            accessibilityLabel={t('Topline app icon')}
          />

          <Text style={styles.tagline}>
            {t('Connect. Share. Stay informed.')}
          </Text>
        </View>

        <View style={styles.form}>
          <Text style={styles.title}>
            {t('Welcome back')}
          </Text>

          <Text style={styles.subtitle}>
            {t('Sign in to continue to Topline.')}
          </Text>

          <AppInput
            label={t('Email or Username')}
            placeholder={t('Enter your email or username')}
            value={identifier}
            onChangeText={setIdentifier}
            autoCapitalize="none"
            keyboardType="email-address"
          />

          <AppInput
            label={t('Password')}
            placeholder={t('Enter your password')}
            value={password}
            onChangeText={setPassword}
            secureTextEntry
          />

          <AppButton
            title={
              submitting
                ? t('Signing In...')
                : t('Sign In')
            }
            onPress={handleLogin}
            disabled={submitting}
          />

          <AppButton
            title={t('Create Account')}
            variant="outline"
            onPress={() =>
              navigation.navigate('Register')
            }
            disabled={submitting}
          />
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const createStyles = (colors) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },

  content: {
    flexGrow: 1,
    justifyContent: 'center',
    padding: 24,
  },

  logoContainer: {
    alignItems: 'center',
    marginBottom: 48,
  },

  logoImage: {
    width: 112,
    height: 112,
  },

  tagline: {
    fontSize: 14,
    color: colors.textSecondary,
    marginTop: 8,
  },

  form: {
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    padding: 20,
  },

  title: {
    fontSize: 28,
    fontWeight: '700',
    color: colors.text,
  },

  subtitle: {
    fontSize: 16,
    color: colors.textSecondary,
    marginTop: 4,
    marginBottom: 24,
  },
});
