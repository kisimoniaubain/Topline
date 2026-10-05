import React, { useEffect, useState } from 'react';
import {
	ActivityIndicator,
	Alert,
	FlatList,
	Image,
	KeyboardAvoidingView,
	Modal,
	Platform,
	Pressable,
	SafeAreaView,
	Text,
	TextInput,
	TouchableOpacity,
	View,
} from 'react-native';
import { MessageCircle, Send, Trash2, X } from 'lucide-react-native';

import { useAuth } from '../context/AuthContext';
import { languageLocales, useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import { addComment, deleteComment, getComments } from '../services/commentService';
import useThemeStyles from '../theme/useThemeStyles';
import createStyles from './CommentsScreen.css';

const getId = (value) => String(value?._id || value?.id || '');

const formatDate = (value, language, t) => {
	const date = new Date(value);
	if (Number.isNaN(date.getTime())) return t('Just now');
	return date.toLocaleDateString(languageLocales[language] || 'en', { month: 'short', day: 'numeric' });
};

export default function CommentsScreen({
	visible,
	post,
	onClose,
	onCommentAdded,
	onCommentCountChanged,
}) {
	const styles = useThemeStyles(createStyles);
	const { colors } = useTheme();
	const { language, t } = useLanguage();
	const { user, token } = useAuth();
	const postId = post?.id;
	const localSample = post?.localSample;
	const [comments, setComments] = useState(() => post?.localComments || []);
	const [commentCount, setCommentCount] = useState(() => post?.comments || 0);
	const [text, setText] = useState('');
	const [loading, setLoading] = useState(() => Boolean(post && !post.localSample));
	const [submitting, setSubmitting] = useState(false);
	const [deletingId, setDeletingId] = useState(null);
	const [error, setError] = useState('');

	useEffect(() => {
		if (!visible || !postId) return undefined;

		if (localSample) {
			return undefined;
		}

		let active = true;

		getComments(postId)
			.then((data) => {
				if (!active) return;
				const loadedComments = data.comments || [];
				setComments(loadedComments);
				setCommentCount(data.commentsCount ?? loadedComments.length);
			})
			.catch((loadError) => {
				if (active) setError(t(loadError.message || 'Unable to load comments.'));
			})
			.finally(() => {
				if (active) setLoading(false);
			});

		return () => {
			active = false;
		};
	}, [visible, postId, localSample, t]);

	const handleSubmit = async () => {
		const message = text.trim();
		if (!message || submitting) return;

		setSubmitting(true);
		setError('');

		try {
			if (post.localSample) {
				const localComment = {
					id: `local-${Date.now()}`,
					text: message,
					createdAt: new Date().toISOString(),
					author: user,
				};
				const nextComments = [...comments, localComment];
				setComments(nextComments);
				setCommentCount(nextComments.length);
				setText('');
				onCommentAdded?.(post.id, localComment, nextComments.length, nextComments);
				return;
			}

			const data = await addComment(post.id, message, token);
			const nextComments = [...comments, data.comment];
			setComments(nextComments);
			setCommentCount(data.commentsCount);
			setText('');
			onCommentAdded?.(post.id, data.comment, data.commentsCount);
		} catch (submitError) {
			setError(t(submitError.message || 'Unable to add comment.'));
		} finally {
			setSubmitting(false);
		}
	};

	const confirmDelete = (comment) => {
		Alert.alert(t('Delete comment?'), t('This comment will be removed from the video.'), [
			{ text: t('Cancel'), style: 'cancel' },
			{
				text: t('Delete'),
				style: 'destructive',
				onPress: () => handleDelete(comment),
			},
		]);
	};

	const handleDelete = async (comment) => {
		const commentId = getId(comment);
		setDeletingId(commentId);
		setError('');

		try {
			let nextComments;
			let nextCount;

			if (post.localSample) {
				nextComments = comments.filter((entry) => getId(entry) !== commentId);
				nextCount = nextComments.length;
			} else {
				const data = await deleteComment(commentId, token);
				nextComments = comments.filter((entry) => getId(entry) !== commentId);
				nextCount = data.commentsCount;
			}

			setComments(nextComments);
			setCommentCount(nextCount);
			onCommentCountChanged?.(post.id, nextCount, post.localSample ? nextComments : undefined);
		} catch (deleteError) {
			setError(t(deleteError.message || 'Unable to delete comment.'));
		} finally {
			setDeletingId(null);
		}
	};

	const renderComment = ({ item }) => {
		const author = item.author || {};
		const isOwnComment = getId(author) === getId(user);

		return (
			<View style={styles.commentRow}>
				{author.profilePicture ? (
					<Image source={{ uri: author.profilePicture }} style={styles.avatar} />
				) : (
					<View style={styles.avatarFallback}>
						<Text style={styles.avatarInitial}>
							{(author.name || author.username || '?').charAt(0).toUpperCase()}
						</Text>
					</View>
				)}
				<View style={styles.commentBody}>
					<View style={styles.commentMeta}>
						<Text style={styles.authorName} numberOfLines={1}>
							{author.name || (author.username ? `@${author.username}` : t('Topline user'))}
						</Text>
						<Text style={styles.commentDate}>{formatDate(item.createdAt, language, t)}</Text>
					</View>
					<Text style={styles.commentText}>{item.text}</Text>
				</View>
				{isOwnComment ? (
					<TouchableOpacity
						style={styles.deleteButton}
						onPress={() => confirmDelete(item)}
						disabled={deletingId === getId(item)}
						accessibilityRole="button"
						accessibilityLabel={t('Delete your comment')}
					>
						{deletingId === getId(item) ? (
							<ActivityIndicator size="small" color={colors.textLight} />
						) : (
							<Trash2 size={17} color={colors.textLight} />
						)}
					</TouchableOpacity>
				) : null}
			</View>
		);
	};

	return (
		<Modal
			visible={visible}
			transparent
			animationType="slide"
			statusBarTranslucent
			onRequestClose={onClose}
		>
			<View style={styles.overlay}>
				<Pressable style={styles.backdrop} onPress={onClose} />
				<KeyboardAvoidingView
					style={styles.keyboardArea}
					behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
				>
					<SafeAreaView style={styles.sheet}>
						<View style={styles.handle} />
						<View style={styles.header}>
							<View style={styles.headerTitleWrap}>
								<Text style={styles.title}>{t('Comments')}</Text>
								<Text style={styles.count}>{commentCount}</Text>
							</View>
							<TouchableOpacity
								onPress={onClose}
								style={styles.closeButton}
								accessibilityRole="button"
								accessibilityLabel={t('Close comments')}
							>
								<X size={22} color={colors.text} />
							</TouchableOpacity>
						</View>

						{error ? <Text style={styles.errorText}>{error}</Text> : null}

						{loading ? (
							<View style={styles.loadingState}>
								<ActivityIndicator color={colors.primary} />
							</View>
						) : (
							<FlatList
								data={comments}
								renderItem={renderComment}
								keyExtractor={(item, index) => getId(item) || `comment-${index}`}
								contentContainerStyle={comments.length ? styles.commentList : styles.emptyList}
								keyboardShouldPersistTaps="handled"
								ListEmptyComponent={(
									<View style={styles.emptyState}>
										<MessageCircle size={26} color={colors.textLight} />
										<Text style={styles.emptyTitle}>{t('No comments yet')}</Text>
										<Text style={styles.emptyText}>{t('Start the conversation.')}</Text>
									</View>
								)}
							/>
						)}

						<View style={styles.composerRow}>
							{user?.profilePicture ? (
								<Image source={{ uri: user.profilePicture }} style={styles.composerAvatar} />
							) : (
								<View style={styles.composerAvatarFallback}>
									<Text style={styles.avatarInitial}>
										{(user?.name || user?.username || 'U').charAt(0).toUpperCase()}
									</Text>
								</View>
							)}
							<TextInput
								value={text}
								onChangeText={setText}
								placeholder={t('Add a comment...')}
								placeholderTextColor={colors.textLight}
								style={[
									styles.input,
									language === 'ar' && { textAlign: 'right', writingDirection: 'rtl' },
								]}
								multiline
								maxLength={1000}
								editable={!submitting}
								returnKeyType="send"
								onSubmitEditing={handleSubmit}
							/>
							<TouchableOpacity
								onPress={handleSubmit}
								disabled={!text.trim() || submitting}
								style={[styles.sendButton, (!text.trim() || submitting) && styles.sendButtonDisabled]}
								accessibilityRole="button"
								accessibilityLabel={t('Post comment')}
							>
								{submitting ? (
									<ActivityIndicator size="small" color={colors.white} />
								) : (
									<Send size={17} color={colors.white} />
								)}
							</TouchableOpacity>
						</View>
					</SafeAreaView>
				</KeyboardAvoidingView>
			</View>
		</Modal>
	);
}
