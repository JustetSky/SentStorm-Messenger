package com.sentstorm.messenger.core.repository;

import com.sentstorm.messenger.core.entity.UserDevice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, UUID> {

    List<UserDevice> findByUserId(UUID userId);

    Optional<UserDevice> findByUserIdAndDeviceId(UUID userId, String deviceId);

}