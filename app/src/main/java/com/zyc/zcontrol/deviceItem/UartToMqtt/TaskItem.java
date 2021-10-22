package com.zyc.zcontrol.deviceItem.UartToMqtt;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class TaskItem {

    public TaskItem() {
    }

    public TaskItem(Context context) {
        this.context = context;
    }

    private Context context;

    public String name;
    public int type;
    public int on = 0;    //开关
    public int key;

    public String topic="";
    public String payload="";
    public int qos;
    public int retained;
    public int udp;
    public int max;
    public int min;
    public int step;
    public int val;

    public int[] mac = new int[6];
    public int[] ip = new int[4];
    public int port;
    public int[] secure = new int[6];


    public boolean getOn() {
        return !(on == 0);
    }

    public void setOn(int on) {
        this.on = on;
    }

    public void setBase(String name, int on, int key, int type) {
        this.name = name;
        this.type = type;
        this.on = on;
        this.key = key;
    }

    public void setMqtt(String topic, String payload, int qos, int retained, int udp, int[] ip, int port) {
        this.topic = topic;
        this.payload = payload;
        this.qos = qos;
        this.retained = retained;
        this.udp = udp;
        this.port = port;
        this.ip[0] = ip[0];
        this.ip[1] = ip[1];
        this.ip[2] = ip[2];
        this.ip[3] = ip[3];
    }

    public void setWol(int[] mac, int[] ip, int port) {
        setWol(mac, ip, port, new int[]{0, 0, 0, 0, 0, 0});
    }

    public void setWol(int[] mac, int[] ip, int port, int[] secure) {

        this.port = port;
        for (int i = 0; i < 4; i++) {
            this.ip[i] = ip[i];
        }
        for (int i = 0; i < 6; i++) {
            this.mac[i] = mac[i];
            this.secure[i] = secure[i];
        }
    }

    public void setEncoder(String topic, String payload, int qos, int retained,
                           int udp, int[] ip, int port,
                           int max, int min, int step, int val) {
        this.topic = topic;
        this.payload = payload;
        this.qos = qos;
        this.retained = retained;
        this.udp = udp;
        this.port = port;
        this.ip[0] = ip[0];
        this.ip[1] = ip[1];
        this.ip[2] = ip[2];
        this.ip[3] = ip[3];
        this.max = max;
        this.min = min;
        this.step = step;
        this.val = val;
    }

    public String getMacString() {
        return String.format("%02x",mac[0]) + ":"
                + String.format("%02x",mac[1]) + ":"
                + String.format("%02x",mac[2]) + ":"
                + String.format("%02x",mac[3]) + ":"
                + String.format("%02x",mac[4]) + ":"
                + String.format("%02x",mac[5]);
//        return Integer.toHexString(mac[0]) + ":"
//                + Integer.toHexString(mac[1]) + ":"
//                + Integer.toHexString(mac[2]) + ":"
//                + Integer.toHexString(mac[3]) + ":"
//                + Integer.toHexString(mac[4]) + ":"
//                + Integer.toHexString(mac[5]);

    }

    public String getIPString() {
        return ip[0] + "." + ip[1] + "." + ip[2] + "." + ip[3];
    }

    public String getSecureString() {
        if (secure[0] == 0 && secure[1] == 0 && secure[2] == 0 && secure[3] == 0 && secure[4] == 0 && secure[5] == 0)
            return "";
        return Integer.toHexString(secure[0]) + ":"
                + Integer.toHexString(secure[1]) + ":"
                + Integer.toHexString(secure[2]) + ":"
                + Integer.toHexString(secure[3]) + ":"
                + Integer.toHexString(secure[4]) + ":"
                + Integer.toHexString(secure[5]);
    }

    public String getJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", name);
            jsonObject.put("type", type);
            jsonObject.put("on", on);
            jsonObject.put("key", key);

            JSONArray jsonArray_mac = new JSONArray();
            for (int i = 0; i < mac.length; i++)
                jsonArray_mac.put(mac[i]);
            JSONArray jsonArray_ip = new JSONArray();
            for (int i = 0; i < ip.length; i++)
                jsonArray_ip.put(ip[i]);

            JSONArray jsonArray_secure = new JSONArray();
            for (int i = 0; i < secure.length; i++)
                jsonArray_secure.put(secure[i]);
            switch (type) {
                case 0:
                    jsonObject.put("topic", topic);
                    jsonObject.put("payload", payload);
                    jsonObject.put("qos", qos);
                    jsonObject.put("retained", retained);
                    jsonObject.put("udp", udp);
                    jsonObject.put("ip", jsonArray_ip);
                    jsonObject.put("port", port);
                    break;
                case 1:
                    jsonObject.put("mac", jsonArray_mac);
                    jsonObject.put("ip", jsonArray_ip);
                    jsonObject.put("port", port);
                    jsonObject.put("secure", jsonArray_secure);
                    break;
                case 2:
                    jsonObject.put("topic", topic);
                    jsonObject.put("payload", payload);
                    jsonObject.put("qos", qos);
                    jsonObject.put("retained", retained);
                    jsonObject.put("udp", udp);
                    jsonObject.put("ip", jsonArray_ip);
                    jsonObject.put("port", port);
                    jsonObject.put("max", max);
                    jsonObject.put("min", min);
                    jsonObject.put("step", step);
                    jsonObject.put("val", val);
                    break;
            }

            Log.d("tag", jsonObject.toString());
            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

}
