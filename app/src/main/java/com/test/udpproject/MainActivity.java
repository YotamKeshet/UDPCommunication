package com.test.udpproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void clientClick(View view) {
        Intent intent = new Intent(this, UDPClientSocketActivity.class);
        startActivity(intent);
    }

    public void serverClick(View view) {
        Intent intent = new Intent(this, UDPServerSocketActivity.class);
        startActivity(intent);
    }
}
