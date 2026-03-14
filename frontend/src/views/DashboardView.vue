<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import MetricCard from '../components/MetricCard.vue'
import { normalizeApiError } from '../lib/api'
import { useCatalogStore } from '../stores/catalogs'
import { useSessionStore } from '../stores/session'

const router = useRouter()
const { api, isAuthenticated } = useSessionStore()
const { houseOptions, loadHousesCatalog } = useCatalogStore()

const loading = reactive({
  summary: false,
  search: false,
  locations: false,
})

const feedback = reactive({
  summary: '',
  search: '',
  locations: '',
})

const stats = reactive({
  houses: 0,
  items: 0,
  itemsWithoutLocation: 0,
  locations: 0,
  recentItems: [],
})

const quickSearch = reactive({
  q: '',
  houseId: '',
})

const quickResults = ref([])
const houseLocations = ref([])
const featuredHouse = ref(null)

const metrics = computed(() => [
  {
    title: 'Objetos visibles',
    value: String(stats.items),
    trend: 'Base actual de cosas registradas',
    accent: 'teal',
  },
  {
    title: 'Casas activas',
    value: String(stats.houses),
    trend: 'Contexto disponible para ubicar',
    accent: 'amber',
  },
  {
    title: 'Ubicaciones',
    value: String(stats.locations),
    trend: 'Locaciones visibles de la casa activa',
    accent: 'info',
  },
  {
    title: 'Sin ubicacion',
    value: String(stats.itemsWithoutLocation),
    trend: 'Objetos que costaria reencontrar',
    accent: 'coral',
  },
])

const activeHouseId = computed(() => quickSearch.houseId || houseOptions.value[0]?.value || '')

async function loadFeaturedLocations(houseId) {
  if (!houseId) {
    featuredHouse.value = null
    houseLocations.value = []
    stats.locations = 0
    return
  }

  loading.locations = true
  feedback.locations = ''

  try {
    const response = await api.listHouseLocations(houseId, {
      limit: 8,
      offset: 0,
    })
    featuredHouse.value = houseOptions.value.find((option) => option.value === houseId)?.house ?? null
    houseLocations.value = response.locations ?? []
    stats.locations = houseLocations.value.length
  } catch (error) {
    feedback.locations = normalizeApiError(error).message
    houseLocations.value = []
    stats.locations = 0
  } finally {
    loading.locations = false
  }
}

async function loadSummary() {
  if (!isAuthenticated.value) {
    return
  }

  loading.summary = true
  feedback.summary = ''

  try {
    await loadHousesCatalog({ force: true })
    const [housesResponse, itemsResponse] = await Promise.all([
      api.listHouses({ limit: 20, offset: 0 }),
      api.listItems({ limit: 20, offset: 0 }),
    ])

    stats.houses = housesResponse.houses?.length ?? 0
    stats.items = itemsResponse.items?.length ?? 0
    stats.recentItems = itemsResponse.items ?? []
    stats.itemsWithoutLocation = (itemsResponse.items ?? []).filter((item) => !item.houseLocationPath).length

    if (!quickSearch.houseId && (housesResponse.houses?.length ?? 0) > 0) {
      quickSearch.houseId = housesResponse.houses[0].houseId
    }

    await loadFeaturedLocations(activeHouseId.value)
  } catch (error) {
    feedback.summary = normalizeApiError(error).message
  } finally {
    loading.summary = false
  }
}

async function runQuickSearch() {
  loading.search = true
  feedback.search = ''

  try {
    const response = await api.searchItems({
      q: quickSearch.q || undefined,
      houseId: quickSearch.houseId || undefined,
      limit: 8,
      offset: 0,
    })
    quickResults.value = response.items ?? []
  } catch (error) {
    feedback.search = normalizeApiError(error).message
  } finally {
    loading.search = false
  }
}

function openObject(item) {
  router.push({
    name: 'objects-list',
    query: {
      item: item.inventoryItemId,
      q: quickSearch.q || '',
      houseId: quickSearch.houseId || '',
    },
  })
}

onMounted(async () => {
  await loadSummary()
  await runQuickSearch()
})

