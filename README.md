# SmartControl_Android_MQTT

点个Star吧~


如果不会用github下载或是github下载慢的,可以在到酷安下载:https://www.coolapk.com/apk/com.zyc.zcontrol


本文档还在编写中!!!



**被控设备:**

1. [按键伴侣ButtonMate](https://github.com/a2633063/SmartControl_ButtonMate_ESP8266)	直接控制墙壁开关,在不修改墙壁开关的前提下实现智能开关的效果
2. [zTC1_a1](https://github.com/a2633063/zTC1)	      斐讯排插TC1重新开发固件,仅支持a1版本.
3. [zDC1](https://github.com/a2633063/zDC1_public)		       斐讯排插DC1重新开发固件.
4. [zA1](https://github.com/a2633063/zA1)		          斐讯空气净化器悟净A1重新开发固件.
5. [zM1](https://github.com/a2633063/zM1)		         斐讯空气检测仪悟空M1重新开发固件.
6. RGB灯             开发中
7. wifi校时时钟   开发中



## 使用说明



> 此app于设备通信通过udp广播或mqtt服务器通信.udp广播为在整个局域网(255.255.255.255)的10181和10182端口通信.由于udp广播的特性,udp局域网通信不稳定,建议有条件的还是使用mqtt服务器来通信.



### app设置

在侧边栏点击设置,进入设置页面.可设置mqtt服务器.(此处*总是通过UDP连接*选项无效!)



### 设备控制页面

(每总设备页面不同)

> 界面下方的*服务器已连接*、*服务器已断开* 是指app与mqtt服务器连接状态显示.与设备连接状态无关.

右上角,云图标为与设备同步mqtt服务器配置.由于可以自定义mqtt服务器,所以除了需要将手机连入mqtt服务器外,还需要将被控设备连入mqtt服务器.点击云图标按钮即可将mqtt的服务器信息同步给设备,让设备也连入mqtt服务器

右上角,笔形图标为设备设置页面,每个设备的设置都为独立配置.
