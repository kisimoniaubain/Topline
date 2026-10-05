import React from 'react';
import {
  View,
  Image,
  StyleSheet,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useTheme } from '../context/ThemeContext';

export default function AppHeader() {
  const { colors, mode } = useTheme();

  const logo =
    mode === 'dark'
      ? require('../../assets/logo1.png')
      : require('../../assets/logo.png');

  return (
    <SafeAreaView
      edges={['top']}
      style={[
        styles.safeArea,
        {
          backgroundColor: colors.background,
        },
      ]}
    >
      <View
        style={[
          styles.header,
          {
            backgroundColor: colors.surface,
            borderBottomColor: colors.border,
          },
        ]}
      >
        <Image
          source={logo}
          style={styles.logo}
          resizeMode="contain"
        />
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    width: '100%',
  },

  header: {
    height: 64,
    width: '100%',
    alignItems: 'center',
    justifyContent: 'center',
    borderBottomWidth: StyleSheet.hairlineWidth,
  },

  logo: {
    width: 130,
    height: 42,
  },
});