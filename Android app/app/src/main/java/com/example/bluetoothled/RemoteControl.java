/*
 * (c) Matey Nenov (https://www.thinker-talk.com)
 *
 * Licensed under Creative Commons: By Attribution 3.0
 * http://creativecommons.org/licenses/by/3.0/
 *
 */

package com.example.bluetoothled;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.nio.charset.StandardCharsets;

public class RemoteControl {
    private final static byte START = 0x3;
    private final static byte HEARTBEAT = 0x2;
    private final static byte LED_COMMAND = 0x4;
    //private final static int a = 0;
    private final static byte VALUE_OFF = 0x0;
    private final static byte VALUE_ON = (byte)0xFF;
    private final static byte value_on = 0x01;
    private final static byte value_off = 0x00;

    private BLEController bleController;

    public RemoteControl(BLEController bleController) {
        this.bleController = bleController;
    }

    private byte [] createControlWord(byte type, byte ... args) {
        byte [] command = new byte[args.length];
//        command[0] = START;
//        command[1] = type;
//        command[2] = (byte)args.length;
        for(int i=0; i<args.length; i++)
            command[i] = args[i];

        return command;
    }

    public void switchLED(boolean on) {
        this.bleController.sendData(createControlWord(LED_COMMAND, on?value_on:value_off));
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String readBluetooth(){
        // string to byte[]
        byte[] data = this.bleController.receiveData();

        String s = new String("There is no data yet");
        if(data!=null){
            // byte[] to string
//            s = new String(data, StandardCharsets.UTF_8);
            s = new String(data);
        }
        return s;
    }



    public void heartbeat() {
        this.bleController.sendData(createControlWord(HEARTBEAT));
    }
}
