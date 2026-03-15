import { createRouter, createWebHistory } from 'vue-router'

import AdminLayout from '../layouts/AdminLayout.vue'
import AddObjectView from '../views/AddObjectView.vue'
import AddHouseView from '../views/AddHouseView.vue'
import AddLocationView from '../views/AddLocationView.vue'
import DashboardView from '../views/DashboardView.vue'
import HousesListView from '../views/HousesListView.vue'
import LoginView from '../views/LoginView.vue'
import LocationsListView from '../views/LocationsListView.vue'
import ObjectListView from '../views/ObjectListView.vue'
import UsersView from '../views/UsersView.vue'
import { useSessionStore } from '../stores/session'

const SITE_NAME = 'HouseDB'
const SITE_URL = (import.meta.env.VITE_SITE_URL || 'https://housedb.v1.rafex.cloud').replace(/\/$/, '')

function upsertMeta(selector, attributes) {
  let element = document.head.querySelector(selector)

  if (!element) {
    element = document.createElement('meta')
    document.head.appendChild(element)
  }

  Object.entries(attributes).forEach(([key, value]) => {
    element.setAttribute(key, value)
  })
}

function upsertLink(selector, attributes) {
  let element = document.head.querySelector(selector)

  if (!element) {
    element = document.createElement('link')
    document.head.appendChild(element)
  }

  Object.entries(attributes).forEach(([key, value]) => {
    element.setAttribute(key, value)
  })
}

function applySeo(to) {
  const title = to.meta.title ? `${to.meta.title} | ${SITE_NAME}` : SITE_NAME
  const description =
    to.meta.description ||
    'HouseDB te ayuda a recordar que objetos tienes en casa, donde estan y como encontrarlos rapido.'
  const robots = to.meta.robots || 'noindex,nofollow'
  const canonical = `${SITE_URL}${to.path || '/'}`

  document.documentElement.lang = 'es'
  document.title = title

  upsertMeta('meta[name="description"]', { name: 'description', content: description })
  upsertMeta('meta[name="robots"]', { name: 'robots', content: robots })
  upsertMeta('meta[property="og:title"]', { property: 'og:title', content: title })
  upsertMeta('meta[property="og:description"]', { property: 'og:description', content: description })
  upsertMeta('meta[property="og:type"]', { property: 'og:type', content: 'website' })
  upsertMeta('meta[property="og:url"]', { property: 'og:url', content: canonical })
  upsertMeta('meta[property="og:site_name"]', { property: 'og:site_name', content: SITE_NAME })
  upsertMeta('meta[name="twitter:card"]', { name: 'twitter:card', content: 'summary_large_image' })
  upsertMeta('meta[name="twitter:title"]', { name: 'twitter:title', content: title })
  upsertMeta('meta[name="twitter:description"]', { name: 'twitter:description', content: description })
  upsertLink('link[rel="canonical"]', { rel: 'canonical', href: canonical })
}

const routes = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: {
      title: 'Login',
      publicOnly: true,
      description: 'Accede a HouseDB para consultar, registrar y ubicar objetos dentro de tu casa.',
      robots: 'index,follow',
    },
  },
  {
    path: '/',
    component: AdminLayout,
    meta: {
      requiresAuth: true,
    },
    children: [
      {
        path: '',
        name: 'dashboard',
        component: DashboardView,
        meta: {
          title: 'Dashboard',
          section: 'Resumen general',
          requiresAuth: true,
          description: 'Resumen operativo de HouseDB con acceso rapido a objetos, casas y ubicaciones.',
        },
      },
      {
        path: 'casas',
        name: 'spaces-houses',
        component: HousesListView,
        meta: {
          title: 'Casas',
          section: 'Espacios > Casas',
          requiresAuth: true,
          description: 'Consulta las casas registradas y abre su contexto de ubicaciones dentro de HouseDB.',
        },
      },
      {
        path: 'objetos',
        name: 'objects-list',
        component: ObjectListView,
        meta: {
          title: 'Lista de objetos',
          section: 'Objetos > Lista',
          requiresAuth: true,
          description: 'Busca y revisa objetos guardados en HouseDB para saber si los tienes y donde estan.',
        },
      },
      {
        path: 'objetos/nuevo',
        name: 'add-object',
        component: AddObjectView,
        meta: {
          title: 'Nuevo objeto',
          section: 'Alta guiada de un objeto',
          requiresAuth: true,
          description: 'Registra un objeto con ubicacion, atributos y metadata reutilizable para encontrarlo despues.',
        },
      },
      {
        path: 'casas/nueva',
        name: 'add-house',
        component: AddHouseView,
        meta: {
          title: 'Nueva casa',
          section: 'Espacios > Casas > Agregar',
          requiresAuth: true,
          description: 'Agrega una casa a HouseDB para empezar a organizar objetos y locaciones.',
        },
      },
      {
        path: 'locaciones',
        name: 'spaces-locations',
        component: LocationsListView,
        meta: {
          title: 'Locaciones',
          section: 'Espacios > Locaciones',
          requiresAuth: true,
          description: 'Explora locaciones y estructura espacios fisicos donde viven tus objetos.',
        },
      },
      {
        path: 'locaciones/nueva',
        name: 'add-location',
        component: AddLocationView,
        meta: {
          title: 'Nueva locacion',
          section: 'Espacios > Locaciones > Agregar',
          requiresAuth: true,
          description: 'Crea una nueva locacion dentro de una casa para ubicar objetos con mayor precision.',
        },
      },
      {
        path: 'usuarios-api',
        name: 'users-api',
        component: UsersView,
        meta: {
          title: 'Acceso y API',
          section: 'Usuarios, tokens y administracion tecnica',
          requiresAuth: true,
          description: 'Administra usuarios, tokens, catalogos y plantillas de metadata reutilizable en HouseDB.',
        },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to) => {
  const { isAuthenticated } = useSessionStore()

  if (to.meta.requiresAuth && !isAuthenticated.value) {
    return {
      name: 'login',
      query: {
        redirect: to.fullPath,
      },
    }
  }

  if (to.meta.publicOnly && isAuthenticated.value) {
    return { name: 'dashboard' }
  }
})

router.afterEach((to) => {
  applySeo(to)
})

export default router
