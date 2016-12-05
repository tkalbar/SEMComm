package mpc.ut.uisandbox;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MenuItem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import android.view.View;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bluelinelabs.logansquare.LoganSquare;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.peak.salut.Callbacks.SalutDataCallback;

import static mpc.ut.uisandbox.BluetoothTech.mReceiver;

public class SettingsActivity extends Activity implements SalutDataCallback {

    private static final String TAG = SettingsActivity.class.getName();
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    public static final String PULSE = "00:24:E4:38:75:79";
    public static final String POLAR = "00:22:D0:BD:25:8E";
    public static final String UUID_CRED = "00000000-0000-0000-0000-000000000001";
    public static final String UUID_MAC = "00000000-0000-0000-0000-000000000002";
    public final static UUID UUID_HEART_RATE_MEASUREMENT = UUID
            .fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);

    public final static String deviceOne = "B4:CE:F6:34:46:53";
    public final static String deviceTwo = "B4:CE:F6:34:4A:C3";

    public final static String sensorTagOne = "B4:99:4C:64:AF:FD";
    public final static String sensorTagTwo = "78:A5:04:8C:21:97";

    public static long tagOneTime = 0;
    public static long tagTwoTime = 0;


    public final static int DOCTOR = 7;
    public final static int NURSE = 8;
    public static String DOCTOR_APP = "doctor";
    public static String PATIENT_APP = "patient";


    public static SettingsActivity topActivity = null;
    public static String uniqueDeviceId = null;
    public static String localMacAddress = null;
    public static boolean foundPolar = false;
    public static boolean foundPulse = false;
    public static String appType = null;

    // Connections
    public static BtClientConnection ctLiveConnection;

    // Threads
    public static BtAcceptThread at = null;
    public static BtServerConnectionThread ctLiveThread = null;
    public static BtConnectThread ct = null;

    // Models
    public static PatientModel pm = null;
    public static DoctorModel dm = null;

    GeneralPreferenceFragment genPrefFrag = null;

    public void onNewPatient(Patient newPatient) {
        Log.d(TAG, "On new patient");
        genPrefFrag.setPatientUi(newPatient);
    }

    public void changeLocalHeartRate() {
        Preference hrPref = genPrefFrag.findPreference("hr_data_pref");
        hrPref.setSummary("Last measured heart rate was " + PatientModel.patientData.heartRate + " BPM");
        PreferenceScreen ps = (PreferenceScreen) genPrefFrag.findPreference("patient_screen");
        ((BaseAdapter)ps.getRootAdapter()).notifyDataSetChanged();
    }

    public void changeLocalBloodPressure() {
        Preference bpPref = genPrefFrag.findPreference("bp_data_pref");
        bpPref.setSummary("Last measured blood pressure was XXX/XXX mmHg");
    }

    public static void initPatient() {
        pm = new PatientModel(SettingsActivity.topActivity);
        pm.initialize();
        initializePost();
    }

    public static void initDoctor() {
        dm = new DoctorModel(SettingsActivity.topActivity);
        initializePost();
        //DoctorModel.
        dm.initialize();
    }

    public static void initializePost() {
        // try bluetooth first
        boolean cond = BluetoothTech.initializeBluetooth();
        WifiDirectTech.initializeWifiDirect();
        if (cond) {
            Log.d(TAG, "initialized bluetooth");
        }
        else {
            Log.d(TAG, "initializing wifidirect, because bluetooth unsuccessful");

            //TODO: init wifidirect here
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate()");
        Log.d(TAG, "initializing universal settings");
        uniqueDeviceId= Settings.Secure.getString(getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        topActivity = this;

        perm();
        localMacAddress = android.provider.Settings.Secure.getString(getApplicationContext().getContentResolver(), "bluetooth_address");
        Log.d(TAG, "My local bt mac address: " + localMacAddress);

        setContentView(R.layout.base);

        genPrefFrag = new GeneralPreferenceFragment();

        getFragmentManager().beginTransaction()
                .replace(R.id.preferences, genPrefFrag)
                .commit();

        // Register bluetooth broadcast receiver
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

    }

    @Override
    protected void onStop() {
        super.onStop();
        WifiDirectTech.cleanup();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (at.isAlive()) {
            at.interrupt();
        }

        if (ct.isAlive()) {
            ct.interrupt();
        }

        if (ctLiveThread.isAlive()) {
            ctLiveThread.interrupt();
        }

        if (ctLiveConnection.mmSocket!=null  && ctLiveConnection.mmSocket.isConnected()) {
            try {
                ctLiveConnection.mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Unregister broadcast listeners
        unregisterReceiver(mReceiver);
        BluetoothTech.cleanup();

    }

    public static class GeneralPreferenceFragment extends PreferenceFragment {

        private static final String TAG = GeneralPreferenceFragment.class.getName();

        PreferenceScreen doctorScreen = null;
        HeartRateDataPreference localHrDataPref = null;
        BloodPressureDataPreference localBpDataPref = null;
        HashMap<String,PreferenceScreen> doctorPatients = new HashMap<String, PreferenceScreen>();

        public void setMainPrefListeners() {
            PreferenceScreen patMain = (PreferenceScreen) findPreference("patient_screen");
            patMain.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {

                    Log.d(TAG, "Running patient");
                    appType = PATIENT_APP;
                    SettingsActivity.initPatient();
                    initPatientPrefUi();
                    return true;
                }
            });
            PreferenceScreen docMain = (PreferenceScreen) findPreference("doctor_screen");
            docMain.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {

                    Log.d(TAG, "Running doctor");
                    appType = DOCTOR_APP;
                    SettingsActivity.initDoctor();
                    initDoctorPrefUi();
                    return true;
                }
            });
        }

        public void updateHrUi() {
            PreferenceScreen hrDataPrefScreen = (PreferenceScreen) findPreference("hr_data_pref");
            HeartRateDataPreference hrDataPref = (HeartRateDataPreference) findPreference(PatientModel.patientData.name+"raw_hr");
            if (hrDataPref==null) {
                hrDataPref = new HeartRateDataPreference(GeneralPreferenceFragment.this.getActivity(), null);
                hrDataPref.pat = PatientModel.patientData;
                hrDataPref.setKey(PatientModel.patientData.name+"raw_hr");
                hrDataPrefScreen.addPreference(hrDataPref);
            } else {
                hrDataPrefScreen.removePreference(hrDataPref);
                hrDataPref.pat = PatientModel.patientData;
                hrDataPrefScreen.addPreference(hrDataPref);
            }
        }

        public void initPatientPrefUi() {
            // bind switches
            PatientModel.bindPreferenceSummaryToBool(findPreference("hr_switch"));
            PatientModel.bindPreferenceSummaryToBool(findPreference("bp_switch"));
            PatientModel.bindPreferenceSummaryToBool(findPreference("loc_switch"));

            // bind data preferences
            PatientModel.bindPreferenceSummaryToValue(findPreference("hr_data_pref"));
            PatientModel.bindPreferenceSummaryToValue(findPreference("bp_data_pref"));

            // initialize HRD Preference object
            PreferenceScreen hrDataPrefScreen = (PreferenceScreen) findPreference("hr_data_pref");
            localHrDataPref = new HeartRateDataPreference(GeneralPreferenceFragment.this.getActivity(), null);
            localHrDataPref.pat = PatientModel.patientData;
            localHrDataPref.dataSet = PatientModel.hrDataSet;
            localHrDataPref.setKey(PatientModel.patientData.name+"raw_hr");
            hrDataPrefScreen.addPreference(localHrDataPref);

            // initialize BPD Preference object
            PreferenceScreen bpDataPrefScreen = (PreferenceScreen) findPreference("bp_data_pref");
            localBpDataPref = new BloodPressureDataPreference(GeneralPreferenceFragment.this.getActivity(), null);
            localBpDataPref.pat = PatientModel.patientData;
            bpDataPrefScreen.addPreference(localBpDataPref);

            // display Heart Rate and Blood Pressure Data on main screen
            displayMainScreenData();
        }

        public void initDoctorPrefUi() {
            // initialize doctor screen
            doctorScreen = (PreferenceScreen) findPreference("doctor_screen");

            // dummy patient tests
            // Patient testPatient = dummyPatientTest();
            // addPatientToDisplay(doctorScreen, testPatient, doctorPatients);
            // generateHrForPatient(doctorScreen, doctorPatients.get(testPatient.name), testPatient);
        }

        public void setPatientUi(Patient patient) {
            Log.d(TAG, "setPatientUi");
            addPatientToDisplay(doctorScreen, patient, doctorPatients);
            generateHrForPatient(doctorScreen, doctorPatients.get(patient.name), patient);
        }

        public void displayMainScreenData() {
            PreferenceScreen mainScreenHr = (PreferenceScreen) findPreference("hr_data_pref");

            // set summary on the main screen
            mainScreenHr.setSummary("Last measured heart rate was " + PatientModel.patientData.heartRate + " BPM");

            // set data value on detailed screen

            //TODO: setup blood pressure here
            // set data value on detailed screen
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_global);
            setHasOptionsMenu(true);

            setMainPrefListeners();

        }

        public Patient dummyPatientTest() {
            Patient pat = new Patient();
            pat.name = "Nathaniel Wendt";
            pat.shareHeart = true;
            pat.shareBlood = true;
            pat.locationSensitive = true;
            pat.heartRate = 78;
            pat.setBloodPressure();
            return pat;
        }

        // parent is doctor screen or nurse screen
        public void addPatientToDisplay(PreferenceScreen parent, Patient patient, HashMap<String, PreferenceScreen> currentPatients) {
            Log.d(TAG, "addPatientToDisplay");
            String patientName = patient.name;
            PreferenceScreen patientScreen = null;

            // check if patient exists
            if (currentPatients.containsKey(patientName)) {
                Log.d(TAG, "patient exists");
                // update the patient
                patientScreen = currentPatients.get(patientName);
            } else {
                // create the patient screen and add it
                patientScreen = getPreferenceManager().createPreferenceScreen(this.getActivity());
                patientScreen.setTitle(patientName);
                parent.addPreference(patientScreen);
                currentPatients.put(patientName, patientScreen);
            }
        }


        // parent is patient_screen
        public void generateHrForPatient(PreferenceScreen staff, PreferenceScreen parent, Patient patient) {
            Log.d(TAG, "generateHrForPatient");
            if (findPreference("ehr_heading")==null) {
                PreferenceCategory ehrTopLevel = new PreferenceCategory(GeneralPreferenceFragment.this.getActivity());
                ehrTopLevel.setTitle("Electronic Health Record");
                ehrTopLevel.setKey("ehr_heading");
                parent.addPreference(ehrTopLevel);
            }


            if (staff.getKey().equals("doctor_screen")) {
                HeartRateDataPreference hrDataPref = (HeartRateDataPreference) findPreference(patient.name+"hr");
                if (patient.shareHeart) {
                    if (hrDataPref == null) {
                        hrDataPref = new HeartRateDataPreference(GeneralPreferenceFragment.this.getActivity(), null);
                        hrDataPref.pat = patient;
                        ArrayList<Integer> newDataSet = new ArrayList<>();
                        newDataSet.add(patient.heartRate);
                        DoctorModel.idToDataMap.put(patient.name, newDataSet);
                        hrDataPref.dataSet = DoctorModel.idToDataMap.get(patient.name);
                        hrDataPref.setKey(patient.name+"hr");
                        parent.addPreference(hrDataPref);
                    } else {
                        parent.removePreference(hrDataPref);
                        ArrayList<Integer> dataSet = DoctorModel.idToDataMap.get(patient.name);
                        dataSet.add(patient.heartRate);
                        hrDataPref.pat = patient;
                        parent.addPreference(hrDataPref);
                    }
                } else if (hrDataPref!=null){
                    parent.removePreference(hrDataPref);
                }

                BloodPressureDataPreference bpDataPref = (BloodPressureDataPreference) findPreference(patient.name+"bp");
                if (patient.shareBlood) {
                    if (bpDataPref == null) {
                        bpDataPref = new BloodPressureDataPreference(GeneralPreferenceFragment.this.getActivity(), null);
                        bpDataPref.pat = patient;
                        bpDataPref.setKey(patient.name+"bp");
                        parent.addPreference(bpDataPref);
                    } else {
                        parent.removePreference(bpDataPref);
                        bpDataPref.pat = patient;
                        parent.addPreference(bpDataPref);
                    }
                } else if (bpDataPref!=null) {
                    parent.removePreference(bpDataPref);
                }
            }

        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    public static class HeartRateDataPreference extends Preference {

        private static final String TAG = GeneralPreferenceFragment.class.getName();
        public Patient pat = new Patient();
        public ArrayList<Integer> dataSet = new ArrayList<>();

        public HeartRateDataPreference(Context context, AttributeSet attrs) {
            super(context, attrs);
            Log.d(TAG, "Constructor");
            setLayoutResource(R.layout.heart_rate_data);
        }

        @Override
        public void onBindView(View view) {
            super.onBindView(view);
            Log.d(TAG, "onBindView");

            TextView hrText = (TextView) view.findViewById(R.id.hr_text_data);
            if (pat.heartRate!=null && pat.heartRate!=0) {
                // set summary on the main screen
                hrText.setText(pat.heartRate + " BPM");

                // set data value on detailed screen
            } else {
                hrText.setText("Disconnected");
            }

            while (dataSet.size() >= 50) {
                dataSet.remove(0);
            }

            LineChart chart = (LineChart) view.findViewById(R.id.line_chart_heart);

            Description desc = new Description();
            desc.setText("Live Heart Rate");
            desc.setTextColor(Color.WHITE);

            List<Entry> entries = new ArrayList<>();

            int count = 0;
            for (Integer nextHeartRate : dataSet) {

                // turn your data into Entry objects
                entries.add(new Entry(count, nextHeartRate));
                count++;
            }
            if (entries.isEmpty()) {
                return;
            }

            LineDataSet gxDataSet = new LineDataSet(entries, "Heart Rate in BPM");
            gxDataSet.setColor(ColorTemplate.getHoloBlue());
            gxDataSet.setValueTextColor(Color.DKGRAY);

            IValueFormatter format = new IValueFormatter() {
                @Override
                public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                    int intVal = (int) value;
                    String strVal = "";
                    strVal += intVal;
                    return strVal;
                }
            };
            gxDataSet.setValueFormatter(format);

            LineData lineData = new LineData(gxDataSet);
            lineData.setValueTextColor(Color.WHITE);
            chart.setData(lineData);
            chart.setDescription(desc);
            chart.setMaxVisibleValueCount(30);
            chart.getXAxis().setTextColor(Color.WHITE);
            chart.getAxisLeft().setTextColor(Color.WHITE);
            chart.getAxisRight().setTextColor(Color.WHITE);
            chart.getLegend().setTextColor(Color.WHITE);
            chart.invalidate();
        }
    }

    public static class BloodPressureDataPreference extends Preference {

        private static final String TAG = GeneralPreferenceFragment.class.getName();
        public Patient pat = new Patient();


        public BloodPressureDataPreference(Context context, AttributeSet attrs) {
            super(context, attrs);
            Log.d(TAG, "Constructor");
            setLayoutResource(R.layout.blood_pressure_data);


        }
        @Override
        public void onBindView(View view) {
            super.onBindView(view);
            Log.d(TAG, "onBindView");

            LineChart chart = (LineChart) view.findViewById(R.id.line_chart_blood);

            Description desc = new Description();
            desc.setText("Blood Pressure Recordings in mmHg");
            desc.setTextColor(Color.WHITE);

            List<Entry> entriesSys = new ArrayList<>();
            List<Entry> entriesDia = new ArrayList<>();
            int count = 0;
            for (Integer sys : pat.systolicArray) {
                entriesSys.add(new Entry(count, sys));
                count++;
            }

            count = 0;
            for (Integer dia : pat.diastolicArray) {
                entriesDia.add(new Entry(count, dia));
                count++;
            }

            if (entriesDia.isEmpty()) {
                return;
            }

            LineDataSet gxDataSetSys = new LineDataSet(entriesSys, "Systolic");
            LineDataSet gxDataSetDia = new LineDataSet(entriesDia, "Diastolic");
            gxDataSetSys.setColor(ColorTemplate.getHoloBlue());
            gxDataSetSys.setValueTextColor(Color.DKGRAY);

            gxDataSetDia.setColor(Color.MAGENTA);
            gxDataSetDia.setValueTextColor(Color.DKGRAY);

            IValueFormatter format = new IValueFormatter() {
                @Override
                public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                    int intVal = (int) value;
                    String strVal = "";
                    strVal += intVal;
                    return strVal;
                }
            };
            gxDataSetSys.setValueFormatter(format);
            gxDataSetDia.setValueFormatter(format);

            LineData lineData = new LineData(gxDataSetSys,gxDataSetDia);
            lineData.setValueTextColor(Color.WHITE);
            chart.setData(lineData);
            chart.setDescription(desc);
            chart.setMaxVisibleValueCount(30);
            chart.getXAxis().setTextColor(Color.WHITE);
            chart.getAxisLeft().setTextColor(Color.WHITE);
            chart.getAxisRight().setTextColor(Color.WHITE);
            chart.getLegend().setTextColor(Color.WHITE);
            chart.invalidate();
        }
    }

    @TargetApi(23)
    public void perm() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }
    }

    @Override
    public void onDataReceived(Object o) {
        Log.d(TAG, "Received data!!!");
        try
        {
            final Patient newPat = LoganSquare.parse((String)o, Patient.class);
            Log.d(TAG, "Parsed Logan Message");
            if(newPat!=null) {
                int heartRate = newPat.heartRate;
                Log.d(TAG, "Received heart rate: " + heartRate);
                SettingsActivity.topActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        Log.d(TAG, "Running onNewPatient on UI Thread");
                        SettingsActivity.topActivity.onNewPatient(newPat);
                    }
                });
            }
            //Do other stuff with data.
//            try {
//                blQueue.put(pack);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
        catch (IOException ex)
        {
            Log.e(TAG, "Failed to parse network data.");
        }
    }
}
