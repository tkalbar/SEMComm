package mpc.ut.uisandbox;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;

/**
 * Created by Aurelius on 12/2/16.
 */

public class ReceiveWrapper extends ObjectInputStream {

    InputStream is;

    public ReceiveWrapper(InputStream is) throws IOException {
        super(is);
        Log.d("test", ""+2);
        this.is = is;
        Log.d("test", ""+3);
    }

    public ReceiveWrapper(Socket s) throws IOException {
        this(s.getInputStream());
    }

    public ReceiveWrapper(BluetoothSocket s) throws IOException {
        this(s.getInputStream());
    }

    public Object receive() {
        try {
            return super.readObject();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public void close() throws IOException {
        super.close();
    }
}