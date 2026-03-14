import { computed, reactive } from 'vue'

import { normalizeApiError, paginationFromResponse } from '../lib/api'
import { useSessionStore } from './session'

const state = reactive({
  houses: [],
  loadingHouses: false,
  housesError: '',
  loadedOnce: false,
})

export function useCatalogStore() {
  const { api, isAuthenticated } = useSessionStore()

  const houseOptions = computed(() =>
    state.houses.map((house) => ({
      value: house.houseId,
      label: [house.name, house.city].filter(Boolean).join(' · '),
      house,
    })),
  )

  function getHouseById(houseId) {
    return state.houses.find((house) => house.houseId === houseId) ?? null
  }

  async function loadHousesCatalog({ force = false } = {}) {
    if (!isAuthenticated.value) {
      state.houses = []
      state.housesError = ''
      state.loadedOnce = false
      return []
    }

    if (state.loadingHouses) {
      return state.houses
    }

    if (state.loadedOnce && !force) {
      return state.houses
    }

    state.loadingHouses = true
    state.housesError = ''

    try {
      const houses = []
      let offset = 0
      let hasMore = true

      while (hasMore) {
        const response = await api.listHouses({ limit: 100, offset })
        houses.push(...(response.houses ?? []))

        const pagination = paginationFromResponse(response, {
          limit: 100,
          offset,
          returned: response.houses?.length ?? 0,
          hasMore: false,
        })

        hasMore = pagination.hasMore
        offset = pagination.nextOffset ?? 0
      }

      state.houses = houses
      state.loadedOnce = true
      return state.houses
    } catch (error) {
      state.housesError = normalizeApiError(error).message
      throw error
    } finally {
      state.loadingHouses = false
    }
  }

  return {
    state,
    houseOptions,
    getHouseById,
    loadHousesCatalog,
  }
}
