const PROD_DEFAULT_BASE_URL = 'http://localhost:8080'

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
}

export function normalizeApiError(error) {
  if (error instanceof HouseDbApiError) {
    return {
      message: error.message,
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
