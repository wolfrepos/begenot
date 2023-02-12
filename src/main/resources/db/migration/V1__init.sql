
CREATE TABLE IF NOT EXISTS account (
    id BIGINT PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS offers (
    id SERIAL PRIMARY KEY,
    description TEXT NOT NULL,
    photo_ids VARCHAR NOT NULL,
    publish_time TIMESTAMP WITH TIME ZONE NOT NULL,
    owner_id BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_fts_offers ON offers
USING gin(to_tsvector('russian', description));
