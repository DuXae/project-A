-- V3: мягкое удаление товаров (FR-6).
-- NULL = активная запись, заполненное значение = удалённая.

ALTER TABLE products ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_products_deleted_at ON products (deleted_at);
