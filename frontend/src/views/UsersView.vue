<script setup>
import { onMounted, reactive, ref, watch } from 'vue'

import PaginationControls from '../components/PaginationControls.vue'
import { createPaginationState, normalizeApiError, paginationFromResponse } from '../lib/api'
import { useSessionStore } from '../stores/session'

const { api, isAuthenticated } = useSessionStore()

const loading = reactive({
  token: false,
  createUser: false,
  catalogs: false,
  templates: false,
  createCatalog: false,
  createTemplate: false,
})

const feedback = reactive({
  token: '',
  createUser: '',
  catalogs: '',
  templates: '',
  createCatalog: '',
  createTemplate: '',
})

const tokenPreview = ref(null)
const metadataCatalogs = ref([])
const metadataTemplates = ref([])
const catalogPager = reactive(createPaginationState(8))
const templatePager = reactive(createPaginationState(8))

const tokenForm = reactive({
  client_id: '',
  client_secret: '',
  grant_type: 'client_credentials',
})

const userForm = reactive({
  userId: '',
  username: '',
  password: '',
})

const catalogFilters = reactive({
  metadataTarget: '',
})

const templateFilters = reactive({
  metadataTarget: '',
})

const defaultCatalogPayload =
  '[\n  {\n    "key": "brand",\n    "label": "Marca",\n    "placeholder": "Ej. Sony",\n    "hint": "Marca o fabricante del objeto"\n  }\n]'

const defaultTemplateDefinition =
  '[\n  {\n    "key": "brand",\n    "label": "Marca",\n    "placeholder": "Ej. Sony",\n    "hint": "Marca visible del objeto",\n    "defaultValue": ""\n  }\n]'

const catalogForm = reactive({
  metadataTarget: 'inventory_item',
  code: '',
  name: '',
  description: '',
  payloadJson: defaultCatalogPayload,
  enabled: true,
})

const templateForm = reactive({
  metadataTarget: 'inventory_item',
  code: '',
  name: '',
  description: '',
  definitionJson: defaultTemplateDefinition,
  enabled: true,
})

const endpointCatalog = [
  'GET /health',
  'GET /hello',
  'POST /auth/login',
  'POST /auth/refresh',
  'POST /auth/token',
  'GET /items',
  'GET /items/search',
  'GET /items/nearby',
  'GET /items/by-location',
  'GET /items/{inventoryItemId}',
  'PATCH /items/{inventoryItemId}/move',
  'GET /items/{inventoryItemId}/timeline',
  'PUT /items/{inventoryItemId}/favorite',
  'GET /houses',
  'POST /houses',
  'GET /houses/ids',
  'GET|POST|PUT /houses/{houseId}/members',
  'POST /houses/{houseId}/locations',
  'GET|POST /metadata-catalogs',
  'GET|POST /metadata-templates',
  'POST /users',
]

async function requestClientToken() {
  loading.token = true
  feedback.token = ''

  try {
    tokenPreview.value = await api.clientToken(tokenForm)
    feedback.token = 'Token M2M recibido.'
  } catch (error) {
    feedback.token = normalizeApiError(error).message
  } finally {
    loading.token = false
  }
}

async function createUser() {
  loading.createUser = true
  feedback.createUser = ''

  try {
    const response = await api.createUser({
      userId: userForm.userId || undefined,
      username: userForm.username,
      password: userForm.password,
    })

    feedback.createUser = `Usuario creado: ${response.userId}`
    userForm.userId = ''
    userForm.username = ''
    userForm.password = ''
  } catch (error) {
    feedback.createUser = normalizeApiError(error).message
  } finally {
    loading.createUser = false
  }
}

async function loadMetadataCatalogs() {
  if (!isAuthenticated.value) {
    metadataCatalogs.value = []
    return
  }

  loading.catalogs = true
  feedback.catalogs = ''

  try {
    const response = await api.listMetadataCatalogs({
      metadataTarget: catalogFilters.metadataTarget || undefined,
      limit: catalogPager.limit,
      offset: catalogPager.offset,
    })
    metadataCatalogs.value = response.catalogs ?? []
    Object.assign(catalogPager, paginationFromResponse(response, catalogPager))
  } catch (error) {
    feedback.catalogs = normalizeApiError(error).message
  } finally {
    loading.catalogs = false
  }
}

async function loadMetadataTemplates() {
  if (!isAuthenticated.value) {
    metadataTemplates.value = []
    return
  }

  loading.templates = true
  feedback.templates = ''

  try {
    const response = await api.listMetadataTemplates({
      metadataTarget: templateFilters.metadataTarget || undefined,
      limit: templatePager.limit,
      offset: templatePager.offset,
    })
    metadataTemplates.value = response.templates ?? []
    Object.assign(templatePager, paginationFromResponse(response, templatePager))
  } catch (error) {
    feedback.templates = normalizeApiError(error).message
  } finally {
    loading.templates = false
  }
}

