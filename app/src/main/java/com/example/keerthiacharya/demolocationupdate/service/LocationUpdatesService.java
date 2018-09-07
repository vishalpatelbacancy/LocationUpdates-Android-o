package com.example.keerthiacharya.demolocationupdate.service;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.example.keerthiacharya.demolocationupdate.MainActivity;
import com.example.keerthiacharya.demolocationupdate.MyApp;
import com.example.keerthiacharya.demolocationupdate.R;

import com.example.keerthiacharya.demolocationupdate.common.Utils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

/**
 * Created by Ayush Jain on 8/31/17.
 */

public class LocationUpdatesService extends Service {

    private static final String TAG = LocationUpdatesService.class.getSimpleName();

    public static final String ACTION_BROADCAST = LocationUpdatesService.class.getPackage().getName() + ".broadcast";
    public static final String EXTRA_LOCATION = LocationUpdatesService.class.getPackage().getName() + ".location";
    public static final String EXTRA_DATA = LocationUpdatesService.class.getPackage().getName() + ".data";

    private static final String EXTRA_STARTED_FROM_NOTIFICATION = LocationUpdatesService.class.getPackage().getName() +
            ".started_from_notification";

    private static final int NOTIFICATION_ID = 1008;

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private final IBinder mBinder = new LocationBinder();

    /**
     * Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Callback for changes in location.
     */
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;
    private Location mLocation;

    private Handler mServiceHandler;
    private NotificationManager mNotificationManager;
    private boolean mChangingConfiguration = false;

    //    private io.socket.client.Socket mSocket;
    private String tripID = "";
    private String statusSocket = "";

    private MyApp myApp;
    private Socket mSocket;

    @Override
    public void onCreate() {
        super.onCreate();
//        init FusedLocationProviderClient
        myApp = (MyApp) getApplication();
        mSocket = myApp.getSocket();

        connectCall();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
//        init and set LocationCallBack
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                Intent intent = new Intent(ACTION_BROADCAST);
                mLocation = locationResult.getLastLocation();
                Log.d("mlocationResult", "::" + mLocation);
                intent.putExtra(EXTRA_LOCATION, locationResult.getLastLocation() + " :: ");
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);


                // Update notification content if running as a foreground service.
                if (serviceIsRunningInForeground(LocationUpdatesService.this)) {
                    mNotificationManager.notify(NOTIFICATION_ID, getNotification());
                    sendSocket();
                } else {
                    Log.e("dddd--", "serviceIsRunningInForeground");
                    sendSocket();
                }
            }

            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                super.onLocationAvailability(locationAvailability);
//                locationAvailability.isLocationAvailable();
            }
        };


//        create a request for location
        createLocationRequest();
        getLastLocation();

        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());

