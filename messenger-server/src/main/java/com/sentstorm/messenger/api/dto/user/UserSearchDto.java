package com.sentstorm.messenger.api.dto.user;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchDto {

    private String publicId;
    private String firstName;
    private String lastName;
    private Instant lastSeen;

}