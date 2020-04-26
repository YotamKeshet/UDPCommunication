package com.test.udpproject.icmp;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class PingTask extends AsyncTask<String, Void, Void> {
    private static final String TAG = "PingTask";

    private PipedOutputStream mPOut;
    private PipedInputStream mPIn;
    private LineNumberReader mReader;
    private Process mProcess;
    private PingTaskCallback mPingTaskCallback;
    private int mCount;
    private int mWait;
    private int mPacketTransmitted;
    private int mPacketReceived;
    private long mMinDelay;
    private long mMaxDelay;
    private long mSumDelay;

    PingTask(PingTaskCallback pingTaskCallback, int count, int wait) {
        mPingTaskCallback = pingTaskCallback;
        mCount = count;
        mWait = wait;

        resetParameters();
    }

    private void resetParameters(){
        Log.d(TAG, "resetParameters");
        mPacketTransmitted = 0;
        mPacketReceived = 0;
        mMinDelay = Long.MAX_VALUE;
        mMaxDelay = Long.MIN_VALUE;
        mSumDelay = 0;
    }

    @Override
    protected void onPreExecute() {
        Log.d(TAG, "onPreExecute");

        mPOut = new PipedOutputStream();
        try {
            mPIn = new PipedInputStream(mPOut);
            mReader = new LineNumberReader(new InputStreamReader(mPIn));
        } catch (IOException e) {
            cancel(true);
        }

    }

    void stop() {
        Log.d(TAG, "stop");

        Process p = mProcess;
        if (p != null) {
            p.destroy();
        }
        cancel(true);

        resetParameters();
    }

    @Override
    protected Void doInBackground(String... params) {
        Log.d(TAG, "doInBackground");

        try {
            mProcess = new ProcessBuilder()
                    .command("/system/bin/ping" , params[0])
                    .redirectErrorStream(true)
                    .start();

            try {
                InputStream in = mProcess.getInputStream();
                OutputStream out = mProcess.getOutputStream();
                byte[] buffer = new byte[1024];

                int len;

                while(mCount != 0){
                    len = in.read(buffer);
                    if(len != -1){
                        Log.d(TAG, "write, len = " + len);
                        mPOut.write(buffer, 0, len);
                        mPacketTransmitted++;
                        publishProgress();

                        try {
                            Thread.sleep(mWait);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    mCount--;
                }

                out.close();
                in.close();
                mPOut.close();
                mPIn.close();
            } finally {
                mProcess.destroy();
                mProcess = null;
            }
        } catch (IOException ignored) {
        }
        return null;
    }
    @Override
    protected void onProgressUpdate(Void... values) {
        try {
            // Is a line ready to read from the "ping" command?
            while (mReader.ready()) {
                String answerFromServer = mReader.readLine();
                Log.d(TAG, answerFromServer);
                if(answerFromServer.contains("from 8.8.8.8:")){
                    String[] strings = answerFromServer.split(" ");

                    String packetReceivedString = strings[4];
                    String[] packetReceivedStrings = packetReceivedString.split("=");

                    mPacketReceived = Integer.valueOf(packetReceivedStrings[1]);

                    String stringDelay = strings[6];
                    String[] delayStrings = stringDelay.split("=");
                    double doubleDelay = Double.valueOf(delayStrings[1]);
                    long delay = (long)doubleDelay;

                    if(delay > mMaxDelay){
                        mMaxDelay = delay;
                    }

                    if(delay < mMinDelay){
                        mMinDelay = delay;
                    }

                    mSumDelay += delay;

                    mPingTaskCallback.update(mPacketTransmitted, mPacketReceived, mMinDelay, mMaxDelay, mSumDelay);
                }
                else{
                    Log.e(TAG, "Error Not from server..");
                }

            }
        } catch (IOException ignored) {
        }
    }

}

