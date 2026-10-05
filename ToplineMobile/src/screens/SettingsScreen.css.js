import { StyleSheet } from 'react-native';
const createStyles = (colors) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },

  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingTop: 16,
    paddingBottom: 20,
  },

  backButton: {
    flex: 1,
  },

  backText: {
    color: colors.primary,
    fontWeight: '600',
    fontSize: 16,
  },

  title: {
    flex: 1,
    textAlign: 'center',
    fontSize: 22,
    fontWeight: '700',
    color: colors.text,
  },

  spacer: {
    flex: 1,
  },

  card: {
    marginHorizontal: 20,
    backgroundColor: colors.surface,
    borderRadius: 8,
    padding: 20,
    borderWidth: 1,
    borderColor: colors.border,
    shadowColor: '#000',
    shadowOpacity: 0.04,
    shadowRadius: 6,
    shadowOffset: { width: 0, height: 2 },
    elevation: 2,
  },

  themeCard: {
    minHeight: 92,
    marginTop: 16,
    marginHorizontal: 20,
    paddingHorizontal: 18,
    paddingVertical: 14,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
  },

  languageCard: {
    minHeight: 92,
    marginTop: 12,
    marginHorizontal: 20,
    paddingHorizontal: 18,
    paddingVertical: 14,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
  },

  themeCopy: {
    flex: 1,
    marginRight: 12,
  },

  themeTitle: {
    fontSize: 15,
    fontWeight: '700',
    color: colors.text,
  },

  themeDescription: {
    marginTop: 3,
    fontSize: 12,
    color: colors.textSecondary,
  },

  languageBackdrop: {
    flex: 1,
    justifyContent: 'center',
    paddingHorizontal: 24,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
  },

  languageDialog: {
    paddingHorizontal: 18,
    paddingVertical: 12,
    backgroundColor: colors.surface,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.border,
  },

  languageDialogHeader: {
    minHeight: 48,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },

  languageDialogTitle: {
    fontSize: 17,
    fontWeight: '700',
    color: colors.text,
  },

  languageCloseButton: {
    width: 36,
    height: 36,
    alignItems: 'center',
    justifyContent: 'center',
  },

  languageOption: {
    minHeight: 52,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },

  languageOptionText: {
    fontSize: 15,
    color: colors.text,
  },

  sectionLabel: {
    fontSize: 12,
    textTransform: 'uppercase',
    letterSpacing: 1,
    color: colors.textSecondary,
    marginBottom: 10,
  },

  userName: {
    fontSize: 22,
    fontWeight: '700',
    color: colors.text,
    marginBottom: 6,
  },

  userEmail: {
    fontSize: 15,
    color: colors.textSecondary,
  },

  primaryButton: {
    marginTop: 28,
    marginHorizontal: 20,
    backgroundColor: colors.primary,
    borderRadius: 12,
    paddingVertical: 16,
    alignItems: 'center',
  },

  primaryButtonText: {
    color: colors.white,
    fontSize: 16,
    fontWeight: '700',
  },

  logoutButton: {
    marginTop: 16,
    marginHorizontal: 20,
    backgroundColor: colors.surfaceMuted,
    borderRadius: 12,
    paddingVertical: 16,
    alignItems: 'center',
  },

  logoutText: {
    color: colors.text,
    fontSize: 16,
    fontWeight: '700',
  },
});

export default createStyles;
