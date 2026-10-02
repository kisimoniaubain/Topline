import { StyleSheet } from 'react-native';

import { colors, spacing, typography } from '../theme';

const styles = StyleSheet.create({
	container: {
		flex: 1,
		backgroundColor: colors.background,
	},

	header: {
		height: 60,
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

	headerTitle: {
		flex: 1,
		marginHorizontal: spacing.sm,
		textAlign: 'center',
		fontSize: 18,
		fontWeight: '700',
		color: colors.text,
	},

	emptyState: {
		flex: 1,
		alignItems: 'center',
		justifyContent: 'center',
		paddingHorizontal: spacing.xl,
		paddingBottom: spacing.xxl,
	},

	iconBadge: {
		width: 72,
		height: 72,
		alignItems: 'center',
		justifyContent: 'center',
		backgroundColor: '#FFF1E3',
		borderRadius: 36,
	},

	emptyTitle: {
		marginTop: spacing.lg,
		fontSize: 21,
		fontWeight: '700',
		color: colors.text,
		textAlign: 'center',
	},

	emptyDescription: {
		maxWidth: 300,
		marginTop: spacing.sm,
		fontSize: typography.body.fontSize,
		lineHeight: 21,
		color: colors.textSecondary,
		textAlign: 'center',
	},

	actionButton: {
		minHeight: 44,
		flexDirection: 'row',
		alignItems: 'center',
		justifyContent: 'center',
		gap: spacing.sm,
		marginTop: spacing.lg,
		paddingHorizontal: spacing.lg,
		backgroundColor: colors.primary,
		borderRadius: 8,
	},

	actionText: {
		fontSize: 14,
		fontWeight: '700',
		color: colors.white,
	},
});

export default styles;
