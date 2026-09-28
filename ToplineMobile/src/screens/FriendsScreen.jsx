import React, { useMemo, useState } from 'react';
import {
  View,
  Text,
  SafeAreaView,
  ScrollView,
  TextInput,
  TouchableOpacity,
  Image,
} from 'react-native';

import {
  ArrowLeft,
  Search,
  UserPlus,
  UserCheck,
  Users,
} from 'lucide-react-native';

import { colors } from '../theme';
import styles from './FriendsScreen.css';

const initialUsers = [
  {
    id: '1',
    name: 'Alex Johnson',
    username: 'alexjohnson',
    bio: 'Living, learning and sharing.',
    location: 'Nairobi, Kenya',
    followers: 128,
    following: 84,
    profilePicture: '',
    posts: [],
  },
  {
    id: '2',
    name: 'Sarah Williams',
    username: 'sarahw',
    bio: 'Creator • Photographer • Traveler',
    location: 'Kenya',
    followers: 342,
    following: 119,
    profilePicture: '',
    posts: [],
  },
  {
    id: '3',
    name: 'David Kim',
    username: 'davidkim',
    bio: 'Technology and community.',
    location: 'Nairobi, Kenya',
    followers: 219,
    following: 156,
    profilePicture: '',
    posts: [],
  },
  {
    id: '4',
    name: 'Grace Mwangi',
    username: 'gracemwangi',
    bio: 'Making a difference every day.',
    location: 'Kenya',
    followers: 487,
    following: 201,
    profilePicture: '',
    posts: [],
  },
];

export default function FriendsScreen({ navigation }) {
  const [search, setSearch] = useState('');
  const [followingUsers, setFollowingUsers] = useState([]);

  const filteredUsers = useMemo(() => {
    const value = search.trim().toLowerCase();

    if (!value) {
      return initialUsers;
    }

    return initialUsers.filter((user) => {
      return (
        user.name.toLowerCase().includes(value) ||
        user.username.toLowerCase().includes(value)
      );
    });
  }, [search]);

  const toggleFollow = (userId) => {
    setFollowingUsers((current) => {
      if (current.includes(userId)) {
        return current.filter((id) => id !== userId);
      }

      return [...current, userId];
    });
  };

  const openProfile = (user) => {
    navigation.navigate('UserProfile', {
      user,
    });
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity
          style={styles.headerButton}
          onPress={() => navigation.goBack()}
        >
          <ArrowLeft
            size={24}
            color={colors.text}
          />
        </TouchableOpacity>

        <View style={styles.headerTitleContainer}>
          <Text style={styles.headerTitle}>
            Friends
          </Text>

          <Text style={styles.headerSubtitle}>
            Find people and connect
          </Text>
        </View>

        <View style={styles.headerButton} />
      </View>

      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
      >
        <View style={styles.searchContainer}>
          <Search
            size={20}
            color={colors.textSecondary}
          />

          <TextInput
            style={styles.searchInput}
            placeholder="Search people..."
            placeholderTextColor={colors.textLight}
            value={search}
            onChangeText={setSearch}
            autoCapitalize="none"
          />
        </View>

        <View style={styles.sectionHeader}>
          <View>
            <Text style={styles.sectionTitle}>
              People you may know
            </Text>

            <Text style={styles.sectionSubtitle}>
              Discover people on Topline
            </Text>
          </View>

          <Users
            size={22}
            color={colors.primary}
          />
        </View>

        {filteredUsers.length > 0 ? (
          filteredUsers.map((user) => {
            const isFollowing =
              followingUsers.includes(user.id);

            return (
              <View
                key={user.id}
                style={styles.userCard}
              >
                <TouchableOpacity
                  style={styles.userInfo}
                  onPress={() => openProfile(user)}
                  activeOpacity={0.75}
                >
                  {user.profilePicture ? (
                    <Image
                      source={{
                        uri: user.profilePicture,
                      }}
                      style={styles.avatar}
                    />
                  ) : (
                    <View
                      style={
                        styles.avatarPlaceholder
                      }
                    >
                      <Text
                        style={styles.avatarText}
                      >
                        {user.name
                          .charAt(0)
                          .toUpperCase()}
                      </Text>
                    </View>
                  )}

                  <View style={styles.userDetails}>
                    <Text
                      style={styles.userName}
                      numberOfLines={1}
                    >
                      {user.name}
                    </Text>

                    <Text
                      style={styles.username}
                      numberOfLines={1}
                    >
                      @{user.username}
                    </Text>

                    <Text
                      style={styles.userBio}
                      numberOfLines={1}
                    >
                      {user.bio}
                    </Text>
                  </View>
                </TouchableOpacity>

                <TouchableOpacity
                  style={[
                    styles.followButton,
                    isFollowing &&
                      styles.followingButton,
                  ]}
                  onPress={() =>
                    toggleFollow(user.id)
                  }
                  activeOpacity={0.8}
                >
                  {isFollowing ? (
                    <UserCheck
                      size={17}
                      color={colors.text}
                    />
                  ) : (
                    <UserPlus
                      size={17}
                      color={colors.white}
                    />
                  )}

                  <Text
                    style={[
                      styles.followButtonText,
                      isFollowing &&
                        styles.followingButtonText,
                    ]}
                  >
                    {isFollowing
                      ? 'Following'
                      : 'Follow'}
                  </Text>
                </TouchableOpacity>
              </View>
            );
          })
        ) : (
          <View style={styles.emptyState}>
            <Search
              size={42}
              color={colors.textLight}
            />

            <Text style={styles.emptyTitle}>
              No people found
            </Text>

            <Text style={styles.emptyText}>
              Try searching for another name or
              username.
            </Text>
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}
