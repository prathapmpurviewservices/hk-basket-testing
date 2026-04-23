package com.hongkongbasket;

public class User {
    private final String userId;
    private final String email;
    private final String password;
    private final String fullName;

    public User(String userId, String email, String password, String fullName) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getFullName() {
        return fullName;
    }

    public boolean validatePassword(String inputPassword) {
        return this.password.equals(inputPassword);
    }
}
