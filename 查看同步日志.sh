#!/bin/bash

# 查看微信私域自动化APK的同步日志
# 使用方法: ./查看同步日志.sh

echo "开始监听微信私域自动化APK的日志..."
echo "请在手机上点击'同步通讯录'按钮"
echo "按Ctrl+C停止监听"
echo "================================"

adb -s 192.168.1.3:37139 logcat -c  # 清空日志
adb -s 192.168.1.3:37139 logcat | grep -E "WeChatAccessibility|WeChatAuto"

