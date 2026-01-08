package com.wechat.auto;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 任务配置页面
 * 功能:
 * 1. 选择要发送消息的好友(支持多选、全选)
 * 2. 配置消息内容(文字、图片、视频、卡片)
 * 3. 保存任务配置
 * 4. 启动自动化任务
 */
public class TaskConfigActivity extends AppCompatActivity {

    private static final String TAG = "TaskConfigActivity";
    private static final String PREFS_NAME = "WeChatAutoPrefs";

    private Button btnBack;
    private Button btnSelectFriends;
    private TextView tvSelectedCount;
    private RadioGroup rgMessageType;
    private LinearLayout llTextInput;
    private LinearLayout llImageInput;
    private EditText etMessageText;
    private Button btnStartTask;

    private List<FriendItem> friendsList = new ArrayList<>();
    private Set<String> selectedFriends = new HashSet<>();
    private Dialog friendsDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_config);

        initViews();
        loadFriends();
        setupListeners();
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnSelectFriends = findViewById(R.id.btn_select_friends);
        tvSelectedCount = findViewById(R.id.tv_selected_count);
        rgMessageType = findViewById(R.id.rg_message_type);
        llTextInput = findViewById(R.id.ll_text_input);
        llImageInput = findViewById(R.id.ll_image_input);
        etMessageText = findViewById(R.id.et_message_text);
        btnStartTask = findViewById(R.id.btn_start_task);
    }

    /**
     * 加载好友列表 (保持顺序,兼容旧版本)
     */
    private void loadFriends() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        friendsList.clear();

        try {
            // 尝试读取新格式(JSON字符串)
            String friendsJson = prefs.getString("friends_list", null);

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
                                friendsList.add(new FriendItem(friend, false));
                            }
                        }
                    }
                }
            } else {
                // 旧格式:StringSet
                Set<String> friendsSet = prefs.getStringSet("friends_list", null);
                if (friendsSet != null) {
                    for (String nickname : friendsSet) {
                        friendsList.add(new FriendItem(nickname, false));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "加载好友列表失败: " + e.getMessage());
        }

        Log.d(TAG, "加载了 " + friendsList.size() + " 位好友");
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 返回按钮
        btnBack.setOnClickListener(v -> finish());

        // 选择好友按钮
        btnSelectFriends.setOnClickListener(v -> showFriendsDialog());

        // 消息类型切换
        rgMessageType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_text) {
                llTextInput.setVisibility(View.VISIBLE);
                llImageInput.setVisibility(View.GONE);
            } else if (checkedId == R.id.rb_image) {
                llTextInput.setVisibility(View.GONE);
                llImageInput.setVisibility(View.VISIBLE);
            } else {
                llTextInput.setVisibility(View.GONE);
                llImageInput.setVisibility(View.GONE);
                Toast.makeText(this, "该功能即将上线", Toast.LENGTH_SHORT).show();
            }
        });

        // 开始任务
        btnStartTask.setOnClickListener(v -> startTask());
    }

    /**
     * 显示好友选择弹窗
     */
    private void showFriendsDialog() {
        friendsDialog = new Dialog(this);
        friendsDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        friendsDialog.setContentView(R.layout.dialog_select_friends);

        // 设置弹窗大小(占屏幕90%高度)
        Window window = friendsDialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                           (int)(getResources().getDisplayMetrics().heightPixels * 0.9));
        }

        // 初始化弹窗内的控件
        EditText etSearch = friendsDialog.findViewById(R.id.et_search);
        CheckBox cbSelectAll = friendsDialog.findViewById(R.id.cb_select_all);
        TextView tvCount = friendsDialog.findViewById(R.id.tv_selected_count);
        ListView lvFriends = friendsDialog.findViewById(R.id.lv_friends);
        Button btnClose = friendsDialog.findViewById(R.id.btn_close);
        Button btnConfirm = friendsDialog.findViewById(R.id.btn_confirm);

        // 创建过滤列表(初始显示全部)
        List<FriendItem> filteredList = new ArrayList<>(friendsList);

        // 设置适配器
        FriendsAdapter adapter = new FriendsAdapter(this, filteredList, tvCount);
        lvFriends.setAdapter(adapter);

        // 搜索功能
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String keyword = s.toString().trim().toLowerCase();
                filteredList.clear();

                if (keyword.isEmpty()) {
                    // 搜索框为空,显示全部
                    filteredList.addAll(friendsList);
                } else {
                    // 根据关键词过滤
                    for (FriendItem friend : friendsList) {
                        if (friend.getNickname().toLowerCase().contains(keyword)) {
                            filteredList.add(friend);
                        }
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 全选/取消全选(只对当前显示的列表操作)
        cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for (FriendItem friend : filteredList) {
                friend.setSelected(isChecked);
            }
            adapter.notifyDataSetChanged();
            updateDialogCount(tvCount);
        });

        // 关闭按钮
        btnClose.setOnClickListener(v -> friendsDialog.dismiss());

        // 确认按钮
        btnConfirm.setOnClickListener(v -> {
            updateSelectedCount();
            friendsDialog.dismiss();
        });

        friendsDialog.show();
    }

    /**
     * 更新弹窗中的已选数量
     */
    private void updateDialogCount(TextView tvCount) {
        int count = 0;
        for (FriendItem friend : friendsList) {
            if (friend.isSelected()) {
                count++;
            }
        }
        tvCount.setText("已选择 " + count + " 位好友");
    }

    /**
     * 更新已选好友数量(主页面)
     */
    private void updateSelectedCount() {
        selectedFriends.clear();
        for (FriendItem friend : friendsList) {
            if (friend.isSelected()) {
                selectedFriends.add(friend.getNickname());
            }
        }
        tvSelectedCount.setText("✅ 已选择 " + selectedFriends.size() + " 位好友");
    }

    /**
     * 开始执行任务
     */
    private void startTask() {
        // TODO: 实现任务执行逻辑
        Toast.makeText(this, "任务配置完成,即将开始执行", Toast.LENGTH_SHORT).show();
    }

    /**
     * 好友列表适配器
     */
    private class FriendsAdapter extends BaseAdapter {
        private Context context;
        private List<FriendItem> friends;
        private TextView tvCount; // 用于更新弹窗中的计数

        public FriendsAdapter(Context context, List<FriendItem> friends) {
            this(context, friends, null);
        }

        public FriendsAdapter(Context context, List<FriendItem> friends, TextView tvCount) {
            this.context = context;
            this.friends = friends;
            this.tvCount = tvCount;
        }

        @Override
        public int getCount() {
            return friends.size();
        }

        @Override
        public Object getItem(int position) {
            return friends.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_friend_checkbox, parent, false);
                holder = new ViewHolder();
                holder.cbFriend = convertView.findViewById(R.id.cb_friend);
                holder.tvAvatar = convertView.findViewById(R.id.tv_avatar);
                holder.tvNickname = convertView.findViewById(R.id.tv_nickname);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            FriendItem friend = friends.get(position);
            holder.tvNickname.setText(friend.getNickname());
            holder.cbFriend.setChecked(friend.isSelected());

            // 显示头像(取昵称首字母)
            String nickname = friend.getNickname();
            String avatar = nickname.length() > 0 ? nickname.substring(0, 1) : "?";
            holder.tvAvatar.setText(avatar);

            // 复选框点击事件
            holder.cbFriend.setOnCheckedChangeListener((buttonView, isChecked) -> {
                friend.setSelected(isChecked);
                if (tvCount != null) {
                    updateDialogCount(tvCount);
                }
            });

            // 整行点击事件
            convertView.setOnClickListener(v -> {
                friend.setSelected(!friend.isSelected());
                holder.cbFriend.setChecked(friend.isSelected());
                if (tvCount != null) {
                    updateDialogCount(tvCount);
                }
            });

            return convertView;
        }

        class ViewHolder {
            CheckBox cbFriend;
            TextView tvAvatar;
            TextView tvNickname;
        }
    }

    /**
     * 好友数据类
     */
    private static class FriendItem {
        private String nickname;
        private boolean selected;

        public FriendItem(String nickname, boolean selected) {
            this.nickname = nickname;
            this.selected = selected;
        }

        public String getNickname() {
            return nickname;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }
}

