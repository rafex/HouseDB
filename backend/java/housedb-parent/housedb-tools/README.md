# housedb-tools

CLI para tareas operativas de HouseDB.

## Crear usuario

Requiere conexión DB (`DB_URL`, opcional `DB_USER`, `DB_PASSWORD`).

```bash
cd /Users/rafex/repository/github/rafex/HouseDB/backend/java/housedb-parent
export DB_URL='jdbc:postgresql://localhost:5432/housedb'
export DB_USER='housedb'
export DB_PASSWORD='housedb'
export ADMIN_PASS='Admin123!'

./mvnw -q -pl housedb-tools -am exec:java -Dexec.args="--username admin --password-env ADMIN_PASS --roles ADMIN,USER"
```

Opciones:

- `--username <name>` requerido
- `--user-id <uuid>` opcional
- `--password <pass>` o `--password-env <ENV_VAR>`
- `--roles USER,ADMIN` opcional (default: `USER`)

Ayuda:

```bash
./mvnw -q -pl housedb-tools -am exec:java -Dexec.args="--help"
```
