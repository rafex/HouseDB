const PROD_DEFAULT_BASE_URL = 'https://housedb.v1.rafex.cloud'

function getBaseUrl() {
  if (import.meta.env.DEV) {
    return '/api'
  }

  return import.meta.env.VITE_API_BASE_URL || PROD_DEFAULT_BASE_URL
}

function withQuery(path, query) {
  const params = new URLSearchParams()

  Object.entries(query ?? {}).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return
    }

    params.set(key, String(value))
  })

  const queryString = params.toString()
  return queryString ? `${path}?${queryString}` : path
}

export function createPaginationState(limit = 10, offset = 0) {
  return {
    limit,
    offset,
    returned: 0,
    hasMore: false,
    previousOffset: null,
    nextOffset: null,
  }
}

export function paginationFromResponse(response, fallback = {}) {
  const pagination = response?.pagination ?? {}

  return {
    limit:
      typeof pagination.limit === 'number'
        ? pagination.limit
        : (fallback.limit ?? 0),
    offset:
      typeof pagination.offset === 'number'
        ? pagination.offset
        : (fallback.offset ?? 0),
    returned:
      typeof pagination.returned === 'number'
        ? pagination.returned
        : (typeof response?.count === 'number' ? response.count : (fallback.returned ?? 0)),
    hasMore:
      typeof pagination.hasMore === 'boolean'
        ? pagination.hasMore
        : Boolean(fallback.hasMore),
    previousOffset:
      typeof pagination.previousOffset === 'number'
        ? pagination.previousOffset
        : null,
    nextOffset:
      typeof pagination.nextOffset === 'number'
        ? pagination.nextOffset
        : null,
  }
}

export function buildOpenStreetMapUrl(latitude, longitude) {
  const normalizedLatitude = Number(latitude)
  const normalizedLongitude = Number(longitude)

  if (!Number.isFinite(normalizedLatitude) || !Number.isFinite(normalizedLongitude)) {
    return ''
  }

  return `https://www.openstreetmap.org/?mlat=${normalizedLatitude}&mlon=${normalizedLongitude}#map=18/${normalizedLatitude}/${normalizedLongitude}`
}

export class HouseDbApiError extends Error {
  constructor(message, { status, data } = {}) {
    super(message)
    this.name = 'HouseDbApiError'
    this.status = status
    this.data = data
  }
}

export class HouseDbApiClient {
  constructor({ getToken } = {}) {
    this.baseUrl = getBaseUrl().replace(/\/$/, '')
    this.getToken = getToken ?? (() => '')
  }

  async request(path, { method = 'GET', body, auth = true } = {}) {
    const headers = {
      Accept: 'application/json',
    }

    if (body !== undefined) {
      headers['Content-Type'] = 'application/json'
    }

    if (auth) {
      const token = this.getToken()
      if (token) {
        headers.Authorization = `Bearer ${token}`
      }
    }

    const response = await fetch(`${this.baseUrl}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })

    const text = await response.text()
    let data = null

    if (text) {
      try {
        data = JSON.parse(text)
      } catch {
        data = text
      }
    }

    if (!response.ok) {
      const message =
        data?.message || data?.error || `HTTP ${response.status} ${response.statusText}`
      throw new HouseDbApiError(message, {
        status: response.status,
        data,
      })
    }

    return data
  }

  health() {
    return this.request('/health', { auth: false })
  }

  hello() {
    return this.request('/hello', { auth: false })
  }

  login(payload) {
    return this.request('/auth/login', {
      method: 'POST',
      body: payload,
      auth: false,
    })
  }

  refreshSession(refreshToken) {
    return this.request('/auth/refresh', {
      method: 'POST',
      body: refreshToken ? { refreshToken } : undefined,
      auth: false,
    })
  }

  clientToken(payload) {
    return this.request('/auth/token', {
      method: 'POST',
      body: payload,
      auth: false,
    })
  }

  listItems(query) {
    return this.request(withQuery('/items', query))
  }

  searchItems(query) {
    return this.request(withQuery('/items/search', query))
  }

  listItemsByLocation(query) {
    return this.request(withQuery('/items/by-location', query))
  }

  nearbyItems(query) {
    return this.request(withQuery('/items/nearby', query))
  }

  getItem(inventoryItemId) {
    return this.request(`/items/${inventoryItemId}`)
  }

  createItem(payload) {
    return this.request('/items', {
      method: 'POST',
      body: payload,
    })
  }

  moveItem(inventoryItemId, payload) {
    return this.request(`/items/${inventoryItemId}/move`, {
      method: 'PATCH',
      body: payload,
    })
  }

  listItemTimeline(inventoryItemId, query) {
    return this.request(withQuery(`/items/${inventoryItemId}/timeline`, query))
  }

  setFavorite(inventoryItemId, payload) {
    return this.request(`/items/${inventoryItemId}/favorite`, {
      method: 'PUT',
      body: payload,
    })
  }

  listHouses(query) {
    return this.request(withQuery('/houses', query))
  }

  listHouseIds(query) {
    return this.request(withQuery('/houses/ids', query))
  }

  createHouse(payload) {
    return this.request('/houses', {
      method: 'POST',
      body: payload,
    })
  }

  listHouseMembers(houseId, query) {
    return this.request(withQuery(`/houses/${houseId}/members`, query))
  }

  listHouseLocations(houseId, query) {
    return this.request(withQuery(`/houses/${houseId}/locations`, query))
  }

  upsertHouseMember(houseId, payload, method = 'POST') {
    return this.request(`/houses/${houseId}/members`, {
      method,
      body: payload,
    })
  }

  createHouseLocation(houseId, payload) {
    return this.request(`/houses/${houseId}/locations`, {
      method: 'POST',
      body: payload,
    })
  }

  createUser(payload) {
    return this.request('/users', {
      method: 'POST',
      body: payload,
    })
  }

  listMetadataCatalogs(query) {
    return this.request(withQuery('/metadata-catalogs', query))
  }

  createMetadataCatalog(payload) {
    return this.request('/metadata-catalogs', {
      method: 'POST',
      body: payload,
    })
  }

  listMetadataTemplates(query) {
    return this.request(withQuery('/metadata-templates', query))
  }

  createMetadataTemplate(payload) {
    return this.request('/metadata-templates', {
      method: 'POST',
      body: payload,
    })
  }
}

export function normalizeApiError(error) {
  if (error instanceof HouseDbApiError) {
    const code = error.data?.code
    const serverMessage = error.data?.message || error.data?.error
    let message = serverMessage || `HTTP ${error.status ?? ''}`.trim()

    if (code === 'bad_credentials') {
      message = 'Usuario o password incorrectos.'
    } else if (error.status === 401 && serverMessage === 'unauthorized') {
      message = 'Tu sesion no es valida o ya vencio.'
    } else if (serverMessage === 'invalid_refresh_token') {
      message = 'La sesion ya no se puede renovar. Ingresa nuevamente.'
    } else if (serverMessage === 'database error') {
      message = 'El backend reporto un error de base de datos durante la autenticacion.'
    }

    return {
      message,
      status: error.status,
      detail: error.data,
    }
  }

  return {
    message: error?.message || 'Ocurrio un error inesperado.',
    status: null,
    detail: null,
  }
}
