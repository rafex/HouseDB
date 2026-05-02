<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import DetailModal from '../components/DetailModal.vue'
import PaginationControls from '../components/PaginationControls.vue'
import { createPaginationState, normalizeApiError, paginationFromResponse } from '../lib/api'
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
  move: false,
  favorite: false,
  locations: false,
})

const feedback = reactive({
  search: '',
  detail: '',
  move: '',
  favorite: '',
  locations: '',
})

const filters = reactive({
  q: '',
  houseId: '',
})

const results = ref([])
const selectedItem = ref(null)
const selectedTimeline = ref([])
const moveLocations = ref([])

const pager = reactive(createPaginationState(PAGE_SIZE))
const timelinePager = reactive(createPaginationState(8))

const hasSelectedItem = computed(() => Boolean(selectedItem.value))
const selectedItemId = computed(() => selectedItem.value?.inventoryItem?.inventoryItemId ?? '')

const moveForm = reactive({
  houseId: '',
  toHouseLocationLeafId: '',
  movedBy: '',
  movementReason: '',
  notes: '',
})

const moveLeafLocationOptions = computed(() =>
  moveLocations.value
    .filter((location) => location.isLeaf)
    .map((location) => ({
      value: location.houseLocationId,
      label: location.path || location.name,
    })),
)

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
    Object.assign(pager, paginationFromResponse(response, pager))
  } catch (error) {
    feedback.search = normalizeApiError(error).message
  } finally {
    loading.search = false
  }
}

async function loadMoveLocations() {
  if (!moveForm.houseId) {
    moveLocations.value = []
    moveForm.toHouseLocationLeafId = ''
    return
  }

  loading.locations = true
  feedback.locations = ''

  try {
    const response = await api.listHouseLocations(moveForm.houseId, {
      limit: 500,
      offset: 0,
    })
    moveLocations.value = response.locations ?? []
  } catch (error) {
    feedback.locations = normalizeApiError(error).message
  } finally {
    loading.locations = false
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
        limit: timelinePager.limit,
        offset: timelinePager.offset,
      }),
    ])

    selectedItem.value = detail
    selectedTimeline.value = timeline.events ?? []
    Object.assign(timelinePager, paginationFromResponse(timeline, timelinePager))
    moveForm.houseId = detail.inventoryItem.houseId || ''
    await loadMoveLocations()
    syncListQuery(inventoryItemId)
  } catch (error) {
    feedback.detail = normalizeApiError(error).message
  } finally {
    loading.detail = false
  }
}

async function moveItem() {
  if (!selectedItemId.value) {
    feedback.move = 'Selecciona un objeto primero.'
    return
  }

  loading.move = true
  feedback.move = ''

  try {
    await api.moveItem(selectedItemId.value, {
      toHouseLocationLeafId: moveForm.toHouseLocationLeafId,
      movedBy: moveForm.movedBy || undefined,
      movementReason: moveForm.movementReason || undefined,
      notes: moveForm.notes || undefined,
    })
    feedback.move = 'Objeto movido correctamente.'
    timelinePager.offset = 0
    await loadItemDetail(selectedItemId.value)
  } catch (error) {
    feedback.move = normalizeApiError(error).message
  } finally {
    loading.move = false
  }
}

async function toggleFavorite(isFavorite) {
  if (!selectedItemId.value) {
    feedback.favorite = 'Selecciona un objeto primero.'
    return
  }

  loading.favorite = true
  feedback.favorite = ''

  try {
    await api.setFavorite(selectedItemId.value, { isFavorite })
    feedback.favorite = isFavorite ? 'Marcado como favorito.' : 'Favorito removido.'
  } catch (error) {
    feedback.favorite = normalizeApiError(error).message
  } finally {
    loading.favorite = false
  }
}

function closeDetail() {
  selectedItem.value = null
  selectedTimeline.value = []
  feedback.detail = ''
  timelinePager.offset = 0
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

watch(() => pager.limit, () => {
  pager.offset = 0
  syncListQuery()
  runSearch()
})

watch(() => timelinePager.offset, () => {
  if (selectedItem.value?.inventoryItem?.inventoryItemId) {
    loadItemDetail(selectedItem.value.inventoryItem.inventoryItemId)
  }
})

watch(() => timelinePager.limit, () => {
  timelinePager.offset = 0
  if (selectedItem.value?.inventoryItem?.inventoryItemId) {
    loadItemDetail(selectedItem.value.inventoryItem.inventoryItemId)
  }
})

watch(() => moveForm.houseId, loadMoveLocations)
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
        p.muted-copy {{ loading.search ? 'Consultando...' : `${pager.returned} registros visibles` }}
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
      :count="pager.returned"
      :limit="pager.limit"
      :offset="pager.offset"
      :hasMore="pager.hasMore"
      :previousOffset="pager.previousOffset"
      :nextOffset="pager.nextOffset"
      :loading="loading.search"
      @change="pager.offset = $event"
      @limit-change="pager.limit = $event"
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
      form.form-grid.form-grid--compact(@submit.prevent="moveItem")
        p.section-label Mover objeto
        select.form-input(v-model="moveForm.houseId")
          option(value="") Selecciona la casa destino
          option(v-for="option in houseOptions" :key="option.value" :value="option.value")
            | {{ option.label }}
        select.form-input(v-model="moveForm.toHouseLocationLeafId" :disabled="loading.locations || moveLeafLocationOptions.length === 0")
          option(value="") Selecciona nueva locacion hoja
          option(v-for="option in moveLeafLocationOptions" :key="option.value" :value="option.value")
            | {{ option.label }}
        input.form-input(v-model="moveForm.movedBy" placeholder="Quien lo movio")
        input.form-input(v-model="moveForm.movementReason" placeholder="Motivo del movimiento")
        textarea.form-input.form-textarea(v-model="moveForm.notes" placeholder="Notas para encontrarlo despues")
        .button-row
          button.primary-button(type="submit" :disabled="loading.move") Guardar movimiento
          button.ghost-button(type="button" @click="toggleFavorite(true)" :disabled="loading.favorite") Favorito
          button.ghost-button(type="button" @click="toggleFavorite(false)" :disabled="loading.favorite") Quitar favorito
        p.form-feedback(v-if="feedback.move || feedback.favorite || feedback.locations")
          | {{ feedback.move || feedback.favorite || feedback.locations }}
      .table-card
        .table-card__header
          h4.table-card__title Historial
          p.muted-copy {{ timelinePager.returned }} movimientos visibles
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
        PaginationControls(
          :count="timelinePager.returned"
          :limit="timelinePager.limit"
          :offset="timelinePager.offset"
          :hasMore="timelinePager.hasMore"
          :previousOffset="timelinePager.previousOffset"
          :nextOffset="timelinePager.nextOffset"
          :loading="loading.detail"
          @change="timelinePager.offset = $event"
          @limit-change="timelinePager.limit = $event"
        )
</template>
