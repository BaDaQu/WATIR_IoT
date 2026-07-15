require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { Pool } = require('pg');
const dgram = require('dgram');

const app = express();
app.use(cors());
app.use(express.json());

// Słownik przechowujący ostatnio znane IP urządzeń w sieci lokalnej (Auto-discovery)
const activeDevices = {};

// ==========================================
// UDP AUTO-DISCOVERY SERVER
// ==========================================
const udpServer = dgram.createSocket('udp4');

udpServer.on('message', (msg, rinfo) => {
  const message = msg.toString();
  if (message.startsWith('WATIR_DISCOVER_CLIENT:')) {
    const deviceId = message.split(':')[1];
    activeDevices[deviceId] = rinfo.address;
    console.log(`[UDP] Odkryto urządzenie ${deviceId} pod adresem IP: ${rinfo.address}`);
    
    // Odpowiedz serwera do klienta (Unicast)
    const reply = Buffer.from('WATIR_DISCOVER_SERVER');
    udpServer.send(reply, 3000, rinfo.address, (err) => {
      if (err) console.error('[UDP] Błąd podczas wysyłania odpowiedzi:', err);
    });
  }
});

udpServer.on('error', (err) => {
  console.error(`[UDP] Błąd serwera UDP:\n${err.stack}`);
  udpServer.close();
});

udpServer.bind(3000, () => {
  console.log('✅ Serwer UDP Auto-Discovery nasłuchuje na porcie 3000');
});

// ==========================================
// KONFIGURACJA RETENCJI — Issue #22
// Maksymalna liczba rekordów na urządzenie
// ==========================================
const RETENTION_LIMIT = 1000;

// Połączenie z PostgreSQL
const pool = new Pool({
  host: process.env.DB_HOST,
  port: process.env.DB_PORT,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
});

// POPRAWKA #5: Crashowanie przy braku połączenia z bazą,
// żeby Docker Compose mógł zrestartować kontener
pool.connect()
  .then(() => console.log('✅ Połączono z bazą PostgreSQL!'))
  .catch(err => {
    console.error('❌ Krytyczny błąd połączenia z bazą:', err.stack);
    process.exit(1); // Zamknięcie procesu z kodem błędu dla Dockera
  });

// ==========================================
// FUNKCJA RETENCJI — Issue #22
// Usuwa stare rekordy, zostawia ostatnie RETENTION_LIMIT
// na urządzenie. Wywoływana po każdym INSERT.
// ==========================================
async function enforceRetention(device_id) {
  try {
    const result = await pool.query(
      `DELETE FROM telemetry_logs
       WHERE device_id = $1
         AND id NOT IN (
           SELECT id FROM telemetry_logs
           WHERE device_id = $1
           ORDER BY timestamp DESC
           LIMIT $2
         )`,
      [device_id, RETENTION_LIMIT]
    );
    if (result.rowCount > 0) {
      console.log(`🧹 Retencja [${device_id}]: usunięto ${result.rowCount} starych rekordów`);
    }
  } catch (err) {
    console.error('⚠️ Błąd retencji:', err.message);
  }
}

// ==========================================
// ENDPOINT 1: POST /api/telemetry (Dla ESP8266)
// Odbiera JSON z ESP, zapisuje do bazy,
// automatycznie zapamiętuje rzeczywiste IP przysłane w JSONie,
// następnie wymusza retencję dla tego urządzenia
// ==========================================
app.post('/api/telemetry', async (req, res) => {
  try {
    const data = req.body;

    // Walidacja wymaganych pól
    if (!data.device_id || !data.sensors || !data.status || !data.servos) {
      return res.status(400).json({
        status: 'error',
        message: 'Brakujące pola: wymagane device_id, sensors, status, servos'
      });
    }

    const query = `
      INSERT INTO telemetry_logs
      (device_id, timestamp, temp, humidity, soil_moisture_1, soil_moisture_2, water_level_cm, water_error, pump_active, pan, tilt)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
      RETURNING *;
    `;

    const values = [
      data.device_id,
      data.timestamp || Math.floor(Date.now() / 1000),
      data.sensors.temp,
      data.sensors.humidity,
      data.sensors.soil_moisture_1,
      data.sensors.soil_moisture_2,
      data.sensors.water_level_cm,
      data.status.water_error,
      data.status.pump_active,
      data.servos.pan,
      data.servos.tilt
    ];

    const result = await pool.query(query, values);

    // Automatyczne wykrywanie i zapisywanie adresu IP ESP8266 (z nagłówka lub z połączenia)
    const rawIp = req.headers['x-forwarded-for'] || req.socket.remoteAddress;
    const clientIp = rawIp.includes('::ffff:') ? rawIp.split('::ffff:')[1] : rawIp;

    if (data.device_id) {
      activeDevices[data.device_id] = clientIp;
      console.log(`📡 Urządzenie ${data.device_id} zameldowało się z IP: ${clientIp}`);
    }

    // Retencja w tle — nie blokuje odpowiedzi dla ESP
    enforceRetention(data.device_id);

    res.status(201).json({
      status: 'success',
      message: 'Log zapisany poprawnie!',
      saved: result.rows[0]
    });

  } catch (error) {
    console.error(error);
    res.status(500).json({ status: 'error', message: 'Błąd zapisu do bazy danych.' });
  }
});

