CREATE TABLE IF NOT EXISTS users_roles (
    user_id UUID NOT NULL
        CONSTRAINT fk_users_roles_user REFERENCES users (id) ON DELETE CASCADE,
    roles VARCHAR(50) NOT NULL,
    CONSTRAINT pk_users_roles PRIMARY KEY (user_id, roles)
);

CREATE INDEX IF NOT EXISTS idx_users_roles_user_id ON users_roles (user_id);

INSERT INTO users_roles (user_id, roles)
SELECT u.id, 'SUPERADMIN'
FROM users u
LEFT JOIN users_roles existing_superadmin
       ON existing_superadmin.user_id = u.id
      AND existing_superadmin.roles = 'SUPERADMIN'
WHERE LOWER(u.email) = 'admin@agora.local'
  AND existing_superadmin.user_id IS NULL;
