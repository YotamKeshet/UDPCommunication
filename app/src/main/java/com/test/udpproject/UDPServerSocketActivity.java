package com.test.udpproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

public class UDPServerSocketActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "UDPServerSocketActivity";

    private int mServerPort;
    private int mPacketSize;

    private Button buttonStartReceiving;
    private Button buttonStopReceiving;
    private EditText mPortEditText;
    private EditText mPacketSizeEditText;

    private ServiceConnection mServiceConnection;
    private ServerService mServerService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_udpserver_socket);

        initializeViews();
        setListeners();
        mServiceConnection = setServiceConnection();

        Intent intent = new Intent(this, ServerService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
    }

    private ServiceConnection setServiceConnection(){
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                Log.d(TAG, "onServiceConnected");
                mServerService = ((ServerService.LocalBinder) iBinder).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {

            }
        };
    }

    private void initializeViews(){
        buttonStartReceiving =  findViewById(R.id.btn_start_receiving);
        buttonStopReceiving = findViewById(R.id.btn_stop_receiving);
        mPortEditText = findViewById(R.id.portEditText);
        mPacketSizeEditText = findViewById(R.id.packetSizeEditText);
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

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick");

        switch (v.getId()) {

            case R.id.btn_start_receiving:
                getEditTextParameters();
                mServerService.startServerSocket(mPacketSize, mServerPort);
                buttonStartReceiving.setEnabled(false);
                buttonStopReceiving.setEnabled(true);
                break;

            case R.id.btn_stop_receiving:
                mServerService.stopServerSocket();
                buttonStartReceiving.setEnabled(true);
                buttonStopReceiving.setEnabled(false);
                break;
        }
    }

    private void getEditTextParameters(){
        mServerPort = Integer.valueOf(mPortEditText.getText().toString());
        mPacketSize = Integer.valueOf(mPacketSizeEditText.getText().toString());

        Log.d(TAG, "mServerPort = " + mServerPort + "  mPacketSize = " + mPacketSize);
    }



}