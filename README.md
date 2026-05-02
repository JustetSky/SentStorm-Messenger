# SentStorm — End-to-End Encrypted Messenger

English | [Русский](./README-ru.md)

## Overview
SentStorm is a private messenger with end-to-end (E2E) encryption, where message content is accessible only to conversation participants and remains unreadable by the server.

---

## Key Features
- User registration and authentication via Keycloak
- Search for users and create private chats
- Exchange text messages and emoji
- Send images without storing metadata
- Real-time message delivery via WebSocket
- Chat history retention
- Client-side end-to-end encryption

---

## Interface

### Main Screen (Chat List)
<div style="text-align: center;">
  <img 
    src="docs/main_page.png" 
    alt="Main screen with chat list"
    style="max-width: 600px; width: 100%; height: auto;"
  >
</div>

### Chat Window
<div style="text-align: center;">
  <img 
    src="docs/chat.png" 
    alt="Chat window with messages"
    style="max-width: 600px; width: 100%; height: auto;"
  >
</div>

### User Menu
<div style="text-align: center;">
  <img 
    src="docs/user_menu.png" 
    alt="User dropdown menu"
    style="max-width: 600px; width: 100%; height: auto;"
  >
</div>

### User Profile
<div style="text-align: center;">
  <img 
    src="docs/user_profile.png" 
    alt="User profile window"
    style="max-width: 600px; width: 100%; height: auto;"
  >
</div>

### Contact Profile
<div style="text-align: center;">
  <img 
    src="docs/chat_participant_profile.png" 
    alt="Contact profile window"
    style="max-width: 600px; width: 100%; height: auto;"
  >
</div>

### User Search
<div style="text-align: center;">
  <img 
    src="docs/user_search.png" 
    alt="User search window"
    style="max-width: 600px; width: 100%; height: auto;"
  >
</div>

---

## Architecture

```
             ┌────────────────┐                       ┌────────────────┐
             │     User       │                       │      User      │
             └───────┬────────┘                       └───────┬────────┘
                     │                                        │
                     └──────────────────┬─────────────────────┘
                                        │
                              ┌───────────────────┐
                              │   Vue.js Client   │
                              │  (Web SPA + E2EE) │
                              └─────────┬─────────┘
                                        │
                  ┌─────────────────────┼─────────────────────┐
                  │                     │                     │
             HTTPS (REST)        WSS (WebSocket)         OAuth2/OIDC
                  │                     │                     │
                  ▼                     ▼                     ▼
          ┌─────────────────────────────────────────────────────────────┐
          │                   Spring Boot Backend                       │
          │                                                             │
          │  ┌────────────────────────────────────────────────────────┐ │
          │  │                  Application Layer                     │ │
          │  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐   │ │
          │  │  │   User   │ │   Chat   │ │ Message  │ │  Device  │   │ │
          │  │  │ Service  │ │ Service  │ │ Service  │ │ Service  │   │ │
          │  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘   │ │
          │  │                                                        │ │
          │  │              ┌──────────────────────┐                  │ │
          │  │              │  MessagePublisher    │                  │ │
          │  │              └──────────────────────┘                  │ │
          │  └────────────────────────────────────────────────────────┘ │
          │                                                             │
          │              OAuth2 Resource Server                         │
          │              (Keycloak JWT validation)                      │
          └───────────────────────────────┬─────────────────────────────┘
                                          │
                                          ▼
                       ┌────────────────┐   ┌────────────────┐
                       │   PostgreSQL   │   │    Keycloak    │
                       │ (DB container) │   │ (Auth Server)  │
                       └────────────────┘   └────────────────┘

```

---

## Technology Stack

### Backend
- **Java 21**: Latest stable LTS release
- **Spring Boot 4**: Core framework for building REST and WebSocket applications
- **Spring Web**: REST API implementation for client-server communication
- **Spring WebSocket (STOMP)**: Real-time message and event delivery
- **Spring Data JPA**: ORM layer for database operations and entity management
- **Spring Security + OAuth2 Resource Server**: Keycloak JWT validation and API protection
- **PostgreSQL**: Relational database for users, chats, and encrypted messages
- **Flyway**: Database schema versioning and migrations
- **Lombok**: Boilerplate reduction through automatic code generation

### Frontend
- **Vue.js**: Framework for building the single-page messenger client
- **TypeScript**: Typed language for client-side logic
- **TweetNaCl**: Client-side cryptographic operations (key generation, E2E encryption)
- **WebSocket (STOMP)**: Real-time message and event reception

### Authentication
- **Keycloak**: Identity and access management server
- **OAuth2 / OpenID Connect**: Authentication and authorization protocols
- **JWT**: Access tokens used by the client for API and WebSocket requests

