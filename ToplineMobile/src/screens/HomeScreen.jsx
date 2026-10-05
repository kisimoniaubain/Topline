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
import { useBottomTabBarHeight } from '@react-navigation/bottom-tabs';

import {
  Heart,
  Bell,
  MessageCircle,
  Share2,
  UserPlus,
  Plus,
  Music2,
} from 'lucide-react-native';

import VideoPlayer from '../components/VideoPlayer';
import CommentsScreen from './CommentsScreen';
import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import { getPosts } from '../services/postService';
import useThemeStyles from '../theme/useThemeStyles';
import createStyles from './HomeScreen.css';

const { height: SCREEN_HEIGHT, width: SCREEN_WIDTH } =
  Dimensions.get('window');

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
    comments: 0,
    localSample: true,
    localComments: [],
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
    comments: 0,
    localSample: true,
    localComments: [],
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
    comments: 0,
    localSample: true,
    localComments: [],
    shares: 29,
    liked: false,
    following: false,
  },
];

export default function HomeScreen({ navigation }) {
  const styles = useThemeStyles(createStyles);
  const { colors, mode } = useTheme();
  const { t } = useLanguage();
  const ORANGE = colors.primary;
  const WHITE = colors.white;
  const { token } = useAuth();
  const tabBarHeight = useBottomTabBarHeight();
  const videoHeight = SCREEN_HEIGHT - tabBarHeight;

  const [activeIndex, setActiveIndex] = useState(0);
  const [feed, setFeed] = useState(videos);
  const [selectedPost, setSelectedPost] = useState(null);
  const [commentsVisible, setCommentsVisible] = useState(false);

  useEffect(() => {
    const loadPosts = async () => {
      try {
        const data = await getPosts(token);
        const savedPosts = (data.posts || []).map((post) => ({
          id: post._id,
          author: post.author || null,
          video: post.video || null,
          image: post.image || null,
          username: post.author?.username
            ? `@${post.author.username}`
            : '@toplineuser',
          caption: post.text || '',
          music: post.video ? 'Video post' : 'Topline post',
          likes: 0,
          comments: post.commentsCount || 0,
          shares: 0,
          liked: false,
          following: false,
          localSample: false,
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

  const updateCommentCount = (postId, commentsCount, localComments) => {
    setFeed((current) =>
      current.map((item) =>
        item.id === postId
          ? {
              ...item,
              comments: commentsCount,
              ...(localComments ? { localComments } : {}),
            }
          : item
      )
    );
    setSelectedPost((current) =>
      current?.id === postId
        ? {
            ...current,
            comments: commentsCount,
            ...(localComments ? { localComments } : {}),
          }
        : current
    );
  };

  const handleCommentAdded = (postId, _comment, commentsCount, localComments) => {
    updateCommentCount(postId, commentsCount, localComments);
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
            height: videoHeight,
          },
        ]}
      >
        {/* VIDEO */}

        {item.video ? (
          <VideoPlayer
            source={item.video}
            isActive={activeIndex === index && !commentsVisible}
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

        {/* RIGHT SIDE ACTIONS */}

        <View style={styles.actionsContainer}>
          {/* CREATOR */}

          <TouchableOpacity
            style={styles.profileAction}
            onPress={() => {
              const profileOwner = item.author || {
                name: item.username.replace(/^@/, ''),
                username: item.username.replace(/^@/, ''),
                profilePicture: '',
                posts: [],
              };
              navigation.navigate('UserProfile', { user: profileOwner });
            }}
            accessibilityRole="button"
            accessibilityLabel={t('Open profile for {name}', { name: item.username })}
            activeOpacity={0.8}
          >
            {item.author?.profilePicture ? (
              <Image
                source={{ uri: item.author.profilePicture }}
                style={styles.creatorAvatarImage}
              />
            ) : (
              <View style={styles.creatorAvatar}>
                <Text style={styles.creatorAvatarText}>
                  {item.username.charAt(1).toUpperCase()}
                </Text>
              </View>
            )}

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
            onPress={() => {
              setSelectedPost(item);
              setCommentsVisible(true);
            }}
            accessibilityRole="button"
            accessibilityLabel={t('View {count} comments', { count: item.comments })}
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
                ? t('Following')
                : t('Follow')}
            </Text>
          </TouchableOpacity>
        </View>

        {/* CAPTION */}

        <View style={styles.captionContainer}>
          <Text style={styles.username}>
            {item.username}
          </Text>

          <Text style={styles.caption}>
            {item.localSample ? t(item.caption) : item.caption}
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
              {item.localSample ? t(item.music) : item.music}
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
        snapToInterval={videoHeight}
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
          length: videoHeight,
          offset: videoHeight * index,
          index,
        })}
      />

      {commentsVisible && selectedPost ? (
        <CommentsScreen
          key={selectedPost.id}
          visible={commentsVisible}
          post={selectedPost}
          onClose={() => setCommentsVisible(false)}
          onCommentAdded={handleCommentAdded}
          onCommentCountChanged={updateCommentCount}
        />
      ) : null}

      <SafeAreaView style={styles.topArea}>
        <View style={styles.topBar}>
          <Image
            source={mode === 'dark'
              ? require('../../assets/logo1.png')
              : require('../../assets/logo.png')}
            style={styles.logoImage}
            resizeMode="contain"
          />

          <View style={styles.feedTabs}>
            <TouchableOpacity activeOpacity={0.8}>
              <Text style={styles.feedTab}>{t('Following')}</Text>
            </TouchableOpacity>

            <View style={styles.tabDivider} />

            <TouchableOpacity activeOpacity={0.8}>
              <Text style={styles.feedTabActive}>{t('For You')}</Text>
            </TouchableOpacity>
          </View>

          <TouchableOpacity
            style={styles.messagesButton}
            onPress={() => navigation.navigate('Notifications')}
            accessibilityRole="button"
            accessibilityLabel={t('Open alerts')}
            activeOpacity={0.8}
          >
            <Bell size={21} color={WHITE} />
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    </View>
  );
}