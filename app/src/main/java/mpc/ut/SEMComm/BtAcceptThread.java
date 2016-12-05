package mpc.ut.SEMComm;

import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Aurelius on 12/1/16.
 */

public class BtAcceptThread extends Thread {
    private static final String TAG = SettingsActivity.class.getName();
    public SettingsActivity mainAct = null;

    public BtAcceptThread(SettingsActivity topLevel) {
        mainAct = topLevel;
        Log.d(TAG, "initializing bt accept thread");
        try {
            BluetoothTech.btServerSocket = BluetoothTech.btAdapter.listenUsingInsecureRfcommWithServiceRecord("BT_SERVER", UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public void run() {
        // Keep listening until exception occurs or a socket is returned
        while(!isInterrupted()) {
            Log.d(TAG, "running bt accept thread");
            try {
                Log.d(TAG, "before accept");
                BluetoothTech.serverSideSocket = BluetoothTech.btServerSocket.accept();

            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
                break;
            }
            // If a connection was accepted
            if(BluetoothTech.serverSideSocket != null) {
                // transfer the data here
                Log.d(TAG, "Connection accepted!");
                SettingsActivity.ctLiveThread = new BtServerConnectionThread(BluetoothTech.serverSideSocket);
                SettingsActivity.ctLiveThread.start();
                Log.d(TAG, "Started connection thread on server side");

            }
        }
    }
}
