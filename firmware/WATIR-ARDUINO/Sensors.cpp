#include "Sensors.h"
#include <Adafruit_Sensor.h>
#include <Adafruit_BME280.h>

// Piny dla czujników środowiskowych
const int pinGleba1 = A1;  
const int pinGleba2 = A0; 
const int pinTrig = 8;
const int pinEcho = 7;

Adafruit_BME280 bme; // Obiekt czujnika BME280

// Inicjalizacja czujników przy starcie
bool konfigurujCzujniki() {
  pinMode(pinTrig, OUTPUT);
  pinMode(pinEcho, INPUT);
  return bme.begin(0x77); // Adres I2C czujnika
}

// Pomiar odległości do lustra wody (w centymetrach)
int zmierzDystans() {
  digitalWrite(pinTrig, LOW); delayMicroseconds(2);
  digitalWrite(pinTrig, HIGH); delayMicroseconds(10);
  digitalWrite(pinTrig, LOW);
  
  long czasTrwania = pulseIn(pinEcho, HIGH, 25000);
  if (czasTrwania == 0) return 999; // Zwróć błąd jeśli brak echa
  return czasTrwania * 0.034 / 2;
}

// Odczyt z czujnika gleby nr 1 (wartość w procentach: 0-100%)
int zmierzWilgotnoscGleby1() {
  return constrain(map(analogRead(pinGleba1), 0, 1023, 0, 100), 0, 100);
}

// Odczyt z czujnika gleby nr 2 (wartość w procentach: 0-100%)
int zmierzWilgotnoscGleby2() {
  return constrain(map(analogRead(pinGleba2), 0, 1023, 0, 100), 0, 100);
}

// Odczyt temperatury z BME280
float zmierzTemperature() { 
  return bme.readTemperature();
}

// Odczyt wilgotności powietrza z BME280
float zmierzWilgotnoscPowietrza() { 
  return bme.readHumidity(); 
}