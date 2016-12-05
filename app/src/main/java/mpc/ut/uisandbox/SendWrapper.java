package mpc.ut.uisandbox;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by Aurelius on 12/2/16.
 */

public class SendWrapper extends ObjectOutputStream {

    OutputStream os;

    public SendWrapper(OutputStream os) throws IOException {
        super(os);
        this.os = os;
    }

    public SendWrapper(Socket s) throws IOException {
        this(s.getOutputStream());
    }

    public SendWrapper(BluetoothSocket s) throws IOException {
        this(s.getOutputStream());
    }

    public int send(Object obj) {
        try {
            super.writeObject(obj);
            super.flush();
            super.reset();
            return 0;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return -1;
        }
    }

    public void close() {
        try {
            super.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
