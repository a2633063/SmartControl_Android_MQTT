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


    public final static int TASK_TYPE_MQTT = 0;
    public final static int TASK_TYPE_WOL = 1;
    public final static int TASK_TYPE_UART = 2;
    public final static int TASK_TYPE_HTTP = 3;
    public final static int TASK_TYPE_TIME_MQTT = 4;
    public final static int TASK_TYPE_TIME_UART = 5;
    public final static int TASK_TYPE_MAX = 6;


    private Context context;

    public String name;
    public int type;
    public int on = 0;    //开关(

    //触发
    //串口接收触发
    public String condition_dat;
    public int reserved;
    public int mqtt_send;

    //定时触发
    public int hour;
    public int minute;
    public int repeat;


    //region 自动化类定义
    class MQTT_C {
        public String topic = "";
        public String payload = "";
        public int qos;
        public int retained;
        public int udp;
        public int reserved = 0;
        public int[] ip = new int[4];
        public int port;
    }

    public class WOL_C {
        public int[] mac = new int[6];
        public int[] ip = new int[4];
        public int port;
        public int[] secure = new int[6];
    }

    public class UART_C {
        //public int dat_length;                      //满足条件时,是否将接收到的数据发至mqtt
        public int reserved_rec;   //将接收到的数据中第reserved_rec个值
        public int reserved_send;  //填入要发送的第reserved_send个字段
        String dat;  //要发送是数据
    }

    /*class HTTP_C {   //http
        public String dat;
    }*/
    //endregion

    public MQTT_C mqtt = new MQTT_C();
    public WOL_C wol = new WOL_C();
    public UART_C uart = new UART_C();


    public boolean getOn() {
        return !(on == 0);
    }

    public void setOn(int on) {
        if (on != 0) on = 1;
        this.on = on;
    }

    public void setBase(String name, int on, int type) {
        this.name = name;
        this.type = type;
        this.on = on;
    }


    //region 设置触发部分
    //region 设置串口触发
    public void setTriggerUart(String dat) {
        setTriggerUart(dat, 0);
    }

    public void setTriggerUart(String dat, int reserved) {
        setTriggerUart(dat, reserved, 0);
    }

    public void setTriggerUart(String dat, int reserved, int mqtt_send) {
        this.condition_dat = dat;
        this.reserved = reserved;
        this.mqtt_send = mqtt_send;
    }

    //endregion
    //region 设置定时触发
    public void setTriggerTime(int hour, int minute) {
        setTriggerTime(hour, minute, 255);
    }

    public void setTriggerTime(int hour, int minute, int repeat) {
        this.hour = hour;
        this.minute = minute;
        this.repeat = repeat;
    }
    //endregion
    //endregion

    //region 设置自动化任务

    //region mqtt任务部分
    public void setMqtt(String topic, String payload, int qos, int retained) {
        setMqtt(topic, payload, qos, retained, 0, null, 0);
    }

    public void setMqtt(String topic, String payload, int qos, int retained, int udp, int[] ip, int port) {
        setMqtt(topic, payload, qos, retained, 0, udp, ip, port);
    }

    public void setMqtt(String topic, String payload, int qos, int retained, int reserved, int udp, int[] ip, int port) {
        this.mqtt.topic = topic;
        this.mqtt.payload = payload;
        this.mqtt.qos = qos;
        this.mqtt.retained = retained;
        this.mqtt.reserved = reserved;
        this.mqtt.udp = udp;
        this.mqtt.port = port;
        if (ip != null) {
            this.mqtt.ip[0] = ip[0];
            this.mqtt.ip[1] = ip[1];
            this.mqtt.ip[2] = ip[2];
            this.mqtt.ip[3] = ip[3];
        } else {
            this.mqtt.ip[0] = 255;
            this.mqtt.ip[1] = 255;
            this.mqtt.ip[2] = 255;
            this.mqtt.ip[3] = 255;
        }
    }
    //endregion

    //region Wol局域网唤醒部分
    public void setWol(int[] mac, int[] ip, int port) {
        setWol(mac, ip, port, new int[]{0, 0, 0, 0, 0, 0});
    }

    public void setWol(int[] mac, int[] ip, int port, int[] secure) {
        this.wol.port = port;
        for (int i = 0; i < 4; i++) {
            this.wol.ip[i] = ip[i];
        }
        for (int i = 0; i < 6; i++) {
            this.wol.mac[i] = mac[i];
            this.wol.secure[i] = secure[i];
        }
    }
    //endregion

    //region uart部分
    public void setUart(String dat) {
        setUart(dat, 0, 0);
    }

    public void setUart(String dat, int reserved_rec, int reserved_send) {
        this.uart.dat = dat;
        this.uart.reserved_rec = reserved_rec;
        this.uart.reserved_send = reserved_send;

    }
    //endregion
    //endregion

    public JSONObject getJson() {
        JSONObject jsonRoot = new JSONObject();
        try {
            jsonRoot.put("name", name);
            jsonRoot.put("type", type);
            jsonRoot.put("on", on);

            //region 触发部分
            switch (type) {
                case TASK_TYPE_MQTT:
                case TASK_TYPE_WOL:
                case TASK_TYPE_UART:
                    //case TASK_TYPE_HTTP:
                    //串口触发
                    jsonRoot.put("uart_dat", this.condition_dat);
                    jsonRoot.put("reserved", this.reserved);
                    jsonRoot.put("mqtt_send", this.mqtt_send);
                    break;
                case TASK_TYPE_TIME_MQTT:
                case TASK_TYPE_TIME_UART:
                    //定时触发
                    jsonRoot.put("hour", this.hour);
                    jsonRoot.put("minute", this.minute);
                    jsonRoot.put("repeat", this.repeat);
                    break;
            }
            //endregion
            //region 执行部分
            JSONArray jsonArray;
            JSONObject jsonObject = new JSONObject();
            switch (type) {
                case TASK_TYPE_MQTT:
                case TASK_TYPE_TIME_MQTT: {
                    jsonArray = new JSONArray();
                    for (int i = 0; i < this.mqtt.ip.length; i++)
                        jsonArray.put(this.mqtt.ip[i]);

                    jsonObject.put("topic", this.mqtt.topic);
                    jsonObject.put("payload", this.mqtt.payload);
                    jsonObject.put("qos", this.mqtt.qos);
                    jsonObject.put("retained", this.mqtt.retained);
                    if (this.mqtt.reserved > 0)
                        jsonObject.put("reserved", this.mqtt.reserved);
                    jsonObject.put("udp", this.mqtt.udp);
                    jsonObject.put("ip", jsonArray);
                    jsonObject.put("port", this.mqtt.port);

                    jsonRoot.put("mqtt", jsonObject);
                    break;
                }
                case TASK_TYPE_WOL: {
                    jsonArray = new JSONArray();
                    for (int i = 0; i < this.wol.mac.length; i++)
                        jsonArray.put(this.wol.mac[i]);
                    jsonObject.put("mac", jsonArray);
                    jsonArray = new JSONArray();
                    for (int i = 0; i < this.wol.ip.length; i++)
                        jsonArray.put(this.wol.ip[i]);
                    jsonObject.put("ip", jsonArray);
                    if (this.wol.port == 0) this.wol.port = 9;
                    jsonObject.put("port", this.wol.port);
                    if (this.wol.secure[0] != 0 && this.wol.secure[1] != 0 && this.wol.secure[2] != 0
                            && this.wol.secure[3] != 0 && this.wol.secure[4] != 0 && this.wol.secure[5] != 0) {
                        jsonArray = new JSONArray();
                        for (int i = 0; i < this.wol.secure.length; i++)
                            jsonArray.put(this.wol.secure[i]);
                        jsonObject.put("secure", jsonArray);
                    }
                    jsonRoot.put("wol", jsonObject);
                    break;
                }
                case TASK_TYPE_UART:
                case TASK_TYPE_TIME_UART: {
                    jsonObject.put("uart", this.uart.dat);
                    if (this.uart.reserved_rec > 0 && this.uart.reserved_send > 0) {
                        jsonObject.put("reserved_rec", this.uart.reserved_rec);
                        jsonObject.put("reserved_send", this.uart.reserved_send);
                    }
                    jsonRoot.put("uart", jsonObject);
                    break;
                }
                //case TASK_TYPE_HTTP:
            }
            //endregion

            Log.d("uartToMqtt task json", jsonRoot.toString());
            return jsonRoot;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }


    public TaskItem clone() {
        TaskItem newTask = new TaskItem();
        newTask.setBase(this.name, this.on, this.type);
        newTask.setTriggerUart(this.condition_dat, this.reserved, this.mqtt_send);
        newTask.setTriggerTime(this.hour, this.minute, this.repeat);
        newTask.setMqtt(this.mqtt.topic, this.mqtt.payload, this.mqtt.qos, this.mqtt.retained, this.mqtt.reserved, this.mqtt.udp, this.mqtt.ip, this.mqtt.port);
        newTask.setWol(this.wol.mac, this.wol.ip, this.wol.port, this.wol.secure);
        newTask.setUart(this.uart.dat, this.uart.reserved_rec, this.uart.reserved_send);
        return newTask;
    }

    public void Copy(TaskItem oldTask) {
        if (oldTask == null) return;
        this.setBase(oldTask.name, oldTask.on, oldTask.type);
        this.setTriggerUart(oldTask.condition_dat, oldTask.reserved, oldTask.mqtt_send);
        this.setTriggerTime(oldTask.hour, oldTask.minute, oldTask.repeat);
        this.setMqtt(oldTask.mqtt.topic, oldTask.mqtt.payload, oldTask.mqtt.qos, oldTask.mqtt.retained, oldTask.mqtt.reserved, oldTask.mqtt.udp, oldTask.mqtt.ip, oldTask.mqtt.port);
        this.setWol(oldTask.wol.mac, oldTask.wol.ip, oldTask.wol.port, oldTask.wol.secure);
        this.setUart(oldTask.uart.dat, oldTask.uart.reserved_rec, oldTask.uart.reserved_send);
    }

}