### Infrastructure
- **Gradle**: Build system and dependency management
- **Docker**: Containerization of system components (app, DB, Keycloak)
- **Docker Compose**: Orchestration for running all services in development
- **PostgreSQL (containerized)**: Database deployment in an isolated container
- **Keycloak (containerized)**: Auth server deployment in a container

---

## Data Model

### ERD Diagram
<div style="text-align: center;">
  <img 
    src="docs/erd-diagram.png" 
    alt="ERD Diagram"
    style="max-width: 700px; width: 100%; height: auto;"
  >
</div>

### Core Entities

#### Chat
```java
public class Chat {
   private UUID id;
   private List<Message> messages;
   private List<ChatParticipant> participants;
}
```

#### ChatParticipant
```java
public class ChatParticipant {
   private ChatParticipantId id;
   private Chat chat;
   private User user;
   private Instant joinedDate;
}
```

#### Message
```java
public class Message {
   private UUID id;
   private UUID clientMessageId;
   private Chat chat;
   private User sender;
   private String ciphertext;
   private MessageType type;
   private MessageState state;
}
```

#### User
```java
public class User {
   private UUID id;
   private UUID keycloakId;
   private String email;
   private String firstName;
   private String lastName;
   private String publicId;
   private Instant lastSeen;
   private List<UserDevice> devices;
}
```

#### UserDevice
```java
public class UserDevice {
    private UUID id;
    private User user;
    private String deviceId;
    private String publicKey;
    private Instant createdDate;
    private Instant lastActive;
    private Boolean isActive;
}
```

---

## API Endpoints

### Users
```
GET /users/me              # Get current authenticated user profile
GET /users/{publicId}      # Get public information about a user
GET /users/search          # Search users by publicId to start a new chat
```

### Chats
```
GET    /chats              # Get current user's chat list
POST   /chats              # Create a new private chat with a user
GET    /chats/{chatId}     # Get chat details
DELETE /chats/{chatId}     # Delete a chat
```

### Messages
```
POST   /messages                          # Send an encrypted message to a chat
GET    /chats/{chatId}/messages           # Get message history for a chat
PATCH  /messages/{messageId}/read         # Mark message as read (double blue check)
PATCH  /messages/{messageId}/delivered    # Mark message as delivered (double gray check)
DELETE /messages/{messageId}              # Delete a message
```

### Attachment
```
POST /messages/upload      # Upload an image to attach to a message
```

### Device
```
POST   /devices              # Register a user device and its public key
DELETE /devices/{deviceId}   # Remove a user device from the system
```

### Crypto
```
GET /users/{publicId}/devices   # Get a user's device list and public keys for E2E encryption
```

---

## Security Model

### Authentication
User authentication is handled by Keycloak. 
All protected endpoints require a JWT token in the header:
```
Authorization: Bearer <token>
```

The token is used for:
- User identification
- API access control
- Synchronizing users between Keycloak and the application database

### E2E Encryption
SentStorm implements end-to-end encryption with the following principles:

- Cryptographic keys are generated on the client
- Private keys are never transmitted to the server
- Messages are encrypted on the sender's device
- Decryption occurs only on the recipient's device
- The server stores only encrypted data

The server has no technical capability to read user conversation content. 
Data transmission between client and server is additionally protected by TLS.

---

## Build & Run

### Running the Server

Start all containers (DB + application):
```
docker-compose up -d postgres
```

Build the project:
```
./gradlew clean build
```

Run the application:
```
./gradlew bootRun
```

### Running the Client

Install dependencies:
```
npm install
```

Build the project:
```
npm run build
```

Preview the built project:
```
npm run preview
```

### Default Ports

| Service | URL |
|---------|-----|
| Frontend (Vue.js) | `http://localhost:4200` |
| Backend (Spring Boot) | `https://localhost:8443` |
| Keycloak | `http://localhost:9090` |
| PostgreSQL | `localhost:5432` |

---

## Interaction Flows

### Sending a Message
1. Client retrieves the recipient's public key
2. Message is encrypted on the client
3. Encrypted message is sent to the server via REST
4. Server stores the ciphertext in the database (SENT)
5. Server delivers the message to the recipient
6. On successful delivery, the server updates the message status (DELIVERED)
7. Recipient's client decrypts the message locally

### Receiving a Message
1. If the user is online, the message arrives via WebSocket in real time
2. If the user was offline, the client requests messages after reconnecting
3. Server returns encrypted data
4. Client decrypts it locally
5. When the chat is opened, the client sends a read receipt, and the server updates the message status (READ)

---

## License
This project is licensed under the Apache License 2.0.

See the [LICENSE](LICENSE) file for details or visit: http://www.apache.org/licenses/LICENSE-2.0