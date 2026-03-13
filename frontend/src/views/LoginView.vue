<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { normalizeApiError } from '../lib/api'
import { useSessionStore } from '../stores/session'

const route = useRoute()
const router = useRouter()
const { api, isAuthenticated, login, state } = useSessionStore()

const loading = reactive({
  publicInfo: false,
  login: false,
})

const feedback = reactive({
  publicInfo: '',
  login: '',
})

const health = ref(null)
const hello = ref(null)

const loginForm = reactive({
  username: '',
  password: '',
})

async function loadPublicInfo() {
  loading.publicInfo = true
  feedback.publicInfo = ''

  try {
    const [healthResponse, helloResponse] = await Promise.all([api.health(), api.hello()])
    health.value = healthResponse
    hello.value = helloResponse
  } catch (error) {
    feedback.publicInfo = normalizeApiError(error).message
  } finally {
    loading.publicInfo = false
  }
}

async function submitLogin() {
  loading.login = true
  feedback.login = ''

  try {
    await login(loginForm)
    loginForm.password = ''

    const redirectTo =
      typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
        ? route.query.redirect
        : '/'

    router.push(redirectTo)
  } catch (error) {
    feedback.login = error.message
  } finally {
    loading.login = false
  }
}

onMounted(async () => {
  await loadPublicInfo()

  if (isAuthenticated.value) {
    router.replace('/')
  }
})
</script>

<template lang="pug">
section.auth-shell
  .auth-card
    .auth-card__copy
      p.panel-card__eyebrow HouseDB
      h1.auth-card__title Accede primero para consultar la informacion
      p.auth-card__text
        | El OpenAPI define `bearerAuth` como seguridad global. Solo `health`, `hello` y autenticacion son publicos.

      .status-grid
        article.status-card
          p.section-label Health
          p.muted-copy(v-if="loading.publicInfo") Consultando...
          p(v-else-if="health") {{ health.status }} · {{ health.timestamp }}
        article.status-card
          p.section-label Version
          p.muted-copy(v-if="loading.publicInfo") Consultando...
          p(v-else-if="hello") {{ hello.version }} · {{ hello.message }}

      p.form-feedback.form-feedback--error(v-if="feedback.publicInfo") {{ feedback.publicInfo }}

    form.auth-form(@submit.prevent="submitLogin")
      p.section-label Login JWT
      input.form-input(v-model="loginForm.username" placeholder="username" required)
      input.form-input(v-model="loginForm.password" type="password" placeholder="password" required)
      button.primary-button(type="submit" :disabled="loading.login") Iniciar sesion
      p.form-feedback.form-feedback--error(v-if="feedback.login || state.error") {{ feedback.login || state.error }}
</template>
