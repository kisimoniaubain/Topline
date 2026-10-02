import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  SafeAreaView,
  TouchableOpacity,
  Dimensions,
  FlatList,
  Image,
} from 'react-native';

import {
  Heart,
  MessageCircle,
  Bell,
  Share2,
  UserPlus,
  Home,
  Users,
  Plus,
  User,
  Music2,
} from 'lucide-react-native';

import VideoPlayer from '../components/VideoPlayer';
import { useAuth } from '../context/AuthContext';
import { getPosts } from '../services/postService';
import styles from './HomeScreen.css';

const { height: SCREEN_HEIGHT, width: SCREEN_WIDTH } =
  Dimensions.get('window');

const ORANGE = '#F57F17';
const WHITE = '#FFFFFF';

const videos = [
  {
    id: '1',
    video:
      'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4',
    username: '@topline',
    caption:
      'A sample clip for testing video playback.',
    music: 'MP4 sample',
    likes: 245,
    comments: 18,
    shares: 12,
    liked: false,
    following: false,
  },
  {
    id: '2',
    video:
      'https://media.w3.org/2010/05/sintel/trailer.mp4',
    username: '@sintel',
    caption:
      'Animated trailer sample.',
    music: 'MP4 sample',
    likes: 128,
    comments: 8,
    shares: 5,
    liked: false,
    following: false,
  },
  {
    id: '3',
    video:
      'https://media.w3.org/2010/05/bunny/trailer.mp4',
    username: '@bigbuckbunny',
    caption:
      'A short animated sample clip.',
    music: 'MP4 sample',
    likes: 532,
    comments: 42,
    shares: 29,
    liked: false,
    following: false,
  },
];

