<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import DetailModal from '../components/DetailModal.vue'
import PaginationControls from '../components/PaginationControls.vue'
import { normalizeApiError } from '../lib/api'
import { useCatalogStore } from '../stores/catalogs'
import { useSessionStore } from '../stores/session'

const route = useRoute()
const router = useRouter()
const { api, isAuthenticated } = useSessionStore()
const { houseOptions, loadHousesCatalog } = useCatalogStore()

const PAGE_SIZE = 12

const loading = reactive({
  search: false,
  detail: false,
})

const feedback = reactive({
  search: '',
  detail: '',
})

const filters = reactive({
  q: '',
  houseId: '',
})

const results = ref([])
const selectedItem = ref(null)
const selectedTimeline = ref([])

const pager = reactive({ limit: PAGE_SIZE, offset: 0 })

const hasSelectedItem = computed(() => Boolean(selectedItem.value))

function syncListQuery(itemId = '') {
  const query = {}

  if (filters.q) {
    query.q = filters.q
  }

  if (filters.houseId) {
    query.houseId = filters.houseId
  }

  if (itemId) {
    query.item = itemId
  }

  router.replace({
    name: 'objects-list',
    query,
  })
}

async function runSearch() {
  if (!isAuthenticated.value) {
    results.value = []
    return
  }

  loading.search = true
  feedback.search = ''

  try {
    await loadHousesCatalog()
    const response = await api.searchItems({
      q: filters.q || undefined,
      houseId: filters.houseId || undefined,
      limit: pager.limit,
      offset: pager.offset,
    })
    results.value = response.items ?? []
  } catch (error) {
    feedback.search = normalizeApiError(error).message
  } finally {
    loading.search = false
  }
}

async function loadItemDetail(inventoryItemId) {
  if (!inventoryItemId) {
    return
  }

  loading.detail = true
  feedback.detail = ''

  try {
    const [detail, timeline] = await Promise.all([
      api.getItem(inventoryItemId),
      api.listItemTimeline(inventoryItemId, {
        limit: 8,
        offset: 0,
      }),
    ])

    selectedItem.value = detail
    selectedTimeline.value = timeline.events ?? []
    syncListQuery(inventoryItemId)
  } catch (error) {
    feedback.detail = normalizeApiError(error).message
  } finally {
    loading.detail = false
  }
}

function closeDetail() {
  selectedItem.value = null
  selectedTimeline.value = []
  feedback.detail = ''
  syncListQuery()
}

function submitSearch() {
  pager.offset = 0
  syncListQuery()
  runSearch()
}

function resetSearch() {
  filters.q = ''
  filters.houseId = ''
  pager.offset = 0
  closeDetail()
  runSearch()
}

function applyRouteContext() {
  filters.q = typeof route.query.q === 'string' ? route.query.q : ''
  filters.houseId = typeof route.query.houseId === 'string' ? route.query.houseId : ''
}

onMounted(async () => {
  applyRouteContext()
  await runSearch()

  if (typeof route.query.item === 'string' && route.query.item) {
    await loadItemDetail(route.query.item)
  }
})

watch(isAuthenticated, () => {
  if (isAuthenticated.value) {
    runSearch()
  }
})

watch(() => pager.offset, () => {
  syncListQuery()
  runSearch()
})
</script>