// ==========================================
// ENDPOINT 2: GET /api/telemetry (Dla Aplikacji)
// Query params: device_id, limit
// ==========================================
app.get('/api/telemetry', async (req, res) => {
  try {
    const { device_id, limit } = req.query;
    const safeLimit = Math.min(parseInt(limit) || 50, 500);

    let result;

    if (device_id) {
      result = await pool.query(
        `SELECT * FROM telemetry_logs
         WHERE device_id = $1
         ORDER BY timestamp DESC
         LIMIT $2`,
        [device_id, safeLimit]
      );
    } else {
      result = await pool.query(
        `SELECT * FROM telemetry_logs
         ORDER BY timestamp DESC
         LIMIT $1`,
        [safeLimit]
      );
    }

    res.status(200).json({ status: 'success', data: result.rows });

  } catch (error) {
    console.error(error);
    res.status(500).json({ status: 'error', message: 'Błąd odczytu z bazy danych.' });
  }
});

// ==========================================
// ENDPOINT 3: GET /api/telemetry/stats
// Zwraca statystyki retencji — pomocne do debugowania
// ==========================================
app.get('/api/telemetry/stats', async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT
         device_id,
         COUNT(*) AS total_records,
         MIN(timestamp) AS oldest_timestamp,
         MAX(timestamp) AS newest_timestamp,
         ROUND(COUNT(*) * 100.0 / $1, 1) AS retention_usage_pct
       FROM telemetry_logs
       GROUP BY device_id
       ORDER BY device_id`,
      [RETENTION_LIMIT]
    );

    res.status(200).json({
      status: 'success',
      retention_limit: RETENTION_LIMIT,
      devices: result.rows
    });

  } catch (error) {
    console.error(error);
    res.status(500).json({ status: 'error', message: 'Błąd pobierania statystyk.' });
  }
});

// ==========================================
// ENDPOINT 4: DELETE /api/telemetry
// Ręczne czyszczenie bazy
// ==========================================
app.delete('/api/telemetry', async (req, res) => {
  try {
    const { device_id, mode = 'retain' } = req.query;

    if (!device_id) {
      return res.status(400).json({
        status: 'error',
        message: 'Wymagany parametr: device_id'
      });
    }

    let deletedCount;

    if (mode === 'purge') {
      const result = await pool.query(
        'DELETE FROM telemetry_logs WHERE device_id = $1',
        [device_id]
      );
      deletedCount = result.rowCount;
    } else {
      const result = await pool.query(
        `DELETE FROM telemetry_logs
         WHERE device_id = $1
           AND id NOT IN (
             SELECT id FROM telemetry_logs
             WHERE device_id = $1
             ORDER BY timestamp DESC
             LIMIT $2
           )`,
        [device_id, RETENTION_LIMIT]
      );
      deletedCount = result.rowCount;
    }

    res.status(200).json({
      status: 'success',
      message: `Usunięto ${deletedCount} rekordów`,
      device_id,
      mode,
      deleted: deletedCount
    });

  } catch (error) {
    console.error(error);
    res.status(500).json({ status: 'error', message: 'Błąd czyszczenia bazy.' });
  }
});

// ==========================================
// ENDPOINT 5: POST /api/move (Dla Aplikacji)
// Odbiera ruch z joysticka i przekazuje bezpośrednio do ESP8266
// ==========================================
app.post('/api/move', async (req, res) => {
  try {
    const { device_id = 'WATIR_01', axis, value, direction } = req.body;

    if (!direction && (!axis || value === undefined)) {
      return res.status(400).json({
        status: 'error',
        message: 'Brakujące parametry: wymagane axis i value, LUB direction'
      });
    }

    const espIp = activeDevices[device_id];

    if (!espIp) {
      return res.status(404).json({
        status: 'error',
        message: `Urządzenie o ID ${device_id} nie wysłało jeszcze telemetrii (IP nieznane).`
      });
    }

    console.log(`➡️ Przekierowanie komendy do ${device_id} na adres: http://${espIp}/api/move`);
    
    const payload = direction ? { direction } : { axis, value };

    const response = await fetch(`http://${espIp}/api/move`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    });

    if (response.ok) {
      const espResponse = await response.json();
      return res.status(200).json({
        status: 'success',
        message: 'Komenda przekazana pomyślnie do ESP8266',
        esp_response: espResponse
      });
    } else {
      return res.status(response.status).json({
        status: 'error',
        message: 'ESP odrzuciło żądanie',
        esp_status: response.status
      });
    }

  } catch (error) {
    console.error('❌ Błąd przesyłania do ESP:', error.message);
    res.status(500).json({
      status: 'error',
      message: 'Błąd komunikacji z ESP. Upewnij się, że urządzenie jest uruchomione i podłączone do tej samej sieci.'
    });
  }
});

