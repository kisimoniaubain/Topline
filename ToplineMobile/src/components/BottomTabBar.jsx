import React from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
} from 'react-native';
import { colors, spacing } from '../theme';

const BottomTabBar = ({
  activeTab,
  onTabPress,
}) => {
  const tabs = [
    { name: 'Home', label: 'Home' },
    { name: 'Profile', label: 'Profile' },
    { name: 'Settings', label: 'Settings' },
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

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    backgroundColor: colors.white,
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