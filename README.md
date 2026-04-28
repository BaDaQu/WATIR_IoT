# 🌿 WATIR IoT (Watering Automation Technology for Indoor Plants)

Projekt WATIR IoT to kompleksowy system sprzętowo-programowy do autonomicznego nawadniania roślin, łączący mikrokontroler ESP8266 (Wemos D1 Mini), chmurową bazę danych oraz dedykowaną aplikację mobilną. Naszym celem jest zbudowanie kompletnego ekosystemu, który zdejmuje z użytkownika obowiązek pamiętania o podlewaniu, oferując automatyzację na podstawie profili gatunkowych, szczegółową analitykę danych oraz hybrydowe sterowanie.

---

# 🛠️ Instrukcja uruchomienia środowiska WATIR_IoT

Ten dokument opisuje krok po kroku, jak przygotować i uruchomić lokalną bazę danych oraz infrastrukturę backendową przy użyciu **WSL2** oraz **Docker Desktop**.

## 1. Wymagania wstępne
Przed rozpoczęciem upewnij się, że masz zainstalowane:
*   **Docker Desktop** (z włączoną opcją "WSL Integration" w ustawieniach).
*   **WSL2** (rekomendowana dystrybucja: Ubuntu).
*   **DBeaver** lub **Postman** (do testowania połączenia).
*   **Android Studio** (do rozwoju aplikacji mobilnej).

## 2. Pierwsze uruchomienie (Klonowanie)
Jeśli jeszcze tego nie zrobiłeś, sklonuj repozytorium:
```bash
git clone https://github.com/TwojLogin/WATIR_IoT.git
cd WATIR_IoT
```

## 3. Konfiguracja bazy danych (PostgreSQL)
Baza danych działa wewnątrz kontenera Docker. Aby ją uruchomić:

1.  Przejdź do folderu backendu:
    ```bash
    cd backend
    ```
2.  **Stwórz plik `.env`**. Ponieważ prawdziwy plik `.env` jest ignorowany przez Git, musisz go utworzyć ręcznie w folderze `backend/`. Wklej do niego dokładnie poniższą treść:

    **Plik: `backend/.env`**
    ```env
    DB_HOST=127.0.0.1
    DB_PORT=5432
    DB_USER=watir_user
    DB_PASSWORD=watir_password
    DB_NAME=watir_db
    ```

3.  Uruchom kontener za pomocą Docker Compose:
    ```bash
    docker-compose up -d
    ```

## 4. Dane połączenia i Connection Strings
Poniżej znajdują się gotowe formaty danych do konfiguracji narzędzi (DBeaver) lub kodu źródłowego (Backend / App).

### Podstawowe parametry (zgodne z .env):
*   **Host:** `localhost` lub `127.0.0.1`
*   **Port:** `5432`
*   **Database:** `watir_db`
*   **User:** `watir_user`
*   **Password:** `watir_password`

### Uniwersalny format URL (np. dla Node.js, Python, Prisma):
```text
postgresql://watir_user:watir_password@localhost:5432/watir_db
```

### Format JDBC (np. dla Java / Kotlin / DBeaver):
```text
jdbc:postgresql://localhost:5432/watir_db?user=watir_user&password=watir_password
```

> **Ważna uwaga dla WSL2:** Jeśli próbujesz połączyć się z bazą danych uruchomioną na WSL2 z poziomu aplikacji mobilnej na fizycznym telefonie, zamiast `localhost` musisz użyć adresu IP swojego komputera w sieci lokalnej (np. `192.168.1.15`).

## 5. Struktura tabel (Schema)
Baza inicjalizuje się automatycznie ze skryptu `init.sql`. Projekt wykorzystuje dwie główne tabele:

*   **`telemetry_logs`** - przechowuje odczyty z sensorów przesyłane z ESP8266.
*   **`plant_profiles`** - przechowuje predefiniowane progi nawadniania (np. `moisture_threshold`).

## 6. Przykładowe zapytanie testowe (SQL)
Aby sprawdzić, czy tabele istnieją i czy dane spływają poprawnie, wykonaj w DBeaver:
```sql
SELECT * FROM telemetry_logs ORDER BY created_at DESC LIMIT 10;
```

## 7. Rozwiązywanie problemów
*   **Błąd połączenia z Dockerem w WSL:** Upewnij się, że w ustawieniach Docker Desktop (Settings -> Resources -> WSL Integration) zaznaczono integrację z Twoją dystrybucją Ubuntu.
*   **Zamykanie środowiska:** Aby wyłączyć bazę, wpisz w folderze backend: `docker-compose down`.
*   **Restart bazy:** Jeśli chcesz całkowicie wyczyścić bazę i dane (reset tabel), użyj: `docker-compose down -v`.

---
*Dokumentacja techniczna projektu WATIR IoT. Zatwierdzono przez zespół projektowy.*
