-- Pet health profile extensions + structured medical history (MySQL, idempotent).

SET @db := DATABASE();

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @db AND table_name = 'pets' AND column_name = 'allergies') > 0,
  'SELECT 1',
  'ALTER TABLE pets ADD COLUMN allergies VARCHAR(500) NULL'
);
PREPARE _bp_stmt FROM @sql; EXECUTE _bp_stmt; DEALLOCATE PREPARE _bp_stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @db AND table_name = 'pets' AND column_name = 'chronic_conditions') > 0,
  'SELECT 1',
  'ALTER TABLE pets ADD COLUMN chronic_conditions VARCHAR(500) NULL'
);
PREPARE _bp_stmt FROM @sql; EXECUTE _bp_stmt; DEALLOCATE PREPARE _bp_stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @db AND table_name = 'pets' AND column_name = 'known_medications') > 0,
  'SELECT 1',
  'ALTER TABLE pets ADD COLUMN known_medications TEXT NULL'
);
PREPARE _bp_stmt FROM @sql; EXECUTE _bp_stmt; DEALLOCATE PREPARE _bp_stmt;

CREATE TABLE IF NOT EXISTS pet_medical_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pet_id BIGINT NOT NULL,
    record_type VARCHAR(50) NOT NULL,
    record_date DATE NULL,
    title VARCHAR(255) NULL,
    description TEXT NULL,
    source_clinic VARCHAR(255) NULL,
    veterinarian VARCHAR(255) NULL,
    vaccine_type VARCHAR(100) NULL,
    dose_number VARCHAR(50) NULL,
    next_due_date DATE NULL,
    diagnosis TEXT NULL,
    treatment_plan TEXT NULL,
    test_results TEXT NULL,
    medications TEXT NULL,
    attachment_url VARCHAR(500) NULL,
    attachment_name VARCHAR(255) NULL,
    external_record TINYINT(1) DEFAULT 0,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_pet_medical_records_pet_id (pet_id),
    INDEX idx_pet_medical_records_type (record_type)
);
