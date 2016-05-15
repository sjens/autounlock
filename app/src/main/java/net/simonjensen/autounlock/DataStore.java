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

    static final String BLUETOOTH_TABLE = "bluetooth";
    static final String BLUETOOTH_NAME = "name";
    static final String BLUETOOTH_SOURCE = "Source";
    static final String BLUETOOTH_RSSI = "RSSI";

    static final String WIFI_TABLE = "wifi";
    static final String WIFI_SSID = "SSID";
    static final String WIFI_MAC = "MAC";
    static final String WIFI_RSSI = "RSSI";

    static final String ACCELEROMETER_TABLE = "accelerometer";
    static final String ACCELEROMETER_X = "accelerometer_x";
    static final String ACCELEROMETER_Y = "accelerometer_y";
    static final String ACCELEROMETER_Z = "accelerometer_z";
    static final String ROTATION_X = "rotation_x";
    static final String ROTATION_Y = "rotation_y";
    static final String ROTATION_Z = "rotation_z";

    static final String LOCATION_TABLE = "location";
    static final String LOCATION_PROVIDER = "provider";
    static final String LOCATION_LATITUDE = "latitude";
    static final String LOCATION_LONGITUDE = "longitude";
    static final String LOCATION_ACCURACY = "accuracy";

    private SQLiteDatabase database;
    private DatabaseHelper databaseHelper;

    public DataStore(Context context) {
        databaseHelper = new DatabaseHelper(context);
    }

    public void close() {
        database.close();
    }

    public void newDatastore() {
        database = databaseHelper.getWritableDatabase();

        database.delete(BLUETOOTH_TABLE, null, null);
        database.delete(WIFI_TABLE, null, null);
        database.delete(ACCELEROMETER_TABLE, null, null);
        database.delete(LOCATION_TABLE, null, null);

        database.close();
    }

    public void insertBtle(String name, String btleSource, int btleRSSI, long timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(BLUETOOTH_NAME, name);
        contentValues.put(BLUETOOTH_SOURCE, btleSource);
        contentValues.put(BLUETOOTH_RSSI, btleRSSI);
        contentValues.put(TIMESTAMP, timestamp);
        database = databaseHelper.getWritableDatabase();
        database.replace(BLUETOOTH_TABLE, null, contentValues);
        database.close();
    }

    public void insertWifi(String wifiSSID, String wifiMAC, String wifiRSSI, long timestamp) {
        ContentValues contentValues = new ContentValues();
        //contentValues.put(ID, id);
        contentValues.put(WIFI_SSID, wifiSSID);
        contentValues.put(WIFI_MAC, wifiMAC);
        contentValues.put(WIFI_RSSI, wifiRSSI);
        contentValues.put(TIMESTAMP, timestamp);
        database = databaseHelper.getWritableDatabase();
        database.replace(WIFI_TABLE, null, contentValues);
        database.close();
    }

    public void insertAccelerometer(String accelerometerX, String accelerometerY, String accelerometerZ,
                                    String rotationX, String rotationY, String rotationZ, long timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ACCELEROMETER_X, accelerometerX);
        contentValues.put(ACCELEROMETER_Y, accelerometerY);
        contentValues.put(ACCELEROMETER_Z, accelerometerZ);
        contentValues.put(ROTATION_X, rotationX);
        contentValues.put(ROTATION_Y, rotationY);
        contentValues.put(ROTATION_Z, rotationZ);
        contentValues.put(TIMESTAMP, timestamp);
        database = databaseHelper.getWritableDatabase();
        database.replace(ACCELEROMETER_TABLE, null, contentValues);
        database.close();
    }

    public void insertLocation(String provider, double latitude, double longitude, float accuracy, long timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(LOCATION_PROVIDER, provider);
        contentValues.put(LOCATION_LATITUDE, latitude);
        contentValues.put(LOCATION_LONGITUDE, longitude);
        contentValues.put(LOCATION_ACCURACY, accuracy);
        contentValues.put(TIMESTAMP, timestamp);
        database = databaseHelper.getWritableDatabase();
        database.replace(LOCATION_TABLE, null, contentValues);
        database.close();
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
                    + WIFI_RSSI + " TEXT, "
                    + TIMESTAMP + " LONG)");

            database.execSQL("CREATE TABLE " + ACCELEROMETER_TABLE + " ("
                    + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + ACCELEROMETER_X + " TEXT, "
                    + ACCELEROMETER_Y + " TEXT, "
                    + ACCELEROMETER_Z + " TEXT, "
                    + ROTATION_X + " TEXT, "
                    + ROTATION_Y + " TEXT, "
                    + ROTATION_Z + " TEXT, "
                    + TIMESTAMP + " LONG)");

            database.execSQL("CREATE TABLE " + LOCATION_TABLE + " ("
                    + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + LOCATION_PROVIDER + " TEXT, "
                    + LOCATION_LATITUDE + " FLOAT, "
                    + LOCATION_LONGITUDE + " FLOAT, "
                    + LOCATION_ACCURACY + " FLOAT, "
                    + TIMESTAMP + " LONG)");
        }

        private void dropDatastore() {
            database.execSQL("DROP TABLE IF EXISTS " + BLUETOOTH_TABLE);
            database.execSQL("DROP TABLE IF EXISTS " + WIFI_TABLE);
            database.execSQL("DROP TABLE IF EXISTS " + ACCELEROMETER_TABLE);
            database.execSQL("DROP TABLE IF EXISTS " + LOCATION_TABLE);
        }
    }
}
