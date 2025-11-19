package domain.utils;

import domain.User;

public class LoginResponse {
    private User user;
    private Boolean requires2FA;

    public LoginResponse() {
    }

    public LoginResponse(User user, Boolean requires2FA) {
        this.user = user;
        this.requires2FA = requires2FA;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Boolean getRequires2FA() {
        return requires2FA;
    }

    public void setRequires2FA(Boolean requires2FA) {
        this.requires2FA = requires2FA;
    }
}

