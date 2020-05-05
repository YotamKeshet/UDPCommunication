package com.test.udpproject.client;

public interface AlarmReceiverCallback {
    void startSendMessageFromCallback(String serverIp, int serverPort, int packetSize, int packetToIgnored);
}
