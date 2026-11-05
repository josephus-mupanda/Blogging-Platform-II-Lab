
-- Run against a dedicated benchmark database after the application has created the schema.
INSERT INTO users (username, email, password_hash, display_name, role)
VALUES ('bench-author', 'bench-author@example.test', 'disabled', 'Benchmark author', 'AUTHOR')
ON CONFLICT (username) DO NOTHING;

INSERT INTO posts (author_id, title, body, created_at, updated_at)
SELECT u.user_id, 'Benchmark post ' || n,
       CASE WHEN n % 100 = 0 THEN 'springneedle ' ELSE '' END || repeat('Benchmark article content. ', 30),
       timestamp '2025-01-01' + n * interval '1 second',
       timestamp '2025-01-01' + n * interval '1 second'
FROM generate_series(1, 20000) n CROSS JOIN users u
WHERE u.username = 'bench-author'
ON CONFLICT (lower(btrim(title))) DO NOTHING;

-- Flush the GIN pending lists before measuring steady-state reads.
VACUUM (ANALYZE) posts;

EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM posts
WHERE deleted_at IS NULL AND (title ILIKE '%springneedle%' OR body ILIKE '%springneedle%')
ORDER BY created_at DESC, post_id DESC LIMIT 10;

EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM posts
WHERE deleted_at IS NULL AND (title ILIKE '%springneedle%' OR body ILIKE '%springneedle%');

-- Compare the same count with PostgreSQL bitmap scans disabled for this session only.
SET enable_bitmapscan = off;
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM posts
WHERE deleted_at IS NULL AND (title ILIKE '%springneedle%' OR body ILIKE '%springneedle%');
RESET enable_bitmapscan;
