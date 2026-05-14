CREATE TABLE users (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    role VARCHAR(30) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE shelters (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    external_shelter_code VARCHAR(100),
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(30),
    address VARCHAR(255),
    region_code VARCHAR(20),
    homepage_url VARCHAR(255),
    description TEXT,
    approval_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE shelter_staff (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    shelter_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    staff_role VARCHAR(30) NOT NULL DEFAULT 'MANAGER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_shelter_staff_shelter FOREIGN KEY (shelter_id) REFERENCES shelters(id),
    CONSTRAINT fk_shelter_staff_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uq_shelter_staff UNIQUE (shelter_id, user_id)
);

CREATE TABLE animals (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    public_api_id VARCHAR(120) UNIQUE,
    shelter_id BIGINT,
    notice_no VARCHAR(100),
    desertion_no VARCHAR(100),
    species VARCHAR(50) NOT NULL,
    breed VARCHAR(150),
    sex VARCHAR(20),
    age_text VARCHAR(100),
    weight_text VARCHAR(50),
    color_text VARCHAR(100),
    neuter_status VARCHAR(20),
    special_mark TEXT,
    image_url VARCHAR(500),
    found_place VARCHAR(255),
    rescue_date DATE,
    notice_start_date DATE,
    notice_end_date DATE,
    api_status_raw VARCHAR(100),
    service_status VARCHAR(30) NOT NULL DEFAULT 'PROTECTED',
    is_adoptable BOOLEAN NOT NULL DEFAULT TRUE,
    synced_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_animals_shelter FOREIGN KEY (shelter_id) REFERENCES shelters(id)
);

CREATE TABLE animal_api_sync_logs (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    animal_id BIGINT,
    external_id VARCHAR(120) NOT NULL,
    sync_type VARCHAR(30) NOT NULL,
    sync_status VARCHAR(30) NOT NULL,
    raw_payload TEXT,
    synced_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sync_logs_animal FOREIGN KEY (animal_id) REFERENCES animals(id)
);

CREATE TABLE animal_favorites (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL,
    animal_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_animal_favorites_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_animal_favorites_animal FOREIGN KEY (animal_id) REFERENCES animals(id),
    CONSTRAINT uq_animal_favorite UNIQUE (user_id, animal_id)
);

CREATE TABLE adoption_applications (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL,
    animal_id BIGINT NOT NULL,
    shelter_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    applicant_message TEXT,
    phone VARCHAR(30),
    residence_type VARCHAR(50),
    has_pet_experience BOOLEAN,
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_adoption_applications_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_adoption_applications_animal FOREIGN KEY (animal_id) REFERENCES animals(id),
    CONSTRAINT fk_adoption_applications_shelter FOREIGN KEY (shelter_id) REFERENCES shelters(id),
    CONSTRAINT fk_adoption_applications_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id)
);

CREATE TABLE adoption_status_histories (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    application_id BIGINT NOT NULL,
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    changed_by BIGINT,
    memo TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_adoption_status_histories_application FOREIGN KEY (application_id) REFERENCES adoption_applications(id),
    CONSTRAINT fk_adoption_status_histories_changed_by FOREIGN KEY (changed_by) REFERENCES users(id)
);

CREATE TABLE donations (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL,
    shelter_id BIGINT,
    animal_id BIGINT,
    donation_target_type VARCHAR(20) NOT NULL,
    donation_type VARCHAR(20) NOT NULL DEFAULT 'ONE_TIME',
    amount DECIMAL(12, 2) NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'READY',
    payment_provider VARCHAR(50),
    payment_key VARCHAR(120),
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_donations_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_donations_shelter FOREIGN KEY (shelter_id) REFERENCES shelters(id),
    CONSTRAINT fk_donations_animal FOREIGN KEY (animal_id) REFERENCES animals(id)
);

CREATE TABLE donation_usages (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    shelter_id BIGINT NOT NULL,
    donation_id BIGINT,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    amount DECIMAL(12, 2) NOT NULL,
    used_at DATE NOT NULL,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_donation_usages_shelter FOREIGN KEY (shelter_id) REFERENCES shelters(id),
    CONSTRAINT fk_donation_usages_donation FOREIGN KEY (donation_id) REFERENCES donations(id),
    CONSTRAINT fk_donation_usages_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE notices (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    author_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notices_author FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE TABLE statistics_daily (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    stat_date DATE NOT NULL UNIQUE,
    total_animals_count INT NOT NULL DEFAULT 0,
    adoptable_animals_count INT NOT NULL DEFAULT 0,
    adopted_animals_count INT NOT NULL DEFAULT 0,
    favorite_count INT NOT NULL DEFAULT 0,
    adoption_application_count INT NOT NULL DEFAULT 0,
    donation_amount_total DECIMAL(14, 2) NOT NULL DEFAULT 0,
    active_shelter_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_animals_shelter_id ON animals (shelter_id);
CREATE INDEX idx_animals_service_status ON animals (service_status);
CREATE INDEX idx_adoption_applications_user_id ON adoption_applications (user_id);
CREATE INDEX idx_adoption_applications_animal_id ON adoption_applications (animal_id);
CREATE INDEX idx_adoption_applications_shelter_id ON adoption_applications (shelter_id);
CREATE INDEX idx_donations_user_id ON donations (user_id);
CREATE INDEX idx_donations_shelter_id ON donations (shelter_id);
CREATE INDEX idx_donations_animal_id ON donations (animal_id);
