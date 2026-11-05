
-- Local demo account for Swagger and GraphiQL. Run only on a development database.
INSERT INTO users (username, email, password_hash, display_name, role)
VALUES ('admin', 'admin@example.test',
        'bcb15f821479b4d5772bd0ca866c00ad5f926e3580720659cc80d39c9d09802a', 'Demo administrator', 'ADMIN')
ON CONFLICT (username) DO NOTHING;
