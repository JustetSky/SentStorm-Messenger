import SockJS from 'sockjs-client'
import { Client, Frame } from 'stompjs'
import type { Message as StompMessage } from 'stompjs'
import keycloak from '@/auth/keycloak'
import { useMessageStore } from '@/stores/message'
import { useChatStore } from '@/stores/chat'
import { cryptoService } from "@/services/crypto.ts"

declare global {
  interface Window {
    Stomp: any
  }
}

class WebSocketService {
  private stompClient: any = null
  private connected = false
  private reconnectTimer: number | null = null
  private subscriptions: Map<string, any> = new Map()

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.connected && this.stompClient?.connected) {
        resolve()
        return
      }

      const socket = new SockJS('https://localhost:8443/ws')
      this.stompClient = window.Stomp.over(socket)

      this.stompClient.debug = () => {}

      const headers = {
        Authorization: `Bearer ${keycloak.token}`
      }

      this.stompClient.connect(
        headers,
        () => {
          this.connected = true

          const chatStore = useChatStore()
          if (chatStore.activeChatId) {
            this.subscribeToChat(chatStore.activeChatId)
          }

          resolve()
        },
        () => {
          this.connected = false
          this.scheduleReconnect()
          reject(new Error('WebSocket connection failed'))
        }
      )
    })
  }

  private scheduleReconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
    }

    this.reconnectTimer = window.setTimeout(() => {
      this.connect().catch(() => {})
    }, 5000)
  }

  subscribeToChat(chatId: string) {
    if (!this.stompClient?.connected) {
      return
    }

    this.unsubscribeFromChat(chatId)

    const messageSub = this.stompClient.subscribe(
      `/topic/chats/${chatId}`,
      (message: StompMessage) => {
        try {
          const data = JSON.parse(message.body)
          this.handleIncomingMessage(chatId, data)
        } catch {
          // ignore
        }
      }
    )

    const statusSub = this.stompClient.subscribe(
      `/topic/chats/${chatId}/status`,
      (message: StompMessage) => {
        try {
          const data = JSON.parse(message.body)
          this.handleMessageStatus(chatId, data)
        } catch {
          // ignore
        }
      }
    )

    const deleteSub = this.stompClient.subscribe(
      `/topic/chats/${chatId}/deleted`,
      (message: StompMessage) => {
        try {
          const data = JSON.parse(message.body)
          this.handleMessageDeleted(chatId, data)
        } catch {
          // ignore
        }
      }
    )

    const lastMessageSub = this.stompClient.subscribe(
      `/topic/chats/${chatId}/last-message`,
      (message: StompMessage) => {
        try {
          const data = message.body ? JSON.parse(message.body) : null
          this.handleLastMessageUpdate(chatId, data)
        } catch {
          // ignore
        }
      }
    )

    this.subscriptions.set(`${chatId}-messages`, messageSub)
    this.subscriptions.set(`${chatId}-status`, statusSub)
    this.subscriptions.set(`${chatId}-deleted`, deleteSub)
    this.subscriptions.set(`${chatId}-last-message`, lastMessageSub)
  }

  private handleLastMessageUpdate(chatId: string, data: { lastMessageId: string | null }) {
    const chatStore = useChatStore()
    const messageStore = useMessageStore()

    const chat = chatStore.chats.find(c => c.chatId === chatId)
    if (!chat) return

    if (data.lastMessageId) {
      chat.lastMessageId = data.lastMessageId

      const localMessage = messageStore.messages.find(m => m.id === data.lastMessageId)
      if (localMessage) {
        chat.lastMessageCiphertext = localMessage.ciphertext
        chat.lastMessageTime = localMessage.createdDate
      }
    } else {
      chat.lastMessageId = null
      chat.lastMessageCiphertext = null
      chat.lastMessageTime = null
    }
  }

  unsubscribeFromChat(chatId: string) {
    const messageSub = this.subscriptions.get(`${chatId}-messages`)
    const statusSub = this.subscriptions.get(`${chatId}-status`)
    const deleteSub = this.subscriptions.get(`${chatId}-deleted`)
    const lastMessageSub = this.subscriptions.get(`${chatId}-last-message`)

    if (lastMessageSub) {
      lastMessageSub.unsubscribe()
      this.subscriptions.delete(`${chatId}-last-message`)
    }

    if (messageSub) {
      messageSub.unsubscribe()
      this.subscriptions.delete(`${chatId}-messages`)
    }

    if (statusSub) {
      statusSub.unsubscribe()
      this.subscriptions.delete(`${chatId}-status`)
    }

    if (deleteSub) {
      deleteSub.unsubscribe()
      this.subscriptions.delete(`${chatId}-deleted`)
    }
  }

  private handleIncomingMessage(chatId: string, message: any) {
    const messageStore = useMessageStore()
    const chatStore = useChatStore()

    try {
      if (message.ciphertext && message.ciphertext.startsWith('{') && message.ciphertext.includes('senderDeviceId')) {
        const decryptedText = cryptoService.decryptFromSender(message.ciphertext)
        message.ciphertext = decryptedText
      }
    } catch {
      message.ciphertext = '[Cannot decrypt]'
    }

    if (chatStore.activeChatId === chatId) {
      messageStore.addMessage(message)
    }

    chatStore.updateLastMessage(chatId, message)
  }

  private handleMessageStatus(_chatId: string, status: any) {
    const messageStore = useMessageStore()
    messageStore.updateMessageStatus(status.messageId, status.status)
  }

  private handleMessageDeleted(chatId: string, data: { messageId: string }) {
    const messageStore = useMessageStore()
    const chatStore = useChatStore()

    const chat = chatStore.chats.find(c => c.chatId === chatId)
    const wasLastMessage = chat?.lastMessageId === data.messageId

    messageStore.deleteMessage(data.messageId)

    if (wasLastMessage && chat) {
      const remainingMessages = messageStore.messages
      const newLastMessage = remainingMessages.length > 0
        ? remainingMessages[remainingMessages.length - 1]
        : null

      if (newLastMessage) {
        chat.lastMessageId = newLastMessage.id
        chat.lastMessageCiphertext = newLastMessage.ciphertext
        chat.lastMessageTime = newLastMessage.createdDate
      } else {
        chat.lastMessageId = null
        chat.lastMessageCiphertext = null
        chat.lastMessageTime = null
      }
    }
  }

  disconnect() {
    if (this.stompClient?.connected) {
      this.stompClient.disconnect(() => {
        this.connected = false
      })
    }

    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }

  isConnected(): boolean {
    return this.connected && (this.stompClient?.connected || false)
  }
}

export const webSocketService = new WebSocketService()
