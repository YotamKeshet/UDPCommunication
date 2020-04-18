package com.test.udpproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
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
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class UDPServerSocketActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "UDPServerSocketActivity";

    private int mServerPort;

    final Handler handler = new Handler();
    private Button buttonStartReceiving;
    private Button buttonStopReceiving;
    private EditText mPortEditText;

    private TextView mTextViewDataFromClient;
    private TextView mAverageDelayTextView;
    private TextView mMaxDelayTextView;
    private TextView mMinDelayTextView;
    private TextView mLossRatioTextView;

    private int messageCounter = 0;
    private int numberOfMessages = 0;
    private int numberOfLossPackets = 0;
    private long maxDelay = Long.MIN_VALUE;
    private long minDelay = Long.MAX_VALUE;
    private long sumOfDelays = 0;


    private boolean isStartFlag = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_udpserver_socket);

        initializeViews();
        setListeners();
    }

    private void initializeViews(){
        buttonStartReceiving =  findViewById(R.id.btn_start_receiving);
        buttonStopReceiving = findViewById(R.id.btn_stop_receiving);
        mTextViewDataFromClient = findViewById(R.id.tv_data_from_client);
        mPortEditText = findViewById(R.id.portEditText);
        mAverageDelayTextView = findViewById(R.id.averageDelay);
        mMaxDelayTextView = findViewById(R.id.maxDelay);
        mMinDelayTextView = findViewById(R.id.minDelay);
        mLossRatioTextView = findViewById(R.id.lossRatio);
    }

    private void setListeners(){
        buttonStartReceiving.setOnClickListener(this);
        buttonStopReceiving.setOnClickListener(this);
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

    private void startServerSocket() {
        Log.d(TAG, "startServerSocket");
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                while(isStartFlag){
                    byte[] msg = new byte[28];
                    DatagramPacket dp = new DatagramPacket(msg, msg.length);
                    try (DatagramSocket ds = new DatagramSocket(mServerPort)) {
                        //ds.setSoTimeout(50000);
                        ds.receive(dp);

                        Log.d(TAG, "Message has been received.");

                        updateUI(dp.getData());

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        });

        thread.start();
    }

    private void updateUI(final byte[] data) {
        Log.d(TAG, "updateUI");

        handler.post(new Runnable() {
            @Override
            public void run() {
                // parseMessage
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
                analyzeMessage(clientMessageCounter, clientTimestamp);

//                if(mTextViewDataFromClient.getText().toString().equals("")){
//                    StringBuilder stringBuilder = new StringBuilder();
//                    stringBuilder.append("MessageNumber: ").append(clientMessageCounter).append("  ").append("Timestamp: ").append(clientTimestamp);
//                    mTextViewDataFromClient.setText(stringBuilder);
//                }
//                else{
//                    String s = mTextViewDataFromClient.getText().toString();
//                    StringBuilder stringBuilder = new StringBuilder();
//                    stringBuilder.append(s).append("\n").append("MessageNumber: ").append(clientMessageCounter).append("  ").append("Timestamp: ").append(clientTimestamp);
//                    mTextViewDataFromClient.setText(stringBuilder);
//                }
            }
        });
    }

    private void analyzeMessage(int clientMessageCounter, long clientTimestamp){
        Log.d(TAG, "clientMessageCounter = " + clientMessageCounter + "  clientTimestamp = " + clientTimestamp);

        numberOfMessages++;

        if(!(clientMessageCounter == messageCounter + 1)){
            numberOfLossPackets++;
        }

        messageCounter = clientMessageCounter;

        long delay = TimeUnit.MILLISECONDS.toMillis(new Date().getTime()) - clientTimestamp;

        Log.d(TAG, "delay = " + delay);

        if(delay > maxDelay){
            maxDelay = delay;
        }

        if(delay < minDelay){
            minDelay = delay;
        }

        sumOfDelays += delay;
        long averageDelay = sumOfDelays / numberOfMessages;
        double lossRatio = (numberOfLossPackets / numberOfMessages) * 100;

        StringBuilder averageDelayStringBuilder = new StringBuilder();
        averageDelayStringBuilder.append(getString(R.string.average_delay)).append(averageDelay).append("ms");

        StringBuilder maxDelayStringBuilder = new StringBuilder();
        maxDelayStringBuilder.append(getString(R.string.max_delay)).append(maxDelay).append("ms");

        StringBuilder minDelayStringBuilder = new StringBuilder();
        minDelayStringBuilder.append(getString(R.string.min_delay)).append(minDelay).append("ms");

        StringBuilder lossRatioStringBuilder = new StringBuilder();
        lossRatioStringBuilder.append(getString(R.string.loss_ratio)).append(lossRatio).append("%");

        mAverageDelayTextView.setText(averageDelayStringBuilder);
        mMaxDelayTextView.setText(maxDelayStringBuilder);
        mMinDelayTextView.setText(minDelayStringBuilder);
        mLossRatioTextView.setText(lossRatioStringBuilder);
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

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick");

        switch (v.getId()) {

            case R.id.btn_start_receiving:
                resetTestParameters();
                clearViews();
                isStartFlag = true;
                getEditTextParameters();
                startServerSocket();

                buttonStartReceiving.setEnabled(false);
                buttonStopReceiving.setEnabled(true);
                break;

            case R.id.btn_stop_receiving:
                isStartFlag = false;

                buttonStartReceiving.setEnabled(true);
                buttonStopReceiving.setEnabled(false);
                break;
        }
    }

    private void getEditTextParameters(){
        mServerPort = Integer.valueOf(mPortEditText.getText().toString());

        Log.d(TAG, "mServerPort = " + mServerPort);
    }

    private void resetTestParameters(){
        messageCounter = 0;
        numberOfMessages = 0;
        numberOfLossPackets = 0;
        maxDelay = Integer.MIN_VALUE;
        minDelay = Integer.MAX_VALUE;
        sumOfDelays = 0;
    }

    private void clearViews(){
        mAverageDelayTextView.setText(R.string.average_delay);
        mMaxDelayTextView.setText(R.string.max_delay);
        mMinDelayTextView.setText(R.string.min_delay);
        mLossRatioTextView.setText(R.string.loss_ratio);
        mTextViewDataFromClient.setText("");
    }
}