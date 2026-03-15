<script setup>
import { onMounted, reactive, ref, watch } from 'vue'

import LocationPickerMap from '../components/LocationPickerMap.vue'
import PaginationControls from '../components/PaginationControls.vue'
import {
  buildOpenStreetMapUrl,
  createPaginationState,
  normalizeApiError,
  paginationFromResponse,
} from '../lib/api'
import { useCatalogStore } from '../stores/catalogs'
import { useSessionStore } from '../stores/session'

const { api, isAuthenticated } = useSessionStore()
const { houseOptions, getHouseById, loadHousesCatalog } = useCatalogStore()
const LOCATION_KIND_HELP =
  'El nombre podria ser "Closet principal" y el locationKind algo como "room", "cabinet", "shelf" o "slot".'

function optionalCoordinate(value) {
  const numericValue = Number(value)
  return Number.isFinite(numericValue) ? numericValue : undefined
}

const loading = reactive({
  houses: false,
  locations: false,
  createHouse: false,
  createLocation: false,
})

const feedback = reactive({
  houses: '',
  locations: '',
  createHouse: '',
  createLocation: '',
})

const houses = ref([])
const locations = ref([])
const selectedHouseId = ref('')
const activeHouse = ref(null)

const housesPager = reactive(createPaginationState(10))
const locationsPager = reactive(createPaginationState(16))

const houseForm = reactive({
  name: '',
  description: '',
  city: '',
  state: '',
  country: 'Mexico',
  latitude: null,
  longitude: null,
  urlMap: '',
})

const locationForm = reactive({
  name: '',
  parentHouseLocationId: '',
  locationKind: 'slot',
  isLeaf: true,
  referenceCode: '',
  notes: '',
  latitude: null,
  longitude: null,
})

watch(
  () => [houseForm.latitude, houseForm.longitude],
  ([latitude, longitude], [previousLatitude, previousLongitude]) => {
    const previousUrl = buildOpenStreetMapUrl(previousLatitude, previousLongitude)

    const nextUrl = buildOpenStreetMapUrl(latitude, longitude)

    if (!nextUrl) {
      if (houseForm.urlMap === previousUrl) {
        houseForm.urlMap = ''
      }
      return
    }

    if (!houseForm.urlMap || houseForm.urlMap === previousUrl) {
      houseForm.urlMap = nextUrl
    }
  },
)

async function loadHouses() {
  if (!isAuthenticated.value) {
    houses.value = []
    selectedHouseId.value = ''
    activeHouse.value = null
    return
  }

  loading.houses = true
  feedback.houses = ''

  try {
    await loadHousesCatalog({ force: true })
    const response = await api.listHouses({
      limit: housesPager.limit,
      offset: housesPager.offset,
    })
    houses.value = response.houses ?? []
    Object.assign(housesPager, paginationFromResponse(response, housesPager))

    if (!selectedHouseId.value && houses.value.length > 0) {
      selectedHouseId.value = houses.value[0].houseId
      activeHouse.value = getHouseById(selectedHouseId.value)
      await loadLocations()
    }
  } catch (error) {
    feedback.houses = normalizeApiError(error).message
  } finally {
    loading.houses = false
  }
}

async function loadLocations() {
  if (!selectedHouseId.value) {
    locations.value = []
    return
  }

  loading.locations = true
  feedback.locations = ''

  try {
    activeHouse.value = getHouseById(selectedHouseId.value)
    const response = await api.listHouseLocations(selectedHouseId.value, {
      limit: locationsPager.limit,
      offset: locationsPager.offset,
    })
    locations.value = response.locations ?? []
    Object.assign(locationsPager, paginationFromResponse(response, locationsPager))
  } catch (error) {
    feedback.locations = normalizeApiError(error).message
  } finally {
    loading.locations = false
  }
}

