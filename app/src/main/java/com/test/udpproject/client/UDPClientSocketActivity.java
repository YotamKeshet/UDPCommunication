package com.test.udpproject.client;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.test.udpproject.R;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class UDPClientSocketActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "UDPClientSocketActivity";
    private EditText mIPEditText;
    private EditText mPortEditText;
    private EditText mPacketSizeEditText;
    private EditText mDelayEditText;
    private EditText mTestDurationEditText;
    private EditText mJitterBufferEditText;
    private EditText mIgnoredPacketsEditText;
    private TextView mAverageDelayTextView;
    private TextView mMaxDelayTextView;
    private TextView mMinDelayTextView;
    private TextView mLossRatioTextView;
    private TextView mPacketsTransmittedTextView;
    private TextView mPacketsReceivedTextView;
    private TextView mElapsedTimeTextView;

    private Button mButtonStart;
    private Button mButtonStop;

    private int mServerPort;
    private String mServerIp;
    private int mPacketSize;
    private int mDelay;
    private int mJitterBuffer;
    private int mNumberOfPacketsToIgnored;

    private long mElapsedTime = 0;
    private long mTestDuration = 0;

    private ServiceConnection mServiceConnection;
    private ClientService mClientService;

    private boolean mUpdateUi = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_udpclient_socket);

        initializeViews();
        setListeners();
        mServiceConnection = setServiceConnection();

        Intent intent = new Intent(this, ClientService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
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
        mTestDurationEditText = findViewById(R.id.testDurationEditText);
        mJitterBufferEditText = findViewById(R.id.jitterBufferEditText);
        mIgnoredPacketsEditText = findViewById(R.id.ignoredPacketsEditText);
    }

    private ServiceConnection setServiceConnection(){
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                Log.d(TAG, "onServiceConnected");
                mClientService = ((ClientService.LocalBinder) iBinder).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {

            }
        };
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

        if(mTestDuration < (TimeUnit.MILLISECONDS.toSeconds(new Date().getTime()) - mElapsedTime)){
            mButtonStop.callOnClick();
            return;
        }

        final Handler timerHandler = new Handler();
        Runnable timerRunnable = new Runnable() {

            @Override
            public void run() {
                if(mUpdateUi){
                   UpdateObject updateObject = mClientService.getDataToUpdateUI();

                    long averageDelay = 0;
                    if(updateObject.getPacketsReceived() != 0){
                        averageDelay = updateObject.getSumOfDelays() / (updateObject.getPacketsReceived());
                    }

                    double lossRatio = 0;
                    if(updateObject.getPacketsTransmitted() != 0){
                        lossRatio = ((double)(updateObject.getPacketsTransmitted() - updateObject.getPacketsReceived()) / (double)updateObject.getPacketsTransmitted()) * 100;
                    }

                    StringBuilder averageDelayStringBuilder = new StringBuilder();
                    averageDelayStringBuilder.append(getString(R.string.average_delay)).append(averageDelay).append("ms");

                    StringBuilder maxDelayStringBuilder = new StringBuilder();
                    maxDelayStringBuilder.append(getString(R.string.max_delay)).append(updateObject.getMaxDelay()).append("ms");

                    StringBuilder minDelayStringBuilder = new StringBuilder();
                    minDelayStringBuilder.append(getString(R.string.min_delay)).append(updateObject.getMinDelay()).append("ms");

                    StringBuilder lossRatioStringBuilder = new StringBuilder();
                    lossRatioStringBuilder.append(getString(R.string.loss_ratio)).append(lossRatio).append("%");

                    StringBuilder packetsTransmittedStringBuilder = new StringBuilder();
                    packetsTransmittedStringBuilder.append(getString(R.string.packets_transmitted)).append(updateObject.getPacketsTransmitted());

                    StringBuilder packetsReceivedStringBuilder = new StringBuilder();
                    packetsReceivedStringBuilder.append(getString(R.string.packets_received)).append(updateObject.getPacketsReceived());

                    StringBuilder elapsedTimeStringBuilder = new StringBuilder();
                    elapsedTimeStringBuilder.append(getString(R.string.elapsed_time)).append(TimeUnit.MILLISECONDS.toSeconds(new Date().getTime()) - mElapsedTime).append(" sec");

                    mAverageDelayTextView.setText(averageDelayStringBuilder);
                    mMaxDelayTextView.setText(maxDelayStringBuilder);
                    mMinDelayTextView.setText(minDelayStringBuilder);
                    mLossRatioTextView.setText(lossRatioStringBuilder);
                    mPacketsTransmittedTextView.setText(packetsTransmittedStringBuilder);
                    mPacketsReceivedTextView.setText(packetsReceivedStringBuilder);
                    mElapsedTimeTextView.setText(elapsedTimeStringBuilder);

                    updateUI();
                }
            }
        };

        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void getEditTextParameters(){
        mServerIp = mIPEditText.getText().toString();
        mServerPort = Integer.valueOf(mPortEditText.getText().toString());
        mPacketSize = Integer.valueOf(mPacketSizeEditText.getText().toString());
        mDelay = Integer.valueOf(mDelayEditText.getText().toString());
        mTestDuration = Integer.valueOf(mTestDurationEditText.getText().toString());
        mJitterBuffer = Integer.valueOf(mJitterBufferEditText.getText().toString());
        mNumberOfPacketsToIgnored = Integer.valueOf(mIgnoredPacketsEditText.getText().toString());

        Log.d(TAG, "mServerIp = " + mServerIp + "  mServerPort = " + mServerPort + "  mPacketSize = " + mPacketSize + "  mDelay = " + mDelay + " mTestDuration = " + mTestDuration + " mJitterBuffer = " + mJitterBuffer + " mNumberOfPacketsToIgnored = " + mNumberOfPacketsToIgnored);
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick");
        if (v.getId() == R.id.start) {
            mButtonStart.setEnabled(false);
            mButtonStop.setEnabled(true);

            mClientService.resetTestParameters();
            mElapsedTime = 0;
            mTestDuration = 0;
            clearViews();
            mElapsedTime = TimeUnit.MILLISECONDS.toSeconds(new Date().getTime());

            getEditTextParameters();
            mClientService.startReceiveMessages(mPacketSize, mDelay, mJitterBuffer, mNumberOfPacketsToIgnored);
            mClientService.startSendMessage(mServerIp, mServerPort, mPacketSize, mDelay, mNumberOfPacketsToIgnored);
            updateUI();
            mUpdateUi = true;
        }
        else if(v.getId() == R.id.stop){
            new Thread() {
                public void run() {
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                mButtonStart.setEnabled(true);
                                mButtonStop.setEnabled(false);
                                mClientService.stopTest();
                                mUpdateUi = false;
                            }
                        });
                }
            }.start();
        }
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

}
