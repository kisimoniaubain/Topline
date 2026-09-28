import React, { useState } from 'react';
import {
  View,
  Text,
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

export default function LoginScreen() {
  const navigation = useNavigation();

  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const { login } = useAuth();

  const handleLogin = async () => {
    if (!identifier.trim() || !password) {
      Alert.alert(
        'Missing information',
        'Please enter your email/username and password.'
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
        'Login failed',
        error.message || 'Unable to log in.'
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
          <Text style={styles.logo}>
            TOPLINE
          </Text>

          <Text style={styles.tagline}>
            Connect. Share. Stay informed.
          </Text>
        </View>

        <View style={styles.form}>
          <Text style={styles.title}>
            Welcome back
          </Text>

          <Text style={styles.subtitle}>
            Sign in to continue to Topline.
          </Text>

          <AppInput
            label="Email or Username"
            placeholder="Enter your email or username"
            value={identifier}
            onChangeText={setIdentifier}
            autoCapitalize="none"
            keyboardType="email-address"
          />

          <AppInput
            label="Password"
            placeholder="Enter your password"
            value={password}
            onChangeText={setPassword}
            secureTextEntry
          />

          <AppButton
            title={
              submitting
                ? 'Signing In...'
                : 'Sign In'
            }
            onPress={handleLogin}
            disabled={submitting}
          />

          <AppButton
            title="Create Account"
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

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F7F7F7',
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

  logo: {
    fontSize: 36,
    fontWeight: '800',
    letterSpacing: 3,
    color: '#F57F17',
  },

  tagline: {
    fontSize: 14,
    color: '#6B6B6B',
    marginTop: 8,
  },

  form: {
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#E5E5E5',
    padding: 20,
  },

  title: {
    fontSize: 28,
    fontWeight: '700',
    color: '#0A0A0A',
  },

  subtitle: {
    fontSize: 16,
    color: '#6B6B6B',
    marginTop: 4,
    marginBottom: 24,
  },
});