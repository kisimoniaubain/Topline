import { StyleSheet } from 'react-native';

import { spacing, typography } from '../theme';

const createStyles = (colors) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },

  header: {
    height: 64,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: spacing.md,
    backgroundColor: colors.surface,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },

  headerButton: {
    width: 38,
    height: 38,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.surfaceMuted,
    borderRadius: 19,
  },

  headerTitle: {
    flex: 1,
    textAlign: 'center',
    marginHorizontal: spacing.sm,
    fontSize: 17,
    fontWeight: '700',
    color: colors.text,
  },

  content: {
    paddingBottom: spacing.xxl,
  },

  profileSection: {
    alignItems: 'center',
    paddingHorizontal: spacing.lg,
    paddingTop: spacing.xl,
    paddingBottom: spacing.lg,
    backgroundColor: colors.surface,
  },

  avatar: {
    width: 104,
    height: 104,
    borderRadius: 52,
    marginBottom: spacing.md,
  },

  avatarPlaceholder: {
    width: 104,
    height: 104,
    borderRadius: 52,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.primary,
    marginBottom: spacing.md,
  },

  avatarText: {
    fontSize: 40,
    fontWeight: '800',
    color: colors.white,
  },

  editAvatarButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.xs,
    marginBottom: spacing.md,
    paddingVertical: spacing.xs,
  },

  editAvatarText: {
    fontSize: 13,
    fontWeight: '600',
    color: colors.primaryDark,
  },

  name: {
    fontSize: 24,
    fontWeight: '800',
    color: colors.text,
    textAlign: 'center',
  },

  username: {
    marginTop: 3,
    fontSize: 15,
    color: colors.textSecondary,
    textAlign: 'center',
  },

  bio: {
    maxWidth: 340,
    marginTop: spacing.md,
    fontSize: 15,
    lineHeight: 22,
    color: colors.text,
    textAlign: 'center',
  },

  details: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    gap: spacing.md,
    marginTop: spacing.md,
  },

  detailItem: {
    flexDirection: 'row',
    alignItems: 'center',
  },

  detailText: {
    marginLeft: 5,
    fontSize: 13,
    color: colors.textSecondary,
  },

  stats: {
    flexDirection: 'row',
    paddingVertical: spacing.lg,
    backgroundColor: colors.surface,
    borderTopWidth: 1,
    borderBottomWidth: 1,
    borderColor: colors.border,
  },

  stat: {
    flex: 1,
    alignItems: 'center',
  },

  statNumber: {
    fontSize: 19,
    fontWeight: '800',
    color: colors.text,
  },

  statLabel: {
    marginTop: 3,
    fontSize: 13,
    color: colors.textSecondary,
  },

  actions: {
    flexDirection: 'row',
    padding: spacing.md,
    gap: spacing.sm,
    backgroundColor: colors.surface,
  },

  followButton: {
    flex: 1,
    height: 46,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.sm,
    backgroundColor: colors.primary,
    borderRadius: 8,
  },

  followingButton: {
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
  },

  followButtonText: {
    fontSize: typography.button.fontSize,
    fontWeight: '600',
    color: colors.white,
  },

  followingButtonText: {
    color: colors.text,
  },

  messageButton: {
    flex: 1,
    height: 46,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.sm,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
  },

  messageButtonText: {
    fontSize: typography.button.fontSize,
    fontWeight: '600',
    color: colors.text,
  },

  postsHeader: {
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.md,
    backgroundColor: colors.surface,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },

  postsTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: colors.text,
  },

  post: {
    marginTop: spacing.sm,
    padding: spacing.md,
    backgroundColor: colors.surface,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },

  postImage: {
    width: '100%',
    height: 220,
    borderRadius: 8,
    marginBottom: spacing.sm,
  },

  postText: {
    fontSize: 16,
    lineHeight: 23,
    color: colors.text,
  },

  postDate: {
    marginTop: spacing.sm,
    fontSize: 12,
    color: colors.textLight,
  },

  emptyPosts: {
    alignItems: 'center',
    paddingHorizontal: spacing.xl,
    paddingVertical: 60,
    backgroundColor: colors.surface,
  },

  emptyTitle: {
    fontSize: 17,
    fontWeight: '700',
    color: colors.text,
  },

  emptyText: {
    marginTop: spacing.sm,
    fontSize: 14,
    lineHeight: 20,
    color: colors.textSecondary,
    textAlign: 'center',
  },

  postVideoFrame: {
    height: 210,
    marginBottom: spacing.sm,
    position: 'relative',
    overflow: 'hidden',
    backgroundColor: colors.black,
    borderRadius: 8,
  },

  postVideoControl: {
    position: 'absolute',
    top: '50%',
    left: '50%',
    width: 52,
    height: 52,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.65)',
    borderRadius: 26,
    transform: [{ translateX: -26 }, { translateY: -26 }],
  },

});

export default createStyles;