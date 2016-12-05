package mpc.ut.uisandbox;

import android.bluetooth.le.AdvertiseData;
import android.media.audiofx.BassBoost;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Aurelius on 12/2/16.
 */

public class DoctorModel {

    private static final String TAG = DoctorModel.class.getName();

    public static ArrayList<Patient> localPatientStore;

    public static HashMap<String, ArrayList<Integer>> idToDataMap = null;

    public SettingsActivity context = null;
    public DoctorModel(SettingsActivity c) {
        context = c;
        idToDataMap = new HashMap<>();
    }

    public void initialize() {

    }

    public static void sendBeacon(String myMacAddr) {
        int credentials = SettingsActivity.DOCTOR;
        String realMac =  myMacAddr;
        //TODO: add wifiDirect
        String salutId = "";

        //idToDataMap.put(SettingsActivity.UUID_CRED, credentials);
        //idToDataMap.put(SettingsActivity.UUID_MAC, realMac);
        AdvertiseData ad  = BluetoothTech.buildAdvertiseData(credentials, realMac);
        BluetoothTech.startAdvertiseLe(ad);

    }

    public static void listenForPatientConn() {
        SettingsActivity.at = new BtAcceptThread(SettingsActivity.topActivity);
        SettingsActivity.at.start();
    }

//    public void btSendToPatient(GenericMessage gm) {
//        if (SettingsActivity.ctLive.isAlive()) {
//            Log.d(TAG, "sending to patient (connected)");
//            SettingsActivity.ctLive.sendGenericMessage(gm);
//        } else {
//            Log.d(TAG, "could not send to patient (thread not alive)");
//        }
//    }

}
