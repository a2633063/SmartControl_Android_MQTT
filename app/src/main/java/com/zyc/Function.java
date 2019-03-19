package com.zyc;

public class Function {

    public static String getWeek(int repeat){
        String str = "";
        String Week[] = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        repeat &= 0x7f;
        if (repeat == 0) str = "一次";
        else if ((repeat & 0x7f) == 0x7f) str = "每天";
        else {
            for (int i = 0; i < 7; i++) {
                if ((repeat & (1 << i)) != 0) {
                    str = str + "," + Week[i];
                }
            }
            str = str.replaceFirst(",", "");
        }

        return str;
    }
}
