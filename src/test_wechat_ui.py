"""
测试微信UI元素读取
"""

import uiautomator2 as u2
from loguru import logger
import sys
import time

# 配置日志
logger.remove()
logger.add(sys.stdout, level="INFO", format="<green>{time:HH:mm:ss}</green> | <level>{level: <8}</level> | <level>{message}</level>")

def main():
    logger.info("=" * 60)
    logger.info("测试微信UI元素读取")
    logger.info("=" * 60)
    logger.info("")
    
    # 连接设备
    device_address = "192.168.1.3:41239"
    logger.info(f"连接设备: {device_address}")
    d = u2.connect(device_address)
    
    # 启动微信
    logger.info("启动微信...")
    d.app_start("com.tencent.mm")
    time.sleep(5)
    
    # 确保微信在前台
    d.app_start("com.tencent.mm")
    time.sleep(2)
    
    # 获取当前应用信息
    current_app = d.app_current()
    logger.info(f"当前应用: {current_app.get('package')}")
    logger.info(f"当前Activity: {current_app.get('activity')}")
    logger.info("")
    
    # 截图
    logger.info("截图...")
    screenshot = d.screenshot()
    screenshot.save("../data/wechat_ui_test.png")
    logger.success("✅ 截图已保存: ../data/wechat_ui_test.png")
    logger.info("")
    
    # 获取UI层级
    logger.info("获取UI层级...")
    xml = d.dump_hierarchy()
    with open("../data/wechat_ui_hierarchy.xml", "w", encoding="utf-8") as f:
        f.write(xml)
    logger.success("✅ UI层级已保存: ../data/wechat_ui_hierarchy.xml")
    logger.info("")
    
    # 查找所有文本元素
    logger.info("查找所有文本元素...")
    logger.info("-" * 60)
    
    # 方法1: 查找所有TextView
    textviews = d(className="android.widget.TextView")
    logger.info(f"找到 {textviews.count} 个TextView")
    
    if textviews.count > 0:
        logger.info("前20个TextView的文本:")
        for i in range(min(20, textviews.count)):
            try:
                info = textviews[i].info
                text = info.get('text', '')
                if text:
                    logger.info(f"  [{i+1}] {text}")
            except:
                pass
    
    logger.info("")
    
    # 方法2: 使用xpath查找所有有文本的元素
    logger.info("使用xpath查找所有有文本的元素...")
    try:
        elements = d.xpath('//*[@text]').all()
        logger.info(f"找到 {len(elements)} 个有文本的元素")
        
        if len(elements) > 0:
            logger.info("前20个元素的文本:")
            for i, elem in enumerate(elements[:20]):
                try:
                    text = elem.attrib.get('text', '')
                    if text:
                        logger.info(f"  [{i+1}] {text}")
                except:
                    pass
    except Exception as e:
        logger.error(f"xpath查找失败: {e}")
    
    logger.info("")
    logger.info("=" * 60)
    logger.info("测试完成!")
    logger.info("=" * 60)

if __name__ == "__main__":
    main()

