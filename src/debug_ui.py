"""
UI调试工具
用于查看当前界面的元素和截图
"""

import uiautomator2 as u2
from loguru import logger
import sys
import json

# 配置日志
logger.remove()
logger.add(sys.stdout, level="INFO", format="<green>{time:HH:mm:ss}</green> | <level>{level: <8}</level> | <level>{message}</level>")


def debug_current_ui():
    """调试当前UI界面"""
    try:
        # 连接设备
        device_address = "192.168.1.3:41239"
        logger.info(f"连接设备: {device_address}")
        d = u2.connect(device_address)
        
        # 获取当前应用信息
        current_app = d.app_current()
        logger.info(f"当前应用: {current_app.get('package')}")
        logger.info(f"当前Activity: {current_app.get('activity')}")
        
        # 截图
        screenshot_path = "../data/screenshot_debug.png"
        d.screenshot(screenshot_path)
        logger.success(f"✅ 截图已保存: {screenshot_path}")
        
        # 获取UI层级结构
        logger.info("\n正在获取UI层级结构...")
        xml = d.dump_hierarchy()
        
        # 保存XML
        xml_path = "../data/ui_hierarchy.xml"
        with open(xml_path, 'w', encoding='utf-8') as f:
            f.write(xml)
        logger.success(f"✅ UI层级已保存: {xml_path}")
        
        # 查找所有文本元素
        logger.info("\n查找界面上的所有文本元素:")
        logger.info("-" * 60)
        
        # 使用xpath查找所有包含text属性的元素
        import xml.etree.ElementTree as ET
        root = ET.fromstring(xml)
        
        texts = []
        for elem in root.iter():
            text = elem.get('text')
            if text and text.strip():
                texts.append(text)
        
        # 去重并显示
        unique_texts = list(set(texts))
        for i, text in enumerate(unique_texts[:30], 1):  # 只显示前30个
            logger.info(f"  {i}. {text}")
        
        if len(unique_texts) > 30:
            logger.info(f"  ... 还有 {len(unique_texts) - 30} 个文本元素")
        
        logger.info("-" * 60)
        logger.info(f"总共找到 {len(unique_texts)} 个不同的文本元素")
        
        # 查找常见的微信元素
        logger.info("\n检测微信常见元素:")
        logger.info("-" * 60)
        
        wechat_elements = [
            "微信",
            "通讯录", 
            "发现",
            "我",
            "聊天",
            "消息",
            "WeChat",
            "Chats",
            "Contacts",
            "Discover",
            "Me"
        ]
        
        for elem_text in wechat_elements:
            exists = d(text=elem_text).exists
            status = "✅ 存在" if exists else "❌ 不存在"
            logger.info(f"  {elem_text}: {status}")
        
        logger.info("-" * 60)
        
        # 提示
        logger.info("\n💡 提示:")
        logger.info("  1. 查看截图: data/screenshot_debug.png")
        logger.info("  2. 查看UI层级: data/ui_hierarchy.xml")
        logger.info("  3. 根据上面的文本元素,可以判断当前界面状态")
        
    except Exception as e:
        logger.exception(f"调试失败: {str(e)}")


if __name__ == "__main__":
    logger.info("=" * 60)
    logger.info("UI调试工具")
    logger.info("=" * 60)
    logger.info("")
    
    debug_current_ui()

