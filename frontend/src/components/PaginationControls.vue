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
})

const emit = defineEmits(['change'])

const currentPage = computed(() => Math.floor(props.offset / props.limit) + 1)
const startRow = computed(() => (props.count === 0 ? 0 : props.offset + 1))
const endRow = computed(() => props.offset + props.count)
const canGoPrevious = computed(() => props.previousOffset !== null || props.offset > 0)
const canGoNext = computed(() => props.nextOffset !== null || props.hasMore)

function goTo(offset) {
  emit('change', Math.max(0, offset))
}

function goPrevious() {
  goTo(props.previousOffset ?? (props.offset - props.limit))
}

function goNext() {
  goTo(props.nextOffset ?? (props.offset + props.limit))
}
</script>

<template lang="pug">
.pagination-controls
  p.pagination-controls__summary
    | {{ startRow }}-{{ endRow }}

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
