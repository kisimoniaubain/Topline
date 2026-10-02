import React from 'react';
import {
	SafeAreaView,
	Text,
	TouchableOpacity,
	View,
} from 'react-native';
import { ArrowLeft, Bell } from 'lucide-react-native';

import { colors } from '../theme';
import styles from './NotificationsScreen.css';

export default function NotificationsScreen({ navigation }) {
	return (
		<SafeAreaView style={styles.container}>
			<View style={styles.header}>
				<TouchableOpacity
					style={styles.headerButton}
					onPress={() => navigation.goBack()}
					accessibilityRole="button"
					accessibilityLabel="Go back"
				>
					<ArrowLeft size={23} color={colors.text} />
				</TouchableOpacity>

				<Text style={styles.headerTitle}>Alerts</Text>

				<View style={styles.headerButton} />
			</View>

			<View style={styles.emptyState}>
				<View style={styles.iconBadge}>
					<Bell size={30} color={colors.primary} />
				</View>

				<Text style={styles.emptyTitle}>You’re all caught up</Text>
				<Text style={styles.emptyDescription}>
					Likes, replies, and follows will show up here.
				</Text>

				<TouchableOpacity
					style={styles.actionButton}
					onPress={() => navigation.navigate('Home')}
					activeOpacity={0.8}
				>
					<Text style={styles.actionText}>Back to feed</Text>
				</TouchableOpacity>
			</View>
		</SafeAreaView>
	);
}
