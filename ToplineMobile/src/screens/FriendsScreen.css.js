import { StyleSheet } from 'react-native';

import { spacing } from '../theme';

const createStyles = (colors) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },

  header: {
    height: 68,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: spacing.md,
    backgroundColor: colors.surface,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },

  headerButton: {
    width: 40,
    height: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },

  headerTitleContainer: {
    flex: 1,
    alignItems: 'center',
  },

  headerTitle: {
    fontSize: 19,
    fontWeight: '800',
    color: colors.text,
  },

  headerSubtitle: {
    marginTop: 2,
    fontSize: 12,
    color: colors.textSecondary,
  },

  content: {
    padding: spacing.md,
    paddingBottom: spacing.xxl,
  },

  searchContainer: {
    height: 48,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: spacing.md,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
  },

  searchInput: {
    flex: 1,
    marginLeft: spacing.sm,
    fontSize: 15,
    color: colors.text,
  },

  sectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: spacing.xl,
    marginBottom: spacing.md,
  },

  sectionTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: colors.text,
  },

  sectionSubtitle: {
    marginTop: 3,
    fontSize: 13,
    color: colors.textSecondary,
  },

  userCard: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: spacing.md,
    marginBottom: spacing.sm,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 10,
  },

  userInfo: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    minWidth: 0,
  },

  avatar: {
    width: 56,
    height: 56,
    borderRadius: 28,
  },

  avatarPlaceholder: {
    width: 56,
    height: 56,
    borderRadius: 28,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.primary,
  },

  avatarText: {
    fontSize: 22,
    fontWeight: '800',
    color: colors.white,
  },

  userDetails: {
    flex: 1,
    marginLeft: spacing.md,
    marginRight: spacing.sm,
  },

  userName: {
    fontSize: 16,
    fontWeight: '700',
    color: colors.text,
  },

  username: {
    marginTop: 2,
    fontSize: 13,
    color: colors.textSecondary,
  },

  userBio: {
    marginTop: 5,
    fontSize: 12,
    color: colors.textLight,
  },

  followButton: {
    minWidth: 92,
    height: 38,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: spacing.sm,
    gap: 5,
    backgroundColor: colors.primary,
    borderRadius: 7,
  },

  followingButton: {
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
  },

  followButtonText: {
    fontSize: 13,
    fontWeight: '700',
    color: colors.white,
  },

  followingButtonText: {
    color: colors.text,
  },

  emptyState: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 70,
    paddingHorizontal: spacing.xl,
  },

  emptyTitle: {
    marginTop: spacing.md,
    fontSize: 18,
    fontWeight: '700',
    color: colors.text,
  },

  emptyText: {
    marginTop: spacing.sm,
    fontSize: 14,
    lineHeight: 20,
    textAlign: 'center',
    color: colors.textSecondary,
  },
});

export default createStyles;