import { computed, reactive } from 'vue'

import { HouseDbApiClient, normalizeApiError } from '../lib/api'

const STORAGE_KEY = 'housedb.frontend.session'
const REFRESH_GRACE_MS = 60 * 1000
const REDIRECT_AFTER_EXPIRE_MS = 60 * 1000

let refreshTimer = null
let expiryTimer = null
let redirectTimer = null
let heartbeatTimer = null
let attachedRouter = null

function clearTimer(timer) {
  if (timer) {
    window.clearTimeout(timer)
  }
}

function clearIntervalTimer(timer) {
  if (timer) {
    window.clearInterval(timer)
  }
}

function clearAllTimers() {
  if (typeof window === 'undefined') {
    return
  }

  clearTimer(refreshTimer)
  clearTimer(expiryTimer)
  clearTimer(redirectTimer)
  clearIntervalTimer(heartbeatTimer)
  refreshTimer = null
  expiryTimer = null
  redirectTimer = null
  heartbeatTimer = null
}

function readPersistedState() {
  if (typeof window === 'undefined') {
    return {
      token: '',
      tokenType: '',
      expiresIn: null,
      expiresAt: null,
      refreshToken: '',
      refreshExpiresIn: null,
      refreshExpiresAt: null,
      username: '',
      sessionExpiredAt: null,
      showSessionModal: false,
    }
  }

  try {
    const parsed = JSON.parse(window.localStorage.getItem(STORAGE_KEY) ?? '{}')
    return {
      token: parsed.token ?? '',
      tokenType: parsed.tokenType ?? '',
      expiresIn: parsed.expiresIn ?? null,
      expiresAt: parsed.expiresAt ?? null,
      refreshToken: parsed.refreshToken ?? '',
      refreshExpiresIn: parsed.refreshExpiresIn ?? null,
      refreshExpiresAt: parsed.refreshExpiresAt ?? null,
      username: parsed.username ?? '',
      sessionExpiredAt: parsed.sessionExpiredAt ?? null,
      showSessionModal: Boolean(parsed.showSessionModal),
    }
  } catch {
    return {
      token: '',
      tokenType: '',
      expiresIn: null,
      expiresAt: null,
      refreshToken: '',
      refreshExpiresIn: null,
      refreshExpiresAt: null,
      username: '',
      sessionExpiredAt: null,
      showSessionModal: false,
    }
  }
}

function persistState(state) {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      token: state.token,
      tokenType: state.tokenType,
      expiresIn: state.expiresIn,
      expiresAt: state.expiresAt,
      refreshToken: state.refreshToken,
      refreshExpiresIn: state.refreshExpiresIn,
      refreshExpiresAt: state.refreshExpiresAt,
      username: state.username,
      sessionExpiredAt: state.sessionExpiredAt,
      showSessionModal: state.showSessionModal,
    }),
  )
}

const persisted = readPersistedState()

const state = reactive({
  token: persisted.token,
  tokenType: persisted.tokenType,
  expiresIn: persisted.expiresIn,
  expiresAt: persisted.expiresAt,
  refreshToken: persisted.refreshToken,
  refreshExpiresIn: persisted.refreshExpiresIn,
  refreshExpiresAt: persisted.refreshExpiresAt,
  username: persisted.username,
  loading: false,
  error: '',
  showSessionModal: persisted.showSessionModal,
  sessionExpiredAt: persisted.sessionExpiredAt,
  reauthLoading: false,
  now: Date.now(),
})

let refreshInFlight = null

export const api = new HouseDbApiClient({
  getToken: () => state.token,
})

async function renewTokenSilently() {
  if (!state.refreshToken) {
    return
  }

  if (refreshInFlight) {
    return refreshInFlight
  }

  try {
    refreshInFlight = api.refreshSession(state.refreshToken)
    const response = await refreshInFlight
    applyAuthResponse(response, state.username)
  } catch {
    if (state.expiresAt && Date.now() >= state.expiresAt) {
      expireSession('La sesion expiro. Ingresa nuevamente para continuar.')
    }
  } finally {
    refreshInFlight = null
  }
}

function scheduleRedirectToLogin() {
  if (typeof window === 'undefined') {
    return
  }

  clearTimer(redirectTimer)
  redirectTimer = window.setTimeout(() => {
    if (attachedRouter) {
      attachedRouter.push({ name: 'login' })
    }
  }, REDIRECT_AFTER_EXPIRE_MS)
}

