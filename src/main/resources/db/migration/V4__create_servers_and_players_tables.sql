-- Create servers table
CREATE TABLE IF NOT EXISTS servers (
    id BIGSERIAL PRIMARY KEY,
    server_id INTEGER NOT NULL,
    ip VARCHAR(50) NOT NULL,
    hostport INTEGER NOT NULL,
    hostname VARCHAR(255) NOT NULL,
    gamename VARCHAR(50) NOT NULL,
    gametype VARCHAR(100),
    country VARCHAR(2),
    mapname VARCHAR(100),
    maptitle VARCHAR(255),
    mapurl VARCHAR(255),
    gamever VARCHAR(50),
    adminname VARCHAR(255),
    adminemail VARCHAR(255),
    num_players INTEGER NOT NULL,
    max_players INTEGER NOT NULL,
    password VARCHAR(50),
    timelimit VARCHAR(50),
    fraglimit VARCHAR(50),
    mutators VARCHAR(500),
    snapshot_time TIMESTAMP NOT NULL,
    dt_updated BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create players table
CREATE TABLE IF NOT EXISTS players (
    id BIGSERIAL PRIMARY KEY,
    server_snapshot_id BIGINT NOT NULL,
    player_name VARCHAR(255) NOT NULL,
    team VARCHAR(50),
    frags INTEGER NOT NULL,
    mesh VARCHAR(100),
    skin VARCHAR(100),
    face VARCHAR(100),
    ping INTEGER NOT NULL,
    snapshot_time TIMESTAMP NOT NULL,
    dt_player BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_server FOREIGN KEY (server_snapshot_id) REFERENCES servers(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_servers_snapshot_time ON servers(snapshot_time DESC);
CREATE INDEX IF NOT EXISTS idx_servers_gamename ON servers(gamename);
CREATE INDEX IF NOT EXISTS idx_servers_ip_port ON servers(ip, hostport);
CREATE INDEX IF NOT EXISTS idx_servers_server_id ON servers(server_id);
CREATE INDEX IF NOT EXISTS idx_servers_num_players ON servers(num_players);
CREATE INDEX IF NOT EXISTS idx_servers_composite ON servers(ip, hostport, snapshot_time DESC);

CREATE INDEX IF NOT EXISTS idx_players_server_snapshot ON players(server_snapshot_id);
CREATE INDEX IF NOT EXISTS idx_players_snapshot_time ON players(snapshot_time DESC);
CREATE INDEX IF NOT EXISTS idx_players_name ON players(player_name);

-- Create a view for the latest server snapshots
CREATE OR REPLACE VIEW latest_server_snapshots AS
SELECT DISTINCT ON (ip, hostport)
    id,
    server_id,
    ip,
    hostport,
    hostname,
    gamename,
    gametype,
    country,
    mapname,
    maptitle,
    num_players,
    max_players,
    snapshot_time
FROM servers
ORDER BY ip, hostport, snapshot_time DESC;

COMMENT ON TABLE servers IS 'Server snapshots from 333networks API';
COMMENT ON TABLE players IS 'Player snapshots associated with server snapshots';
COMMENT ON VIEW latest_server_snapshots IS 'Latest snapshot for each unique server';
