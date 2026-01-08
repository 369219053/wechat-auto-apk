package com.wechat.auto.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
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
     * 读取通讯录好友列表(带滚动加载)
     */
    private void readContactsList() {
        try {
            Log.d(TAG, "开始读取通讯录,将滚动加载所有好友...");

            // 使用Set避免重复
            Set<String> allFriends = new HashSet<>();
            int scrollCount = 0;
            int maxScrolls = 500; // 最大滚动次数(设置为500,足够大),主要依靠连续无新好友判断
            int noNewFriendsCount = 0; // 连续没有新好友的次数

            while (scrollCount < maxScrolls) {
                int beforeSize = allFriends.size();

                // 读取当前屏幕的好友
                Set<String> currentFriends = readCurrentScreenFriends();
                allFriends.addAll(currentFriends);

                int afterSize = allFriends.size();
                int newFriends = afterSize - beforeSize;

                Log.d(TAG, "第" + (scrollCount + 1) + "次读取: 本次找到" + currentFriends.size() + "个好友, 新增" + newFriends + "个, 总计" + afterSize + "个");

                if (newFriends == 0) {
                    noNewFriendsCount++;
                    Log.d(TAG, "连续" + noNewFriendsCount + "次没有新好友");
                } else {
                    noNewFriendsCount = 0;
                }

                // 如果连续5次没有新好友,说明已经到底了(增加容错次数)
                if (noNewFriendsCount >= 5) {
                    Log.d(TAG, "已到达列表底部(连续5次无新好友)");
                    break;
                }

                // 执行滚动
                boolean scrolled = scrollContactsList();
                if (!scrolled) {
                    Log.w(TAG, "滚动失败,可能已到底部");
                    break;
                }

                scrollCount++;

                // 等待滚动完成和列表加载(增加等待时间到1.5秒)
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Log.d(TAG, "通讯录读取完成! 总共找到 " + allFriends.size() + " 个真实好友");

            // 转换为List并按首字母排序(模仿微信排序:字母在前,特殊符号在后)
            List<String> sortedFriends = new ArrayList<>(allFriends);
            java.util.Collections.sort(sortedFriends, new java.util.Comparator<String>() {
                @Override
                public int compare(String s1, String s2) {
                    // 判断是否以字母开头
                    boolean s1StartsWithLetter = s1.length() > 0 && Character.isLetter(s1.charAt(0));
                    boolean s2StartsWithLetter = s2.length() > 0 && Character.isLetter(s2.charAt(0));

                    // 字母开头的排在前面
                    if (s1StartsWithLetter && !s2StartsWithLetter) {
                        return -1;
                    } else if (!s1StartsWithLetter && s2StartsWithLetter) {
                        return 1;
                    } else {
                        // 都是字母或都不是字母,按字母顺序排序
                        return s1.compareToIgnoreCase(s2);
                    }
                }
            });

            Log.d(TAG, "好友列表已按首字母排序");

            // 打印前10个好友用于调试
            int printCount = Math.min(10, sortedFriends.size());
            StringBuilder sb = new StringBuilder("前" + printCount + "个好友: ");
            for (int i = 0; i < printCount; i++) {
                sb.append(sortedFriends.get(i)).append(", ");
            }
            Log.d(TAG, sb.toString());

            // 发送广播通知MainActivity
            sendFriendsBroadcast(sortedFriends);

        } catch (Exception e) {
            Log.e(TAG, "读取通讯录失败: " + e.getMessage(), e);
        }
    }

    /**
     * 读取当前屏幕上的好友
     */
    private Set<String> readCurrentScreenFriends() {
        Set<String> friends = new HashSet<>();

        try {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) {
                Log.e(TAG, "无法获取根节点");
                return friends;
            }

            // 查找所有好友昵称节点 (resource-id: com.tencent.mm:id/kbq)
            List<AccessibilityNodeInfo> friendNodes = rootNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/kbq");

            if (friendNodes != null && !friendNodes.isEmpty()) {
                for (AccessibilityNodeInfo node : friendNodes) {
                    CharSequence text = node.getText();
                    if (text != null && text.length() > 0) {
                        String nickname = text.toString();

                        // 过滤掉特殊项
                        if (!isSpecialItem(nickname)) {
                            friends.add(nickname);
                        }
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "读取当前屏幕好友失败: " + e.getMessage(), e);
        }

        return friends;
    }

    /**
     * 滚动通讯录列表(使用手势滑动)
     */
    private boolean scrollContactsList() {
        try {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) {
                Log.e(TAG, "无法获取根节点");
                return false;
            }

            // 查找RecyclerView (通讯录列表)
            List<AccessibilityNodeInfo> recyclerViews = rootNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/mg");

            if (recyclerViews == null || recyclerViews.isEmpty()) {
                Log.w(TAG, "未找到RecyclerView,尝试全屏滑动");
                return performSwipeGesture(360, 1200, 360, 400);
            }

            AccessibilityNodeInfo recyclerView = recyclerViews.get(0);
            android.graphics.Rect rect = new android.graphics.Rect();
            recyclerView.getBoundsInScreen(rect);

            Log.d(TAG, "RecyclerView bounds: " + rect.toString());

            // 在RecyclerView区域内滑动
            int centerX = rect.centerX();
            int startY = (int) (rect.top + rect.height() * 0.8);  // 从80%位置开始
            int endY = (int) (rect.top + rect.height() * 0.2);    // 滑动到20%位置

            return performSwipeGesture(centerX, startY, centerX, endY);

        } catch (Exception e) {
            Log.e(TAG, "滚动失败: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * 执行滑动手势
     */
    private boolean performSwipeGesture(int startX, int startY, int endX, int endY) {
        try {
            Path path = new Path();
            path.moveTo(startX, startY);
            path.lineTo(endX, endY);

            // 创建手势描述(滑动时长800ms,更慢更自然,给列表更多加载时间)
            GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, 800);

            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(stroke);

            // 执行手势
            boolean dispatched = dispatchGesture(builder.build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    Log.d(TAG, "滑动手势执行完成");
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    Log.w(TAG, "滑动手势被取消");
                }
            }, null);

            Log.d(TAG, String.format("执行滑动手势 (%d,%d)->(%d,%d): %s",
                startX, startY, endX, endY, dispatched ? "成功" : "失败"));
            return dispatched;

        } catch (Exception e) {
            Log.e(TAG, "执行滑动手势失败: " + e.getMessage(), e);
            return false;
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
     * 保存好友列表到SharedPreferences (保持顺序)
     */
    private void saveFriendsToPrefs(List<String> friends) {
        SharedPreferences prefs = getSharedPreferences("WeChatAutoPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // 使用JSON字符串保存,保持顺序
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < friends.size(); i++) {
            if (i > 0) json.append(",");
            // 转义双引号和反斜杠
            String escaped = friends.get(i).replace("\\", "\\\\").replace("\"", "\\\"");
            json.append("\"").append(escaped).append("\"");
        }
        json.append("]");

        editor.putString("friends_list", json.toString());
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

