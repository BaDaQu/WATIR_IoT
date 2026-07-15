-- ==========================================
-- WATIR IoT — Inicjalizacja bazy danych
-- Issue #22: Optymalizacja zapytań i retencja
-- ==========================================

-- Tabela 1: Logi Telemetryczne (Zrzuty z czujników ESP8266)
CREATE TABLE IF NOT EXISTS telemetry_logs (
    id SERIAL PRIMARY KEY,
    device_id VARCHAR(50) NOT NULL,
    timestamp BIGINT NOT NULL,          -- UNIX Epoch z JSONa
    temp REAL,                          -- Temperatura
    humidity INT,                       -- Wilgotność powietrza
    soil_moisture_1 INT,                -- Wilgotność gleby 1 (A0)
    soil_moisture_2 INT,                -- Wilgotność gleby 2 (A1)
    water_level_cm INT,                 -- Poziom wody HC-SR04
    water_error BOOLEAN,                -- Zabezpieczenie na sucho
    pump_active BOOLEAN,                -- Czy pompa lała
    pan INT,                            -- Pozycja serwa X
    tilt INT,                           -- Pozycja serwa Y
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela 2: Profile Roślin (Konfiguracja dla ESP8266)
-- UWAGA: moisture_threshold w % (0-100). Dodano pan/tilt (0-180 stopni) dla ramienia.
CREATE TABLE IF NOT EXISTS plant_profiles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    moisture_threshold INT NOT NULL CHECK (moisture_threshold >= 0 AND moisture_threshold <= 100),
    auto_watering BOOLEAN DEFAULT TRUE,
    check_interval_ms INT DEFAULT 10000,
    pan INT NOT NULL CHECK (pan >= 0 AND pan <= 180) DEFAULT 90,
    tilt INT NOT NULL CHECK (tilt >= 0 AND tilt <= 180) DEFAULT 90,
    sensor INT DEFAULT 1 CHECK (sensor IN (1, 2)),
    pump_power INT DEFAULT 70 CHECK (pump_power >= 0 AND pump_power <= 100)
);

-- ==========================================
-- INDEKSY — Issue #22: Optymalizacja zapytań
-- ==========================================

-- Indeks główny: backend zawsze robi ORDER BY timestamp DESC
CREATE INDEX IF NOT EXISTS idx_telemetry_time ON telemetry_logs (timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_telemetry_device ON telemetry_logs (device_id);

-- ==========================================
-- Tabela 3: Ustawienia Globalne Systemu (BME280)
-- ==========================================
CREATE TABLE IF NOT EXISTS global_settings (
    id SERIAL PRIMARY KEY,
    min_temp_block INT DEFAULT 5,
    max_temp_force INT DEFAULT 35,
    min_air_humidity_force INT DEFAULT 30
);

-- Wstawienie wartości domyślnych (jeśli tabela jest pusta)
INSERT INTO global_settings (id, min_temp_block, max_temp_force, min_air_humidity_force)
VALUES (1, 5, 35, 30)
ON CONFLICT (id) DO NOTHING;

-- Indeks złożony: device_id + timestamp razem — najszybszy dla zapytań
CREATE INDEX IF NOT EXISTS idx_telemetry_device_timestamp
    ON telemetry_logs (device_id, timestamp DESC);

-- ==========================================
-- DANE TESTOWE
-- ==========================================

-- Wrzucamy testowy profil rośliny (jeśli nie istnieje) z domyślnym położeniem serw (X: 120, Y: 30) i przypisanym czujnikiem G1
INSERT INTO plant_profiles (name, moisture_threshold, auto_watering, check_interval_ms, pan, tilt, sensor)
SELECT 'Kaktus_Testowy', 20, true, 10000, 120, 30, 1
WHERE NOT EXISTS (
    SELECT 1 FROM plant_profiles WHERE name = 'Kaktus_Testowy'
);
