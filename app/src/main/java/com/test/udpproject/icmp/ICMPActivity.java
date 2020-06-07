package com.test.udpproject.icmp;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
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

public class ICMPActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "ICMPActivity";
    private PingTask mTask;
    private EditText mCountEditText;
    private EditText mWaitEditText;

    private Button mButtonStart;
    private Button mButtonStop;

    private TextView mAverageDelayTextView;
    private TextView mMaxDelayTextView;
    private TextView mMinDelayTextView;
    private TextView mLossRatioTextView;
    private TextView mPacketsTransmittedTextView;
    private TextView mPacketsReceivedTextView;
    private TextView mElapsedTimeTextView;

    private int mCount;
    private int mWait;
    private long mElapsedTime;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_icmp);

        initializeViews();
        setListeners();
    }

    private void initializeViews() {
        mCountEditText = findViewById(R.id.countEditText);
        mWaitEditText = findViewById(R.id.waitEditText);
        mButtonStart = findViewById(R.id.start);
        mButtonStop = findViewById(R.id.stop);
        mAverageDelayTextView = findViewById(R.id.averageDelay);
        mMaxDelayTextView = findViewById(R.id.maxDelay);
        mMinDelayTextView = findViewById(R.id.minDelay);
        mLossRatioTextView = findViewById(R.id.lossRatio);
        mPacketsTransmittedTextView = findViewById(R.id.packetsTransmitted);
        mPacketsReceivedTextView = findViewById(R.id.packetsReceived);
        mElapsedTimeTextView = findViewById(R.id.elapsedTime);
    }

    private void setListeners() {
        mButtonStart.setOnClickListener(this);
        mButtonStop.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    public void onClick(View view) {
        Log.d(TAG, "onClick");
        if (view.getId() == R.id.start) {
            Log.d(TAG, "start");

            mButtonStart.setEnabled(false);
            mButtonStop.setEnabled(true);

            mElapsedTime = TimeUnit.MILLISECONDS.toSeconds(new Date().getTime());

            getEditTextParameters();
            mTask = new PingTask(new PingTaskCallback() {
                @Override
                public void update(int packetTransmitted, int packetReceived, long minDelay, long maxDelay, long sumDelay) {
                    updateUI(packetTransmitted, packetReceived, minDelay, maxDelay, sumDelay);
                }

            }, mCount, mWait);

            mTask.execute("8.8.8.8");
        } else if (view.getId() == R.id.stop) {
            Log.d(TAG, "stop");
            mButtonStart.setEnabled(true);
            mButtonStop.setEnabled(false);

            mTask.stop();
        }
    }


    private void getEditTextParameters() {
        mCount = Integer.valueOf(mCountEditText.getText().toString());
        mWait = Integer.valueOf(mWaitEditText.getText().toString());

        Log.d(TAG, "mCount = " + mCount + "  mWait = " + mWait);
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

    private void updateUI(final int packetTransmitted, final int packetReceived, final long minDelay, final long maxDelay, final long sumDelay) {
        new Thread() {
            public void run() {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        long averageDelay = 0;
                        if (packetReceived != 0) {
                            averageDelay = sumDelay / packetReceived;
                        }

                        double lossRatio = 0;
                        if (packetTransmitted != 0) {
                            lossRatio = ((double) (packetTransmitted - packetReceived) / (double) packetTransmitted) * 100;
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
                        packetsTransmittedStringBuilder.append(getString(R.string.packets_transmitted)).append(packetTransmitted);

                        StringBuilder packetsReceivedStringBuilder = new StringBuilder();
                        packetsReceivedStringBuilder.append(getString(R.string.packets_received)).append(packetReceived);

                        StringBuilder elapsedTimeStringBuilder = new StringBuilder();
                        elapsedTimeStringBuilder.append(getString(R.string.elapsed_time)).append(TimeUnit.MILLISECONDS.toSeconds(new Date().getTime()) - mElapsedTime).append(" sec");

                        mAverageDelayTextView.setText(averageDelayStringBuilder);
                        mMaxDelayTextView.setText(maxDelayStringBuilder);
                        mMinDelayTextView.setText(minDelayStringBuilder);
                        mLossRatioTextView.setText(lossRatioStringBuilder);
                        mPacketsTransmittedTextView.setText(packetsTransmittedStringBuilder);
                        mPacketsReceivedTextView.setText(packetsReceivedStringBuilder);
                        mElapsedTimeTextView.setText(elapsedTimeStringBuilder);

                        if (packetTransmitted == mCount) {
                            mButtonStop.callOnClick();
                        }
                    }
                });
            }
        }.start();
    }
}