<template lang="pug">
section.page-section
  article.panel-card.panel-card--accent
    p.panel-card__eyebrow Objetos
    h3.panel-card__title Lista de objetos
    p.panel-card__text
      | Usa la búsqueda para encontrar un objeto y abre su detalle en modal sin salir de la tabla.

  article.panel-card
    .panel-card__header
      div
        p.panel-card__eyebrow Herramientas
        h3.panel-card__title Buscar y administrar
      .toolbar-actions
        RouterLink.split-button.split-button--primary(to="/objetos/nuevo")
          span.split-button__icon +
          span.split-button__text Nuevo objeto
        button.circle-button.circle-button--info(type="button" @click="runSearch" :disabled="loading.search") R

    form.table-toolbar(@submit.prevent="submitSearch")
      .table-toolbar__search
        input.form-input.table-search-input(v-model="filters.q" placeholder="Buscar por nombre, alias o categoria")
        select.form-input.table-search-select(v-model="filters.houseId")
          option(value="") Todas las casas
          option(v-for="option in houseOptions" :key="option.value" :value="option.value")
            | {{ option.label }}
      .table-toolbar__actions
        button.primary-button.primary-button--warning(type="submit" :disabled="loading.search") Buscar
        button.ghost-button(type="button" @click="resetSearch") Limpiar

    p.form-feedback.form-feedback--error(v-if="feedback.search") {{ feedback.search }}

    .table-card
      .table-card__header
        h4.table-card__title Resultados
        p.muted-copy {{ loading.search ? 'Consultando...' : `${results.length} registros visibles` }}
      table.table-grid(v-if="results.length > 0")
        thead
          tr
            th Objeto
            th Casa
            th Ubicacion
            th Estado
            th.table-grid__actions Acciones
        tbody
          tr.table-grid__row(v-for="item in results" :key="item.inventoryItemId" @click="loadItemDetail(item.inventoryItemId)")
            td
              strong {{ item.objectName }}
              p.muted-copy {{ item.nickname || item.objectCategory || 'Sin alias' }}
            td {{ item.houseName || 'Sin casa' }}
            td {{ item.houseLocationPath || 'Sin ubicacion' }}
            td {{ item.conditionStatus || 'Sin estado' }}
            td.table-grid__actions(@click.stop)
              button.circle-button.circle-button--danger(type="button" @click="loadItemDetail(item.inventoryItemId)") i
      p.empty-copy(v-else-if="loading.search") Buscando objetos...
      p.empty-copy(v-else) No hay objetos con los filtros actuales.

    PaginationControls(
      :count="results.length"
      :limit="pager.limit"
      :offset="pager.offset"
      :hasMore="results.length === pager.limit"
      :loading="loading.search"
      @change="pager.offset = $event"
    )

  DetailModal(v-if="hasSelectedItem" :title="selectedItem.inventoryItem.objectName || 'Objeto'" @close="closeDetail")
    p.form-feedback.form-feedback--error(v-if="feedback.detail") {{ feedback.detail }}
    p.muted-copy(v-if="loading.detail") Cargando detalle...
    template(v-if="selectedItem")
      dl.detail-list
        div
          dt Alias
          dd {{ selectedItem.inventoryItem.nickname || 'Sin alias' }}
        div
          dt Categoria
          dd {{ selectedItem.inventoryItem.objectCategory || 'Sin categoria' }}
        div
          dt Casa
          dd {{ selectedItem.inventoryItem.houseName || 'Sin casa' }}
        div
          dt Ubicacion
          dd {{ selectedItem.inventoryItem.houseLocationPath || 'Sin ubicacion' }}
        div
          dt Estado
          dd {{ selectedItem.inventoryItem.conditionStatus || 'Sin estado' }}
        div
          dt Kiwi
          dd {{ selectedItem.kiwiStatus || 'Sin estado Kiwi' }}
      .table-card
        .table-card__header
          h4.table-card__title Historial
          p.muted-copy {{ selectedTimeline.length }} movimientos
        table.table-grid(v-if="selectedTimeline.length > 0")
          thead
            tr
              th Fecha
              th Destino
              th Motivo
          tbody
            tr(v-for="event in selectedTimeline" :key="event.itemMovementId")
              td {{ event.movedAt }}
              td {{ event.toHouseLocationPath || event.toHouseLocationLeafId || 'Sin destino' }}
              td {{ event.movementReason || 'Sin motivo' }}
        p.empty-copy(v-else) Este objeto aun no tiene historial visible.
</template>