async function createMetadataCatalog() {
  loading.createCatalog = true
  feedback.createCatalog = ''

  try {
    await api.createMetadataCatalog({
      metadataTarget: catalogForm.metadataTarget,
      code: catalogForm.code,
      name: catalogForm.name,
      description: catalogForm.description || undefined,
      payloadJson: catalogForm.payloadJson || undefined,
      enabled: catalogForm.enabled,
    })

    Object.assign(catalogForm, {
      metadataTarget: catalogForm.metadataTarget,
      code: '',
      name: '',
      description: '',
      payloadJson: defaultCatalogPayload,
      enabled: true,
    })
    feedback.createCatalog = 'Catalogo creado.'
    catalogPager.offset = 0
    await loadMetadataCatalogs()
  } catch (error) {
    feedback.createCatalog = normalizeApiError(error).message
  } finally {
    loading.createCatalog = false
  }
}

async function createMetadataTemplate() {
  loading.createTemplate = true
  feedback.createTemplate = ''

  try {
    await api.createMetadataTemplate({
      metadataTarget: templateForm.metadataTarget,
      code: templateForm.code,
      name: templateForm.name,
      description: templateForm.description || undefined,
      definitionJson: templateForm.definitionJson || undefined,
      enabled: templateForm.enabled,
    })

    Object.assign(templateForm, {
      metadataTarget: templateForm.metadataTarget,
      code: '',
      name: '',
      description: '',
      definitionJson: defaultTemplateDefinition,
      enabled: true,
    })
    feedback.createTemplate = 'Plantilla creada.'
    templatePager.offset = 0
    await loadMetadataTemplates()
  } catch (error) {
    feedback.createTemplate = normalizeApiError(error).message
  } finally {
    loading.createTemplate = false
  }
}

onMounted(async () => {
  if (isAuthenticated.value) {
    await Promise.all([loadMetadataCatalogs(), loadMetadataTemplates()])
  }
})

watch(isAuthenticated, (value) => {
  if (value) {
    loadMetadataCatalogs()
    loadMetadataTemplates()
  }
})

watch(() => catalogPager.offset, loadMetadataCatalogs)
watch(() => templatePager.offset, loadMetadataTemplates)
watch(() => catalogPager.limit, () => {
  catalogPager.offset = 0
  loadMetadataCatalogs()
})
watch(() => templatePager.limit, () => {
  templatePager.offset = 0
  loadMetadataTemplates()
})
watch(() => catalogFilters.metadataTarget, () => {
  catalogPager.offset = 0
  loadMetadataCatalogs()
})
watch(() => templateFilters.metadataTarget, () => {
  templatePager.offset = 0
  loadMetadataTemplates()
})
</script>

