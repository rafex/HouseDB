# housedb-backend Helm Chart

Chart Helm para desplegar HouseDB backend en Kubernetes con imagen en GHCR.

## Imagen por defecto

- `ghcr.io/rafex/housedb-backend:latest`

## Instalacion recomendada

1) Crear secret con credenciales y secretos:

```bash
kubectl -n mvps create secret generic housedb-backend-secrets \
  --from-literal=DB_URL='jdbc:postgresql://postgres:5432/housedb' \
  --from-literal=DB_USER='housedb_app' \
  --from-literal=DB_PASSWORD='changeme' \
  --from-literal=JWT_SECRET='CHANGE_ME_32_CHARS_MIN' \
  --from-literal=KIWI_APP_CLIENT_ID='housedb-app' \
  --from-literal=KIWI_APP_CLIENT_SECRET='supersecret'
```

2) Instalar/actualizar release:

```bash
helm upgrade --install housedb-backend ./helm/housedb-backend \
  --namespace mvps --create-namespace \
  --set image.tag="<sha-o-tag-version>" \
  --set existingSecret="housedb-backend-secrets"
```

## Ingress

Por defecto publica la API en:

- `housedb.v1.rafex.cloud`

## Valores importantes

- `image.repository`, `image.tag`
- `service.port`
- `env.*`
- `secretEnv.*` o `existingSecret`
- `ingress.hosts`
