import { createRouter, createWebHistory } from 'vue-router'

import AdminLayout from '../layouts/AdminLayout.vue'
import AddObjectView from '../views/AddObjectView.vue'
import DashboardView from '../views/DashboardView.vue'
import HousesView from '../views/HousesView.vue'
import InventoryView from '../views/InventoryView.vue'
import LoginView from '../views/LoginView.vue'
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
        name: 'houses',
        component: HousesView,
        meta: {
          title: 'Espacios',
          section: 'Casas y locaciones como contexto',
          requiresAuth: true,
        },
      },
      {
        path: 'objetos',
        name: 'inventory',
        component: InventoryView,
        meta: {
          title: 'Objetos',
          section: 'Buscar, ubicar y mover objetos',
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
