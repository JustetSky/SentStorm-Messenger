package com.sentstorm.messenger.core.service;

import com.sentstorm.messenger.api.dto.UserSearchProjection;
import com.sentstorm.messenger.core.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    User getCurrentUser();

    User getUserByPublicId(String publicId);

    Page<UserSearchProjection> searchUsers(String query, Pageable pageable);

}