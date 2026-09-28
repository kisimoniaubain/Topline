import React from 'react';
import {
  View,
  Image,
  StyleSheet,
  useColorScheme,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { colors } from '../theme';

export default function AppHeader() {
  const colorScheme = useColorScheme();

  const logo =
    colorScheme === 'dark'
      ? require('../../assets/logo1.png')
      : require('../../assets/logo.png');

  return (
    <SafeAreaView
      edges={['top']}
      style={[
        styles.safeArea,
        {
          backgroundColor:
            colorScheme === 'dark'
              ? colors.black
              : colors.white,
        },
      ]}
    >
      <View
        style={[
          styles.header,
          {
            backgroundColor:
              colorScheme === 'dark'
                ? colors.black
                : colors.white,
            borderBottomColor:
              colorScheme === 'dark'
                ? colors.borderDark
                : colors.border,
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