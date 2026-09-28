
import { StyleSheet } from 'react-native';

import {
  colors,
  spacing,
  typography,
} from '../theme';

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },

  keyboard: {
    flex: 1,
  },

  header: {
    height: 64,
    paddingHorizontal: spacing.md,

    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',

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

  headerTitle: {
    flex: 1,

    marginHorizontal: spacing.sm,

    textAlign: 'center',

    fontSize: 18,
    fontWeight: '700',

    color: colors.text,
  },

  publishButton: {
    minWidth: 60,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,

    alignItems: 'center',
    justifyContent: 'center',

    backgroundColor: colors.primary,
    borderRadius: 18,
  },

  publishButtonDisabled: {
    opacity: 0.45,
  },

  publishText: {
    fontSize: 14,
    fontWeight: '700',
    color: colors.white,
  },

  scrollView: {
    flex: 1,
  },

  content: {
    padding: spacing.md,
    paddingBottom: spacing.xxl,
  },

  userRow: {
    flexDirection: 'row',
    alignItems: 'center',

    marginBottom: spacing.lg,
  },

  avatar: {
    width: 48,
    height: 48,
    borderRadius: 24,

    marginRight: spacing.md,

    alignItems: 'center',
    justifyContent: 'center',

    backgroundColor: colors.primary,
  },

  avatarText: {
    fontSize: 18,
    fontWeight: '800',
    color: colors.white,
  },

  userName: {
    fontSize: 16,
    fontWeight: '700',
    color: colors.text,
  },

  visibility: {
    marginTop: 3,

    fontSize: 12,
    color: colors.textSecondary,
  },

  textInput: {
    minHeight: 180,

    padding: spacing.md,

    backgroundColor: colors.surface,

    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 12,

    fontSize: 18,
    lineHeight: 26,

    color: colors.text,

    textAlignVertical: 'top',
  },

  mediaSection: {
    marginTop: spacing.lg,

    padding: spacing.md,

    backgroundColor: colors.surface,

    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 12,
  },

  mediaTitle: {
    fontSize: typography.bodyMedium.fontSize,
    fontWeight: '600',
    color: colors.text,
  },

  mediaActions: {
    flexDirection: 'row',

    marginTop: spacing.md,

    gap: spacing.sm,
  },

  mediaButton: {
    flex: 1,

    minHeight: 52,

    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',

    gap: spacing.sm,

    borderWidth: 1,
    borderColor: colors.border,

    borderRadius: 8,
  },

  mediaText: {
    fontSize: 14,
    fontWeight: '600',
    color: colors.text,
  },
});

export default styles;
