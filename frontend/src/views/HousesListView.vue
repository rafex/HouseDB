<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'

import DetailModal from '../components/DetailModal.vue'
import { normalizeApiError } from '../lib/api'
import { useCatalogStore } from '../stores/catalogs'
import { useSessionStore } from '../stores/session'

const { api, isAuthenticated } = useSessionStore()
const { loadHousesCatalog } = useCatalogStore()

const loading = reactive({
  houses: false,
  detail: false,
})

const feedback = reactive({
  houses: '',
  detail: '',
})

const filters = reactive({
  q: '',
})

const houses = ref([])
const selectedHouse = ref(null)
const selectedHouseLocations = ref([])

const filteredHouses = computed(() => {
  const query = filters.q.trim().toLowerCase()
  if (!query) {
    return houses.value
  }

  return houses.value.filter((house) =>
    [house.name, house.description, house.city, house.state, house.country]
      .filter(Boolean)
      .some((value) => value.toLowerCase().includes(query)),
  )
})

async function loadHouses() {
  if (!isAuthenticated.value) {
    houses.value = []
    return
  }

  loading.houses = true
  feedback.houses = ''

  try {
    await loadHousesCatalog({ force: true })
    const response = await api.listHouses({
      limit: 100,
      offset: 0,
    })
    houses.value = response.houses ?? []
  } catch (error) {
    feedback.houses = normalizeApiError(error).message
  } finally {
    loading.houses = false
  }
}

async function openHouse(house) {
  selectedHouse.value = house
  selectedHouseLocations.value = []
  loading.detail = true
  feedback.detail = ''

  try {
    const response = await api.listHouseLocations(house.houseId, {
      limit: 12,
      offset: 0,
    })
    selectedHouseLocations.value = response.locations ?? []
  } catch (error) {
    feedback.detail = normalizeApiError(error).message
  } finally {
    loading.detail = false
  }
}

function closeDetail() {
  selectedHouse.value = null
  selectedHouseLocations.value = []
  feedback.detail = ''
}

onMounted(loadHouses)
</script>

<template lang="pug">
section.page-section
  article.panel-card.panel-card--accent-success
    p.panel-card__eyebrow Espacios
    h3.panel-card__title Casas
    p.panel-card__text
      | Consulta las casas registradas, busca por ciudad o nombre y abre un modal para revisar su contexto.

  article.panel-card
    .panel-card__header
      div
        p.panel-card__eyebrow Lista
        h3.panel-card__title Casas registradas
      .toolbar-actions
        RouterLink.split-button.split-button--success(to="/casas/nueva")
          span.split-button__icon +
          span.split-button__text Agregar casa
        button.circle-button.circle-button--warning(type="button" @click="loadHouses" :disabled="loading.houses") R

    .table-toolbar
      .table-toolbar__search
        input.form-input.table-search-input(v-model="filters.q" placeholder="Buscar por nombre, ciudad o pais")

    p.form-feedback.form-feedback--error(v-if="feedback.houses") {{ feedback.houses }}

    .table-card
      .table-card__header
        h4.table-card__title Resultado
        p.muted-copy {{ loading.houses ? 'Consultando...' : `${filteredHouses.length} casas` }}
      table.table-grid(v-if="filteredHouses.length > 0")
        thead
          tr
            th Casa
            th Ciudad
            th Estado
            th Pais
            th.table-grid__actions Acciones
        tbody
          tr.table-grid__row(v-for="house in filteredHouses" :key="house.houseId" @click="openHouse(house)")
            td
              strong {{ house.name }}
              p.muted-copy {{ house.description || 'Sin descripcion' }}
            td {{ house.city || 'Sin ciudad' }}
            td {{ house.state || 'Sin estado' }}
            td {{ house.country || 'Sin pais' }}
            td.table-grid__actions(@click.stop)
              button.circle-button.circle-button--success(type="button" @click="openHouse(house)") i
      p.empty-copy(v-else-if="loading.houses") Cargando casas...
      p.empty-copy(v-else) No hay casas visibles.

  DetailModal(v-if="selectedHouse" :title="selectedHouse.name || 'Casa'" @close="closeDetail")
    p.form-feedback.form-feedback--error(v-if="feedback.detail") {{ feedback.detail }}
    p.muted-copy(v-if="loading.detail") Cargando locaciones de la casa...
    template(v-if="selectedHouse")
      dl.detail-list
        div
          dt Descripcion
          dd {{ selectedHouse.description || 'Sin descripcion' }}
        div
          dt Ciudad
          dd {{ selectedHouse.city || 'Sin ciudad' }}
        div
          dt Estado
          dd {{ selectedHouse.state || 'Sin estado' }}
        div
          dt Pais
          dd {{ selectedHouse.country || 'Sin pais' }}
        div
          dt Id
          dd {{ selectedHouse.houseId }}
      .table-card
        .table-card__header
          h4.table-card__title Locaciones relacionadas
          p.muted-copy {{ selectedHouseLocations.length }} visibles
        table.table-grid(v-if="selectedHouseLocations.length > 0")
          thead
            tr
              th Nombre
              th Tipo
              th Ruta
          tbody
            tr(v-for="location in selectedHouseLocations" :key="location.houseLocationId")
              td {{ location.name }}
              td {{ location.locationKind || 'Sin tipo' }}
              td {{ location.path || 'Sin ruta' }}
        p.empty-copy(v-else-if="!loading.detail") Esta casa aun no tiene locaciones visibles.
</template>