async function submitHouse() {
  loading.createHouse = true
  feedback.createHouse = ''

  try {
    await api.createHouse({
      name: houseForm.name,
      description: houseForm.description || undefined,
      city: houseForm.city || undefined,
      state: houseForm.state || undefined,
      country: houseForm.country || undefined,
      latitude: optionalCoordinate(houseForm.latitude),
      longitude: optionalCoordinate(houseForm.longitude),
      urlMap: houseForm.urlMap || undefined,
    })
    Object.assign(houseForm, {
      name: '',
      description: '',
      city: '',
      state: '',
      country: 'Mexico',
      latitude: null,
      longitude: null,
      urlMap: '',
    })
    await loadHouses()
    feedback.createHouse = 'Casa registrada.'
  } catch (error) {
    feedback.createHouse = normalizeApiError(error).message
  } finally {
    loading.createHouse = false
  }
}

async function submitLocation() {
  if (!selectedHouseId.value) {
    feedback.createLocation = 'Selecciona una casa primero.'
    return
  }

  loading.createLocation = true
  feedback.createLocation = ''

  try {
    await api.createHouseLocation(selectedHouseId.value, {
      name: locationForm.name,
      parentHouseLocationId: locationForm.parentHouseLocationId || undefined,
      locationKind: locationForm.locationKind,
      isLeaf: locationForm.isLeaf,
      referenceCode: locationForm.referenceCode || undefined,
      notes: locationForm.notes || undefined,
      latitude: optionalCoordinate(locationForm.latitude),
      longitude: optionalCoordinate(locationForm.longitude),
    })
    Object.assign(locationForm, {
      name: '',
      parentHouseLocationId: '',
      locationKind: 'slot',
      isLeaf: true,
      referenceCode: '',
      notes: '',
      latitude: null,
      longitude: null,
    })
    locationsPager.offset = 0
    await loadLocations()
    feedback.createLocation = 'Locacion registrada.'
  } catch (error) {
    feedback.createLocation = normalizeApiError(error).message
  } finally {
    loading.createLocation = false
  }
}

onMounted(loadHouses)
watch(isAuthenticated, loadHouses)
watch(() => housesPager.offset, loadHouses)
watch(() => housesPager.limit, () => {
  housesPager.offset = 0
  loadHouses()
})
watch(() => locationsPager.offset, () => {
  if (selectedHouseId.value) {
    loadLocations()
  }
})
watch(() => locationsPager.limit, () => {
  locationsPager.offset = 0
  if (selectedHouseId.value) {
    loadLocations()
  }
})
</script>

