package com.test.udpproject;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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

public class UDPClientSocketActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "UDPClientSocketActivity";
    private EditText mIPEditText;
    private EditText mPortEditText;
    private EditText mPacketSizeEditText;
    private EditText mDelayEditText;
    private TextView mAverageDelayTextView;
    private TextView mMaxDelayTextView;
    private TextView mMinDelayTextView;
    private TextView mLossRatioTextView;
    private TextView mPacketsTransmittedTextView;
    private TextView mPacketsReceivedTextView;
    private TextView mElapsedTimeTextView;

    private int packetsTransmitted = 0;
    private int packetsReceived = 0;
    private long maxDelay = Long.MIN_VALUE;
    private long minDelay = Long.MAX_VALUE;
    private long sumOfDelays = 0;
    private long mElapsedTime = 0;
    int messageNumber = 1;



    private Button mButtonStart;
    private Button mButtonStop;

    private int mServerPort;
    private String mServerIp;
    private int mPacketSize;
    private int mDelay;

    private boolean isStartFlag = false;

    private AnalyzeThread analyzeThread;
    private AnalyzeRunnable analyzeRunnable;
    private DatagramSocket mDatagramSocket;

    HandlerThread handlerThreadReceive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_udpclient_socket);

        initializeViews();
        setListeners();

        try {
            mDatagramSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        analyzeRunnable = new AnalyzeRunnable();
        analyzeThread = new AnalyzeThread();
        analyzeThread.setPriority(Thread.MAX_PRIORITY);
        analyzeThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handlerThreadReceive.quitSafely();
    }

    private void initializeViews(){
        mButtonStart = findViewById(R.id.start);
        mButtonStop = findViewById(R.id.stop);
        mPortEditText = findViewById(R.id.portEditText);
        mIPEditText = findViewById(R.id.ipEditText);
        mPacketSizeEditText = findViewById(R.id.packetSizeEditText);
        mDelayEditText = findViewById(R.id.delayEditText);
        mAverageDelayTextView = findViewById(R.id.averageDelay);
        mMaxDelayTextView = findViewById(R.id.maxDelay);
        mMinDelayTextView = findViewById(R.id.minDelay);
        mLossRatioTextView = findViewById(R.id.lossRatio);
        mPacketsTransmittedTextView = findViewById(R.id.packetsTransmitted);
        mPacketsReceivedTextView = findViewById(R.id.packetsReceived);
        mElapsedTimeTextView = findViewById(R.id.elapsedTime);
    }

    private void setListeners(){
        mButtonStart.setOnClickListener(this);
        mButtonStop.setOnClickListener(this);
        setIpFilter();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void setIpFilter(){
        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start,
                                       int end, Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart) +
                            source.subSequence(start, end) +
                            destTxt.substring(dend);
                    if (!resultingTxt.matches ("^\\d{1,3}(\\." +
                            "(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (int i=0; i<splits.length; i++) {
                            if (Integer.valueOf(splits[i]) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }
        };

        mIPEditText.setFilters(filters);
    }

    public synchronized void updateUI(){
        long averageDelay = 0;
        if(packetsReceived != 0){
            averageDelay = sumOfDelays / (packetsReceived);
        }

        double lossRatio = 0;
        if(packetsTransmitted != 0){
            lossRatio = ((double)(packetsTransmitted - packetsReceived) / (double)packetsTransmitted) * 100;
        }

        StringBuilder averageDelayStringBuilder = new StringBuilder();
        averageDelayStringBuilder.append(getString(R.string.average_delay)).append(averageDelay).append("ms");

        StringBuilder maxDelayStringBuilder = new StringBuilder();
        maxDelayStringBuilder.append(getString(R.string.max_delay)).append(maxDelay).append("ms");

        StringBuilder minDelayStringBuilder = new StringBuilder();
        minDelayStringBuilder.append(getString(R.string.min_delay)).append(minDelay).append("ms");

        StringBuilder lossRatioStringBuilder = new StringBuilder();
        lossRatioStringBuilder.append(getString(R.string.loss_ratio)).append(lossRatio).append("%");

        StringBuilder packetsTransmittedStringBuilder = new StringBuilder();
        packetsTransmittedStringBuilder.append(getString(R.string.packets_transmitted)).append(packetsTransmitted);

        StringBuilder packetsReceivedStringBuilder = new StringBuilder();
        packetsReceivedStringBuilder.append(getString(R.string.packets_received)).append(packetsReceived);

        StringBuilder elapsedTimeStringBuilder = new StringBuilder();
        elapsedTimeStringBuilder.append(getString(R.string.elapsed_time)).append(TimeUnit.MILLISECONDS.toSeconds(new Date().getTime()) - mElapsedTime).append(" sec");

        mAverageDelayTextView.setText(averageDelayStringBuilder);
        mMaxDelayTextView.setText(maxDelayStringBuilder);
        mMinDelayTextView.setText(minDelayStringBuilder);
        mLossRatioTextView.setText(lossRatioStringBuilder);
        mPacketsTransmittedTextView.setText(packetsTransmittedStringBuilder);
        mPacketsReceivedTextView.setText(packetsReceivedStringBuilder);
        mElapsedTimeTextView.setText(elapsedTimeStringBuilder);
    }

    private void getEditTextParameters(){
        mServerIp = mIPEditText.getText().toString();
        mServerPort = Integer.valueOf(mPortEditText.getText().toString());
        mPacketSize = Integer.valueOf(mPacketSizeEditText.getText().toString());
        mDelay = Integer.valueOf(mDelayEditText.getText().toString());

        Log.d(TAG, "mServerIp = " + mServerIp + "  mServerPort = " + mServerPort + "  mPacketSize = " + mPacketSize + "  mDelay = " + mDelay);
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick");
        if (v.getId() == R.id.start) {
            mButtonStart.setEnabled(false);
            mButtonStop.setEnabled(true);
            isStartFlag = true;

            resetTestParameters();
            clearViews();
            mElapsedTime = TimeUnit.MILLISECONDS.toSeconds(new Date().getTime());

            getEditTextParameters();
            startReceiveMessages();
            startSendMessage();
        }
        else if(v.getId() == R.id.stop){
            mButtonStart.setEnabled(true);
            mButtonStop.setEnabled(false);
            isStartFlag = false;
            updateUI();
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

    private void startSendMessage(){
        Log.d(TAG, "startSendMessage");

        if(isStartFlag){
            final HandlerThread handlerThread = new HandlerThread("sendMessagesHandlerThread");
            handlerThread.start();

            final Handler timerHandler = new Handler(handlerThread.getLooper());
            Runnable timerRunnable = new Runnable() {

                @Override
                public void run() {

                    sendMessage();
                    startSendMessage();
                    handlerThread.quitSafely();
                }
            };

            timerHandler.postDelayed(timerRunnable, mDelay);
        }

    }

    private void startReceiveMessages(){
        if(isStartFlag){
            handlerThreadReceive = new HandlerThread("receiveMessagesHandlerThread");
            handlerThreadReceive.start();

            final Handler timerHandler = new Handler(handlerThreadReceive.getLooper());
            Runnable timerRunnable = new Runnable() {

                @Override
                public void run() {
                    receiveMessage();
                }
            };

            timerHandler.postDelayed(timerRunnable, 0);
        }
    }

    private void sendMessage(){
        Log.d(TAG, "sendMessage");
        if(isStartFlag){
            try{
                // IP Address below is the IP address of that Device where server socket is opened.
                InetAddress serverAddr = InetAddress.getByName(mServerIp);
                DatagramPacket dp;
                dp = new DatagramPacket(buildMessage(mPacketSize), mPacketSize, serverAddr, mServerPort);
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

    private void receiveMessage(){
        Log.d(TAG, "receiveMessage");
        while(true){
            try{
                byte[] answerFromServer = new byte[mPacketSize];
                DatagramPacket dp = new DatagramPacket(answerFromServer, answerFromServer.length);
                mDatagramSocket.receive(dp);
                mDatagramSocket.setSoTimeout(mDelay + 10000);

                packetsReceived++;

                Log.d(TAG, "Message has been received.");

                analyzeRunnable.setData(answerFromServer);
                analyzeThread.setRunnable(analyzeRunnable);
                analyzeThread.run();
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

    private void resetTestParameters(){
        messageNumber = 1;
        packetsTransmitted = 0;
        packetsReceived = 0;
        maxDelay = Long.MIN_VALUE;
        minDelay = Long.MAX_VALUE;
        sumOfDelays = 0;
        mElapsedTime = 0;
    }

    private void clearViews(){
        mAverageDelayTextView.setText(getString(R.string.average_delay));
        mMaxDelayTextView.setText(getString(R.string.max_delay));
        mMinDelayTextView.setText(getString(R.string.min_delay));
        mLossRatioTextView.setText(getString(R.string.loss_ratio));
        mPacketsTransmittedTextView.setText(getString(R.string.packets_transmitted));
        mPacketsReceivedTextView.setText(getString(R.string.packets_received));
        mElapsedTimeTextView.setText(getString(R.string.elapsed_time));
    }

    public class AnalyzeRunnable implements Runnable{
        private byte[] data;

        private void setData(byte[] data){
            this.data = data;
        }

        @Override
        public void run() {
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

            updateUI();
        }
    }

    public class AnalyzeThread extends Thread {

        private Runnable runnable;

        private void setRunnable(Runnable runnable){
            this.runnable = runnable;
        }

        public void run() {
            runOnUiThread(runnable);
        }
    }

}
