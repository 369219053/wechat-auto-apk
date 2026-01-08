"""
微信私域自动化APK - 主入口
"""

from loguru import logger
import sys
from wechat_controller import WeChatController

# 配置日志
logger.remove()
logger.add(
    sys.stdout, 
    level="INFO", 
    format="<green>{time:HH:mm:ss}</green> | <level>{level: <8}</level> | <level>{message}</level>"
)
logger.add(
    "../logs/app.log",
    rotation="10 MB",
    retention="7 days",
    level="DEBUG",
    format="{time:YYYY-MM-DD HH:mm:ss} | {level: <8} | {message}"
)


def test_wechat_basic():
    """测试微信基础功能"""
    logger.info("=" * 60)
    logger.info("微信私域自动化APK - 功能测试")
    logger.info("=" * 60)
    logger.info("")
    
    # 创建微信控制器
    wechat = WeChatController()
    
    # 步骤1: 连接设备
    logger.info("【步骤1】连接设备")
    if not wechat.connect():
        logger.error("设备连接失败,退出程序")
        return False
    logger.info("")
    
    # 步骤2: 启动微信
    logger.info("【步骤2】启动微信")
    if not wechat.start_wechat():
        logger.error("微信启动失败,退出程序")
        return False
    logger.info("")
    
    # 步骤3: 检测登录状态
    logger.info("【步骤3】检测登录状态")
    if not wechat.check_login(timeout=60):
        logger.error("微信未登录,退出程序")
        return False
    logger.info("")
    
    # 步骤4: 返回首页并打开搜索
    logger.info("【步骤4】返回首页并打开搜索")
    if not wechat.go_to_home():
        logger.error("返回首页失败,退出程序")
        return False

    if not wechat.open_search():
        logger.error("打开搜索失败,退出程序")
        return False
    logger.info("")
    
    logger.success("✅ 所有基础功能测试通过!")
    return True


if __name__ == "__main__":
    try:
        success = test_wechat_basic()
        
        if success:
            logger.info("")
            logger.info("=" * 60)
            logger.info("测试完成! 准备开始朋友圈采集功能开发...")
            logger.info("=" * 60)
        else:
            logger.error("")
            logger.error("=" * 60)
            logger.error("测试失败! 请检查错误信息")
            logger.error("=" * 60)
            
    except KeyboardInterrupt:
        logger.warning("\n用户中断程序")
    except Exception as e:
        logger.exception(f"程序异常: {str(e)}")

