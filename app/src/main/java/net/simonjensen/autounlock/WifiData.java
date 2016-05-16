package net.simonjensen.autounlock;

public class WifiData {
    String ssid;
    String mac;
    int rssi;

    public WifiData(String ssid, String mac, int rssi) {
        this.ssid = ssid;
        this.mac = mac;
        this.rssi = rssi;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    @Override
    public String toString() {
        return "WifiData{" +
                "ssid='" + ssid + '\'' +
                ", mac='" + mac + '\'' +
                ", rssi=" + rssi +
                '}';
    }
}
