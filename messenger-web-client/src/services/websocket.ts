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
        (frame: Frame) => {
          console.log('WebSocket connected:', frame)
          this.connected = true

          const chatStore = useChatStore()
          if (chatStore.activeChatId) {
            this.subscribeToChat(chatStore.activeChatId)
          }

          resolve()
        },
        (error: any) => {
          console.error('WebSocket connection error:', error)
          this.connected = false
          this.scheduleReconnect()
          reject(error)
        }
      )
    })
  }

  private scheduleReconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
    }

    this.reconnectTimer = window.setTimeout(() => {
      console.log('Attempting to reconnect WebSocket...')
      this.connect().catch(console.error)
    }, 5000)
  }

  subscribeToChat(chatId: string) {
    if (!this.stompClient?.connected) {
      console.warn('WebSocket not connected, cannot subscribe to chat')
      return
    }

    this.unsubscribeFromChat(chatId)

    const messageSub = this.stompClient.subscribe(
      `/topic/chats/${chatId}`,
      (message: StompMessage) => {
        try {
          const data = JSON.parse(message.body)
          this.handleIncomingMessage(chatId, data)
        } catch (error) {
          console.error('Failed to parse message:', error)
        }
      }
    )

    const statusSub = this.stompClient.subscribe(
      `/topic/chats/${chatId}/status`,
      (message: StompMessage) => {
        try {
          const data = JSON.parse(message.body)
          this.handleMessageStatus(chatId, data)
        } catch (error) {
          console.error('Failed to parse status:', error)
        }
      }
    )

    const deleteSub = this.stompClient.subscribe(
      `/topic/chats/${chatId}/deleted`,
      (message: StompMessage) => {
        try {
          const data = JSON.parse(message.body)
          this.handleMessageDeleted(chatId, data)
        } catch (error) {
          console.error('Failed to parse delete event:', error)
        }
      }
    )

    const lastMessageSub = this.stompClient.subscribe(
      `/topic/chats/${chatId}/last-message`,
      (message: StompMessage) => {
        try {
          const data = message.body ? JSON.parse(message.body) : null
          this.handleLastMessageUpdate(chatId, data)
        } catch (error) {
          console.error('Failed to parse last message update:', error)
        }
      }
    )

    this.subscriptions.set(`${chatId}-messages`, messageSub)
    this.subscriptions.set(`${chatId}-status`, statusSub)
    this.subscriptions.set(`${chatId}-deleted`, deleteSub)
    this.subscriptions.set(`${chatId}-last-message`, lastMessageSub)
  }

  private handleLastMessageUpdate(chatId: string, data: { lastMessageId: string | null }) {
    console.log('📋 Last message update received from server:', data)
    const chatStore = useChatStore()
    const messageStore = useMessageStore()

    const chat = chatStore.chats.find(c => c.chatId === chatId)
    if (!chat) return

    if (data.lastMessageId) {
      chat.lastMessageId = data.lastMessageId

      // Пытаемся найти сообщение в локальном сторе
      const localMessage = messageStore.messages.find(m => m.id === data.lastMessageId)
      if (localMessage) {
        chat.lastMessageCiphertext = localMessage.ciphertext
        chat.lastMessageTime = localMessage.createdDate
        console.log('✅ Updated last message from local store')
      }
      // Если нет в сторе — не запрашиваем с сервера (эндпоинта нет)
    } else {
      chat.lastMessageId = null
      chat.lastMessageCiphertext = null
      chat.lastMessageTime = null
      console.log('✅ Last message cleared (no messages)')
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
    console.log('📨 ========== WEBSOCKET MESSAGE ==========')
    console.log('📨 Raw message:', message)

    const messageStore = useMessageStore()
    const chatStore = useChatStore()

    try {
      if (message.ciphertext && message.ciphertext.startsWith('{') && message.ciphertext.includes('senderDeviceId')) {
        console.log('🔓 Decrypting WebSocket message...')
        const decryptedText = cryptoService.decryptFromSender(message.ciphertext)
        message.ciphertext = decryptedText
        console.log('✅ WebSocket message decrypted')
      }
    } catch (error) {
      console.error('❌ Failed to decrypt WebSocket message:', error)
      message.ciphertext = '[Cannot decrypt]'
    }

    if (chatStore.activeChatId === chatId) {
      messageStore.addMessage(message)
    }

    chatStore.updateLastMessage(chatId, message)
    console.log('📨 ========================================')
  }

  private handleMessageStatus(chatId: string, status: any) {
    const messageStore = useMessageStore()
    messageStore.updateMessageStatus(status.messageId, status.status)
  }

  private handleMessageDeleted(chatId: string, data: { messageId: string }) {
    console.log('🗑️ Message deleted event received:', data)
    const messageStore = useMessageStore()
    const chatStore = useChatStore()

    const chat = chatStore.chats.find(c => c.chatId === chatId)
    const wasLastMessage = chat?.lastMessageId === data.messageId

    messageStore.deleteMessage(data.messageId)

    if (wasLastMessage && chat) {
      // Берём новое последнее сообщение из локального стора
      const remainingMessages = messageStore.messages
      const newLastMessage = remainingMessages.length > 0
        ? remainingMessages[remainingMessages.length - 1]
        : null

      if (newLastMessage) {
        chat.lastMessageId = newLastMessage.id
        chat.lastMessageCiphertext = newLastMessage.ciphertext
        chat.lastMessageTime = newLastMessage.createdDate
        console.log('✅ Updated last message from local store:', newLastMessage.id)
      } else {
        chat.lastMessageId = null
        chat.lastMessageCiphertext = null
        chat.lastMessageTime = null
        console.log('✅ No messages left in chat')
      }
    }
  }

  disconnect() {
    if (this.stompClient?.connected) {
      this.stompClient.disconnect(() => {
        console.log('WebSocket disconnected')
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
