// ============================================
// WATIR IoT — Firmware dla Arduino UNO R4 WiFi
// Wersja: 2.0 — WiFi wbudowane (bez osobnego ESP)
// ============================================
//
// Moduł: Podlewanie
// Plik nagłówkowy dla funkcji sterujących pompą wodną
// za pomocą sterownika silników L298N (12V/2A).
//
// Podłączenie L298N:
//   ENA → Pin 3  (PWM — regulacja mocy 0-100%)
//   IN1 → Pin 5  (kierunek +)
//   IN2 → Pin 4  (kierunek -)
//   OUT1 → pompa (+)
//   OUT2 → pompa (-)
//

#ifndef WATERING_H
#define WATERING_H

void konfigurujPodlewanie();
void podlej();
void ustawPompe(bool wlacz);       // Bezpośrednie włącz/wyłącz pompę
void ustawMocPompy(int procent);   // Ustawia moc 0–100%
int  pobierzMocPompy();            // Zwraca aktualną moc 0–100%

#endif