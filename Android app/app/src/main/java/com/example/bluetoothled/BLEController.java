/*
 * (c) Matey Nenov (https://www.thinker-talk.com)
 *
 * Licensed under Creative Commons: By Attribution 3.0
 * http://creativecommons.org/licenses/by/3.0/
 *
 */

package com.example.bluetoothled;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.nio.ByteBuffer;

import static android.bluetooth.BluetoothProfile.GATT;

public class BLEController {
    private static com.example.bluetoothled.BLEController instance;

    private BluetoothLeScanner scanner;
    private BluetoothDevice device;
    private BluetoothGatt bluetoothGatt;
    private BluetoothManager bluetoothManager;

    private BluetoothGattCharacteristic btGattChar = null;

    private ArrayList<com.example.bluetoothled.BLEControllerListener> listeners = new ArrayList<>();
    private HashMap<String, BluetoothDevice> devices = new HashMap<>();
    protected static final UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    private BLEController(Context ctx) {
        this.bluetoothManager = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
    }

    public static com.example.bluetoothled.BLEController getInstance(Context ctx) {
        if(null == instance)
            instance = new com.example.bluetoothled.BLEController((ctx));

        return instance;
    }

    public void addBLEControllerListener(com.example.bluetoothled.BLEControllerListener l) {
        if(!this.listeners.contains(l))
            this.listeners.add(l);
    }

    public void removeBLEControllerListener(com.example.bluetoothled.BLEControllerListener l) {
        this.listeners.remove(l);
    }

    public void init() {
        this.devices.clear();
        this.scanner = this.bluetoothManager.getAdapter().getBluetoothLeScanner();
        scanner.startScan(bleCallback);
    }

    private ScanCallback bleCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if(!devices.containsKey(device.getAddress()) && isThisTheDevice(device)) {
                deviceFound(device);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for(ScanResult sr : results) {
                BluetoothDevice device = sr.getDevice();
                if(!devices.containsKey(device.getAddress()) && isThisTheDevice(device)) {
                    deviceFound(device);
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.i("[BLE]", "scan failed with errorcode: " + errorCode);
        }
    };

    private boolean isThisTheDevice(BluetoothDevice device) {
        return null != device.getName() && device.getName().startsWith("Luditech");
    }

    private void deviceFound(BluetoothDevice device) {
        this.devices.put(device.getAddress(), device);
        fireDeviceFound(device);
    }

    public void connectToDevice(String address) {
        this.device = this.devices.get(address);
        this.scanner.stopScan(this.bleCallback);
        Log.i("[BLE]", "connect to device " + device.getAddress());
        this.bluetoothGatt = device.connectGatt(null, false, this.bleConnectCallback);
    }

    private final BluetoothGattCallback bleConnectCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("[BLE]", "start service discovery " + bluetoothGatt.discoverServices());
            }else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                btGattChar = null;
                Log.w("[BLE]", "DISCONNECTED with status " + status);
                fireDisconnected();
            }else {
                Log.i("[BLE]", "unknown state " + newState + " and status " + status);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if(null == btGattChar) {
                for (BluetoothGattService service : gatt.getServices()) {
                    Log.i("[BLE]", "Service UUID Found: " + service.getUuid().toString());
                    if (service.getUuid().toString().toUpperCase().startsWith("0000FFE0")) {
                        boolean succes =false;
                        for (BluetoothGattCharacteristic bgc : service.getCharacteristics()) {
                            Log.i("[BLE]", "Characteristic UUID Found: " + bgc.getUuid().toString());
                            if (bgc.getUuid().toString().toUpperCase().startsWith("0000FFE1")||bgc.getUuid().toString().toUpperCase().startsWith("0000FFE2")||bgc.getUuid().toString().toUpperCase().startsWith("0000FFE3")) {
                                int chprop = bgc.getProperties();
                                if (((chprop & BluetoothGattCharacteristic.PROPERTY_WRITE) | (chprop & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) {
                                    if ((chprop & BluetoothGattCharacteristic.PROPERTY_NOTIFY)  > 0) {
                                        if(setCharacteristicNotification(bgc, gatt,true)) {
                                            Log.i("[BLE]", "Subscription succeed");
                                        }
                                        else{
                                            Log.w("[BLE]", "Subscription failed");
                                        }
                                    }
                                    btGattChar = bgc;
                                    Log.i("[BLE]", "CONNECTED and ready to send");
                                    succes=true;
                                }

                            }
                        }
                        if(succes){
                            fireConnected();
                        }
                    }
                }
            }
        }

        public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, BluetoothGatt gatt,
                                                     boolean enable) {
            gatt.setCharacteristicNotification(characteristic, enable);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
            boolean succes = false;
            if(descriptor != null) {
                descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : new byte[]{0x00, 0x00});
                succes = gatt.writeDescriptor(descriptor); //descriptor write operation successfully started?
            }
            else{
                Log.i("[BLE]","descriptor is null");
            }
            return succes; //descriptor write operation successfully started?
        }

        @Override
        public void onCharacteristicChanged (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
//            Log.d("[BLE]","reading new notified message");
            bluetoothGatt.readCharacteristic(characteristic);
            byte [] s = characteristic.getValue(); //convert incoming byte array to string
            int x = ((s[0] ) << 8) | (s[1] );
            int y = ((s[2] ) << 8) | (s[3] );
            int z = ((s[4] ) << 8) | (s[5] );
            MainActivity.getInstance().logx(String.valueOf(x));
            MainActivity.getInstance().logy(String.valueOf(y));
            MainActivity.getInstance().logz(String.valueOf(z));
        }

    };

    private boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic,boolean enabled) {
        if (bluetoothGatt == null) {
            Log.w("[BLE]", "BluetoothGatt not initialized");
            return false;
        }
        return bluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    private void fireDisconnected() {
        for(com.example.bluetoothled.BLEControllerListener l : this.listeners)
            l.BLEControllerDisconnected();

        this.device = null;
    }

    private void fireConnected() {
        for(com.example.bluetoothled.BLEControllerListener l : this.listeners)
            l.BLEControllerConnected();
    }

    private void fireDeviceFound(BluetoothDevice device) {
        for(com.example.bluetoothled.BLEControllerListener l : this.listeners)
            l.BLEDeviceFound(device.getName().trim(), device.getAddress());
    }

    public void sendData(byte [] data) {
        this.btGattChar.setValue(data);
        bluetoothGatt.writeCharacteristic(this.btGattChar);
    }

    public byte[] receiveData(){
        bluetoothGatt.readCharacteristic(this.btGattChar);
        return this.btGattChar.getValue();
    }

    public boolean checkConnectedState() {
        return this.bluetoothManager.getConnectionState(this.device, GATT) == 2;
    }

    public void disconnect() {
        this.bluetoothGatt.disconnect();
    }
}