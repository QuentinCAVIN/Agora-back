DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'users_roles'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'user_roles'
    ) THEN
        ALTER TABLE users_roles RENAME TO user_roles;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL
        CONSTRAINT fk_user_roles_user REFERENCES users (id) ON DELETE CASCADE,
    roles VARCHAR(50) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, roles)
);

CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles (user_id);
