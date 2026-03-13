<script setup>
import { computed, reactive } from 'vue'

import { useSessionStore } from '../stores/session'

const { state, reauthenticate, closeSessionModalAndGoToLogin } = useSessionStore()

const form = reactive({
  username: state.username || '',
  password: '',
})

const expiredSeconds = computed(() => {
  if (!state.sessionExpiredAt) {
    return 0
  }
  return Math.max(0, Math.floor((state.now - state.sessionExpiredAt) / 1000))
})

async function submit() {
  await reauthenticate({
    username: form.username,
    password: form.password,
  })
  form.password = ''
}
</script>

<template lang="pug">
Teleport(to="body")
  .session-modal(v-if="state.showSessionModal")
    .session-modal__backdrop
    .session-modal__card
      p.panel-card__eyebrow Sesion
      h2.session-modal__title Iniciar sesion para continuar
      p.session-modal__text
        | Tu sesion expiro. Puedes reingresar aqui mismo sin perder el contexto actual.
      p.muted-copy Han pasado {{ expiredSeconds }} segundos desde que vencio el token.

      form.form-grid(@submit.prevent="submit")
        input.form-input(v-model="form.username" placeholder="username" required)
        input.form-input(v-model="form.password" type="password" placeholder="password" required)
        .button-row
          button.primary-button(type="submit" :disabled="state.reauthLoading") Reautenticar
          button.ghost-button(type="button" @click="closeSessionModalAndGoToLogin") Ir a login
        p.form-feedback.form-feedback--error(v-if="state.error") {{ state.error }}
</template>
