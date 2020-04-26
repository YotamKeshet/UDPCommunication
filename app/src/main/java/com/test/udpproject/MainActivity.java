package com.test.udpproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.test.udpproject.client.UDPClientSocketActivity;
import com.test.udpproject.icmp.ICMPActivity;
import com.test.udpproject.server.UDPServerSocketActivity;

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

    public void icmpClick(View view) {
        Intent intent = new Intent(this, ICMPActivity.class);
        startActivity(intent);
    }
}
