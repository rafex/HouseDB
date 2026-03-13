import { createApp } from 'vue'
import './styles/main.scss'
import App from './App.vue'
import router from './router'
import { installSessionLifecycle } from './stores/session'

if (import.meta.env.DEV && 'serviceWorker' in navigator) {
  navigator.serviceWorker.getRegistrations().then((registrations) => {
    registrations.forEach((registration) => {
      registration.unregister()
    })
  })
}

installSessionLifecycle(router)

createApp(App).use(router).mount('#app')
