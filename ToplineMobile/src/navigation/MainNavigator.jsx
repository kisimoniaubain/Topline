
import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';

import HomeScreen from '../screens/HomeScreen';
import FriendsScreen from '../screens/FriendsScreen';
import CreatePostScreen from '../screens/CreatePostScreen';
import UserProfileScreen from '../screens/UserProfileScreen';

const Stack = createNativeStackNavigator();

export default function MainNavigator() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
      }}
    >
      <Stack.Screen
        name="Home"
        component={HomeScreen}
      />

      <Stack.Screen
        name="Friends"
        component={FriendsScreen}
      />

<Stack.Screen
  name="CreatePost"
  component={CreatePostScreen}
/>

      <Stack.Screen
        name="UserProfile"
        component={UserProfileScreen}
      />
    </Stack.Navigator>
  );
}