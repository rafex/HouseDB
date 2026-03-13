<script setup>
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'

import { useSessionStore } from '../stores/session'

const route = useRoute()
const router = useRouter()
const { isAuthenticated, state, logout } = useSessionStore()

const navigationItems = [
  {
    label: 'Inicio',
    to: '/',
    icon: 'DB',
  },
  {
    label: 'Objetos',
    to: '/objetos',
    icon: 'OB',
  },
  {
    label: 'Nuevo objeto',
    to: '/objetos/nuevo',
    icon: 'NW',
  },
  {
    label: 'Espacios',
    to: '/casas',
    icon: 'ES',
  },
  {
    label: 'Acceso/API',
    to: '/usuarios-api',
    icon: 'UA',
  },
]

const currentSection = computed(() => route.meta.section ?? 'Panel principal')

function handleLogout() {
  logout()
  router.push({ name: 'login' })
}
</script>

<template lang="pug">
.admin-shell
  aside.sidebar
    .brand
      .brand__mark H
      .brand__copy
        span.brand__eyebrow Suite
        h1.brand__title HouseDB

    nav.sidebar__nav(aria-label="Principal")
      RouterLink.sidebar__link(
        v-for="item in navigationItems"
        :key="item.to"
        :to="item.to"
      )
        span.sidebar__icon {{ item.icon }}
        span {{ item.label }}

    .sidebar__footer
      p.sidebar__label Operacion del dia
      strong.sidebar__value {{ isAuthenticated ? 'Sesion autenticada' : 'Sesion pendiente' }}
      p.sidebar__hint
        | {{ isAuthenticated ? `Token listo para ${state.username || 'usuario actual'}.` : 'Inicia sesion para activar las operaciones protegidas.' }}

  .main-panel
    header.topbar
      .topbar__copy
        p.topbar__eyebrow Plataforma inmobiliaria
        h2.topbar__title {{ route.meta.title }}
        p.topbar__subtitle {{ currentSection }}

      .topbar__actions
        RouterLink.ghost-button(to="/objetos") Ver objetos
        RouterLink.primary-button(v-if="isAuthenticated" to="/objetos/nuevo") Nuevo objeto
        button.primary-button(v-if="isAuthenticated" type="button" @click="handleLogout") Cerrar sesion

    main.content-area
      RouterView
</template>
