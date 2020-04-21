package com.test.udpproject;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ServerService extends Service {

    private static final String TAG = "ServerService";
    private LocalBinder myService = new LocalBinder();
    private boolean isStartFlag = false;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "startServerSocket");
        return myService;
    }


    class LocalBinder extends Binder {
        ServerService getService() {
            return ServerService.this;
        }
    }

    public void startServerSocket(final int packetSize, final int serverPort) {
        Log.d(TAG, "startServerSocket");

        isStartFlag = true;



        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                while(isStartFlag){
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

    public void stopServerSocket(){
        isStartFlag = false;
    }
}
