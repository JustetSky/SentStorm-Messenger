import { createApp } from 'vue'
import App from './App.vue'
import { initKeycloak } from './auth/initKeycloak'
import { createPinia } from 'pinia'
import router from './router'
import './assets/main.css'

async function bootstrap() {
  await initKeycloak()

  const app = createApp(App)

  app.use(createPinia())
  app.use(router)

  app.mount('#app')
}

bootstrap()
