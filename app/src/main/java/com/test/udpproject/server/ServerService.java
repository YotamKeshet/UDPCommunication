package com.test.udpproject.server;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.test.udpproject.R;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ServerService extends Service {

    private static final String TAG = "ServerService";
    private LocalBinder myService = new LocalBinder();
    private boolean isStartFlag = false;

    @Override
    public void onCreate() {
        super.onCreate();

        runAsForeground();
    }

    private void runAsForeground() {
        String channelName = getString(R.string.app_name);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mNotificationBuilder = new NotificationCompat.Builder(this, "com.test.udpproject");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel("com.test.udpproject", channelName, NotificationManager.IMPORTANCE_DEFAULT);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(chan);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mNotificationBuilder.setPriority(NotificationManager.IMPORTANCE_MIN);
        }

        Notification mNotification = mNotificationBuilder.setOngoing(true).setSmallIcon(R.mipmap.ic_launcher).setContentTitle("UDP server is running in background").setContentText("").setCategory(Notification.CATEGORY_SERVICE).setTicker("").build();
        startForeground(1, mNotification);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "startServerSocket");
        return myService;
    }

    public void startServerSocket(final int packetSize, final int serverPort) {
        Log.d(TAG, "startServerSocket");

        isStartFlag = true;


        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (isStartFlag) {
                    byte[] msg = new byte[packetSize];
                    DatagramPacket dp = new DatagramPacket(msg, msg.length);
                    try (DatagramSocket ds = new DatagramSocket(serverPort)) {
                        //ds.setSoTimeout(50000);
                        ds.receive(dp);

                        Log.d(TAG, "Message has been received.");

                        dp = new DatagramPacket(dp.getData(), dp.getData().length, dp.getAddress(), dp.getPort());
                        ds.send(dp);

                        Log.d(TAG, "Message has been sent.");

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        });

        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    public void stopServerSocket() {
        isStartFlag = false;
    }

    class LocalBinder extends Binder {
        ServerService getService() {
            return ServerService.this;
        }
    }
}
