import tkinter as tk
from tkinter import ttk, messagebox
import serial
import serial.tools.list_ports
import json
import time

class WatirConfigApp:
    def __init__(self, root):
        self.root = root
        self.root.title("WATIR - Konfigurator Offline (Serial)")
        self.root.geometry("500x700")
        self.root.resizable(False, False)

        self.serial_conn = None
        self.current_pan = 90
        self.current_tilt = 90

        self.create_widgets()
        self.refresh_ports()
        self.root.after(100, self.read_serial_loop)

    def create_widgets(self):
        # --- Sekcja połączenia ---
        conn_frame = ttk.LabelFrame(self.root, text="Połączenie", padding=(10, 5))
        conn_frame.pack(fill=tk.X, padx=10, pady=5)

        ttk.Label(conn_frame, text="Port COM:").grid(row=0, column=0, padx=5, pady=5, sticky=tk.W)
        self.port_var = tk.StringVar()
        self.port_combo = ttk.Combobox(conn_frame, textvariable=self.port_var, state="readonly", width=15)
        self.port_combo.grid(row=0, column=1, padx=5, pady=5)

        ttk.Button(conn_frame, text="Odśwież", command=self.refresh_ports, width=10).grid(row=0, column=2, padx=5, pady=5)
        
        self.connect_btn = ttk.Button(conn_frame, text="Połącz", command=self.toggle_connection, width=10)
        self.connect_btn.grid(row=0, column=3, padx=5, pady=5)

        # --- Sekcja konfiguracji (Główne Parametry) ---
        self.config_frame = ttk.LabelFrame(self.root, text="Główne Parametry (Klimat i System)", padding=(10, 5))
        self.config_frame.pack(fill=tk.X, padx=10, pady=5)

        row = 0
        ttk.Label(self.config_frame, text="Moc pompy (%):").grid(row=row, column=0, sticky=tk.W, pady=5)
        self.pump_power_var = tk.IntVar(value=70)
        ttk.Spinbox(self.config_frame, from_=0, to=100, textvariable=self.pump_power_var, width=10).grid(row=row, column=1, pady=5)

        row += 1
        ttk.Label(self.config_frame, text="Interwał sprawdzania (sek):").grid(row=row, column=0, sticky=tk.W, pady=5)
        self.interval_var = tk.IntVar(value=10)
        ttk.Spinbox(self.config_frame, from_=1, to=3600, textvariable=self.interval_var, width=10).grid(row=row, column=1, pady=5)

        row += 1
        ttk.Label(self.config_frame, text="Temp. min. (blokada podlewania) °C:").grid(row=row, column=0, sticky=tk.W, pady=5)
        self.min_temp_var = tk.IntVar(value=5)
        ttk.Spinbox(self.config_frame, from_=-20, to=50, textvariable=self.min_temp_var, width=10).grid(row=row, column=1, pady=5)

        row += 1
        ttk.Label(self.config_frame, text="Temp. max. (wymuszenie) °C:").grid(row=row, column=0, sticky=tk.W, pady=5)
        self.max_temp_var = tk.IntVar(value=35)
        ttk.Spinbox(self.config_frame, from_=0, to=80, textvariable=self.max_temp_var, width=10).grid(row=row, column=1, pady=5)

        row += 1
        self.auto_watering_var = tk.BooleanVar(value=True)
        ttk.Checkbutton(self.config_frame, text="Tryb automatycznego podlewania", variable=self.auto_watering_var).grid(row=row, column=0, columnspan=2, sticky=tk.W, pady=5)

        action_frame = ttk.Frame(self.config_frame)
        action_frame.grid(row=row+1, column=0, columnspan=2, pady=5, sticky=tk.E)
        self.send_btn = ttk.Button(action_frame, text="Zapisz główne parametry", command=self.send_main_config, state=tk.DISABLED)
        self.send_btn.pack(side=tk.RIGHT, ipadx=10)

        # --- Sekcja Rośliny 1 ---
        self.p1_frame = ttk.LabelFrame(self.root, text="Roślina 1", padding=(10, 5))
        self.p1_frame.pack(fill=tk.X, padx=10, pady=5)

        row = 0
        ttk.Label(self.p1_frame, text="Próg wilgotności gleby (%):").grid(row=row, column=0, sticky=tk.W, pady=5)
        self.p1_moisture_var = tk.IntVar(value=35)
        ttk.Spinbox(self.p1_frame, from_=0, to=100, textvariable=self.p1_moisture_var, width=10).grid(row=row, column=1, pady=5)

        row += 1
        ttk.Label(self.p1_frame, text="Czujnik podpięty do rośliny:").grid(row=row, column=0, sticky=tk.W, pady=5)
        self.p1_sensor_var = tk.StringVar(value="Czujnik A1 (Pin 15)")
        self.p1_sensor_combo = ttk.Combobox(self.p1_frame, textvariable=self.p1_sensor_var, values=["Czujnik A0 (Pin 14)", "Czujnik A1 (Pin 15)"], state="readonly", width=20)
        self.p1_sensor_combo.grid(row=row, column=1, pady=5)

        row += 1
        self.p1_pos_label = ttk.Label(self.p1_frame, text="Aktualna pozycja joysticka: 90° X / 90° Y")
        self.p1_pos_label.grid(row=row, column=0, columnspan=2, sticky=tk.W, pady=5)

        row += 1
        self.save_plant1_btn = ttk.Button(self.p1_frame, text="Zapisz konfigurację Rośliny 1 (Kąty + Czujnik + Próg)", command=lambda: self.send_plant_config(1), state=tk.DISABLED)
        self.save_plant1_btn.grid(row=row, column=0, columnspan=2, pady=5)

        # --- Sekcja Rośliny 2 ---
        self.p2_frame = ttk.LabelFrame(self.root, text="Roślina 2", padding=(10, 5))
        self.p2_frame.pack(fill=tk.X, padx=10, pady=5)

        row = 0
        ttk.Label(self.p2_frame, text="Próg wilgotności gleby (%):").grid(row=row, column=0, sticky=tk.W, pady=5)
        self.p2_moisture_var = tk.IntVar(value=35)
        ttk.Spinbox(self.p2_frame, from_=0, to=100, textvariable=self.p2_moisture_var, width=10).grid(row=row, column=1, pady=5)

        row += 1
        ttk.Label(self.p2_frame, text="Czujnik podpięty do rośliny:").grid(row=row, column=0, sticky=tk.W, pady=5)
        self.p2_sensor_var = tk.StringVar(value="Czujnik A0 (Pin 14)")
        self.p2_sensor_combo = ttk.Combobox(self.p2_frame, textvariable=self.p2_sensor_var, values=["Czujnik A0 (Pin 14)", "Czujnik A1 (Pin 15)"], state="readonly", width=20)
        self.p2_sensor_combo.grid(row=row, column=1, pady=5)

        row += 1
        self.p2_pos_label = ttk.Label(self.p2_frame, text="Aktualna pozycja joysticka: 90° X / 90° Y")
        self.p2_pos_label.grid(row=row, column=0, columnspan=2, sticky=tk.W, pady=5)

        row += 1
        self.save_plant2_btn = ttk.Button(self.p2_frame, text="Zapisz konfigurację Rośliny 2 (Kąty + Czujnik + Próg)", command=lambda: self.send_plant_config(2), state=tk.DISABLED)
        self.save_plant2_btn.grid(row=row, column=0, columnspan=2, pady=5)

    def refresh_ports(self):
        ports = [port.device for port in serial.tools.list_ports.comports()]
        self.port_combo['values'] = ports
        if ports:
            self.port_combo.current(0)
        else:
            self.port_combo.set("Brak urządzeń")

    def toggle_connection(self):
        if self.serial_conn and self.serial_conn.is_open:
            self.serial_conn.close()
            self.connect_btn.config(text="Połącz")
            self.send_btn.config(state=tk.DISABLED)
            self.save_plant1_btn.config(state=tk.DISABLED)
            self.save_plant2_btn.config(state=tk.DISABLED)
            self.port_combo.config(state="readonly")
        else:
            port = self.port_var.get()
            if not port or port == "Brak urządzeń":
                messagebox.showerror("Błąd", "Nie wybrano poprawnego portu COM!")
                return
            try:
                self.serial_conn = serial.Serial(port, 115200, timeout=1)
                time.sleep(2)  
                
                self.connect_btn.config(text="Rozłącz")
                self.send_btn.config(state=tk.NORMAL)
                self.save_plant1_btn.config(state=tk.NORMAL)
                self.save_plant2_btn.config(state=tk.NORMAL)
                self.port_combo.config(state=tk.DISABLED)
            except Exception as e:
                messagebox.showerror("Błąd połączenia", str(e))

    def _send_json_and_wait(self, config_data):
        if not self.serial_conn or not self.serial_conn.is_open:
            messagebox.showerror("Błąd", "Brak połączenia z urządzeniem!")
            return

        try:
            json_str = json.dumps(config_data) + "\n"
            self.serial_conn.reset_input_buffer()
            self.serial_conn.write(json_str.encode('utf-8'))
            
            start_time = time.time()
            response = ""
            
            while time.time() - start_time < 2:
                if self.serial_conn.in_waiting > 0:
                    line = self.serial_conn.readline().decode('utf-8', errors='ignore').strip()
                    if line.startswith("{") and "telemetry" not in line:
                        response = line
                        break
            
            if response:
                try:
                    resp_json = json.loads(response)
                    if resp_json.get("status") == "success":
                        messagebox.showinfo("Sukces", "Parametry zostały pomyślnie zapisane na urządzeniu!")
                    else:
                        messagebox.showwarning("Uwaga", f"Odpowiedź z urządzenia: {response}")
                except json.JSONDecodeError:
                    messagebox.showinfo("Informacja", f"Zapisano, odpowiedź: {response}")
            else:
                messagebox.showwarning("Brak odpowiedzi", "Wysłano konfigurację, ale Arduino nie odpowiedziało.")
                
        except Exception as e:
            messagebox.showerror("Błąd komunikacji", str(e))

    def send_main_config(self):
        config_data = {
            "config": {
                "pump_power": self.pump_power_var.get(),
                "check_interval_ms": self.interval_var.get() * 1000,
                "auto_watering": self.auto_watering_var.get(),
                "min_temp_block": self.min_temp_var.get(),
                "max_temp_force": self.max_temp_var.get()
            }
        }
        self._send_json_and_wait(config_data)

    def _parse_sensor_pin(self, val):
        if "A0" in val:
            return 14
        if "A1" in val:
            return 15
        return 14

    def send_plant_config(self, plant_id):
        if plant_id == 1:
            config_data = {
                "config": {
                    "target_plant": 1,
                    "moisture_threshold": self.p1_moisture_var.get(),
                    "sensor": self._parse_sensor_pin(self.p1_sensor_var.get()),
                    "pan": self.current_pan,
                    "tilt": self.current_tilt
                }
            }
        else:
            config_data = {
                "config": {
                    "target_plant": 2,
                    "moisture_threshold": self.p2_moisture_var.get(),
                    "sensor": self._parse_sensor_pin(self.p2_sensor_var.get()),
                    "pan": self.current_pan,
                    "tilt": self.current_tilt
                }
            }
        self._send_json_and_wait(config_data)

    def read_serial_loop(self):
        if not self.root.winfo_exists():
            return
            
        if self.serial_conn and self.serial_conn.is_open:
            try:
                while self.serial_conn.in_waiting > 0:
                    line = self.serial_conn.readline().decode('utf-8', errors='ignore').strip()
                    if line.startswith('{"telemetry"'):
                        try:
                            data = json.loads(line)
                            pan = data["telemetry"]["pan"]
                            tilt = data["telemetry"]["tilt"]
                            self.current_pan = pan
                            self.current_tilt = tilt
                            label_txt = f"Aktualna pozycja joysticka: {pan}° X / {tilt}° Y"
                            self.p1_pos_label.config(text=label_txt)
                            self.p2_pos_label.config(text=label_txt)
                        except (json.JSONDecodeError, KeyError):
                            pass
            except Exception:
                pass
                
        self.root.after(100, self.read_serial_loop)

if __name__ == "__main__":
    root = tk.Tk()
    app = WatirConfigApp(root)
    root.mainloop()
