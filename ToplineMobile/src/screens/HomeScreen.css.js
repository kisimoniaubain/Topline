import { StyleSheet } from 'react-native';

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000000',
  },

  videoContainer: {
    backgroundColor: '#000000',
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
    color: '#BDBDBD',
    fontSize: 15,
    fontWeight: '600',
  },

  feedTabActive: {
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '800',
  },

  tabDivider: {
    width: 1,
    height: 15,
    backgroundColor: '#777777',
  },

  messagesButton: {
    width: 36,
    height: 36,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(255, 255, 255, 0.16)',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.35)',
    borderRadius: 18,
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
    backgroundColor: '#F57F17',
    borderWidth: 2,
    borderColor: '#FFFFFF',
    alignItems: 'center',
    justifyContent: 'center',
  },

  creatorAvatarText: {
    color: '#FFFFFF',
    fontSize: 19,
    fontWeight: '800',
  },

  followBadge: {
    position: 'absolute',
    bottom: -2,
    width: 22,
    height: 22,
    borderRadius: 11,
    backgroundColor: '#F57F17',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 2,
    borderColor: '#000000',
  },

  action: {
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 21,
    minWidth: 55,
  },

  actionCount: {
    color: '#FFFFFF',
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
    color: '#F57F17',
  },

  captionContainer: {
    position: 'absolute',
    left: 16,
    right: 82,
    bottom: 108,
  },

  username: {
    color: '#FFFFFF',
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
    color: '#FFFFFF',
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
    color: '#FFFFFF',
    fontSize: 13,
    marginLeft: 7,
    flex: 1,
    fontWeight: '600',
  },

  bottomSafeArea: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
  },

  bottomNav: {
    height: 72,
    backgroundColor: 'rgba(0, 0, 0, 0.94)',
    borderTopWidth: 1,
    borderTopColor: '#252525',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-around',
    paddingHorizontal: 7,
  },

  navItem: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },

  navText: {
    color: '#BDBDBD',
    fontSize: 10,
    fontWeight: '600',
    marginTop: 4,
  },

  navActiveText: {
    color: '#F57F17',
    fontSize: 10,
    fontWeight: '800',
    marginTop: 4,
  },

  createNavButton: {
    width: 52,
    height: 38,
    borderRadius: 10,
    backgroundColor: '#F57F17',
    alignItems: 'center',
    justifyContent: 'center',
    marginHorizontal: 4,
  },
});

export default styles;