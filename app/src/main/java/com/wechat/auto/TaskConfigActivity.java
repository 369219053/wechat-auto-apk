package com.wechat.auto;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import com.wechat.auto.model.SendTask;
import com.wechat.auto.service.WeChatAccessibilityService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 任务配置页面
 * 功能:
 * 1. 选择要发送消息的好友(支持多选、全选)
 * 2. 配置消息内容(支持多条文字、多张图片、多个视频)
 * 3. 保存任务配置
 * 4. 启动自动化任务
 */
public class TaskConfigActivity extends AppCompatActivity {

    private static final String TAG = "TaskConfigActivity";
    private static final String PREFS_NAME = "WeChatAutoPrefs";

    // 请求码
    private static final int REQUEST_IMAGE_PICK = 1001;
    private static final int REQUEST_VIDEO_PICK = 1002;

    // 步骤1: 选择好友
    private Button btnBack;
    private Button btnSelectFriends;
    private TextView tvSelectedCount;

    // 步骤2: 配置消息(支持多选)
    private Button btnAddText;
    private Button btnAddImage;
    private Button btnAddVideo;
    private TextView tvMessageCount;
    private ListView lvMessages;

    private Button btnStartTask;

    private List<FriendItem> friendsList = new ArrayList<>();
    private Set<String> selectedFriends = new HashSet<>();
    private List<MessageItem> messagesList = new ArrayList<>();
    private MessagesAdapter messagesAdapter;
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
        // 步骤1: 选择好友
        btnBack = findViewById(R.id.btn_back);
        btnSelectFriends = findViewById(R.id.btn_select_friends);
        tvSelectedCount = findViewById(R.id.tv_selected_count);

        // 步骤2: 配置消息(支持多选)
        btnAddText = findViewById(R.id.btn_add_text);
        btnAddImage = findViewById(R.id.btn_add_image);
        btnAddVideo = findViewById(R.id.btn_add_video);
        tvMessageCount = findViewById(R.id.tv_message_count);
        lvMessages = findViewById(R.id.lv_messages);

        btnStartTask = findViewById(R.id.btn_start_task);

        // 初始化消息列表适配器
        messagesAdapter = new MessagesAdapter(this, messagesList);
        lvMessages.setAdapter(messagesAdapter);
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

