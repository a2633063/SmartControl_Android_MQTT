package com.zyc.zcontrol.deviceItem.z863key;

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


    public final static int TASK_TYPE_RELAY = 0;
    public final static int TASK_TYPE_MQTT = 1;
    public final static int TASK_TYPE_MAX = 2;

    public final static int TASK_BUTTON_TYPE_BUTTON = 0;
    public final static int TASK_BUTTON_TYPE_TOGGLE = 1;


    private Context context;

    public String name;
    public int button_type;
    public int type;
    public int[] color = new int[2];

    //region 自动化类定义
    class MQTT_C {
        public String topic = "";
        public String payload = "";
        public int qos;
        public int retained;
    }

    public class RELAY_C {
        //public int dat_length;                      //满足条件时,是否将接收到的数据发至mqtt
        public int index;   //将接收到的数据中第reserved_rec个值
        public int on;  //填入要发送的第reserved_send个字段
    }

    public class DATA_C {
        //public int dat_length;                      //满足条件时,是否将接收到的数据发至mqtt
        public MQTT_C mqtt= new MQTT_C();
        public RELAY_C relay= new RELAY_C();
    }
    //endregion

//    public MQTT_C mqtt = new MQTT_C();
//    public RELAY_C relay = new RELAY_C();

    public DATA_C[] data =
            {
                    new DATA_C(),
                    new DATA_C(),
            };

    public void setBase(String name, int button_type, int type) {
        this.name = name;
        this.button_type = button_type;
        this.type = type;
    }

    public void setColor(int color0,int color1) {
        this.color[0] = color0;
        this.color[1] = color1;
    }
    //region 设置自动化任务

    //region mqtt任务部分
    public void setMqtt(int task_index, String topic, String payload, int qos, int retained) {
        this.data[task_index].mqtt.topic = topic;
        this.data[task_index].mqtt.payload = payload;
        this.data[task_index].mqtt.qos = qos;
        this.data[task_index].mqtt.retained = retained;
    }
    //endregion

    //region relay
    public void setRelay(int task_index, int index, int on) {
        this.data[task_index].relay.index = index;
        this.data[task_index].relay.on = on;
    }
    //endregion
    //endregion

    public JSONObject getJson() {
        JSONObject jsonRoot = new JSONObject();
        try {
            JSONObject jsonObject ;
            JSONArray jsonArray ;
            jsonRoot.put("name", name);
            jsonRoot.put("type", type);
            jsonRoot.put("button_type", button_type);
            jsonArray = new JSONArray();
            jsonArray.put(color[0]);
            jsonArray.put(color[1]);
            jsonRoot.put("color", jsonArray);
            //region 执行部分
            switch (type) {
                case TASK_TYPE_MQTT: {
                    jsonObject = new JSONObject();
                    jsonArray = new JSONArray();
                    jsonObject.put("topic", this.data[0].mqtt.topic);
                    jsonObject.put("payload", this.data[0].mqtt.payload);
                    jsonObject.put("qos", this.data[0].mqtt.qos);
                    jsonObject.put("retained", this.data[0].mqtt.retained);
                    jsonArray.put(jsonObject);
                    if (button_type == TASK_BUTTON_TYPE_TOGGLE) {
                        jsonObject = new JSONObject();
                        jsonObject.put("topic", this.data[1].mqtt.topic);
                        jsonObject.put("payload", this.data[1].mqtt.payload);
                        jsonObject.put("qos", this.data[1].mqtt.qos);
                        jsonObject.put("retained", this.data[1].mqtt.retained);
                        jsonArray.put(jsonObject);
                    }
                    jsonRoot.put("mqtt", jsonArray);
                    break;
                }

                case TASK_TYPE_RELAY: {
                    jsonObject = new JSONObject();
                    jsonArray = new JSONArray();
                    jsonObject.put("index", this.data[0].relay.index);
                    jsonObject.put("on", this.data[0].relay.on);
                    jsonArray.put(jsonObject);
                    if (button_type == TASK_BUTTON_TYPE_TOGGLE) {
                        jsonObject = new JSONObject();
                        jsonObject.put("index", this.data[1].relay.index);
                        jsonObject.put("on", this.data[1].relay.on);
                        jsonArray.put(jsonObject);
                    }
                    jsonRoot.put("relay", jsonArray);
                    break;
                }
            }
            //endregion

            Log.d("z86_3key task json", jsonRoot.toString());
            return jsonRoot;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }


    public TaskItem clone() {
        TaskItem newTask = new TaskItem();
        newTask.setBase(this.name, this.button_type, this.type);
        newTask.setMqtt(0,this.data[0].mqtt.topic, this.data[0].mqtt.payload, this.data[0].mqtt.qos, this.data[0].mqtt.retained);
        newTask.setMqtt(1,this.data[1].mqtt.topic, this.data[1].mqtt.payload, this.data[1].mqtt.qos, this.data[1].mqtt.retained);
        newTask.setRelay(0,this.data[0].relay.index, this.data[0].relay.on);
        newTask.setRelay(1,this.data[1].relay.index, this.data[1].relay.on);
        return newTask;
    }

    public void Copy(TaskItem oldTask) {
        if (oldTask == null) return;
        this.setBase(oldTask.name, this.button_type, oldTask.type);
        this.setMqtt(0,oldTask.data[0].mqtt.topic, oldTask.data[0].mqtt.payload, oldTask.data[0].mqtt.qos, oldTask.data[0].mqtt.retained);
        this.setMqtt(1,oldTask.data[1].mqtt.topic, oldTask.data[1].mqtt.payload, oldTask.data[1].mqtt.qos, oldTask.data[1].mqtt.retained);
        this.setRelay(0,oldTask.data[0].relay.index, oldTask.data[0].relay.on);
        this.setRelay(1,oldTask.data[1].relay.index, oldTask.data[1].relay.on);
    }

}
