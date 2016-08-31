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
import java.util.List;

public class NotificationUtils {
    public static final int NOTIFICATION_ID = 1;

    public static final String ACTION_YES = "action_yes";
    public static final String ACTION_NO = "action_no";

    private static List<BluetoothData> bluetoothDataList;
    private static List<WifiData> wifiDataList;
    private static List<LocationData> locationDataList;

    public void displayNotification(Context context,
                                    List<BluetoothData> bluetoothDataList,
                                    List<WifiData> wifiDataList,
                                    List<LocationData> locationDataList) {

        this.bluetoothDataList = bluetoothDataList;
        this.wifiDataList = wifiDataList;
        this.locationDataList = locationDataList;

        Intent yesIntent = new Intent(context, NotificationActionService.class)
                .setAction(ACTION_YES);
        Intent noIntent = new Intent(context, NotificationActionService.class)
                .setAction(ACTION_NO);

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
                        .setContentTitle("Unlock decision was made")
                        .setContentText("decide")
                        .addAction(new NotificationCompat.Action(R.drawable.ic_check_black,
                                "Yes", pendingYesIntent))
                        .addAction(new NotificationCompat.Action(R.drawable.ic_close_black,
                                "No", pendingNoIntent))
                        .setVibrate(new long[] {0, 1000, 1000, 1000});

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
                // TODO: handle action 1.
                // If you want to cancel the notification: NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
                Heuristics heuristics = new Heuristics();
            } else if (ACTION_NO.equals(action)) {
                Intent notificationDecision = new Intent(this, NotificationDecisionActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                notificationDecision.putExtra("bluetoothList", (Serializable) bluetoothDataList);
                notificationDecision.putExtra("wifiList", (Serializable) wifiDataList);
                notificationDecision.putExtra("locationList", (Serializable) locationDataList);
                startActivity(notificationDecision);
            }
        }
    }
}
