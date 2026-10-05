
import React from 'react';
import { StyleSheet, View } from 'react-native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Bell, Home, MessageCircle, Plus, User, Users } from 'lucide-react-native';
import { useTheme } from '../context/ThemeContext';
import { useLanguage } from '../context/LanguageContext';

import HomeScreen from '../screens/HomeScreen';
import FriendsScreen from '../screens/FriendsScreen';
import CreatePostScreen from '../screens/CreatePostScreen';
import UserProfileScreen from '../screens/UserProfileScreen';
import SettingsScreen from '../screens/SettingsScreen';
import EditProfileScreen from '../screens/EditProfileScreen';
import MessagesScreen from '../screens/MessagesScreen';
import NotificationsScreen from '../screens/NotificationsScreen';

const Tab = createBottomTabNavigator();

const hiddenTabOptions = {
  tabBarButton: () => null,
  tabBarItemStyle: { display: 'none' },
};

export default function MainNavigator() {
  const { colors } = useTheme();
  const { t } = useLanguage();

  return (
    <Tab.Navigator
      initialRouteName="Home"
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: colors.textSecondary,
        tabBarStyle: {
          height: 64,
          backgroundColor: colors.surface,
          borderTopColor: colors.border,
        },
        tabBarLabelStyle: {
          fontSize: 10,
          fontWeight: '600',
        },
        tabBarIcon: ({ color, size, focused }) => {
          const iconProps = { color, size: size ?? 23 };
          switch (route.name) {
            case 'Home':
              return <Home {...iconProps} fill={focused ? color : 'transparent'} />;
            case 'Friends':
              return <Users {...iconProps} />;
            case 'CreatePost':
              return (
                <View style={[styles.createButton, { backgroundColor: colors.primaryDark }]}>
                  <Plus size={22} color={colors.textOnPrimary} strokeWidth={2.5} />
                </View>
              );
            case 'Notifications':
              return <Bell {...iconProps} />;
            case 'Messages':
              return <MessageCircle {...iconProps} />;
            case 'UserProfile':
              return <User {...iconProps} />;
            default:
              return null;
          }
        },
      })}
    >
      <Tab.Screen name="Home" component={HomeScreen} options={{ title: t('Home') }} />
      <Tab.Screen name="Friends" component={FriendsScreen} options={{ title: t('People') }} />
      <Tab.Screen name="CreatePost" component={CreatePostScreen} options={{ title: t('Create') }} />
      <Tab.Screen name="Messages" component={MessagesScreen} options={{ title: t('Inbox') }} />
      <Tab.Screen name="UserProfile" component={UserProfileScreen} options={{ title: t('Profile') }} />
      <Tab.Screen name="Notifications" component={NotificationsScreen} options={{ ...hiddenTabOptions, title: t('Alerts') }} />
      <Tab.Screen name="Settings" component={SettingsScreen} options={{ ...hiddenTabOptions, title: t('Settings') }} />
      <Tab.Screen name="EditProfile" component={EditProfileScreen} options={hiddenTabOptions} />
    </Tab.Navigator>
  );
}

const styles = StyleSheet.create({
  createButton: {
    width: 42,
    height: 30,
    borderRadius: 7,
    alignItems: 'center',
    justifyContent: 'center',
  },
});