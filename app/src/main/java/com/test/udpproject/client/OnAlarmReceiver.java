package com.test.udpproject.client;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import java.util.Objects;

public class OnAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "OnAlarmReceiver";
    private static AlarmReceiverCallback mAlarmReceiverCallback;
    private static String mServerIp;
    private static int mServerPort;
    private static int mPacketSize;
    private static int mPacketToIgnored;

    public OnAlarmReceiver(AlarmReceiverCallback alarmReceiverCallback) {
        mAlarmReceiverCallback = alarmReceiverCallback;
    }

    public OnAlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        if (Objects.equals(intent.getAction(), "INTENT_ALARM_ACTION")) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = null;
            if (pm != null) {
                wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ClientAlarm:TAG");
            }
            if (wl != null) {
                wl.acquire(10);
            }

            mAlarmReceiverCallback.startSendMessageFromCallback(mServerIp, mServerPort, mPacketSize, mPacketToIgnored);

            if (wl != null) {
                wl.release();
            }
        }
    }

    public void setAlarm(Context context, String serverIp, int serverPort, int packetSize, long delay, int packetToIgnored) {
        Log.d(TAG, "setAlarm");

        mServerIp = serverIp;
        mServerPort = serverPort;
        mPacketSize = packetSize;
        mPacketToIgnored = packetToIgnored;

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, OnAlarmReceiver.class);
        i.setAction("INTENT_ALARM_ACTION");
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        if (am != null) {
            am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, delay, pi);
        }
    }

    public void cancelAlarm(Context context) {
        Log.d(TAG, "cancelAlarm");
        Intent intent = new Intent(context, OnAlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(sender);
        }
    }
}
