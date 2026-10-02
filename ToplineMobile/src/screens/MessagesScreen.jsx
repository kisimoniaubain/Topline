import React from 'react';
import {
	SafeAreaView,
	Text,
	TouchableOpacity,
	View,
} from 'react-native';
import { ArrowLeft, MessageCircle, Users } from 'lucide-react-native';

import { colors } from '../theme';
import styles from './MessagesScreen.css';

export default function MessagesScreen({ navigation, route }) {
	const contact = route?.params?.user;

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

				<Text style={styles.headerTitle} numberOfLines={1}>
					{contact?.name || 'Messages'}
				</Text>

				<View style={styles.headerButton} />
			</View>

			<View style={styles.emptyState}>
				<View style={styles.iconBadge}>
					<MessageCircle size={30} color={colors.primary} />
				</View>

				<Text style={styles.emptyTitle}>
					{contact ? 'No messages yet' : 'No conversations yet'}
				</Text>
				<Text style={styles.emptyDescription}>
					{contact
						? `Your conversation with ${contact.name || 'this person'} will appear here.`
						: 'Your conversations will appear here.'}
				</Text>

				{!contact ? (
					<TouchableOpacity
						style={styles.actionButton}
						onPress={() => navigation.navigate('Friends')}
						activeOpacity={0.8}
					>
						<Users size={18} color={colors.white} />
						<Text style={styles.actionText}>Find people</Text>
					</TouchableOpacity>
				) : null}
			</View>
		</SafeAreaView>
	);
}
