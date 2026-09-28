import React, { useState } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  Image,
  SafeAreaView,
} from 'react-native';

import {
  ArrowLeft,
  MoreHorizontal,
  UserPlus,
  UserCheck,
  MessageCircle,
  MapPin,
  Calendar,
} from 'lucide-react-native';

import { colors } from '../theme';
import styles from './UserProfileScreen.css';

export default function UserProfileScreen({ navigation, route }) {
  const user = route?.params?.user || {
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

  const posts = user.posts || [];

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
        >
          <MoreHorizontal
            size={24}
            color={colors.text}
          />
        </TouchableOpacity>
      </View>

      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.content}
      >
        {/* PROFILE INFO */}
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

          {/* DETAILS */}
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

        {/* STATS */}
        <View style={styles.stats}>
          <View style={styles.stat}>
            <Text style={styles.statNumber}>
              {posts.length}
            </Text>

            <Text style={styles.statLabel}>
              Posts
            </Text>
          </View>

          <View style={styles.stat}>
            <Text style={styles.statNumber}>
              {user.followers || 0}
            </Text>

            <Text style={styles.statLabel}>
              Followers
            </Text>
          </View>

          <View style={styles.stat}>
            <Text style={styles.statNumber}>
              {user.following || 0}
            </Text>

            <Text style={styles.statLabel}>
              Following
            </Text>
          </View>
        </View>

        {/* ACTIONS */}
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
              <UserCheck
                size={18}
                color={colors.text}
              />
            ) : (
              <UserPlus
                size={18}
                color={colors.white}
              />
            )}

            <Text
              style={[
                styles.followButtonText,
                following &&
                  styles.followingButtonText,
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
            <MessageCircle
              size={18}
              color={colors.text}
            />

            <Text style={styles.messageButtonText}>
              Message
            </Text>
          </TouchableOpacity>
        </View>

        {/* POSTS TITLE */}
        <View style={styles.postsHeader}>
          <Text style={styles.postsTitle}>
            Posts
          </Text>
        </View>

        {/* POSTS */}
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
            <Text style={styles.emptyTitle}>
              No posts yet
            </Text>

            <Text style={styles.emptyText}>
              When this user shares something, it
              will appear here.
            </Text>
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}