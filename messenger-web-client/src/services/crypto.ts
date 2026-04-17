import nacl from 'tweetnacl'
import util from 'tweetnacl-util'

export interface KeyPair {
  publicKey: string
  privateKey: string
}

export interface EncryptedMessage {
  ciphertext: string
  nonce: string
}

export interface EncryptedMessageForAll {
  ciphertext: string
  senderDeviceId: string
  senderPublicKey: string
}

const DB_NAME = 'messenger_keys'
const STORE_NAME = 'keys'

class CryptoService {
  private keyPair: KeyPair | null = null
  private deviceId: string | null = null

  private async openDB(): Promise<IDBDatabase> {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(DB_NAME)

      request.onerror = () => reject(request.error)

      request.onupgradeneeded = (event) => {
        const db = (event.target as IDBOpenDBRequest).result
        if (!db.objectStoreNames.contains(STORE_NAME)) {
          db.createObjectStore(STORE_NAME)
        }
      }

      request.onsuccess = () => {
        const db = request.result
        if (!db.objectStoreNames.contains(STORE_NAME)) {
          db.close()
          const upgradeRequest = indexedDB.open(DB_NAME, (db.version || 0) + 1)

          upgradeRequest.onupgradeneeded = (event) => {
            const upgradedDB = (event.target as IDBOpenDBRequest).result
            if (!upgradedDB.objectStoreNames.contains(STORE_NAME)) {
              upgradedDB.createObjectStore(STORE_NAME)
            }
          }

          upgradeRequest.onsuccess = () => resolve(upgradeRequest.result)
          upgradeRequest.onerror = () => reject(upgradeRequest.error)
        } else {
          resolve(db)
        }
      }
    })
  }

  generateKeyPair(): KeyPair {
    const keyPair = nacl.box.keyPair()
    return {
      publicKey: util.encodeBase64(keyPair.publicKey),
      privateKey: util.encodeBase64(keyPair.secretKey)
    }
  }

  getDeviceId(): string {
    let deviceId = localStorage.getItem('deviceId')
    if (!deviceId) {
      deviceId = crypto.randomUUID()
      localStorage.setItem('deviceId', deviceId)
    }
    this.deviceId = deviceId
    return deviceId
  }

  async saveKeys(keyPair: KeyPair): Promise<void> {
    this.keyPair = keyPair

    const db = await this.openDB()

    return new Promise((resolve, reject) => {
      const transaction = db.transaction(STORE_NAME, 'readwrite')
      const store = transaction.objectStore(STORE_NAME)

      store.put(keyPair.privateKey, 'privateKey')
      store.put(keyPair.publicKey, 'publicKey')

      transaction.oncomplete = () => {
        db.close()
        resolve()
      }

      transaction.onerror = () => {
        db.close()
        reject(transaction.error)
      }
    })
  }

  async loadKeys(): Promise<KeyPair | null> {
    if (this.keyPair) {
      return this.keyPair
    }

    try {
      const db = await this.openDB()

      return new Promise((resolve, reject) => {
        const transaction = db.transaction(STORE_NAME, 'readonly')
        const store = transaction.objectStore(STORE_NAME)

        const privateKeyReq = store.get('privateKey')
        const publicKeyReq = store.get('publicKey')

        let privateKey: string | null = null
        let publicKey: string | null = null

        privateKeyReq.onsuccess = () => { privateKey = privateKeyReq.result }
        publicKeyReq.onsuccess = () => { publicKey = publicKeyReq.result }

        transaction.oncomplete = () => {
          db.close()
          if (privateKey && publicKey) {
            this.keyPair = { privateKey, publicKey }
            resolve(this.keyPair)
          } else {
            resolve(null)
          }
        }

        transaction.onerror = () => {
          db.close()
          reject(transaction.error)
        }
      })
    } catch {
      return null
    }
  }

  async getOrCreateKeys(): Promise<KeyPair> {
    let keys = await this.loadKeys()
    if (!keys) {
      keys = this.generateKeyPair()
      await this.saveKeys(keys)
    }
    return keys
  }

  encryptForRecipient(message: string, recipientPublicKey: string): EncryptedMessage {
    if (!this.keyPair) throw new Error('Keys not initialized')

    const recipientPubKey = util.decodeBase64(recipientPublicKey)
    const senderPrivKey = util.decodeBase64(this.keyPair.privateKey)
    const nonce = nacl.randomBytes(nacl.box.nonceLength)
    const messageUint8 = util.decodeUTF8(message)
    const ciphertext = nacl.box(messageUint8, nonce, recipientPubKey, senderPrivKey)

    return {
      ciphertext: util.encodeBase64(ciphertext),
      nonce: util.encodeBase64(nonce)
    }
  }

  decryptMessage(encrypted: EncryptedMessage, senderPublicKey: string): string {
    if (!this.keyPair) throw new Error('Keys not initialized')

    const senderPubKey = util.decodeBase64(senderPublicKey)
    const recipientPrivKey = util.decodeBase64(this.keyPair.privateKey)
    const nonce = util.decodeBase64(encrypted.nonce)
    const ciphertext = util.decodeBase64(encrypted.ciphertext)

    const decrypted = nacl.box.open(ciphertext, nonce, senderPubKey, recipientPrivKey)
    if (!decrypted) throw new Error('Failed to decrypt - wrong key')

    return util.encodeUTF8(decrypted)
  }

  encryptForAllDevices(
    message: string,
    recipientDevices: Array<{ deviceId: string, publicKey: string }>
  ): string {
    const senderPublicKey = this.keyPair?.publicKey
    const senderDeviceId = this.deviceId
    const encryptedForDevices: Record<string, EncryptedMessage> = {}

    for (const device of recipientDevices) {
      encryptedForDevices[device.deviceId] = this.encryptForRecipient(message, device.publicKey)
    }

    if (senderPublicKey && senderDeviceId) {
      encryptedForDevices[senderDeviceId] = this.encryptForRecipient(message, senderPublicKey)
    }

    return JSON.stringify({
      ciphertext: JSON.stringify(encryptedForDevices),
      senderDeviceId: senderDeviceId || '',
      senderPublicKey: senderPublicKey || ''
    })
  }

  decryptFromSender(encryptedPayload: string): string {
    const payload: EncryptedMessageForAll = JSON.parse(encryptedPayload)
    const encryptedForDevices: Record<string, EncryptedMessage> = JSON.parse(payload.ciphertext)

    const myDeviceId = this.deviceId

    if (myDeviceId && encryptedForDevices[myDeviceId]) {
      try {
        return this.decryptMessage(encryptedForDevices[myDeviceId], payload.senderPublicKey)
      } catch {
        // ignore
      }
    }

    for (const encrypted of Object.values(encryptedForDevices)) {
      try {
        return this.decryptMessage(encrypted, payload.senderPublicKey)
      } catch {
        // ignore
      }
    }

    throw new Error('Cannot decrypt message')
  }

  async clearKeys(): Promise<void> {
    this.keyPair = null
    try {
      const db = await this.openDB()
      const transaction = db.transaction(STORE_NAME, 'readwrite')
      const store = transaction.objectStore(STORE_NAME)
      store.clear()
      db.close()
    } catch {
      // ignore
    }
  }

  getPublicKey(): string | null {
    return this.keyPair?.publicKey || null
  }
}

export const cryptoService = new CryptoService()
