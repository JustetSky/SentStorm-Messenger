import api from '@/api/api'
import { cryptoService } from './crypto'

class DeviceService {
  private registered: boolean = false

  async registerDevice(): Promise<void> {
    if (this.registered) return

    const deviceId = cryptoService.getDeviceId()
    const keys = await cryptoService.getOrCreateKeys()

    try {
      await api.post('/devices', { deviceId, publicKey: keys.publicKey })
      this.registered = true
    } catch (error: any) {
      if (error.response?.status === 409) {
        this.registered = true
      }
    }
  }

  async getRecipientDevices(publicId: string): Promise<Array<{ deviceId: string, publicKey: string }>> {
    const res = await api.get(`/users/${publicId}/devices`)
    return res.data
  }
}

export const deviceService = new DeviceService()
