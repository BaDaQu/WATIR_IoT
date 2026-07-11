#ifndef CONFIG_H
#define CONFIG_H

#include <Arduino.h>

struct WatirConfig {
  uint32_t magic;
  char wifi_ssid[32];
  char wifi_pass[64];
  int p1_moisture;
  int p1_sensor;
  int p1_pan;
  int p1_tilt;
  int p1_pump_power;
  int p2_moisture;
  int p2_sensor;
  int p2_pan;
  int p2_tilt;
  int p2_pump_power;
  int min_temp_block;
  int max_temp_force;
  int min_air_humidity_force;
  bool auto_watering;
  unsigned long check_interval_ms;
};

extern WatirConfig watirConfig;

#endif
