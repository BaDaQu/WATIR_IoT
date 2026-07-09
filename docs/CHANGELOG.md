# 🌿 WATIR IoT — Pełny Changelog Firmware

> **Branch:** `feature/firmware-r4-wifi-migration`  
> **Wersja firmware:** 2.0  
> **Okres zmian:** 2026-06 — 2026-07-09

---

## Spis treści

1. [Podsumowanie](#podsumowanie)
2. [Faza 1 — Wersja manualna (bez WiFi)](#faza-1--wersja-manualna-bez-wifi)
3. [Faza 2 — Konfiguracja PuTTY (Serial)](#faza-2--konfiguracja-putty-serial)
4. [Faza 3 — Migracja na Arduino R4 WiFi](#faza-3--migracja-na-arduino-r4-wifi)
5. [Faza 4 — Naprawa BME280 (SPI)](#faza-4--naprawa-bme280-spi)
6. [Faza 5 — Sterownik pompy L298N](#faza-5--sterownik-pompy-l298n)
7. [Aktualny stan — mapa pinów i API](#aktualny-stan--mapa-pinów-i-api)
8. [Znane ograniczenia](#znane-ograniczenia)
9. [Wymagane biblioteki](#wymagane-biblioteki)

---

## Podsumowanie

Projekt przeszedł ewolucję od prostego systemu dwuukładowego (Arduino UNO R3 + ESP8266) sterowanego przez Serial, przez wersję czysto manualną (joystick), aż do obecnej jednoukładowej architektury na **Arduino UNO R4 WiFi** z wbudowanym WiFi, HTTP API, sterownikiem pompy L298N i regulacją mocy PWM.

```
[Ewolucja architektury]

v1.0  Arduino UNO R3 ←UART→ ESP8266 (Wemos D1 Mini)    ← dwa układy
v1.1  Arduino UNO R3 (tryb manualny, bez WiFi)           ← jeden układ, offline
v1.2  Arduino UNO R3 + Serial/PuTTY konfigurator         ← jeden układ, konfiguracja przez port szeregowy
v2.0  Arduino UNO R4 WiFi (WiFi + HTTP + wszystko)       ← jeden układ, WiFi wbudowane ✅
```

---

## Faza 1 — Wersja manualna (bez WiFi)

> Cel: Pozbycie się niestabilnej komunikacji WiFi/ESP i stworzenie w 100% działającego systemu offline sterowanego joystickiem.

### Usunięcie modułu ESP8266

- **`WATIR.ino`**: Usunięto wirtualny port `SoftwareSerial` i obiekt `portESP`. Zlikwidowano bloki parsujące komendy tekstowe z modułu bezprzewodowego oraz zmienną `trybAutomatyczny`.
- **`ServoControl.cpp` / `.h`**: Odchudzono `aktualizujSerwa()` z parametru `String komendaWiFi` — sterowanie ramienia wyłącznie przez fizyczny joystick.

### Uproszczenie Display Menu

- Usunięto interaktywną pętlę pytającą o tryb „AUTO / MANUAL" za pomocą joysticka.
- Zostawiono sam proces inicjalizacji LCD i napis powitalny „STARTUJEMY…".
- Usunięto `pokazMenu()`. Funkcja `pobierzKierunekJoysticka()` pozostała w kodzie (używana wewnętrznie przez `ServoControl`).

### Uproszczenie głównej pętli

- `loop()` sprowadzony do trzech zadań:
  1. Aktualizacja pozycji serwomechanizmów (joystick)
  2. Odczyt czujników co 1 sekundę (dystans, gleba, temperatura, wilgotność)
  3. Odświeżanie LCD

### Poprawki sensoryki

- **Filtr HC-SR04**: Serwomechanizmy powodowały spadki napięcia dające błędne odczyty 0 cm. Dodano bufor ignorujący nierealistyczne wartości i „pamiętający" ostatni prawidłowy dystans.
- **BME280 adres alternatywny**: Dodano fallback z adresu I2C `0x77` na `0x76` (tańsze klony modułu).
- **Aktualizacja pinów serw**: Przepięto drugi serwomechanizm z pinu `10` na pin `6`.
- **Poprawka LCD**: Naprawiono „duchy" na wyświetlaczu po zniknięciu komunikatu „!!! BRAK WODY !!!".

### Fail-Safe pompy

- Dodano jawne `digitalWrite(pinPrzekaznika, HIGH)` zapewniające wyłączenie pompy przy braku wody.
- Udostępniono `extern int pinPrzekaznika` w nagłówku `Watering.h` (rozwiązanie błędu kompilacji „not declared in this scope").
- Dodano `return` w pętli fail-safe, aby unikać wielokrotnego czyszczenia LCD.

### Diagnostyka Serial Monitor

- Dodano logowanie odczytów czujników (`Dystans`, `Gleba1`, `Gleba2`, `System blok`) na port szeregowy (9600 baud) do celów debugowania.

---

## Faza 2 — Konfiguracja PuTTY (Serial)

> Cel: Umożliwienie konfiguracji systemu bez WiFi — przez Serial/PuTTY.

### Konfigurator interaktywny (`Config.h` / `Config.cpp`)

- Skrypt uruchamiany w `setup()`, blokujący program do momentu zakończenia konfiguracji.
- Wysyłał pytania po Serial (9600 baud), wymagając podania:
  1. Progów wilgotności gleby dla obu roślin (%)
  2. Progu temperatury pokojowej (z kierunkiem: powyżej/poniżej)
  3. Progu wilgotności powietrza (z kierunkiem)
- Etap kalibracji na żywo — użytkownik najeżdżał ramieniem nad roślinę i klikał gałką joysticka. Kąty zapisywane w RAM.

### Odtworzenie logiki podlewania

- Przywrócono `logikaPodlewania()` opartą o zmienne z konfiguratora Serial.
- Dodano cooldown 60 sekund między cyklami podlewania (ochrona przed „powodzią").
- Odtworzono `ustawNadRoslina()` — rezygnacja z EEPROM na rzecz dynamicznej konfiguracji.

### Powiadomienia LCD przy podlewaniu

- Funkcja `podlej()` czyści LCD i wyświetla dwuliniowy komunikat „PODLEWANIE W TOKU…" przez czas pracy pompy (3 sekundy).

> **Uwaga:** Faza 2 została później zastąpiona przez pełną migrację na R4 WiFi (Faza 3). Konfigurator Serial został usunięty na rzecz HTTP API.

---

## Faza 3 — Migracja na Arduino R4 WiFi

> Cel: Połączenie wszystkiego w jeden układ — WiFi, czujniki, serwa, pompa, LCD.

### Zmiana architektury

```
PRZED (2 urządzenia):
[Apka] → [Backend] → [ESP8266] ←Serial 9600→ [Arduino UNO R3]

PO (1 urządzenie):
[Apka] → [Backend] → [Arduino UNO R4 WiFi]
                            ↑
              WiFi + HTTP + czujniki + serwa
              + pompa + LCD + joystick
```

Arduino R4 WiFi (Renesas RA4M1 + ESP32-S3) przejmuje rolę obu układów. Komunikacja Serial między płytkami wyeliminowana.

### Nowy plik główny: `WATIR-ARDUINO.ino`

- Zastąpił stary `WATIR.ino` (131 → 860+ linii)
- WiFi przez `WiFiS3.h` (biblioteka dedykowana R4 WiFi)
- Serial z 9600 na 115200 baud (natywny USB R4)
- Wbudowany HTTP Server na porcie 80 (`WiFiServer`)
- Ręczne parsowanie HTTP (brak AsyncWebServer na R4)
- Automatyczne przełączanie trybu AUTO/MANUAL na podstawie statusu WiFi (bez menu)
- Timeout łączenia WiFi: 60 sekund (120 prób × 500ms)
- Oczekiwanie na przydzielenie IP przez DHCP

### Panel diagnostyczny na matrycy LED

- Animacja rośliny (8 klatek, 8×12 pikseli) gdy wszystko OK
- Podział na 4 sektory przy błędach:
  - Górny-lewy: WiFi
  - Górny-prawy: BME280
  - Dolny-lewy: Brak wody
  - Dolny-prawy: Tryb ręczny

### Zmiany w ServoControl

- `aktualizujSerwa()` — usunięty parametr `komendaWiFi`, zastąpiony zmienną globalną `komendaWiFiCiagla`
- Nowa funkcja `ustawPozycjeSerwaWiFi(int x, int y)` — absolutne ustawianie pozycji z HTTP API
- Tryb ciągły (joystick w aplikacji): `{"direction":"lewo"}` → firmware sam porusza serwem w `loop()`

### Profil rośliny (`/api/config`)

Zmienne konfiguracji aktualizowane dynamicznie przez HTTP:
- `profilProgWilgotnosci` — próg wilgotności (%)
- `profilAutoWatering` — tryb auto (bool)
- `profilCheckIntervalMs` — interwał sprawdzania (ms)
- `profilPan` / `profilTilt` — pozycja serw (0-180°)

### Telemetria

- Cykliczna co 30 sekund (`POST /api/telemetry` do backendu Node.js)
- JSON kompatybilny z formatem starego ESP8266
- Serwa usypiane na czas transmisji (oszczędność prądu)

---

## Faza 4 — Naprawa BME280 (SPI)

> Problem: BME280 na I2C (A4/A5) wyświetlał `NaN` po połączeniu z WiFi. W trybie manualnym działał.

### Przyczyna

Biblioteka `WiFiS3` i koprocesor ESP32-S3 na R4 WiFi rekonfigurują piny/magistrale podczas `WiFi.begin()`. BME280 inicjalizowany **przed** WiFi tracił konfigurację SPI.

### Rozwiązanie 1: Zmiana kolejności inicjalizacji

```diff
  konfigurujPodlewanie();
  konfigurujWyswietlacz();
  konfigurujSerwa();
- konfigurujCzujniki();     // ← było PRZED WiFi

  WiFi.begin(ssid, password);
  // ... łączenie ...

+ konfigurujCzujniki();     // ← teraz PO WiFi
```

### Rozwiązanie 2: Przejście z I2C na SPI

BME280 na I2C (A4/A5) po prostu nie był wykrywany na R4 WiFi — nawet po zmianie kolejności. Przeniesiono na SPI:

```cpp
// Było (I2C):
Adafruit_BME280 bme;
bme.begin(0x77);

// Jest (SPI):
#define BME_CS 10
Adafruit_BME280 bme(BME_CS);  // Hardware SPI: SCK=13, MISO=12, MOSI=11, CS=10
bme.begin();
```

Dodano zmienną `bool bmeOK` — status inicjalizacji dla panelu diagnostycznego na matrycy LED.

---

## Faza 5 — Sterownik pompy L298N

> Cel: Wymiana prostego przekaźnika na sterownik L298N z regulacją mocy PWM.

### Zmiana sprzętowa

| Parametr | Było | Jest |
|----------|------|------|
| Sterownik | Przekaźnik (pin 12) | L298N motor driver |
| Sterowanie | `digitalWrite(HIGH/LOW)` | PWM (ENA) + kierunek (IN1/IN2) |
| Regulacja mocy | Brak (ON/OFF) | 0–100% przez PWM |
| Zasilanie pompy | 5V przez przekaźnik | 12V przez L298N |

### Podłączenie L298N

```
Arduino          L298N                Pompa 12V
───────          ─────                ─────────
Pin 3  ────────► ENA (zdjąć jumper!)
Pin 5  ────────► IN1
Pin 4  ────────► IN2
                 OUT1 ───────────────► Pompa (+)
                 OUT2 ───────────────► Pompa (-)
                 12V  ◄──────────────  Zasilacz 12V/3A
                 GND  ◄──────────────  GND (wspólna masa)
```

> **⚠️ Ważne:** Zdjąć zworkę (jumper) z ENA na module L298N!  
> **⚠️ Ważne:** Piny A4/A5 są zajęte przez I2C (LCD) — nie podłączać!

### Nowe funkcje w `Watering.cpp`

```cpp
void ustawPompe(bool wlacz);          // Bezpośrednie ON/OFF
void ustawMocPompy(int procent);      // Regulacja 0–100%
int  pobierzMocPompy();               // Odczyt aktualnej mocy
static int procentNaPWM(int procent); // Konwersja na 0–255
```

- **ON**: IN1=HIGH, IN2=LOW, ENA=PWM → prąd płynie OUT1(+) → OUT2(-)
- **OFF**: IN1=LOW, IN2=LOW, ENA=0 → silnik zatrzymany (coast)
- Domyślna moc: **70%** (bezpieczne dla zasilacza 3A dzielonego z przetwornicą 5V)

### Budżet mocy

```
Zasilacz 12V / 3A
  ├──► L298N (pompa, max ~2A przy 100%)
  └──► Przetwornica 5V → Arduino + LCD + Serwa + Czujniki (~0.5–1A)

Przy 100%: ~2A + ~1A = ~3A → NA GRANICY zasilacza (ryzyko brownout!)
Przy  70%: ~1.4A + ~1A = ~2.4A → OK ✅
```

### Nowy endpoint: `POST /api/pump`

```json
// Sprawdź status:
{"action": "status"}
→ {"status":"success", "pump_power":70, "pump_active":false}

// Ustaw moc:
{"power": 60}

// Podlej z parametrami:
{"power": 50, "duration": 5000}

// Tylko czas (aktualna moc):
{"duration": 2000}
```

Zabezpieczenia:
- `duration`: 500ms – 30000ms
- `power`: 0 – 100%
- Sprawdzany poziom wody przed podlewaniem

### Aktualizacja `/api/config`

Dodany nowy parametr `pump_power`:
```json
{
  "config": {
    "moisture_threshold": 35,
    "auto_watering": true,
    "check_interval_ms": 10000,
    "pump_power": 70,
    "pan": 90,
    "tilt": 90
  }
}
```

### Aktualizacja telemetrii

Nowe pole `pump_power` w obiekcie `status`:
```json
{
  "status": {
    "water_error": false,
    "pump_active": false,
    "pump_power": 70
  }
}
```

### Obserwacja: buzzing przy niskim PWM

Przy ~30% mocy silnik pompy nie miał siły się rozkręcić — emitował dźwięk buzowania (PWM ~490Hz w zakresie słyszalnym). Minimum praktyczne: **~40–50%**.

---

## Aktualny stan — mapa pinów i API

### Mapa pinów Arduino UNO R4 WiFi

| Pin | Funkcja | Moduł |
|-----|---------|-------|
| 2 | Przycisk joysticka | INPUT_PULLUP |
| **3** | **L298N ENA (PWM)** | **Regulacja mocy pompy** |
| **4** | **L298N IN2** | **Kierunek pompy (-)** |
| **5** | **L298N IN1** | **Kierunek pompy (+)** |
| **6** | **Serwo Y (tilt)** | **Ramię robota** |
| 7 | HC-SR04 Echo | Czujnik poziomu wody |
| 8 | HC-SR04 Trig | Czujnik poziomu wody |
| **9** | **Serwo X (pan)** | **Ramię robota** |
| 10 | BME280 CS | SPI Chip Select |
| 11 | SPI MOSI | BME280 |
| 12 | SPI MISO | BME280 |
| 13 | SPI SCK | BME280 |
| A0 | Czujnik gleby 2 | Analog |
| A1 | Czujnik gleby 1 | Analog |
| A2 | Joystick Y | Analog |
| A3 | Joystick X | Analog |
| A4 (SDA) | LCD I2C | Wire (**zajęte!**) |
| A5 (SCL) | LCD I2C | Wire (**zajęte!**) |

### Endpointy HTTP (port 80)

| Metoda | Endpoint | Opis |
|--------|----------|------|
| POST | `/api/move` | Sterowanie serwami (absolutne + ciągłe) |
| POST | `/api/config` | Konfiguracja profilu rośliny + moc pompy |
| POST | `/api/water` | Szybkie podlewanie (3s, aktualna moc) |
| POST | `/api/pump` | Pełne sterowanie pompą (moc, czas, status) |

### Sterowanie serwami — porównanie

| Cecha | Joystick fizyczny | Aplikacja mobilna |
|-------|------------------|-------------------|
| Sposób | `analogRead()` → dead zone → krok ±5° | Drag → POST `/api/move` |
| Interwał | Co 25ms | Co 50ms |
| Wartość | Inkrementalna (±5° od aktualnej) | Absolutna (0-180°) lub ciągła (direction) |
| Dead zone | 400–600 (analog 0–1023) | 0.2f (procent wychylenia) |

---

## Znane ograniczenia

1. **Zasilacz 3A** — przy pełnej mocy pompy (100% ≈ 2A) + reszta układu możliwy brownout. Domyślna moc: 70%.
2. **BME280 na I2C nie działa na R4 WiFi** — biblioteka WiFi rekonfiguruje magistralę. Rozwiązanie: SPI.
3. **Piny SDA/SCL zajęte** przez I2C (LCD) — nie podłączać nic innego.
4. **Pompa buczy przy niskim PWM** (~30%) — silnik nie ma siły się rozkręcić. Minimum: ~40–50%.
5. **Brak uwierzytelniania HTTP** — endpointy dostępne dla każdego w sieci lokalnej.

---

## Wymagane biblioteki Arduino IDE

| Biblioteka | Źródło | Opis |
|-----------|--------|------|
| `WiFiS3` | Board Package R4 | WiFi dla Arduino UNO R4 WiFi |
| `Servo` | Board Package R4 | Sterowanie serwami PWM |
| `ArduinoJson` | Library Manager | Parsowanie/budowanie JSON (v6/v7) |
| `LiquidCrystal I2C` | Library Manager | Wyświetlacz LCD 16×2 I2C |
| `Adafruit BME280` | Library Manager | Czujnik temperatury/wilgotności (SPI) |
| `Adafruit Unified Sensor` | Library Manager | Zależność BME280 |
| `EEPROM` | Wbudowana | Zapis pozycji serw |
| `Wire` | Wbudowana | Komunikacja I2C (LCD) |
| `SPI` | Wbudowana | Komunikacja SPI (BME280) |
| `Arduino_LED_Matrix` | Board Package R4 | Matryca LED na R4 WiFi |

---

## Konfiguracja do zmiany ręcznie

```cpp
// WATIR-ARDUINO.ino — linie na górze pliku:
const char* ssid           = "WATIR";          // Nazwa sieci WiFi
const char* password       = "WATIRINO";       // Hasło WiFi
const char* backendHost    = "10.101.29.4";    // IP serwera Node.js
const int   backendPort    = 3000;             // Port backendu
```

---

## Statystyki zmian (vs. main)

```
 16 plików zmienionych
 1240 wstawień (+)
  296 usunięć (-)
```

---

## Załącznik: Historyczne notatki (Archiwum)

Poniższe notatki pochodzą z początkowej fazy rozwiązywania problemów przy migracji na Arduino UNO R4 WiFi. Zostały zachowane jako dziennik techniczny, jednak **w przypadku rozbieżności z powyższą dokumentacją, nadrzędny jest kod źródłowy (aktualny stan)**.

### 1. Rozwiązanie błędu "Multiple Definition" (C++)

**Problem:** Podczas dzielenia kodu na zakładki (`.ino` oraz `.cpp`), kompilator zgłaszał błąd `multiple definition of 'bme'`, uniemożliwiając wgranie kodu.
**Rozwiązanie:** Użycie słowa kluczowego `extern`. Obiekt czujnika jest tworzony fizycznie tylko raz w pliku `Sensors.cpp`, a główny plik `WATIR-ARDUINO.ino` jedynie "wskazuje" na niego.

### 2. Walka z I2C i "Duchami" (BME280 Waveshare)

**Problem:** Skaner I2C na nowym Arduino R4 pokazywał dziesiątki podłączonych urządzeń (tzw. "duchy" pod adresami 0x76-0x7E) lub nie widział czujnika wcale, podczas gdy na starym Arduino R3 wszystko działało idealnie.
**Diagnoza:**
1. **Rygorystyczny procesor R4:** Nowy mikrokontroler Renesas RA4M1 ma bardzo czuły sprzętowy kontroler I2C, który odrzuca niedoskonałe sygnały (brak mocnych rezystorów podciągających).
2. **Wiszący pin CS:** Moduł Waveshare posiada pin CS. Na R3 niepodłączony pin łapał zakłócenia i "jakoś" działał w I2C. Na R4 pin zjeżdżał do zera, co wyłączało komunikację I2C i blokowało czujnik.

> **Ważna uwaga (Złącze ICSP):** Sześć pinów na środku płytki R4 (ICSP) to elektrycznie te same piny co cyfrowe 11, 12, 13. Wpięcie się w nie nie oszczędza pinów cyfrowych!

### 3. Zewnętrzny przycisk Reset

Dodano przycisk "twardego" resetu wyprowadzony na obudowę.
**Podłączenie:** Zwykły przycisk (tact-switch) podłączony jednym kablem do pinu `RESET` na Arduino, a drugim do pinu `GND`. Po kliknięciu obwód zwiera się do masy, natychmiastowo restartując robota. (Wbudowany rezystor podciągający dba o stan wysoki).

### 4. Zasilanie L298N z perspektywy akumulatora

Pojawiła się obawa, czy akumulator o wydajności 3A nie spali modułu L298N (limit 2A).
**Wniosek:** Akumulator "oddaje" prąd, a nie go "wpycha". To pompa decyduje, ile prądu pobierze. Zasilanie 3A jest ogromną zaletą, ponieważ zapobiega spadkom napięcia (brownoutom).
Krytyczne przy zasilaniu było połączenie **Wspólnej Masy (GND)** pomiędzy akumulatorem, Arduino i sterownikiem.

### 5. Odrzucone pomysły (Zgodność ze stanem faktycznym)

W trakcie burzy mózgów rozważano kilka rozwiązań, które **ostatecznie nie znalazły się w kodzie produkcyjnym**:
1. **Kick-Start Pompy:** Pomysł podawania PWM=200 przez 150ms na start. Odrzucono na rzecz bezpiecznego mapowania całego zakresu w funkcji `procentNaPWM()`.
2. **Opoźnienie serwa przed pompą (`delay(500)`):** Rozważano blokowanie kodu na czas ruchu serwa. Ostatecznie główna pętla jest w pełni asynchroniczna, a jedyne blokady (`delay(1000)`) występują już po zakończeniu wylewania wody.
3. **Piny A0/A1 jako wyjścia:** Rozważano przypięcie pompy lub serw do A0/A1. Ostatecznie A0/A1 zostały przeznaczone na czujniki wilgotności gleby (G2 i G1).
