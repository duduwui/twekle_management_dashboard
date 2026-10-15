# Twekl Dashboard - 24/7 Traffic Simulator & Live Performance Monitor

This suite simulates realistic high-volume traffic (up to **2,000 requests/sec**) against Twekl Dashboard for **30 days continuously**, providing real-time live performance monitoring.

---

## 1. Features
- **Target Rate**: Configurable up to **2,000 req/s**.
- **Realistic Weighted Operations**:
  - **60% Customer & Follow-up Actions**: Querying customer directory, checking overdue follow-ups, adding call notes, completing items, creating diverse customer leads.
  - **20% Dashboard SSR**: Continuous overview & KPI rendering.
  - **10% Report Tabs**: Requesting milestone, products, satisfaction, and follow-up data.
  - **5% Static Time Filters**: Strictly limited to **6 static presets** (`TODAY`, `YESTERDAY`, `LAST_7_DAYS`, `LAST_30_DAYS`, `THIS_MONTH`, `LAST_MONTH`).
  - **5% Staff & Roles**: Admin logins and role permission checks.
- **Embedded Real-Time Web Dashboard**: Serves live metrics on port `9090` (live RPS, total requests, success/error rates, p95/p99 latency, server status badge).
- **Persistent Daemon**: Systemd service runs 24/7 on your VPS even when your laptop is turned off or SSH is closed.

---

## 2. Quick Setup on Your VPS

### Step 1: Install Python Dependencies
```bash
cd scripts/load-simulation
pip3 install -r requirements.txt
```

### Step 2: Run in Foreground (Testing)
```bash
python3 traffic_simulator.py --host http://localhost:8080 --rate 2000 --port 9090
```

### Step 3: Run as a 24/7 Background System Service (for 30 Days)
To keep it running non-stop even when you close your laptop:
```bash
# 1. Copy service file to systemd directory
sudo cp twekl-simulator.service /etc/systemd/system/

# 2. Reload systemd daemon
sudo systemctl daemon-reload

# 3. Enable and start the service
sudo systemctl enable --now twekl-simulator.service

# 4. Check status
sudo systemctl status twekl-simulator.service
```

---

## 3. How to View Live Performance in Your Browser

### A. Live Simulation Status Dashboard (Port 9090)
Open in any browser (phone, laptop, tablet):
```
http://<YOUR-VPS-IP>:9090
```
- **Real-Time RPS Meter**: Live requests/sec count.
- **Server Health Status**: Green (Healthy), Orange (Degraded/High Latency), Red (Down/5xx errors).
- **Latency**: Real-time average, p95, and p99 response times.
- **Error Log**: Displays recent error responses in real-time.

### B. Live VPS System Resources (CPU, RAM, Disk, MySQL, Tomcat)
To see your VPS hardware consumption live:
```bash
# Install glances on VPS
sudo apt update && sudo apt install -y glances

# Run glances web server
glances -w -p 61208 &
```
Open in your browser:
```
http://<YOUR-VPS-IP>:61208
```
You will see real-time graphs and tables for:
- CPU usage percentage across all cores.
- RAM and Swap memory usage.
- Disk read/write I/O and network throughput.
- MySQL and Tomcat resource consumption.
