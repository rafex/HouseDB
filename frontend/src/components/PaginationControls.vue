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
  loading: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['change'])

const currentPage = computed(() => Math.floor(props.offset / props.limit) + 1)
const startRow = computed(() => (props.count === 0 ? 0 : props.offset + 1))
const endRow = computed(() => props.offset + props.count)

function goTo(offset) {
  emit('change', Math.max(0, offset))
}
</script>

<template lang="pug">
.pagination-controls
  p.pagination-controls__summary
    | {{ startRow }}-{{ endRow }}

  .pagination-controls__actions
    button.ghost-button(
      type="button"
      @click="goTo(offset - limit)"
      :disabled="loading || offset <= 0"
    ) Anterior
    span.pagination-controls__page Pagina {{ currentPage }}
    button.ghost-button(
      type="button"
      @click="goTo(offset + limit)"
      :disabled="loading || !hasMore"
    ) Siguiente
</template>
