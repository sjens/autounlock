package net.simonjensen.autounlock;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;

public class GeofenceService extends IntentService {

    private static String TAG = "GeofenceService";

    public GeofenceService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = getErrorString(this, geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            ArrayList triggeringLockList = new ArrayList();
            for (Geofence geofence : triggeringGeofences) {
                String lockMAC = geofence.getRequestId().substring(5);
                if (!triggeringLockList.contains(lockMAC)) {
                    triggeringLockList.add(lockMAC);
                }
            }

            Intent geofencesEntered = new Intent("GEOFENCES_ENTERED");
            geofencesEntered.putExtra("Geofences", triggeringLockList);
            sendBroadcast(geofencesEntered);
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT){
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            ArrayList triggeringLockList = new ArrayList();
            for (Geofence geofence : triggeringGeofences) {
                String lockMAC = geofence.getRequestId().substring(5);
                if (!triggeringLockList.contains(lockMAC)) {
                    triggeringLockList.add(lockMAC);
                }
            }
            Intent geofencesExited = new Intent("GEOFENCES_EXITED");
            geofencesExited.putExtra("Geofences", triggeringLockList);
            sendBroadcast(geofencesExited);
        } else {
            Log.e(TAG, getString(R.string.geofence_transition_invalid_type, geofenceTransition));
        }
    }

    private static String getErrorString(Context context, int errorCode) {
        Resources mResources = context.getResources();
        switch (errorCode) {
            case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                return mResources.getString(R.string.geofence_not_available);
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                return mResources.getString(R.string.geofence_too_many_geofences);
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                return mResources.getString(R.string.geofence_too_many_pending_intents);
            default:
                return mResources.getString(R.string.unknown_geofence_error);
        }
    }
}
