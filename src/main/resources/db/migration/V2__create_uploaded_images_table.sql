CREATE TABLE uploaded_images (
    id UUID PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL,
    stored_path VARCHAR(500) NOT NULL,
    thumbnail_path VARCHAR(500),
    size BIGINT NOT NULL,
    mime_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id UUID
);
