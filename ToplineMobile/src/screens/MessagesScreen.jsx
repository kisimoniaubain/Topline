import React from 'react';
import {
	SafeAreaView,
	Text,
	TouchableOpacity,
	View,
} from 'react-native';
import { ArrowLeft, MessageCircle, Users } from 'lucide-react-native';

import { useTheme } from '../context/ThemeContext';
import { useLanguage } from '../context/LanguageContext';
import useThemeStyles from '../theme/useThemeStyles';
import createStyles from './MessagesScreen.css';

export default function MessagesScreen({ navigation, route }) {
	const styles = useThemeStyles(createStyles);
	const { colors } = useTheme();
	const { t } = useLanguage();
	const contact = route?.params?.user;

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

				<Text style={styles.headerTitle} numberOfLines={1}>
					{contact?.name || t('Messages')}
				</Text>

				<View style={styles.headerButton} />
			</View>

			<View style={styles.emptyState}>
				<View style={styles.iconBadge}>
					<MessageCircle size={30} color={colors.primary} />
				</View>

				<Text style={styles.emptyTitle}>
					{contact ? t('No messages yet') : t('No conversations yet')}
				</Text>
				<Text style={styles.emptyDescription}>
					{contact
						? t('Your conversation with {name} will appear here.', { name: contact.name || t('this person') })
						: t('Your conversations will appear here.')}
				</Text>

				{!contact ? (
					<TouchableOpacity
						style={styles.actionButton}
						onPress={() => navigation.navigate('Friends')}
						activeOpacity={0.8}
					>
						<Users size={18} color={colors.white} />
						<Text style={styles.actionText}>{t('Find people')}</Text>
					</TouchableOpacity>
				) : null}
			</View>
		</SafeAreaView>
	);
}
