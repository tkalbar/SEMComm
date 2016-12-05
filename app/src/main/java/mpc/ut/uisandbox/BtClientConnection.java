package mpc.ut.uisandbox;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Aurelius on 12/2/16.
 */

public class BtClientConnection {

    private static final String TAG = BtClientConnection.class.getName();

    public BluetoothSocket mmSocket;
    public SendWrapper sw;

    public BtClientConnection(BluetoothSocket sock) {
        Log.d(TAG, "In constructor");
        mmSocket = sock;
        try {
            sw = new SendWrapper(sock);
        } catch (IOException e) {
            Log.e(TAG, "send stream on client could not be created", e);
            mmSocket = null;
            e.printStackTrace();
        }
        Log.d(TAG, "End constructor");
    }

    public void sendGenericMessage(Patient pat) {
        if (mmSocket.isConnected()) {
            Log.d(TAG, "message sent by client");
            int check = sw.send(pat);
            if (check==-1) {
                try {
                    mmSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
            // Share the sent message back to the UI Activity
            //mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
            //        .sendToTarget();
    }

}
