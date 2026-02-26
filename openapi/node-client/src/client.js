const DEFAULT_BASE_URL = process.env.HOUSEDB_BASE_URL || "http://localhost:8080";

export class HouseDbClient {
  constructor({ baseUrl = DEFAULT_BASE_URL, token = process.env.HOUSEDB_TOKEN || "" } = {}) {
    this.baseUrl = baseUrl.replace(/\/$/, "");
    this.token = token;
  }

  setToken(token) {
    this.token = token;
  }

  async request(path, { method = "GET", headers = {}, body, token } = {}) {
    const requestHeaders = {
      Accept: "application/json",
      ...headers
    };

    const bearer = token ?? this.token;
    if (bearer) {
      requestHeaders.Authorization = `Bearer ${bearer}`;
    }

    const init = { method, headers: requestHeaders };
    if (body !== undefined) {
      init.body = JSON.stringify(body);
      requestHeaders["Content-Type"] = "application/json";
    }

    const response = await fetch(`${this.baseUrl}${path}`, init);
    const text = await response.text();

    let data = null;
    if (text) {
      try {
        data = JSON.parse(text);
      } catch {
        data = text;
      }
    }

    if (!response.ok) {
      const error = new Error(`HTTP ${response.status} ${response.statusText}`);
      error.status = response.status;
      error.data = data;
      throw error;
    }

    return { status: response.status, data };
  }

  health() {
    return this.request("/health");
  }

  login({ username, password } = {}) {
    const payload = username || password ? { username, password } : undefined;
    return this.request("/auth/login", {
      method: "POST",
      body: payload,
      token: ""
    });
  }

  loginWithBasic(username, password) {
    const basic = Buffer.from(`${username}:${password}`).toString("base64");
    return this.request("/auth/login", {
      method: "POST",
      headers: {
        Authorization: `Basic ${basic}`
      },
      token: ""
    });
  }

  clientToken({ clientId, clientSecret, grantType = "client_credentials" } = {}) {
    return this.request("/auth/token", {
      method: "POST",
      body: {
        client_id: clientId,
        client_secret: clientSecret,
        grant_type: grantType
      },
      token: ""
    });
  }

  clientTokenWithBasic(clientId, clientSecret) {
    const basic = Buffer.from(`${clientId}:${clientSecret}`).toString("base64");
    return this.request("/auth/token", {
      method: "POST",
      headers: {
        Authorization: `Basic ${basic}`
      },
      body: { grant_type: "client_credentials" },
      token: ""
    });
  }

  searchItems({ q, houseId, houseLocationLeafId, limit }) {
    const params = new URLSearchParams();
    if (q) params.set("q", q);
    if (houseId) params.set("houseId", houseId);
    if (houseLocationLeafId) params.set("houseLocationLeafId", houseLocationLeafId);
    if (limit !== undefined) params.set("limit", String(limit));
    const query = params.toString();
    return this.request(`/items/search${query ? `?${query}` : ""}`);
  }

  getItem(inventoryItemId) {
    return this.request(`/items/${inventoryItemId}`);
  }

  createItem(payload) {
    return this.request("/items", { method: "POST", body: payload });
  }

  listHouses({ includeDisabled, limit } = {}) {
    const params = new URLSearchParams();
    if (includeDisabled !== undefined) params.set("includeDisabled", String(includeDisabled));
    if (limit !== undefined) params.set("limit", String(limit));
    const query = params.toString();
    return this.request(`/houses${query ? `?${query}` : ""}`);
  }

  listHouseIds({ includeDisabled, limit } = {}) {
    const params = new URLSearchParams();
    if (includeDisabled !== undefined) params.set("includeDisabled", String(includeDisabled));
    if (limit !== undefined) params.set("limit", String(limit));
    const query = params.toString();
    return this.request(`/houses/ids${query ? `?${query}` : ""}`);
  }

  createHouse(payload) {
    return this.request("/houses", { method: "POST", body: payload });
  }

  listHouseMembers(houseId, { includeDisabled, limit } = {}) {
    const params = new URLSearchParams();
    if (includeDisabled !== undefined) params.set("includeDisabled", String(includeDisabled));
    if (limit !== undefined) params.set("limit", String(limit));
    const query = params.toString();
    return this.request(`/houses/${houseId}/members${query ? `?${query}` : ""}`);
  }

  upsertHouseMember(houseId, payload, method = "POST") {
    return this.request(`/houses/${houseId}/members`, { method, body: payload });
  }

  createHouseLocation(houseId, payload) {
    return this.request(`/houses/${houseId}/locations`, { method: "POST", body: payload });
  }
}
