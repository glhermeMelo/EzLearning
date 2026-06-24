CREATE TABLE generated_media (
    id UUID PRIMARY KEY,
    prompt TEXT NOT NULL,
    prompt_hash VARCHAR(255) NOT NULL,
    stored_path VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    referenced BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL,
    last_accessed_at TIMESTAMP,
    user_id UUID REFERENCES users(id)
);
