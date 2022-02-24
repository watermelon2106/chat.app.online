package com.example.chatzyapp.models;

public class ModelChatlist {
    String id; //lấy id khóa chính để lấy danh sách trò truyện, và UID của ng nhận và người gửi tin nhắn

    boolean isBlocked = false;

    public ModelChatlist() {
    }

    public ModelChatlist(String id, boolean isBlocked) {
        this.id = id;
        this.isBlocked = isBlocked;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }
}
