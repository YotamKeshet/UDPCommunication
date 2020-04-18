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
    private TextView mSendMessageTextView;

    private Button mButtonStart;
    private Button mButtonStop;

    private int mServerPort;
    private String mServerIp;
    private int mPacketSize;
    private int mDelay;

    private boolean isStartFlag = false;
    int messageNumber = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_udpclient_socket);

        initializeViews();
        setListeners();
    }

    private void initializeViews(){
        mButtonStart = findViewById(R.id.start);
        mButtonStop = findViewById(R.id.stop);
        mPortEditText = findViewById(R.id.portEditText);
        mIPEditText = findViewById(R.id.ipEditText);
        mPacketSizeEditText = findViewById(R.id.packetSizeEditText);
        mDelayEditText = findViewById(R.id.delayEditText);
        mSendMessageTextView = findViewById(R.id.sentMessageTextView);
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
            resetTestParameters();
            clearViews();
            isStartFlag = true;
            getEditTextParameters();
            startSendMessage();
        }
        else if(v.getId() == R.id.stop){
            messageNumber = 0;
            isStartFlag = false;
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


//        presentMessageThatSent(messageNumber, timestamp);
        messageNumber++;
        return message;
    }

    void presentMessageThatSent(final int messageNumber, final long timestamp){
        new Thread() {
            public void run() {

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if(mSendMessageTextView.getText().toString().equals("")){
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("MessageNumber: ").append(messageNumber).append("  ").append("Timestamp: ").append(timestamp);
                                mSendMessageTextView.setText(stringBuilder);
                            }
                            else{
                                String s = mSendMessageTextView.getText().toString();
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append(s).append("\n").append("MessageNumber: ").append(messageNumber).append("  ").append("Timestamp: ").append(timestamp);
                                mSendMessageTextView.setText(stringBuilder);
                            }
                        }
                    });
            }
        }.start();
    }

    private void startSendMessage(){
        Log.d(TAG, "startSendMessage");

        if(isStartFlag){
            HandlerThread handlerThread = new HandlerThread("sendMessagesHandlerThread");
            handlerThread.start();

            final Handler timerHandler = new Handler(handlerThread.getLooper());
            Runnable timerRunnable = new Runnable() {

                @Override
                public void run() {
                    sendMessage();
                    startSendMessage();
                }
            };

            timerHandler.postDelayed(timerRunnable, mDelay);
        }

    }

    private void sendMessage(){
        Log.d(TAG, "sendMessage");
        if(isStartFlag){
            try (DatagramSocket ds = new DatagramSocket()) {
                // IP Address below is the IP address of that Device where server socket is opened.
                InetAddress serverAddr = InetAddress.getByName(mServerIp);
                DatagramPacket dp;
                dp = new DatagramPacket(buildMessage(mPacketSize), mPacketSize, serverAddr, mServerPort);
                ds.send(dp);

                Log.d(TAG, "Message has been sent.");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void resetTestParameters(){
        messageNumber = 1;
    }

    private void clearViews(){
        mSendMessageTextView.setText("");
    }

}
