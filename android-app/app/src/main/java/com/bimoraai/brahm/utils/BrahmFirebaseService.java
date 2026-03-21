package com.bimoraai.brahm.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.bimoraai.brahm.R;
import com.bimoraai.brahm.ui.main.MainActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Handles incoming Firebase Cloud Messaging (FCM) push notifications.
 *
 * Notifications sent from the backend (or Firebase Console) are displayed
 * as heads-up notifications. Tapping any notification opens MainActivity.
 *
 * Channel: "brahm_channel" — "Brahm AI Alerts" (HIGH importance).
 */
public class BrahmFirebaseService extends FirebaseMessagingService {

    private static final String CHANNEL_ID   = "brahm_channel";
    private static final String CHANNEL_NAME = "Brahm AI Alerts";
    private static final int    NOTIF_ID     = 1001;

    // ── Token lifecycle ───────────────────────────────────────────────────────

    /**
     * Called when FCM assigns or refreshes the device token.
     * TODO: POST the new token to /api/auth/fcm-token so the backend can
     *       target this device for personalised alerts (e.g. Rahu Kaal reminders).
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // TODO: API call — POST /api/auth/fcm-token  { "token": token }
        // Retrieve the current user's auth token from PrefsHelper if needed.
    }

    // ── Incoming messages ─────────────────────────────────────────────────────

    /**
     * Called when a data or notification message arrives while the app is
     * in the foreground. For background messages, the system tray handles display.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "Brahm AI";
        String body  = "You have a new update.";

        // Prefer the notification payload if present
        if (remoteMessage.getNotification() != null) {
            RemoteMessage.Notification notif = remoteMessage.getNotification();
            if (notif.getTitle() != null && !notif.getTitle().isEmpty()) {
                title = notif.getTitle();
            }
            if (notif.getBody() != null && !notif.getBody().isEmpty()) {
                body = notif.getBody();
            }
        }

        // Fall back to data payload keys if notification payload was empty
        if (remoteMessage.getData().containsKey("title")) {
            title = remoteMessage.getData().get("title");
        }
        if (remoteMessage.getData().containsKey("body")) {
            body = remoteMessage.getData().get("body");
        }

        showNotification(title, body);
    }

    // ── Notification builder ──────────────────────────────────────────────────

    private void showNotification(String title, String body) {
        // Ensure the channel exists (required on Android O+)
        createChannelIfNeeded();

        // Tap action — open MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_star)          // replace with your app icon
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent);

        NotificationManager nm =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIF_ID, builder.build());
        }
    }

    private void createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Alerts from Brahm AI — dashas, muhurtas, Rahu Kaal");
            channel.enableLights(true);
            channel.enableVibration(true);

            NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }
}
