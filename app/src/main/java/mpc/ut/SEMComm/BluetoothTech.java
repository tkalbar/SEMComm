package mpc.ut.SEMComm;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BluetoothTech {

    private static final String TAG = BluetoothTech.class.getName();

    public static BluetoothLeScanner bleScanner;
    public static BluetoothLeAdvertiser bleAdvertiser;
    public static BluetoothAdapter btAdapter;
    public static BluetoothServerSocket btServerSocket;
    public static BluetoothSocket serverSideSocket;
    public static BluetoothSocket clientSideSocket;
    public static BluetoothGatt currentGattConnection;

    public static boolean initializeBluetooth() {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) SettingsActivity.topActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = bluetoothManager.getAdapter();
        if (btAdapter.isEnabled()) {
            SettingsActivity.foundPolar = false;
            bleScanner = btAdapter.getBluetoothLeScanner();
            bleAdvertiser = btAdapter.getBluetoothLeAdvertiser();
            scanLeDevice(true);
            if (SettingsActivity.appType.equals(SettingsActivity.DOCTOR_APP)) {
                SettingsActivity.dm.sendBeacon(SettingsActivity.localMacAddress);
                SettingsActivity.dm.listenForPatientConn();
            }
            return true;
        } else {
            return false;
        }
    }

    public static AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder build = new AdvertiseSettings.Builder();
        return build.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED).setConnectable(true).build();
    }

    public static AdvertiseData buildAdvertiseData(HashMap<String, String> idToDataMap) {
        AdvertiseData.Builder builder = new AdvertiseData.Builder();
        for (Map.Entry<String, String> e : idToDataMap.entrySet()) {
            String strUuid = e.getKey();
            String strData = e.getValue();
            ParcelUuid pUuid = ParcelUuid.fromString(strUuid);
            byte[] byteData = strData.getBytes();

            builder = builder.addServiceData(pUuid, byteData);
        }
        builder = builder.setIncludeDeviceName(false);
        return builder.build();
    }

    public static AdvertiseData buildAdvertiseData(int cred, String mac) {
        AdvertiseData.Builder builder = new AdvertiseData.Builder();
        return builder.addManufacturerData(cred, mac.getBytes()).setIncludeDeviceName(false).build();
    }

    public static void startAdvertiseLe(AdvertiseData ad) {
        bleAdvertiser.startAdvertising(buildAdvertiseSettings(), ad, mLeAdvertiseCallback);
    }

    public static AdvertiseCallback mLeAdvertiseCallback =
            new AdvertiseCallback() {
                @Override
                public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                    super.onStartSuccess(settingsInEffect);
                    Log.d(TAG, "advertising by BLE");
                }

                @Override
                public void onStartFailure(int errorCode) {
                    super.onStartFailure(errorCode);
                    Log.d(TAG, "Failed advertising, error code: " + errorCode);
                }
            };

    public static void scanLeDevice(final boolean enable) {
        bleScanner.startScan(mLeScanCallback);
    }

    public static Map<String,String> formatBeacon(Map<ParcelUuid,byte[]> map) {
        HashMap<String, String> formatMap = new HashMap<>();
        for (ParcelUuid p : map.keySet()) {
            byte[] serviceBytes = map.get(p);
            String strServiceBytes = new String(serviceBytes, StandardCharsets.UTF_8);
            Log.d(TAG, "Service Bytes: " + strServiceBytes);
            String pUuidStr = p.toString();
            Log.d(TAG, "pUuidStr: " + pUuidStr);
            formatMap.put(pUuidStr, strServiceBytes);
        }
        return formatMap;
    }

    // Device scan callback.
    public static ScanCallback mLeScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    BluetoothDevice dev = result.getDevice();
                    ScanRecord sr = result.getScanRecord();
                    String srName = sr.getDeviceName();
                    //byte[] bytes = sr.getBytes();
                    //String byteString = new String(bytes, StandardCharsets.UTF_8);
                    String addr = dev.getAddress();
                    Log.d(TAG, "Mac address of device: " + addr);
                    //String name = dev.getName();
                    //Map<ParcelUuid,byte[]> map = sr.getServiceData();

                    // pass beacon to patient app
                    byte[] bytes = sr.getManufacturerSpecificData(SettingsActivity.DOCTOR);
                    if (SettingsActivity.pm != null && bytes!=null) {
                        Log.d(TAG, "Got doctor data");
                        String strManuBytes = new String(bytes, StandardCharsets.UTF_8);
                        //Log.d(TAG, "Manu Bytes: " + strManuBytes);
                        SettingsActivity.pm.processDoctorBeacon(strManuBytes);
                    }

                    if (SettingsActivity.appType.equals(SettingsActivity.PATIENT_APP)) {
                        if (addr.equals(SettingsActivity.POLAR) && SettingsActivity.foundPolar == false) {
                            SettingsActivity.foundPolar = true;
                            Log.d(TAG, "POLAR found, doing connectGatt");
                            //bleScanner.stopScan(mLeScanCallback);
                            currentGattConnection = dev.connectGatt(SettingsActivity.topActivity.getApplicationContext(),
                                    false, gattCallback);
                            //currentGattConnection = dev.connectGatt(SettingsActivity.topActivity.getApplicationContext(),
                            //        false, gattCallback, TRANSPORT_LE);
                            //BluetoothGatt gatt = dev.connectGatt(topActivity.getApplicationContext(), false, gattCallback);
                            //gatt.connect();
                        }
                    }

                    if (SettingsActivity.appType.equals(SettingsActivity.PATIENT_APP)) {
                        if (addr.equals(SettingsActivity.sensorTagOne)) {
                            Log.d(TAG, "Setting tag one time");
                            SettingsActivity.tagOneTime = System.currentTimeMillis();
                        }
                        if (addr.equals(SettingsActivity.sensorTagTwo)) {
                            Log.d(TAG, "Setting tag two time");
                            SettingsActivity.tagTwoTime = System.currentTimeMillis();
                        }
                    }

