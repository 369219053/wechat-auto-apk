"""
微信控制器
负责微信APP的启动、登录检测、导航等基础操作
"""

import uiautomator2 as u2
from loguru import logger
import time
from typing import Optional


class WeChatController:
    """微信控制器类"""
    
    # 微信包名
    PACKAGE_NAME = "com.tencent.mm"
    
    # 微信主要Activity
    LAUNCHER_UI = ".ui.LauncherUI"
    
    def __init__(self, device_address: str = "192.168.1.3:41239"):
        """
        初始化微信控制器

        Args:
            device_address: 设备地址,格式为 IP:端口
        """
        self.device_address = device_address
        self.device: Optional[u2.Device] = None
        
    def connect(self) -> bool:
        """
        连接设备
        
        Returns:
            bool: 连接是否成功
        """
        try:
            logger.info(f"正在连接设备: {self.device_address}")
            self.device = u2.connect(self.device_address)
            logger.success("✅ 设备连接成功!")
            return True
        except Exception as e:
            logger.error(f"❌ 设备连接失败: {str(e)}")
            return False
    
    def start_wechat(self) -> bool:
        """
        启动微信APP

        Returns:
            bool: 启动是否成功
        """
        try:
            logger.info("正在启动微信...")

            # 先唤醒屏幕
            if not self.device.info.get('screenOn'):
                logger.info("屏幕未点亮,正在唤醒...")
                self.device.screen_on()
                time.sleep(1)

            # 解锁屏幕(上滑)
            logger.info("解锁屏幕...")
            self.device.swipe(360, 1400, 360, 400, 0.1)
            time.sleep(1)

            # 启动微信
            self.device.app_start(self.PACKAGE_NAME)

            # 等待APP启动(增加等待时间,等待闪屏页结束)
            logger.info("等待微信启动...")
            time.sleep(5)

            # 确保微信在前台
            self.device.app_start(self.PACKAGE_NAME)
            time.sleep(2)

            # 检查是否成功启动
            current_app = self.device.app_current()
            if current_app.get('package') == self.PACKAGE_NAME:
                logger.success("✅ 微信启动成功!")
                return True
            else:
                logger.warning(f"⚠️ 当前应用: {current_app.get('package')}, 不是微信")
                return False

        except Exception as e:
            logger.error(f"❌ 微信启动失败: {str(e)}")
            return False
    
    def check_login(self, timeout: int = 60) -> bool:
        """
        检测微信是否已登录

        Args:
            timeout: 等待登录的超时时间(秒)

        Returns:
            bool: 是否已登录
        """
        try:
            logger.info("检测微信登录状态...")

            # 等待界面加载
            time.sleep(2)

            # 检测微信主界面的特征元素
            # 方法1: 检测聊天列表(RecyclerView)
            if self.device(className="androidx.recyclerview.widget.RecyclerView").exists:
                logger.success("✅ 微信已登录! (检测到聊天列表)")
                return True

            # 方法2: 检测底部输入框区域
            if self.device(resourceId="com.tencent.mm:id/bkk").exists:
                logger.success("✅ 微信已登录! (检测到输入框)")
                return True

            # 方法3: 检测任何聊天记录文本
            textviews = self.device(className="android.widget.TextView")
            if textviews.count > 5:  # 如果有超过5个TextView,说明有聊天记录
                logger.success(f"✅ 微信已登录! (检测到{textviews.count}个文本元素)")
                return True

            # 如果未登录,提示用户手动登录
            logger.warning("⚠️ 微信未登录,请手动登录...")
            logger.info(f"等待登录,超时时间: {timeout}秒")

            # 等待登录完成(检测聊天列表出现)
            if self.device(className="androidx.recyclerview.widget.RecyclerView").wait(timeout=timeout):
                logger.success("✅ 登录成功!")
                return True
            else:
                logger.error("❌ 登录超时!")
                return False

        except Exception as e:
            logger.error(f"❌ 登录检测失败: {str(e)}")
            return False
    
    def is_on_home_page(self) -> bool:
        """
        检测是否在微信首页

        Returns:
            bool: 是否在首页
        """
        try:
            # 检测"微信"按钮是否是选中状态
            if self.device(text="微信").exists:
                info = self.device(text="微信").info
                if info.get('selected', False):
                    return True
            return False
        except:
            return False

    def go_to_home(self) -> bool:
        """
        返回微信首页(聊天列表)

        Returns:
            bool: 是否成功返回
        """
        try:
            logger.info("正在返回微信首页...")

            # 先检查当前是否在微信
            current_app = self.device.app_current()
            if current_app.get('package') != self.PACKAGE_NAME:
                logger.warning("⚠️ 当前不在微信,重新启动微信...")
                self.device.app_start(self.PACKAGE_NAME)
                time.sleep(3)

            # 检查是否已经在首页(检测"微信"按钮是否选中)
            if self.is_on_home_page():
                logger.success("✅ 已在微信首页!")
                return True

            # 点击底部导航栏的"微信"按钮回到首页
            if self.device(text="微信").exists:
                logger.info("点击首页按钮...")
                self.device(text="微信").click()
                time.sleep(2)

                # 检查是否回到首页
                if self.is_on_home_page():
                    logger.success("✅ 已返回微信首页!")
                    return True

            logger.error("❌ 未能返回微信首页")
            return False

        except Exception as e:
            logger.error(f"❌ 返回首页失败: {str(e)}")
            return False

    def open_search(self) -> bool:
        """
        打开搜索功能(点击右上角放大镜)

        Returns:
            bool: 是否成功打开搜索
        """
        try:
            logger.info("正在打开搜索...")

            # 方法1: 使用content-desc定位
            if self.device(description="搜索").exists:
                logger.info("使用content-desc点击搜索按钮...")
                self.device(description="搜索").click()
                time.sleep(2)
            # 方法2: 使用resourceId定位
            elif self.device(resourceId="com.tencent.mm:id/jha").exists:
                logger.info("使用resourceId点击搜索按钮...")
                self.device(resourceId="com.tencent.mm:id/jha").click()
                time.sleep(2)
            # 方法3: 使用坐标点击
            else:
                logger.info("使用坐标点击搜索按钮...")
                # 搜索按钮坐标: bounds="[548,76][634,162]"
                # 中心点: (591, 119)
                self.device.click(591, 119)
                time.sleep(2)

            # 验证是否打开搜索页面
            # 搜索页面通常有搜索框
            if self.device(className="android.widget.EditText").exists:
                logger.success("✅ 已打开搜索!")
                return True
            else:
                logger.warning("⚠️ 可能未成功打开搜索")
                return True  # 暂时返回True,继续执行

        except Exception as e:
            logger.error(f"❌ 打开搜索失败: {str(e)}")
            return False
    
    def search_friend(self, friend_name: str) -> bool:
        """
        搜索好友
        
        Args:
            friend_name: 好友名称或微信号
            
        Returns:
            bool: 是否找到好友
        """
        try:
            logger.info(f"正在搜索好友: {friend_name}")
            
            # TODO: 实现搜索逻辑
            # 1. 点击搜索按钮
            # 2. 输入好友名称
            # 3. 点击搜索结果
            
            logger.warning("⚠️ 搜索功能待实现")
            return False
            
        except Exception as e:
            logger.error(f"❌ 搜索好友失败: {str(e)}")
            return False

