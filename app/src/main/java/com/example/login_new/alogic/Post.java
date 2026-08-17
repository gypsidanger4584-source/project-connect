package com.example.login_new.alogic;

import java.io.Serializable;
import java.util.Date;

public class Post implements Serializable {
    private String id;
    private String title;
    private String content;
    private String author;
    private Date timestamp;
    private String border;

    // Firestore용 기본 생성자
    public Post() {}

    // 전체 필드 생성자
    public Post(String id, String title, String content, String author, String border) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
        this.timestamp = timestamp;
        this.border = border;
    }

    // getter/setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getBorder() { return border; }
    public void setBorder(String border) { this.border = border; }
}