watch(isAuthenticated, async (value) => {
  if (value) {
    await loadSummary()
    await runQuickSearch()
  }
})

watch(activeHouseId, async (houseId, previousHouseId) => {
  if (!isAuthenticated.value || houseId === previousHouseId) {
    return
  }

  await loadFeaturedLocations(houseId)
})
</script>

<template lang="pug">
section.dashboard-view
  .metrics-grid
    MetricCard(
      v-for="metric in metrics"
      :key="metric.title"
      :title="metric.title"
      :value="metric.value"
      :trend="metric.trend"
      :accent="metric.accent"
    )

  article.panel-card
    .panel-card__header
      div
        p.panel-card__eyebrow Locaciones al inicio
        h3.panel-card__title {{ featuredHouse?.name || 'Ubicaciones de la casa activa' }}
      select.form-input.dashboard-house-select(v-model="quickSearch.houseId")
        option(value="") Selecciona una casa
        option(v-for="option in houseOptions" :key="option.value" :value="option.value")
          | {{ option.label }}

    p.muted-copy(v-if="featuredHouse") {{ featuredHouse.city || 'Sin ciudad' }} · {{ stats.locations }} locaciones visibles
    .detail-grid(v-if="houseLocations.length > 0")
      article.summary-card(v-for="location in houseLocations" :key="location.houseLocationId")
        strong {{ location.name }}
        span {{ location.path || 'Sin path' }}
    p.muted-copy(v-else-if="loading.locations || loading.summary") Cargando locaciones...
    p.form-feedback.form-feedback--error(v-else-if="feedback.locations") {{ feedback.locations }}
    p.muted-copy(v-else) Selecciona una casa para ver sus locaciones disponibles en el inicio.

  .dashboard-grid
    article.panel-card.panel-card--wide
      .panel-card__header
        div
          p.panel-card__eyebrow Punto de partida
          h3.panel-card__title Encuentra primero, administra despues
        RouterLink.primary-button(to="/objetos/nuevo") Registrar objeto

      form.form-grid.form-grid--compact(@submit.prevent="runQuickSearch")
        .form-row
          input.form-input(v-model="quickSearch.q" placeholder="Busca por nombre, categoria o alias")
          select.form-input(v-model="quickSearch.houseId")
            option(value="") Todas las casas
            option(v-for="option in houseOptions" :key="option.value" :value="option.value")
              | {{ option.label }}
        .button-row
          button.primary-button(type="submit" :disabled="loading.search") Buscar objeto
          RouterLink.ghost-button(to="/objetos") Ir al modulo de objetos
        p.form-feedback.form-feedback--error(v-if="feedback.search") {{ feedback.search }}

      table.data-table(v-if="quickResults.length > 0")
        thead
          tr
            th Objeto
            th Casa
            th Ubicacion
        tbody
          tr(v-for="item in quickResults" :key="item.inventoryItemId" @click="openObject(item)")
            td {{ item.objectName }}
            td {{ item.houseName || 'Sin casa' }}
            td {{ item.houseLocationPath || 'Sin ubicacion' }}
      p.muted-copy(v-else-if="loading.search") Buscando objetos...
      p.muted-copy(v-else) Escribe algo y empieza por encontrar lo que ya tienes.

    article.panel-card
      .panel-card__header
        div
          p.panel-card__eyebrow Recientes
          h3.panel-card__title Objetos registrados recientemente

      ul.activity-list(v-if="stats.recentItems.length > 0")
        li(v-for="item in stats.recentItems.slice(0, 5)" :key="item.inventoryItemId")
          strong {{ item.objectName }}
          p {{ item.houseName || 'Sin casa' }} · {{ item.houseLocationPath || 'Sin ubicacion' }}
      p.muted-copy(v-else-if="loading.summary") Cargando resumen...
      p.muted-copy(v-else) Aun no hay objetos visibles.

    article.panel-card.panel-card--accent
      p.panel-card__eyebrow Objetivo
      h3.panel-card__title HouseDB debe ayudarte a recordar lo que tienes y donde esta
      p.panel-card__text
        | Por eso las casas y locaciones quedan como contexto de apoyo, mientras que el flujo principal gira alrededor del objeto.
</template>
