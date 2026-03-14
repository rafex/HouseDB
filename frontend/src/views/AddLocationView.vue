<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import LocationPickerMap from '../components/LocationPickerMap.vue'
import { normalizeApiError } from '../lib/api'
import { useCatalogStore } from '../stores/catalogs'
import { useSessionStore } from '../stores/session'

const route = useRoute()
const router = useRouter()
const { api } = useSessionStore()
const { houseOptions, loadHousesCatalog } = useCatalogStore()
const LOCATION_KIND_HELP =
  'El nombre podria ser "Closet principal" y el locationKind algo como "room", "cabinet", "shelf" o "slot".'

function optionalCoordinate(value) {
  const numericValue = Number(value)
  return Number.isFinite(numericValue) ? numericValue : undefined
}

const loading = reactive({
  create: false,
  locations: false,
})

const feedback = reactive({
  create: '',
  locations: '',
})

const form = reactive({
  houseId: '',
  name: '',
  parentHouseLocationId: '',
  locationKind: 'slot',
  isLeaf: true,
  referenceCode: '',
  notes: '',
  latitude: null,
  longitude: null,
})

const locations = ref([])

const parentOptions = computed(() =>
  locations.value.map((location) => ({
    value: location.houseLocationId,
    label: location.path || location.name,
  })),
)

async function loadLocations() {
  if (!form.houseId) {
    locations.value = []
    form.parentHouseLocationId = ''
    return
  }

  loading.locations = true
  feedback.locations = ''

  try {
    const response = await api.listHouseLocations(form.houseId, {
      limit: 500,
      offset: 0,
    })
    locations.value = response.locations ?? []
  } catch (error) {
    feedback.locations = normalizeApiError(error).message
  } finally {
    loading.locations = false
  }
}

async function submit() {
  loading.create = true
  feedback.create = ''

  try {
    await api.createHouseLocation(form.houseId, {
      name: form.name,
      parentHouseLocationId: form.parentHouseLocationId || undefined,
      locationKind: form.locationKind || undefined,
      isLeaf: form.isLeaf,
      referenceCode: form.referenceCode || undefined,
      notes: form.notes || undefined,
      latitude: optionalCoordinate(form.latitude),
      longitude: optionalCoordinate(form.longitude),
    })

    await router.push({
      name: 'spaces-locations',
      query: {
        houseId: form.houseId,
      },
    })
  } catch (error) {
    feedback.create = normalizeApiError(error).message
  } finally {
    loading.create = false
  }
}

onMounted(async () => {
  await loadHousesCatalog()

  if (typeof route.query.houseId === 'string') {
    form.houseId = route.query.houseId
  } else if (houseOptions.value.length > 0) {
    form.houseId = houseOptions.value[0].value
  }

  await loadLocations()
})

watch(() => form.houseId, loadLocations)
</script>

<template lang="pug">
section.page-section
  article.panel-card.panel-card--accent-info
    p.panel-card__eyebrow Alta
    h3.panel-card__title Registrar locacion
    p.panel-card__text
      | Crea una locacion nueva dentro de una casa para ordenar mejor la búsqueda de objetos.

  article.panel-card
    .panel-card__header
      div
        p.panel-card__eyebrow Formulario
        h3.panel-card__title Datos de la locacion
      RouterLink.split-button.split-button--secondary(:to="{ name: 'spaces-locations', query: form.houseId ? { houseId: form.houseId } : {} }")
        span.split-button__icon <
        span.split-button__text Volver a locaciones

    form.form-grid(@submit.prevent="submit")
      select.form-input(v-model="form.houseId" required)
        option(value="") Selecciona una casa
        option(v-for="option in houseOptions" :key="option.value" :value="option.value")
          | {{ option.label }}
      input.form-input(v-model="form.name" placeholder="Nombre de la locacion" required)
      select.form-input(v-model="form.parentHouseLocationId")
        option(value="") Sin locacion padre
        option(v-for="option in parentOptions" :key="option.value" :value="option.value")
          | {{ option.label }}
      .form-row
        input.form-input(v-model="form.locationKind" placeholder="Tipo de locacion" :title="LOCATION_KIND_HELP")
        input.form-input(v-model="form.referenceCode" placeholder="Referencia")
      p.muted-copy(:title="LOCATION_KIND_HELP") {{ LOCATION_KIND_HELP }}
      label.toggle-field
        input(type="checkbox" v-model="form.isLeaf")
        span Es locacion hoja
      .form-row
        input.form-input(v-model.number="form.latitude" type="number" step="any" placeholder="Latitud")
        input.form-input(v-model.number="form.longitude" type="number" step="any" placeholder="Longitud")
      LocationPickerMap(
        v-model:latitude="form.latitude"
        v-model:longitude="form.longitude"
        title="Ubicacion de la locacion"
      )
      textarea.form-input.form-textarea(v-model="form.notes" placeholder="Notas")
      button.primary-button.primary-button--info(type="submit" :disabled="loading.create || !form.houseId") Guardar locacion
      p.form-feedback.form-feedback--error(v-if="feedback.create || feedback.locations") {{ feedback.create || feedback.locations }}
</template>
