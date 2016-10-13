package net.simonjensen.autounlock;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NotificationUtility {
    public static final int NOTIFICATION_ID = 1;

    public static final String ACTION_ADD_ORIENTATION = "action_add_orientation";
    public static final String ACTION_IGNORE_ORIENTATION = "action_ignore_orientation";
    public static final String ACTION_YES = "action_yes";
    public static final String ACTION_NO = "action_no";

    public void displayOrientationNotification(Context context, String lockMAC, float orientation) {
        Intent yesIntent = new Intent(context, NotificationActionService.class)
                .setAction(ACTION_ADD_ORIENTATION);
        yesIntent.putExtra("lock", lockMAC);
        yesIntent.putExtra("orientation", orientation);

        Intent noIntent = new Intent(context, NotificationActionService.class)
                .setAction(ACTION_IGNORE_ORIENTATION);

        // use System.currentTimeMillis() to have adapter unique ID for the pending intent
        PendingIntent pendingYesIntent = PendingIntent.getService(
                context,
                (int) System.currentTimeMillis(),
                yesIntent,
                PendingIntent.FLAG_ONE_SHOT);

        PendingIntent pendingNoIntent = PendingIntent.getService(
                context,
                (int) System.currentTimeMillis(),
                noIntent,
                PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder notificationBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_lock_open_black)
                        .setAutoCancel(true)
                        .setContentTitle("Unlock orientation was recorded")
                        .setContentText("Did you wish to unlock the door?")
                        .addAction(new NotificationCompat.Action(R.drawable.ic_check_black,
                                "Yes", pendingYesIntent))
                        .addAction(new NotificationCompat.Action(R.drawable.ic_close_black,
                                "No", pendingNoIntent))
                        .setVibrate(new long[] {0, 100});

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        Notification notification = notificationBuilder.build();
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    public void displayUnlockNotification(Context context,
                                          String lock,
                                          List<BluetoothData> bluetoothDataList,
                                          List<WifiData> wifiDataList,
                                          List<LocationData> locationDataList) {

        Intent yesIntent = new Intent(context, NotificationActionService.class)
                .setAction(ACTION_YES);

        Intent noIntent = new Intent(context, NotificationActionService.class)
                .setAction(ACTION_NO);
        noIntent.putExtra("Lock", lock);
        noIntent.putExtra("BluetoothList", (Serializable) bluetoothDataList);
        noIntent.putExtra("WifiList", (Serializable) wifiDataList);
        noIntent.putExtra("LocationList", (Serializable) locationDataList);

        // use System.currentTimeMillis() to have adapter unique ID for the pending intent
        PendingIntent pendingYesIntent = PendingIntent.getService(
                context,
                (int) System.currentTimeMillis(),
                yesIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent pendingNoIntent = PendingIntent.getService(
                context,
                (int) System.currentTimeMillis(),
                noIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_lock_open_black)
                        .setAutoCancel(true)
                        .setContentTitle(String.valueOf(lock) + " was unlocked")
                        .setContentText("Was this decision correct?")
                        .addAction(new NotificationCompat.Action(R.drawable.ic_check_black,
                                "Yes", pendingYesIntent))
                        .addAction(new NotificationCompat.Action(R.drawable.ic_close_black,
                                "No", pendingNoIntent))
                        .setVibrate(new long[] {0, 100});

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        Notification notification = notificationBuilder.build();
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    public static class NotificationActionService extends IntentService {
        public NotificationActionService() {
            super(NotificationActionService.class.getSimpleName());
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            String action = intent.getAction();
            Log.d("Notification", "Received notification action: " + action);
            if (ACTION_YES.equals(action)) {
                NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
            } else if (ACTION_NO.equals(action)) {
                NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
                Intent notificationDecision = new Intent(this, NotificationDecisionActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                notificationDecision.putExtras(intent.getExtras());
                startActivity(notificationDecision);
            } else if (ACTION_ADD_ORIENTATION.equals(action)) {
                NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
                Intent addOrientationIntent = new Intent("HEURISTICS_TUNER");
                addOrientationIntent.setAction("ADD_ORIENTATION");
                addOrientationIntent.putExtras(intent.getExtras());
                sendBroadcast(addOrientationIntent);
            } else if (ACTION_IGNORE_ORIENTATION.equals(action)) {
                Intent restartScanner = new Intent("START_SCAN");
                sendBroadcast(restartScanner);
            }
        }
    }
}