// ==========================================
// ENDPOINT 6: POST /api/pump (Dla Aplikacji)
// Ustawianie mocy pompy i ręczne podlewanie
// ==========================================
app.post('/api/pump', async (req, res) => {
  try {
    const { device_id = 'WATIR_01', power, duration, pan, tilt } = req.body;

    const espIp = activeDevices[device_id];

    if (!espIp) {
      return res.status(404).json({
        status: 'error',
        message: `Urządzenie o ID ${device_id} nie wysłało jeszcze telemetrii (IP nieznane).`
      });
    }

    const payload = {};
    if (power !== undefined) payload.power = power;
    if (duration !== undefined) payload.duration = duration;
    if (pan !== undefined) payload.pan = pan;
    if (tilt !== undefined) payload.tilt = tilt;

    console.log(`➡️ Przekierowanie komendy pompy do ${device_id} na adres: http://${espIp}/api/pump`);

    const response = await fetch(`http://${espIp}/api/pump`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    });

    if (response.ok) {
      const espResponse = await response.json();
      return res.status(200).json({
        status: 'success',
        message: 'Komenda pompy przekazana pomyślnie do ESP8266',
        esp_response: espResponse
      });
    } else {
      return res.status(response.status).json({
        status: 'error',
        message: 'ESP odrzuciło żądanie',
        esp_status: response.status
      });
    }

  } catch (error) {
    console.error('❌ Błąd przesyłania do ESP:', error.message);
    res.status(500).json({
      status: 'error',
      message: 'Błąd komunikacji z ESP. Upewnij się, że urządzenie jest uruchomione.'
    });
  }
});

// ==========================================
// PROFIL ROŚLIN — pełny CRUD z pozycjami serw (X/Y)
// Podstawa pod issue #25 i #28
// ==========================================

function validatePlantProfile(data, requireAll = true) {
  const errors = [];
  if (requireAll && !data.name) errors.push('name jest wymagany');
  if (requireAll && data.moisture_threshold === undefined) errors.push('moisture_threshold jest wymagany');

  if (data.name !== undefined && typeof data.name !== 'string') errors.push('name musi być tekstem');
  if (data.moisture_threshold !== undefined && (
    typeof data.moisture_threshold !== 'number' ||
    data.moisture_threshold < 0 ||
    data.moisture_threshold > 100
  )) errors.push('moisture_threshold musi być liczbą 0–100 (procenty)');
  if (data.auto_watering !== undefined && typeof data.auto_watering !== 'boolean') errors.push('auto_watering musi być true/false');
  if (data.check_interval_ms !== undefined && (
    typeof data.check_interval_ms !== 'number' ||
    data.check_interval_ms < 1000
  )) errors.push('check_interval_ms musi być liczbą >= 1000');

  // Walidacja kątów serw (0-180 stopni)
  if (data.pan !== undefined && (typeof data.pan !== 'number' || data.pan < 0 || data.pan > 180))
    errors.push('pan musi być liczbą w zakresie 0-180');
  if (data.tilt !== undefined && (typeof data.tilt !== 'number' || data.tilt < 0 || data.tilt > 180))
    errors.push('tilt musi być liczbą w zakresie 0-180');
    
  if (data.sensor !== undefined && (typeof data.sensor !== 'number' || (data.sensor !== 1 && data.sensor !== 2)))
    errors.push('sensor musi mieć wartość 1 (G1) lub 2 (G2)');

  return errors;
}

