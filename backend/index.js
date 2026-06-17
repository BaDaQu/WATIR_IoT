require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { Pool } = require('pg');

const app = express();
app.use(cors());
app.use(express.json());

// Słownik przechowujący ostatnio znane IP urządzeń w sieci lokalnej (Auto-discovery)
const activeDevices = {};

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
      (device_id, timestamp, temp, humidity, soil_moisture, water_level_cm, water_error, pump_active, pan, tilt)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
      RETURNING *;
    `;

    const values = [
      data.device_id,
      data.timestamp || Math.floor(Date.now() / 1000),
      data.sensors.temp,
      data.sensors.humidity,
      data.sensors.soil_moisture,
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
    const { device_id = 'WATIR_01', axis, value } = req.body;

    if (!axis || value === undefined) {
      return res.status(400).json({
        status: 'error',
        message: 'Brakujące parametry: wymagane axis ("X" lub "Y") oraz value'
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

    const response = await fetch(`http://${espIp}/api/move`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ axis, value }),
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
      `INSERT INTO plant_profiles (name, moisture_threshold, auto_watering, check_interval_ms, pan, tilt)
       VALUES ($1, $2, $3, $4, $5, $6) RETURNING *`,
      [
        data.name,
        data.moisture_threshold,
        data.auto_watering ?? true,
        data.check_interval_ms ?? 10000,
        data.pan ?? 90,
        data.tilt ?? 90
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
           tilt = $6
       WHERE id = $7 RETURNING *`,
      [
        data.name,
        data.moisture_threshold,
        data.auto_watering ?? true,
        data.check_interval_ms ?? 10000,
        data.pan ?? 90,
        data.tilt ?? 90,
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

    const allowed = ['name', 'moisture_threshold', 'auto_watering', 'check_interval_ms', 'pan', 'tilt'];
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
    res.status(200).json({ status: 'success', message: `Profil "${result.rows[0].name}" został usunięty.`, deleted: result.rows[0] });
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

    // Dodajemy pozycje serw (pan/tilt) do konfiguracji wysyłanej do ESP
    const espConfig = {
      config: {
        moisture_threshold: profile.moisture_threshold,
        auto_watering: profile.auto_watering,
        check_interval_ms: profile.check_interval_ms,
        pan: profile.pan,
        tilt: profile.tilt
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
