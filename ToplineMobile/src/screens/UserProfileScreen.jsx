import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  Image,
  SafeAreaView,
  ActivityIndicator,
} from 'react-native';

import {
  ArrowLeft,
  Settings,
  UserPlus,
  UserCheck,
  MessageCircle,
  Camera,
  MapPin,
  Calendar,
  Play,
  Pause,
} from 'lucide-react-native';

import { useAuth } from '../context/AuthContext';
import { languageLocales, useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import { apiRequest } from '../services/api';
import useThemeStyles from '../theme/useThemeStyles';
import VideoPlayer from '../components/VideoPlayer';
import createStyles from './UserProfileScreen.css';

export default function UserProfileScreen({ navigation, route }) {
  const styles = useThemeStyles(createStyles);
  const { colors } = useTheme();
  const { language, t } = useLanguage();
  const { user: authUser, token } = useAuth();
  const selectedUser = route?.params?.user;
  const selectedUserId = selectedUser?._id || selectedUser?.id;
  const [profileUser, setProfileUser] = useState(
    selectedUser || authUser || null
  );
  const [loading, setLoading] = useState(
    Boolean(selectedUserId) || (!selectedUser && !!token)
  );

  useEffect(() => {
    const fetchProfile = async () => {
      if (selectedUser) {
        setProfileUser(selectedUser);
        if (!selectedUserId) {
          setLoading(false);
          return;
        }

        try {
          const data = await apiRequest(`/posts/author/${encodeURIComponent(selectedUserId)}`);
          setProfileUser({
            ...selectedUser,
            ...(data.user || {}),
            posts: data.posts || [],
          });
        } catch (error) {
          console.error('Failed to load profile posts:', error);
        } finally {
          setLoading(false);
        }
        return;
      }

      if (!token) {
        setProfileUser(authUser || null);
        setLoading(false);
        return;
      }

      try {
        const headers = {
          Authorization: `Bearer ${token}`,
        };
        const [profileData, postsData] = await Promise.all([
          apiRequest('/user/me', { headers }),
          apiRequest('/posts/mine', { headers }),
        ]);

        setProfileUser({
          ...(profileData.user || authUser || {}),
          posts: postsData.posts || [],
        });
      } catch (error) {
        console.error('Failed to load profile:', error);
        setProfileUser(authUser || null);
      } finally {
        setLoading(false);
      }
    };

    fetchProfile();
  }, [selectedUser, selectedUserId, token, authUser]);

  const user =
    profileUser ||
    authUser ||
    {
      name: 'Topline User',
      username: 'toplineuser',
      profilePicture: '',
      bio: t('Connect. Share. Stay informed.'),
      location: 'Kenya',
      joined: t('Joined recently'),
      followers: 0,
      following: 0,
      posts: [],
    };

  const [following, setFollowing] = useState(false);
  const [activeVideoId, setActiveVideoId] = useState(null);

  const posts = user.posts || [];
  const authUserId = authUser?._id || authUser?.id;
  const profileUserId = profileUser?._id || profileUser?.id;
  const isOwnProfile =
    !selectedUser ||
    Boolean(
      (authUserId &&
        profileUserId &&
        String(authUserId) === String(profileUserId)) ||
        (authUser?.username &&
          profileUser?.username &&
          authUser.username.toLowerCase() ===
            profileUser.username.toLowerCase())
    );

  const handleFollow = () => {
    setFollowing((current) => !current);
  };

  const handleMessage = () => {
    navigation.navigate('Messages', {
      user,
    });
  };

  return (
    <SafeAreaView style={styles.container}>
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

        <Text
          style={styles.headerTitle}
          numberOfLines={1}
        >
          {user.username
            ? `@${user.username}`
            : t('Profile')}
        </Text>

        <TouchableOpacity
          style={styles.headerButton}
          activeOpacity={0.7}
          onPress={() => navigation.navigate('Settings')}
        >
          <Settings
            size={24}
            color={colors.text}
          />
        </TouchableOpacity>
      </View>

      {loading ? (
        <View
          style={{
            flex: 1,
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <ActivityIndicator size="large" color={colors.primary} />
        </View>
      ) : (
        <ScrollView
          showsVerticalScrollIndicator={false}
          contentContainerStyle={styles.content}
        >
          <View style={styles.profileSection}>
            {user.profilePicture ? (
              <Image
                source={{
                  uri: user.profilePicture,
                }}
                style={styles.avatar}
              />
            ) : (
              <View style={styles.avatarPlaceholder}>
                <Text style={styles.avatarText}>
                  {user.name?.charAt(0)?.toUpperCase() || 'U'}
                </Text>
              </View>
            )}

            {isOwnProfile ? (
              <TouchableOpacity
                style={styles.editAvatarButton}
                onPress={() => navigation.navigate('EditProfile')}
                accessibilityRole="button"
                accessibilityLabel={t('Edit profile photo')}
                activeOpacity={0.8}
              >
                <Camera size={15} color={colors.primaryDark} />
                <Text style={styles.editAvatarText}>{t('Edit profile photo')}</Text>
              </TouchableOpacity>
            ) : null}

            <Text style={styles.name}>
              {user.name || t('Topline User')}
            </Text>

            <Text style={styles.username}>
              @{user.username || 'toplineuser'}
            </Text>

            {user.bio ? (
              <Text style={styles.bio}>
                {user.bio}
              </Text>
            ) : null}

            <View style={styles.details}>
              {user.location ? (
                <View style={styles.detailItem}>
                  <MapPin
                    size={15}
                    color={colors.textSecondary}
                  />

                  <Text style={styles.detailText}>
                    {user.location}
                  </Text>
                </View>
              ) : null}

              {user.joined ? (
                <View style={styles.detailItem}>
                  <Calendar
                    size={15}
                    color={colors.textSecondary}
                  />

                  <Text style={styles.detailText}>{t(user.joined)}</Text>
                </View>
              ) : null}
            </View>
          </View>

          <View style={styles.stats}>
            <View style={styles.stat}>
              <Text style={styles.statNumber}>
                {posts.length}
              </Text>

              <Text style={styles.statLabel}>{t('Posts')}</Text>
            </View>

            <View style={styles.stat}>
              <Text style={styles.statNumber}>
                {user.followers || 0}
              </Text>

              <Text style={styles.statLabel}>{t('Followers')}</Text>
            </View>

            <View style={styles.stat}>
              <Text style={styles.statNumber}>
                {user.following || 0}
              </Text>

              <Text style={styles.statLabel}>{t('Following')}</Text>
            </View>
          </View>

          {!isOwnProfile ? (
            <View style={styles.actions}>
              <TouchableOpacity
                style={[
                  styles.followButton,
                  following && styles.followingButton,
                ]}
                onPress={handleFollow}
                activeOpacity={0.8}
              >
                {following ? (
                  <UserCheck size={18} color={colors.text} />
                ) : (
                  <UserPlus size={18} color={colors.white} />
                )}

                <Text
                  style={[
                    styles.followButtonText,
                    following && styles.followingButtonText,
                  ]}
                >
                  {following ? t('Following') : t('Follow')}
                </Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={styles.messageButton}
                onPress={handleMessage}
                activeOpacity={0.8}
              >
                <MessageCircle size={18} color={colors.text} />

                <Text style={styles.messageButtonText}>{t('Message')}</Text>
              </TouchableOpacity>
            </View>
          ) : null}

          <View style={styles.postsHeader}>
            <Text style={styles.postsTitle}>{t('Posts')}</Text>
          </View>

          {posts.length > 0 ? (
            posts.map((post, index) => (
              <View
                key={post.id || post._id || index}
                style={styles.post}
              >
                {post.video ? (
                  <View style={styles.postVideoFrame}>
                    <VideoPlayer
                      source={post.video}
                      isActive={activeVideoId === String(post._id || post.id)}
                      loop
                    />
                    <TouchableOpacity
                      style={styles.postVideoControl}
                      onPress={() => {
                        const videoId = String(post._id || post.id);
                        setActiveVideoId((current) =>
                          current === videoId ? null : videoId
                        );
                      }}
                      accessibilityRole="button"
                      accessibilityLabel={
                        activeVideoId === String(post._id || post.id)
                          ? t('Pause posted video')
                          : t('Play posted video')
                      }
                      activeOpacity={0.8}
                    >
                      {activeVideoId === String(post._id || post.id) ? (
                        <Pause size={22} color={colors.white} fill={colors.white} />
                      ) : (
                        <Play size={22} color={colors.white} fill={colors.white} />
                      )}
                    </TouchableOpacity>
                  </View>
                ) : null}

                {post.image ? (
                  <Image
                    source={{ uri: post.image }}
                    style={styles.postImage}
                  />
                ) : null}

                {post.text ? (
                  <Text style={styles.postText}>
                    {post.text}
                  </Text>
                ) : null}

                <Text style={styles.postDate}>
                  {post.createdAt
                    ? new Date(post.createdAt).toLocaleDateString(languageLocales[language] || 'en')
                    : t('Recently')}
                </Text>
              </View>
            ))
          ) : (
            <View style={styles.emptyPosts}>
              <Text style={styles.emptyTitle}>{t('No posts yet')}</Text>

              <Text style={styles.emptyText}>
                {t('When this user shares something, it will appear here.')}
              </Text>
            </View>
          )}

        </ScrollView>
      )}
    </SafeAreaView>
  );
}