-- Clear existing data to allow clean schema change
TRUNCATE TABLE players CASCADE;
TRUNCATE TABLE servers CASCADE;

-- Create server_snapshots table
CREATE TABLE server_snapshots (
    id BIGSERIAL PRIMARY KEY,
    server_id BIGINT NOT NULL,
    gametype VARCHAR(255),
    mapname VARCHAR(255),
    mapurl VARCHAR(255),
    gamever VARCHAR(255),
    num_players INTEGER NOT NULL,
    max_players INTEGER NOT NULL,
    password VARCHAR(255),
    timelimit VARCHAR(255),
    fraglimit VARCHAR(255),
    snapshot_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    dt_updated BIGINT,
    CONSTRAINT fk_server_snapshots_server FOREIGN KEY (server_id) REFERENCES servers (id)
);

-- Modify servers table to be the Identity table
ALTER TABLE servers DROP COLUMN gametype;
ALTER TABLE servers DROP COLUMN mapname;
ALTER TABLE servers DROP COLUMN mapurl;
ALTER TABLE servers DROP COLUMN gamever;
ALTER TABLE servers DROP COLUMN num_players;
ALTER TABLE servers DROP COLUMN max_players;
ALTER TABLE servers DROP COLUMN password;
ALTER TABLE servers DROP COLUMN timelimit;
ALTER TABLE servers DROP COLUMN fraglimit;
ALTER TABLE servers DROP COLUMN snapshot_time;
ALTER TABLE servers DROP COLUMN dt_updated;

-- Add unique constraint to servers
ALTER TABLE servers ADD CONSTRAINT uk_server_identity UNIQUE (ip, hostport, hostname);

-- Update players table to reference server_snapshots
ALTER TABLE players DROP CONSTRAINT IF EXISTS players_server_snapshot_id_fkey;

ALTER TABLE players
    ADD CONSTRAINT fk_players_server_snapshot
    FOREIGN KEY (server_snapshot_id)
    REFERENCES server_snapshots (id);
