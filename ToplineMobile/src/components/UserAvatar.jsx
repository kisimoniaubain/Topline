import React from 'react';
import {
  View,
  Text,
  Image,
  StyleSheet,
} from 'react-native';
import useThemeStyles from '../theme/useThemeStyles';

const UserAvatar = ({
  uri,
  name = 'User',
  size = 48,
}) => {
  const styles = useThemeStyles(createStyles);
  const initial = name.charAt(0).toUpperCase();

  if (uri) {
    return (
      <Image
        source={{ uri }}
        style={[
          styles.image,
          {
            width: size,
            height: size,
            borderRadius: size / 2,
          },
        ]}
      />
    );
  }

  return (
    <View
      style={[
        styles.placeholder,
        {
          width: size,
          height: size,
          borderRadius: size / 2,
        },
      ]}
    >
      <Text style={[styles.initial, { fontSize: size * 0.4 }]}>
        {initial}
      </Text>
    </View>
  );
};

const createStyles = (colors) => StyleSheet.create({
  image: {
    backgroundColor: colors.border,
  },

  placeholder: {
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },

  initial: {
    color: colors.white,
    fontWeight: '700',
  },
});

export default UserAvatar;