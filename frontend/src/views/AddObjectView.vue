<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import MetadataFieldEditor from '../components/MetadataFieldEditor.vue'
import { normalizeApiError } from '../lib/api'
import { useCatalogStore } from '../stores/catalogs'
import { useSessionStore } from '../stores/session'

const router = useRouter()
const { api, isAuthenticated } = useSessionStore()
const { houseOptions, loadHousesCatalog } = useCatalogStore()

const loading = reactive({
  create: false,
  locations: false,
  metadataTemplates: false,
})

const feedback = reactive({
  create: '',
  locations: '',
  metadataTemplates: '',
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
  housedbMetadataTemplateId: '',
  kiwiMetadataTemplateId: '',
})

const locations = ref([])
const metadataTemplates = ref([])
const housedbMetadataEntries = ref([])
const kiwiMetadataEntries = ref([])

const housedbSuggestionPresets = [
  {
    key: 'alias',
    label: 'Alias',
    placeholder: 'Ej. audifonos del escritorio',
    hint: 'Nombre cotidiano con el que lo reconocerias rapidamente.',
  },
  {
    key: 'color',
    label: 'Color',
    placeholder: 'Ej. negro mate',
    hint: 'Describe el color o apariencia que te ayuda a ubicarlo.',
  },
  {
    key: 'material',
    label: 'Material',
    placeholder: 'Ej. metal, plastico, madera',
    hint: 'Util para distinguirlo de objetos parecidos.',
  },
  {
    key: 'visualReference',
    label: 'Referencia visual',
    placeholder: 'Ej. caja azul con etiqueta blanca',
    hint: 'Pista visual concreta para reencontrarlo.',
  },
  {
    key: 'careNotes',
    label: 'Nota de cuidado',
    placeholder: 'Ej. no mojar, guardar en funda',
    hint: 'Dato de cuidado o manejo para no perder contexto.',
  },
]

const kiwiSuggestionPresets = [
  {
    key: 'brand',
    label: 'Brand',
    placeholder: 'Ej. Sony',
    hint: 'Marca o fabricante reconocido por sistemas externos.',
  },
  {
    key: 'model',
    label: 'Model',
    placeholder: 'Ej. WH-1000XM5',
    hint: 'Modelo tecnico del objeto o activo.',
  },
  {
    key: 'serialNumber',
    label: 'Serial',
    placeholder: 'Ej. SN-12345',
    hint: 'Serie o identificador tecnico del activo.',
  },
  {
    key: 'trackingLabel',
    label: 'Tracking',
    placeholder: 'Ej. source-housedb-001',
    hint: 'Etiqueta tecnica para sincronizacion o trazabilidad.',
  },
]

const leafLocationOptions = computed(() =>
  locations.value
    .filter((location) => location.isLeaf)
    .map((location) => ({
      value: location.houseLocationId,
      label: location.path || location.name,
    })),
)

const housedbMetadataOptions = computed(() =>
  metadataTemplates.value
    .filter((template) => template.metadataTarget === 'inventory_item')
    .map((template) => ({
      value: template.metadataTemplateId,
      label: template.name,
      description: template.description,
      definitionJson: template.definitionJson,
    })),
)

const kiwiMetadataOptions = computed(() =>
  metadataTemplates.value
    .filter((template) => template.metadataTarget === 'kiwi_object')
    .map((template) => ({
      value: template.metadataTemplateId,
      label: template.name,
      description: template.description,
      definitionJson: template.definitionJson,
    })),
)

const housedbSuggestions = computed(() =>
  mergeSuggestions(housedbSuggestionPresets, housedbMetadataOptions.value, housedbMetadataEntries.value),
)

const kiwiSuggestions = computed(() =>
  mergeSuggestions(kiwiSuggestionPresets, kiwiMetadataOptions.value, kiwiMetadataEntries.value),
)

