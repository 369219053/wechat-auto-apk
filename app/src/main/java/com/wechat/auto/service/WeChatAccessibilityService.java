package com.wechat.auto.service;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 微信无障碍服务
 * 核心功能:
 * 1. 监听微信UI事件
 * 2. 自动化操作微信
 * 3. 朋友圈数据采集
 * 4. AI智能私信
 */
public class WeChatAccessibilityService extends AccessibilityService {

    private static final String TAG = "WeChatAutoService";
    private static final String WECHAT_PACKAGE = "com.tencent.mm";
    
    private static WeChatAccessibilityService instance;
    private boolean isAutoTaskRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(TAG, "服务创建成功");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "无障碍服务已连接");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isAutoTaskRunning) {
            return;
        }

        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
        
        // 只处理微信的事件
        if (!WECHAT_PACKAGE.equals(packageName)) {
            return;
        }

        int eventType = event.getEventType();
        Log.d(TAG, "收到微信事件: " + AccessibilityEvent.eventTypeToString(eventType));

        // 处理不同类型的事件
        switch (eventType) {
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                handleWindowStateChanged(event);
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                handleWindowContentChanged(event);
                break;
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "服务中断");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        Log.d(TAG, "服务销毁");
    }

    /**
     * 获取服务实例
     */
    public static WeChatAccessibilityService getInstance() {
        return instance;
    }

    /**
     * 启动自动化任务
     */
    public void startAutoTask() {
        isAutoTaskRunning = true;
        Log.d(TAG, "自动化任务已启动");

        // 启动微信
        launchWeChat();

        // 延迟2秒后点击通讯录
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            clickContactsTab();
        }, 2000);
    }

    /**
     * 同步通讯录
     */
    public void syncContacts() {
        Log.d(TAG, "开始同步通讯录");

        // 启动微信
        launchWeChat();

        // 延迟2秒后点击通讯录
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            clickContactsTab();
        }, 2000);

        // 延迟4秒后读取通讯录
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            readContactsList();
        }, 4000);
    }

    /**
     * 启动微信应用
     */
    private void launchWeChat() {
        try {
            // 直接启动微信的LauncherUI
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(WECHAT_PACKAGE, "com.tencent.mm.ui.LauncherUI"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            startActivity(intent);
            Log.d(TAG, "正在启动微信...");

        } catch (Exception e) {
            Log.e(TAG, "启动微信失败: " + e.getMessage(), e);
        }
    }

    /**
     * 点击通讯录标签
     */
    private void clickContactsTab() {
        try {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) {
                Log.e(TAG, "无法获取根节点");
                return;
            }

            // 方法1: 通过文本"通讯录"查找
            List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText("通讯录");
            if (nodes != null && !nodes.isEmpty()) {
                for (AccessibilityNodeInfo node : nodes) {
                    // 查找可点击的父节点
                    AccessibilityNodeInfo clickableNode = findClickableParent(node);
                    if (clickableNode != null) {
                        boolean clicked = clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.d(TAG, "点击通讯录标签: " + (clicked ? "成功" : "失败"));
                        return;
                    }
                }
            }

            Log.w(TAG, "未找到通讯录标签");

        } catch (Exception e) {
            Log.e(TAG, "点击通讯录失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查找可点击的父节点
     */
    private AccessibilityNodeInfo findClickableParent(AccessibilityNodeInfo node) {
        if (node == null) {
            return null;
        }

        if (node.isClickable()) {
            return node;
        }

        AccessibilityNodeInfo parent = node.getParent();
        if (parent != null) {
            return findClickableParent(parent);
        }

        return null;
    }

    /**
     * 读取通讯录好友列表
     */
    private void readContactsList() {
        try {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) {
                Log.e(TAG, "无法获取根节点");
                return;
            }

            // 查找所有好友昵称节点 (resource-id: com.tencent.mm:id/kbq)
            List<AccessibilityNodeInfo> friendNodes = rootNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/kbq");

            if (friendNodes == null || friendNodes.isEmpty()) {
                Log.w(TAG, "未找到好友节点");
                return;
            }

            Log.d(TAG, "找到 " + friendNodes.size() + " 个好友节点");

            // 提取好友昵称
            List<String> friends = new ArrayList<>();
            for (AccessibilityNodeInfo node : friendNodes) {
                CharSequence text = node.getText();
                if (text != null && text.length() > 0) {
                    String nickname = text.toString();

                    // 过滤掉特殊项
                    if (!isSpecialItem(nickname)) {
                        friends.add(nickname);
                        Log.d(TAG, "好友: " + nickname);
                    }
                }
            }

            Log.d(TAG, "总共找到 " + friends.size() + " 个真实好友");

            // 发送广播通知MainActivity
            sendFriendsBroadcast(friends);

        } catch (Exception e) {
            Log.e(TAG, "读取通讯录失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送好友列表广播并保存到SharedPreferences
     */
    private void sendFriendsBroadcast(List<String> friends) {
        // 保存到SharedPreferences
        saveFriendsToPrefs(friends);

        // 发送广播
        Intent intent = new Intent("com.wechat.auto.FRIENDS_SYNCED");
        intent.putStringArrayListExtra("friends", new ArrayList<>(friends));
        intent.setPackage(getPackageName()); // 限制只发送给本应用
        sendBroadcast(intent);
        Log.d(TAG, "已发送好友列表广播并保存到SharedPreferences");
    }

    /**
     * 保存好友列表到SharedPreferences
     */
    private void saveFriendsToPrefs(List<String> friends) {
        SharedPreferences prefs = getSharedPreferences("WeChatAutoPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> friendsSet = new HashSet<>(friends);
        editor.putStringSet("friends_list", friendsSet);
        editor.putLong("sync_time", System.currentTimeMillis());
        editor.apply();

        Log.d(TAG, "好友列表已保存到SharedPreferences: " + friends.size() + " 位好友");
    }

    /**
     * 判断是否是特殊项(非真实好友)
     */
    private boolean isSpecialItem(String text) {
        String[] specialItems = {
            "新的朋友",
            "仅聊天的朋友",
            "群聊",
            "标签",
            "服务号",
            "我的企业及企业联系人",
            "企业微信联系人"
        };

        for (String item : specialItems) {
            if (item.equals(text)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 停止自动化任务
     */
    public void stopAutoTask() {
        isAutoTaskRunning = false;
        Log.d(TAG, "自动化任务已停止");
    }

    /**
     * 处理窗口状态变化
     */
    private void handleWindowStateChanged(AccessibilityEvent event) {
        String className = event.getClassName() != null ? event.getClassName().toString() : "";
        Log.d(TAG, "窗口变化: " + className);
    }

    /**
     * 处理窗口内容变化
     */
    private void handleWindowContentChanged(AccessibilityEvent event) {
        // 窗口内容变化时的处理逻辑
    }

    /**
     * 查找并点击元素 (通过content-desc)
     */
    public boolean findAndClickByContentDesc(String contentDesc) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.e(TAG, "无法获取根节点");
            return false;
        }

        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(contentDesc);
        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo node : nodes) {
                if (node.isClickable()) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    Log.d(TAG, "点击元素成功: " + contentDesc);
                    return true;
                }
            }
        }

        Log.w(TAG, "未找到可点击元素: " + contentDesc);
        return false;
    }

    /**
     * 查找并点击元素 (通过resourceId)
     */
    public boolean findAndClickByResourceId(String resourceId) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            return false;
        }

        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByViewId(resourceId);
        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo node : nodes) {
                if (node.isClickable()) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    Log.d(TAG, "点击元素成功: " + resourceId);
                    return true;
                }
            }
        }

        return false;
    }
}

