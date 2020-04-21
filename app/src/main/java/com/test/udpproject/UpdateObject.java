package com.test.udpproject;

public class UpdateObject {
    private int packetsTransmitted;
    private int packetsReceived;
    private long maxDelay;
    private long minDelay;
    private long sumOfDelays;

    public UpdateObject(int packetsTransmitted, int packetsReceived, long maxDelay, long minDelay, long sumOfDelays) {
        this.packetsTransmitted = packetsTransmitted;
        this.packetsReceived = packetsReceived;
        this.maxDelay = maxDelay;
        this.minDelay = minDelay;
        this.sumOfDelays = sumOfDelays;
    }


    public int getPacketsTransmitted() {
        return packetsTransmitted;
    }

    public int getPacketsReceived() {
        return packetsReceived;
    }

    public long getMaxDelay() {
        return maxDelay;
    }

    public long getMinDelay() {
        return minDelay;
    }

    public long getSumOfDelays() {
        return sumOfDelays;
    }
}
