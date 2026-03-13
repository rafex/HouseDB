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
})

const feedback = reactive({
  summary: '',
  search: '',
})

const stats = reactive({
  houses: 0,
  items: 0,
  itemsWithoutLocation: 0,
  recentItems: [],
})

const quickSearch = reactive({
  q: '',
  houseId: '',
})

const quickResults = ref([])

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
    title: 'Sin ubicacion',
    value: String(stats.itemsWithoutLocation),
    trend: 'Objetos que costaria reencontrar',
    accent: 'coral',
  },
  {
    title: 'Busqueda',
    value: quickResults.value.length > 0 ? String(quickResults.value.length) : 'Lista',
    trend: 'Encuentra rapido lo que ya tienes',
    accent: 'indigo',
  },
])

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
    name: 'inventory',
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

    article.panel-card
      .panel-card__header
        div
          p.panel-card__eyebrow Espacios
          h3.panel-card__title Casas disponibles

      ul.legend-list(v-if="houseOptions.length > 0")
        li(v-for="option in houseOptions.slice(0, 4)" :key="option.value") {{ option.label }}
      p.muted-copy(v-else-if="loading.summary") Cargando casas...
      p.muted-copy(v-else) No hay casas cargadas para esta sesion.

    article.panel-card.panel-card--accent
      p.panel-card__eyebrow Objetivo
      h3.panel-card__title HouseDB debe ayudarte a recordar lo que tienes y donde esta
      p.panel-card__text
        | Por eso las casas y locaciones quedan como contexto de apoyo, mientras que el flujo principal gira alrededor del objeto.
</template>
