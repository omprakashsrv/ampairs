ALTER TABLE ecom_listed_product
    ADD COLUMN search_vector tsvector
        GENERATED ALWAYS AS (
            to_tsvector('english',
                coalesce(name, '') || ' ' ||
                coalesce(brand, '') || ' ' ||
                coalesce(category, '') || ' ' ||
                coalesce(subcategory, '')
            )
        ) STORED;

CREATE INDEX idx_ecom_product_search ON ecom_listed_product USING GIN(search_vector);
