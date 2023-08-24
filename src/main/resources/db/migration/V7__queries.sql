
CREATE TABLE IF NOT EXISTS QUERIES (
    user_id BIGINT NOT NULL,
    query_time TIMESTAMP WITH TIME ZONE NOT NULL,
    brand VARCHAR,
    model VARCHAR,
    yearr INTEGER,
    transmission VARCHAR,
    steering VARCHAR,
    mileage INTEGER,
    price_min INTEGER,
    price_max INTEGER,
    city VARCHAR
);
