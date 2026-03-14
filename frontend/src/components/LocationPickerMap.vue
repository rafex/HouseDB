<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png'
import markerIcon from 'leaflet/dist/images/marker-icon.png'
import markerShadow from 'leaflet/dist/images/marker-shadow.png'

import { buildOpenStreetMapUrl } from '../lib/api'

const DEFAULT_LATITUDE = 19.4326
const DEFAULT_LONGITUDE = -99.1332

delete L.Icon.Default.prototype._getIconUrl

L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
})

const props = defineProps({
  latitude: {
    type: Number,
    default: null,
  },
  longitude: {
    type: Number,
    default: null,
  },
  title: {
    type: String,
    default: 'Ubicacion',
  },
})

const emit = defineEmits(['update:latitude', 'update:longitude'])

const mapRoot = ref(null)
const geolocationError = ref('')

let map = null
let marker = null

function normalizedLatitude() {
  const value = Number(props.latitude)
  return Number.isFinite(value) ? value : null
}

function normalizedLongitude() {
  const value = Number(props.longitude)
  return Number.isFinite(value) ? value : null
}

const hasCoordinates = computed(
  () => normalizedLatitude() !== null && normalizedLongitude() !== null,
)

const mapLink = computed(() => buildOpenStreetMapUrl(normalizedLatitude(), normalizedLongitude()))

function roundCoordinate(value) {
  return Math.round(value * 1_000_000) / 1_000_000
}

function updateMarker(latitude, longitude, { pan = true } = {}) {
  if (!map) {
    return
  }

  const target = [latitude, longitude]

  if (!marker) {
    marker = L.marker(target).addTo(map)
  } else {
    marker.setLatLng(target)
  }

  if (pan) {
    map.setView(target, Math.max(map.getZoom(), 16))
  }
}

function clearMarker() {
  if (marker && map) {
    map.removeLayer(marker)
    marker = null
  }
}

function syncFromProps({ pan = false } = {}) {
  const latitude = normalizedLatitude()
  const longitude = normalizedLongitude()

  if (latitude !== null && longitude !== null) {
    updateMarker(latitude, longitude, { pan })
    return
  }

  clearMarker()
}

function setCoordinates(latitude, longitude, options = {}) {
  emit('update:latitude', roundCoordinate(latitude))
  emit('update:longitude', roundCoordinate(longitude))
  updateMarker(latitude, longitude, options)
}

function useCurrentLocation() {
  geolocationError.value = ''

  if (!navigator.geolocation) {
    geolocationError.value = 'Tu navegador no soporta geolocalizacion.'
    return
  }

  navigator.geolocation.getCurrentPosition(
    ({ coords }) => {
      setCoordinates(coords.latitude, coords.longitude, { pan: true })
    },
    () => {
      geolocationError.value = 'No se pudo obtener tu ubicacion actual.'
    },
    {
      enableHighAccuracy: true,
      timeout: 10_000,
    },
  )
}

function requestInitialLocation() {
  if (!hasCoordinates.value) {
    useCurrentLocation()
  }
}

function clearCoordinates() {
  emit('update:latitude', null)
  emit('update:longitude', null)
  geolocationError.value = ''
  if (map) {
    map.setView([DEFAULT_LATITUDE, DEFAULT_LONGITUDE], 5)
  }
  clearMarker()
}

onMounted(() => {
  if (!mapRoot.value) {
    return
  }

  map = L.map(mapRoot.value, {
    zoomControl: true,
  }).setView([DEFAULT_LATITUDE, DEFAULT_LONGITUDE], 5)

  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; OpenStreetMap contributors',
  }).addTo(map)

  map.on('click', (event) => {
    setCoordinates(event.latlng.lat, event.latlng.lng, { pan: false })
  })

  syncFromProps({ pan: true })

  nextTick(() => {
    window.setTimeout(() => {
      map?.invalidateSize()
      requestInitialLocation()
    }, 150)
  })
})

watch(
  () => [props.latitude, props.longitude],
  () => {
    syncFromProps({ pan: false })
  },
)

onBeforeUnmount(() => {
  if (map) {
    map.remove()
    map = null
    marker = null
  }
})
</script>

<template lang="pug">
.location-picker
  .location-picker__header
    div
      p.section-label {{ title }}
      p.muted-copy Haz click en el mapa para ajustar el punto o usa tu ubicacion actual.
    .location-picker__actions
      button.ghost-button(type="button" @click="useCurrentLocation") Usar mi ubicacion
      button.ghost-button(type="button" @click="clearCoordinates" :disabled="!hasCoordinates") Limpiar punto
      a.ghost-button(
        v-if="mapLink"
        :href="mapLink"
        target="_blank"
        rel="noreferrer"
      ) Ver en OSM

  .location-picker__map(ref="mapRoot")
  p.form-feedback.form-feedback--error(v-if="geolocationError") {{ geolocationError }}
</template>

<style scoped>
.location-picker {
  display: grid;
  gap: 0.75rem;
}

.location-picker__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
  flex-wrap: wrap;
}

.location-picker__actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.location-picker__map {
  min-height: 20rem;
  border-radius: 1rem;
  overflow: hidden;
  border: 1px solid var(--color-border);
  box-shadow: inset 0 0 0 1px rgba(78, 115, 223, 0.04);
}

.location-picker :deep(.leaflet-container) {
  width: 100%;
  height: 100%;
  background: #d6eef8;
}

.location-picker :deep(.leaflet-container img),
.location-picker :deep(.leaflet-container svg),
.location-picker :deep(.leaflet-container canvas) {
  max-width: none !important;
  max-height: none !important;
}

.location-picker :deep(.leaflet-marker-icon),
.location-picker :deep(.leaflet-marker-shadow),
.location-picker :deep(.leaflet-tile) {
  display: block;
}
</style>
