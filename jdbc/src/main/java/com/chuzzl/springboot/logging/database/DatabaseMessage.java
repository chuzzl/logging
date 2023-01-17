package com.chuzzl.springboot.logging.database;

import com.chuzzl.springboot.logging.api.Message;

import java.util.HashMap;
import java.util.Map;

public class DatabaseMessage implements Message {
    private String body;
    private Map<String, String> metaMap = null;
    private Message.Type type;

    public DatabaseMessage(String body) {
        this.body = body;
        this.type = Type.OUTBOUND;
    }

    public DatabaseMessage(String body, int rows) {
        this.body = body;
        metaMap = new HashMap<>();
        metaMap.put("Number rows", String.valueOf(rows));
        this.type = Type.INBOUND;
    }


    public Type type() {
        return type;
    }

    public String label() {
        return null;
    }

    public Map<String, String> meta() {
        return metaMap;
    }

    public Map<String, String> properties() {
        return null;
    }

    public String body() {
        return body;
    }
}
