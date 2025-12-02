# API Usage Examples

This document provides practical examples of using the Manon's Underground API to interact with 333networks server data.

## Prerequisites

Start the application:
```bash
./gradlew bootRun
```

The API will be available at `http://localhost:8080`

## Example 1: Get All MOHAA Servers

Get servers from all three MOHAA variants (mohaa, mohaas, mohaab):

```bash
curl http://localhost:8080/api/servers/mohaa/all
```

```json
{
  "mohaa": {
    "servers": [...],
    "metadata": { "players": 0, "total": 135 }
  },
  "mohaas": {
    "servers": [...],
    "metadata": { "players": 0, "total": 54 }
  },
  "mohaab": {
    "servers": [...],
    "metadata": { "players": 0, "total": 25 }
  }
}
```

## Example 2: Search for Specific Servers

Search for servers with "freeze" in their name:

```bash
curl "http://localhost:8080/api/servers/mohaa?query=freeze&results=10"
```

## Example 3: Get Servers by Country

Get all MOHAA servers hosted in Germany:

```bash
curl "http://localhost:8080/api/servers/mohaa?country=DE"
```

## Example 4: Sort by Player Count

Get top 10 servers by player count (descending):

```bash
curl "http://localhost:8080/api/servers/mohaa?sortBy=numplayers&order=d&results=10"
```

## Example 5: Get Server Details with Players

Get detailed information including player list:

```bash
curl "http://localhost:8080/api/servers/mohaa/141.94.205.35/12205"
```

Response includes server details and player information:
```json
{
  "id": 206492,
  "hostname": "{AB}<> Airbourne Server",
  "gametype": "*Airborne D-OBJ*",
  "numplayers": 2,
  "maxplayers": 40,
  "mapname": "m2l3",
  ...
}
```

To extract player information (if server has players), use the `extractPlayers()` method in your application code.

## Example 6: Filter by Game Type

Get all "Freeze Tag" servers:

```bash
curl "http://localhost:8080/api/servers/mohaa?gametype=freeze"
```

## Example 7: Pagination

Get page 2 of results with 20 servers per page:

```bash
curl "http://localhost:8080/api/servers/mohaa?results=20&page=2"
```

Calculate total pages: `ceiling(metadata.total / results)`

## Example 8: Combine Filters

Get German servers with more than 0 players, sorted by player count:

```bash
curl "http://localhost:8080/api/servers/mohaa?country=DE&sortBy=numplayers&order=d"
```

## Example 9: Get MOTD

Get the Message of the Day for MOHAA:

```bash
curl http://localhost:8080/api/servers/mohaa/motd
```

## Using with JavaScript/TypeScript

```typescript
// Get all MOHAA servers
fetch('http://localhost:8080/api/servers/mohaa/all')
  .then(res => res.json())
  .then(data => {
    console.log('MOHAA servers:', data.mohaa.servers);
    console.log('MOHAAS servers:', data.mohaas.servers);
    console.log('MOHAAB servers:', data.mohaab.servers);
  });

// Search with filters
const params = new URLSearchParams({
  query: 'freeze',
  sortBy: 'numplayers',
  order: 'd',
  results: '10'
});

fetch(`http://localhost:8080/api/servers/mohaa?${params}`)
  .then(res => res.json())
  .then(data => {
    console.log('Servers:', data.servers);
    console.log('Total found:', data.metadata.total);
  });
```

## Using with Python

```python
import requests

# Get all MOHAA servers
response = requests.get('http://localhost:8080/api/servers/mohaa/all')
data = response.json()

print(f"MOHAA servers: {len(data['mohaa']['servers'])}")
print(f"MOHAAS servers: {len(data['mohaas']['servers'])}")
print(f"MOHAAB servers: {len(data['mohaab']['servers'])}")

# Search with filters
params = {
    'query': 'freeze',
    'sortBy': 'numplayers',
    'order': 'd',
    'results': 10
}
response = requests.get('http://localhost:8080/api/servers/mohaa', params=params)
data = response.json()

for server in data['servers']:
    print(f"{server['hostname']} - Players: {server['numplayers']}/{server['maxplayers']}")
```

## Caching

The API automatically caches responses for 7.5 minutes to respect the 333networks API rate limits. To manually clear the cache (for testing):

```bash
curl -X POST http://localhost:8080/api/servers/cache/clear
```

## Notes

- Server information updates every 7.5 minutes via 333networks
- All timestamps are Unix epoch (UTC)
- Country codes follow ISO 3166-1 alpha-2 standard
- Empty/null fields indicate data not provided by the server

---

**Data provided by 333networks** - https://www.333networks.com
