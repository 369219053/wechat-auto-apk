#!/bin/bash
# 自动配对ADB设备

DEVICE_IP="192.168.1.3"
PAIR_PORT="43149"
PAIRING_CODE="438648"

echo "正在配对设备..."
echo "设备地址: $DEVICE_IP:$PAIR_PORT"
echo "配对码: $PAIRING_CODE"
echo ""

# 使用expect自动输入配对码
expect << EOF
spawn adb pair $DEVICE_IP:$PAIR_PORT
expect "Enter pairing code:"
send "$PAIRING_CODE\r"
expect eof
EOF

echo ""
echo "配对完成! 现在尝试连接设备..."
echo ""

# 连接设备(使用ADB端口,通常是配对端口-1或另一个端口)
# 需要刀仔老板提供ADB连接端口
adb connect $DEVICE_IP:5555

echo ""
echo "检查连接状态..."
adb devices