// GET /api/plants — lista wszystkich profili
app.get('/api/plants', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM plant_profiles ORDER BY id');
    res.status(200).json({ status: 'success', data: result.rows });
  } catch (error) {
    console.error(error);
    res.status(500).json({ status: 'error', message: 'Błąd odczytu profili.' });
  }
});

// GET /api/plants/:id — jeden profil po ID
app.get('/api/plants/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const result = await pool.query('SELECT * FROM plant_profiles WHERE id = $1', [id]);
    if (result.rows.length === 0) return res.status(404).json({ status: 'error', message: `Profil o ID ${id} nie istnieje.` });
    res.status(200).json({ status: 'success', data: result.rows[0] });
  } catch (error) {
    console.error(error);
    res.status(500).json({ status: 'error', message: 'Błąd odczytu profilu.' });
  }
});

// POST /api/plants — utwórz nowy profil (z pozycjami serw)
app.post('/api/plants', async (req, res) => {
  try {
    const data = req.body;
    const errors = validatePlantProfile(data, true);
    if (errors.length > 0) return res.status(400).json({ status: 'error', message: errors.join(', ') });

    const result = await pool.query(
      `INSERT INTO plant_profiles (name, moisture_threshold, auto_watering, check_interval_ms, pan, tilt, sensor, pump_power)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8) RETURNING *`,
      [
        data.name,
        data.moisture_threshold,
        data.auto_watering ?? true,
        data.check_interval_ms ?? 10000,
        data.pan ?? 90,
        data.tilt ?? 90,
        data.sensor ?? 1,
        data.pump_power ?? 70
      ]
    );
    res.status(201).json({ status: 'success', message: `Profil "${data.name}" został utworzony.`, data: result.rows[0] });
  } catch (error) {
    console.error(error);
    res.status(500).json({ status: 'error', message: 'Błąd tworzenia profilu.' });
  }
});

// PUT /api/plants/:id — nadpisz cały profil
app.put('/api/plants/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const data = req.body;
    const errors = validatePlantProfile(data, true);
    if (errors.length > 0) return res.status(400).json({ status: 'error', message: errors.join(', ') });

    const result = await pool.query(
      `UPDATE plant_profiles
       SET name = $1,
           moisture_threshold = $2,
           auto_watering = $3,
           check_interval_ms = $4,
           pan = $5,
           tilt = $6,
           sensor = $7,
           pump_power = $8
       WHERE id = $9 RETURNING *`,
      [
        data.name,
        data.moisture_threshold,
        data.auto_watering ?? true,
        data.check_interval_ms ?? 10000,
        data.pan ?? 90,
        data.tilt ?? 90,
        data.sensor ?? 1,
        data.pump_power ?? 70,
        id
      ]
    );
    if (result.rows.length === 0) return res.status(404).json({ status: 'error', message: `Profil o ID ${id} nie istnieje.` });
    res.status(200).json({ status: 'success', message: `Profil ID ${id} został zaktualizowany.`, data: result.rows[0] });
  } catch (error) {
    console.error(error);
    res.status(500).json({ status: 'error', message: 'Błąd aktualizacji profilu.' });
  }
});

