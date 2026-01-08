"""
检测底部导航栏的选中状态
"""

import uiautomator2 as u2
from loguru import logger
import sys

# 配置日志
logger.remove()
logger.add(sys.stdout, level="INFO", format="<green>{time:HH:mm:ss}</green> | <level>{level: <8}</level> | <level>{message}</level>")

def main():
    logger.info("=" * 60)
    logger.info("检测底部导航栏选中状态")
    logger.info("=" * 60)
    logger.info("")
    
    # 连接设备
    device_address = "192.168.1.3:41239"
    logger.info(f"连接设备: {device_address}")
    d = u2.connect(device_address)
    
    # 查找底部导航栏的所有TextView
    logger.info("查找底部导航栏的文本元素...")
    logger.info("-" * 60)
    
    # 底部导航栏的4个按钮
    tabs = ["微信", "通讯录", "发现", "我"]
    
    for tab in tabs:
        if d(text=tab).exists:
            info = d(text=tab).info
            logger.info(f"\n【{tab}】")
            logger.info(f"  text: {info.get('text', '')}")
            logger.info(f"  resourceId: {info.get('resourceId', '')}")
            logger.info(f"  className: {info.get('className', '')}")
            logger.info(f"  selected: {info.get('selected', False)}")
            logger.info(f"  enabled: {info.get('enabled', True)}")
            logger.info(f"  focused: {info.get('focused', False)}")
            logger.info(f"  bounds: {info.get('bounds', {})}")
    
    logger.info("")
    logger.info("=" * 60)
    logger.info("检测完成!")
    logger.info("=" * 60)

if __name__ == "__main__":
    main()

