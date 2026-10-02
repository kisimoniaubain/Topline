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
  MapPin,
  Calendar,
  Play,
  Pause,
} from 'lucide-react-native';

import { useAuth } from '../context/AuthContext';
import { apiRequest } from '../services/api';
import { colors } from '../theme';
import VideoPlayer from '../components/VideoPlayer';
import styles from './UserProfileScreen.css';

const sampleVideos = [
  {
    id: 'sample-flower',
    title: 'Flower clip',
    source: 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4',
  },
  {
    id: 'sample-sintel',
    title: 'Sintel trailer',
    source: 'https://media.w3.org/2010/05/sintel/trailer.mp4',
  },
  {
    id: 'sample-bunny',
    title: 'Big Buck Bunny trailer',
    source: 'https://media.w3.org/2010/05/bunny/trailer.mp4',
  },
];

export default function UserProfileScreen({ navigation, route }) {
  const { user: authUser, token } = useAuth();
  const [profileUser, setProfileUser] = useState(
    route?.params?.user || authUser || null
  );
  const [loading, setLoading] = useState(
    !route?.params?.user && !!token
  );

  useEffect(() => {
    const fetchProfile = async () => {
      if (route?.params?.user) {
        setProfileUser(route.params.user);
        setLoading(false);
        return;
      }

      if (!token) {
        setProfileUser(authUser || null);
        setLoading(false);
        return;
      }

      try {
        const data = await apiRequest('/user/me', {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        setProfileUser(data.user || authUser || null);
      } catch (error) {
        console.error('Failed to load profile:', error);
        setProfileUser(authUser || null);
      } finally {
        setLoading(false);
      }
    };

    fetchProfile();
  }, [route?.params?.user, token, authUser]);

  const user =
    profileUser ||
    authUser ||
    {
      name: 'Topline User',
      username: 'toplineuser',
      profilePicture: '',
      bio: 'Connect. Share. Stay informed.',
      location: 'Kenya',
      joined: 'Joined recently',
      followers: 0,
      following: 0,
      posts: [],
    };

  const [following, setFollowing] = useState(false);
  const [activeSampleId, setActiveSampleId] = useState(null);

  const posts = user.posts || [];
  const authUserId = authUser?._id || authUser?.id;
  const profileUserId = profileUser?._id || profileUser?.id;
  const isOwnProfile =
    !route?.params?.user ||
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
            : 'Profile'}
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

            <Text style={styles.name}>
              {user.name || 'Topline User'}
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

                  <Text style={styles.detailText}>
                    {user.joined}
                  </Text>
                </View>
              ) : null}
            </View>
          </View>

          <View style={styles.stats}>
            <View style={styles.stat}>
              <Text style={styles.statNumber}>
                {posts.length}
              </Text>

              <Text style={styles.statLabel}>Posts</Text>
            </View>

            <View style={styles.stat}>
              <Text style={styles.statNumber}>
                {user.followers || 0}
              </Text>

              <Text style={styles.statLabel}>Followers</Text>
            </View>

            <View style={styles.stat}>
              <Text style={styles.statNumber}>
                {user.following || 0}
              </Text>

              <Text style={styles.statLabel}>Following</Text>
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
                  {following ? 'Following' : 'Follow'}
                </Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={styles.messageButton}
                onPress={handleMessage}
                activeOpacity={0.8}
              >
                <MessageCircle size={18} color={colors.text} />

                <Text style={styles.messageButtonText}>Message</Text>
              </TouchableOpacity>
            </View>
          ) : null}

          <View style={styles.postsHeader}>
            <Text style={styles.postsTitle}>Posts</Text>
          </View>

          {posts.length > 0 ? (
            posts.map((post, index) => (
              <View
                key={post.id || post._id || index}
                style={styles.post}
              >
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
                  {post.createdAt || 'Recently'}
                </Text>
              </View>
            ))
          ) : (
            <View style={styles.emptyPosts}>
              <Text style={styles.emptyTitle}>No posts yet</Text>

              <Text style={styles.emptyText}>
                When this user shares something, it will appear here.
              </Text>
            </View>
          )}

          {isOwnProfile && posts.length === 0 ? (
            <>
              <View style={styles.postsHeader}>
                <Text style={styles.postsTitle}>Sample videos</Text>
              </View>

              <View style={styles.videoSamples}>
                {sampleVideos.map((sample) => {
                  const isPlaying = activeSampleId === sample.id;

                  return (
                    <View key={sample.id} style={styles.sampleVideoCard}>
                      <View style={styles.sampleVideoFrame}>
                        <VideoPlayer
                          source={sample.source}
                          isActive={isPlaying}
                          loop
                        />

                        <TouchableOpacity
                          style={styles.sampleVideoControl}
                          onPress={() =>
                            setActiveSampleId(isPlaying ? null : sample.id)
                          }
                          accessibilityRole="button"
                          accessibilityLabel={
                            isPlaying ? 'Pause sample video' : 'Play sample video'
                          }
                          activeOpacity={0.8}
                        >
                          {isPlaying ? (
                            <Pause size={22} color={colors.white} fill={colors.white} />
                          ) : (
                            <Play size={22} color={colors.white} fill={colors.white} />
                          )}
                        </TouchableOpacity>
                      </View>

                      <Text style={styles.sampleVideoTitle}>
                        {sample.title}
                      </Text>
                    </View>
                  );
                })}
              </View>
            </>
          ) : null}
        </ScrollView>
      )}
    </SafeAreaView>
  );
}