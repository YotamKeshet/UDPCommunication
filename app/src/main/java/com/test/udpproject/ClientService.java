package com.test.udpproject;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class ClientService extends Service {

    private static final String TAG = "ClientService";

    private HandlerThread handlerThreadReceive;
    private DatagramSocket mDatagramSocket;

    private boolean isStartFlag = false;

    private int packetsTransmitted = 0;
    private int packetsReceived = 0;
    int messageNumber = 1;
    private long maxDelay = Long.MIN_VALUE;
    private long minDelay = Long.MAX_VALUE;
    private long sumOfDelays = 0;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        initialize();
        runAsForeground();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return START_NOT_STICKY;
    }

    private void runAsForeground() {
        Log.d(TAG, "runAsForeground");
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

        Notification mNotification = mNotificationBuilder.setOngoing(true).setSmallIcon(R.mipmap.ic_launcher).setContentTitle("UDP client is running in background").setContentText("").setCategory(Notification.CATEGORY_SERVICE).setTicker("").build();
        startForeground(1, mNotification);
    }

    private LocalBinder myService = new LocalBinder();

    private void initialize(){
        Log.d(TAG, "initialize");
        try {
            mDatagramSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return myService;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        handlerThreadReceive.quitSafely();
        return super.onUnbind(intent);
    }


    class LocalBinder extends Binder {
        ClientService getService() {
            return ClientService.this;
        }
    }

    public void startSendMessage(final String serverIp, final int serverPort, final int packetSize, final int delay){
        Log.d(TAG, "startSendMessage");

        if(isStartFlag){
            final HandlerThread handlerThread = new HandlerThread("sendMessagesHandlerThread");
            handlerThread.start();

            final Handler timerHandler = new Handler(handlerThread.getLooper());
            Runnable timerRunnable = new Runnable() {

                @Override
                public void run() {
                    sendMessage(serverIp, serverPort, packetSize);
                    startSendMessage(serverIp, serverPort, packetSize, delay);
                    handlerThread.quitSafely();
                }
            };

            timerHandler.postDelayed(timerRunnable, delay);
        }

    }

    public void startReceiveMessages(final int packetSize, final int delay){
        Log.d(TAG, "startReceiveMessages");
        isStartFlag = true;

        handlerThreadReceive = new HandlerThread("receiveMessagesHandlerThread");
        handlerThreadReceive.start();

        final Handler timerHandler = new Handler(handlerThreadReceive.getLooper());
        Runnable timerRunnable = new Runnable() {

            @Override
            public void run() {
                receiveMessage(packetSize, delay);
            }
        };

        timerHandler.postDelayed(timerRunnable, 0);
    }

    public void stopTest(){
        Log.d(TAG, "stopTest");
        isStartFlag = false;
    }

    public UpdateObject getDataToUpdateUI(){
        Log.d(TAG, "getDataToUpdateUI");
        return new UpdateObject(packetsTransmitted, packetsReceived, maxDelay, minDelay, sumOfDelays);
    }

    private void sendMessage(String serverIp, int serverPort, int packetSize){
        Log.d(TAG, "sendMessage");
        if(isStartFlag){
            try{
                // IP Address below is the IP address of that Device where server socket is opened.
                InetAddress serverAddr = InetAddress.getByName(serverIp);
                DatagramPacket dp;
                dp = new DatagramPacket(buildMessage(packetSize), packetSize, serverAddr, serverPort);
                mDatagramSocket.send(dp);
                packetsTransmitted++;
            }
            catch(UnknownHostException e){
                Log.e(TAG, "Fail to find host");
            }
            catch(SocketTimeoutException e){
                Log.e(TAG, "Timeout exception");
            }
            catch(IOException e){
                Log.e(TAG, "Fail to send or receive packet");
            }

        }
    }

    private void receiveMessage(int packetSize, int delay){
        Log.d(TAG, "receiveMessage");
        while(true){
            try{
                byte[] answerFromServer = new byte[packetSize];
                DatagramPacket dp = new DatagramPacket(answerFromServer, answerFromServer.length);
                mDatagramSocket.receive(dp);
                mDatagramSocket.setSoTimeout(delay + 10000);

                packetsReceived++;

                Log.d(TAG, "Message has been received.");

                analyzeData(answerFromServer);
            }
            catch(UnknownHostException e){
                Log.e(TAG, "Fail to find host");
            }
            catch(SocketTimeoutException e){
                Log.d(TAG, "Timeout exception");
                if(!isStartFlag){
                    return;
                }
            }
            catch(IOException e){
                Log.e(TAG, "Fail to send or receive packet");
            }

        }

    }

    private byte[] buildMessage(int messageSize){
        byte[] counterBytes = ByteBuffer.allocate(4).putInt(messageNumber).array();
        Log.d(TAG, "messageNumber = " + messageNumber + "  counterBytes = " + Arrays.toString(counterBytes));

        long timestamp = TimeUnit.MILLISECONDS.toMillis(new Date().getTime());
        byte[] timestampBytes = ByteBuffer.allocate(28).putLong(timestamp).array();
        Log.d(TAG, "timestamp = " + timestamp + "  timestampBytes = " + Arrays.toString(timestampBytes));

        byte[] padding = new byte[messageSize - 28];

        for(int i = 0 ; i < padding.length ; i++){
            padding[i] = 0x00;
        }

        byte[] message = new byte[messageSize];

        for(int i = 0 ; i < messageSize ; i ++){
            if(i < 4){
                message[i] = counterBytes[i];
            }
            else if(i < 32){
                message[i] = timestampBytes[i - 4];
            }
            else{
                message[i] = padding[i - 32];
            }
        }

        Log.d(TAG, "message = " + Arrays.toString(message));

        messageNumber++;

        return message;
    }

    private void analyzeData(byte[] data){
        byte[] clientMessageCounterByteArray = new byte[4];
        byte[] clientTimestampByteArray = new byte[8];

        for(int i = 0 ; i < 8 ; i++){
            if(i < 4){
                clientMessageCounterByteArray[i] = data[i];
            }

            clientTimestampByteArray[i] = data[i+4];
        }

        int clientMessageCounter = bytesToInt(clientMessageCounterByteArray);
        long clientTimestamp = bytesToLong(clientTimestampByteArray);

        Log.d(TAG, "clientMessageCounter = " + clientMessageCounter + "  clientTimestamp = " + clientTimestamp);

        long delay = TimeUnit.MILLISECONDS.toMillis(new Date().getTime()) - clientTimestamp;

        Log.d(TAG, "delay = " + delay);

        if(delay > maxDelay){
            maxDelay = delay;
        }

        if(delay < minDelay){
            minDelay = delay;
        }

        sumOfDelays += delay;
    }

    public void resetTestParameters(){
        messageNumber = 1;
        packetsTransmitted = 0;
        packetsReceived = 0;
        maxDelay = Long.MIN_VALUE;
        minDelay = Long.MAX_VALUE;
        sumOfDelays = 0;
    }

    private int bytesToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) |
                ((bytes[1] & 0xFF) << 16) |
                ((bytes[2] & 0xFF) << 8 ) |
                ((bytes[3] & 0xFF));
    }

    public long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.flip();
        return buffer.getLong();
    }

}
