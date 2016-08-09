package net.simonjensen.autounlock;

import android.location.Location;

import java.util.ArrayList;

public class LockData {
    String MAC;
    String passphrase;
    Location location;
    int innerGeofence;
    int outerGeofence;
    ArrayList<BluetoothData> nearbyBluetoothDevices;
    ArrayList<WifiData> nearbyWifiAccessPoints;

    public LockData(String MAC, String passphrase, Location location, int innerGeofence, int outerGeofence, ArrayList<BluetoothData> nearbyBluetoothDevices, ArrayList<WifiData> nearbyWifiAccessPoints) {
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

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
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

