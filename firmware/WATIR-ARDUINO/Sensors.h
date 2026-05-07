#ifndef SENSORS_H
#define SENSORS_H
#include <Arduino.h>

bool konfigurujCzujniki();
int zmierzDystans();
int zmierzWilgotnoscGleby1(); 
int zmierzWilgotnoscGleby2(); 
float zmierzTemperature();
float zmierzWilgotnoscPowietrza();

#endif