<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import heroImage from '../assets/hero.png'
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
  loginCode: '',
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
  feedback.loginCode = ''

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
    feedback.loginCode = error.detail?.code ?? ''
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
  .auth-card.auth-card--sb.animate__animated.animate__fadeIn
    .auth-card__media(:style="{ backgroundImage: `url(${heroImage})` }")
      .auth-card__media-overlay
        p.panel-card__eyebrow HouseDB
        h1.auth-card__title Welcome Back!
        p.auth-card__text
          | Accede con tu usuario para consultar casas, inventario y usuarios desde la interfaz principal.

        .status-grid.auth-status-grid
          article.status-card.status-card--glass
            p.section-label Health
            p.muted-copy(v-if="loading.publicInfo") Consultando...
            p(v-else-if="health") {{ health.status }} · {{ health.timestamp }}
            p.muted-copy(v-else) Sin respuesta
          article.status-card.status-card--glass
            p.section-label Version
            p.muted-copy(v-if="loading.publicInfo") Consultando...
            p(v-else-if="hello") {{ hello.version }} · {{ hello.message }}
            p.muted-copy(v-else) Sin respuesta

        p.form-feedback.form-feedback--error(v-if="feedback.publicInfo") {{ feedback.publicInfo }}

    .auth-card__panel
      .auth-card__panel-header
        p.section-label Login JWT
        h2.auth-card__form-title Inicia sesion en HouseDB
        p.muted-copy
          | Se usa la autenticacion actual del backend. Solo cambiamos la vista.

      form.auth-form.auth-form--sb(@submit.prevent="submitLogin")
        label.auth-field
          span.auth-field__label Usuario
          input.form-input.form-input--pill(
            v-model="loginForm.username"
            autocomplete="username"
            placeholder="Ingresa tu usuario"
            required
          )
        label.auth-field
          span.auth-field__label Password
          input.form-input.form-input--pill(
            v-model="loginForm.password"
            type="password"
            autocomplete="current-password"
            placeholder="Ingresa tu password"
            required
          )

        button.primary-button.primary-button--block.primary-button--pill(type="submit" :disabled="loading.login")
          | {{ loading.login ? 'Ingresando...' : 'Login' }}

        p.form-feedback.form-feedback--error(v-if="feedback.login || state.error") {{ feedback.login || state.error }}
        p.muted-copy(v-if="feedback.loginCode") Codigo backend: {{ feedback.loginCode }}

      .auth-card__footer
        p.muted-copy
          | El OpenAPI define `bearerAuth` como seguridad global. `health`, `hello` y autenticacion siguen siendo publicos.
</template>
