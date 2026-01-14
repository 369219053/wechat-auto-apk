package com.wechat.auto;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.wechat.auto.service.WeChatAccessibilityService;
import com.wechat.auto.utils.PermissionHelper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * 主Activity - 应用入口
 * 功能:
 * 1. 检查和申请无障碍服务权限
 * 2. 检查和申请悬浮窗权限
 * 3. 启动/停止自动化服务
 * 4. 显示服务状态
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    public static final String ACTION_FRIENDS_SYNCED = "com.wechat.auto.FRIENDS_SYNCED";
    public static final String EXTRA_FRIENDS = "friends";
    private static final String PREFS_NAME = "WeChatAutoPrefs";
    private static final String KEY_FRIENDS = "friends_list";

    private TextView tvServiceStatus;
    private Button btnEnableAccessibility;
    private Button btnEnableOverlay;
    private Button btnStartService;
    private Button btnStopService;
    private Button btnSyncContacts;
    private TextView tvFriendCount;

    private BroadcastReceiver friendsReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupListeners();
        setupBroadcastReceiver();
        loadFriendsFromPrefs();
        updateServiceStatus();

        // 请求存储权限
        requestStoragePermissions();
    }

    /**
     * 请求存储权限
     */
    private void requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 使用新的媒体权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                    new String[]{
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO
                    }, 100);
            }
        } else {
            // Android 12及以下使用旧的存储权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Toast.makeText(this, "存储权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "存储权限被拒绝,无法选择图片和视频", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateServiceStatus();
        // 每次回到前台时重新加载好友列表
        loadFriendsFromPrefs();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (friendsReceiver != null) {
            unregisterReceiver(friendsReceiver);
        }
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        tvServiceStatus = findViewById(R.id.tv_service_status);
        btnEnableAccessibility = findViewById(R.id.btn_enable_accessibility);
        btnEnableOverlay = findViewById(R.id.btn_enable_overlay);
        btnStartService = findViewById(R.id.btn_start_service);
        btnStopService = findViewById(R.id.btn_stop_service);
        btnSyncContacts = findViewById(R.id.btn_sync_contacts);
        tvFriendCount = findViewById(R.id.tv_friend_count);
    }

    /**
     * 设置按钮监听
     */
    private void setupListeners() {
        // 开启无障碍服务
        btnEnableAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        // 开启悬浮窗权限
        btnEnableOverlay.setOnClickListener(v -> {
            PermissionHelper.requestOverlayPermission(this);
        });

        // 启动服务 - 打开任务配置页面
        btnStartService.setOnClickListener(v -> {
            // 检查是否有好友数据
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String friendsJson = prefs.getString(KEY_FRIENDS, "[]");

            // 简单判断是否为空
            boolean hasFriends = friendsJson != null && friendsJson.length() > 2;

            if (!hasFriends) {
                Toast.makeText(this, "请先同步通讯录好友", Toast.LENGTH_LONG).show();
                return;
            }

            // 打开任务配置页面
            Intent intent = new Intent(this, TaskConfigActivity.class);
            startActivity(intent);
        });

        // 停止服务
        btnStopService.setOnClickListener(v -> {
            if (WeChatAccessibilityService.getInstance() != null) {
                WeChatAccessibilityService.getInstance().stopAutoTask();
                Toast.makeText(this, "自动化任务已停止", Toast.LENGTH_SHORT).show();
                updateServiceStatus();
            } else {
                Toast.makeText(this, "无障碍服务未运行", Toast.LENGTH_SHORT).show();
            }
        });

        // 同步通讯录
        btnSyncContacts.setOnClickListener(v -> {
            WeChatAccessibilityService service = WeChatAccessibilityService.getInstance();
            if (service != null) {
                service.syncContacts();
                Toast.makeText(this, "开始同步通讯录...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "无障碍服务未运行,请先开启无障碍服务", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * 设置广播接收器
     */
    private void setupBroadcastReceiver() {
        friendsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_FRIENDS_SYNCED.equals(intent.getAction())) {
                    ArrayList<String> friends = intent.getStringArrayListExtra(EXTRA_FRIENDS);
                    if (friends != null) {
                        updateFriendsList(friends);
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter(ACTION_FRIENDS_SYNCED);
        // Android 13+ 需要指定 RECEIVER_NOT_EXPORTED (应用内广播)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(friendsReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(friendsReceiver, filter);
        }
    }

    /**
     * 更新好友列表显示
     */
    private void updateFriendsList(ArrayList<String> friends) {
        // 更新好友数量显示
        tvFriendCount.setText("✅ 共 " + friends.size() + " 位好友");

        // 保存到SharedPreferences
        saveFriendsToPrefs(friends);

        Toast.makeText(this, "同步成功! 共 " + friends.size() + " 位好友", Toast.LENGTH_SHORT).show();
    }

    /**
     * 保存好友列表到SharedPreferences (保持顺序)
     */
    private void saveFriendsToPrefs(ArrayList<String> friends) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
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

        editor.putString(KEY_FRIENDS, json.toString());
        editor.apply();

        Log.d(TAG, "好友列表已保存到SharedPreferences (保持顺序)");
    }

    /**
     * 从SharedPreferences加载好友列表 (保持顺序,兼容旧版本)
     */
    private void loadFriendsFromPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long syncTime = prefs.getLong("sync_time", 0);
        ArrayList<String> friends = new ArrayList<>();

        try {
            // 尝试读取新格式(JSON字符串)
            String friendsJson = prefs.getString(KEY_FRIENDS, null);

            if (friendsJson != null) {
                // 新格式:JSON字符串
                if (friendsJson.length() > 2) {
                    String content = friendsJson.substring(1, friendsJson.length() - 1);
                    if (!content.isEmpty()) {
                        String[] items = content.split("\",\"");
                        for (String item : items) {
                            String friend = item.replace("\"", "")
                                               .replace("\\\\", "\\")
                                               .replace("\\\"", "\"");
                            if (!friend.isEmpty()) {
                                friends.add(friend);
                            }
                        }
                    }
                }
            } else {
                // 旧格式:StringSet,尝试读取并转换
                Set<String> friendsSet = prefs.getStringSet(KEY_FRIENDS, null);
                if (friendsSet != null && !friendsSet.isEmpty()) {
                    friends.addAll(friendsSet);
                    // 转换成新格式并保存
                    saveFriendsToPrefs(friends);
                    Log.d(TAG, "已将旧格式数据转换为新格式");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "加载好友列表失败: " + e.getMessage());
            // 清除损坏的数据
            prefs.edit().remove(KEY_FRIENDS).apply();
        }

        if (!friends.isEmpty()) {
            // 显示同步时间
            String timeInfo = "";
            if (syncTime > 0) {
                long diff = System.currentTimeMillis() - syncTime;
                if (diff < 60000) {
                    timeInfo = " (刚刚同步)";
                } else if (diff < 3600000) {
                    timeInfo = " (" + (diff / 60000) + "分钟前同步)";
                } else {
                    timeInfo = " (" + (diff / 3600000) + "小时前同步)";
                }
            }

            tvFriendCount.setText("✅ 共 " + friends.size() + " 位好友" + timeInfo);
            Log.d(TAG, "从SharedPreferences加载了 " + friends.size() + " 位好友");
        } else {
            tvFriendCount.setText("⚠️ 暂无好友数据,请先同步通讯录");
        }
    }

    /**
     * 更新服务状态显示
     */
    private void updateServiceStatus() {
        boolean accessibilityEnabled = PermissionHelper.isAccessibilityServiceEnabled(
                this, WeChatAccessibilityService.class);
        boolean overlayEnabled = PermissionHelper.hasOverlayPermission(this);
        boolean serviceRunning = WeChatAccessibilityService.getInstance() != null;

        StringBuilder status = new StringBuilder();
        status.append("无障碍服务: ").append(accessibilityEnabled ? "✅ 已开启" : "❌ 未开启").append("\n");
        status.append("悬浮窗权限: ").append(overlayEnabled ? "✅ 已授予" : "❌ 未授予").append("\n");
        status.append("服务状态: ").append(serviceRunning ? "✅ 运行中" : "⏸️ 已停止");

        tvServiceStatus.setText(status.toString());

        // 更新按钮状态
        // 启动按钮: 无障碍服务已开启即可点击
        btnStartService.setEnabled(accessibilityEnabled);
        // 停止按钮: 服务运行中才能点击
        btnStopService.setEnabled(accessibilityEnabled && serviceRunning);
        // 同步通讯录按钮: 无障碍服务已开启即可点击
        btnSyncContacts.setEnabled(accessibilityEnabled);
    }
}

