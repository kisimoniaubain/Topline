import React, { useState } from 'react';
import {
  View,
  Text,
  Image,
  ScrollView,
  TouchableOpacity,
  Alert,
  ActivityIndicator,
} from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import { Camera, UserRound } from 'lucide-react-native';

import AppInput from '../components/AppInput';
import AppButton from '../components/AppButton';
import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';

export default function EditProfileScreen({ navigation }) {
  const { user, updateProfile, updateAvatar } = useAuth();
  const { colors } = useTheme();
  const { t } = useLanguage();

  const [name, setName] = useState(user?.name || '');
  const [username, setUsername] = useState(user?.username || '');
  const [email, setEmail] = useState(user?.email || '');
  const [phone, setPhone] = useState(user?.phone || '');
  const [bio, setBio] = useState(user?.bio || '');
  const [location, setLocation] = useState(user?.location || '');
  const [profilePicture, setProfilePicture] = useState(user?.profilePicture || '');
  const [profilePictureMimeType, setProfilePictureMimeType] = useState('image/jpeg');
  const [saving, setSaving] = useState(false);

  const handlePickAvatar = async () => {
    try {
      const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
      if (!permission.granted) {
        Alert.alert(t('Permission required'), t('Allow access to photos to choose a profile picture.'));
        return;
      }

      const result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: ['images'],
        allowsEditing: true,
        aspect: [1, 1],
        quality: 0.8,
      });

      if (!result.canceled) {
        setProfilePicture(result.assets[0].uri);
        setProfilePictureMimeType(result.assets[0].mimeType || 'image/jpeg');
      }
    } catch (error) {
      Alert.alert(t('Photo unavailable'), t(error.message || 'Unable to open your photos.'));
    }
  };

  const handleSave = async () => {
    if (!name.trim() || !username.trim()) {
      Alert.alert(t('Missing info'), t('Name and username are required.'));
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

      if (profilePicture && profilePicture !== user.profilePicture) {
        await updateAvatar(profilePicture, profilePictureMimeType);
      }

      Alert.alert(t('Success'), t('Your profile has been updated.'));
      navigation.goBack();
    } catch (error) {
      Alert.alert(
        t('Update failed'),
        t(error.message || 'Unable to update your profile.')
      );
    } finally {
      setSaving(false);
    }
  };

  if (!user) {
    return (
      <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.background }}>
        <ActivityIndicator size="large" color={colors.primary} />
      </View>
    );
  }

  return (
    <ScrollView
      style={{ backgroundColor: colors.background }}
      contentContainerStyle={{ padding: 20, paddingBottom: 50 }}
    >
      <Text style={{ fontSize: 28, fontWeight: '700', marginBottom: 20, color: colors.text }}>
        {t('Edit Profile')}
      </Text>

      <TouchableOpacity
        onPress={handlePickAvatar}
        accessibilityRole="button"
        accessibilityLabel={t('Choose or change profile photo')}
        activeOpacity={0.8}
        style={{ alignSelf: 'center', alignItems: 'center', marginBottom: 24 }}
      >
        {profilePicture ? (
          <Image
            source={{ uri: profilePicture }}
            style={{ width: 104, height: 104, borderRadius: 52, marginBottom: 8 }}
          />
        ) : (
          <View
            style={{
              width: 104,
              height: 104,
              borderRadius: 52,
              marginBottom: 8,
              alignItems: 'center',
              justifyContent: 'center',
              backgroundColor: colors.primary,
            }}
          >
            <UserRound size={42} color={colors.textOnPrimary} />
          </View>
        )}
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
          <Camera size={16} color={colors.primaryDark} />
          <Text style={{ color: colors.primaryDark, fontWeight: '600' }}>
            {t(profilePicture ? 'Change profile photo' : 'Add profile photo')}
          </Text>
        </View>
      </TouchableOpacity>

      <AppInput label={t('Full Name')} value={name} onChangeText={setName} />
      <AppInput
        label={t('Username')}
        value={username}
        onChangeText={setUsername}
        autoCapitalize="none"
      />
      <AppInput
        label={t('Email')}
        value={email}
        onChangeText={setEmail}
        autoCapitalize="none"
        keyboardType="email-address"
      />
      <AppInput
        label={t('Phone')}
        value={phone}
        onChangeText={setPhone}
        keyboardType="phone-pad"
      />
      <AppInput
        label={t('Bio')}
        value={bio}
        onChangeText={setBio}
        placeholder={t('Tell people about yourself')}
      />
      <AppInput
        label={t('Location')}
        value={location}
        onChangeText={setLocation}
        placeholder={t('City or country')}
      />

      <AppButton
        title={t(saving ? 'Saving...' : 'Save Changes')}
        onPress={handleSave}
        disabled={saving}
      />

      <AppButton
        title={t('Cancel')}
        variant="outline"
        onPress={() => navigation.goBack()}
        disabled={saving}
      />
    </ScrollView>
  );
}
