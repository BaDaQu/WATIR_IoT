-- Tabela 1: Logi Telemetryczne (Zrzuty z czujników ESP8266)
CREATE TABLE telemetry_logs (
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
CREATE TABLE plant_profiles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,          
    moisture_threshold INT NOT NULL,    
    auto_watering BOOLEAN DEFAULT true, 
    check_interval_ms INT DEFAULT 10000 
);

-- Wrzucamy testowy profil rośliny
INSERT INTO plant_profiles (name, moisture_threshold, auto_watering, check_interval_ms)
VALUES ('Kaktus_Testowy', 200, true, 10000);
