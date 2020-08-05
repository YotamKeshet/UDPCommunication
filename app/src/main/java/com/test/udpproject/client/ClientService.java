package com.test.udpproject.client;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.opencsv.CSVWriter;
import com.test.udpproject.R;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class ClientService extends Service {

    private static final String TAG = "ClientService";
    private static final String TESTS_FOLDER_NAME = "BluetoothTestStatistics";
    int messageNumber = 1;
    private HandlerThread handlerThreadReceive = null;
    private DatagramSocket mDatagramSocket;
    private boolean isStartFlag = false;
    private int packetsTransmitted = 0;
    private int packetsReceived = 0;
    private long maxDelay = Long.MIN_VALUE;
    private long minDelay = Long.MAX_VALUE;
    private long sumOfDelays = 0;

    private OnAlarmReceiver mOnAlarmReceiver;
    private AlarmReceiverCallback mAlarmReceiverCallback;

    private CSVWriter mCSVwriter;
    private LocalBinder myService = new LocalBinder();

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

    private void initialize() {
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
        setAlarmCallback();
        mOnAlarmReceiver = new OnAlarmReceiver(mAlarmReceiverCallback);
        return myService;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (handlerThreadReceive != null) {
            handlerThreadReceive.quitSafely();
        }

        return super.onUnbind(intent);
    }

    private void setAlarmCallback() {
        mAlarmReceiverCallback = new AlarmReceiverCallback() {
            @Override
            public void startSendMessageFromCallback(final String serverIp, final int serverPort, final int packetSize, final int packetToIgnored) {

                final HandlerThread handlerThread = new HandlerThread("sendMessagesHandlerThread");
                handlerThread.start();

                final Handler timerHandler = new Handler(handlerThread.getLooper());
                Runnable timerRunnable = new Runnable() {

                    @Override
                    public void run() {
                        sendMessage(serverIp, serverPort, packetSize, packetToIgnored);
                        handlerThread.quitSafely();
                    }
                };

                timerHandler.postDelayed(timerRunnable, 0);

            }
        };
    }

    public void startSendMessage(final String serverIp, final int serverPort, final int packetSize, final int delay, final int packetToIgnored) {
        Log.d(TAG, "startSendMessage, packetSize = " + packetSize + "  delay = " + delay);

        if (isStartFlag) {

            if (delay > 60000) {
                mOnAlarmReceiver.setAlarm(this, serverIp, serverPort, packetSize, delay, packetToIgnored);
            } else {
                final HandlerThread handlerThread = new HandlerThread("sendMessagesHandlerThread");
                handlerThread.start();

                final Handler timerHandler = new Handler(handlerThread.getLooper());
                Runnable timerRunnable = new Runnable() {

                    @Override
                    public void run() {
                        sendMessage(serverIp, serverPort, packetSize, packetToIgnored);
                        startSendMessage(serverIp, serverPort, packetSize, delay, packetToIgnored);
                        handlerThread.quitSafely();
                    }
                };

                timerHandler.postDelayed(timerRunnable, delay);
            }
        }
    }

    public void startReceiveMessages(final int packetSize, final int delay, final int jitterBuffer, final int packetsToIgnored) {
        Log.d(TAG, "startReceiveMessages");
        isStartFlag = true;

        handlerThreadReceive = new HandlerThread("receiveMessagesHandlerThread");
        handlerThreadReceive.start();

        final Handler timerHandler = new Handler(handlerThreadReceive.getLooper());
        Runnable timerRunnable = new Runnable() {

            @Override
            public void run() {
                receiveMessage(packetSize, delay, jitterBuffer, packetsToIgnored);
            }
        };

        timerHandler.postDelayed(timerRunnable, 0);
    }

    public void stopTest() {
        Log.d(TAG, "stopTest");
        mOnAlarmReceiver.cancelAlarm(this);
        isStartFlag = false;
    }

    public UpdateObject getDataToUpdateUI() {
        Log.d(TAG, "getDataToUpdateUI");
        return new UpdateObject(packetsTransmitted, packetsReceived, maxDelay, minDelay, sumOfDelays);
    }

    private void sendMessage(String serverIp, int serverPort, int packetSize, int packetsToIgnored) {
        Log.d(TAG, "startSendMessageFromCallback");
        if (isStartFlag) {
            try {
                // IP Address below is the IP address of that Device where server socket is opened.
                InetAddress serverAddr = InetAddress.getByName(serverIp);
                DatagramPacket dp;
                dp = new DatagramPacket(buildMessage(packetSize), packetSize, serverAddr, serverPort);
                mDatagramSocket.send(dp);

                if (messageNumber - 1 > packetsToIgnored) {
                    packetsTransmitted++;
                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "Fail to find host");
            } catch (SocketTimeoutException e) {
                Log.e(TAG, "Timeout exception");
            } catch (IOException e) {
                Log.e(TAG, "Fail to send or receive packet");
            }

        }
    }

    private void receiveMessage(int packetSize, int delay, int jitterBuffer, int packetsToIgnored) {
        Log.d(TAG, "receiveMessage");
        while (true) {
            try {
                byte[] answerFromServer = new byte[packetSize];
                DatagramPacket dp = new DatagramPacket(answerFromServer, answerFromServer.length);
                mDatagramSocket.setSoTimeout(10000);
                mDatagramSocket.receive(dp);

                packetsReceived++;

                Log.d(TAG, "Message has been received.");

                analyzeData(answerFromServer, jitterBuffer, packetsToIgnored);
            } catch (UnknownHostException e) {
                Log.e(TAG, "Fail to find host");
            } catch (SocketTimeoutException e) {
                Log.d(TAG, "Timeout exception");
                if (!isStartFlag) {
                    return;
                }
            } catch (IOException e) {
                Log.e(TAG, "Fail to send or receive packet");
            }

        }

    }

    private byte[] buildMessage(int messageSize) {
        byte[] counterBytes = ByteBuffer.allocate(4).putInt(messageNumber).array();
        Log.d(TAG, "messageNumber = " + messageNumber + "  counterBytes = " + Arrays.toString(counterBytes));

        long timestamp = System.nanoTime();

        byte[] timestampBytes = ByteBuffer.allocate(28).putLong(timestamp).array();
        Log.d(TAG, "timestamp = " + timestamp + "  timestampBytes = " + Arrays.toString(timestampBytes));

        byte[] padding = new byte[messageSize - 28];

        for (int i = 0; i < padding.length; i++) {
            padding[i] = 0x00;
        }

        byte[] message = new byte[messageSize];

        for (int i = 0; i < messageSize; i++) {
            if (i < 4) {
                message[i] = counterBytes[i];
            } else if (i < 32) {
                message[i] = timestampBytes[i - 4];
            } else {
                message[i] = padding[i - 32];
            }
        }

        Log.d(TAG, "message = " + Arrays.toString(message));

        messageNumber++;

        return message;
    }

    private synchronized void analyzeData(byte[] data, int jitterBuffer, int packetsToIgnored) {
        byte[] clientMessageCounterByteArray = new byte[4];
        byte[] clientTimestampByteArray = new byte[8];

        for (int i = 0; i < 8; i++) {
            if (i < 4) {
                clientMessageCounterByteArray[i] = data[i];
            }

            clientTimestampByteArray[i] = data[i + 4];
        }

        int clientMessageCounter = bytesToInt(clientMessageCounterByteArray);
        long clientTimestamp = bytesToLong(clientTimestampByteArray);

        Log.d(TAG, "clientMessageCounter = " + clientMessageCounter + "  clientTimestamp = " + clientTimestamp);

        if (packetsToIgnored >= clientMessageCounter) {
            packetsReceived--;
            Log.d(TAG, "ignore packet...");
            return;
        }

        long delayNano = System.nanoTime() - clientTimestamp;
        long delay = TimeUnit.NANOSECONDS.toMillis(delayNano);
        Log.d(TAG, "delay = " + delay);

        if (delay < jitterBuffer) {

            String[] packetData = {String.valueOf(clientMessageCounter), String.valueOf(delay), String.valueOf(clientTimestamp)};
            addToStatisticFile(packetData);

            if (delay > maxDelay) {
                maxDelay = delay;
            }

            if (delay < minDelay) {
                minDelay = delay;
            }

            sumOfDelays += delay;
        } else {
            packetsReceived--;
        }
    }

    public void resetTestParameters() {
        messageNumber = 1;
        packetsTransmitted = 0;
        packetsReceived = 0;
        maxDelay = Long.MIN_VALUE;
        minDelay = Long.MAX_VALUE;
        sumOfDelays = 0;
    }

    private int bytesToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16) | ((bytes[2] & 0xFF) << 8) | ((bytes[3] & 0xFF));
    }

    private long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.flip();
        return buffer.getLong();
    }

    public void startWriteStatisticFile() {
        String baseDir = Environment.getExternalStorageDirectory() + File.separator + TESTS_FOLDER_NAME;

        File baseDirectory = new File(baseDir);

        boolean baseDirectoryMkdirResult = false;
        try {
            if (!baseDirectory.exists()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Files.createDirectories(baseDirectory.toPath());
                    baseDirectoryMkdirResult = true;
                } else {
                    baseDirectoryMkdirResult = baseDirectory.mkdir();
                }
            } else {
                baseDirectoryMkdirResult = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


        if (baseDirectoryMkdirResult) {
            String testDir = baseDir + File.separator + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());
            Log.d(TAG, "CSV Folder Path - " + baseDir);

            File testFolder = new File(testDir);

            boolean testFolderMkdirResult = false;
            if (!testFolder.exists()) {
                testFolderMkdirResult = testFolder.mkdir();
            } else {
                Log.e(TAG, "Fail to create Directory for CSV statistic file");
            }

            if (testFolderMkdirResult) {
                String fileName = "AnalysisData.csv";
                String filePath = testDir + File.separator + fileName;
                File f = new File(filePath);

                Log.d(TAG, "CSV File path - " + filePath);

                if (!f.exists() && !f.isDirectory()) {
                    try {
                        Log.d(TAG, "Creating new CSV file writer");
                        mCSVwriter = new CSVWriter(new FileWriter(filePath));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    private void addToStatisticFile(String[] data) {
        mCSVwriter.writeNext(data);
    }

    public void stopWriteStatisticFile() {
        try {
            mCSVwriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class LocalBinder extends Binder {
        ClientService getService() {
            return ClientService.this;
        }
    }


}
