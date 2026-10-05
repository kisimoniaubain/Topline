
import React, { useState } from 'react';

import {
  View,
  Text,
  SafeAreaView,
  TextInput,
  TouchableOpacity,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
  Image,
  Alert,
  ActivityIndicator,
} from 'react-native';

import {
  ArrowLeft,
  Image as ImageIcon,
  Video,
  X,
} from 'lucide-react-native';

import * as ImagePicker from 'expo-image-picker';
import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import { createPost } from '../services/postService';
import VideoPlayer from '../components/VideoPlayer';
import useThemeStyles from '../theme/useThemeStyles';
import createStyles from './CreatePostScreen.css';

export default function CreatePostScreen({ navigation }) {
  const styles = useThemeStyles(createStyles);
  const { colors } = useTheme();
  const { language, t } = useLanguage();
  const { user, token } = useAuth();
  const [postText, setPostText] = useState('');
  const [selectedMedia, setSelectedMedia] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const pickMedia = async (mediaType) => {
    try {
      const permission =
        await ImagePicker.requestMediaLibraryPermissionsAsync();

      if (!permission.granted) {
        Alert.alert(
          t('Permission required'),
          t('Allow access to your photos and videos to add media to a post.')
        );
      return;
    }

      const result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: [mediaType],
        allowsEditing: false,
        quality: 1,
      });

      if (!result.canceled && result.assets?.[0]) {
        setSelectedMedia({
          ...result.assets[0],
          type: result.assets[0].type || mediaType.slice(0, -1),
        });
      }
    } catch (error) {
      Alert.alert(t('Unable to open media'), t(error.message));
    }
  };

  const handlePost = async () => {
    const text = postText.trim();

    if (!text && !selectedMedia) {
      return;
    }

    if (!token) {
      Alert.alert(t('Sign in required'), t('Sign in before creating a post.'));
      return;
    }

    setSubmitting(true);

    try {
      await createPost({
        text,
        media: selectedMedia,
        token,
      });

      setPostText('');
      setSelectedMedia(null);
      navigation.goBack();
    } catch (error) {
      Alert.alert(t('Post failed'), t(error.message || 'Please try again.'));
    } finally {
      setSubmitting(false);
    }
  };

  const canPost = !!postText.trim() || !!selectedMedia;

  return (
    <SafeAreaView style={styles.container}>
      <KeyboardAvoidingView
        style={styles.keyboard}
        behavior={
          Platform.OS === 'ios'
            ? 'padding'
            : undefined
        }
      >
        {/* HEADER */}

        <View style={styles.header}>
          <TouchableOpacity
            style={styles.headerButton}
            onPress={() => navigation.goBack()}
            activeOpacity={0.7}
          >
            <ArrowLeft
              size={24}
              color={colors.text}
            />
          </TouchableOpacity>

          <Text style={styles.headerTitle}>
            {t('Create Post')}
          </Text>

          <TouchableOpacity
            style={[
              styles.publishButton,
              (!canPost || submitting) &&
                styles.publishButtonDisabled,
            ]}
            onPress={handlePost}
            disabled={!canPost || submitting}
            activeOpacity={0.8}
          >
            {submitting ? (
              <ActivityIndicator size="small" color={colors.white} />
            ) : (
              <Text style={styles.publishText}>{t('Post')}</Text>
            )}
          </TouchableOpacity>
        </View>

        <ScrollView
          style={styles.scrollView}
          contentContainerStyle={styles.content}
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}
        >
          {/* USER */}

          <View style={styles.userRow}>
            <View style={styles.avatar}>
              <Text style={styles.avatarText}>
                {user?.name?.charAt(0)?.toUpperCase() || 'U'}
              </Text>
            </View>

            <View>
              <Text style={styles.userName}>
                {user?.name || t('You')}
              </Text>

              <Text style={styles.visibility}>
                {t('Everyone can see this post')}
              </Text>
            </View>
          </View>

          {/* TEXT */}

          <TextInput
            style={[
              styles.textInput,
              language === 'ar' && { textAlign: 'right', writingDirection: 'rtl' },
            ]}
            placeholder={t("What's on your mind?")}
            placeholderTextColor={colors.textLight}
            value={postText}
            onChangeText={setPostText}
            multiline
            autoFocus
          />

          {/* MEDIA */}

          <View style={styles.mediaSection}>
            <Text style={styles.mediaTitle}>
              {t('Add to your post')}
            </Text>

            <View style={styles.mediaActions}>
              <TouchableOpacity
                style={styles.mediaButton}
                onPress={() => pickMedia('images')}
                disabled={submitting}
                activeOpacity={0.7}
              >
                <ImageIcon
                  size={23}
                  color={colors.primary}
                />

                <Text style={styles.mediaText}>
                  {t('Photo')}
                </Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={styles.mediaButton}
                onPress={() => pickMedia('videos')}
                disabled={submitting}
                activeOpacity={0.7}
              >
                <Video
                  size={23}
                  color={colors.primary}
                />

                <Text style={styles.mediaText}>
                  {t('Video')}
                </Text>
              </TouchableOpacity>
            </View>

            {selectedMedia ? (
              <View style={styles.mediaPreview}>
                {selectedMedia.type === 'video' ? (
                  <VideoPlayer
                    source={selectedMedia.uri}
                    isActive={!submitting}
                    loop
                  />
                ) : (
                  <Image
                    source={{ uri: selectedMedia.uri }}
                    style={styles.mediaPreviewImage}
                    resizeMode="cover"
                  />
                )}

                <View style={styles.mediaPreviewLabel}>
                  {selectedMedia.type === 'video' ? (
                    <Video size={15} color={colors.white} />
                  ) : (
                    <ImageIcon size={15} color={colors.white} />
                  )}
                  <Text style={styles.mediaPreviewText} numberOfLines={1}>
                    {selectedMedia.fileName ||
                      t(selectedMedia.type === 'video' ? 'Video selected' : 'Photo selected')}
                  </Text>
                </View>

                <TouchableOpacity
                  style={styles.removeMediaButton}
                  onPress={() => setSelectedMedia(null)}
                  accessibilityLabel={t('Remove selected media')}
                  disabled={submitting}
                >
                  <X size={18} color={colors.white} />
                </TouchableOpacity>
              </View>
            ) : null}
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}
