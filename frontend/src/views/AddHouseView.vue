<script setup>
import { reactive, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import LocationPickerMap from '../components/LocationPickerMap.vue'
import { buildOpenStreetMapUrl, normalizeApiError } from '../lib/api'
import { useSessionStore } from '../stores/session'

const router = useRouter()
const { api } = useSessionStore()

function optionalCoordinate(value) {
  const numericValue = Number(value)
  return Number.isFinite(numericValue) ? numericValue : undefined
}

const loading = reactive({
  create: false,
})

const feedback = reactive({
  create: '',
})

const form = reactive({
  name: '',
  description: '',
  city: '',
  state: '',
  country: 'Mexico',
  latitude: null,
  longitude: null,
  urlMap: '',
})

watch(
  () => [form.latitude, form.longitude],
  ([latitude, longitude], [previousLatitude, previousLongitude]) => {
    const previousUrl = buildOpenStreetMapUrl(previousLatitude, previousLongitude)

    const nextUrl = buildOpenStreetMapUrl(latitude, longitude)

    if (!nextUrl) {
      if (form.urlMap === previousUrl) {
        form.urlMap = ''
      }
      return
    }

    if (!form.urlMap || form.urlMap === previousUrl) {
      form.urlMap = nextUrl
    }
  },
)

async function submit() {
  loading.create = true
  feedback.create = ''

  try {
    await api.createHouse({
      name: form.name,
      description: form.description || undefined,
      city: form.city || undefined,
      state: form.state || undefined,
      country: form.country || undefined,
      latitude: optionalCoordinate(form.latitude),
      longitude: optionalCoordinate(form.longitude),
      urlMap: form.urlMap || undefined,
    })

    await router.push({ name: 'spaces-houses' })
  } catch (error) {
    feedback.create = normalizeApiError(error).message
  } finally {
    loading.create = false
  }
}
</script>

<template lang="pug">
section.page-section
  article.panel-card.panel-card--accent-success
    p.panel-card__eyebrow Alta
    h3.panel-card__title Registrar casa
    p.panel-card__text
      | Crea una casa nueva para que después puedas asignarle locaciones y objetos.

  article.panel-card
    .panel-card__header
      div
        p.panel-card__eyebrow Formulario
        h3.panel-card__title Datos generales
      RouterLink.split-button.split-button--secondary(:to="{ name: 'spaces-houses' }")
        span.split-button__icon <
        span.split-button__text Volver a casas

    form.form-grid(@submit.prevent="submit")
      input.form-input(v-model="form.name" placeholder="Nombre de la casa" required)
      textarea.form-input.form-textarea(v-model="form.description" placeholder="Descripcion breve")
      .form-row
        input.form-input(v-model="form.city" placeholder="Ciudad")
        input.form-input(v-model="form.state" placeholder="Estado")
      input.form-input(v-model="form.country" placeholder="Pais")
      .form-row
        input.form-input(v-model.number="form.latitude" type="number" step="any" placeholder="Latitud")
        input.form-input(v-model.number="form.longitude" type="number" step="any" placeholder="Longitud")
      LocationPickerMap(
        v-model:latitude="form.latitude"
        v-model:longitude="form.longitude"
        title="Ubicacion de la casa"
      )
      input.form-input(v-model="form.urlMap" placeholder="URL de Google Maps u OpenStreetMap")
      p.muted-copy Puedes pegar una URL externa o dejar la sugerida por OpenStreetMap.
      button.primary-button.primary-button--success(type="submit" :disabled="loading.create") Guardar casa
      p.form-feedback.form-feedback--error(v-if="feedback.create") {{ feedback.create }}
</template>
