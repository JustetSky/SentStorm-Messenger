package com.sentstorm.messenger.api.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiPath {

    public static final String USERS = "/users";
    public static final String CURRENT_USER = "/me";
    public static final String PUBLIC_ID = "/{publicId}";
    public static final String SEARCH = "/search";
    public static final String CHATS = "/chats";
    public static final String CHAT_ID = "/{chatId}";
    public static final String MESSAGES = "/messages";
    public static final String MESSAGE_ID = "/{messageId}";
    public static final String DELIVERED = "/delivered";
    public static final String READ = "/read";
    public static final String DEVICES = "/devices";
    public static final String PUSH_TOKEN = "/push-token";
    public static final String DEVICE_ID = "/{deviceId}";

}