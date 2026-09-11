-- V36 accidentally reused the platform dictionary table name express_company.
-- Rename it so the tenant interceptor applies shop_id filtering automatically.
RENAME TABLE express_company TO store_express_company;
