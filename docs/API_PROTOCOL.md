# Protokół Wymiany Danych WATIR (JSON API Contract) – V0.1

Niniejszy dokument definiuje sztywny format wymiany danych pomiędzy modułem ESP8266 (Wemos D1 Mini), aplikacją mobilną oraz serwerem w projekcie WATIR. Standard ten zapewnia spójność komunikacji i pozwala na równoległy rozwój oprogramowania układowego (firmware) oraz aplikacji.

---

## 1. Telemetria (Stan Systemu)

* **Kierunek:** Urządzenie (ESP8266) -> Chmura/Aplikacja
* **Opis:** Pakiet danych wysyłany okresowo (np. co 5 sekund), zawierający aktualne odczyty ze wszystkich sensorów.

**Przykład Payloadu (JSON)**:
```json
{
  "device_id": "WATIR_01",
  "timestamp": 1713341614,
  "sensors": {
    "temp": 24.5,
    "humidity": 46,
    "soil_moisture": 40,
    "water_level_cm": 9
  },
  "status": {
    "water_error": false,
    "pump_active": false
  },
  "servos": {
    "pan": 90,
    "tilt": 45
  }
}
```

**Słownik pól telemetrii**:
* **timestamp**: Czas wygenerowania pakietu w standardzie UNIX epoch (liczba sekund). Wymagany do zapisu historii zdarzeń w bazie danych.
* **temp**: Temperatura otoczenia z czujnika BME280 (°C).
* **humidity**: Wilgotność powietrza z czujnika BME280 (%).
* **soil_moisture**: Wilgotność gleby przeliczona na procenty (0-100%) na podstawie odczytu analogowego A0.
* **water_level_cm**: Dystans do lustra wody zmierzony przez HC-SR04.
* **water_error**: Flaga błędu - true, jeśli poziom wody w zbiorniku jest krytycznie niski (> 12 cm).
* **pump_active**: Stan przekaźnika sterującego pompą.
* **pan / tilt**: Aktualna pozycja serwomechanizmów w stopniach (0-180).

---

## 2. Sterowanie i Akcje

* **Kierunek:** Aplikacja/Serwer -> Urządzenie (ESP8266)
* **Opis:** Żądania wykonania konkretnych czynności przez system, np. ręczne podlanie lub ruch ramieniem.

**A. Komenda ruchu serwomechanizmów**
```json
{
  "action": "move_servo",
  "params": {
    "pan": 120,
    "tilt": 30
  }
}
```

**B. Komenda wymuszenia podlewania**
```json
{
  "action": "force_watering",
  "params": {
    "duration_ms": 3000
  }
}
```

---

## 3. Konfiguracja i Profile Roślin

* **Kierunek:** Aplikacja -> Urządzenie (ESP8266)
* **Opis:** Aktualizacja parametrów operacyjnych systemu na podstawie wybranego profilu rośliny lub decyzji AI.

**Przykład zapytania (Update Config)**:
```json
{
  "config": {
    "moisture_threshold": 200,
    "auto_watering": true,
    "check_interval_ms": 10000
  }
}
```

**Słownik pól konfiguracji**:
* **moisture_threshold**: Wartość graniczna z sensora gleby, poniżej której system uruchamia nawadnianie.
* **auto_watering**: Tryb pracy – automatyczny (true) lub tylko manualny (false).

---

## 4. Tabela Typów Danych i Zakresów

| Pole | Typ | Zakres / Jednostka | Opis |
| :--- | :--- | :--- | :--- |
| timestamp | int | UNIX epoch (sekundy) | Czas zdarzenia / odczytu |
| temp | float | -40.0 do 85.0 °C | Dokładność ±1.0 °C |
| soil_moisture | int | 0 do 100 % | Mapowane z 0-1023 (ADC) |
| water_level_cm | int | 2 do 400 cm | Czujnik HC-SR04 |
| pan / tilt | int | 0 do 180 ° | Pozycja serwa |
| water_error | bool | true / false | Zabezpieczenie przed pracą na sucho |

---

## 5. Przykładowy Cykl Payload/Response (Synchronizacja)

**Aplikacja wysyła zapytanie o status**:
`GET /api/status`

**Urządzenie odpowiada (Response) - Zwraca PEŁNY JSON telemetrii**:
```json
{
  "status": "success",
  "data": {
    "device_id": "WATIR_01",
    "timestamp": 1713341614,
    "sensors": {
      "temp": 24.5,
      "humidity": 46,
      "soil_moisture": 40,
      "water_level_cm": 9
    },
    "status": {
      "water_error": false,
      "pump_active": false
    },
    "servos": {
      "pan": 90,
      "tilt": 45
    }
  }
}
```

**Aplikacja wysyła komendę podlewania**:
`POST /api/action` z body `{"action": "force_watering"}`

**Urządzenie potwierdza**:
```json
{
  "status": "accepted",
  "message": "Watering started"
}
```