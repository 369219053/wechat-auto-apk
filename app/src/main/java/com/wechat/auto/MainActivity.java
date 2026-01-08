package com.wechat.auto;

import android.content.Intent;
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

    private TextView tvServiceStatus;
    private Button btnEnableAccessibility;
    private Button btnEnableOverlay;
    private Button btnStartService;
    private Button btnStopService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupListeners();
        updateServiceStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateServiceStatus();
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

        // 启动服务
        btnStartService.setOnClickListener(v -> {
            Log.d(TAG, "点击启动按钮");
            WeChatAccessibilityService service = WeChatAccessibilityService.getInstance();
            Log.d(TAG, "服务实例: " + (service != null ? "存在" : "null"));

            if (service != null) {
                Log.d(TAG, "调用startAutoTask()");
                service.startAutoTask();
                Toast.makeText(this, "自动化任务已启动", Toast.LENGTH_SHORT).show();
                updateServiceStatus();
            } else {
                Log.e(TAG, "无障碍服务实例为null");
                Toast.makeText(this, "无障碍服务未运行,请先开启无障碍服务", Toast.LENGTH_LONG).show();
            }
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
    }
}

