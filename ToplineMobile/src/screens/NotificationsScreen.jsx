import React from 'react';
import {
	SafeAreaView,
	Text,
	TouchableOpacity,
	View,
} from 'react-native';
import { ArrowLeft, Bell } from 'lucide-react-native';

import { useTheme } from '../context/ThemeContext';
import { useLanguage } from '../context/LanguageContext';
import useThemeStyles from '../theme/useThemeStyles';
import createStyles from './NotificationsScreen.css';

export default function NotificationsScreen({ navigation }) {
	const styles = useThemeStyles(createStyles);
	const { colors } = useTheme();
	const { t } = useLanguage();
	return (
		<SafeAreaView style={styles.container}>
			<View style={styles.header}>
				<TouchableOpacity
					style={styles.headerButton}
					onPress={() => navigation.goBack()}
					accessibilityRole="button"
					accessibilityLabel={t('Go back')}
				>
					<ArrowLeft size={23} color={colors.text} />
				</TouchableOpacity>

				<Text style={styles.headerTitle}>{t('Alerts')}</Text>

				<View style={styles.headerButton} />
			</View>

			<View style={styles.emptyState}>
				<View style={styles.iconBadge}>
					<Bell size={30} color={colors.primary} />
				</View>

				<Text style={styles.emptyTitle}>{t('You’re all caught up')}</Text>
				<Text style={styles.emptyDescription}>
					{t('Likes, replies, and follows will show up here.')}
				</Text>

				<TouchableOpacity
					style={styles.actionButton}
					onPress={() => navigation.navigate('Home')}
					activeOpacity={0.8}
				>
					<Text style={styles.actionText}>{t('Back to feed')}</Text>
				</TouchableOpacity>
			</View>
		</SafeAreaView>
	);
}
