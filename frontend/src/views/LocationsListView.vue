<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import DetailModal from '../components/DetailModal.vue'
import { normalizeApiError } from '../lib/api'
import { useCatalogStore } from '../stores/catalogs'
import { useSessionStore } from '../stores/session'

const route = useRoute()
const router = useRouter()
const { api, isAuthenticated } = useSessionStore()
const { houseOptions, getHouseById, loadHousesCatalog } = useCatalogStore()

const loading = reactive({
  houses: false,
  locations: false,
})

const feedback = reactive({
  houses: '',
  locations: '',
})

const filters = reactive({
  houseId: '',
  q: '',
})

const locations = ref([])
const selectedLocation = ref(null)

const filteredLocations = computed(() => {
  const query = filters.q.trim().toLowerCase()
  if (!query) {
    return locations.value
  }

  return locations.value.filter((location) =>
    [location.name, location.locationKind, location.path, location.referenceCode, location.notes]
      .filter(Boolean)
      .some((value) => value.toLowerCase().includes(query)),
  )
})

const activeHouse = computed(() => getHouseById(filters.houseId))

function syncQuery() {
  router.replace({
    name: 'spaces-locations',
    query: {
      ...(filters.houseId ? { houseId: filters.houseId } : {}),
    },
  })
}

async function ensureHouseContext() {
  loading.houses = true
  feedback.houses = ''

  try {
    await loadHousesCatalog({ force: true })

    if (!filters.houseId && typeof route.query.houseId === 'string') {
      filters.houseId = route.query.houseId
    }

    if (!filters.houseId && houseOptions.value.length > 0) {
      filters.houseId = houseOptions.value[0].value
    }
  } catch (error) {
    feedback.houses = normalizeApiError(error).message
  } finally {
    loading.houses = false
  }
}

async function loadLocations() {
  if (!isAuthenticated.value || !filters.houseId) {
    locations.value = []
    return
  }

  loading.locations = true
  feedback.locations = ''

  try {
    const response = await api.listHouseLocations(filters.houseId, {
      limit: 500,
      offset: 0,
    })
    locations.value = response.locations ?? []
    syncQuery()
  } catch (error) {
    feedback.locations = normalizeApiError(error).message
  } finally {
    loading.locations = false
  }
}

function openLocation(location) {
  selectedLocation.value = location
}

function closeDetail() {
  selectedLocation.value = null
}

onMounted(async () => {
  await ensureHouseContext()
  await loadLocations()
})

watch(
  () => filters.houseId,
  async (value, previousValue) => {
    if (!value || value === previousValue) {
      return
    }

    selectedLocation.value = null
    await loadLocations()
  },
)
</script>

<template lang="pug">
section.page-section
  article.panel-card.panel-card--accent-warning
    p.panel-card__eyebrow Espacios
    h3.panel-card__title Locaciones
    p.panel-card__text
      | Explora la estructura de locaciones por casa y abre un detalle modal para revisar el contexto completo.

  article.panel-card
    .panel-card__header
      div
        p.panel-card__eyebrow Lista
        h3.panel-card__title Locaciones por casa
      .toolbar-actions
        RouterLink.split-button.split-button--warning(:to="{ name: 'add-location', query: filters.houseId ? { houseId: filters.houseId } : {} }")
          span.split-button__icon +
          span.split-button__text Agregar locacion
        button.circle-button.circle-button--secondary(type="button" @click="loadLocations" :disabled="loading.locations || !filters.houseId") R

    .table-toolbar
      .table-toolbar__search
        select.form-input.table-search-select(v-model="filters.houseId")
          option(value="") Selecciona una casa
          option(v-for="option in houseOptions" :key="option.value" :value="option.value")
            | {{ option.label }}
        input.form-input.table-search-input(v-model="filters.q" placeholder="Buscar por nombre, ruta o tipo")

    p.form-feedback.form-feedback--error(v-if="feedback.houses || feedback.locations") {{ feedback.houses || feedback.locations }}
    p.muted-copy(v-if="activeHouse") Mostrando locaciones de {{ activeHouse.name }}.

    .table-card
      .table-card__header
        h4.table-card__title Resultado
        p.muted-copy {{ loading.locations ? 'Consultando...' : `${filteredLocations.length} locaciones` }}
      table.table-grid(v-if="filteredLocations.length > 0")
        thead
          tr
            th Nombre
            th Tipo
            th Ruta
            th Hoja
            th.table-grid__actions Acciones
        tbody
          tr.table-grid__row(v-for="location in filteredLocations" :key="location.houseLocationId" @click="openLocation(location)")
            td
              strong {{ location.name }}
              p.muted-copy {{ location.referenceCode || 'Sin referencia' }}
            td {{ location.locationKind || 'Sin tipo' }}
            td {{ location.path || 'Sin ruta' }}
            td {{ location.isLeaf ? 'Si' : 'No' }}
            td.table-grid__actions(@click.stop)
              button.circle-button.circle-button--info(type="button" @click="openLocation(location)") i
      p.empty-copy(v-else-if="loading.locations || loading.houses") Cargando locaciones...
      p.empty-copy(v-else) Selecciona una casa y agrega locaciones para ver resultados aqui.

  DetailModal(v-if="selectedLocation" :title="selectedLocation.name || 'Locacion'" @close="closeDetail")
    dl.detail-list
      div
        dt Casa
        dd {{ activeHouse?.name || 'Sin casa' }}
      div
        dt Tipo
        dd {{ selectedLocation.locationKind || 'Sin tipo' }}
      div
        dt Ruta
        dd {{ selectedLocation.path || 'Sin ruta' }}
      div
        dt Referencia
        dd {{ selectedLocation.referenceCode || 'Sin referencia' }}
      div
        dt Es hoja
        dd {{ selectedLocation.isLeaf ? 'Si' : 'No' }}
      div
        dt Notas
        dd {{ selectedLocation.notes || 'Sin notas' }}
</template>
