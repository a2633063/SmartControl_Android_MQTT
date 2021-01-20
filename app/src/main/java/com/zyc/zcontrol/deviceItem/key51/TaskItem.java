package com.zyc.zcontrol.deviceItem.key51;

import android.content.Context;

import com.zyc.Function;


public class TaskItem {

    public TaskItem(Context context) {
        this.context = context;
    }

    private Context context;

    public String name;
    public int type;
    public int on = 0;    //开关
    public int key;

    public String topic;
    public String payload;
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
        return !(on==0);
    }

    public void setOn(int on) {
        this.on = on;
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
    public void setWol(int[] mac,int[] ip, int port) {
        setWol(mac, ip,  port,new int[]{0,0,0,0,0,0});
    }
    public void setWol(int[] mac,int[] ip, int port,int[] secure) {

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
}
