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

}