package com.wechat.auto;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
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
            Set<String> friendsSet = prefs.getStringSet(KEY_FRIENDS, new HashSet<>());

            if (friendsSet.isEmpty()) {
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
     * 保存好友列表到SharedPreferences
     */
    private void saveFriendsToPrefs(ArrayList<String> friends) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> friendsSet = new HashSet<>(friends);
        editor.putStringSet(KEY_FRIENDS, friendsSet);
        editor.apply();

        Log.d(TAG, "好友列表已保存到SharedPreferences");
    }

    /**
     * 从SharedPreferences加载好友列表
     */
    private void loadFriendsFromPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> friendsSet = prefs.getStringSet(KEY_FRIENDS, new HashSet<>());
        long syncTime = prefs.getLong("sync_time", 0);

        if (!friendsSet.isEmpty()) {
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

            tvFriendCount.setText("✅ 共 " + friendsSet.size() + " 位好友" + timeInfo);
            Log.d(TAG, "从SharedPreferences加载了 " + friendsSet.size() + " 位好友");
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

