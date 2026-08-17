package com.example.login_new.login;

import java.io.Serializable;
import java.util.Date;

public class Comment implements Serializable {
    private String id;
    private String postId;
    private String author;
    private String uid;
    private String content;
    private Date timestamp;
    private String parentId;

    // 이 부분 추가
    private int depth = 0;

    public Comment() {
    }

    public Comment(String id, String postId, String author, String uid, String content, Date timestamp, String parentId) {
        this.id = id;
        this.postId = postId;
        this.author = author;
        this.uid = uid;
        this.content = content;
        this.timestamp = timestamp;
        this.parentId = parentId;
    }

    // getters / setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    // 여기부터 추가
    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }
}
