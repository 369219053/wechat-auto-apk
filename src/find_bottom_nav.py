"""
查找底部导航栏元素
"""

import uiautomator2 as u2
from loguru import logger
import sys

# 配置日志
logger.remove()
logger.add(sys.stdout, level="INFO", format="<green>{time:HH:mm:ss}</green> | <level>{level: <8}</level> | <level>{message}</level>")

def main():
    logger.info("=" * 60)
    logger.info("查找底部导航栏元素")
    logger.info("=" * 60)
    logger.info("")
    
    # 连接设备
    device_address = "192.168.1.3:41239"
    logger.info(f"连接设备: {device_address}")
    d = u2.connect(device_address)
    
    # 启动微信
    logger.info("启动微信...")
    d.app_start("com.tencent.mm")
    import time
    time.sleep(5)
    
    # 获取屏幕尺寸
    width, height = d.window_size()
    logger.info(f"屏幕尺寸: {width}x{height}")
    logger.info("")
    
    # 底部导航栏通常在屏幕底部100像素内
    bottom_y = height - 100
    logger.info(f"底部区域: y > {bottom_y}")
    logger.info("")
    
    # 查找底部区域的所有可点击元素
    logger.info("查找底部区域的可点击元素...")
    logger.info("-" * 60)
    
    # 方法1: 查找所有可点击的元素
    clickables = d(clickable=True)
    logger.info(f"找到 {clickables.count} 个可点击元素")
    
    bottom_clickables = []
    for i in range(clickables.count):
        try:
            info = clickables[i].info
            bounds = info.get('bounds', {})
            top = bounds.get('top', 0)
            bottom = bounds.get('bottom', 0)
            
            # 如果元素在底部区域
            if bottom > bottom_y:
                bottom_clickables.append({
                    'index': i,
                    'resourceId': info.get('resourceId', ''),
                    'className': info.get('className', ''),
                    'text': info.get('text', ''),
                    'contentDescription': info.get('contentDescription', ''),
                    'bounds': bounds
                })
        except:
            pass
    
    logger.info(f"底部区域找到 {len(bottom_clickables)} 个可点击元素:")
    for item in bottom_clickables:
        logger.info(f"  - resourceId: {item['resourceId']}")
        logger.info(f"    className: {item['className']}")
        logger.info(f"    text: {item['text']}")
        logger.info(f"    desc: {item['contentDescription']}")
        logger.info(f"    bounds: {item['bounds']}")
        logger.info("")
    
    logger.info("=" * 60)
    logger.info("尝试点击底部导航栏的第2个位置(通讯录)")
    logger.info("=" * 60)
    
    # 底部导航栏通常有4个按钮,平均分布
    # 第1个: 微信/聊天
    # 第2个: 通讯录
    # 第3个: 发现
    # 第4个: 我
    
    nav_y = height - 50  # 导航栏中心Y坐标
    button_width = width // 4
    
    positions = {
        '微信/聊天': (button_width // 2, nav_y),
        '通讯录': (button_width + button_width // 2, nav_y),
        '发现': (button_width * 2 + button_width // 2, nav_y),
        '我': (button_width * 3 + button_width // 2, nav_y)
    }
    
    logger.info("底部导航栏按钮坐标:")
    for name, (x, y) in positions.items():
        logger.info(f"  {name}: ({x}, {y})")
    
    logger.info("")
    logger.info("=" * 60)
    logger.info("测试完成!")
    logger.info("=" * 60)

if __name__ == "__main__":
    main()

