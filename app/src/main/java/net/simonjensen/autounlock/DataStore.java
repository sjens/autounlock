package net.simonjensen.autounlock;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DataStore extends SQLiteOpenHelper {
    static final String id="ID";
    static final String timestamp="timestamp";

    static final String btleTable="btle";
    static final String btleRSSI="RSSI";
    static final String btleSource="Source";

    static final String wifiTable="wifi";
    static final String wifiSSID="SSID";
    static final String wifiMAC="MAC";
    static final String wifiRSSI="RSSI";

    static final String accelTable="accelerometer";
    static final String accelVector="vector";
    static final String accelVelocity="velocity";

    static final String magnetTable="magnetometer";
    static final String magnetDegree="degree";

    static final String locationTable="location";
    static final String locationLatitude="latitude";
    static final String locationLongitude="longitude";
    static final String locationPrecision="precision";

    public DataStore(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + btleTable + " ("
                + id + " INTEGER PRIMARY KEY, "
                + btleRSSI + " TEXT, "
                + btleSource + " TEXT, "
                + timestamp + " TEXT)");

        db.execSQL("CREATE TABLE " + wifiTable + " ("
                + id + " INTEGER PRIMARY KEY, "
                + wifiSSID + " TEXT, "
                + wifiMAC + " TEXT, "
                + wifiRSSI + " TEXT, "
                + timestamp + " TEXT)");

        db.execSQL("CREATE TABLE " + accelTable + " ("
                + id + " INTEGER PRIMARY KEY, "
                + accelVector + " TEXT, "
                + accelVelocity + " TEXT, "
                + timestamp + " TEXT)");

        db.execSQL("CREATE TABLE " + magnetTable + " ("
                + id + " INTEGER PRIMARY KEY, "
                + magnetDegree + " TEXT, "
                + timestamp + " TEXT)");

        db.execSQL("CREATE TABLE " + locationTable + " ("
                + id + " INTEGER PRIMARY KEY, "
                + locationLatitude + " TEXT, "
                + locationLongitude + " TEXT, "
                + locationPrecision + " TEXT, "
                + timestamp + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
