require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { Pool } = require('pg');

const app = express();
app.use(cors());
app.use(express.json()); // Do parsowania JSON z body

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
// ENDPOINT 1: POST /api/telemetry (Dla ESP8266)
// Odbiera JSON z ESP i zapisuje do bazy
// ==========================================
app.post('/api/telemetry', async (req, res) => {
  try {
    const data = req.body;
    
    const query = `
      INSERT INTO telemetry_logs 
      (device_id, timestamp, temp, humidity, soil_moisture, water_level_cm, water_error, pump_active, pan, tilt) 
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10) RETURNING *;
    `;
    
    const values =[
      data.device_id,
      data.timestamp || Math.floor(Date.now() / 1000), // Domyślnie aktualny czas UNIX
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
    res.status(201).json({ status: "success", message: "Log zapisany poprawnie!", saved: result.rows[0] });
    
  } catch (error) {
    console.error(error);
    res.status(500).json({ status: "error", message: "Błąd zapisu do bazy danych." });
  }
});

// ==========================================
// ENDPOINT 2: GET /api/telemetry (Dla Aplikacji)
// Zwraca historię logów (np. ostatnie 50 wpisów)
// ==========================================
app.get('/api/telemetry', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM telemetry_logs ORDER BY timestamp DESC LIMIT 50');
    res.status(200).json({ status: "success", data: result.rows });
  } catch (error) {
    console.error(error);
    res.status(500).json({ status: "error", message: "Błąd odczytu z bazy danych." });
  }
});

// Start serwera
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Serwer API działa na porcie http://localhost:${PORT}`);
});
