package com.jypLord.redis.streams;

import org.springframework.data.redis.connection.stream.MapRecord;

public record StreamEnvelope(StreamKey streamKey, MapRecord<String, String, String> record) {

}
