package com.test.udpproject.client;

class UpdateObject {
    private int packetsTransmitted;
    private int packetsReceived;
    private long maxDelay;
    private long minDelay;
    private long sumOfDelays;

    UpdateObject(int packetsTransmitted, int packetsReceived, long maxDelay, long minDelay, long sumOfDelays) {
        this.packetsTransmitted = packetsTransmitted;
        this.packetsReceived = packetsReceived;
        this.maxDelay = maxDelay;
        this.minDelay = minDelay;
        this.sumOfDelays = sumOfDelays;
    }


    int getPacketsTransmitted() {
        return packetsTransmitted;
    }

    int getPacketsReceived() {
        return packetsReceived;
    }

    long getMaxDelay() {
        return maxDelay;
    }

    long getMinDelay() {
        return minDelay;
    }

    long getSumOfDelays() {
        return sumOfDelays;
    }
}
