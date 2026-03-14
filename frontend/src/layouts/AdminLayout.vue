<script setup>
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'

import { useSessionStore } from '../stores/session'

const route = useRoute()
const router = useRouter()
const { isAuthenticated, sessionSecondsLeft, state, logout } = useSessionStore()

const navigationGroups = [
  {
    label: 'Inicio',
    items: [
      {
        label: 'Inicio',
        to: '/',
        icon: 'IN',
      },
    ],
  },
  {
    label: 'Objetos',
    items: [
      {
        label: 'Lista',
        to: '/objetos',
        icon: 'LS',
      },
      {
        label: 'Nuevo',
        to: '/objetos/nuevo',
        icon: 'NV',
      },
    ],
  },
  {
    label: 'Espacios',
    items: [
      {
        label: 'Casas',
        to: '/casas',
        icon: 'CA',
      },
      {
        label: 'Locaciones',
        to: '/locaciones',
        icon: 'LC',
      },
    ],
  },
]

const currentSection = computed(() => route.meta.section ?? 'Panel principal')
const currentDateLabel = computed(() =>
  new Intl.DateTimeFormat('es-MX', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
  }).format(new Date()),
)
const userInitial = computed(() => (state.username || 'H').slice(0, 1).toUpperCase())

function handleLogout() {
  logout()
  router.push({ name: 'login' })
}
</script>

<template lang="pug">
.app-shell
  aside.app-sidebar.animate__animated.animate__fadeInLeft
    RouterLink.sidebar-brand(to="/")
      .sidebar-brand__icon
        span H
      .sidebar-brand__text
        strong HouseDB
        span Object memory

    hr.sidebar-divider

    nav.sidebar-nav(aria-label="Principal")
      template(v-for="group in navigationGroups" :key="group.label")
        p.sidebar-heading {{ group.label }}
        RouterLink.sidebar-link(
          v-for="item in group.items"
          :key="item.to"
          :to="item.to"
        )
          span.sidebar-link__icon {{ item.icon }}
          span.sidebar-link__text {{ item.label }}

    .sidebar-cta
      p.sidebar-heading Sesion
      strong.sidebar-cta__title {{ isAuthenticated ? 'Activa y protegida' : 'Pendiente de acceso' }}
      p.sidebar-cta__text
        | {{ isAuthenticated ? `Trabajando como ${state.username || 'usuario actual'}.` : 'Inicia sesion para ver casas, objetos y movimientos.' }}
      RouterLink.split-button.split-button--success(to="/objetos/nuevo")
        span.split-button__icon +
        span.split-button__text Registrar objeto

  .app-content
    header.topbar.animate__animated.animate__fadeInDown
      .topbar__left
        p.topbar__eyebrow House inventory console
        h1.topbar__title {{ route.meta.title }}
        p.topbar__subtitle {{ currentSection }}

      .topbar__right
        .topbar-chip
          span.topbar-chip__label Hoy
          strong {{ currentDateLabel }}
        .topbar-chip(v-if="isAuthenticated && sessionSecondsLeft !== null")
          span.topbar-chip__label Token
          strong {{ sessionSecondsLeft }}s
        .topbar-user(v-if="isAuthenticated")
          .topbar-user__avatar {{ userInitial }}
          .topbar-user__meta
            strong {{ state.username || 'Usuario HouseDB' }}
            span Sesion operativa
        RouterLink.ghost-button(to="/objetos") Ver objetos
        button.primary-button.primary-button--danger(v-if="isAuthenticated" type="button" @click="handleLogout") Cerrar sesion

    main.page-content.animate__animated.animate__fadeIn
      RouterView
</template>
