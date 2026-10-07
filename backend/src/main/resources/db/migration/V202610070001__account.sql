-- Accounts and organizer profiles (DOC-14 §4.2). No role column: an organizer row makes the user an ORGANIZER (DR-23).

CREATE TABLE app_user (
  user_id       uuid PRIMARY KEY DEFAULT uuidv7(),
  email         text NOT NULL,                       -- normalized: trimmed, lower-case (DR-21)
  locale        text NOT NULL DEFAULT 'vi' CHECK (locale IN ('vi','en')),   -- DR-10
  created_at    timestamptz NOT NULL DEFAULT now(),
  last_login_at timestamptz
);
CREATE UNIQUE INDEX app_user_email_uq ON app_user (email);

CREATE TABLE organizer (
  organizer_id  uuid PRIMARY KEY DEFAULT uuidv7(),
  owner_user_id uuid NOT NULL UNIQUE REFERENCES app_user,   -- one account, at most one organizer
  name          text NOT NULL CHECK (char_length(name) BETWEEN 1 AND 120),
  contact_email text,                                        -- NULL: use the login email
  created_at    timestamptz NOT NULL DEFAULT now()
);
