<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { normalizeApiError } from '../lib/api'
import { useCatalogStore } from '../stores/catalogs'
import { useSessionStore } from '../stores/session'

const router = useRouter()
const { api, isAuthenticated } = useSessionStore()
const { houseOptions, loadHousesCatalog } = useCatalogStore()

const loading = reactive({
  create: false,
  locations: false,
  metadataCatalogs: false,
})

const feedback = reactive({
  create: '',
  locations: '',
  metadataCatalogs: '',
})

const form = reactive({
  houseId: '',
  houseLocationLeafId: '',
  objectName: '',
  objectDescription: '',
  objectCategory: '',
  objectType: 'EQUIPMENT',
  objectTags: '',
  nickname: '',
  serialNumber: '',
  conditionStatus: 'active',
  movedBy: '',
  notes: '',
  housedbMetadataCatalogId: '',
  kiwiMetadataCatalogId: '',
  housedbMetadataPreview: '',
  kiwiMetadataPreview: '',
})

const locations = ref([])
const metadataCatalogs = ref([])

const leafLocationOptions = computed(() =>
  locations.value
    .filter((location) => location.isLeaf)
    .map((location) => ({
      value: location.houseLocationId,
      label: location.path || location.name,
    })),
)

const housedbMetadataOptions = computed(() =>
  metadataCatalogs.value
    .filter((catalog) => catalog.metadataTarget === 'inventory_item')
    .map((catalog) => ({
      value: catalog.metadataCatalogId,
      label: catalog.name,
      payloadJson: catalog.payloadJson,
    })),
)

const kiwiMetadataOptions = computed(() =>
  metadataCatalogs.value
    .filter((catalog) => catalog.metadataTarget === 'kiwi_object')
    .map((catalog) => ({
      value: catalog.metadataCatalogId,
      label: catalog.name,
      payloadJson: catalog.payloadJson,
    })),
)

function parseJsonField(value) {
  if (!value) {
    return undefined
  }

  return JSON.parse(value)
}

async function loadLocations() {
  if (!form.houseId) {
    locations.value = []
    form.houseLocationLeafId = ''
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

async function loadMetadataCatalogs() {
  loading.metadataCatalogs = true
  feedback.metadataCatalogs = ''

  try {
    const response = await api.listMetadataCatalogs({
      limit: 100,
      offset: 0,
    })
    metadataCatalogs.value = response.catalogs ?? []
  } catch (error) {
    feedback.metadataCatalogs = normalizeApiError(error).message
  } finally {
    loading.metadataCatalogs = false
  }
}

async function submit() {
  loading.create = true
  feedback.create = ''

  try {
    const response = await api.createItem({
      objectName: form.objectName,
      objectDescription: form.objectDescription || undefined,
      objectCategory: form.objectCategory || undefined,
      objectType: form.objectType || undefined,
      objectTags: form.objectTags
        .split(',')
        .map((tag) => tag.trim())
        .filter(Boolean),
      nickname: form.nickname || undefined,
      serialNumber: form.serialNumber || undefined,
      conditionStatus: form.conditionStatus || undefined,
      houseLocationLeafId: form.houseLocationLeafId,
      movedBy: form.movedBy || undefined,
      notes: form.notes || undefined,
      housedbMetadata: parseJsonField(form.housedbMetadataPreview),
      kiwiMetadata: parseJsonField(form.kiwiMetadataPreview),
    })

    await router.push({
      name: 'inventory',
      query: {
        created: response.inventoryItemId,
      },
    })
  } catch (error) {
    feedback.create = normalizeApiError(error).message
  } finally {
    loading.create = false
  }
}

onMounted(async () => {
  if (!isAuthenticated.value) {
    return
  }

  await Promise.all([loadHousesCatalog(), loadMetadataCatalogs()])
})

watch(() => form.houseId, loadLocations)
watch(() => form.housedbMetadataCatalogId, (catalogId) => {
  const selected = housedbMetadataOptions.value.find((option) => option.value === catalogId)
  form.housedbMetadataPreview = selected?.payloadJson ?? ''
})
watch(() => form.kiwiMetadataCatalogId, (catalogId) => {
  const selected = kiwiMetadataOptions.value.find((option) => option.value === catalogId)
  form.kiwiMetadataPreview = selected?.payloadJson ?? ''
})
</script>

<template lang="pug">
section.page-section
  article.panel-card.panel-card--accent
    p.panel-card__eyebrow Alta guiada
    h3.panel-card__title Registra un objeto para poder encontrarlo despues
    p.panel-card__text
      | Primero elige la casa y la ubicacion exacta. Despues describe el objeto con palabras y metadatos utiles.

  article.panel-card
    form.form-grid(@submit.prevent="submit")
      p.section-label Contexto fisico
      .form-row
        select.form-input(v-model="form.houseId" required)
          option(value="") Selecciona una casa
          option(v-for="option in houseOptions" :key="option.value" :value="option.value")
            | {{ option.label }}
        select.form-input(v-model="form.houseLocationLeafId" :disabled="loading.locations || leafLocationOptions.length === 0" required)
          option(value="") Selecciona una locacion hoja
          option(v-for="option in leafLocationOptions" :key="option.value" :value="option.value")
            | {{ option.label }}

      p.section-label Identidad del objeto
      .form-row
        input.form-input(v-model="form.objectName" placeholder="Nombre del objeto" required)
        input.form-input(v-model="form.nickname" placeholder="Alias para reconocerlo")
      textarea.form-input.form-textarea(v-model="form.objectDescription" placeholder="Describe el objeto con palabras que ayuden a reconocerlo")
      .form-row
        input.form-input(v-model="form.objectCategory" placeholder="Categoria")
        input.form-input(v-model="form.objectType" placeholder="Tipo")
      .form-row
        input.form-input(v-model="form.objectTags" placeholder="Tags separados por coma")
        input.form-input(v-model="form.serialNumber" placeholder="Serie o identificador")

      p.section-label Estado y movimiento inicial
      .form-row
        input.form-input(v-model="form.conditionStatus" placeholder="conditionStatus")
        input.form-input(v-model="form.movedBy" placeholder="Quien lo registro")
      textarea.form-input.form-textarea(v-model="form.notes" placeholder="Notas para encontrarlo o cuidarlo")

      p.section-label Metadatos reutilizables
      .form-row
        select.form-input(v-model="form.housedbMetadataCatalogId" :disabled="loading.metadataCatalogs || housedbMetadataOptions.length === 0")
          option(value="") Sin metadata HouseDB
          option(v-for="option in housedbMetadataOptions" :key="option.value" :value="option.value")
            | {{ option.label }}
        select.form-input(v-model="form.kiwiMetadataCatalogId" :disabled="loading.metadataCatalogs || kiwiMetadataOptions.length === 0")
          option(value="") Sin metadata Kiwi
          option(v-for="option in kiwiMetadataOptions" :key="option.value" :value="option.value")
            | {{ option.label }}
      textarea.form-input.form-textarea(
        v-model="form.housedbMetadataPreview"
        placeholder='Metadata HouseDB editable'
        :disabled="!form.housedbMetadataCatalogId"
      )
      textarea.form-input.form-textarea(
        v-model="form.kiwiMetadataPreview"
        placeholder='Metadata Kiwi editable'
        :disabled="!form.kiwiMetadataCatalogId"
      )

      button.primary-button(type="submit" :disabled="loading.create") Guardar objeto
      p.form-feedback(v-if="feedback.create || feedback.locations || feedback.metadataCatalogs")
        | {{ feedback.create || feedback.locations || feedback.metadataCatalogs }}
</template>
