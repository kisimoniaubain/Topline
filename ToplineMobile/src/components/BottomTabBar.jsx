import React from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
} from 'react-native';
import { spacing } from '../theme';
import { useLanguage } from '../context/LanguageContext';
import useThemeStyles from '../theme/useThemeStyles';

const BottomTabBar = ({
  activeTab,
  onTabPress,
}) => {
  const styles = useThemeStyles(createStyles);
  const { t } = useLanguage();
  const tabs = [
    { name: 'Home', label: t('Home') },
    { name: 'Profile', label: t('Profile') },
    { name: 'Settings', label: t('Settings') },
  ];

  return (
    <View style={styles.container}>
      {tabs.map((tab) => {
        const active = activeTab === tab.name;

        return (
          <TouchableOpacity
            key={tab.name}
            style={styles.tab}
            onPress={() => onTabPress(tab.name)}
            activeOpacity={0.7}
          >
            <Text
              style={[
                styles.label,
                active && styles.activeLabel,
              ]}
            >
              {tab.label}
            </Text>
          </TouchableOpacity>
        );
      })}
    </View>
  );
};

const createStyles = (colors) => StyleSheet.create({
  container: {
    flexDirection: 'row',
    backgroundColor: colors.surface,
    borderTopWidth: 1,
    borderTopColor: colors.border,
    paddingVertical: spacing.sm,
  },

  tab: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: spacing.sm,
  },

  label: {
    fontSize: 13,
    fontWeight: '500',
    color: colors.textSecondary,
  },

  activeLabel: {
    color: colors.primary,
    fontWeight: '700',
  },
});

export default BottomTabBar;