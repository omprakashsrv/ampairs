-- Seller's place of supply (origin state). IGST applies iff it differs from the buyer's
-- place_of_supply; both are snapshotted so the scenario is a pure state-code compare at edit.
ALTER TABLE invoice ADD COLUMN seller_place_of_supply VARCHAR(255);
