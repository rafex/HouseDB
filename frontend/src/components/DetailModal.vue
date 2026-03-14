<script setup>
defineProps({
  title: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['close'])

function close() {
  emit('close')
}
</script>

<template lang="pug">
.detail-modal(role="dialog" aria-modal="true" @click.self="close")
  .detail-modal__backdrop(@click="close")
  .detail-modal__card.panel-card.animate__animated.animate__fadeInUp
    .detail-modal__header
      div
        p.panel-card__eyebrow Detalle
        h2.detail-modal__title {{ title }}
      button.circle-button.circle-button--ghost(type="button" aria-label="Cerrar detalle" @click="close") x
    .detail-modal__content
      slot
</template>
