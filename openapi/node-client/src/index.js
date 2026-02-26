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
  login --username USER --password PASS
  login-basic --username USER --password PASS
  token --client-id ID --client-secret SECRET [--grant-type client_credentials]
  token-basic --client-id ID --client-secret SECRET
  get-item --id UUID
  create-item --house-location-leaf-id UUID --object-name NOMBRE [--object-category CAT] [--object-description DESC]
              [--object-type EQUIPMENT] [--object-tags a,b] [--kiwi-metadata JSON] [--housedb-metadata JSON]
              [--nickname TXT] [--serial-number TXT] [--condition-status active] [--moved-by TXT] [--notes TXT]
  search-items [--q TEXTO] [--house-id UUID] [--house-location-leaf-id UUID] [--limit N]
  list-houses [--include-disabled true|false] [--limit N]
  list-house-ids [--include-disabled true|false] [--limit N]
  create-house --name NOMBRE [--city CITY] [--state STATE] [--country COUNTRY]
  list-house-members --house-id UUID [--include-disabled true|false] [--limit N]
  upsert-house-member --house-id UUID --user-id UUID [--role owner|family|guest] [--enabled true|false] [--method POST|PUT]
  create-house-location --house-id UUID --name NOMBRE [--parent-kiwi-location-id UUID]
  demo

Variables de entorno:
  HOUSEDB_BASE_URL  (default: http://localhost:8080)
  HOUSEDB_TOKEN     token Bearer opcional
`);
}

function toBool(value) {
  if (value === undefined || value === "") return undefined;
  return String(value).toLowerCase() === "true";
}

function parseJsonFlag(value, flagName) {
  if (!value) return undefined;
  try {
    return JSON.parse(value);
  } catch {
    throw new Error(`Invalid JSON for ${flagName}`);
  }
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

      case "login":
        res = await client.login({
          username: argValue("--username"),
          password: argValue("--password")
        });
        break;

      case "login-basic":
        res = await client.loginWithBasic(argValue("--username"), argValue("--password"));
        break;

      case "token":
        res = await client.clientToken({
          clientId: argValue("--client-id"),
          clientSecret: argValue("--client-secret"),
          grantType: argValue("--grant-type", "client_credentials") || "client_credentials"
        });
        break;

      case "token-basic":
        res = await client.clientTokenWithBasic(argValue("--client-id"), argValue("--client-secret"));
        break;

      case "get-item":
        res = await client.getItem(argValue("--id"));
        break;

      case "create-item": {
        const objectTagsRaw = argValue("--object-tags");
        const objectTags = objectTagsRaw
          ? objectTagsRaw
              .split(",")
              .map((v) => v.trim())
              .filter(Boolean)
          : undefined;

        res = await client.createItem({
          objectName: argValue("--object-name"),
          objectDescription: argValue("--object-description") || undefined,
          objectCategory: argValue("--object-category") || undefined,
          objectType: argValue("--object-type") || undefined,
          objectTags,
          kiwiMetadata: parseJsonFlag(argValue("--kiwi-metadata"), "--kiwi-metadata"),
          housedbMetadata: parseJsonFlag(argValue("--housedb-metadata"), "--housedb-metadata"),
          nickname: argValue("--nickname") || undefined,
          serialNumber: argValue("--serial-number") || undefined,
          conditionStatus: argValue("--condition-status") || undefined,
          houseLocationLeafId: argValue("--house-location-leaf-id"),
          movedBy: argValue("--moved-by") || undefined,
          notes: argValue("--notes") || undefined
        });
        break;
      }

      case "search-items":
        res = await client.searchItems({
          q: argValue("--q") || undefined,
          houseId: argValue("--house-id") || undefined,
          houseLocationLeafId: argValue("--house-location-leaf-id") || undefined,
          limit: argValue("--limit") ? Number(argValue("--limit")) : undefined
        });
        break;

      case "list-houses":
        res = await client.listHouses({
          includeDisabled: toBool(argValue("--include-disabled")),
          limit: argValue("--limit") ? Number(argValue("--limit")) : undefined
        });
        break;

      case "list-house-ids":
        res = await client.listHouseIds({
          includeDisabled: toBool(argValue("--include-disabled")),
          limit: argValue("--limit") ? Number(argValue("--limit")) : undefined
        });
        break;

      case "create-house":
        res = await client.createHouse({
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
        const health = await client.health();
        const houses = await client.listHouses();
        const houseIds = await client.listHouseIds();
        printJson({ health: health.data, houses: houses.data, houseIds: houseIds.data, baseUrl: client.baseUrl });
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
