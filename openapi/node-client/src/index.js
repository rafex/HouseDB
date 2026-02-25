import { HouseDbClient } from "./client.js";

function argValue(flag, fallback = "") {
  const idx = process.argv.indexOf(flag);
  if (idx === -1 || idx + 1 >= process.argv.length) return fallback;
  return process.argv[idx + 1];
}

function printJson(value) {
  console.log(JSON.stringify(value, null, 2));
}

function usage() {
  console.log(`
Uso:
  node src/index.js <comando> [opciones]

Comandos:
  health
  get-item --id UUID
  search-items --user-id UUID [--q TEXTO] [--house-id UUID] [--house-location-leaf-id UUID] [--limit N]
  list-houses --user-id UUID [--include-disabled true|false] [--limit N]
  create-house --owner-user-id UUID --name NOMBRE [--city CITY] [--state STATE] [--country COUNTRY]
  list-house-members --house-id UUID [--include-disabled true|false] [--limit N]
  upsert-house-member --house-id UUID --user-id UUID [--role owner|family|guest] [--enabled true|false] [--method POST|PUT]
  create-house-location --house-id UUID --name NOMBRE [--parent-kiwi-location-id UUID]
  demo --user-id UUID

Variables de entorno:
  HOUSEDB_BASE_URL  (default: http://localhost:8080)
  HOUSEDB_TOKEN     token Bearer opcional
`);
}

function toBool(value) {
  if (value === undefined || value === "") return undefined;
  return String(value).toLowerCase() === "true";
}

async function main() {
  const command = process.argv[2];

  if (!command || command === "-h" || command === "--help") {
    usage();
    process.exit(0);
  }

  const client = new HouseDbClient({
    baseUrl: process.env.HOUSEDB_BASE_URL,
    token: process.env.HOUSEDB_TOKEN
  });

  try {
    let res;

    switch (command) {
      case "health":
        res = await client.health();
        break;

      case "get-item":
        res = await client.getItem(argValue("--id"));
        break;

      case "search-items":
        res = await client.searchItems({
          userId: argValue("--user-id"),
          q: argValue("--q") || undefined,
          houseId: argValue("--house-id") || undefined,
          houseLocationLeafId: argValue("--house-location-leaf-id") || undefined,
          limit: argValue("--limit") ? Number(argValue("--limit")) : undefined
        });
        break;

      case "list-houses":
        res = await client.listHouses({
          userId: argValue("--user-id"),
          includeDisabled: toBool(argValue("--include-disabled")),
          limit: argValue("--limit") ? Number(argValue("--limit")) : undefined
        });
        break;

      case "create-house":
        res = await client.createHouse({
          ownerUserId: argValue("--owner-user-id"),
          name: argValue("--name"),
          city: argValue("--city") || undefined,
          state: argValue("--state") || undefined,
          country: argValue("--country") || undefined
        });
        break;

      case "list-house-members":
        res = await client.listHouseMembers(argValue("--house-id"), {
          includeDisabled: toBool(argValue("--include-disabled")),
          limit: argValue("--limit") ? Number(argValue("--limit")) : undefined
        });
        break;

      case "upsert-house-member":
        res = await client.upsertHouseMember(
          argValue("--house-id"),
          {
            userId: argValue("--user-id"),
            role: argValue("--role") || undefined,
            enabled: toBool(argValue("--enabled"))
          },
          (argValue("--method", "POST") || "POST").toUpperCase()
        );
        break;

      case "create-house-location":
        res = await client.createHouseLocation(argValue("--house-id"), {
          name: argValue("--name"),
          parentKiwiLocationId: argValue("--parent-kiwi-location-id") || undefined
        });
        break;

      case "demo": {
        const userId = argValue("--user-id");
        const health = await client.health();
        const houses = userId ? await client.listHouses({ userId }) : null;
        printJson({ health: health.data, houses: houses?.data || null, baseUrl: client.baseUrl });
        return;
      }

      default:
        usage();
        process.exit(1);
    }

    printJson({ status: res.status, data: res.data });
  } catch (error) {
    printJson({
      error: error.message,
      status: error.status,
      data: error.data
    });
    process.exit(1);
  }
}

main();
