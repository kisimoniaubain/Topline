
import React, { useState } from 'react';
import {
  View,
  Text,
  ScrollView,
  TextInput,
  TouchableOpacity,
  SafeAreaView,
  Image,
  useColorScheme,
} from 'react-native';

import {
  Heart,
  MessageCircle,
  Share2,
  Plus,
  MoreHorizontal,
  Image as ImageIcon,
  Video,
  Home,
  Users,
  Bell,
  Menu,
} from 'lucide-react-native';

import { colors } from '../theme';
import styles from './HomeScreen.css';

const stories = [
  { id: 1, name: 'Your Story', own: true },
  { id: 2, name: 'Sarah' },
  { id: 3, name: 'David' },
  { id: 4, name: 'Mary' },
  { id: 5, name: 'Alex' },
];

const initialPosts = [
  {
    id: 1,
    name: 'John Doe',
    username: 'johndoe',
    time: '2h',
    text: 'Had a great day today! Sometimes the simplest moments are the best ones. 🧡',
    likes: 245,
    comments: 1,
    shares: 12,
    liked: false,
  },
  {
    id: 2,
    name: 'Sarah',
    username: 'sarah',
    time: '4h',
    text: 'Beautiful day to connect with friends and share positive moments.',
    likes: 128,
    comments: 8,
    shares: 5,
    liked: false,
  },
];

