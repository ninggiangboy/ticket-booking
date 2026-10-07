-- Operational tables (DOC-15 §3-§7): login tokens, sessions, idempotency keys, Stripe events, outbox.

-- One row per accepted magic link request; only the SHA-256 of the token is stored (NFR-07).
CREATE TABLE login_token (
  token_hash    bytea PRIMARY KEY,
  email         text NOT NULL,
  return_to     text,
  locale        text NOT NULL,
  requested_ip  inet,
  created_at    timestamptz NOT NULL DEFAULT now(),
  expires_at    timestamptz NOT NULL,
  used_at       timestamptz,
  superseded_at timestamptz
);
CREATE INDEX login_token_email_idx   ON login_token (email, created_at DESC);
CREATE INDEX login_token_ip_idx      ON login_token (requested_ip, created_at DESC) WHERE requested_ip IS NOT NULL;
CREATE INDEX login_token_expires_idx ON login_token (expires_at);

CREATE TABLE session (
  session_hash  bytea PRIMARY KEY,
  user_id       uuid NOT NULL REFERENCES app_user,
  csrf_token    text NOT NULL,
  created_at    timestamptz NOT NULL DEFAULT now(),
  last_seen_at  timestamptz NOT NULL DEFAULT now(),
  revoked_at    timestamptz
);
CREATE INDEX session_user_idx      ON session (user_id);
CREATE INDEX session_last_seen_idx ON session (last_seen_at);
CREATE INDEX session_revoked_idx   ON session (revoked_at) WHERE revoked_at IS NOT NULL;

CREATE TABLE idempotency_key (
  user_id         uuid NOT NULL,                     -- no FK: short-lived rows, avoids locking app_user
  idem_key        uuid NOT NULL,
  operation       text NOT NULL CHECK (operation IN ('HOLD','CANCEL_HOLD','CONFIRM_FREE')),
  request_hash    bytea NOT NULL,
  response_status smallint,
  response_body   jsonb,
  created_at      timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, idem_key)
);
CREATE INDEX idempotency_key_created_idx ON idempotency_key (created_at);

CREATE TABLE stripe_event (
  stripe_event_id   text PRIMARY KEY,
  type              text NOT NULL,
  payment_intent_id text,
  outcome           text NOT NULL,
  received_at       timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX stripe_event_pi_idx       ON stripe_event (payment_intent_id) WHERE payment_intent_id IS NOT NULL;
CREATE INDEX stripe_event_received_idx ON stripe_event (received_at);

-- Emails raised by business transactions, written in the same transaction as the change (ADR-0006).
CREATE TABLE outbox (
  outbox_id       uuid PRIMARY KEY DEFAULT uuidv7(),
  kind            text NOT NULL CHECK (kind IN ('EMAIL_TICKETS','EMAIL_EVENT_CHANGED','EMAIL_REFUND_PENDING')),
  payload         jsonb NOT NULL,
  status          text NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','SENT','FAILED')),
  attempts        int  NOT NULL DEFAULT 0,
  next_attempt_at timestamptz NOT NULL DEFAULT now(),
  last_error      text,
  created_at      timestamptz NOT NULL DEFAULT now(),
  sent_at         timestamptz
);
CREATE INDEX outbox_due_idx  ON outbox (next_attempt_at) WHERE status = 'PENDING';
CREATE INDEX outbox_done_idx ON outbox ((COALESCE(sent_at, created_at))) WHERE status <> 'PENDING';
