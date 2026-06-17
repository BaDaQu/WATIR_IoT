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
    soil_moisture INT,                  -- Wilgotność gleby
    water_level_cm INT,                 -- Poziom wody HC-SR04
    water_error BOOLEAN,                -- Zabezpieczenie na sucho
    pump_active BOOLEAN,                -- Czy pompa lała
    pan INT,                            -- Pozycja serwa X
    tilt INT,                           -- Pozycja serwa Y
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela 2: Profile Roślin (Konfiguracja dla ESP8266)
-- UWAGA: moisture_threshold przechowywany w PROCENTACH (0–100)
CREATE TABLE IF NOT EXISTS plant_profiles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    moisture_threshold INT NOT NULL CHECK (moisture_threshold >= 0 AND moisture_threshold <= 100),
    auto_watering BOOLEAN DEFAULT true,
    check_interval_ms INT DEFAULT 10000
);

-- ==========================================
-- INDEKSY — Issue #22: Optymalizacja zapytań
-- ==========================================

-- Indeks główny: backend zawsze robi ORDER BY timestamp DESC
-- Największy zysk wydajnościowy przy dużej tabeli
CREATE INDEX IF NOT EXISTS idx_telemetry_timestamp
    ON telemetry_logs (timestamp DESC);

-- Indeks pomocniczy: filtrowanie po urządzeniu (GET /api/telemetry?device_id=...)
-- Potrzebny gdy w sieci jest więcej niż jedno urządzenie WATIR
CREATE INDEX IF NOT EXISTS idx_telemetry_device_id
    ON telemetry_logs (device_id);

-- Indeks złożony: device_id + timestamp razem — najszybszy dla zapytań
-- "ostatnie N rekordów z konkretnego urządzenia"
CREATE INDEX IF NOT EXISTS idx_telemetry_device_timestamp
    ON telemetry_logs (device_id, timestamp DESC);

-- ==========================================
-- DANE TESTOWE
-- ==========================================

-- POPRAWKA #2: moisture_threshold = 20 (procenty), było 200 (wartość ADC)
-- Constraint CHECK na tabeli odrzuciłby teraz wartość 200
INSERT INTO plant_profiles (name, moisture_threshold, auto_watering, check_interval_ms)
SELECT 'Kaktus_Testowy', 20, true, 10000
WHERE NOT EXISTS (
    SELECT 1 FROM plant_profiles WHERE name = 'Kaktus_Testowy'
);