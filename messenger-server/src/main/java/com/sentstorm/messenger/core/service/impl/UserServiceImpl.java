package com.sentstorm.messenger.core.service.impl;

import com.sentstorm.messenger.core.entity.User;
import com.sentstorm.messenger.core.service.CurrentUserService;
import com.sentstorm.messenger.core.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final CurrentUserService currentUserService;

    @Override
    public User getCurrentUser() {
        return currentUserService.getCurrentUser();
    }

}