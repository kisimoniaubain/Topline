import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  Alert,
  ActivityIndicator,
} from 'react-native';

import AppInput from '../components/AppInput';
import AppButton from '../components/AppButton';
import { useAuth } from '../context/AuthContext';

export default function EditProfileScreen({ navigation }) {
  const { user, updateProfile } = useAuth();

  const [name, setName] = useState('');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [bio, setBio] = useState('');
  const [location, setLocation] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (user) {
      setName(user.name || '');
      setUsername(user.username || '');
      setEmail(user.email || '');
      setPhone(user.phone || '');
      setBio(user.bio || '');
      setLocation(user.location || '');
    }
  }, [user]);

  const handleSave = async () => {
    if (!name.trim() || !username.trim()) {
      Alert.alert('Missing info', 'Name and username are required.');
      return;
    }

    try {
      setSaving(true);

      await updateProfile({
        name: name.trim(),
        username: username.trim(),
        email: email.trim(),
        phone: phone.trim(),
        bio: bio.trim(),
        location: location.trim(),
      });

      Alert.alert('Success', 'Your profile has been updated.');
      navigation.goBack();
    } catch (error) {
      Alert.alert(
        'Update failed',
        error.message || 'Unable to update your profile.'
      );
    } finally {
      setSaving(false);
    }
  };

  if (!user) {
    return (
      <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  return (
    <ScrollView contentContainerStyle={{ padding: 20, paddingBottom: 50 }}>
      <Text style={{ fontSize: 28, fontWeight: '700', marginBottom: 20 }}>
        Edit Profile
      </Text>

      <AppInput label="Full Name" value={name} onChangeText={setName} />
      <AppInput
        label="Username"
        value={username}
        onChangeText={setUsername}
        autoCapitalize="none"
      />
      <AppInput
        label="Email"
        value={email}
        onChangeText={setEmail}
        autoCapitalize="none"
        keyboardType="email-address"
      />
      <AppInput
        label="Phone"
        value={phone}
        onChangeText={setPhone}
        keyboardType="phone-pad"
      />
      <AppInput
        label="Bio"
        value={bio}
        onChangeText={setBio}
        placeholder="Tell people about yourself"
      />
      <AppInput
        label="Location"
        value={location}
        onChangeText={setLocation}
        placeholder="City or country"
      />

      <AppButton
        title={saving ? 'Saving...' : 'Save Changes'}
        onPress={handleSave}
        disabled={saving}
      />

      <AppButton
        title="Cancel"
        variant="outline"
        onPress={() => navigation.goBack()}
        disabled={saving}
      />
    </ScrollView>
  );
}
