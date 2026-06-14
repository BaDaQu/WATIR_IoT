#ifndef SERVO_CONTROL_H
#define SERVO_CONTROL_H

#include <Arduino.h>

void konfigurujSerwa();
void aktualizujSerwa(String komendaWiFi, bool blokadaSerw);
int pobierzKierunekJoysticka();
int pobierzPozycjeSerwaX();
int pobierzPozycjeSerwaY();
void uzyjSerw(bool wlacz);
void zapiszPozycje(int roslina);
void ustawNadRoslina(int roslina);
void powrotDoBazy();

#endif