
CREATE TABLE IF NOT EXISTS pending_offers (
    id SERIAL PRIMARY KEY,
    description TEXT NOT NULL,
    photo_ids VARCHAR NOT NULL,
    publish_time TIMESTAMP WITH TIME ZONE NOT NULL,
    owner_id BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS publish_time_idx ON pending_offers (publish_time);
