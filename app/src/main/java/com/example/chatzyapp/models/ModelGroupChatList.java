package com.example.chatzyapp.models;

public class ModelGroupChatList {
    String groupId, groupTitle, groupDescription, groupIcon, timeStamp, createdBy;

    public ModelGroupChatList() {
    }

    public ModelGroupChatList(String groupId, String groupTitle, String groupDescription, String groupIcon, String timeStamp, String createdBy) {
        this.groupId = groupId;
        this.groupTitle = groupTitle;
        this.groupDescription = groupDescription;
        this.groupIcon = groupIcon;
        this.timeStamp = timeStamp;
        this.createdBy = createdBy;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupTitle() {
        return groupTitle;
    }

    public void setGroupTitle(String groupTitle) {
        this.groupTitle = groupTitle;
    }

    public String getGroupDescription() {
        return groupDescription;
    }

    public void setGroupDescription(String groupDescription) {
        this.groupDescription = groupDescription;
    }

    public String getGroupIcon() {
        return groupIcon;
    }

    public void setGroupIcon(String groupIcon) {
        this.groupIcon = groupIcon;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
