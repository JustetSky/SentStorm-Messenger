import api from '@/api/api'
import { cryptoService } from './crypto'

class DeviceService {
  private registered: boolean = false

  async registerDevice(): Promise<void> {
    if (this.registered) return

    console.log('📱 ========== REGISTERING DEVICE ==========')
    const deviceId = cryptoService.getDeviceId()
    const keys = await cryptoService.getOrCreateKeys()

    console.log('📱 DeviceId:', deviceId)
    console.log('🔑 PublicKey:', keys.publicKey.substring(0, 30) + '...')

    try {
      await api.post('/devices', { deviceId, publicKey: keys.publicKey })
      this.registered = true
      console.log('✅ Device registered')
    } catch (error: any) {
      if (error.response?.status === 409) {
        console.log('⚠️ Device already registered')
        this.registered = true
      } else {
        console.error('❌ Failed:', error)
      }
    }
  }

  async getRecipientDevices(publicId: string): Promise<Array<{ deviceId: string, publicKey: string }>> {
    const res = await api.get(`/users/${publicId}/devices`)
    return res.data
  }
}

export const deviceService = new DeviceService()
