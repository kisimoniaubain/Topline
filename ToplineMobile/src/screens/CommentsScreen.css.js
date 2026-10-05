import { StyleSheet } from 'react-native';
import { spacing } from '../theme';

const createStyles = (colors) => StyleSheet.create({
	overlay: {
		flex: 1,
		justifyContent: 'flex-end',
	},
	backdrop: {
		...StyleSheet.absoluteFillObject,
		backgroundColor: 'rgba(0, 0, 0, 0.48)',
	},
	keyboardArea: {
		flex: 1,
		justifyContent: 'flex-end',
	},
	sheet: {
		height: '82%',
		maxHeight: '88%',
		backgroundColor: colors.surface,
		borderTopLeftRadius: 18,
		borderTopRightRadius: 18,
		overflow: 'hidden',
	},
	handle: {
		alignSelf: 'center',
		width: 38,
		height: 4,
		marginTop: 9,
		marginBottom: 7,
		borderRadius: 2,
		backgroundColor: colors.borderDark,
	},
	header: {
		minHeight: 52,
		flexDirection: 'row',
		alignItems: 'center',
		justifyContent: 'center',
		borderBottomWidth: StyleSheet.hairlineWidth,
		borderBottomColor: colors.border,
	},
	headerTitleWrap: {
		flexDirection: 'row',
		alignItems: 'center',
		gap: spacing.xs,
	},
	title: {
		fontSize: 16,
		fontWeight: '700',
		color: colors.text,
	},
	count: {
		fontSize: 14,
		fontWeight: '600',
		color: colors.textSecondary,
	},
	closeButton: {
		position: 'absolute',
		right: spacing.md,
		width: 36,
		height: 36,
		alignItems: 'center',
		justifyContent: 'center',
	},
	errorText: {
		paddingHorizontal: spacing.md,
		paddingTop: spacing.sm,
		color: colors.error,
		fontSize: 13,
	},
	loadingState: {
		flex: 1,
		alignItems: 'center',
		justifyContent: 'center',
	},
	commentList: {
		paddingHorizontal: spacing.md,
		paddingVertical: spacing.sm,
	},
	emptyList: {
		flexGrow: 1,
		justifyContent: 'center',
	},
	emptyState: {
		alignItems: 'center',
		justifyContent: 'center',
		padding: spacing.xl,
	},
	emptyTitle: {
		marginTop: spacing.sm,
		fontSize: 15,
		fontWeight: '700',
		color: colors.text,
	},
	emptyText: {
		marginTop: spacing.xs,
		fontSize: 13,
		color: colors.textSecondary,
	},
	commentRow: {
		flexDirection: 'row',
		alignItems: 'flex-start',
		gap: spacing.sm,
		paddingVertical: spacing.sm,
	},
	avatar: {
		width: 36,
		height: 36,
		borderRadius: 18,
		backgroundColor: colors.border,
	},
	avatarFallback: {
		width: 36,
		height: 36,
		borderRadius: 18,
		alignItems: 'center',
		justifyContent: 'center',
		backgroundColor: colors.primaryLight,
	},
	avatarInitial: {
		fontSize: 14,
		fontWeight: '700',
		color: colors.text,
	},
	commentBody: {
		flex: 1,
		minWidth: 0,
	},
	commentMeta: {
		flexDirection: 'row',
		alignItems: 'center',
		gap: spacing.sm,
	},
	authorName: {
		maxWidth: '75%',
		fontSize: 13,
		fontWeight: '700',
		color: colors.textSecondary,
	},
	commentDate: {
		fontSize: 11,
		color: colors.textLight,
	},
	commentText: {
		marginTop: 3,
		fontSize: 14,
		lineHeight: 20,
		color: colors.text,
	},
	deleteButton: {
		width: 30,
		height: 30,
		alignItems: 'center',
		justifyContent: 'center',
	},
	composerRow: {
		flexDirection: 'row',
		alignItems: 'center',
		gap: spacing.sm,
		paddingHorizontal: spacing.md,
		paddingVertical: spacing.sm,
		borderTopWidth: StyleSheet.hairlineWidth,
		borderTopColor: colors.border,
	},
	composerAvatar: {
		width: 34,
		height: 34,
		borderRadius: 17,
		backgroundColor: colors.border,
	},
	composerAvatarFallback: {
		width: 34,
		height: 34,
		borderRadius: 17,
		alignItems: 'center',
		justifyContent: 'center',
		backgroundColor: colors.primaryLight,
	},
	input: {
		flex: 1,
		maxHeight: 84,
		minHeight: 42,
		paddingHorizontal: spacing.md,
		paddingVertical: spacing.sm,
		borderRadius: 21,
		backgroundColor: colors.inputBackground,
		color: colors.text,
		fontSize: 14,
	},
	sendButton: {
		width: 38,
		height: 38,
		borderRadius: 19,
		alignItems: 'center',
		justifyContent: 'center',
		backgroundColor: colors.primaryDark,
	},
	sendButtonDisabled: {
		backgroundColor: colors.textLight,
	},
});

export default createStyles;
