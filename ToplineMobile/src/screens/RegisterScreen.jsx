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

export default function RegisterScreen() {
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
        'Missing information',
        'Please fill in all required fields.'
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
        'Registration failed',
        error.message ||
          'Unable to create your account.'
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
          <Text style={styles.logo}>
            TOPLINE
          </Text>

          <Text style={styles.tagline}>
            Create your account
          </Text>
        </View>

        <View style={styles.form}>
          <Text style={styles.title}>
            Create Account
          </Text>

          <Text style={styles.subtitle}>
            Join Topline and connect with others.
          </Text>

          <AppInput
            label="Full Name"
            placeholder="Enter your full name"
            value={name}
            onChangeText={setName}
          />

          <AppInput
            label="Username"
            placeholder="Choose a username"
            value={username}
            onChangeText={setUsername}
            autoCapitalize="none"
          />

          <AppInput
            label="Email"
            placeholder="Enter your email"
            value={email}
            onChangeText={setEmail}
            autoCapitalize="none"
            keyboardType="email-address"
          />

          <AppInput
            label="Phone"
            placeholder="Enter your phone number"
            value={phone}
            onChangeText={setPhone}
            keyboardType="phone-pad"
          />

          <AppInput
            label="Date of Birth"
            placeholder="YYYY-MM-DD"
            value={dateOfBirth}
            onChangeText={setDateOfBirth}
          />

          <AppInput
            label="Password"
            placeholder="Create a password"
            value={password}
            onChangeText={setPassword}
            secureTextEntry
          />

          <AppButton
            title={
              submitting
                ? 'Creating Account...'
                : 'Create Account'
            }
            onPress={handleRegister}
            disabled={submitting}
          />

          <AppButton
            title="Already have an account? Sign In"
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
    marginBottom: 32,
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