function expireSession(message = 'La sesion expiro. Ingresa nuevamente para continuar.') {
  clearAllTimers()
  state.token = ''
  state.tokenType = ''
  state.expiresIn = null
  state.expiresAt = null
  state.refreshToken = ''
  state.refreshExpiresIn = null
  state.refreshExpiresAt = null
  state.error = message
  state.showSessionModal = true
  state.sessionExpiredAt = Date.now()
  persistState(state)
  scheduleRedirectToLogin()
}

function scheduleSessionLifecycle() {
  if (typeof window === 'undefined') {
    return
  }

  clearAllTimers()
  heartbeatTimer = window.setInterval(() => {
    state.now = Date.now()
  }, 1000)

  if (!state.token || !state.expiresAt) {
    if (state.showSessionModal && state.sessionExpiredAt) {
      scheduleRedirectToLogin()
    }
    return
  }

  const refreshIn = state.expiresAt - Date.now() - REFRESH_GRACE_MS
  const expireIn = state.expiresAt - Date.now()

  if (refreshIn <= 0) {
    refreshTimer = window.setTimeout(() => {
      renewTokenSilently()
    }, 0)
  } else {
    refreshTimer = window.setTimeout(() => {
      renewTokenSilently()
    }, refreshIn)
  }

  if (expireIn <= 0) {
    expireSession()
  } else {
    expiryTimer = window.setTimeout(() => {
      expireSession()
    }, expireIn)
  }
}

function applyAuthResponse(response, username) {
  state.token = response.access_token ?? ''
  state.tokenType = response.token_type ?? 'Bearer'
  state.expiresIn = response.expires_in ?? null
  state.expiresAt = response.expires_in ? Date.now() + response.expires_in * 1000 : null
  state.refreshToken = response.refresh_token ?? ''
  state.refreshExpiresIn = response.refresh_expires_in ?? null
  state.refreshExpiresAt = response.refresh_expires_in
    ? Date.now() + response.refresh_expires_in * 1000
    : null
  state.username = username
  state.error = ''
  state.showSessionModal = false
  state.sessionExpiredAt = null
  persistState(state)
  scheduleSessionLifecycle()
}

export function installSessionLifecycle(router) {
  attachedRouter = router
  state.now = Date.now()
  scheduleSessionLifecycle()
}

export function useSessionStore() {
  const isAuthenticated = computed(() => Boolean(state.token))
  const sessionSecondsLeft = computed(() => {
    if (!state.expiresAt) {
      return null
    }

    return Math.max(0, Math.floor((state.expiresAt - state.now) / 1000))
  })

  async function login(credentials) {
    state.loading = true
    state.error = ''

    try {
      const response = await api.login(credentials)
      applyAuthResponse(response, credentials.username)
      return response
    } catch (error) {
      const normalized = normalizeApiError(error)
      state.error = normalized.message
      throw normalized
    } finally {
      state.loading = false
    }
  }

  async function reauthenticate(credentials) {
    state.reauthLoading = true
    state.error = ''

    try {
      const response = await api.login(credentials)
      applyAuthResponse(response, credentials.username)
      return response
    } catch (error) {
      const normalized = normalizeApiError(error)
      state.error = normalized.message
      throw normalized
    } finally {
      state.reauthLoading = false
    }
  }

  function logout() {
    clearAllTimers()
    state.token = ''
    state.tokenType = ''
    state.expiresIn = null
    state.expiresAt = null
    state.refreshToken = ''
    state.refreshExpiresIn = null
    state.refreshExpiresAt = null
    state.username = ''
    state.error = ''
    state.showSessionModal = false
    state.sessionExpiredAt = null
    persistState(state)
  }

  function closeSessionModalAndGoToLogin() {
    clearTimer(redirectTimer)
    redirectTimer = null
    state.showSessionModal = false
    persistState(state)
    if (attachedRouter) {
      attachedRouter.push({ name: 'login' })
    }
  }

  return {
    state,
    api,
    isAuthenticated,
    sessionSecondsLeft,
    login,
    logout,
    reauthenticate,
    closeSessionModalAndGoToLogin,
  }
}