//        HandlerThread handlerThread = new HandlerThread(TAG);
//        handlerThread.start();
//        mServiceHandler = new Handler(handlerThread.getLooper());
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

    }

    private void connectCall() {

        mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {

                onMessage("EVENT_CONNECT");

            }
        }).on(Socket.EVENT_CONNECTING, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                onMessage("EVENT_CONNECTING");
            }
        }).on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {

                Log.e("Data-->", "" + args[0].toString());

                onMessage("EVENT_CONNECT_ERROR");
            }
        }).on(Socket.EVENT_CONNECT_TIMEOUT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                onMessage("EVENT_CONNECT_TIMEOUT");
            }
        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                onMessage("EVENT_DISCONNECT");
            }
        });

        // Receiving an object
        mSocket.on("testing_location", new Emitter.Listener() {
            @Override
            public void call(Object... args) {

//                JSONObject obj = (JSONObject) args[0];
                if (args != null && args.length > 0)
                    Log.e("data", "" + args[args.length - 1]);

                Intent intent = new Intent(ACTION_BROADCAST);
                Log.d("mlocationResult", "::" + args[args.length - 1]);
                intent.putExtra(EXTRA_DATA, args[args.length - 1] + "");
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);


            }
        });
        if (!mSocket.connected())
            mSocket.connect();
    }


    private void onMessage(final String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                Toast.makeText(LocationUpdatesService.this, message, Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service started");
        boolean startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION,
                false);

        // on tap of remove update from notification
        if (startedFromNotification) {
            removeLocationUpdates();
            stopSelf();
        }
        // Tells the system to not try to recreate the service after it has been killed.
        return START_NOT_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }

    @NonNull
    @Override
    public IBinder onBind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) comes to the foreground
        // and binds with this service. The service should cease to be a foreground service
        // when that happens.
        Log.i(TAG, "in onBind()");
        stopForeground(true);
        mChangingConfiguration = false;
        return mBinder;
    }


    @Override
    public void onRebind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) returns to the foreground
        // and binds once again with this service. The service should cease to be a foreground
        // service when that happens.
        Log.i(TAG, "in onRebind()");
        stopForeground(true);
        mChangingConfiguration = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "Last client unbound from service");

        // Called when the last client (MainActivity in case of this sample) unbinds from this
        // service. If this method is called due to a configuration change in MainActivity, we
        // do nothing. Otherwise, we make this service a foreground service.
        if (!mChangingConfiguration && Utils.requestingLocationUpdates(this)) {
            Log.i(TAG, "Starting foreground service");
            /*
            // TODO(developer). If targeting O, use the following code.
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
                mNotificationManager.startServiceInForeground(new Intent(this,
                        LocationUpdatesService.class), NOTIFICATION_ID, getNotification());
            } else {
                startForeground(NOTIFICATION_ID, getNotification());
            }
             */
            startForeground(NOTIFICATION_ID, getNotification());
        }
        return true; // Ensures onRebind() is called when a client re-binds.
    }

    private Notification getNotification() {
        Intent intent = new Intent(this, LocationUpdatesService.class);

        CharSequence text = Utils.getLocationText(mLocation);

        // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

        // The PendingIntent that leads to a call to onStartCommand() in this service.
        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // The PendingIntent to launch activity.
        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification noti;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            String CHANNEL_ID = "dopplet_01";// The id of the channel.
            CharSequence name = getString(R.string.app_name);// The user-visible name of the channel.
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mChannel.setSound(null, null);
            notificationManager.createNotificationChannel(mChannel);
            noti = new Notification.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Location App")
                    .setContentText(mLocation.getLatitude() + " " + mLocation.getLongitude() + statusSocket)
                    .build();
            return noti;
        } else {
            return new NotificationCompat.Builder(this)
                    .addAction(R.drawable.ic_launch, getString(R.string.launch_activity),
                            activityPendingIntent)
                    .addAction(R.drawable.ic_cancel, getString(R.string.remove_location_updates),
                            servicePendingIntent)
                    .setContentText(text)
                    .setContentTitle(mLocation.getLatitude() + " " + mLocation.getLongitude() + statusSocket)
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setTicker(text)
                    .setWhen(System.currentTimeMillis()).build();
        }
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void getLastLocation() {
        try {
            mFusedLocationClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                mLocation = task.getResult();
                                Log.d("mlocationLast", "::" + mLocation);
                            } else {
                                Log.e("Error", "Failed to get location.");
                            }
                        }
                    });
        } catch (SecurityException e) {
            Log.e("Exception", "Last location permission." + e);
        }
    }

    public void removeLocationUpdates() {
        Log.i(TAG, "Removing location updates");
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            Utils.setRequestingLocationUpdates(LocationUpdatesService.this, false);
            stopSelf();
        } catch (SecurityException unlikely) {
            Utils.setRequestingLocationUpdates(LocationUpdatesService.this, true);
            Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
        }
    }

    public void requestLocationUpdates() {
        Log.i(TAG, "Requesting location updates");
        Utils.setRequestingLocationUpdates(LocationUpdatesService.this, true);
        startService(new Intent(getApplicationContext(), LocationUpdatesService.class));
        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, Looper.myLooper());
        } catch (SecurityException unlikely) {
            Utils.setRequestingLocationUpdates(LocationUpdatesService.this, false);
            Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
        }
    }

    public class LocationBinder extends Binder {
        public LocationUpdatesService getLocationUpdateService() {
            return LocationUpdatesService.this;
        }
    }

    public boolean serviceIsRunningInForeground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
                Integer.MAX_VALUE)) {
            if (getClass().getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onDestroy() {
        mServiceHandler.removeCallbacksAndMessages(null);
    }


    private void sendSocket() {

        if (mSocket != null && mSocket.connected()) {
            final JSONObject obj = new JSONObject();
            try {
                obj.put("id", "124");
                obj.put("lat", mLocation.getLatitude() + "");
                obj.put("lng", mLocation.getLongitude() + "");
                obj.put("oid", tripID);
                obj.put("roleId", "10");

                Log.d("socket", "socket updated::" + obj.toString());


                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    public void run() {
                        Toast.makeText(LocationUpdatesService.this, "sendSocket \n" + obj.toString(), Toast.LENGTH_SHORT).show();
                    }
                });

                mSocket.emit("testing_location", obj.toString());
//                if (Pref.getValue(this, StringLabels.WhereLeft, "").equalsIgnoreCase(START_TRIP) ||
//                        Pref.getValue(this, StringLabels.WhereLeft, "").equalsIgnoreCase(PICKUP_TRIP)) {
//                    mSocket.emit("hefty_live_location", obj);
//                }
            } catch (JSONException e) {
                e.printStackTrace();
            }


        }


    }


}
