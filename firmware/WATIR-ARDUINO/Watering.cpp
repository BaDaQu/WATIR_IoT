#include "Watering.h"
#include "ServoControl.h"
#include <Arduino.h>

// Ustawienia systemu nawadniania
const int pinPrzekaznika = 12; // Pin załączający pompę
const int progWilgotnosci = 35; // Procent, poniżej którego aktywuje się podlewanie

// Inicjalizacja układu pompy
void konfigurujPodlewanie() {
    pinMode(pinPrzekaznika, OUTPUT);
    digitalWrite(pinPrzekaznika, HIGH); // Przekaźniki są często aktywowane stanem LOW, więc domyślnie wyłączony
}

// Szybkie podlanie np. po ręcznym kliknięciu
void podlej() {
    digitalWrite(pinPrzekaznika, LOW);  // Włącz pompę
    delay(3000);                        // Pompuj wodę przez 3 sekundy
    digitalWrite(pinPrzekaznika, HIGH); // Wyłącz pompę
}

// Inteligentna logika dbająca o rośliny
void logikaPodlewania(int dystans, int g1, int g2) {
    // Zabezpieczenie przed przepaleniem pompy (Fail-Safe)
    if (dystans >= 12) return;

    // Sprawdzanie i podlewanie Rośliny nr 1
    if (g1 < progWilgotnosci) {
        ustawNadRoslina(1); // Wyceluj ramię w pierwszą doniczkę
        digitalWrite(pinPrzekaznika, LOW);
        delay(3000);
        digitalWrite(pinPrzekaznika, HIGH);
        delay(2000); // Odczekaj 2s na wsiąknięcie wody
    }

    // Sprawdzanie i podlewanie Rośliny nr 2
    if (g2 < progWilgotnosci) {
        ustawNadRoslina(2); // Wyceluj ramię w drugą doniczkę
        digitalWrite(pinPrzekaznika, LOW);
        delay(3000);
        digitalWrite(pinPrzekaznika, HIGH);
        delay(2000); // Odczekaj 2s na wsiąknięcie wody
        powrotDoBazy()
    }
}