package com.test.udpproject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

public class OnAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "OnAlarmReceiver";
    private AlarmReceiverCallback mAlarmReceiverCallback;
    private String mServerIp;
    private int mServerPort;
    private int mPacketSize;

    public OnAlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.d(TAG, "onReceive");
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = null;
        if (pm != null) {
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ClientAlarm:TAG");
        }
        if (wl != null) {
            wl.acquire(10);
        }

        mAlarmReceiverCallback.sendMessage(mServerIp, mServerPort, mPacketSize);

        if (wl != null) {
            wl.release();
        }
    }

    public void setAlarm(Context context, AlarmReceiverCallback alarmReceiverCallback, String serverIp, int serverPort, int packetSize, long delay)
    {
        Log.d(TAG, "setAlarm");

        mAlarmReceiverCallback = alarmReceiverCallback;
        mServerIp = serverIp;
        mServerPort = serverPort;
        mPacketSize = packetSize;

        AlarmManager am =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, OnAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        if (am != null) {
            am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), delay, pi);
        }
    }

    public void cancelAlarm(Context context)
    {
        Log.d(TAG, "cancelAlarm");
        Intent intent = new Intent(context, OnAlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(sender);
        }
    }
}
