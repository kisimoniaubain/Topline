import { StyleSheet } from 'react-native';

import {
  colors,
  spacing,
  typography,
} from '../theme';

const styles = StyleSheet.create({

  // =====================================================
  // SAFE AREA
  // =====================================================

  safeArea: {
    flex: 1,
    backgroundColor: colors.background,
  },

  container: {
    flex: 1,
    backgroundColor: colors.background,
  },

  // =====================================================
  // HEADER
  // =====================================================

  header: {
    height: 64,
    paddingHorizontal: spacing.md,
    borderBottomWidth: 1,

    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },

  logo: {
    width: 120,
    height: 42,
  },

  headerActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
  },

  headerButton: {
    width: 40,
    height: 40,
    borderRadius: 20,

    alignItems: 'center',
    justifyContent: 'center',
  },

  avatar: {
    width: 40,
    height: 40,
    borderRadius: 20,

    backgroundColor: colors.primary,

    alignItems: 'center',
    justifyContent: 'center',
  },

  avatarText: {
    color: colors.white,
    fontSize: 16,
    fontWeight: '700',
  },

  // =====================================================
  // SCROLL VIEW
  // =====================================================

  scrollView: {
    flex: 1,
  },

  content: {
    paddingBottom: 24,
  },

  // =====================================================
  // STORIES
  // =====================================================

  section: {
    paddingVertical: spacing.md,
    marginBottom: spacing.sm,
  },

  sectionHeader: {
    paddingHorizontal: spacing.md,
    marginBottom: spacing.sm,

    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },

  sectionTitle: {
    ...typography.subheading,
  },

  seeAll: {
    color: colors.primary,
    fontSize: 14,
    fontWeight: '600',
  },

  storiesRow: {
    paddingHorizontal: spacing.md,
    gap: spacing.md,
  },

  story: {
    width: 68,
    alignItems: 'center',
  },

  storyCircle: {
    width: 58,
    height: 58,
    borderRadius: 29,

    backgroundColor: colors.primary,

    borderWidth: 3,
    borderColor: colors.primaryLight,

    alignItems: 'center',
    justifyContent: 'center',
  },

  ownStoryCircle: {
    backgroundColor: colors.background,
    borderColor: colors.border,
  },

  plusCircle: {
    width: 30,
    height: 30,
    borderRadius: 15,

    backgroundColor: colors.primary,

    alignItems: 'center',
    justifyContent: 'center',
  },

  storyInitial: {
    color: colors.white,
    fontSize: 20,
    fontWeight: '700',
  },

  storyName: {
    marginTop: 6,

    fontSize: 12,
    textAlign: 'center',
  },

  // =====================================================
  // CREATE POST
  // =====================================================

  composer: {
    marginHorizontal: spacing.sm,
    marginBottom: spacing.md,

    padding: spacing.md,

    borderWidth: 1,
    borderRadius: 12,
  },

  composerTop: {
    flexDirection: 'row',
    alignItems: 'center',
  },

  input: {
    flex: 1,

    minHeight: 44,

    marginLeft: spacing.sm,

    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,

    borderRadius: 22,

    fontSize: 15,
  },

  composerDivider: {
    height: 1,
    marginVertical: spacing.md,
  },

  composerActions: {
    flexDirection: 'row',
    alignItems: 'center',
  },

  composerAction: {
    marginRight: spacing.lg,

    flexDirection: 'row',
    alignItems: 'center',
  },

  actionText: {
    marginLeft: 6,

    color: colors.textSecondary,

    fontSize: 14,
    fontWeight: '500',
  },

  postButton: {
    marginLeft: 'auto',

    paddingHorizontal: 20,
    paddingVertical: 9,

    backgroundColor: colors.primary,

    borderRadius: 18,
  },

  postButtonDisabled: {
    opacity: 0.45,
  },

  postButtonText: {
    color: colors.white,

    fontSize: 14,
    fontWeight: '700',
  },

  // =====================================================
  // POSTS
  // =====================================================

  postsSection: {
    paddingHorizontal: spacing.sm,
  },

  postsTitle: {
    marginBottom: spacing.sm,
    paddingHorizontal: spacing.sm,
  },

  postCard: {
    marginBottom: spacing.md,

    padding: spacing.md,

    borderWidth: 1,
    borderRadius: 12,
  },

  postHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },

  postUser: {
    flexDirection: 'row',
    alignItems: 'center',
  },

  postAvatar: {
    width: 44,
    height: 44,
    borderRadius: 22,

    marginRight: spacing.sm,

    backgroundColor: colors.primary,

    alignItems: 'center',
    justifyContent: 'center',
  },

  postName: {
    fontSize: 15,
    fontWeight: '700',
  },

  postMeta: {
    marginTop: 2,

    color: colors.textSecondary,

    fontSize: 12,
  },

  postText: {
    marginTop: spacing.md,

    fontSize: 15,
    lineHeight: 22,
  },

  // =====================================================
  // POST STATISTICS
  // =====================================================

  postStats: {
    marginTop: spacing.md,

    flexDirection: 'row',
    justifyContent: 'space-between',
  },

  statsRight: {
    flexDirection: 'row',
    gap: spacing.md,
  },

  statText: {
    color: colors.textSecondary,
    fontSize: 12,
  },

  // =====================================================
  // POST DIVIDER
  // =====================================================

  postDivider: {
    height: 1,
    marginVertical: spacing.sm,
  },

  // =====================================================
  // POST ACTIONS
  // =====================================================

  postActions: {
    flexDirection: 'row',
    justifyContent: 'space-around',
  },

  postAction: {
    paddingVertical: 6,

    flexDirection: 'row',
    alignItems: 'center',
  },

  postActionText: {
    marginLeft: 6,

    color: colors.textSecondary,

    fontSize: 13,
    fontWeight: '500',
  },

  likedText: {
    color: colors.primary,
  },

  // =====================================================
  // BOTTOM NAVIGATION
  // =====================================================

  bottomNav: {
    height: 72,

    borderTopWidth: 1,

    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-around',

    paddingHorizontal: 4,

    elevation: 10,

    shadowOffset: {
      width: 0,
      height: -2,
    },

    shadowOpacity: 0.08,

    shadowRadius: 5,

    zIndex: 100,
  },

  navItem: {
    minWidth: 55,

    alignItems: 'center',
    justifyContent: 'center',
  },

  navText: {
    marginTop: 3,

    color: colors.textSecondary,

    fontSize: 10,
    fontWeight: '500',
  },

  navTextActive: {
    marginTop: 3,

    color: colors.primary,

    fontSize: 10,
    fontWeight: '700',
  },

  createButton: {
    width: 50,
    height: 50,
    borderRadius: 25,

    marginTop: -24,

    backgroundColor: colors.primary,

    alignItems: 'center',
    justifyContent: 'center',

    elevation: 6,

    shadowOffset: {
      width: 0,
      height: 3,
    },

    shadowOpacity: 0.2,

    shadowRadius: 4,
  },
});

export default styles;