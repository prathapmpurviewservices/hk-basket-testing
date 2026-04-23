package com.hongkongbasket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserStore {
    private static final Map<String, User> users = new HashMap<>();
    private static final Map<String, String> sessions = new HashMap<>();

    public static boolean userExists(String email) {
        return users.values().stream().anyMatch(u -> u.getEmail().equals(email));
    }

    public static User registerUser(String email, String password, String fullName) {
        if (userExists(email)) {
            return null;
        }
        String userId = UUID.randomUUID().toString();
        User user = new User(userId, email, password, fullName);
        users.put(userId, user);
        return user;
    }

    public static User loginUser(String email, String password) {
        User user = users.values().stream()
                .filter(u -> u.getEmail().equals(email) && u.validatePassword(password))
                .findFirst()
                .orElse(null);
        return user;
    }

    public static String createSession(String userId) {
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, userId);
        return sessionId;
    }

    public static User getSessionUser(String sessionId) {
        String userId = sessions.get(sessionId);
        if (userId == null) {
            return null;
        }
        return users.get(userId);
    }

    public static void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }

    public static boolean isValidSession(String sessionId) {
        return sessions.containsKey(sessionId);
    }
}
