<script setup>
import { computed } from 'vue'

const props = defineProps({
  count: {
    type: Number,
    default: 0,
  },
  limit: {
    type: Number,
    required: true,
  },
  offset: {
    type: Number,
    required: true,
  },
  hasMore: {
    type: Boolean,
    default: false,
  },
  previousOffset: {
    type: Number,
    default: null,
  },
  nextOffset: {
    type: Number,
    default: null,
  },
  loading: {
    type: Boolean,
    default: false,
  },
  limitOptions: {
    type: Array,
    default: () => [8, 12, 20, 50, 100],
  },
})

const emit = defineEmits(['change', 'limit-change'])

const currentPage = computed(() => Math.floor(props.offset / props.limit) + 1)
const startRow = computed(() => (props.count === 0 ? 0 : props.offset + 1))
const endRow = computed(() => props.offset + props.count)
const canGoPrevious = computed(() => props.previousOffset !== null || props.offset > 0)
const canGoNext = computed(() => props.nextOffset !== null || props.hasMore)
const normalizedLimit = computed(() =>
  props.limitOptions.includes(props.limit) ? String(props.limit) : String(props.limitOptions[0]),
)

function goTo(offset) {
  emit('change', Math.max(0, offset))
}

function goPrevious() {
  goTo(props.previousOffset ?? (props.offset - props.limit))
}

function goNext() {
  goTo(props.nextOffset ?? (props.offset + props.limit))
}

function updateLimit(value) {
  const nextLimit = Number.parseInt(value, 10)
  if (Number.isFinite(nextLimit) && nextLimit > 0) {
    emit('limit-change', nextLimit)
  }
}
</script>

<template lang="pug">
.pagination-controls
  .pagination-controls__summary
    p.pagination-controls__range
      | {{ startRow }}-{{ endRow }}
    label.pagination-controls__page-size
      span Mostrar
      select.form-input.pagination-controls__select(
        :value="normalizedLimit"
        @change="updateLimit($event.target.value)"
        :disabled="loading"
      )
        option(v-for="option in limitOptions" :key="option" :value="option")
          | {{ option }}

  .pagination-controls__actions
    button.ghost-button(
      type="button"
      @click="goPrevious"
      :disabled="loading || !canGoPrevious"
    ) Anterior
    span.pagination-controls__page Pagina {{ currentPage }}
    button.ghost-button(
      type="button"
      @click="goNext"
      :disabled="loading || !canGoNext"
    ) Siguiente
</template>
