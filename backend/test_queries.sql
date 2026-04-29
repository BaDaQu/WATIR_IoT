-- ==========================================
-- TESTY DLA TABELI: plant_profiles (Profile)
-- ==========================================

-- 1. DODAWANIE (CREATE) - Dodajemy Paproć, która lubi wilgoć
INSERT INTO plant_profiles (name, moisture_threshold, auto_watering, check_interval_ms)
VALUES ('Paproć', 60, true, 5000);

-- 2. ODCZYT (READ) - Wyświetlamy wszystkie rośliny
SELECT * FROM plant_profiles;

-- 3. MODYFIKACJA (UPDATE) - Zmieniamy próg dla Paproci na 65%
UPDATE plant_profiles 
SET moisture_threshold = 65 
WHERE name = 'Paproć';

-- 4. USUWANIE (DELETE) - Usuwamy profil testowy (jeśli istnieje)
DELETE FROM plant_profiles 
WHERE name = 'Kaktus_Testowy';


-- ==========================================
-- TESTY DLA TABELI: telemetry_logs (Logi)
-- ==========================================

-- 1. DODAWANIE (CREATE) - Symulacja masowego zrzutu z ESP8266
INSERT INTO telemetry_logs (device_id, timestamp, temp, humidity, soil_moisture, water_level_cm, water_error, pump_active, pan, tilt)
VALUES 
('WATIR_01', 1713341614, 24.5, 46, 40, 9, false, true, 90, 45),
('WATIR_01', 1713341620, 24.5, 46, 65, 8, false, false, 90, 45);

-- 2. ODCZYT (READ) - Pobieramy historię logów (np. do wykresów w apce)
SELECT timestamp, temp, soil_moisture, pump_active 
FROM telemetry_logs 
ORDER BY timestamp DESC;