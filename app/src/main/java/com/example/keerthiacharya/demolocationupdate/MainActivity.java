package com.example.keerthiacharya.demolocationupdate;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.keerthiacharya.demolocationupdate.common.Utils;
import com.example.keerthiacharya.demolocationupdate.receiver.LocationReceiver;
import com.example.keerthiacharya.demolocationupdate.service.LocationUpdatesService;
import com.example.keerthiacharya.demolocationupdate.service.LocationUpdatesServiceOld;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1001;

    private Button btnStartUpdate, btnStopUpdate;
    private LocationReceiver rcvMReceiver;
    private LocationUpdatesService mLUService;
    private LocationReceiver myReceiver;
    LinearLayout ll;
    ScrollView scrollView;

    // Tracks the bound state of the service.
    private boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ll = findViewById(R.id.ll);
        scrollView = findViewById(R.id.scrollView);

        if (!checkPermission()) {
            requestPermissions();
        }

        myReceiver = new LocationReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
//                    Location location = intent.getParcelableExtra(LocationUpdatesService.EXTRA_LOCATION);
                String data = intent.getStringExtra(LocationUpdatesService.EXTRA_DATA);
                if (data != null) {
                    addText(data+ "\n");
                }
                super.onReceive(context, intent);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermission()) {
            if (mLUService == null)
                mLUService = new LocationUpdatesService();



            btnStartUpdate = (Button) findViewById(R.id.request_location_updates_button);
            btnStopUpdate = (Button) findViewById(R.id.remove_location_updates_button);

            btnStartUpdate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!checkPermission()) {
                        requestPermissions();
                    } else {
                        mLUService.requestLocationUpdates();
                    }
                }
            });

            btnStopUpdate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mLUService.removeLocationUpdates();
                }
            });

            // Restore the state of the buttons when the activity (re)launches.
            changeButtonsState(Utils.requestingLocationUpdates(this));

            // Bind to the service. If the service is in foreground mode, this signals to the service
            // that since this activity is in the foreground, the service can exit foreground mode.
            bindService(new Intent(this, LocationUpdatesService.class), mServiceConection,
                    Context.BIND_AUTO_CREATE);

            LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver,
                    new IntentFilter(LocationUpdatesService.ACTION_BROADCAST));

        }
    }


    public void addText(String str) {
        Random rnd = new Random();
        int currentStrokeColor = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        TextView text = new TextView(this);
        text.setText(str);
        text.setTextColor(currentStrokeColor);
        ll.addView(text);
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mBound) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            unbindService(mServiceConection);
            mBound = false;
        }

        super.onStop();
    }

    private boolean checkPermission() {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void requestPermissions() {
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (shouldProvideRationale) {
            Snackbar.make(
                    findViewById(R.id.activity_main),
                    R.string.permission_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    })
                    .show();
        } else
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS_REQUEST_CODE:
                if (grantResults.length <= 0) {
                    // Permission was not granted.
                } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted.
                    if (mLUService == null)
                        mLUService = new LocationUpdatesService();
//                    mLUService.requestLocationUpdates();
                } else {
                    // Permission denied.
                    changeButtonsState(false);
                    Snackbar.make(
                            findViewById(R.id.activity_main),
                            R.string.permission_denied_explanation,
                            Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.settings, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    // Build intent that displays the App settings screen.
                                    Intent intent = new Intent();
                                    intent.setAction(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package",
                                            BuildConfig.APPLICATION_ID, null);
                                    intent.setData(uri);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                }
                            })
                            .show();
                }
        }
    }

    private void changeButtonsState(boolean requestingLocationUpdates) {
        if (requestingLocationUpdates) {
            btnStartUpdate.setEnabled(false);
            btnStopUpdate.setEnabled(true);
        } else {
            btnStartUpdate.setEnabled(true);
            btnStopUpdate.setEnabled(false);
        }
    }

    private final ServiceConnection mServiceConection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mLUService = ((LocationUpdatesService.LocationBinder) service).getLocationUpdateService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mLUService = null;
            mBound = false;
        }
    };
}
