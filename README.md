# WiFiCall
Implements the voice call function between devices that in the same wlan through UDP communication  
通过UDP通信实现同一局域网设备之间语音通话功能  

## 使用方法 
1.两台设备连接同一个WiFi，或者其中一台设备开启热点，另外一台设备连接这个热点  
2.当两台设备连接网络且在同一个局域网后。打开应用，按提示授予权限   
3.输入IP地址，端口号，点击设置按钮。完成设置之后即可拨打电话  

## 使用演示
1.配置界面：  
<img src="./testUI.png" width="360" height="640"/>  

2.ip地址和端口示例:  
手机1的ip地址：192.168.2.229， 手机2的ip地址：192.168.2.186    
手机1的配置信息填写：  
192.168.2.186  
40001  
40000  
  
手机2的配置信息填写：  
192.168.2.229  
40000  
40001  
  
3.点击设置，完成设置之后，点拨号就可以实现语音通话了  