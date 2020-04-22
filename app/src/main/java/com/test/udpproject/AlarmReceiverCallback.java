package com.test.udpproject;

public interface AlarmReceiverCallback {
    void startSendMessageFromCallback(String serverIp, int serverPort, int packetSize);
}