// PATCH /api/plants/:id — częściowy update (np. tylko zmiana pozycji serwa z telefonu)
app.patch('/api/plants/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const data = req.body;
    if (Object.keys(data).length === 0) return res.status(400).json({ status: 'error', message: 'Brak pól do aktualizacji.' });

    const errors = validatePlantProfile(data, false);
    if (errors.length > 0) return res.status(400).json({ status: 'error', message: errors.join(', ') });

    const allowed = ['name', 'moisture_threshold', 'auto_watering', 'check_interval_ms', 'pan', 'tilt', 'sensor', 'pump_power'];
    const fields = Object.keys(data).filter(k => allowed.includes(k));
    if (fields.length === 0) return res.status(400).json({ status: 'error', message: 'Żadne z podanych pól nie jest dozwolone.' });

    const setClauses = fields.map((field, i) => `${field} = $${i + 1}`).join(', ');
    const values = fields.map(f => data[f]);
    values.push(id);

    const result = await pool.query(`UPDATE plant_profiles SET ${setClauses} WHERE id = $${values.length} RETURNING *`, values);
    if (result.rows.length === 0) return res.status(404).json({ status: 'error', message: `Profil o ID ${id} nie istnieje.` });
    res.status(200).json({ status: 'success', message: `Profil ID ${id} został częściowo zaktualizowany.`, data: result.rows[0] });
  } catch (error) {
    console.error(error);
    res.status(500).json({ status: 'error', message: 'Błąd częściowej aktualizacji profilu.' });
  }
});

// DELETE /api/plants/:id — usuwanie profilu
app.delete('/api/plants/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const result = await pool.query('DELETE FROM plant_profiles WHERE id = $1 RETURNING *', [id]);
    if (result.rows.length === 0) return res.status(404).json({ status: 'error', message: `Profil o ID ${id} nie istnieje.` });
    
    const deletedProfile = result.rows[0];
    
    // Próba poinformowania ESP32 o usunięciu profilu, aby przestało podlewać
    const device_id = 'WATIR_01'; // Domyślne urządzenie
    const espIp = activeDevices[device_id];
    let espNotified = false;
    
    if (espIp) {
      const clearConfig = {
        config: {
          target_plant: deletedProfile.sensor === 1 ? 1 : 2,
          name: "Brak",
          moisture_threshold: 0 // Ważne: ustawienie na 0 wyłącza automatyczne podlewanie
        }
      };
      
      try {
        await fetch(`http://${espIp}/api/config`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(clearConfig),
        });
        espNotified = true;
        console.log(`📡 Wysłano komendę czyszczenia profilu ${deletedProfile.name} do ESP32.`);
      } catch (e) {
        console.warn(`⚠️ Nie udało się powiadomić ESP32 o usunięciu profilu: ${e.message}`);
      }
    }

    res.status(200).json({ 
      status: 'success', 
      message: `Profil "${deletedProfile.name}" został usunięty.${espNotified ? ' Pomyślnie wyczyszczono na ESP32.' : ' ESP32 aktualnie niedostępne (zostanie zaktualizowane przy najbliższej synchronizacji).'}`, 
      deleted: deletedProfile 
    });
  } catch (error) {
    console.error(error);
    res.status(500).json({ status: 'error', message: 'Błąd usuwania profilu.' });
  }
});

