INSERT INTO users (full_name, email, password, role, enabled)
VALUES (
           'System Admin',
           'admin@ordertracker.com',
           '$2a$12$7Vf8vvCq.sMaKzLbYr8Nf.kF0r9JfiBq3Uk/VH4GuvfbN3JlWUlci',
           'ADMIN',
           TRUE
       )
    ON CONFLICT (email) DO NOTHING;