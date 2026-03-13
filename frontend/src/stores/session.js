import { computed, reactive } from 'vue'

import { HouseDbApiClient, normalizeApiError } from '../lib/api'

const STORAGE_KEY = 'housedb.frontend.session'

function readPersistedState() {
  if (typeof window === 'undefined') {
    return { token: '', tokenType: '', expiresIn: null, username: '' }
  }

  try {
    const parsed = JSON.parse(window.localStorage.getItem(STORAGE_KEY) ?? '{}')
    return {
      token: parsed.token ?? '',
      tokenType: parsed.tokenType ?? '',
      expiresIn: parsed.expiresIn ?? null,
      username: parsed.username ?? '',
    }
  } catch {
    return { token: '', tokenType: '', expiresIn: null, username: '' }
  }
}

const persisted = readPersistedState()

const state = reactive({
  token: persisted.token,
  tokenType: persisted.tokenType,
  expiresIn: persisted.expiresIn,
  username: persisted.username,
  loading: false,
  error: '',
})

function persist() {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      token: state.token,
      tokenType: state.tokenType,
      expiresIn: state.expiresIn,
      username: state.username,
    }),
  )
}

export const api = new HouseDbApiClient({
  getToken: () => state.token,
})

export function useSessionStore() {
  const isAuthenticated = computed(() => Boolean(state.token))

  async function login(credentials) {
    state.loading = true
    state.error = ''

    try {
      const response = await api.login(credentials)
      state.token = response.access_token ?? ''
      state.tokenType = response.token_type ?? 'Bearer'
      state.expiresIn = response.expires_in ?? null
      state.username = credentials.username
      persist()
      return response
    } catch (error) {
      const normalized = normalizeApiError(error)
      state.error = normalized.message
      throw normalized
    } finally {
      state.loading = false
    }
  }

  function logout() {
    state.token = ''
    state.tokenType = ''
    state.expiresIn = null
    state.username = ''
    state.error = ''
    persist()
  }

  return {
    state,
    api,
    isAuthenticated,
    login,
    logout,
  }
}
