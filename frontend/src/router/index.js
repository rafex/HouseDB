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

const routes = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: {
      title: 'Login',
      publicOnly: true,
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
  document.title = `${to.meta.title} | HouseDB`
})

export default router
