CREATE TABLE users (
                       id          BIGSERIAL PRIMARY KEY,
                       full_name   VARCHAR(100)        NOT NULL,
                       email       VARCHAR(150)        NOT NULL,
                       password    VARCHAR(255)        NOT NULL,
                       role        VARCHAR(20)         NOT NULL DEFAULT 'USER',
                       enabled     BOOLEAN             NOT NULL DEFAULT TRUE,
                       created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                       updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                       CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE orders (
                        id                BIGSERIAL PRIMARY KEY,
                        order_number      VARCHAR(50)             NOT NULL,
                        user_id           BIGINT                  NOT NULL REFERENCES users(id),
                        status            VARCHAR(30)             NOT NULL DEFAULT 'PENDING',
                        total_amount      NUMERIC(12, 2)          NOT NULL,
                        currency          CHAR(3)                 NOT NULL DEFAULT 'USD',
                        shipping_address  VARCHAR(500)            NOT NULL,
                        payment_reference VARCHAR(100),
                        tracking_number   VARCHAR(100),
                        notes             VARCHAR(1000),
                        created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                        updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                        CONSTRAINT uk_orders_order_number UNIQUE (order_number)
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status   ON orders(status);
CREATE INDEX idx_orders_payment_ref ON orders(payment_reference);

CREATE TABLE order_items (
                             id            BIGSERIAL PRIMARY KEY,
                             order_id      BIGINT          NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                             product_name  VARCHAR(200)    NOT NULL,
                             product_sku   VARCHAR(50),
                             quantity      INT             NOT NULL CHECK (quantity > 0),
                             unit_price    NUMERIC(10, 2)  NOT NULL,
                             total_price   NUMERIC(12, 2)  NOT NULL,
                             created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                             updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);

CREATE TABLE order_status_history (
                                      id              BIGSERIAL PRIMARY KEY,
                                      order_id        BIGINT      NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                                      previous_status VARCHAR(30),
                                      new_status      VARCHAR(30) NOT NULL,
                                      changed_by      VARCHAR(100),
                                      reason          VARCHAR(500),
                                      changed_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                                      created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                                      updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_status_history_order_id ON order_status_history(order_id);

CREATE TABLE webhook_events (
                                id                BIGSERIAL PRIMARY KEY,
                                source            VARCHAR(50)  NOT NULL,
                                event_type        VARCHAR(40)  NOT NULL,
                                status            VARCHAR(20)  NOT NULL DEFAULT 'RECEIVED',
                                order_reference   VARCHAR(100),
                                payload           TEXT         NOT NULL,
                                response_message  VARCHAR(500),
                                received_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                                processed_at      TIMESTAMP WITH TIME ZONE,
                                retry_count       INT          NOT NULL DEFAULT 0,
                                signature_header  VARCHAR(200),
                                ip_address        VARCHAR(50),
                                created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                                updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_webhook_order_ref  ON webhook_events(order_reference);
CREATE INDEX idx_webhook_status     ON webhook_events(status);
CREATE INDEX idx_webhook_type       ON webhook_events(event_type);
CREATE INDEX idx_webhook_received   ON webhook_events(received_at);

CREATE TABLE notification_logs (
                                   id               BIGSERIAL PRIMARY KEY,
                                   order_id         BIGINT       REFERENCES orders(id),
                                   recipient_email  VARCHAR(150) NOT NULL,
                                   subject          VARCHAR(300) NOT NULL,
                                   sent_at          TIMESTAMP WITH TIME ZONE,
                                   status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
                                   error_message    VARCHAR(500),
                                   attempt_count    INT          NOT NULL DEFAULT 0,
                                   created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                                   updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notif_order_id ON notification_logs(order_id);
CREATE INDEX idx_notif_status   ON notification_logs(status);