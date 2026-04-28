# WATIR_IoT
Projekt WATIR IoT to kompleksowy system sprzętowo-programowy do autonomicznego nawadniania roślin, łączący mikrokontroler ESP32, chmurową bazę danych oraz dedykowaną aplikację mobilną. Naszym celem jest zbudowanie kompletnego ekosystemu, który zdejmuje z użytkownika obowiązek pamiętania o podlewaniu

# 🛠️ Instrukcja uruchomienia środowiska WATIR_IoT

Ten dokument opisuje krok po kroku, jak przygotować i uruchomić lokalną bazę danych oraz infrastrukturę backendową przy użyciu **WSL2** oraz **Docker Desktop**.

## 1. Wymagania wstępne
Przed rozpoczęciem upewnij się, że masz zainstalowane:
*   **Docker Desktop** (z włączoną opcją "WSL Integration" w ustawieniach).
*   **WSL2** (rekomendowana dystrybucja: Ubuntu).
*   **DBeaver** lub **Postman** (do testowania połączenia).

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
2.  **Stwórz plik `.env`**, ponieważ jest on ignorowany przez Git (bezpieczeństwo). Skopiuj szablon:
    ```bash
    cp .env.example .env
    ```
    *Upewnij się, że dane w `.env` zgadzają się z Twoją lokalną konfiguracją.*

3.  Uruchom kontener:
    ```bash
    docker-compose up -d
    ```

## 4. Weryfikacja działania
Po uruchomieniu bazy, tabele zostaną automatycznie utworzone dzięki skryptowi `init.sql`. Możesz to sprawdzić komendą:
```bash
docker-compose ps
```
Powinieneś zobaczyć kontener `watir_db` ze statusem **Up**.

### Dane połączenia:
*   **Host:** `localhost` lub `127.0.0.1`
*   **Port:** `5432`
*   **Database:** `watir_db`
*   **User:** `watir_user`
*   **Password:** `watir_password`

## 5. Struktura tabel (Schema)
Projekt wykorzystuje dwie główne tabele:
*   `telemetry_logs` - przechowuje odczyty z sensorów (temp, wilgotność, stan pompy itp.).
*   `plant_profiles` - przechowuje ustawienia progów nawadniania dla różnych roślin.

## 6. Rozwiązywanie problemów
*   **Błąd połączenia z Dockerem w WSL:** Upewnij się, że w ustawieniach Docker Desktop zaznaczono integrację z Twoją konkretną dystrybucją Ubuntu.
*   **Zamykanie środowiska:** Aby wyłączyć bazę i zwolnić zasoby, wpisz w folderze backend: `docker-compose down`.
*   **Restart bazy:** Jeśli chcesz całkowicie wyczyścić bazę i dane, użyj: `docker-compose down -v`.


*Zatwierdzone przez zespół projektowy WATIR.*

