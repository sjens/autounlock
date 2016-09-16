package net.simonjensen.autounlock;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NotificationDecisionActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private ArrayList<String> listItems = new ArrayList<String>();
    ArrayAdapter<String> adapter;

    String lock;
    List<BluetoothData> bluetoothDataList;
    List<WifiData> wifiDataList;
    List<LocationData> locationDataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decision);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        lock = intent.getExtras().getString("Lock");
        bluetoothDataList = (List<BluetoothData>) intent.getExtras().getSerializable("BluetoothList");
        wifiDataList = (List<WifiData>) intent.getExtras().getSerializable("WifiList");
        locationDataList = (List<LocationData>) intent.getExtras().getSerializable("LocationList");

        ListView listView = (ListView) findViewById(R.id.decisionListView);
        if (listView != null) {
            listView.setClickable(true);
            listView.setOnItemClickListener(this);

            adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listItems);
            listView.setAdapter(adapter);

            adapter.addAll(
                    "Decrease inner geofence size",
                    "Increase inner geofence size",
                    "Decrease outer geofence size",
                    "Increase outer geofence size",
                    "Redo data collection for lock",
                    "Redo orientation"
            );
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent heuristicsTuner = new Intent("HEURISTICS_TUNER");
        heuristicsTuner.putExtra("Position", position);
        heuristicsTuner.putExtra("Lock", lock);
        heuristicsTuner.putExtra("BluetoothData", (Serializable) bluetoothDataList);
        heuristicsTuner.putExtra("WifiData", (Serializable) wifiDataList);
        heuristicsTuner.putExtra("LocationData", (Serializable) locationDataList);
        sendBroadcast(heuristicsTuner);
        super.moveTaskToBack(true);
    }
}
