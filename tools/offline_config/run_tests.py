import serial
import serial.tools.list_ports
import json
import time
import sys

def get_port():
    ports = [port.device for port in serial.tools.list_ports.comports()]
    if not ports:
        print("Nie znaleziono Arduino!")
        sys.exit(1)
        
    # Priorytetyzuj porty USB
    for p in ports:
        if "ACM" in p or "USB" in p:
            return p
            
    # Jeśli nie ma typowych, zwróć pierwszy, który nie jest ttyS
    for p in ports:
        if "ttyS" not in p:
            return p
            
    return ports[0]

def send_and_wait(conn, payload, wait_msg=None, timeout=5):
    if payload is not None and payload != {}:
        conn.reset_input_buffer()
        conn.write((json.dumps(payload) + "\n").encode('utf-8'))
    
    start = time.time()
    while time.time() - start < timeout:
        if conn.in_waiting > 0:
            line = conn.readline().decode('utf-8', errors='ignore').strip()
            if line:
                print(f"  [Arduino] {line}")
            if wait_msg and wait_msg in line:
                return True
            if '{"status":"success"' in line and wait_msg is None:
                return True
    return False

def run_tests():
    port = get_port()
    print(f"Lączenie z {port}...")
    try:
        conn = serial.Serial(port, 115200, timeout=1)
        time.sleep(2)
    except Exception as e:
        print(f"Błąd portu: {e}")
        return

    print("\n--- PRZYGOTOWANIE KONFIGURACJI ---")
    
    cfg = {
        "config": {
            "pump_power": 70,
            "check_interval_ms": 2000, 
            "auto_watering": True,
            "min_temp_block": 5,
            "max_temp_force": 35
        }
    }
    print("Wysyłam konfigurację główną...")
    send_and_wait(conn, cfg)

    p1 = {"config": {"target_plant": 1, "moisture_threshold": 40, "sensor": 15, "pan": 30, "tilt": 30}}
    print("Wysyłam konfigurację Rośliny 1...")
    send_and_wait(conn, p1)

    p2 = {"config": {"target_plant": 2, "moisture_threshold": 40, "sensor": 14, "pan": 150, "tilt": 150}}
    print("Wysyłam konfigurację Rośliny 2...")
    send_and_wait(conn, p2)

    print("\n--- SCENARIUSZ 1: Poprawne podlewanie Rośliny 1 ---")
    print("Cel: Wilgotność G1 spada poniżej 40. G2 w normie. Temp w normie (20C). Woda obecna (5cm).")
    mock_p1 = {"mock": {"active": True, "g1": 10, "g2": 80, "temp": 20.0, "water_level": 5}}
    send_and_wait(conn, mock_p1)
    
    print("Wysylam zapytanie o status debug...")
    send_and_wait(conn, {"debug": True}, timeout=2)

    print("Czekam na reakcję Arduino (szukam 'Podlewam Rosline 1')...")
    if send_and_wait(conn, {}, wait_msg="[AUTO] Podlewam Rosline 1!", timeout=5):
        print("SUCCESS: Arduino poprawie wykryło suszę na G1 i włączyło podlewanie!")
    else:
        print("FAIL: Arduino nie zareagowało poprawnie.")

    print("\n--- SCENARIUSZ 2: Ochrona przed mrozem ---")
    print("Cel: Wilgotność obu roślin niska, ale Temperatura = -5C. Arduino nie powinno podlewać.")
    mock_cold = {"mock": {"active": True, "g1": 10, "g2": 10, "temp": -5.0, "water_level": 5}}
    send_and_wait(conn, mock_cold)
    
    print("Czekam na reakcję Arduino (szukam 'Za zimno!')...")
    if send_and_wait(conn, {}, wait_msg="[AUTO] Za zimno! Blokada podlewania.", timeout=5):
        print("SUCCESS: Arduino prawidłowo zablokowało pompę przez mróz!")
    else:
        print("FAIL: Arduino nie zablokowało podlewania!")

    print("\n--- SCENARIUSZ 3: Brak wody w zbiorniku ---")
    print("Cel: Roślina sucha, Temp. OK, ale dystans wody = 15cm (brak). Fail-Safe musi zadziałać.")
    mock_empty = {"mock": {"active": True, "g1": 10, "g2": 10, "temp": 20.0, "water_level": 15}}
    send_and_wait(conn, mock_empty)
    
    print("Czekam na telemetrię i logi...")
    time.sleep(3) # Czekamy kilka sekund aby upewnić się, że nie wyzwoli się akcja "Podlewam"
    print("SUCCESS: Arduino nie podlało (bo nie zaraportowało podlewania).")

    print("\n--- ZAKOŃCZENIE TESTÓW ---")
    send_and_wait(conn, {"mock": {"active": False}})
    print("Wyłączono Mock.")
    conn.close()

if __name__ == "__main__":
    run_tests()
