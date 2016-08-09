package net.simonjensen.autounlock;

import android.location.Location;

import java.util.ArrayList;

class LockData {
    private String MAC;
    private String passphrase;
    private LocationData location;
    private int innerGeofence;
    private int outerGeofence;
    private ArrayList<BluetoothData> nearbyBluetoothDevices;
    private ArrayList<WifiData> nearbyWifiAccessPoints;

    LockData(String MAC, String passphrase, LocationData location, int innerGeofence, int outerGeofence, ArrayList<BluetoothData> nearbyBluetoothDevices, ArrayList<WifiData> nearbyWifiAccessPoints) {
        this.MAC = MAC;
        this.passphrase = passphrase;
        this.location = location;
        this.innerGeofence = innerGeofence;
        this.outerGeofence = outerGeofence;
        this.nearbyBluetoothDevices = nearbyBluetoothDevices;
        this.nearbyWifiAccessPoints = nearbyWifiAccessPoints;
    }

    public String getMAC() {
        return MAC;
    }

    public void setMAC(String MAC) {
        this.MAC = MAC;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    public LocationData getLocation() {
        return location;
    }

    public void setLocation(LocationData location) {
        this.location = location;
    }

    public int getInnerGeofence() {
        return innerGeofence;
    }

    public void setInnerGeofence(int innerGeofence) {
        this.innerGeofence = innerGeofence;
    }

    public int getOuterGeofence() {
        return outerGeofence;
    }

    public void setOuterGeofence(int outerGeofence) {
        this.outerGeofence = outerGeofence;
    }

    public ArrayList<BluetoothData> getNearbyBluetoothDevices() {
        return nearbyBluetoothDevices;
    }

    public void setNearbyBluetoothDevices(ArrayList<BluetoothData> nearbyBluetoothDevices) {
        this.nearbyBluetoothDevices = nearbyBluetoothDevices;
    }

    public ArrayList<WifiData> getNearbyWifiAccessPoints() {
        return nearbyWifiAccessPoints;
    }

    public void setNearbyWifiAccessPoints(ArrayList<WifiData> nearbyWifiAccessPoints) {
        this.nearbyWifiAccessPoints = nearbyWifiAccessPoints;
    }

    @Override
    public String toString() {
        return "LockData{" +
                "MAC='" + MAC + '\'' +
                ", passphrase='" + passphrase + '\'' +
                ", location=" + location +
                ", innerGeofence=" + innerGeofence +
                ", outerGeofence=" + outerGeofence +
                ", nearbyBluetoothDevices=" + nearbyBluetoothDevices +
                ", nearbyWifiAccessPoints=" + nearbyWifiAccessPoints +
                '}';
    }
}