//                    if (addr.equals(SettingsActivity.PULSE) && SettingsActivity.foundPulse == false) {
//                        SettingsActivity.foundPulse= true;
//                        Log.d(TAG, "PULSE found, doing connectGatt");
//                        //bleScanner.stopScan(mLeScanCallback);
//                        currentGattConnection = dev.connectGatt(SettingsActivity.topActivity.getApplicationContext(),
//                                false, gattCallback);
//                        //currentGattConnection = dev.connectGatt(SettingsActivity.topActivity.getApplicationContext(),
//                        //        false, gattCallback, TRANSPORT_LE);
//                        //BluetoothGatt gatt = dev.connectGatt(topActivity.getApplicationContext(), false, gattCallback);
//                        //gatt.connect();
//                    }

                    //Log.d(TAG, "Scan record device name: " + srName);
                    //Log.d(TAG, "bt device name: " + name);
                    //Log.d(TAG, "Bytes: " + byteString);
                    //Log.d(TAG, "Service Bytes: " + new String(serviceBytes, StandardCharsets.UTF_8));

//                    if (!uniqueDeviceId.equals("4e1d3c44cca1e306")) {
//
//                        if (dev.getName() != null && dev.getName().equals("Nexus 9")) {
//                            Log.d(TAG, "Found Nexus 9, attempting connection");
//                            BluetoothDevice btDev = btAdapter.getRemoteDevice("B4:CE:F6:34:46:53");
//                            if (btDev == null) {
//                                Log.d(TAG, "bt dev is null");
//                            } else {
//                                Log.d(TAG, "try raw mac address");
//                                dev = btDev;
//                            }
//                            BtConnectThread btConnectThread = new BtConnectThread(topActivity, dev);
//                            btConnectThread.run();
//                        }
//                    }
//                    if (uniqueDeviceId.equals("4e1d3c44cca1e306")) {
//
//                        if (dev.getName() != null && dev.getName().equals("Nexus 9")) {
//                            Log.d(TAG, "Found Nexus 9, attempting connection");
//                            BluetoothDevice btDev = btAdapter.getRemoteDevice("B4:CE:F6:34:4A:C3");
//                            if (btDev == null) {
//                                Log.d(TAG, "bt dev is null");
//                            } else {
//                                Log.d(TAG, "try raw mac address");
//                                dev = btDev;
//                            }
//                            BtConnectThread btConnectThread = new BtConnectThread(topActivity, dev);
//                            btConnectThread.run();
//                        }
//                    }

                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    super.onBatchScanResults(results);
                    Log.d(TAG, "On batch scan results");
                }

                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                    Log.d(TAG, "ble scan failed");
                    scanLeDevice(true);
                }
            };

    // GATT

    public static BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();

                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    if (btAdapter!=null && btAdapter.isEnabled()) {
                        BluetoothDevice btDev = btAdapter.getRemoteDevice(SettingsActivity.POLAR);
                        currentGattConnection = btDev.connectGatt(SettingsActivity.topActivity.getApplicationContext(),
                                false, gattCallback);
                    }
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();

            Log.i("onServicesDiscovered", services.toString());
            serviceProcessor(services);
            //gatt.readCharacteristic(services.get(1).getCharacteristics().get
            //        (0));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());
            gatt.disconnect();
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        public void serviceProcessor(List<BluetoothGattService> serviceList) {
            for (BluetoothGattService service : serviceList) {
                String serviceUuid = service.getUuid().toString();
                Log.d(TAG, "Service found with uuid: " + serviceUuid);
                //Log.d(TAG, "Service base string: " + service.toString());
                List<BluetoothGattCharacteristic> chars = service.getCharacteristics();
                for (BluetoothGattCharacteristic chara : chars) {
                    //Log.d(TAG, "Chara base string: " + chara.toString());
                    setCharacteristicNotification(chara, true);

                    List<BluetoothGattDescriptor> descs = chara.getDescriptors();
                    for (BluetoothGattDescriptor desc : descs) {
                        Log.d(TAG, "Desc found with uuid: " + desc.getUuid().toString());
                    }
                }
            }

        }

        public void setCharacteristicNotification(
                BluetoothGattCharacteristic characteristic, boolean enabled) {

            currentGattConnection.setCharacteristicNotification(characteristic, enabled);

            try {
                // This is specific to Heart Rate Measurement.
                if (SettingsActivity.UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
                    BluetoothGattDescriptor descriptor = characteristic
                            .getDescriptor(UUID
                                    .fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                    descriptor
                            .setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    currentGattConnection.writeDescriptor(descriptor);

                }
            } catch (Exception e) {
                Log.d(TAG,
                        "Exception while setting up notification for heartrate.", e);
            }
        }

        public void closeGatt() {
            currentGattConnection.disconnect();
            currentGattConnection.close();
        }

        public void handleChara(BluetoothGattCharacteristic chara) {
            if (SettingsActivity.UUID_HEART_RATE_MEASUREMENT.equals(chara.getUuid())) {
                Log.d(TAG, "Chara found with uuid: " + chara.getUuid().toString());
                //String strVal = chara.getStringValue(0);
                //Log.d(TAG, "strVal: " + strVal);
                int flag = chara.getProperties();
                //Log.d(TAG, "flag: " + flag);
                int format = -1;
                if ((flag & 0x01) != 0) {
                    format = BluetoothGattCharacteristic.FORMAT_UINT16;
                    //Log.d(TAG, "Heart rate format UINT16.");
                } else {
                    format = BluetoothGattCharacteristic.FORMAT_UINT8;
                    //Log.d(TAG, "Heart rate format UINT8.");
                }
                final int heartRate = chara.getIntValue(format, 1);
                Log.d(TAG, "Heart Rate: " + heartRate);
                //Log.d(TAG, String.format("Received heart rate: %d", heartRate));
                if (SettingsActivity.pm!=null) {

                    SettingsActivity.pm.sendHeartRate(heartRate);
                }
                //intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
            } else {
                Log.d(TAG, "Chara found with uuid: " + chara.getUuid().toString());
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(TAG, "onCharaChanged");
            handleChara(characteristic);
        }
    };

    public static void cleanup() {
        try {
            // close the connection to stop to listen any connection now
            if (serverSideSocket != null) {
                serverSideSocket.close();
            }
        } catch(IOException e) { }

        try {
            // close the connection to stop to listen any connection now
            if (clientSideSocket != null) {
                clientSideSocket.close();
            }
        } catch(IOException e) { }

        try {
            // close the connection to stop to listen any connection now
            if (btServerSocket != null) {
                btServerSocket.close();
            }
        } catch(IOException e) { }
    }

    public static final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "Bluetooth off");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "Bluetooth turning off");
//                        if (bleAdvertiser != null) {
//                            bleAdvertiser.stopAdvertising(mLeAdvertiseCallback);
//                        }
//                        if (bleScanner != null) {
//                            bleScanner.stopScan(mLeScanCallback);
//                        }

                        if (SettingsActivity.ctLiveConnection!=null && SettingsActivity.ctLiveConnection.mmSocket.isConnected()) {
                            try {
                                SettingsActivity.ctLiveConnection.mmSocket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (SettingsActivity.ctLiveThread!=null && SettingsActivity.ctLiveThread.isAlive()) {
                            SettingsActivity.ctLiveThread.interrupt();
                        }
                        if (SettingsActivity.at!=null && SettingsActivity.at.isAlive()) {
                            SettingsActivity.at.interrupt();
                        }

                        if (SettingsActivity.ct!=null && SettingsActivity.ct.isAlive()) {
                            SettingsActivity.ct.interrupt();
                        }


                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "Bluetooth on");
                        initializeBluetooth();
                        if(SettingsActivity.appType.equals("doctor")) {
                            SettingsActivity.dm.initialize();
                        }


                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "Bluetooth turning on");
                        break;
                }
            }
        }
    };
}