export default function HomeScreen({ navigation }) {
  const { token } = useAuth();

  const [activeIndex, setActiveIndex] = useState(0);
  const [feed, setFeed] = useState(videos);

  useEffect(() => {
    const loadPosts = async () => {
      try {
        const data = await getPosts(token);
        const savedPosts = (data.posts || []).map((post) => ({
          id: post._id,
          video: post.video || null,
          image: post.image || null,
          username: post.author?.username
            ? `@${post.author.username}`
            : '@toplineuser',
          caption: post.text || '',
          music: post.video ? 'Video post' : 'Topline post',
          likes: 0,
          comments: 0,
          shares: 0,
          liked: false,
          following: false,
        }));

        setFeed([...savedPosts, ...videos]);
      } catch (error) {
        console.error('Failed to load posts:', error);
      }
    };

    const unsubscribe = navigation.addListener('focus', loadPosts);
    loadPosts();
    return unsubscribe;
  }, [navigation, token]);

  const toggleLike = (id) => {
    setFeed((current) =>
      current.map((item) =>
        item.id === id
          ? {
              ...item,
              liked: !item.liked,
              likes: item.liked
                ? item.likes - 1
                : item.likes + 1,
            }
          : item
      )
    );
  };

  const toggleFollow = (id) => {
    setFeed((current) =>
      current.map((item) =>
        item.id === id
          ? {
              ...item,
              following: !item.following,
            }
          : item
      )
    );
  };

  const handleViewableItemsChanged = ({ viewableItems }) => {
    if (viewableItems.length > 0) {
      const index = viewableItems[0].index;

      if (typeof index === 'number') {
        setActiveIndex(index);
      }
    }
  };

  const viewabilityConfig = {
    itemVisiblePercentThreshold: 70,
  };

  const renderVideo = ({ item, index }) => {
    return (
      <View
        style={[
          styles.videoContainer,
          {
            width: SCREEN_WIDTH,
            height: SCREEN_HEIGHT,
          },
        ]}
      >
        {/* VIDEO */}

        {item.video ? (
          <VideoPlayer
            source={item.video}
            isActive={activeIndex === index}
            loop
          />
        ) : item.image ? (
          <Image
            source={{ uri: item.image }}
            style={styles.imagePost}
            resizeMode="cover"
          />
        ) : null}

        {/* BOTTOM OVERLAY */}

        <View
          pointerEvents="none"
          style={styles.bottomOverlay}
        />

        {/* TOP */}

        <SafeAreaView style={styles.topArea}>
          <View style={styles.topBar}>
            <Image
              source={require('../../assets/logo1.png')}
              style={styles.logoImage}
              resizeMode="contain"
            />

            <View style={styles.feedTabs}>
              <TouchableOpacity activeOpacity={0.8}>
                <Text style={styles.feedTab}>
                  Following
                </Text>
              </TouchableOpacity>

              <View style={styles.tabDivider} />

              <TouchableOpacity activeOpacity={0.8}>
                <Text style={styles.feedTabActive}>
                  For You
                </Text>
              </TouchableOpacity>
            </View>

            <TouchableOpacity
              style={styles.messagesButton}
              onPress={() => navigation.navigate('Messages')}
              accessibilityRole="button"
              accessibilityLabel="Open messages"
              activeOpacity={0.8}
            >
              <MessageCircle
                size={21}
                color={WHITE}
              />
            </TouchableOpacity>
          </View>
        </SafeAreaView>

        {/* RIGHT SIDE ACTIONS */}

        <View style={styles.actionsContainer}>
          {/* CREATOR */}

          <TouchableOpacity
            style={styles.profileAction}
            activeOpacity={0.8}
          >
            <View style={styles.creatorAvatar}>
              <Text style={styles.creatorAvatarText}>
                {item.username.charAt(1).toUpperCase()}
              </Text>
            </View>

            {!item.following && (
              <View style={styles.followBadge}>
                <Plus
                  size={12}
                  color={WHITE}
                  strokeWidth={3}
                />
              </View>
            )}
          </TouchableOpacity>

          {/* LIKE */}

          <TouchableOpacity
            style={styles.action}
            onPress={() => toggleLike(item.id)}
            activeOpacity={0.8}
          >
            <Heart
              size={34}
              color={item.liked ? ORANGE : WHITE}
              fill={item.liked ? ORANGE : 'transparent'}
            />

            <Text style={styles.actionCount}>
              {item.likes}
            </Text>
          </TouchableOpacity>

          {/* COMMENTS */}

          <TouchableOpacity
            style={styles.action}
            activeOpacity={0.8}
          >
            <MessageCircle
              size={34}
              color={WHITE}
            />

            <Text style={styles.actionCount}>
              {item.comments}
            </Text>
          </TouchableOpacity>

          {/* SHARE */}

          <TouchableOpacity
            style={styles.action}
            activeOpacity={0.8}
          >
            <Share2
              size={33}
              color={WHITE}
            />

            <Text style={styles.actionCount}>
              {item.shares}
            </Text>
          </TouchableOpacity>

          {/* FOLLOW */}

          <TouchableOpacity
            style={styles.action}
            onPress={() => toggleFollow(item.id)}
            activeOpacity={0.8}
          >
            <UserPlus
              size={31}
              color={item.following ? ORANGE : WHITE}
            />

            <Text
              style={[
                styles.actionCount,
                item.following && styles.followingText,
              ]}
            >
              {item.following
                ? 'Following'
                : 'Follow'}
            </Text>
          </TouchableOpacity>
        </View>

        {/* CAPTION */}

        <View style={styles.captionContainer}>
          <Text style={styles.username}>
            {item.username}
          </Text>

          <Text style={styles.caption}>
            {item.caption}
          </Text>

          <View style={styles.musicRow}>
            <Music2
              size={16}
              color={WHITE}
            />

            <Text
              style={styles.musicText}
              numberOfLines={1}
            >
              {item.music}
            </Text>
          </View>
        </View>
      </View>
    );
  };

  return (
    <View style={styles.container}>
      <FlatList
        data={feed}
        renderItem={renderVideo}
        keyExtractor={(item) => item.id}
        pagingEnabled
        showsVerticalScrollIndicator={false}
        snapToInterval={SCREEN_HEIGHT}
        snapToAlignment="start"
        decelerationRate="fast"
        disableIntervalMomentum
        onViewableItemsChanged={
          handleViewableItemsChanged
        }
        viewabilityConfig={viewabilityConfig}
        initialNumToRender={2}
        maxToRenderPerBatch={2}
        windowSize={3}
        getItemLayout={(_, index) => ({
          length: SCREEN_HEIGHT,
          offset: SCREEN_HEIGHT * index,
          index,
        })}
      />

      {/* BOTTOM NAVIGATION */}

      <SafeAreaView style={styles.bottomSafeArea}>
        <View style={styles.bottomNav}>
          {/* HOME */}

          <TouchableOpacity
            style={styles.navItem}
            activeOpacity={0.8}
          >
            <Home
              size={24}
              color={ORANGE}
              fill={ORANGE}
            />

            <Text style={styles.navActiveText}>
              Home
            </Text>
          </TouchableOpacity>

          {/* FRIENDS */}

          <TouchableOpacity
            style={styles.navItem}
            onPress={() =>
              navigation.navigate('Friends')
            }
            activeOpacity={0.8}
          >
            <Users
              size={24}
              color={WHITE}
            />

            <Text style={styles.navText}>
              People
            </Text>
          </TouchableOpacity>

          {/* CREATE */}

          <TouchableOpacity
            style={styles.createNavButton}
            onPress={() =>
              navigation.navigate('CreatePost')
            }
            activeOpacity={0.85}
          >
            <Plus
              size={30}
              color={WHITE}
              strokeWidth={2.5}
            />
          </TouchableOpacity>

          {/* ALERTS */}

          <TouchableOpacity
            style={styles.navItem}
            onPress={() => navigation.navigate('Notifications')}
            activeOpacity={0.8}
          >
            <Bell
              size={24}
              color={WHITE}
            />

            <Text style={styles.navText}>
              Alerts
            </Text>
          </TouchableOpacity>

          {/* PROFILE */}

          <TouchableOpacity
            style={styles.navItem}
            onPress={() =>
              navigation.navigate('UserProfile')
            }
            activeOpacity={0.8}
          >
            <User
              size={24}
              color={WHITE}
            />

            <Text style={styles.navText}>
              Profile
            </Text>
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    </View>
  );
}