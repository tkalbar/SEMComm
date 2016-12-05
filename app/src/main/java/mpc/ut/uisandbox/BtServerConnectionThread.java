package mpc.ut.uisandbox;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Aurelius on 12/2/16.
 */

public class BtServerConnectionThread extends Thread {

    private static final String TAG = BtServerConnectionThread.class.getName();
    public SettingsActivity mainAct = null;

    public BluetoothSocket mmSocket;
    public ReceiveWrapper rw;

    public BtServerConnectionThread(BluetoothSocket sock) {
        Log.d(TAG, "In constructor");
        mmSocket = sock;

        try {
            rw = new ReceiveWrapper(sock);
        } catch (IOException e) {
            Log.e(TAG, "receive stream on server could not be created", e);
            mmSocket = null;
            e.printStackTrace();
        }
        Log.d(TAG, "End constructor");

    }

    @Override
    public void run() {
        Log.d(TAG, "Running server connection thread");
        while(mmSocket!=null && mmSocket.isConnected() && !isInterrupted()) {

            final Patient pat = (Patient) rw.receive();
            if (pat!=null) {
                int heartRate = pat.heartRate;
                Log.d(TAG, "Received heart rate: " + heartRate);
                SettingsActivity.topActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        Log.d(TAG, "Running onNewPatient on UI Thread");
                        SettingsActivity.topActivity.onNewPatient(pat);
                    }
                });

            } else {
                break;
            }

        }
    }

}
