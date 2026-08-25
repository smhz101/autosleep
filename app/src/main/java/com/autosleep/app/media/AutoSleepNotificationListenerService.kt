package com.autosleep.app.media

import android.service.notification.NotificationListenerService

/**
 * Grants AutoSleep access to active media sessions after the user explicitly enables
 * Notification Access in Android settings. Notification contents are not inspected.
 */
class AutoSleepNotificationListenerService : NotificationListenerService()
