<script setup>
import { computed, reactive, watch } from 'vue'

const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  subtitle: {
    type: String,
    default: '',
  },
  help: {
    type: String,
    default: '',
  },
  advanced: {
    type: Boolean,
    default: false,
  },
  catalogLabel: {
    type: String,
    default: '',
  },
  catalogValue: {
    type: String,
    default: '',
  },
  catalogOptions: {
    type: Array,
    default: () => [],
  },
  suggestions: {
    type: Array,
    default: () => [],
  },
  modelValue: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue', 'update:catalogValue'])

const draft = reactive({
  key: '',
  value: '',
})

const normalizedRows = computed(() => props.modelValue ?? [])
const suggestionMap = computed(() =>
  Object.fromEntries(
    (props.suggestions ?? []).map((suggestion) => [
      suggestion.key,
      {
        placeholder: suggestion.placeholder ?? '',
        hint: suggestion.hint ?? '',
      },
    ]),
  ),
)

function emitRows(rows) {
  emit('update:modelValue', rows)
}

function appendRow(key = '', value = '') {
  const safeKey = key.trim()
  const safeValue = value.trim()
  if (!safeKey) {
    return
  }

  const existingIndex = normalizedRows.value.findIndex((row) => row.key === safeKey)
  if (existingIndex >= 0) {
    const next = normalizedRows.value.map((row, index) =>
      index === existingIndex
        ? {
            ...row,
            value: safeValue || row.value,
          }
        : row,
    )
    emitRows(next)
    return
  }

  emitRows([
    ...normalizedRows.value,
    {
      key: safeKey,
      value: safeValue,
    },
  ])
}

function addDraftRow() {
  appendRow(draft.key, draft.value)
  draft.key = ''
  draft.value = ''
}

function addSuggestion(suggestion) {
  appendRow(suggestion.key, suggestion.defaultValue ?? '')
}

function updateRow(index, field, value) {
  const next = normalizedRows.value.map((row, rowIndex) =>
    rowIndex === index
      ? {
          ...row,
          [field]: value,
        }
      : row,
  )
  emitRows(next)
}

function removeRow(index) {
  emitRows(normalizedRows.value.filter((_, rowIndex) => rowIndex !== index))
}

watch(
  () => props.catalogValue,
  () => {
    draft.key = ''
    draft.value = ''
  },
)
</script>

<template lang="pug">
article.panel-card.metadata-editor(:class="{ 'metadata-editor--advanced': advanced }")
  .panel-card__header
    div
      p.panel-card__eyebrow {{ advanced ? 'Avanzado' : 'Sugerido' }}
      h3.panel-card__title {{ title }}
      p.muted-copy(v-if="subtitle") {{ subtitle }}
    span.help-bubble(v-if="help" :title="help") ?

  .metadata-editor__toolbar
    label.metadata-editor__catalog(v-if="catalogOptions.length > 0")
      span.section-label {{ catalogLabel }}
      select.form-input(
        :value="catalogValue"
        @change="$emit('update:catalogValue', $event.target.value)"
      )
        option(value="") Sin plantilla
        option(v-for="option in catalogOptions" :key="option.value" :value="option.value")
          | {{ option.label }}

  .metadata-editor__suggestions(v-if="suggestions.length > 0")
    p.section-label Claves sugeridas
    .metadata-editor__chips
      button.metadata-chip(
        v-for="suggestion in suggestions"
        :key="suggestion.key"
        type="button"
        @click="addSuggestion(suggestion)"
        :title="suggestion.hint || suggestion.placeholder || suggestion.key"
      )
        | {{ suggestion.label || suggestion.key }}

  .metadata-editor__rows(v-if="normalizedRows.length > 0")
    article.metadata-row(v-for="(row, index) in normalizedRows" :key="`${row.key}-${index}`")
      input.form-input(
        :value="row.key"
        placeholder="Clave"
        @input="updateRow(index, 'key', $event.target.value)"
      )
      input.form-input(
        :value="row.value"
        :placeholder="suggestionMap[row.key]?.placeholder || 'Valor'"
        @input="updateRow(index, 'value', $event.target.value)"
      )
      button.ghost-button(type="button" @click="removeRow(index)") Quitar
      p.muted-copy(v-if="suggestionMap[row.key]?.hint") {{ suggestionMap[row.key].hint }}

  .metadata-editor__custom
    p.section-label Agregar dato personalizado
    .metadata-row
      input.form-input(v-model="draft.key" placeholder="Ej. brand, material, vendor")
      input.form-input(v-model="draft.value" placeholder="Ej. Sony, metal, Amazon")
      button.primary-button(type="button" @click="addDraftRow") Agregar

  p.muted-copy(v-if="normalizedRows.length === 0")
    | Aun no agregas datos. Usa una clave sugerida o crea un dato personalizado.
</template>
