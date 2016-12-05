package mpc.ut.uisandbox;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Aurelius on 12/1/16.
 */

public class BtConnectThread extends Thread {

    private static final String TAG = SettingsActivity.class.getName();
    public SettingsActivity mainAct = null;

    public BtConnectThread(SettingsActivity topLevel, BluetoothDevice btDevice) {
        mainAct = topLevel;
        Log.d(TAG, "initializing bt connect thread");

        try {
            BluetoothTech.clientSideSocket = btDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());

        }

    }

    @Override
    public void run() {
        // Keep listening until exception occurs or a socket is returned
        while(!isInterrupted()) {

            Log.d(TAG, "running bt connect thread");
            try {
                BluetoothTech.clientSideSocket.connect();
                Log.d(TAG, "bt connection to server succeeded");
                SettingsActivity.ctLiveConnection = new BtClientConnection(BluetoothTech.clientSideSocket);
                break;
            } catch (IOException e) {
                Log.d(TAG, "bt connection to server failed");
                Log.e(TAG, e.getMessage());
            }
//            try {
//                sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }

        }
    }
}
