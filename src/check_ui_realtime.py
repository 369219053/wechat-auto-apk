"""
实时查看UI元素 - 使用weditor
"""

import uiautomator2 as u2
from loguru import logger
import sys

# 配置日志
logger.remove()
logger.add(sys.stdout, level="INFO", format="<green>{time:HH:mm:ss}</green> | <level>{level: <8}</level> | <level>{message}</level>")

def main():
    logger.info("=" * 60)
    logger.info("启动weditor UI调试工具")
    logger.info("=" * 60)
    logger.info("")
    
    logger.info("连接设备: 192.168.1.3:40557")
    d = u2.connect("192.168.1.3:40557")
    
    logger.info("当前应用: " + d.app_current().get('package', 'Unknown'))
    logger.info("")
    
    logger.info("=" * 60)
    logger.info("请在浏览器中打开 weditor")
    logger.info("命令: python3 -m weditor")
    logger.info("然后在weditor中连接设备: 192.168.1.3:40557")
    logger.info("=" * 60)
    logger.info("")
    
    # 获取所有可见的UI元素
    logger.info("正在分析UI元素...")
    logger.info("")
    
    # 尝试不同的方式查找元素
    logger.info("方法1: 查找所有TextView")
    textviews = d(className="android.widget.TextView")
    logger.info(f"找到 {textviews.count} 个TextView")
    
    if textviews.count > 0:
        logger.info("前10个TextView的文本:")
        for i in range(min(10, textviews.count)):
            try:
                info = textviews[i].info
                text = info.get('text', '')
                content_desc = info.get('contentDescription', '')
                resource_id = info.get('resourceId', '')
                
                if text or content_desc:
                    logger.info(f"  [{i+1}] text='{text}' desc='{content_desc}' id='{resource_id}'")
            except:
                pass
    
    logger.info("")
    logger.info("方法2: 查找所有Button")
    buttons = d(className="android.widget.Button")
    logger.info(f"找到 {buttons.count} 个Button")
    
    logger.info("")
    logger.info("方法3: 查找所有ImageView")
    imageviews = d(className="android.widget.ImageView")
    logger.info(f"找到 {imageviews.count} 个ImageView")
    
    logger.info("")
    logger.info("方法4: 查找所有LinearLayout")
    layouts = d(className="android.widget.LinearLayout")
    logger.info(f"找到 {layouts.count} 个LinearLayout")
    
    logger.info("")
    logger.info("方法5: 查找所有FrameLayout")
    framelayouts = d(className="android.widget.FrameLayout")
    logger.info(f"找到 {framelayouts.count} 个FrameLayout")
    
    logger.info("")
    logger.info("=" * 60)
    logger.info("分析完成!")
    logger.info("=" * 60)

if __name__ == "__main__":
    main()

