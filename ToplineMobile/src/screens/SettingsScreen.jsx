import React from 'react';
import {
  SafeAreaView,
  View,
  Text,
  TouchableOpacity,
  Alert,
  Switch,
  Modal,
  Pressable,
} from 'react-native';
import { Check, ChevronRight, X } from 'lucide-react-native';

import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { useLanguage } from '../context/LanguageContext';
import useThemeStyles from '../theme/useThemeStyles';
import createStyles from './SettingsScreen.css';

export default function SettingsScreen({ navigation }) {
  const styles = useThemeStyles(createStyles);
  const { colors, isDark, toggleTheme } = useTheme();
  const { language, setLanguage, supportedLanguages, t } = useLanguage();
  const { user, logout } = useAuth();
  const [languagePickerVisible, setLanguagePickerVisible] = React.useState(false);
  const selectedLanguage = supportedLanguages.find((item) => item.code === language);

  const handleLogout = () => {
    Alert.alert(
      t('Log out'),
      t('Are you sure you want to log out?'),
      [
        {
          text: t('Cancel'),
          style: 'cancel',
        },
        {
          text: t('Log Out'),
          style: 'destructive',
          onPress: async () => {
            await logout();
          },
        },
      ]
    );
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity
          style={styles.backButton}
          onPress={() => navigation.goBack()}
          activeOpacity={0.8}
        >
          <Text style={styles.backText}>{t('Back')}</Text>
        </TouchableOpacity>

        <Text style={styles.title}>{t('Settings')}</Text>
        <View style={styles.spacer} />
      </View>

      <View style={styles.card}>
        <Text style={styles.sectionLabel}>{t('Account')}</Text>

        <Text style={styles.userName}>
          {user?.name || t('Topline User')}
        </Text>

        <Text style={styles.userEmail}>
          {user?.email || t('No email')}
        </Text>
      </View>

      <View style={styles.themeCard}>
        <View style={styles.themeCopy}>
          <Text style={styles.sectionLabel}>{t('Appearance')}</Text>
          <Text style={styles.themeTitle}>{t(isDark ? 'Dark mode' : 'Light mode')}</Text>
          <Text style={styles.themeDescription}>{t('Applies across the entire app')}</Text>
        </View>
        <Switch
          value={isDark}
          onValueChange={toggleTheme}
          trackColor={{ false: colors.border, true: colors.primaryDark }}
          thumbColor={colors.white}
          accessibilityLabel={t('Toggle dark mode')}
        />
      </View>

      <TouchableOpacity
        style={styles.languageCard}
        onPress={() => setLanguagePickerVisible(true)}
        activeOpacity={0.8}
        accessibilityRole="button"
        accessibilityLabel={t('Choose your preferred language')}
      >
        <View style={styles.themeCopy}>
          <Text style={styles.sectionLabel}>{t('Language')}</Text>
          <Text style={styles.themeTitle}>{selectedLanguage?.nativeName}</Text>
          <Text style={styles.themeDescription}>{t('Choose your preferred language')}</Text>
        </View>
        <ChevronRight size={20} color={colors.textSecondary} />
      </TouchableOpacity>

      <TouchableOpacity
        style={styles.primaryButton}
        onPress={() => navigation.navigate('EditProfile')}
        activeOpacity={0.85}
      >
        <Text style={styles.primaryButtonText}>{t('Edit Profile')}</Text>
      </TouchableOpacity>

      <TouchableOpacity
        style={styles.logoutButton}
        onPress={handleLogout}
        activeOpacity={0.85}
      >
        <Text style={styles.logoutText}>{t('Log Out')}</Text>
      </TouchableOpacity>

      <Modal
        visible={languagePickerVisible}
        transparent
        animationType="fade"
        onRequestClose={() => setLanguagePickerVisible(false)}
      >
        <Pressable
          style={styles.languageBackdrop}
          onPress={() => setLanguagePickerVisible(false)}
        >
          <Pressable style={styles.languageDialog} onPress={() => {}}>
            <View style={styles.languageDialogHeader}>
              <Text style={styles.languageDialogTitle}>{t('Select language')}</Text>
              <TouchableOpacity
                onPress={() => setLanguagePickerVisible(false)}
                accessibilityRole="button"
                accessibilityLabel={t('Close language selection')}
                style={styles.languageCloseButton}
              >
                <X size={20} color={colors.text} />
              </TouchableOpacity>
            </View>
            {supportedLanguages.map((item) => (
              <TouchableOpacity
                key={item.code}
                style={styles.languageOption}
                onPress={() => {
                  setLanguage(item.code);
                  setLanguagePickerVisible(false);
                }}
                accessibilityRole="radio"
                accessibilityState={{ checked: language === item.code }}
              >
                <Text style={styles.languageOptionText}>{item.nativeName}</Text>
                {language === item.code ? <Check size={19} color={colors.primary} /> : null}
              </TouchableOpacity>
            ))}
          </Pressable>
        </Pressable>
      </Modal>
    </SafeAreaView>
  );
}
