"""
测试uiautomator2连接
用于验证手机连接是否正常
"""

import uiautomator2 as u2
from loguru import logger
import sys

# 配置日志
logger.remove()
logger.add(sys.stdout, level="INFO", format="<green>{time:HH:mm:ss}</green> | <level>{level: <8}</level> | <level>{message}</level>")

def test_connection():
    """测试连接手机"""
    try:
        # 连接信息 (使用ADB连接端口,不是配对端口)
        device_address = "192.168.1.3:40557"

        logger.info(f"正在连接设备: {device_address}")

        # 尝试连接
        d = u2.connect(device_address)
        
        # 获取设备信息
        device_info = d.info
        
        logger.success("✅ 设备连接成功!")
        logger.info(f"设备信息:")
        logger.info(f"  - 设备型号: {device_info.get('productName', 'Unknown')}")
        logger.info(f"  - Android版本: {device_info.get('version', 'Unknown')}")
        logger.info(f"  - 屏幕分辨率: {device_info.get('displayWidth', 0)}x{device_info.get('displayHeight', 0)}")
        logger.info(f"  - 设备品牌: {device_info.get('brand', 'Unknown')}")
        logger.info(f"  - 设备型号: {device_info.get('model', 'Unknown')}")
        
        # 测试基本操作
        logger.info("\n测试基本操作...")
        
        # 获取当前应用包名
        current_app = d.app_current()
        logger.info(f"  - 当前应用: {current_app.get('package', 'Unknown')}")
        
        # 获取屏幕状态
        screen_on = d.info.get('screenOn', False)
        logger.info(f"  - 屏幕状态: {'开启' if screen_on else '关闭'}")
        
        logger.success("\n✅ 所有测试通过! 设备连接正常,可以开始开发!")
        
        return d
        
    except Exception as e:
        logger.error(f"❌ 连接失败: {str(e)}")
        logger.error("\n可能的原因:")
        logger.error("  1. 设备IP地址或端口不正确")
        logger.error("  2. 设备未开启无线调试")
        logger.error("  3. 设备和电脑不在同一网络")
        logger.error("  4. uiautomator2服务未安装或未启动")
        logger.error("\n解决方法:")
        logger.error("  1. 检查设备IP和端口是否正确")
        logger.error("  2. 在设备上重新开启无线调试")
        logger.error("  3. 确保设备和电脑在同一WiFi网络")
        logger.error("  4. 运行: python -m uiautomator2 init")
        return None

if __name__ == "__main__":
    logger.info("=" * 60)
    logger.info("微信私域自动化APK - 设备连接测试")
    logger.info("=" * 60)
    logger.info("")
    
    device = test_connection()
    
    if device:
        logger.info("\n" + "=" * 60)
        logger.info("测试完成! 准备开始功能开发...")
        logger.info("=" * 60)

