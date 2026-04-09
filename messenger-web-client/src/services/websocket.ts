import SockJS from 'sockjs-client'
import { Client, Frame } from 'stompjs'
import type { Message as StompMessage } from 'stompjs'
import keycloak from '@/auth/keycloak'
import { useMessageStore } from '@/stores/message'
import { useChatStore } from '@/stores/chat'

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
      // @ts-ignore - Stomp добавляется через CDN или глобально
      this.stompClient = window.Stomp.over(socket)

      // Отключаем логирование
      this.stompClient.debug = () => {}

      const headers = {
        Authorization: `Bearer ${keycloak.token}`
      }

      this.stompClient.connect(
        headers,
        (frame: Frame) => {
          console.log('WebSocket connected:', frame)
          this.connected = true

          // Переподписываемся на активный чат, если он есть
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

    // Отписываемся от предыдущих подписок
    this.unsubscribeFromChat(chatId)

    // Подписываемся на сообщения
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

    // Подписываемся на статусы сообщений
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

    this.subscriptions.set(`${chatId}-messages`, messageSub)
    this.subscriptions.set(`${chatId}-status`, statusSub)
  }

  unsubscribeFromChat(chatId: string) {
    const messageSub = this.subscriptions.get(`${chatId}-messages`)
    const statusSub = this.subscriptions.get(`${chatId}-status`)

    if (messageSub) {
      messageSub.unsubscribe()
      this.subscriptions.delete(`${chatId}-messages`)
    }

    if (statusSub) {
      statusSub.unsubscribe()
      this.subscriptions.delete(`${chatId}-status`)
    }
  }

  private handleIncomingMessage(chatId: string, message: any) {
    const messageStore = useMessageStore()
    const chatStore = useChatStore()

    // Добавляем сообщение в хранилище, только если мы в этом чате
    if (chatStore.activeChatId === chatId) {
      messageStore.addMessage(message)
    }

    // Обновляем последнее сообщение в списке чатов
    chatStore.updateLastMessage(chatId, message)
  }

  private handleMessageStatus(chatId: string, status: any) {
    const messageStore = useMessageStore()
    messageStore.updateMessageStatus(status.messageId, status.status)
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
