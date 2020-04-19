package com.test.udpproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPServerSocketActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "UDPServerSocketActivity";

    private int mServerPort;

    private Button buttonStartReceiving;
    private Button buttonStopReceiving;
    private EditText mPortEditText;

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
        mPortEditText = findViewById(R.id.portEditText);
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

                        dp = new DatagramPacket(dp.getData(), dp.getData().length, dp.getAddress(), dp.getPort());
                        ds.send(dp);

                        Log.d(TAG, "Message has been sent.");

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        });

        thread.start();
    }



    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick");

        switch (v.getId()) {

            case R.id.btn_start_receiving:
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

}