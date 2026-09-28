import React from 'react';
import {
  View,
  Text,
  StyleSheet,
} from 'react-native';
import { colors, spacing, typography } from '../theme';
import UserAvatar from './UserAvatar';

const PostCard = ({
  username = 'User',
  content,
  avatar,
  createdAt,
}) => {
  return (
    <View style={styles.card}>
      <View style={styles.header}>
        <UserAvatar
          uri={avatar}
          name={username}
          size={44}
        />

        <View style={styles.userInfo}>
          <Text style={styles.username}>{username}</Text>

          {createdAt && (
            <Text style={styles.date}>{createdAt}</Text>
          )}
        </View>
      </View>

      <Text style={styles.content}>{content}</Text>
    </View>
  );
};

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.white,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 10,
    padding: spacing.md,
    marginBottom: spacing.md,
  },

  header: {
    flexDirection: 'row',
    alignItems: 'center',
  },

  userInfo: {
    marginLeft: spacing.sm,
    flex: 1,
  },

  username: {
    ...typography.bodyMedium,
    color: colors.text,
  },

  date: {
    ...typography.small,
    color: colors.textSecondary,
    marginTop: 2,
  },

  content: {
    ...typography.body,
    color: colors.text,
    lineHeight: 24,
    marginTop: spacing.md,
  },
});

export default PostCard;