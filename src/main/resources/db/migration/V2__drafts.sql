
CREATE TABLE IF NOT EXISTS drafts (
    owner_id BIGINT PRIMARY KEY,
    description TEXT,
    photo_ids VARCHAR,
    create_time TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS offer_spirits (
    id SERIAL PRIMARY KEY,
    description TEXT NOT NULL,
    photo_ids VARCHAR NOT NULL,
    publish_time TIMESTAMP WITH TIME ZONE NOT NULL,
    owner_id BIGINT NOT NULL
);