<template lang="pug">
section.page-section
  article.panel-card
    .panel-card__header
      div
        p.panel-card__eyebrow Contexto
        h3.panel-card__title Espacios fisicos donde viven tus objetos
      button.ghost-button(type="button" @click="loadHouses" :disabled="loading.houses") Recargar

    .split-grid
      .list-panel
        p.section-label Casa activa
        select.form-input(v-model="selectedHouseId" @change="locationsPager.offset = 0; loadLocations()")
          option(value="") Selecciona una casa
          option(v-for="option in houseOptions" :key="option.value" :value="option.value")
            | {{ option.label }}
        .summary-card(v-if="activeHouse")
          strong {{ activeHouse.name }}
          span {{ activeHouse.description || 'Sin descripcion' }}
          span {{ activeHouse.city || 'Sin ciudad' }} · {{ activeHouse.state || 'Sin estado' }} · {{ activeHouse.country || 'Sin pais' }}
        ul.selection-list(v-if="houses.length > 0")
          li(v-for="house in houses" :key="house.houseId" @click="selectedHouseId = house.houseId; locationsPager.offset = 0; loadLocations()")
            strong {{ house.name }}
            span {{ house.city || 'Sin ciudad' }} · {{ house.role }}
        p.form-feedback.form-feedback--error(v-if="feedback.houses") {{ feedback.houses }}
        PaginationControls(
          :count="housesPager.returned"
          :limit="housesPager.limit"
          :offset="housesPager.offset"
          :hasMore="housesPager.hasMore"
          :previousOffset="housesPager.previousOffset"
          :nextOffset="housesPager.nextOffset"
          :loading="loading.houses"
          @change="housesPager.offset = $event"
          @limit-change="housesPager.limit = $event"
        )

      form.form-grid(@submit.prevent="submitHouse")
        p.section-label Registrar una casa
        input.form-input(v-model="houseForm.name" placeholder="Nombre de la casa" required)
        input.form-input(v-model="houseForm.description" placeholder="Descripcion breve")
        .form-row
          input.form-input(v-model="houseForm.city" placeholder="Ciudad")
          input.form-input(v-model="houseForm.state" placeholder="Estado")
        input.form-input(v-model="houseForm.country" placeholder="Pais")
        .form-row
          input.form-input(v-model.number="houseForm.latitude" type="number" step="any" placeholder="Latitud")
          input.form-input(v-model.number="houseForm.longitude" type="number" step="any" placeholder="Longitud")
        LocationPickerMap(
          v-model:latitude="houseForm.latitude"
          v-model:longitude="houseForm.longitude"
          title="Ubicacion de la casa"
        )
        input.form-input(v-model="houseForm.urlMap" placeholder="URL de Google Maps u OpenStreetMap")
        p.muted-copy Puedes pegar una URL manual o usar la sugerida desde el punto seleccionado.
        button.primary-button(type="submit" :disabled="loading.createHouse") Guardar casa
        p.form-feedback(v-if="feedback.createHouse") {{ feedback.createHouse }}

  article.panel-card
    .panel-card__header
      div
        p.panel-card__eyebrow Ubicaciones
        h3.panel-card__title Estructura de espacios para encontrar objetos

    p.muted-copy(v-if="!selectedHouseId") Selecciona una casa para ver sus locaciones.
    template(v-else)
      table.data-table(v-if="locations.length > 0")
        thead
          tr
            th Nombre
            th(:title="LOCATION_KIND_HELP") Tipo
            th Ruta
            th Hoja
        tbody
          tr(v-for="location in locations" :key="location.houseLocationId")
            td {{ location.name }}
            td {{ location.locationKind }}
            td {{ location.path || 'Sin ruta' }}
            td {{ location.isLeaf ? 'Si' : 'No' }}
      p.muted-copy(v-else-if="loading.locations") Cargando locaciones...
      p.muted-copy(v-else) Aun no hay locaciones registradas.
      p.form-feedback.form-feedback--error(v-if="feedback.locations") {{ feedback.locations }}
      PaginationControls(
        :count="locationsPager.returned"
        :limit="locationsPager.limit"
        :offset="locationsPager.offset"
        :hasMore="locationsPager.hasMore"
        :previousOffset="locationsPager.previousOffset"
        :nextOffset="locationsPager.nextOffset"
        :loading="loading.locations"
        @change="locationsPager.offset = $event"
        @limit-change="locationsPager.limit = $event"
      )

      form.form-grid.form-grid--compact(@submit.prevent="submitLocation")
        p.section-label Crear nueva locacion
        input.form-input(v-model="locationForm.name" placeholder="Nombre de la locacion" required)
        select.form-input(v-model="locationForm.parentHouseLocationId")
          option(value="") Sin locacion padre
          option(v-for="location in locations" :key="location.houseLocationId" :value="location.houseLocationId")
            | {{ location.path || location.name }}
        .form-row
          input.form-input(v-model="locationForm.locationKind" placeholder="locationKind" :title="LOCATION_KIND_HELP")
          input.form-input(v-model="locationForm.referenceCode" placeholder="referenceCode")
        p.muted-copy(:title="LOCATION_KIND_HELP") {{ LOCATION_KIND_HELP }}
        label.toggle-field
          input(type="checkbox" v-model="locationForm.isLeaf")
          span Es locacion hoja
        .form-row
          input.form-input(v-model.number="locationForm.latitude" type="number" step="any" placeholder="Latitud")
          input.form-input(v-model.number="locationForm.longitude" type="number" step="any" placeholder="Longitud")
        LocationPickerMap(
          v-model:latitude="locationForm.latitude"
          v-model:longitude="locationForm.longitude"
          title="Ubicacion de la locacion"
        )
        textarea.form-input.form-textarea(v-model="locationForm.notes" placeholder="Notas")
        button.primary-button(type="submit" :disabled="loading.createLocation") Guardar locacion
        p.form-feedback(v-if="feedback.createLocation") {{ feedback.createLocation }}
</template>
