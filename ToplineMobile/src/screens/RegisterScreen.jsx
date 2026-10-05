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

export default function RegisterScreen() {
  const styles = useThemeStyles(createStyles);
  const { mode } = useTheme();
  const { t } = useLanguage();
  const navigation = useNavigation();

  const { register } = useAuth();

  const [name, setName] = useState('');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [dateOfBirth, setDateOfBirth] = useState('');

  const [submitting, setSubmitting] = useState(false);

  const handleRegister = async () => {
    if (
      !name.trim() ||
      !username.trim() ||
      !email.trim() ||
      !password ||
      !dateOfBirth.trim()
    ) {
      Alert.alert(
        t('Missing information'),
        t('Please fill in all required fields.')
      );

      return;
    }

    try {
      setSubmitting(true);

      await register({
        name: name.trim(),
        username: username.trim(),
        email: email.trim(),
        phone: phone.trim(),
        password,
        dateOfBirth: dateOfBirth.trim(),
      });

      /*
       * DO NOT navigate to Home here.
       *
       * AuthContext now contains the token and user.
       * AppNavigator will automatically switch from
       * AuthNavigator to MainNavigator.
       */
    } catch (error) {
      Alert.alert(
        t('Registration failed'),
        t(error.message || 'Unable to create your account.')
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
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.logoContainer}>
          <Image
            source={mode === 'dark'
              ? require('../../assets/logo1.png')
              : require('../../assets/logo.png')}
            style={styles.logoImage}
            resizeMode="contain"
            accessibilityLabel={t('Topline')}
          />

          <Text style={styles.tagline}>
            {t('Create your account')}
          </Text>
        </View>

        <View style={styles.form}>
          <Text style={styles.title}>
            {t('Create Account')}
          </Text>

          <Text style={styles.subtitle}>
            {t('Join Topline and connect with others.')}
          </Text>

          <AppInput
            label={t('Full Name')}
            placeholder={t('Enter your full name')}
            value={name}
            onChangeText={setName}
          />

          <AppInput
            label={t('Username')}
            placeholder={t('Choose a username')}
            value={username}
            onChangeText={setUsername}
            autoCapitalize="none"
          />

          <AppInput
            label={t('Email')}
            placeholder={t('Enter your email')}
            value={email}
            onChangeText={setEmail}
            autoCapitalize="none"
            keyboardType="email-address"
          />

          <AppInput
            label={t('Phone')}
            placeholder={t('Enter your phone number')}
            value={phone}
            onChangeText={setPhone}
            keyboardType="phone-pad"
          />

          <AppInput
            label={t('Date of Birth')}
            placeholder="YYYY-MM-DD"
            value={dateOfBirth}
            onChangeText={setDateOfBirth}
          />

          <AppInput
            label={t('Password')}
            placeholder={t('Create a password')}
            value={password}
            onChangeText={setPassword}
            secureTextEntry
          />

          <AppButton
            title={
              submitting
                ? t('Creating Account...')
                : t('Create Account')
            }
            onPress={handleRegister}
            disabled={submitting}
          />

          <AppButton
            title={t('Already have an account? Sign In')}
            variant="outline"
            onPress={() =>
              navigation.navigate('Login')
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
    marginBottom: 32,
  },

  logoImage: {
    width: 164,
    height: 48,
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