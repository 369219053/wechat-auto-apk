"""
检查uiautomator2权限和服务状态
"""

import uiautomator2 as u2
from loguru import logger
import sys

# 配置日志
logger.remove()
logger.add(sys.stdout, level="INFO", format="<green>{time:HH:mm:ss}</green> | <level>{level: <8}</level> | <level>{message}</level>")

def main():
    logger.info("=" * 60)
    logger.info("检查uiautomator2权限和服务状态")
    logger.info("=" * 60)
    logger.info("")
    
    logger.info("连接设备: 192.168.1.3:40557")
    d = u2.connect("192.168.1.3:40557")
    
    logger.info("设备信息:")
    info = d.info
    logger.info(f"  - 设备型号: {info.get('productName', 'Unknown')}")
    logger.info(f"  - Android版本: {info.get('version', 'Unknown')}")
    logger.info(f"  - SDK版本: {info.get('sdkInt', 'Unknown')}")
    logger.info("")
    
    logger.info("当前应用信息:")
    current = d.app_current()
    logger.info(f"  - 包名: {current.get('package', 'Unknown')}")
    logger.info(f"  - Activity: {current.get('activity', 'Unknown')}")
    logger.info("")
    
    logger.info("屏幕信息:")
    logger.info(f"  - 屏幕状态: {'开启' if d.info.get('screenOn') else '关闭'}")
    logger.info(f"  - 分辨率: {d.window_size()}")
    logger.info("")
    
    logger.info("=" * 60)
    logger.info("尝试点击屏幕中心,看是否有响应")
    logger.info("=" * 60)
    
    # 获取屏幕尺寸
    width, height = d.window_size()
    center_x = width // 2
    center_y = height // 2
    
    logger.info(f"点击坐标: ({center_x}, {center_y})")
    d.click(center_x, center_y)
    
    import time
    time.sleep(2)
    
    logger.info("点击完成,请查看手机屏幕是否有响应")
    logger.info("")
    
    logger.info("=" * 60)
    logger.info("尝试截图")
    logger.info("=" * 60)
    
    screenshot = d.screenshot()
    screenshot.save("../data/test_screenshot.png")
    logger.success("✅ 截图已保存: ../data/test_screenshot.png")
    logger.info(f"截图尺寸: {screenshot.size}")
    logger.info("")
    
    logger.info("=" * 60)
    logger.info("检查完成!")
    logger.info("=" * 60)
    logger.info("")
    logger.info("💡 如果截图是黑屏或空白,可能的原因:")
    logger.info("  1. 微信开启了防截屏保护")
    logger.info("  2. 需要在手机上开启'允许截屏'权限")
    logger.info("  3. 需要在开发者选项中开启'USB调试(安全设置)'")

if __name__ == "__main__":
    main()