        // 添加消息按钮
        btnAddText.setOnClickListener(v -> showAddTextDialog());
        btnAddImage.setOnClickListener(v -> addImageMessage());
        btnAddVideo.setOnClickListener(v -> addVideoMessage());

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
     * 显示添加文字消息对话框
     */
    private void showAddTextDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_text);

        EditText etText = dialog.findViewById(R.id.et_text);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnConfirm = dialog.findViewById(R.id.btn_confirm);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String text = etText.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "请输入文字内容", Toast.LENGTH_SHORT).show();
                return;
            }

            // 添加到消息列表
            messagesList.add(new MessageItem("text", text));
            messagesAdapter.notifyDataSetChanged();
            updateMessageCount();

            dialog.dismiss();
            Toast.makeText(this, "已添加文字消息", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    /**
     * 添加图片消息
     */
    private void addImageMessage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false); // 单选
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    /**
     * 添加视频消息
     */
    private void addVideoMessage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("video/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false); // 单选
        startActivityForResult(intent, REQUEST_VIDEO_PICK);
    }

    /**
     * 处理文件选择结果
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        Uri uri = data.getData();
        if (uri == null) {
            Toast.makeText(this, "未选择文件", Toast.LENGTH_SHORT).show();
            return;
        }

        String filePath = getFilePathFromUri(uri);
        if (filePath == null) {
            Toast.makeText(this, "无法获取文件路径", Toast.LENGTH_SHORT).show();
            return;
        }

        if (requestCode == REQUEST_IMAGE_PICK) {
            // 添加图片消息
            messagesList.add(new MessageItem("image", filePath));
            messagesAdapter.notifyDataSetChanged();
            updateMessageCount();
            Toast.makeText(this, "已添加图片消息", Toast.LENGTH_SHORT).show();

        } else if (requestCode == REQUEST_VIDEO_PICK) {
            // 添加视频消息
            messagesList.add(new MessageItem("video", filePath));
            messagesAdapter.notifyDataSetChanged();
            updateMessageCount();
            Toast.makeText(this, "已添加视频消息", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 从URI获取文件路径
     */
    private String getFilePathFromUri(Uri uri) {
        String filePath = null;

        // 尝试从MediaStore获取路径
        String[] projection = {MediaStore.MediaColumns.DATA};
        Cursor cursor = null;

        try {
            cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                filePath = cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            Log.e(TAG, "获取文件路径失败: " + e.getMessage());
            // 如果失败,直接使用URI的路径
            filePath = uri.getPath();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return filePath;
    }

    /**
     * 删除消息
     */
    private void deleteMessage(int position) {
        messagesList.remove(position);
        messagesAdapter.notifyDataSetChanged();
        updateMessageCount();
        Toast.makeText(this, "已删除消息", Toast.LENGTH_SHORT).show();
    }

    /**
     * 更新消息数量显示
     */
    private void updateMessageCount() {
        tvMessageCount.setText("已添加 " + messagesList.size() + " 条消息");
    }

    /**
     * 开始执行任务
     */
    private void startTask() {
        // 验证是否选择了好友
        if (selectedFriends.isEmpty()) {
            Toast.makeText(this, "请先选择要发送的好友", Toast.LENGTH_SHORT).show();
            return;
        }

        // 验证是否添加了消息
        if (messagesList.isEmpty()) {
            Toast.makeText(this, "请先添加要发送的消息", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查无障碍服务是否开启
        WeChatAccessibilityService service = WeChatAccessibilityService.getInstance();
        if (service == null) {
            Toast.makeText(this, "请先开启无障碍服务", Toast.LENGTH_LONG).show();
            return;
        }

        // 构建任务数据
        SendTask task = new SendTask();

        // 添加好友列表
        List<String> friendNames = new ArrayList<>();
        for (String friendName : selectedFriends) {
            friendNames.add(friendName);
        }
        task.setFriendNames(friendNames);

        // 添加消息列表
        List<SendTask.Message> messages = new ArrayList<>();
        for (MessageItem item : messagesList) {
            messages.add(new SendTask.Message(item.type, item.content));
        }
        task.setMessages(messages);

        // 启动任务
        service.startSendTask(task);

        // 提示用户
        String info = String.format("开始执行任务:\n向 %d 位好友发送 %d 条消息",
                                   selectedFriends.size(), messagesList.size());
        Toast.makeText(this, info, Toast.LENGTH_LONG).show();

        // 返回主界面
        finish();
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

    /**
     * 消息数据类
     */
    private static class MessageItem {
        private String type;  // "text", "image", "video"
        private String content;  // 文字内容或文件路径

        public MessageItem(String type, String content) {
            this.type = type;
            this.content = content;
        }

        public String getType() {
            return type;
        }

        public String getContent() {
            return content;
        }

        public String getTypeDisplay() {
            switch (type) {
                case "text": return "文字消息";
                case "image": return "图片消息";
                case "video": return "视频消息";
                default: return "未知类型";
            }
        }

        public String getIcon() {
            switch (type) {
                case "text": return "📝";
                case "image": return "🖼️";
                case "video": return "🎬";
                default: return "❓";
            }
        }
    }

    /**
     * 消息列表适配器
     */
    private class MessagesAdapter extends BaseAdapter {
        private Context context;
        private List<MessageItem> messages;

        public MessagesAdapter(Context context, List<MessageItem> messages) {
            this.context = context;
            this.messages = messages;
        }

        @Override
        public int getCount() {
            return messages.size();
        }

        @Override
        public Object getItem(int position) {
            return messages.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_message, parent, false);
                holder = new ViewHolder();
                holder.tvIcon = convertView.findViewById(R.id.tv_message_icon);
                holder.tvType = convertView.findViewById(R.id.tv_message_type);
                holder.tvContent = convertView.findViewById(R.id.tv_message_content);
                holder.btnDelete = convertView.findViewById(R.id.btn_delete);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            MessageItem message = messages.get(position);
            holder.tvIcon.setText(message.getIcon());
            holder.tvType.setText(message.getTypeDisplay());
            holder.tvContent.setText(message.getContent());

            // 删除按钮点击事件
            holder.btnDelete.setOnClickListener(v -> deleteMessage(position));

            return convertView;
        }

        class ViewHolder {
            TextView tvIcon;
            TextView tvType;
            TextView tvContent;
            Button btnDelete;
        }
    }
}