export default function HomeScreen({ navigation }) {
  const [postText, setPostText] = useState('');
  const [posts, setPosts] = useState(initialPosts);

  const colorScheme = useColorScheme();
  const isDark = colorScheme === 'dark';

  const logo = isDark
    ? require('../../assets/logo1.png')
    : require('../../assets/logo.png');

  const toggleLike = (id) => {
    setPosts((currentPosts) =>
      currentPosts.map((post) =>
        post.id === id
          ? {
              ...post,
              liked: !post.liked,
              likes: post.liked
                ? post.likes - 1
                : post.likes + 1,
            }
          : post
      )
    );
  };

  const handlePost = () => {
    if (!postText.trim()) {
      return;
    }

    const newPost = {
      id: Date.now(),
      name: 'You',
      username: 'you',
      time: 'now',
      text: postText.trim(),
      likes: 0,
      comments: 0,
      shares: 0,
      liked: false,
    };

    setPosts((currentPosts) => [newPost, ...currentPosts]);
    setPostText('');
  };

  return (
    <SafeAreaView
      style={[
        styles.safeArea,
        {
          backgroundColor: isDark
            ? colors.black
            : colors.background,
        },
      ]}
    >
      <View
        style={[
          styles.container,
          {
            backgroundColor: isDark
              ? colors.black
              : colors.background,
          },
        ]}
      >

        {/* ================= HEADER ================= */}

        <View
          style={[
            styles.header,
            {
              backgroundColor: isDark
                ? colors.black
                : colors.surface,
              borderBottomColor: isDark
                ? colors.borderDark
                : colors.border,
            },
          ]}
        >
          <Image
            source={logo}
            style={styles.logo}
            resizeMode="contain"
          />

          <View style={styles.headerActions}>
{/* CREATE */}
<TouchableOpacity
  style={styles.createButton}
  onPress={() => navigation.navigate('CreatePost')}
  activeOpacity={0.8}
>
              <MessageCircle
                size={22}
                color={
                  isDark
                    ? colors.white
                    : colors.black
                }
              />
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.avatar}
              activeOpacity={0.8}
            >
              <Text style={styles.avatarText}>U</Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* ================= SCROLLABLE CONTENT ================= */}

        <ScrollView
          style={styles.scrollView}
          showsVerticalScrollIndicator={false}
          contentContainerStyle={styles.content}
        >

          {/* ================= STORIES ================= */}

          <View
            style={[
              styles.section,
              {
                backgroundColor: isDark
                  ? colors.black
                  : colors.surface,
              },
            ]}
          >
            <View style={styles.sectionHeader}>
              <Text
                style={[
                  styles.sectionTitle,
                  {
                    color: isDark
                      ? colors.white
                      : colors.text,
                  },
                ]}
              >
                Stories
              </Text>

              <TouchableOpacity activeOpacity={0.7}>
                <Text style={styles.seeAll}>
                  See all
                </Text>
              </TouchableOpacity>
            </View>

            <ScrollView
              horizontal
              showsHorizontalScrollIndicator={false}
              contentContainerStyle={styles.storiesRow}
            >
              {stories.map((story) => (
                <TouchableOpacity
                  key={story.id}
                  style={styles.story}
                  activeOpacity={0.8}
                >
                  <View
                    style={[
                      styles.storyCircle,
                      story.own && styles.ownStoryCircle,
                    ]}
                  >
                    {story.own ? (
                      <View style={styles.plusCircle}>
                        <Plus
                          size={20}
                          color={colors.white}
                        />
                      </View>
                    ) : (
                      <Text style={styles.storyInitial}>
                        {story.name.charAt(0)}
                      </Text>
                    )}
                  </View>

                  <Text
                    style={[
                      styles.storyName,
                      {
                        color: isDark
                          ? colors.white
                          : colors.text,
                      },
                    ]}
                    numberOfLines={1}
                  >
                    {story.name}
                  </Text>
                </TouchableOpacity>
              ))}
            </ScrollView>
          </View>

          {/* ================= CREATE POST ================= */}

          <View
            style={[
              styles.composer,
              {
                backgroundColor: isDark
                  ? '#111111'
                  : colors.surface,
                borderColor: isDark
                  ? colors.borderDark
                  : colors.border,
              },
            ]}
          >
            <View style={styles.composerTop}>
              <View style={styles.avatar}>
                <Text style={styles.avatarText}>U</Text>
              </View>

              <TextInput
                style={[
                  styles.input,
                  {
                    backgroundColor: isDark
                      ? '#1A1A1A'
                      : colors.background,
                    color: isDark
                      ? colors.white
                      : colors.text,
                  },
                ]}
                placeholder="What's on your mind?"
                placeholderTextColor={colors.textLight}
                value={postText}
                onChangeText={setPostText}
                multiline
              />
            </View>

            <View
              style={[
                styles.composerDivider,
                {
                  backgroundColor: isDark
                    ? colors.borderDark
                    : colors.border,
                },
              ]}
            />

            <View style={styles.composerActions}>
              <TouchableOpacity
                style={styles.composerAction}
                activeOpacity={0.7}
              >
                <ImageIcon
                  size={20}
                  color={colors.primary}
                />

                <Text style={styles.actionText}>
                  Photo
                </Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={styles.composerAction}
                activeOpacity={0.7}
              >
                <Video
                  size={20}
                  color={colors.primary}
                />

                <Text style={styles.actionText}>
                  Video
                </Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={[
                  styles.postButton,
                  !postText.trim() &&
                    styles.postButtonDisabled,
                ]}
                onPress={handlePost}
                disabled={!postText.trim()}
                activeOpacity={0.8}
              >
                <Text style={styles.postButtonText}>
                  Post
                </Text>
              </TouchableOpacity>
            </View>
          </View>

          {/* ================= POSTS ================= */}

          <View style={styles.postsSection}>
            <Text
              style={[
                styles.sectionTitle,
                styles.postsTitle,
                {
                  color: isDark
                    ? colors.white
                    : colors.text,
                },
              ]}
            >
              Posts
            </Text>

            {posts.map((post) => (
              <View
                key={post.id}
                style={[
                  styles.postCard,
                  {
                    backgroundColor: isDark
                      ? '#111111'
                      : colors.surface,
                    borderColor: isDark
                      ? colors.borderDark
                      : colors.border,
                  },
                ]}
              >

                {/* POST HEADER */}

                <View style={styles.postHeader}>
                  <View style={styles.postUser}>
                    <View style={styles.postAvatar}>
                      <Text style={styles.avatarText}>
                        {post.name.charAt(0)}
                      </Text>
                    </View>

                    <View>
                      <Text
                        style={[
                          styles.postName,
                          {
                            color: isDark
                              ? colors.white
                              : colors.text,
                          },
                        ]}
                      >
                        {post.name}
                      </Text>

                      <Text style={styles.postMeta}>
                        @{post.username} · {post.time}
                      </Text>
                    </View>
                  </View>

                  <TouchableOpacity
                    activeOpacity={0.7}
                  >
                    <MoreHorizontal
                      size={22}
                      color={colors.textSecondary}
                    />
                  </TouchableOpacity>
                </View>

                {/* POST CONTENT */}

                <Text
                  style={[
                    styles.postText,
                    {
                      color: isDark
                        ? colors.white
                        : colors.text,
                    },
                  ]}
                >
                  {post.text}
                </Text>

                {/* POST STATISTICS */}

                <View style={styles.postStats}>
                  <Text style={styles.statText}>
                    {post.likes} likes
                  </Text>

                  <View style={styles.statsRight}>
                    <Text style={styles.statText}>
                      {post.comments} comments
                    </Text>

                    <Text style={styles.statText}>
                      {post.shares} shares
                    </Text>
                  </View>
                </View>

                {/* DIVIDER */}

                <View
                  style={[
                    styles.postDivider,
                    {
                      backgroundColor: isDark
                        ? colors.borderDark
                        : colors.border,
                    },
                  ]}
                />

                {/* POST ACTIONS */}

                <View style={styles.postActions}>
                  <TouchableOpacity
                    style={styles.postAction}
                    onPress={() => toggleLike(post.id)}
                    activeOpacity={0.7}
                  >
                    <Heart
                      size={21}
                      color={
                        post.liked
                          ? colors.primary
                          : colors.textSecondary
                      }
                      fill={
                        post.liked
                          ? colors.primary
                          : 'transparent'
                      }
                    />

                    <Text
                      style={[
                        styles.postActionText,
                        post.liked &&
                          styles.likedText,
                      ]}
                    >
                      Like
                    </Text>
                  </TouchableOpacity>

                  <TouchableOpacity
                    style={styles.postAction}
                    activeOpacity={0.7}
                  >
                    <MessageCircle
                      size={21}
                      color={colors.textSecondary}
                    />

                    <Text style={styles.postActionText}>
                      Comment
                    </Text>
                  </TouchableOpacity>

                  <TouchableOpacity
                    style={styles.postAction}
                    activeOpacity={0.7}
                  >
                    <Share2
                      size={21}
                      color={colors.textSecondary}
                    />

                    <Text style={styles.postActionText}>
                      Share
                    </Text>
                  </TouchableOpacity>
                </View>
              </View>
            ))}
          </View>

        </ScrollView>

        {/* ================= BOTTOM NAVIGATION ================= */}

        <View
          style={[
            styles.bottomNav,
            {
              backgroundColor: isDark
                ? colors.black
                : colors.surface,
              borderTopColor: isDark
                ? colors.borderDark
                : colors.border,
            },
          ]}
        >

          {/* HOME */}

          <TouchableOpacity
            style={styles.navItem}
            activeOpacity={0.7}
          >
            <Home
              size={23}
              color={colors.primary}
              fill={colors.primary}
            />

            <Text style={styles.navTextActive}>
              Home
            </Text>
          </TouchableOpacity>

          {/* FRIENDS */}

          <TouchableOpacity
            style={styles.navItem}
            onPress={() => navigation.navigate('Friends')}
            activeOpacity={0.7}
          >
            <Users
              size={23}
              color={colors.textSecondary}
            />

            <Text style={styles.navText}>
              Friends
            </Text>
          </TouchableOpacity>

          {/* CREATE */}

          <TouchableOpacity
            style={styles.createButton}
            activeOpacity={0.8}
          >
            <Plus
              size={27}
              color={colors.white}
              strokeWidth={2.5}
            />
          </TouchableOpacity>

          {/* ALERTS */}

          <TouchableOpacity
            style={styles.navItem}
            activeOpacity={0.7}
          >
            <Bell
              size={23}
              color={colors.textSecondary}
            />

            <Text style={styles.navText}>
              Alerts
            </Text>
          </TouchableOpacity>

          {/* MENU */}

          <TouchableOpacity
            style={styles.navItem}
            activeOpacity={0.7}
          >
            <Menu
              size={23}
              color={colors.textSecondary}
            />

            <Text style={styles.navText}>
              Menu
            </Text>
          </TouchableOpacity>

        </View>
      </View>
    </SafeAreaView>
  );
}