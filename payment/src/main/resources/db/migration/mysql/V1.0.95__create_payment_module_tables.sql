-- Payment & Collection Module Migration (MySQL)
-- Description: Subsidiary party ledger — party balances, ledger entries, payment vouchers,
--              payment allocations and adjustment vouchers. Money DECIMAL(19,4); timestamps TIMESTAMP.
-- Spec: 013-payment-collection

-- =====================================================
-- Party balance (one row per party)
-- =====================================================
CREATE TABLE party_balance (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    party_uid VARCHAR(40) NOT NULL,
    opening_balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    opening_direction VARCHAR(4) NOT NULL DEFAULT 'DR',
    opening_as_of TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cached_closing_balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    last_computed_at TIMESTAMP NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_party_balance_uid (uid),
    UNIQUE INDEX uk_party_balance_owner_party (owner_id, party_uid),
    INDEX idx_party_balance_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Per-party opening + cached signed closing balance (recomputable)';

-- =====================================================
-- Ledger entry (sole driver of the balance)
-- =====================================================
CREATE TABLE ledger_entry (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    party_uid VARCHAR(40) NOT NULL,
    entry_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    entry_type VARCHAR(30) NOT NULL,
    direction VARCHAR(4) NOT NULL,
    amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    source_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    source_uid VARCHAR(64),
    voucher_no VARCHAR(64),
    narration VARCHAR(500),
    reversal_of VARCHAR(64),
    reversed BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_ledger_entry_uid (uid),
    INDEX idx_ledger_entry_party_date (owner_id, party_uid, entry_date),
    INDEX idx_ledger_entry_source (owner_id, source_type, source_uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Signed party-ledger postings (DR=+amount, CR=-amount)';

-- =====================================================
-- Payment voucher (money movement header)
-- =====================================================
CREATE TABLE payment_voucher (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    party_uid VARCHAR(40) NOT NULL,
    voucher_no VARCHAR(64),
    voucher_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    direction VARCHAR(10) NOT NULL DEFAULT 'RECEIVED',
    total_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    payment_mode VARCHAR(20) NOT NULL DEFAULT 'CASH',
    reference_number VARCHAR(100),
    instrument_date TIMESTAMP NULL,
    bank_name VARCHAR(120),
    clearance_status VARCHAR(12) NOT NULL DEFAULT 'CLEARED',
    unallocated_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    narration VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_payment_voucher_uid (uid),
    INDEX idx_payment_voucher_party (owner_id, party_uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Receipts (in) / payments (out) per party';

-- =====================================================
-- Payment allocation (matching — aging/open-bills only)
-- =====================================================
CREATE TABLE payment_allocation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    payment_voucher_uid VARCHAR(64) NOT NULL,
    target_type VARCHAR(16) NOT NULL DEFAULT 'INVOICE',
    target_uid VARCHAR(64) NOT NULL,
    amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_payment_allocation_uid (uid),
    INDEX idx_payment_allocation_voucher (owner_id, payment_voucher_uid),
    INDEX idx_payment_allocation_target (owner_id, target_type, target_uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Receipt-to-bill matching; never affects the party balance';

-- =====================================================
-- Adjustment voucher (non-payment movements)
-- =====================================================
CREATE TABLE adjustment_voucher (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    party_uid VARCHAR(40) NOT NULL,
    voucher_no VARCHAR(64),
    voucher_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    adjustment_type VARCHAR(20) NOT NULL DEFAULT 'ADJUSTMENT',
    amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    narration VARCHAR(500),
    source_ref VARCHAR(64),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_adjustment_voucher_uid (uid),
    INDEX idx_adjustment_voucher_party (owner_id, party_uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Returns/notes/write-offs that post a signed ledger entry';
