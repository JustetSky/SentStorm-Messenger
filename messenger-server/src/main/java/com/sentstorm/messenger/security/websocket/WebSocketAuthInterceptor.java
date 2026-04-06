package com.sentstorm.messenger.security.websocket;

import com.sentstorm.messenger.core.repository.chat.ChatParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final ChatParticipantRepository chatParticipantRepository;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            List<String> authHeaders = accessor.getNativeHeader("Authorization");

            if (authHeaders == null || authHeaders.isEmpty()) {
                throw new RuntimeException("Missing Authorization header");
            }

            String token = authHeaders.getFirst().replace("Bearer ", "");

            Jwt jwt = jwtDecoder.decode(token);
            String userId = jwt.getSubject();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of()
                    );

            accessor.setUser(auth);
            Objects.requireNonNull(accessor.getSessionAttributes()).put("user", auth);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {

            if (accessor.getUser() == null) {
                Object sessionUser = Objects.requireNonNull(accessor.getSessionAttributes()).get("user");

                if (sessionUser instanceof UsernamePasswordAuthenticationToken auth) {
                    accessor.setUser(auth);
                } else {
                    throw new RuntimeException("User not authenticated");
                }
            }

            String destination = accessor.getDestination();

            if (destination != null && destination.startsWith("/topic/chats/")) {

                String chatIdStr = destination
                        .replace("/topic/chats/", "")
                        .split("/")[0];

                UUID chatId = UUID.fromString(chatIdStr);

                String userId = accessor.getUser().getName();
                UUID keycloakId = UUID.fromString(userId);

                boolean isParticipant = chatParticipantRepository
                        .existsByChatIdAndUser_KeycloakId(chatId, keycloakId);

                if (!isParticipant) {
                    throw new RuntimeException("Access denied");
                }
            }
        }

        return message;
    }
}