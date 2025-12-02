-- Remove unused columns from players table
ALTER TABLE players DROP COLUMN IF EXISTS team;
ALTER TABLE players DROP COLUMN IF EXISTS mesh;
ALTER TABLE players DROP COLUMN IF EXISTS skin;
ALTER TABLE players DROP COLUMN IF EXISTS face;
ALTER TABLE players DROP COLUMN IF EXISTS dt_player;
