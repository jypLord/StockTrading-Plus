package com.jypLord.util;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;


public class JsonUtil {

    public final static ObjectMapper objectMapper = new ObjectMapper();


    static{
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public static <T> String toJson(T object) {
        try{
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e){
            throw new RuntimeException("JSON 직렬화 실패", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try{
            return objectMapper.readValue(json, clazz);
        }catch (JsonProcessingException e){
            throw new RuntimeException("JSON 역직렬화 실패", e);
        }
    }

    public static JsonNode toJsonNode(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 파싱 실패", e);
        }
    }

}
