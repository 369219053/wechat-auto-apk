package com.wechat.auto.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 发送任务数据模型
 * 包含要发送的好友列表和消息列表
 */
public class SendTask implements Serializable {
    
    private List<String> friendNames;  // 好友昵称列表
    private List<Message> messages;     // 消息列表
    
    public SendTask() {
        this.friendNames = new ArrayList<>();
        this.messages = new ArrayList<>();
    }
    
    public SendTask(List<String> friendNames, List<Message> messages) {
        this.friendNames = friendNames;
        this.messages = messages;
    }
    
    public List<String> getFriendNames() {
        return friendNames;
    }
    
    public void setFriendNames(List<String> friendNames) {
        this.friendNames = friendNames;
    }
    
    public List<Message> getMessages() {
        return messages;
    }
    
    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
    
    /**
     * 消息数据模型
     */
    public static class Message implements Serializable {
        private String type;     // 消息类型: text, image, video
        private String content;  // 消息内容: 文字内容或文件路径
        
        public Message() {
        }
        
        public Message(String type, String content) {
            this.type = type;
            this.content = content;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
    }
}

