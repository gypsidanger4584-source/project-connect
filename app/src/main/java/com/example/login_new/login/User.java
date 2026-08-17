// User.java
package com.example.login_new.login;

public class User {
    public String id;
    public String password;
    public String studentNumber;
    public String email;
    public String gender;
    public String major;

    public User() {} // Firebase에서 필요로 함 (빈 생성자)

    public User(String id, String password, String studentNumber, String email, String gender, String major) {
        this.id = id;
        this.password = password;
        this.studentNumber = studentNumber;
        this.email = email;
        this.gender = gender;
        this.major = major;
    }
}
