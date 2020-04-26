package com.test.udpproject.icmp;

public interface PingTaskCallback {
    void update(int packetTransmitted, int packetReceived, long minDelay, long maxDelay, long sumDelay);
}
