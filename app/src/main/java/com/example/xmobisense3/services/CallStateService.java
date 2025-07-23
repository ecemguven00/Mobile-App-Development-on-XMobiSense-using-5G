package com.example.xmobisense3.services;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CallStateService extends PhoneStateListener {

    private final Context context;
    private final TelephonyManager telephonyManager;
    private String currentCallState = "Idle";

    public CallStateService(Context context) {
        this.context = context;
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }



    public void startListening() {
        telephonyManager.listen(this, PhoneStateListener.LISTEN_CALL_STATE);
    }

    public void stopListening() {
        telephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
        switch (state) {
            case TelephonyManager.CALL_STATE_IDLE:
                currentCallState = "Idle";
                break;
            case TelephonyManager.CALL_STATE_RINGING:
                currentCallState = "Ringing";
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                currentCallState = "In Call";
                break;
            default:
                currentCallState = "Unknown";
        }
    }

    public String getCurrentCallState() {
        return currentCallState;
    }}
