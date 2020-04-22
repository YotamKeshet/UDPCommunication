package com.test.udpproject;

public interface AlarmReceiverCallback {
    void sendMessage(String serverIp, int serverPort, int packetSize);
}
