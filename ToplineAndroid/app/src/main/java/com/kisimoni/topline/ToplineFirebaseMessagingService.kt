package com.kisimoni.topline

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kisimoni.topline.data.local.UserPreferences

class ToplineFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        UserPreferences(applicationContext).savePushToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title?.takeIf(String::isNotBlank) ?: "Topline"
        val body = message.notification?.body?.takeIf(String::isNotBlank) ?: return
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        ToplineNotifications.show(this, title, body)
    }
}