// POST /api/plants/:id/apply — zaaplikowanie profilu (wysyła config wraz z kątami pan/tilt do ESP)
app.post('/api/plants/:id/apply', async (req, res) => {
  try {
    const { id } = req.params;
    const { device_id = 'WATIR_01' } = req.body || {};

    const result = await pool.query('SELECT * FROM plant_profiles WHERE id = $1', [id]);
    if (result.rows.length === 0) return res.status(404).json({ status: 'error', message: `Profil o ID ${id} nie istnieje.` });

    const profile = result.rows[0];

    // Pobierz ustawienia globalne
    const settingsResult = await pool.query('SELECT * FROM global_settings WHERE id = 1');
    const globalSettings = settingsResult.rows.length > 0 ? settingsResult.rows[0] : { min_temp_block: 5, max_temp_force: 35, min_air_humidity_force: 30 };

    // Dodajemy pozycje serw (pan/tilt) i identyfikator rośliny do konfiguracji wysyłanej do ESP
    const espConfig = {
      config: {
        target_plant: profile.sensor === 1 ? 1 : 2, // Mapujemy na podstawie podłączonego czujnika, a nie ID w bazie!
        name: profile.name,
        moisture_threshold: profile.moisture_threshold,
        auto_watering: profile.auto_watering,
        check_interval_ms: profile.check_interval_ms,
        pan: profile.pan,
        tilt: profile.tilt,
        sensor: profile.sensor,
        pump_power: profile.pump_power,
        // Dodajemy ustawienia globalne
        min_temp_block: globalSettings.min_temp_block,
        max_temp_force: globalSettings.max_temp_force,
        min_air_humidity_force: globalSettings.min_air_humidity_force
      }
    };

    const espIp = activeDevices[device_id];

    if (!espIp) {
      console.warn(`⚠️ Urządzenie ${device_id} nieznane (brak telemetrii). Profil zapisany tylko w odpowiedzi.`);
      return res.status(200).json({
        status: 'success',
        message: `Profil "${profile.name}" gotowy, ale urządzenie ${device_id} nie jest dostępne (brak telemetrii).`,
        applied_profile: profile,
        esp_config: espConfig,
        esp_sent: false
      });
    }

    console.log(`📡 Aplikowanie profilu "${profile.name}" (ID: ${id}) do ${device_id} na http://${espIp}/api/config`);

    const espResponse = await fetch(`http://${espIp}/api/config`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(espConfig),
    });

    if (espResponse.ok) {
      return res.status(200).json({
        status: 'success',
        message: `Profil "${profile.name}" został pomyślnie wysłany do urządzenia.`,
        applied_profile: profile,
        esp_config: espConfig,
        esp_sent: true
      });
    } else {
      return res.status(502).json({
        status: 'error',
        message: `Profil znaleziony, ale ESP odrzuciło konfigurację (HTTP ${espResponse.status}).`,
        applied_profile: profile,
        esp_sent: false
      });
    }

  } catch (error) {
    console.error(error);
    res.status(500).json({ status: 'error', message: 'Błąd aplikowania profilu.' });
  }
});

// ==========================================
// USTAWIENIA GLOBALNE (BME280)
// ==========================================

// GET /api/settings
app.get('/api/settings', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM global_settings WHERE id = 1');
    if (result.rows.length === 0) {
      return res.status(404).json({ status: 'error', message: 'Brak ustawień globalnych.' });
    }
    res.status(200).json({ status: 'success', data: result.rows[0] });
  } catch (error) {
    console.error(error);
    res.status(500).json({ status: 'error', message: 'Błąd pobierania ustawień.' });
  }
});

// POST /api/settings
app.post('/api/settings', async (req, res) => {
  try {
    const { min_temp_block, max_temp_force, min_air_humidity_force } = req.body;
    
    // Zapis w bazie
    const result = await pool.query(
      `UPDATE global_settings 
       SET min_temp_block = COALESCE($1, min_temp_block), 
           max_temp_force = COALESCE($2, max_temp_force), 
           min_air_humidity_force = COALESCE($3, min_air_humidity_force)
       WHERE id = 1 RETURNING *`,
      [min_temp_block, max_temp_force, min_air_humidity_force]
    );

    const settings = result.rows[0];

    // Natychmiastowa wysyłka do ESP32
    const device_id = 'WATIR_01';
    const espIp = activeDevices[device_id];
    let espSent = false;

    if (espIp) {
      const espConfig = {
        config: {
          min_temp_block: settings.min_temp_block,
          max_temp_force: settings.max_temp_force,
          min_air_humidity_force: settings.min_air_humidity_force
        }
      };
      
      try {
        await fetch(`http://${espIp}/api/config`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(espConfig),
        });
        espSent = true;
        console.log(`📡 Wysłano ustawienia globalne do ${device_id}`);
      } catch (e) {
        console.warn(`⚠️ Nie udało się wysłać ustawień do ESP32: ${e.message}`);
      }
    }

    res.status(200).json({ 
      status: 'success', 
      message: 'Ustawienia globalne zapisane.', 
      data: settings,
      esp_sent: espSent
    });

  } catch (error) {
    console.error(error);
    res.status(500).json({ status: 'error', message: 'Błąd zapisu ustawień.' });
  }
});

// ==========================================
// Globalny error handler Express
// ==========================================
app.use((err, req, res, next) => {
  console.error('Nieobsłużony błąd:', err);
  res.status(500).json({ status: 'error', message: 'Wewnętrzny błąd serwera.' });
});

// Start serwera
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`🌿 Serwer WATIR API działa na http://localhost:${PORT}`);
  console.log(`📦 Limit retencji: ${RETENTION_LIMIT} rekordów na urządzenie`);
});
