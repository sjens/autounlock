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

import java.util.ArrayList;
import java.util.List;

public class NotificationDecisionActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private ArrayList<String> listItems = new ArrayList<String>();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decision);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        List<BluetoothData> bluetoothDataList = (List<BluetoothData>) intent.getExtras().getSerializable("bluetoothList");
        List<WifiData> wifiDataList = (List<WifiData>) intent.getExtras().getSerializable("wifiList");
        List<LocationData> locationDataList = (List<LocationData>) intent.getExtras().getSerializable("locationList");

        Log.d("test", bluetoothDataList.toString());

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
                    "Door opened too early",
                    "Door opened too late"
            );
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent heuristicsTuner = new Intent("HEURISTICS_TUNER");
        heuristicsTuner.putExtra("Option", position);
        sendBroadcast(heuristicsTuner);
        super.moveTaskToBack(true);
    }
}
