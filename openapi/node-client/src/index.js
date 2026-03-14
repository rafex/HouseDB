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

Entidades:
  /auth
    login --username USER --password PASS
    login-basic --username USER --password PASS
    refresh-session --refresh-token TOKEN
    token --client-id ID --client-secret SECRET [--grant-type client_credentials]
    token-basic --client-id ID --client-secret SECRET

  /hello
    hello
    hello-name [GET] [--name NOMBRE]
    hello-name-post [POST] [--name NOMBRE]

  /health
    health

  /items
    get-item --id UUID
    create-item --house-location-leaf-id UUID --object-name NOMBRE [--object-category CAT] [--object-description DESC]
                [--object-type EQUIPMENT] [--object-tags a,b] [--kiwi-metadata JSON] [--housedb-metadata JSON]
                [--nickname TXT] [--serial-number TXT] [--condition-status active] [--moved-by TXT] [--notes TXT]
    search-items [--q TEXTO] [--house-id UUID] [--house-location-leaf-id UUID] [--limit N]

  /houses
    list-houses [GET] [--include-disabled true|false] [--limit N]
    create-house [POST] --name NOMBRE [--city CITY] [--state STATE] [--country COUNTRY]
    /houses/ids
      list-house-ids [GET] [--include-disabled true|false] [--limit N]
    /houses/{houseId}/members
      list-house-members [GET] --house-id UUID [--include-disabled true|false] [--limit N]
      upsert-house-member [POST|PUT] --house-id UUID --user-id UUID [--role owner|family|guest] [--enabled true|false] [--method POST|PUT]
    /houses/{houseId}/locations
      list-house-locations [GET] --house-id UUID [--include-disabled true|false] [--limit N] [--offset N]
      create-house-location [POST] --house-id UUID --name NOMBRE [--parent-house-location-id UUID]

  /metadata-catalogs
    list-metadata-catalogs [GET] [--metadata-target kiwi_object|inventory_item] [--include-disabled true|false] [--limit N] [--offset N]

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

      case "hello":
        res = await client.hello();
        break;

      case "hello-name":
        res = await client.helloName({
          name: argValue("--name") || undefined
        });
        break;

      case "hello-name-post":
        res = await client.helloNamePost({
          name: argValue("--name") || undefined
        });
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

      case "refresh-session":
        res = await client.refreshSession(argValue("--refresh-token"));
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

      case "list-house-locations":
        res = await client.listHouseLocations(argValue("--house-id"), {
          includeDisabled: toBool(argValue("--include-disabled")),
          limit: argValue("--limit") ? Number(argValue("--limit")) : undefined,
          offset: argValue("--offset") ? Number(argValue("--offset")) : undefined
        });
        break;

      case "list-metadata-catalogs":
        res = await client.listMetadataCatalogs({
          metadataTarget: argValue("--metadata-target") || undefined,
          includeDisabled: toBool(argValue("--include-disabled")),
          limit: argValue("--limit") ? Number(argValue("--limit")) : undefined,
          offset: argValue("--offset") ? Number(argValue("--offset")) : undefined
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
          parentHouseLocationId: argValue("--parent-house-location-id") || undefined
        });
        break;

      case "demo": {
        const health = await client.health();
        const hello = await client.hello();
        const helloName = await client.helloName({ name: "Demo" });
        const houses = await client.listHouses();
        const houseIds = await client.listHouseIds();
        const metadataCatalogs = process.env.HOUSEDB_TOKEN
          ? await client.listMetadataCatalogs()
          : { data: "skipped: requires HOUSEDB_TOKEN" };
        printJson({
          health: health.data,
          hello: hello.data,
          helloName: helloName.data,
          houses: houses.data,
          houseIds: houseIds.data,
          metadataCatalogs: metadataCatalogs.data,
          baseUrl: client.baseUrl
        });
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
