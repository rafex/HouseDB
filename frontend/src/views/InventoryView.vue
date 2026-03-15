<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

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
  houseLocationLeafId: '',
})

const results = ref([])
const selectedItem = ref(null)
const selectedItemTimeline = ref([])
const moveLocations = ref([])

const searchPager = reactive(createPaginationState(PAGE_SIZE))
const timelinePager = reactive(createPaginationState(PAGE_SIZE))

const moveForm = reactive({
  houseId: '',
  toHouseLocationLeafId: '',
  movedBy: '',
  movementReason: '',
  notes: '',
})

const selectedItemId = computed(() => selectedItem.value?.inventoryItem?.inventoryItemId ?? '')

const moveLeafLocationOptions = computed(() =>
  moveLocations.value
    .filter((location) => location.isLeaf)
    .map((location) => ({
      value: location.houseLocationId,
      label: location.path || location.name,
    })),
)

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

async function runSearch() {
  if (!isAuthenticated.value) {
    return
  }

  loading.search = true
  feedback.search = ''

  try {
    await loadHousesCatalog()
    const response = await api.searchItems({
      q: filters.q || undefined,
      houseId: filters.houseId || undefined,
      houseLocationLeafId: filters.houseLocationLeafId || undefined,
      limit: searchPager.limit,
      offset: searchPager.offset,
    })
    results.value = response.items ?? []
    Object.assign(searchPager, paginationFromResponse(response, searchPager))
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
        limit: timelinePager.limit,
        offset: timelinePager.offset,
      }),
    ])

    selectedItem.value = detail
    selectedItemTimeline.value = timeline.events ?? []
    Object.assign(timelinePager, paginationFromResponse(timeline, timelinePager))
    moveForm.houseId = detail.inventoryItem.houseId || ''
    await loadMoveLocations()

    router.replace({
      name: 'inventory',
      query: {
        ...route.query,
        item: inventoryItemId,
      },
    })
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

function applyRouteContext() {
  if (typeof route.query.q === 'string') {
    filters.q = route.query.q
  }
  if (typeof route.query.houseId === 'string') {
    filters.houseId = route.query.houseId
  }
}

function submitSearch() {
  searchPager.offset = 0
  runSearch()
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
watch(() => searchPager.offset, runSearch)
watch(() => searchPager.limit, () => {
  searchPager.offset = 0
  runSearch()
})
watch(() => timelinePager.offset, () => {
  if (selectedItemId.value) {
    loadItemDetail(selectedItemId.value)
  }
})
watch(() => timelinePager.limit, () => {
  timelinePager.offset = 0
  if (selectedItemId.value) {
    loadItemDetail(selectedItemId.value)
  }
})
watch(() => moveForm.houseId, loadMoveLocations)
</script>

<template lang="pug">
section.page-section
  article.panel-card.panel-card--accent
    p.panel-card__eyebrow Flujo principal
    h3.panel-card__title Busca objetos por lo que recuerdas de ellos
    p.panel-card__text
      | Nombre, alias, categoria y ubicacion deben ayudarte a responder rapido: "si lo tengo, donde esta?".

  article.panel-card
    .panel-card__header
      div
        p.panel-card__eyebrow Busqueda
        h3.panel-card__title Explora lo que ya tienes registrado
      RouterLink.primary-button(to="/objetos/nuevo") Nuevo objeto

    form.form-grid.form-grid--compact(@submit.prevent="submitSearch")
      .form-row
        input.form-input(v-model="filters.q" placeholder="Busca por nombre, alias o categoria")
        select.form-input(v-model="filters.houseId")
          option(value="") Todas las casas
          option(v-for="option in houseOptions" :key="option.value" :value="option.value")
            | {{ option.label }}
      .button-row
        button.primary-button(type="submit" :disabled="loading.search") Buscar
        button.ghost-button(type="button" @click="filters.q = ''; filters.houseId = ''; filters.houseLocationLeafId = ''; submitSearch()") Limpiar
      p.form-feedback.form-feedback--error(v-if="feedback.search") {{ feedback.search }}

  .split-grid
    article.panel-card
      .panel-card__header
        div
          p.panel-card__eyebrow Resultados
          h3.panel-card__title Objetos encontrados

      table.data-table(v-if="results.length > 0")
        thead
          tr
            th Objeto
            th Casa
            th Ubicacion
        tbody
          tr(v-for="item in results" :key="item.inventoryItemId" @click="loadItemDetail(item.inventoryItemId)")
            td {{ item.objectName }}
            td {{ item.houseName || 'Sin casa' }}
            td {{ item.houseLocationPath || 'Sin ubicacion' }}
      p.muted-copy(v-else-if="loading.search") Buscando objetos...
      p.muted-copy(v-else) No hay resultados con los filtros actuales.
      PaginationControls(
        :count="searchPager.returned"
        :limit="searchPager.limit"
        :offset="searchPager.offset"
        :hasMore="searchPager.hasMore"
        :previousOffset="searchPager.previousOffset"
        :nextOffset="searchPager.nextOffset"
        :loading="loading.search"
        @change="searchPager.offset = $event"
        @limit-change="searchPager.limit = $event"
      )

    article.panel-card
      .panel-card__header
        div
          p.panel-card__eyebrow Detalle
          h3.panel-card__title Objeto seleccionado

      p.muted-copy(v-if="!selectedItem && !loading.detail") Selecciona un objeto para ver donde esta y como moverlo.
      p.muted-copy(v-if="loading.detail") Cargando detalle...
      p.form-feedback.form-feedback--error(v-if="feedback.detail") {{ feedback.detail }}
      template(v-if="selectedItem")
        .detail-block
          dl.detail-list
            div
              dt Nombre
              dd {{ selectedItem.inventoryItem.nickname || 'Sin alias' }} / {{ selectedItem.inventoryItem.objectId }}
            div
              dt Casa
              dd {{ selectedItem.inventoryItem.houseName || 'Sin casa' }}
            div
              dt Ubicacion actual
              dd {{ selectedItem.inventoryItem.houseLocationPath || 'Sin ubicacion' }}
            div
              dt Estado
              dd {{ selectedItem.inventoryItem.conditionStatus || 'Sin estado' }}
            div
              dt Kiwi
              dd {{ selectedItem.kiwiStatus }}

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

        .timeline-block
          p.section-label Historial de ubicaciones
          table.data-table(v-if="selectedItemTimeline.length > 0")
            thead
              tr
                th Fecha
                th Destino
                th Motivo
            tbody
              tr(v-for="event in selectedItemTimeline" :key="event.itemMovementId")
                td {{ event.movedAt }}
                td {{ event.toHouseLocationPath || event.toHouseLocationLeafId || 'Sin destino' }}
                td {{ event.movementReason || 'Sin motivo' }}
          p.muted-copy(v-else) Este objeto aun no tiene historial visible.
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
