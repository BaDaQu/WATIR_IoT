// ============================================
// WATIR IoT — Firmware dla Arduino UNO R4 WiFi
// ============================================
//
// Moduł: Podlewanie
// Implementacja funkcji sterujących pompą wodną
// za pomocą sterownika silników L298N (12V/2A).
//
// Podłączenie L298N:
//   ENA → Pin 3  (PWM — regulacja mocy 0-100%)
//   IN1 → Pin 5  (kierunek +)
//   IN2 → Pin 4  (kierunek -)
//   OUT1 → pompa (+)
//   OUT2 → pompa (-)
//
// UWAGA: Zdjąć zworkę (jumper) z ENA na module L298N!
//

#include "Watering.h"
#include "ServoControl.h"
#include <Arduino.h>

// =============================================
// PINY STEROWNIKA L298N
// =============================================
const int pinL298N_ENA = 3;   // PWM — regulacja mocy pompy (0-255)
const int pinL298N_IN1 = 5;   // Kierunek „do przodu" — OUT1 = +
const int pinL298N_IN2 = 4;   // Kierunek „do tyłu"  — OUT2 = -

// Próg wilgotności do automatycznego podlewania
const int progWilgotnosci = 35; // Procent, poniżej którego aktywuje się podlewanie

// =============================================
// DOMYŚLNA MOC POMPY (0–100%)
// =============================================
// Można zmienić dynamicznie przez ustawMocPompy().
// 100% ≈ 2A przy zasilaniu 12V.
//
// UWAGA: Zasilacz 3A jest dzielony między L298N i przetwornicę 5V.
// Przy 100% pompa + reszta układu ≈ 3A → ryzyko brownoutu!
// Bezpieczny zakres: 30–70% (zostawia zapas dla Arduino/serw).
static int mocPompy = 70;  // Domyślnie 70% — bezpieczne dla zasilacza 3A

// =============================================
// KONWERSJA PROCENT → PWM (0–255)
// =============================================
static int procentNaPWM(int procent) {
  // Użytkownik przesyła moc z zakresu 0-100 (lub 1-100)
  procent = constrain(procent, 0, 100);
  
  // Ograniczenie sprzętowe:
  // 0-1% -> 45% (min. moc do ruszenia wirnika)
  // 100% -> 70% (bezpieczny prąd dla zasilacza współdzielonego)
  int rzeczywistyProcent = map(procent, 0, 100, 45, 70);
  
  // Zwracamy PWM (0-255) z wyliczonego przedziału 45-70%
  return map(rzeczywistyProcent, 0, 100, 0, 255);
}

// =============================================
// STEROWANIE POMPĄ PRZEZ L298N
// =============================================
// Włączenie: ENA=PWM, IN1=HIGH, IN2=LOW  →  prąd płynie OUT1(+) → OUT2(-)
// Wyłączenie: ENA=0                      →  silnik zatrzymany
//
void ustawPompe(bool wlacz) {
  if (wlacz) {
    digitalWrite(pinL298N_IN1, HIGH);
    digitalWrite(pinL298N_IN2, LOW);
    analogWrite(pinL298N_ENA, procentNaPWM(mocPompy));
  } else {
    analogWrite(pinL298N_ENA, 0);
    digitalWrite(pinL298N_IN1, LOW);
    digitalWrite(pinL298N_IN2, LOW);
  }
}

// =============================================
// API PUBLICZNE
// =============================================

// Inicjalizacja pinów sterownika L298N
void konfigurujPodlewanie() {
  pinMode(pinL298N_ENA, OUTPUT);
  pinMode(pinL298N_IN1, OUTPUT);
  pinMode(pinL298N_IN2, OUTPUT);
  ustawPompe(false); // Pompa domyślnie wyłączona
}

// Ustawienie mocy pompy (0–100%)
// 100% ≈ 2A, 50% ≈ 1A, itd.
void ustawMocPompy(int procent) {
  mocPompy = constrain(procent, 0, 100);
  Serial.print("[POMPA] Moc ustawiona na: ");
  Serial.print(mocPompy);
  Serial.println("%");
}

// Pobranie aktualnej mocy pompy (0–100%)
int pobierzMocPompy() {
  return mocPompy;
}

// Szybkie podlanie np. po ręcznym kliknięciu
void podlej() {
  ustawPompe(true);   // Włącz pompę z aktualną mocą
  delay(3000);        // Pompuj wodę przez 3 sekundy
  ustawPompe(false);  // Wyłącz pompę
}
