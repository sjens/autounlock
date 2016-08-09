package net.simonjensen.autounlock;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DataStore {
    static final String DATABASE_NAME = "datastore.db";
    static final int DATABASE_VERSION = 1;

    static final String ID = "ID";
    static final String TIMESTAMP = "TIMESTAMP";

    static final String LOCK_TABLE = "lock";
    static final String LOCK_MAC = "MAC";
    static final String LOCK_PASSPHRASE = "passphrase";
    static final String LOCK_INNER_GEOFENCE = "inner_geofence";
    static final String LOCK_OUTER_GEOFENCE = "outer_geofence";

    static final String BLUETOOTH_TABLE = "bluetooth";
    static final String BLUETOOTH_NAME = "name";
    static final String BLUETOOTH_SOURCE = "Source";
    static final String BLUETOOTH_RSSI = "RSSI";

    static final String WIFI_TABLE = "wifi";
    static final String WIFI_SSID = "SSID";
    static final String WIFI_MAC = "MAC";
    static final String WIFI_RSSI = "RSSI";

    static final String ACCELEROMETER_TABLE = "accelerometer";
    static final String ACCELERATION_X = "acceleration_x";
    static final String ACCELERATION_Y = "acceleration_y";
    static final String ACCELERATION_Z = "acceleration_z";
    static final String SPEED_X = "speed_x";
    static final String SPEED_Y = "speed_y";
    static final String SPEED_Z = "speed_z";

    static final String LOCATION_TABLE = "location";
    static final String LOCATION_PROVIDER = "provider";
    static final String LOCATION_LATITUDE = "latitude";
    static final String LOCATION_LONGITUDE = "longitude";
    static final String LOCATION_ACCURACY = "accuracy";

    static final String DECISION_TABLE = "decision";
    static final String DECISION_DECISION = "decision";

    static final String BUFFER_TABLE = "buffer";
    static final String DATA = "data";

    private SQLiteDatabase database;
    private DatabaseHelper databaseHelper;

    public DataStore(Context context) {
        databaseHelper = new DatabaseHelper(context);
    }

    public void close() {
        database.close();
    }

    public void deleteDatastore() {
        database = databaseHelper.getWritableDatabase();
        database.delete(BLUETOOTH_TABLE, null, null);
        database.delete(WIFI_TABLE, null, null);
        database.delete(ACCELEROMETER_TABLE, null, null);
        database.delete(LOCATION_TABLE, null, null);
        database.delete(DECISION_TABLE, null, null);
        database.delete(BUFFER_TABLE, null, null);
        database.close();
    }

    public void insertLockDetails(String lockMAC, String lockPassphrase, int lockInnerGeofence, int lockOuterGeofence, long timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(LOCK_MAC, lockMAC);
        contentValues.put(LOCK_PASSPHRASE, lockPassphrase);
        contentValues.put(LOCK_INNER_GEOFENCE, lockInnerGeofence);
        contentValues.put(LOCK_OUTER_GEOFENCE, lockOuterGeofence);
        contentValues.put(TIMESTAMP, timestamp);

        try {
            database = databaseHelper.getWritableDatabase();
            database.beginTransaction();
            database.replace(LOCK_TABLE, null, contentValues);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public LockData getLockDetails(String lockMAC) {
        LockData lockData;

        try {
            database = databaseHelper.getReadableDatabase();
            database.beginTransaction();
        } finally {
            database.endTransaction();
        }

        lockData = new LockData(null, null, null, 0, 0, null, null);
        return  lockData;
    }

    public void insertBtle(String name, String btleSource, int btleRSSI, long timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(BLUETOOTH_NAME, name);
        contentValues.put(BLUETOOTH_SOURCE, btleSource);
        contentValues.put(BLUETOOTH_RSSI, btleRSSI);
        contentValues.put(TIMESTAMP, timestamp);

        try {
            database = databaseHelper.getWritableDatabase();
            database.beginTransaction();
            database.replace(BLUETOOTH_TABLE, null, contentValues);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public void insertWifi(String wifiSSID, String wifiMAC, int wifiRSSI, long timestamp) {
        ContentValues contentValues = new ContentValues();
        //contentValues.put(ID, id);
        contentValues.put(WIFI_SSID, wifiSSID);
        contentValues.put(WIFI_MAC, wifiMAC);
        contentValues.put(WIFI_RSSI, wifiRSSI);
        contentValues.put(TIMESTAMP, timestamp);

        try {
            database = databaseHelper.getWritableDatabase();
            database.beginTransaction();
            database.replace(WIFI_TABLE, null, contentValues);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public void insertAccelerometer(float accelerometerX, float accelerometerY, float accelerometerZ,
                                    float speedX, float speedY, float speedZ, long timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ACCELERATION_X, accelerometerX);
        contentValues.put(ACCELERATION_Y, accelerometerY);
        contentValues.put(ACCELERATION_Z, accelerometerZ);
        contentValues.put(SPEED_X, speedX);
        contentValues.put(SPEED_Y, speedY);
        contentValues.put(SPEED_Z, speedZ);
        contentValues.put(TIMESTAMP, timestamp);

        try {
            database = databaseHelper.getWritableDatabase();
            database.beginTransaction();
            database.replace(ACCELEROMETER_TABLE, null, contentValues);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public void insertLocation(String provider, double latitude, double longitude, float accuracy, long timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(LOCATION_PROVIDER, provider);
        contentValues.put(LOCATION_LATITUDE, latitude);
        contentValues.put(LOCATION_LONGITUDE, longitude);
        contentValues.put(LOCATION_ACCURACY, accuracy);
        contentValues.put(TIMESTAMP, timestamp);

        try {
            database = databaseHelper.getWritableDatabase();
            database.beginTransaction();
            database.replace(LOCATION_TABLE, null, contentValues);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public void insertDecision(int decision, long timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DECISION_DECISION, decision);
        contentValues.put(TIMESTAMP, timestamp);

        try {
            database = databaseHelper.getWritableDatabase();
            database.beginTransaction();
            database.replace(DECISION_TABLE, null, contentValues);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public void insertBuffer(long timestamp, String data) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(TIMESTAMP, timestamp);
        contentValues.put(DATA, data);

        try {
            database = databaseHelper.getWritableDatabase();
            database.beginTransaction();
            database.replace(BUFFER_TABLE, null, contentValues);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            createDatastore(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
            // Production-quality upgrade code should modify the tables when
            // the database version changes instead of dropping the tables and
            // re-creating them.
            if (newVersion != DATABASE_VERSION) {
                Log.w("Datastore", "Database upgrade from old: " + oldVersion + " to: " +
                        newVersion);
                database = databaseHelper.getWritableDatabase();
                dropDatastore();
                createDatastore(database);
                database.close();
                return;
            }
        }

        private void createDatastore(SQLiteDatabase database) {
            database.execSQL("CREATE TABLE " + LOCK_TABLE + " ("
                    + LOCK_MAC + " TEXT PRIMARY KEY, "
                    + LOCK_PASSPHRASE + " TEXT, "
                    + LOCK_INNER_GEOFENCE + " TEXT, "
                    + LOCK_OUTER_GEOFENCE + " INTEGER, "
                    + TIMESTAMP + " LONG)");

            database.execSQL("CREATE TABLE " + BLUETOOTH_TABLE + " ("
                    + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + BLUETOOTH_NAME + " TEXT, "
                    + BLUETOOTH_SOURCE + " TEXT, "
                    + BLUETOOTH_RSSI + " INTEGER, "
                    + TIMESTAMP + " LONG)");

            database.execSQL("CREATE TABLE " + WIFI_TABLE + " ("
                    + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + WIFI_SSID + " TEXT, "
                    + WIFI_MAC + " TEXT, "
                    + WIFI_RSSI + " INTEGER, "
                    + TIMESTAMP + " LONG)");

            database.execSQL("CREATE TABLE " + ACCELEROMETER_TABLE + " ("
                    + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + ACCELERATION_X + " FLOAT, "
                    + ACCELERATION_Y + " FLOAT, "
                    + ACCELERATION_Z + " FLOAT, "
                    + SPEED_X + " FLOAT, "
                    + SPEED_Y + " FLOAT, "
                    + SPEED_Z + " FLOAT, "
                    + TIMESTAMP + " LONG)");

            database.execSQL("CREATE TABLE " + LOCATION_TABLE + " ("
                    + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + LOCATION_PROVIDER + " TEXT, "
                    + LOCATION_LATITUDE + " FLOAT, "
                    + LOCATION_LONGITUDE + " FLOAT, "
                    + LOCATION_ACCURACY + " FLOAT, "
                    + TIMESTAMP + " LONG)");

            database.execSQL("CREATE TABLE " + DECISION_TABLE + " ("
                    + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + DECISION_DECISION + " INTEGER, "
                    + TIMESTAMP + " LONG)");

            database.execSQL("CREATE TABLE " + BUFFER_TABLE + " ("
                    + TIMESTAMP + " LONG PRIMARY KEY, "
                    + DATA + " TEXT)");
        }

        private void dropDatastore() {
            database.execSQL("DROP TABLE IF EXISTS " + LOCK_TABLE);
            database.execSQL("DROP TABLE IF EXISTS " + BLUETOOTH_TABLE);
            database.execSQL("DROP TABLE IF EXISTS " + WIFI_TABLE);
            database.execSQL("DROP TABLE IF EXISTS " + ACCELEROMETER_TABLE);
            database.execSQL("DROP TABLE IF EXISTS " + LOCATION_TABLE);
            database.execSQL("DROP TABLE IF EXISTS " + DECISION_TABLE);
            database.execSQL("DROP TABLE IF EXISTS " + BUFFER_TABLE);
        }
    }
}
