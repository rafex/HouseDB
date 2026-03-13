<script setup>
defineProps({
  title: {
    type: String,
    required: true,
  },
  value: {
    type: String,
    required: true,
  },
  trend: {
    type: String,
    required: true,
  },
  accent: {
    type: String,
    default: 'teal',
  },
})
</script>

<template lang="pug">
article.metric-card(:class="`metric-card--${accent}`")
  p.metric-card__title {{ title }}
  strong.metric-card__value {{ value }}
  p.metric-card__trend {{ trend }}
</template>