<template lang="pug">
section.page-section
  article.panel-card
    .panel-card__header
      div
        p.panel-card__eyebrow Tokens
        h3.panel-card__title Operaciones de autenticacion avanzada

    .split-grid
      .form-stack
        form.form-grid.form-grid--compact(@submit.prevent="requestClientToken")
          p.section-label Token M2M
          input.form-input(v-model="tokenForm.client_id" placeholder="client_id" required)
          input.form-input(v-model="tokenForm.client_secret" type="password" placeholder="client_secret" required)
          button.ghost-button(type="submit" :disabled="loading.token") Solicitar token
          p.form-feedback(v-if="feedback.token") {{ feedback.token }}
          pre.code-block(v-if="tokenPreview") {{ JSON.stringify(tokenPreview, null, 2) }}

  article.panel-card
    .panel-card__header
      div
        p.panel-card__eyebrow Administracion
        h3.panel-card__title Alta de usuarios y revision de endpoints protegidos

    .split-grid
      form.form-grid(@submit.prevent="createUser")
        p.section-label Crear usuario
        input.form-input(v-model="userForm.userId" placeholder="userId opcional")
        input.form-input(v-model="userForm.username" placeholder="username" required)
        input.form-input(v-model="userForm.password" type="password" placeholder="password" required)
        button.primary-button(type="submit" :disabled="loading.createUser || !isAuthenticated") Crear usuario
        p.form-feedback(v-if="feedback.createUser") {{ feedback.createUser }}
        p.muted-copy Requiere token app o un usuario con privilegios ADMIN.

      .list-panel
        p.section-label Superficie de API revisada
        ul.compact-list
          li(v-for="endpoint in endpointCatalog" :key="endpoint")
            strong {{ endpoint }}

  article.panel-card
    .panel-card__header
      div
        p.panel-card__eyebrow Metadata
        h3.panel-card__title Catalogos reutilizables

    .split-grid
      form.form-grid(@submit.prevent="createMetadataCatalog")
        p.section-label Crear catalogo
        select.form-input(v-model="catalogForm.metadataTarget")
          option(value="inventory_item") inventory_item
          option(value="kiwi_object") kiwi_object
        input.form-input(v-model="catalogForm.code" placeholder="code" required)
        input.form-input(v-model="catalogForm.name" placeholder="name" required)
        input.form-input(v-model="catalogForm.description" placeholder="description")
        textarea.form-input.form-textarea(v-model="catalogForm.payloadJson" placeholder="payloadJson")
        label.toggle-field
          input(type="checkbox" v-model="catalogForm.enabled")
          span Habilitado
        button.primary-button(type="submit" :disabled="loading.createCatalog || !isAuthenticated") Crear catalogo
        p.form-feedback(v-if="feedback.createCatalog") {{ feedback.createCatalog }}

      .list-panel
        .table-toolbar
          .table-toolbar__search
            select.form-input.table-search-select(v-model="catalogFilters.metadataTarget")
              option(value="") Todos los targets
              option(value="inventory_item") inventory_item
              option(value="kiwi_object") kiwi_object
        p.form-feedback.form-feedback--error(v-if="feedback.catalogs") {{ feedback.catalogs }}
        .table-card
          .table-card__header
            h4.table-card__title Catalogos
            p.muted-copy {{ loading.catalogs ? 'Consultando...' : `${catalogPager.returned} visibles` }}
          table.table-grid(v-if="metadataCatalogs.length > 0")
            thead
              tr
                th Codigo
                th Target
                th Nombre
                th Estado
            tbody
              tr(v-for="catalog in metadataCatalogs" :key="catalog.metadataCatalogId")
                td
                  strong {{ catalog.code }}
                td {{ catalog.metadataTarget }}
                td {{ catalog.name }}
                td {{ catalog.enabled ? 'Activo' : 'Inactivo' }}
          p.empty-copy(v-else-if="loading.catalogs") Cargando catalogos...
          p.empty-copy(v-else) Aun no hay catalogos visibles.
        PaginationControls(
          :count="catalogPager.returned"
          :limit="catalogPager.limit"
          :offset="catalogPager.offset"
          :hasMore="catalogPager.hasMore"
          :previousOffset="catalogPager.previousOffset"
          :nextOffset="catalogPager.nextOffset"
          :loading="loading.catalogs"
          @change="catalogPager.offset = $event"
          @limit-change="catalogPager.limit = $event"
        )

  article.panel-card
    .panel-card__header
      div
        p.panel-card__eyebrow Metadata
        h3.panel-card__title Plantillas reutilizables

    .split-grid
      form.form-grid(@submit.prevent="createMetadataTemplate")
        p.section-label Crear plantilla
        select.form-input(v-model="templateForm.metadataTarget")
          option(value="inventory_item") inventory_item
          option(value="kiwi_object") kiwi_object
        input.form-input(v-model="templateForm.code" placeholder="code" required)
        input.form-input(v-model="templateForm.name" placeholder="name" required)
        input.form-input(v-model="templateForm.description" placeholder="description")
        textarea.form-input.form-textarea(v-model="templateForm.definitionJson" placeholder="definitionJson")
        label.toggle-field
          input(type="checkbox" v-model="templateForm.enabled")
          span Habilitada
        button.primary-button(type="submit" :disabled="loading.createTemplate || !isAuthenticated") Crear plantilla
        p.form-feedback(v-if="feedback.createTemplate") {{ feedback.createTemplate }}

      .list-panel
        .table-toolbar
          .table-toolbar__search
            select.form-input.table-search-select(v-model="templateFilters.metadataTarget")
              option(value="") Todos los targets
              option(value="inventory_item") inventory_item
              option(value="kiwi_object") kiwi_object
        p.form-feedback.form-feedback--error(v-if="feedback.templates") {{ feedback.templates }}
        .table-card
          .table-card__header
            h4.table-card__title Plantillas
            p.muted-copy {{ loading.templates ? 'Consultando...' : `${templatePager.returned} visibles` }}
          table.table-grid(v-if="metadataTemplates.length > 0")
            thead
              tr
                th Codigo
                th Target
                th Nombre
                th Estado
            tbody
              tr(v-for="template in metadataTemplates" :key="template.metadataTemplateId")
                td
                  strong {{ template.code }}
                td {{ template.metadataTarget }}
                td {{ template.name }}
                td {{ template.enabled ? 'Activa' : 'Inactiva' }}
          p.empty-copy(v-else-if="loading.templates") Cargando plantillas...
          p.empty-copy(v-else) Aun no hay plantillas visibles.
        PaginationControls(
          :count="templatePager.returned"
          :limit="templatePager.limit"
          :offset="templatePager.offset"
          :hasMore="templatePager.hasMore"
          :previousOffset="templatePager.previousOffset"
          :nextOffset="templatePager.nextOffset"
          :loading="loading.templates"
          @change="templatePager.offset = $event"
          @limit-change="templatePager.limit = $event"
        )
</template>
