# 🌿 WATIR IoT (Watering Automation Technology for Indoor Plants)

**WATIR IoT** to zaawansowany ekosystem IoT przeznaczony do automatycznego monitorowania kondycji oraz precyzyjnego nawadniania roślin domowych. System opiera się na mikrokontrolerze **Arduino UNO R4 WiFi**, serwerze pośredniczącym w chmurze lokalnej (Node.js + PostgreSQL) oraz dedykowanej aplikacji mobilnej na system Android.

Projekt eliminuje potrzebę pamiętania o podlewaniu, oferując automatyzację opartą na profilach roślin, detekcji wilgotności gleby, zaawansowanym pozycjonowaniu wylewki wody za pomocą dwuosiowego ramienia (serwa Pan/Tilt) oraz kontroli przepływu wody (PWM).

---

## 🚀 Główne Funkcje Ekosystemu

*   **Autonomiczne Nawadnianie (Tryb AUTO)**: Cykliczny odczyt czujników i automatyczne podlewanie roślin po przekroczeniu indywidualnych progów wilgotności gleby.
*   **Dwuosiowe Ramię Pozycjonujące**: Dwa serwomechanizmy (oś X – Pan, oś Y – Tilt) kierują strumień wody precyzyjnie nad wybraną roślinę przed uruchomieniem pompy.
*   **Sterowanie Pompą przez L298N (PWM)**: Regulacja mocy pompy (0-100%) za pomocą sygnałów PWM (domyślnie 70% w celu ochrony przed brownoutami), zapobiegająca gwałtownemu zalewaniu roślin.
*   **Zabezpieczenie przed suchobiegiem (Fail-Safe)**: Czujnik HC-SR04 mierzy poziom wody w zbiorniku – jeśli odległość przekroczy 12 cm, pompa zostaje zablokowana, a system zgłasza błąd.
*   **Interfejs Lokalny (LCD + Matryca LED)**: Wyświetlacz LCD 16x2 pokazuje aktualne parametry. Wbudowana w R4 matryca LED 12x8 służy jako ekran diagnostyczny (animacja rośliny lub wskaźnik błędów systemowych).
*   **Dedykowana Aplikacja Android**: Nowoczesny interfejs w Jetpack Compose umożliwiający:
    *   Podgląd danych w czasie rzeczywistym i wykresów historycznych.
    *   Sterowanie ramieniem za pomocą wirtualnego joysticka.
    *   Ręczne podlewanie o określonej mocy i czasie trwania.
    *   Zarządzanie profilami roślin (CRUD) i aplikowanie ich na urządzenie.
*   **Inteligentna Retencja Danych**: Backend automatycznie utrzymuje bazę danych w ryzach, przechowując maksymalnie 1000 ostatnich logów na urządzenie.

---

## 📂 Struktura Projektu

```text
WATIR_IoT/
├── app/                  # Aplikacja mobilna (Android Studio, Kotlin, Jetpack Compose, Retrofit)
├── backend/              # Serwer Node.js (Express, PostgreSQL w Docker Compose)
├── firmware/             # Kod mikrokontrolera (Arduino UNO R4 WiFi, C++/.ino)
│   └── WATIR-ARDUINO/    # Moduł jednoukładowy R4 (obsługuje czujniki, serwa, pompę i serwer HTTP)
├── docs/                 # Dokumentacja API (API_PROTOCOL.md) oraz historia zmian (CHANGELOG.md)
└── tools/                # Narzędzia dodatkowe (m.in. kalibrator offline w Python/Tkinter)
```

---

## 🔌 Specyfikacja Sprzętowa i Połączenia

System działa na napięciu **12V** (zasilanie pompy przez L298N) oraz **5V** (zasilanie Arduino i serwomechanizmów z przetwornicy step-down).

### Mapa Pinów Arduino UNO R4 WiFi

| Pin | Funkcja | Moduł | Uwagi |
| :--- | :--- | :--- | :--- |
| **D2** | Przycisk Joysticka | Wejście | INPUT_PULLUP (Wyjście z menu / manualny start) |
| **D3** | L298N ENA (PWM) | Wyjście | Regulacja mocy pompy (Sygnał PWM) |
| **D4** | L298N IN2 | Wyjście | Kierunek pompy (-) |
| **D5** | L298N IN1 | Wyjście | Kierunek pompy (+) |
| **D6** | Serwo Y (Tilt) | Wyjście | Ruch pionowy ramienia wylewki |
| **D7** | HC-SR04 Echo | Wejście | Czujnik poziomu wody w zbiorniku |
| **D8** | HC-SR04 Trig | Wyjście | Czujnik poziomu wody w zbiorniku |
| **D9** | Serwo X (Pan) | Wyjście | Ruch poziomy ramienia wylewki |
| **D10** | BME280 CS | Wyjście | SPI Chip Select (Czujnik temperatury i wilgotności) |
| **D11** | SPI MOSI | Wyjście | BME280 SPI |
| **D12** | SPI MISO | Wejście | BME280 SPI |
| **D13** | SPI SCK | Wyjście | BME280 SPI |
| **A0** | Sensor Gleby 2 | Wejście | Analogowy odczyt wilgotności gleby rośliny 2 |
| **A1** | Sensor Gleby 1 | Wejście | Analogowy odczyt wilgotności gleby rośliny 1 |
| **A2** | Joystick Y | Wejście | Sterowanie manualne pionowe |
| **A3** | Joystick X | Wejście | Sterowanie manualne poziome |
| **SDA/SCL** | LCD 16x2 | I2C | Wyświetlacz I2C (Adres domyślny 0x27) |