function parseTemplateDefinition(definitionJson) {
  if (!definitionJson) {
    return []
  }

  try {
    const parsed = JSON.parse(definitionJson)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function objectToEntries(value) {
  return Object.entries(value ?? {}).map(([key, rawValue]) => ({
    key,
    value: rawValue == null ? '' : String(rawValue),
  }))
}

function entriesToObject(entries) {
  return (entries ?? []).reduce((accumulator, entry) => {
    const key = entry.key?.trim()
    const value = entry.value?.trim()
    if (!key || !value) {
      return accumulator
    }

    accumulator[key] = value
    return accumulator
  }, {})
}

function mergeSuggestions(baseSuggestions, catalogOptions, currentEntries) {
  const currentKeys = new Set((currentEntries ?? []).map((entry) => entry.key))
  const merged = new Map()

  baseSuggestions.forEach((suggestion) => {
    merged.set(suggestion.key, suggestion)
  })

  catalogOptions.forEach((template) => {
    parseTemplateDefinition(template.definitionJson).forEach((field) => {
      if (!field?.key || merged.has(field.key)) {
        return
      }

      merged.set(field.key, {
        key: field.key,
        label: field.label || field.key,
        placeholder: field.placeholder || `Valor para ${field.key}`,
        hint: field.hint || template.description || `Sugerido por la plantilla ${template.label}.`,
        defaultValue: field.defaultValue == null ? '' : String(field.defaultValue),
      })
    })
  })

  return [...merged.values()].filter((suggestion) => !currentKeys.has(suggestion.key))
}

function applyTemplateToEntries(target, templateId, options) {
  const selected = options.find((option) => option.value === templateId)
  target.value = parseTemplateDefinition(selected?.definitionJson).map((field) => ({
    key: field.key,
    value: field.defaultValue == null ? '' : String(field.defaultValue),
  }))
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

async function loadMetadataTemplates() {
  loading.metadataTemplates = true
  feedback.metadataTemplates = ''

  try {
    const response = await api.listMetadataTemplates({
      limit: 100,
      offset: 0,
    })
    metadataTemplates.value = response.templates ?? []
  } catch (error) {
    feedback.metadataTemplates = normalizeApiError(error).message
  } finally {
    loading.metadataTemplates = false
  }
}

async function submit() {
  loading.create = true
  feedback.create = ''

  try {
    const housedbMetadata = entriesToObject(housedbMetadataEntries.value)
    const kiwiMetadata = entriesToObject(kiwiMetadataEntries.value)

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
      housedbMetadata: Object.keys(housedbMetadata).length > 0 ? housedbMetadata : undefined,
      kiwiMetadata: Object.keys(kiwiMetadata).length > 0 ? kiwiMetadata : undefined,
    })

    await router.push({
      name: 'objects-list',
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

  await Promise.all([loadHousesCatalog(), loadMetadataTemplates()])
})

watch(() => form.houseId, loadLocations)
watch(() => form.housedbMetadataTemplateId, (templateId) => {
  applyTemplateToEntries(housedbMetadataEntries, templateId, housedbMetadataOptions.value)
})
watch(() => form.kiwiMetadataTemplateId, (templateId) => {
  applyTemplateToEntries(kiwiMetadataEntries, templateId, kiwiMetadataOptions.value)
})
</script>

<template lang="pug">
section.page-section
  article.panel-card.panel-card--accent
    p.panel-card__eyebrow Alta guiada
    h3.panel-card__title Registra un objeto para poder encontrarlo despues
    p.panel-card__text
      | Primero elige la casa y la ubicacion exacta. Despues describe el objeto con palabras, atributos y pistas utiles.

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

      MetadataFieldEditor(
        v-model="housedbMetadataEntries"
        :catalogValue="form.housedbMetadataTemplateId"
        title="Datos para encontrarlo en casa"
        subtitle="Agrega atributos que te ayuden a reconocer el objeto rapidamente dentro de HouseDB."
        help="Aqui van datos humanos y utiles para reencontrar el objeto: color, alias, material, referencias visuales o notas practicas."
        catalogLabel="Plantilla sugerida HouseDB"
        :catalogOptions="housedbMetadataOptions"
        :suggestions="housedbSuggestions"
        @update:catalogValue="form.housedbMetadataTemplateId = $event"
      )

      details.metadata-advanced
        summary.metadata-advanced__summary Datos tecnicos de integracion con Kiwi
        MetadataFieldEditor(
          v-model="kiwiMetadataEntries"
          :catalogValue="form.kiwiMetadataTemplateId"
          title="Datos tecnicos de integracion"
          subtitle="Usa esta seccion solo cuando necesites guardar atributos tecnicos para Kiwi."
          help="Este bloque es para integracion: marca, modelo, serie, tracking o claves que otros sistemas entiendan."
          catalogLabel="Plantilla sugerida Kiwi"
          :catalogOptions="kiwiMetadataOptions"
          :suggestions="kiwiSuggestions"
          :advanced="true"
          @update:catalogValue="form.kiwiMetadataTemplateId = $event"
        )

      button.primary-button(type="submit" :disabled="loading.create") Guardar objeto
      p.form-feedback(v-if="feedback.create || feedback.locations || feedback.metadataTemplates")
        | {{ feedback.create || feedback.locations || feedback.metadataTemplates }}
</template>
