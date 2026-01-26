package com.jypLord.redis.streams;

public enum StreamKey {
    BROKER_SESSION_TERMINATED_STREAM("broker:session:terminated"),
    LOSSCUT_STREAM("ctrl:losscut");

    private final String key;

    StreamKey(String key) { this.key = key; }

    public String key() { return key; }

}