---

## ⚙️ Instrukcja Uruchomienia Środowiska

### 1. Baza Danych i Backend (Docker & Node.js)

Backend działa jako kontener PostgreSQL oraz serwer Express.js pośredniczący w komunikacji.

1.  Zainstaluj **Docker Desktop** i uruchom obsługę WSL2.
2.  Przejdź do folderu backendu:
    ```bash
    cd backend
    ```
3.  Utwórz plik `.env` w folderze `backend/` i uzupełnij konfigurację połączenia bazy danych:
    ```env
    DB_HOST=127.0.0.1
    DB_PORT=5432
    DB_USER=watir_user
    DB_PASSWORD=watir_password
    DB_NAME=watir_db
    ```
4.  Uruchom bazę danych w tle przy użyciu Docker Compose:
    ```bash
    docker-compose up -d
    ```
    *Baza PostgreSQL zainicjalizuje się automatycznie na podstawie pliku `init.sql` (tworząc tabele i indeksy).*
5.  Zainstaluj zależności serwera Node.js i go uruchom:
    ```bash
    npm install
    npm start
    ```
    Serwer uruchomi się na porcie `3000`.

### Connection Strings:
*   **JDBC (DBeaver / Java)**: `jdbc:postgresql://localhost:5432/watir_db?user=watir_user&password=watir_password`
*   **URL (Node.js/Prisma)**: `postgresql://watir_user:watir_password@localhost:5432/watir_db`

> ⚠️ **Wskazówka dla WSL2 i urządzeń fizycznych**: Jeśli telefon i komputer deweloperski znajdują się w tej samej sieci WiFi, w konfiguracji firmware i aplikacji mobilnej podaj lokalny adres IP komputera (np. `192.168.1.15`), a nie `localhost`.

---

### 2. Oprogramowanie Układowe (Firmware Arduino)

Kod znajduje się w folderze `firmware/WATIR-ARDUINO/`.

1.  Otwórz plik `WATIR-ARDUINO.ino` w **Arduino IDE** (zalecana wersja 2.0+).
2.  W menedżerze płytek zainstaluj pakiet wsparcia dla **Arduino UNO R4 Boards**.
3.  Zainstaluj wymagane biblioteki (dostępne w Menedżerze Bibliotek):
    *   `WiFiS3` (wbudowana w pakiet płytek)
    *   `ArduinoJson` (w wersji 6.x lub 7.x)
    *   `LiquidCrystal I2C`
    *   `Adafruit BME280` (+ `Adafruit Unified Sensor`)
    *   `SPI` i `Wire` (wbudowane)
4.  Skonfiguruj dane sieci WiFi i adres backendu na początku pliku `WATIR-ARDUINO.ino`:
    ```cpp
    const char* ssid         = "NAZWA_TWOJEJ_SIECI";
    const char* password     = "HASLO_SIECI";
    const char* backendHost  = "192.168.1.15"; // Adres IP komputera z backendem
    const int   backendPort  = 3000;
    ```
5.  Podłącz Arduino UNO R4 WiFi kablem USB-C, wybierz odpowiedni port i wgraj program.

---

### 3. Aplikacja Mobilna (Android)

Projekt aplikacji znajduje się w folderze `app/`.

1.  Otwórz folder `app/` jako istniejący projekt w programie **Android Studio**.
2.  Upewnij się, że masz zainstalowany Android SDK dla kompatybilnej wersji systemu.
3.  Podczas uruchamiania aplikacji po raz pierwszy zostaniesz poproszony o wpisanie adresu IP serwera backendu (np. `192.168.1.15`).
4.  Uruchom aplikację na emulatorze lub fizycznym telefonie podłączonym do tej samej sieci WiFi.

---

## 🛠️ Kalibracja Offline (Python GUI)

W folderze `tools/offline_config` znajduje się narzędzie napisane w Pythonie (Tkinter), pozwalające na pełną konfigurację progów podlewania oraz kalibrację kątów serwomechanizmów bezpośrednio przez kabel USB (Serial port 115200 baud) bez konieczności połączenia z WiFi.

Aby je uruchomić:
```bash
cd tools/offline_config
pip install -r requirements.txt
python app.py
```

---

## ⚠️ Znane Ograniczenia i Bezpieczeństwo

1.  **Maksymalny Pobór Prądu**: Pompa pracująca na 100% mocy może pobierać do 2A. W połączeniu z serwami i elektroniką może to przekroczyć wydajność zasilacza 3A i wywołać restart mikrokontrolera (brownout). Domyślny limit w kodzie to **70%**.
2.  **Komunikacja I2C vs WiFi na R4**: Z powodu specyfiki koprocesora ESP32-S3 na płytce R4 WiFi, czujnik BME280 musi być podłączony przez interfejs SPI (piny 10-13) zamiast I2C (A4/A5), aby zapobiec zawieszaniu odczytów po zainicjowaniu sieci WiFi.
3.  **Minimalna moc PWM**: Przy wartościach PWM poniżej 40% pompa wydaje jedynie pisk (buzzing) i nie obraca wirnika. Praktyczny zakres sterowania mocą to **40% – 100%**.
4.  **Brak autoryzacji**: Wersja deweloperska nie posiada mechanizmów autoryzacji zapytań HTTP na porcie 80 urządzenia. Każdy użytkownik w tej samej sieci może wysyłać komendy sterowania serwami i pompą.
