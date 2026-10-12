package com.campus.lostfound.client.util;

import com.campus.lostfound.client.model.Role;
import com.campus.lostfound.client.model.UserDto;

import java.util.prefs.Preferences;

public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();
    private static final Preferences PREFS = Preferences.userNodeForPackage(SessionManager.class);
    private static final String PREF_REMEMBERED_IDENTIFIER = "remembered_identifier";

    private String token;
    private UserDto currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public synchronized void setSession(String token, UserDto user) {
        this.token = token;
        this.currentUser = user;
    }

    public synchronized void clearSession() {
        this.token = null;
        this.currentUser = null;
    }

    public synchronized String getToken() {
        return token;
    }

    public synchronized UserDto getCurrentUser() {
        return currentUser;
    }

    public synchronized boolean isLoggedIn() {
        return token != null && currentUser != null;
    }

    public synchronized boolean isStaffOrAdmin() {
        if (currentUser == null) return false;
        return currentUser.getRole() == Role.STAFF || currentUser.getRole() == Role.ADMIN;
    }

    public synchronized boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == Role.ADMIN;
    }

    public synchronized boolean isStudent() {
        return currentUser != null && currentUser.getRole() == Role.STUDENT;
    }

    public void saveRememberedIdentifier(String identifier) {
        if (identifier != null && !identifier.isBlank()) {
            PREFS.put(PREF_REMEMBERED_IDENTIFIER, identifier);
        } else {
            PREFS.remove(PREF_REMEMBERED_IDENTIFIER);
        }
    }

    public String getRememberedIdentifier() {
        return PREFS.get(PREF_REMEMBERED_IDENTIFIER, "");
    }
}
