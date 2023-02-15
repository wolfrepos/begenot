
CREATE TABLE IF NOT EXISTS drafts (
    id SERIAL PRIMARY KEY,
    description TEXT NOT NULL,
    photo_ids VARCHAR NOT NULL,
    create_time TIMESTAMP WITH TIME ZONE NOT NULL,
    owner_id BIGINT NOT NULL
);

CREATE INDEX create_time_idx ON drafts (create_time);
