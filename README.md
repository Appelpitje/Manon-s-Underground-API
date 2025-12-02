# Manon's Underground API

A Spring Boot service that interacts with the [333networks JSON API](https://www.333networks.com/json.php) to retrieve server information for Medal of Honor Allied Assault and other games.

**Data source**: 333networks - https://www.333networks.com

## Features

- ✅ **Full 333networks API support**
  - Message of the Day (MOTD)
  - Server list with filtering and pagination
  - Detailed server information with player lists
- ✅ **Smart caching** - Respects the 7.5-minute update interval
- ✅ **MOHAA support** - Built-in support for mohaa, mohaas, and mohaab
- ✅ **Rate limiting** - Prevents API flooding
- ✅ **Error handling** - Comprehensive error responses

## API Endpoints

### Get Message of the Day

```http
GET /api/servers/{gamename}/motd
```

**Example:**
```bash
curl http://localhost:8080/api/servers/mohaa/motd
```

### Get Server List

```http
GET /api/servers/{gamename}
```

**Query Parameters:**
- `sortBy` - Sort by field: country, hostname, gametype, ip, hostport, numplayers, mapname
- `order` - Sort order: 'a' (ascending) or 'd' (descending)
- `results` - Number of results (1-1000, default: 50)
- `page` - Page number
- `query` - Search query (max 90 chars)
- `gametype` - Filter by gametype (max 90 chars)
- `hostname` - Filter by server name (max 90 chars)
- `mapname` - Filter by map name (max 90 chars)
- `country` - Filter by country code (2 letter ISO 3166)

**Examples:**
```bash
# Get all mohaa servers
curl http://localhost:8080/api/servers/mohaa

# Get mohaa servers sorted by players (descending)
curl "http://localhost:8080/api/servers/mohaa?sortBy=numplayers&order=d"

# Search for servers with "master" in the name
curl "http://localhost:8080/api/servers/mohaa?query=master"

# Get servers in Netherlands (NL)
curl "http://localhost:8080/api/servers/mohaa?country=NL"

# Get page 2 with 20 results
curl "http://localhost:8080/api/servers/mohaa?results=20&page=2"
```

### Get Server Details

```http
GET /api/servers/{gamename}/{ip}/{port}
```

**Example:**
```bash
curl http://localhost:8080/api/servers/mohaa/84.83.176.234/28900
```

### Get All MOHAA Servers

Get servers for all MOHAA variants (mohaa, mohaas, mohaab) in one call:

```http
GET /api/servers/mohaa/all
```

**Example:**
```bash
# Get all MOHAA servers
curl http://localhost:8080/api/servers/mohaa/all

# Get all MOHAA servers sorted by players
curl "http://localhost:8080/api/servers/mohaa/all?sortBy=numplayers&order=d"
```

### Clear Cache

```http
POST /api/servers/cache/clear
```

**Note:** Use sparingly to respect API rate limits.

```bash
# Clear all cache
curl -X POST http://localhost:8080/api/servers/cache/clear

# Clear specific cache key
curl -X POST "http://localhost:8080/api/servers/cache/clear?key=serverlist_mohaa"
```

## Response Examples

### Server List Response

```json
{
  "servers": [
    {
      "id": 1990,
      "ip": "::ffff:84.83.176.234",
      "hostport": 28900,
      "hostname": "master.333networks.com (333networks MasterServer)",
      "gamename": "mohaa",
      "gametype": "MasterServer",
      "label": "Medal of Honor Allied Assault",
      "country": "NL",
      "numplayers": 15,
      "maxplayers": 2966,
      "maptitle": null,
      "mapname": "333networks",
      "dtAdded": 1616895602,
      "dtUpdated": 1621019250
    }
  ],
  "metadata": {
    "players": 20,
    "total": 5
  }
}
```

### Server Details Response

```json
{
  "id": 3,
  "ip": "::ffff:45.74.100.250",
  "hostport": 10205,
  "mapname": "dm/mohdm1",
  "adminname": "Server Admin",
  "hostname": "My MOHAA Server",
  "gamever": "1.11",
  "gametype": "DM",
  "gamename": "mohaa",
  "country": "US",
  "dtUpdated": 1621022768,
  "numplayers": 2,
  "maxplayers": 16,
  "players": [
    {
      "sid": 3,
      "name": "Player1",
      "team": "0",
      "frags": 8,
      "ping": 63,
      "dtPlayer": 1621022768
    }
  ]
}
```

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
networks:
  api:
    base-url: https://master.333networks.com/json
    default-results-per-page: 50
    min-update-interval-minutes: 7.5
    user-agent: "Manon's Underground API - Data from 333networks"
```

## Running the Application

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun
```

The API will be available at `http://localhost:8080`

## Supported Games

While this API is designed for Medal of Honor Allied Assault variants, it supports any game tracked by 333networks:

- `mohaa` - Medal of Honor Allied Assault
- `mohaas` - Medal of Honor Allied Assault Spearhead
- `mohaab` - Medal of Honor Allied Assault Breakthrough
- `all` - All games
- And many more...

See the [333networks games page](https://www.333networks.com/games.php) for a complete list.

## Terms of Use

This service respects the 333networks API terms:

- ✅ Credits 333networks as the data source (required)
- ✅ Implements caching to avoid flooding (7.5-minute intervals)
- ✅ Does not monetize 333networks data without permission
- ✅ Uses the serverlist appropriately (not requesting detailed info for all servers at once)

## License

This project is provided as-is. The data is sourced from 333networks and subject to their terms of use.

---

**Data provided by 333networks** - https://www.333networks.com
