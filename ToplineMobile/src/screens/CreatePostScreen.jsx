
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
} from 'react-native';

import {
  ArrowLeft,
  Image as ImageIcon,
  Video,
} from 'lucide-react-native';

import { colors } from '../theme';
import styles from './CreatePostScreen.css';

export default function CreatePostScreen({ navigation }) {
  const [postText, setPostText] = useState('');

  const handlePost = () => {
    if (!postText.trim()) {
      return;
    }

    // Temporary local action.
    // We will connect this to MongoDB later.
    setPostText('');

    navigation.goBack();
  };

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
            Create Post
          </Text>

          <TouchableOpacity
            style={[
              styles.publishButton,
              !postText.trim() &&
                styles.publishButtonDisabled,
            ]}
            onPress={handlePost}
            disabled={!postText.trim()}
            activeOpacity={0.8}
          >
            <Text style={styles.publishText}>
              Post
            </Text>
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
                U
              </Text>
            </View>

            <View>
              <Text style={styles.userName}>
                You
              </Text>

              <Text style={styles.visibility}>
                Everyone can see this post
              </Text>
            </View>
          </View>

          {/* TEXT */}

          <TextInput
            style={styles.textInput}
            placeholder="What's on your mind?"
            placeholderTextColor={colors.textLight}
            value={postText}
            onChangeText={setPostText}
            multiline
            autoFocus
          />

          {/* MEDIA */}

          <View style={styles.mediaSection}>
            <Text style={styles.mediaTitle}>
              Add to your post
            </Text>

            <View style={styles.mediaActions}>
              <TouchableOpacity
                style={styles.mediaButton}
                activeOpacity={0.7}
              >
                <ImageIcon
                  size={23}
                  color={colors.primary}
                />

                <Text style={styles.mediaText}>
                  Photo
                </Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={styles.mediaButton}
                activeOpacity={0.7}
              >
                <Video
                  size={23}
                  color={colors.primary}
                />

                <Text style={styles.mediaText}>
                  Video
                </Text>
              </TouchableOpacity>
            </View>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}
