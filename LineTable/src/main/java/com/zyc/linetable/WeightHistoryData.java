package com.zyc.linetable;

import java.text.SimpleDateFormat;
import java.util.Date;

public class WeightHistoryData {
    int weight;
    long utc;

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }


    public long getUtc() {
        return utc;
    }

    public void setUtc(long utc) {
        this.utc = utc;
    }


    public WeightHistoryData(int weight, long utc) {
        this.weight = weight;
        this.utc = utc;
    }
    public String getTimeString() {
        return getTimeString("yyyy/MM/dd  HH:mm ");
    }
    public String getTimeString(String format) {
        if (utc < 1) {
            return "未知时间";
        }
        Date date = new Date(utc * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }
}
