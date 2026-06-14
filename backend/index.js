require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { Pool } = require('pg');

const app = express();
app.use(cors());
app.use(express.json());

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

pool.connect()
  .then(() => console.log('✅ Połączono z bazą PostgreSQL!'))
  .catch(err => console.error('❌ Błąd połączenia z bazą:', err.stack));

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
    // Retencja nie jest krytyczna — logujemy błąd, ale nie przerywamy działania
    console.error('⚠️ Błąd retencji:', err.message);
  }
}

// ==========================================
// ENDPOINT 1: POST /api/telemetry (Dla ESP8266)
// Odbiera JSON z ESP i zapisuje do bazy,
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
// Issue #22: dodano filtrowanie po device_id i limit
//
// Query params:
//   device_id  — opcjonalne, filtruje po urządzeniu
//   limit      — opcjonalne, domyślnie 50, max 500
//
// Przykłady:
//   GET /api/telemetry
//   GET /api/telemetry?device_id=WATIR_01
//   GET /api/telemetry?device_id=WATIR_01&limit=100
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
// ENDPOINT 3: GET /api/telemetry/stats (Nowy)
// Zwraca statystyki retencji — pomocne do debugowania
// i monitorowania rozmiaru bazy
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
// ENDPOINT 4: DELETE /api/telemetry (Nowy)
// Ręczne wymuszenie retencji lub pełne czyszczenie
//
// Query params:
//   device_id  — wymagane
//   mode       — 'retain' (zostaw ostatnie N) lub 'purge' (wyczyść wszystko)
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
// ENDPOINT 5: GET /api/plants (Profile roślin)
// ==========================================
app.get('/api/plants', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM plant_profiles ORDER BY id');
    res.status(200).json({ status: 'success', data: result.rows });
  } catch (error) {
    console.error(error);
    res.status(500).json({ status: 'error', message: 'Błąd odczytu profili.' });
  }
});

// Start serwera
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`🌿 Serwer WATIR API działa na http://localhost:${PORT}`);
  console.log(`📦 Limit retencji: ${RETENTION_LIMIT} rekordów na urządzenie`);
});
