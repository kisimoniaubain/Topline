import { StyleSheet } from 'react-native';

const createStyles = (colors) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },

  videoContainer: {
    backgroundColor: colors.black,
    position: 'relative',
  },

  imagePost: {
    position: 'absolute',
    top: 0,
    right: 0,
    bottom: 0,
    left: 0,
  },

  bottomOverlay: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    height: 280,
    backgroundColor: 'rgba(0, 0, 0, 0.38)',
  },

  topArea: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    zIndex: 10,
    elevation: 10,
  },

  topBar: {
    height: 65,
    paddingHorizontal: 14,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },

  logoImage: {
    width: 88,
    height: 30,
  },

  feedTabs: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },

  feedTab: {
    color: colors.textSecondary,
    fontSize: 15,
    fontWeight: '600',
  },

  feedTabActive: {
    color: colors.white,
    fontSize: 15,
    fontWeight: '800',
  },

  tabDivider: {
    width: 1,
    height: 15,
    backgroundColor: colors.textLight,
  },

  messagesButton: {
    width: 36,
    height: 36,
    alignItems: 'center',
    justifyContent: 'center',
  },

  actionsContainer: {
    position: 'absolute',
    right: 12,
    bottom: 135,
    alignItems: 'center',
  },

  profileAction: {
    width: 50,
    height: 58,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 13,
  },

  creatorAvatar: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: colors.primary,
    borderWidth: 2,
    borderColor: colors.white,
    alignItems: 'center',
    justifyContent: 'center',
  },

  creatorAvatarImage: {
    width: 48,
    height: 48,
    borderRadius: 24,
    borderWidth: 2,
    borderColor: colors.white,
  },

  creatorAvatarText: {
    color: colors.white,
    fontSize: 19,
    fontWeight: '800',
  },

  followBadge: {
    position: 'absolute',
    bottom: -2,
    width: 22,
    height: 22,
    borderRadius: 11,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 2,
    borderColor: colors.black,
  },

  action: {
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 21,
    minWidth: 55,
  },

  actionCount: {
    color: colors.white,
    fontSize: 12,
    fontWeight: '700',
    marginTop: 5,
    textShadowColor: 'rgba(0, 0, 0, 0.8)',
    textShadowOffset: {
      width: 0,
      height: 1,
    },
    textShadowRadius: 3,
  },

  followingText: {
    color: colors.primary,
  },

  captionContainer: {
    position: 'absolute',
    left: 16,
    right: 82,
    bottom: 108,
  },

  username: {
    color: colors.white,
    fontSize: 17,
    fontWeight: '800',
    marginBottom: 7,
    textShadowColor: 'rgba(0, 0, 0, 0.8)',
    textShadowOffset: {
      width: 0,
      height: 1,
    },
    textShadowRadius: 3,
  },

  caption: {
    color: colors.white,
    fontSize: 14,
    lineHeight: 20,
    marginBottom: 10,
    textShadowColor: 'rgba(0, 0, 0, 0.8)',
    textShadowOffset: {
      width: 0,
      height: 1,
    },
    textShadowRadius: 3,
  },

  musicRow: {
    flexDirection: 'row',
    alignItems: 'center',
    maxWidth: '100%',
  },

  musicText: {
    color: colors.white,
    fontSize: 13,
    marginLeft: 7,
    flex: 1,
    fontWeight: '600',
  },

});

export default createStyles;