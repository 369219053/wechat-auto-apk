package com.wechat.auto.utils;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;

/**
 * 权限帮助类
 * 功能:
 * 1. 检查无障碍服务是否开启
 * 2. 检查悬浮窗权限
 * 3. 申请各种权限
 */
public class PermissionHelper {

    /**
     * 检查无障碍服务是否开启
     */
    public static boolean isAccessibilityServiceEnabled(Context context, Class<? extends AccessibilityService> serviceClass) {
        String serviceName = context.getPackageName() + "/" + serviceClass.getName();
        
        try {
            int accessibilityEnabled = Settings.Secure.getInt(
                    context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
            
            if (accessibilityEnabled == 1) {
                String settingValue = Settings.Secure.getString(
                        context.getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                
                if (settingValue != null) {
                    TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
                    splitter.setString(settingValue);
                    
                    while (splitter.hasNext()) {
                        String accessibilityService = splitter.next();
                        if (accessibilityService.equalsIgnoreCase(serviceName)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * 检查悬浮窗权限
     */
    public static boolean hasOverlayPermission(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }

    /**
     * 申请悬浮窗权限
     */
    public static void requestOverlayPermission(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
    }

    /**
     * 打开无障碍服务设置页面
     */
    public static void openAccessibilitySettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}

