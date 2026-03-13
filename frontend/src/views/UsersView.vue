<script setup>
import { reactive, ref } from 'vue'

import { normalizeApiError } from '../lib/api'
import { useSessionStore } from '../stores/session'

const { api, isAuthenticated } = useSessionStore()

const loading = reactive({
  token: false,
  createUser: false,
})

const feedback = reactive({
  token: '',
  createUser: '',
})

const tokenPreview = ref(null)

const tokenForm = reactive({
  client_id: '',
  client_secret: '',
  grant_type: 'client_credentials',
})

const userForm = reactive({
  userId: '',
  username: '',
  password: '',
})

const endpointCatalog = [
  'GET /health',
  'GET /hello',
  'POST /auth/login',
  'POST /auth/refresh',
  'POST /auth/token',
  'GET /items',
  'GET /items/search',
  'GET /items/nearby',
  'GET /items/by-location',
  'GET /items/{inventoryItemId}',
  'PATCH /items/{inventoryItemId}/move',
  'GET /items/{inventoryItemId}/timeline',
  'PUT /items/{inventoryItemId}/favorite',
  'GET /houses',
  'POST /houses',
  'GET /houses/ids',
  'GET|POST|PUT /houses/{houseId}/members',
  'POST /houses/{houseId}/locations',
  'POST /users',
]

async function requestClientToken() {
  loading.token = true
  feedback.token = ''

  try {
    tokenPreview.value = await api.clientToken(tokenForm)
    feedback.token = 'Token M2M recibido.'
  } catch (error) {
    feedback.token = normalizeApiError(error).message
  } finally {
    loading.token = false
  }
}

async function createUser() {
  loading.createUser = true
  feedback.createUser = ''

  try {
    const response = await api.createUser({
      userId: userForm.userId || undefined,
      username: userForm.username,
      password: userForm.password,
    })

    feedback.createUser = `Usuario creado: ${response.userId}`
    userForm.userId = ''
    userForm.username = ''
    userForm.password = ''
  } catch (error) {
    feedback.createUser = normalizeApiError(error).message
  } finally {
    loading.createUser = false
  }
}
</script>

<template lang="pug">
section.page-section
  article.panel-card
    .panel-card__header
      div
        p.panel-card__eyebrow Tokens
        h3.panel-card__title Operaciones de autenticacion avanzada

    .split-grid
      .form-stack
        form.form-grid.form-grid--compact(@submit.prevent="requestClientToken")
          p.section-label Token M2M
          input.form-input(v-model="tokenForm.client_id" placeholder="client_id" required)
          input.form-input(v-model="tokenForm.client_secret" type="password" placeholder="client_secret" required)
          button.ghost-button(type="submit" :disabled="loading.token") Solicitar token
          p.form-feedback(v-if="feedback.token") {{ feedback.token }}
          pre.code-block(v-if="tokenPreview") {{ JSON.stringify(tokenPreview, null, 2) }}

  article.panel-card
    .panel-card__header
      div
        p.panel-card__eyebrow Administracion
        h3.panel-card__title Alta de usuarios y revision de endpoints protegidos

    .split-grid
      form.form-grid(@submit.prevent="createUser")
        p.section-label Crear usuario
        input.form-input(v-model="userForm.userId" placeholder="userId opcional")
        input.form-input(v-model="userForm.username" placeholder="username" required)
        input.form-input(v-model="userForm.password" type="password" placeholder="password" required)
        button.primary-button(type="submit" :disabled="loading.createUser || !isAuthenticated") Crear usuario
        p.form-feedback(v-if="feedback.createUser") {{ feedback.createUser }}
        p.muted-copy Requiere token app o un usuario con privilegios ADMIN.

      .list-panel
        p.section-label Superficie de API revisada
        ul.compact-list
          li(v-for="endpoint in endpointCatalog" :key="endpoint")
            strong {{ endpoint }}
</template>
