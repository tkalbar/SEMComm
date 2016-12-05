package mpc.ut.SEMComm;

import android.bluetooth.BluetoothDevice;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Aurelius on 12/2/16.
 */

public class PatientModel {

    private static final String TAG = PatientModel.class.getName();
    public static Patient patientData;

    public static ArrayList<Integer> hrDataSet;

    //HashMap<String, String> idToDataMap = null;

    //Random rand  = new Random(2356434);

    public SettingsActivity context = null;
    public PatientModel(SettingsActivity c) {
        context = c;
        //idToDataMap = new HashMap<>();
        hrDataSet = new ArrayList<>();
    }

    public void initialize() {
        patientData = new Patient();
        patientData.name = "Tomasz Kalbarczyk";
        patientData.setBloodPressure();
        patientData.heartRate = 0;
        patientData.locationSensitive = false;
        patientData.shareHeart = false;
        patientData.shareBlood = false;
    }

    public void processDoctorBeacon(String mac) {

        Log.d(TAG, "Address received: " + mac);
        btConnectToDoctor(mac);

    }

    public void sendHeartRate(int heartRate) {
        PatientModel.patientData.heartRate = heartRate;
        PatientModel.hrDataSet.add(heartRate);
        SettingsActivity.topActivity.runOnUiThread(new Runnable() {
            public void run() {
                Log.d(TAG, "Running changeLocalHeartRate on UI Thread");
                SettingsActivity.topActivity.changeLocalHeartRate();
                SettingsActivity.topActivity.genPrefFrag.updateHrUi();
            }
        });

        sendPatientData();
    }

    public static void sendPatientData() {
        Log.d(TAG, "Sending patient data");
        if (PatientModel.patientData.shareHeart|| PatientModel.patientData.shareBlood) {
            if (PatientModel.patientData.locationSensitive && !isPatientNearExamRoom()) {
                Log.d(TAG, "Not sending, need to be near exam room");
            } else {
                Log.d(TAG, "Sending to doctor");
                btSendToDoctor(PatientModel.patientData);
            }

        }
    }

    public static boolean isPatientNearExamRoom() {
         if ((Math.abs(System.currentTimeMillis() - SettingsActivity.tagOneTime) < 5000) ||
                 (Math.abs(System.currentTimeMillis() -  SettingsActivity.tagTwoTime) < 5000)) {
             Log.d(TAG, "Patient near exam room");
             return true;
         } else {
             return false;
         }
    }

    public static void btConnectToDoctor(String doctorRealMac) {
        if (BluetoothTech.btAdapter.isEnabled()) {
            BluetoothDevice btDev = BluetoothTech.btAdapter.getRemoteDevice(doctorRealMac);
            if (SettingsActivity.ctLiveConnection!=null && SettingsActivity.ctLiveConnection.mmSocket!=null
                    && SettingsActivity.ctLiveConnection.mmSocket.isConnected()) {
                // just continue since we still have a good connection
            } else if(SettingsActivity.ct == null || !SettingsActivity.ct.isAlive()) {
                    SettingsActivity.ct = new BtConnectThread(SettingsActivity.topActivity, btDev);
                    SettingsActivity.ct.start();
            }
        }
    }

    public static void btSendToDoctor(Patient pat) {
        if (SettingsActivity.ctLiveConnection!=null && SettingsActivity.ctLiveConnection.mmSocket.isConnected()) {
            Log.d(TAG, "sending to patient (connected)");
            SettingsActivity.ctLiveConnection.sendGenericMessage(pat);
        } else {
            Log.d(TAG, "could not send to doctor (thread not alive)");
            wdSendToDoctor(pat);
        }
    }

    public static void wdSendToDoctor(Patient pat) {


        if (WifiDirectTech.wifiManager.isWifiEnabled() && WifiDirectTech.network.isConnectedToAnotherDevice
                && WifiDirectTech.network.registeredHost!=null && WifiDirectTech.registered) {
            Log.d(TAG, "trying using wifidirect");

            WifiDirectTech.sendToHost(pat);
        }
    }

    public static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            Log.d(TAG, "onPreferenceChange");
            String key = preference.getKey();

            if (preference instanceof SwitchPreference) {
                Boolean switchValue = (Boolean) value;
                if (key.equals("hr_switch")) {
                    Log.d(TAG, "hr_switch flipped");
                    patientData.shareHeart = switchValue;
                } else if (key.equals("bp_switch")) {
                    Log.d(TAG, "bp_switch flipped");
                    patientData.shareBlood = switchValue;
                    PatientModel.sendPatientData();
                } else if (key.equals("loc_switch")) {
                    Log.d(TAG, "loc_switch flipped");
                    patientData.locationSensitive = switchValue;
                }
            } else if (key.equals("hr_data_pref")) {
                Log.d(TAG, "hr data changed");
                //TODO: update heart rate summary
            } else if (key.equals("bp_data_pref")) {
                Log.d(TAG, "bp data changed");
                //TODO: update blood pressure summary
            }

            return true;
        }
    };

    public static void bindPreferenceSummaryToBool(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getBoolean(preference.getKey(), false));
    }

    public static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        //sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
        //        Boolean.FALSE);
    }

}
