package mpc.ut.uisandbox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.bluelinelabs.logansquare.LoganSquare;
import com.peak.salut.Callbacks.SalutCallback;
import com.peak.salut.Callbacks.SalutDataCallback;
import com.peak.salut.Callbacks.SalutDeviceCallback;
import com.peak.salut.Salut;
import com.peak.salut.SalutDataReceiver;
import com.peak.salut.SalutDevice;
import com.peak.salut.SalutServiceData;

import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Timer;

/**
 * Created by Aurelius on 12/3/16.
 */

public class WifiDirectTech  {

    private static final String TAG = WifiDirectTech.class.getName();

    public static WifiManager wifiManager = null;
    public static SalutDataReceiver dataReceiver = null;
    public static SalutServiceData serviceData = null;
    public static Salut network = null;

    public static Set<SalutDevice> salutDevices = new HashSet<>();

    public static Boolean registered = false;
    public static Boolean host = false;

    public static Boolean advertising = false;
    public static Boolean discovery = false;

    public static boolean initializeWifiDirect() {
        Log.d(TAG, "Initializing WifiDirect networking");

        wifiManager = (WifiManager) SettingsActivity.topActivity.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            Log.d(TAG, "Since wifi disabled, exit");
            return false;
        }

        discovery = false;
        advertising = false;
        dataReceiver = new SalutDataReceiver(SettingsActivity.topActivity, SettingsActivity.topActivity);
        serviceData = new SalutServiceData("wdService", 50489, SettingsActivity.localMacAddress);
        network = new Salut(dataReceiver, serviceData, new SalutCallback() {
            @Override
            public void call() {
                Log.e(TAG, "Sorry, but this device does not support WiFi Direct.");
            }
        });


        IntentFilter intentFilter =  new IntentFilter();
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        SettingsActivity.topActivity.registerReceiver(wifiReceiver, intentFilter);


        if (SettingsActivity.appType.equals("doctor")) {

            Log.d(TAG, "This is doctor app (so it is the host)");
            host = true;
            startAdvertising();
        } else {
            startDiscovery();
        }
        return true;
    }

    public static void startAdvertising() {
        if (!advertising) {
            network.startNetworkService(new SalutDeviceCallback() {
                @Override
                public void call(SalutDevice device) {
                    Log.d(TAG, device.readableName + " has connected!");

                }
            });
            advertising = true;
        }
    }

    public static void startDiscovery() {
        if (!network.isConnectedToAnotherDevice && !discovery) {
            network.discoverNetworkServices(new SalutDeviceCallback() {
                @Override
                public void call(SalutDevice device) {
                    Log.d(TAG, "A device has connected with the name " + device.deviceName);
                    salutDevices.add(device);
                    for (SalutDevice dev : salutDevices) {
                        Log.d(TAG, "devices: " + dev.deviceName);
                    }
                    Log.d(TAG, "size: " + salutDevices.size());
                    Log.d(TAG, "devices: " + salutDevices.toString());

                    connectNeighbor(device);

                }
            }, true);
            discovery = true;
        }
    }

    public static void connectNeighbor(SalutDevice device) {
        network.registerWithHost(device, new SalutCallback() {
            @Override
            public void call() {
                Log.d(TAG, "We're now registered.");
                //stopDiscovery();
                registered = true;
            }
        }, new SalutCallback() {
            @Override
            public void call() {
                registered = false;
                Log.d(TAG, "We failed to register.");
            }
        });
    }

    public static void sendToHost(Patient pat) {
        Log.d(TAG, "sendToHost");
        network.sendToHost(pat, new SalutCallback() {
            @Override
            public void call() {
                Log.e(TAG, "Oh no! The data failed to send.");
            }
        });
    }



    public static void cleanup() {
        if (network!=null) {
            if(SettingsActivity.appType.equals(SettingsActivity.PATIENT_APP) && network != null) {
                Log.d(TAG, "Unregistering client...");
                network.unregisterClient(null, null, false);
            }
            if (SettingsActivity.appType.equals(SettingsActivity.DOCTOR_APP)  && network!=null) {
                Log.d(TAG, "Stopping network service...");
                network.stopNetworkService(false);
            }
        }
    }

    public static BroadcastReceiver wifiReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {

                if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
                    Log.d(TAG, "Wifi is enabled");

                    //notifyAvailable();
                    return;
                }

                if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
                    Log.d(TAG, "Wifi is enabling");
                    initializeWifiDirect();
                    //notifyAvailable();
                    return;
                }

                if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
                    Log.d(TAG, "Wifi is disabled");
                    //notifyUnavailable();
                    return;
                }

                if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLING) {
                    Log.d(TAG, "Wifi is disabling");
                    //notifyUnavailable();
                    return;
                }

            }
        }
    };

}
