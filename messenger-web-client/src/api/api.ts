import axios from 'axios'
import keycloak from '../auth/keycloak'

const api = axios.create({
  baseURL: 'https://localhost:8443'
})

api.interceptors.request.use(async (config) => {
  if (keycloak.token) {
    config.headers.Authorization = `Bearer ${keycloak.token}`
  }
  return config
})

export default api
