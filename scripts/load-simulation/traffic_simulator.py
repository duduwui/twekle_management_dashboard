#!/usr/bin/env python3
"""
TWEKL DASHBOARD - Enterprise Traffic & Load Simulator
24/7 Endurance & Stress Testing Suite (Target: 35-100 req/s)
Includes Dynamic Customer Lifecycle (Auto-creates real Customers, Orders & Follow-ups),
Live Real-Time Web Dashboard on Port 9090, and VPS Hardware Telemetry.
"""

import asyncio
import aiohttp
from aiohttp import web
import argparse
import random
import time
import os
import re
from datetime import datetime, timezone
from collections import deque

try:
    import psutil
except ImportError:
    psutil = None

# =========================================================
# CONFIGURATION CONSTANTS
# =========================================================
STATIC_TIME_FILTERS = [
    "TODAY",
    "YESTERDAY",
    "LAST_7_DAYS",
    "LAST_30_DAYS",
    "THIS_MONTH",
    "LAST_MONTH"
]

SAMPLE_COMPANIES = [
    'Dijla Trading Group', 'Euphrates Retail Hub', 'Babylon Tech Mart',
    'Al-Mansour POS Solutions', 'Nineveh Logistics Corp', 'Korek Commercial Mart',
    'Ur Wholesale Distribution', 'Chavi Plaza Stores', 'Mesopotamia Supply Co',
    'Al-Ameen Logistics', 'Tigris Electronics Center', 'Sumerian Food Mart'
]

SAMPLE_PRODUCTS = [
    'Thermal Receipt Printers (x2) + Barcode Scanner',
    'Dual Screen POS Terminal & Cash Drawer',
    'Cloud ERP Sync & Thermal Terminal',
    'Heavy Duty Kitchen Display & Network Printers',
    'Wireless Inventory Scanners & Mobile POS Hub',
    'Enterprise Barcode Label Printers (x4)'
]

SAMPLE_CITIES = [
    'Erbil', 'Baghdad', 'Basra', 'Sulaymaniyah', 'Duhok', 'Najaf', 'Karbala', 'Mosul'
]

# =========================================================
# REAL-TIME METRICS & TELEMETRY REGISTRY
# =========================================================
class MetricsRegistry:
    def __init__(self):
        self.start_time = time.time()
        self.total_requests = 0
        self.total_success = 0
        self.total_errors = 0
        self.customers_created = 0
        self.recent_latencies = deque(maxlen=5000)
        self.recent_errors = deque(maxlen=50)
        self.rps_history = deque(maxlen=60)
        
        self.category_counts = {
            "followup": 0,
            "dashboard": 0,
            "reports": 0,
            "timefilter": 0,
            "admin": 0
        }
        
        self.window_requests = 0
        self.current_rps = 0
        self.last_window_time = time.time()

        if psutil:
            psutil.cpu_percent(interval=None)

    def record_result(self, category: str, status_code: int, latency_ms: float, err_msg: str = None):
        self.total_requests += 1
        self.window_requests += 1
        self.category_counts[category] = self.category_counts.get(category, 0) + 1
        self.recent_latencies.append(latency_ms)

        if 200 <= status_code < 400:
            self.total_success += 1
        else:
            self.total_errors += 1
            if err_msg or status_code >= 400:
                self.recent_errors.append({
                    "timestamp": datetime.now(timezone.utc).strftime("%H:%M:%S"),
                    "category": category,
                    "status": status_code,
                    "message": err_msg or f"HTTP {status_code}"
                })

    def tick_second(self):
        now = time.time()
        elapsed = now - self.last_window_time
        if elapsed >= 1.0:
            self.current_rps = int(self.window_requests / elapsed)
            self.rps_history.append(self.current_rps)
            self.window_requests = 0
            self.last_window_time = now

    def get_system_stats(self):
        if not psutil:
            return {"cpu_percent": 0.0, "ram_percent": 0.0, "ram_used_gb": 0.0, "ram_total_gb": 0.0, "tomcat_memory": "N/A"}
        try:
            cpu = psutil.cpu_percent(interval=None)
            mem = psutil.virtual_memory()
            tomcat_mem = "Active"
            for p in psutil.process_iter(['cmdline', 'memory_info']):
                cmd = " ".join(p.info['cmdline'] or [])
                if 'TomcatServer' in cmd:
                    tomcat_mem = f"{p.info['memory_info'].rss // (1024 * 1024)} MB"
                    break
            return {
                "cpu_percent": round(cpu, 1),
                "ram_percent": round(mem.percent, 1),
                "ram_used_gb": round(mem.used / (1024**3), 2),
                "ram_total_gb": round(mem.total / (1024**3), 2),
                "tomcat_memory": tomcat_mem
            }
        except Exception:
            return {"cpu_percent": 0.0, "ram_percent": 0.0, "ram_used_gb": 0.0, "ram_total_gb": 0.0, "tomcat_memory": "N/A"}

    def get_summary(self, pool_size: int):
        latencies = list(self.recent_latencies)
        avg_lat = round(sum(latencies) / len(latencies), 1) if latencies else 0.0
        p95_lat = round(sorted(latencies)[int(len(latencies) * 0.95)], 1) if latencies else 0.0
        p99_lat = round(sorted(latencies)[int(len(latencies) * 0.99)], 1) if latencies else 0.0

        uptime_secs = int(time.time() - self.start_time)
        days = uptime_secs // 86400
        hours = (uptime_secs % 86400) // 3600
        mins = (uptime_secs % 3600) // 60
        secs = uptime_secs % 60
        uptime_str = f"{days}d {hours}h {mins}m {secs}s"

        error_rate = round((self.total_errors / self.total_requests * 100), 2) if self.total_requests > 0 else 0.0
        
        if error_rate > 10.0 or (self.current_rps < 5 and self.total_requests > 500):
            health = "CRITICAL / DEGRADED"
            health_color = "#e53e3e"
        elif error_rate > 2.0 or (p95_lat > 2000):
            health = "WARNING / SLOW"
            health_color = "#dd6b20"
        else:
            health = "HEALTHY"
            health_color = "#38a169"

        sys_stats = self.get_system_stats()

        return {
            "uptime": uptime_str,
            "total_requests": f"{self.total_requests:,}",
            "total_success": f"{self.total_success:,}",
            "total_errors": f"{self.total_errors:,}",
            "customers_created": f"{self.customers_created:,}",
            "customer_pool_size": pool_size,
            "error_rate": f"{error_rate}%",
            "current_rps": self.current_rps,
            "avg_latency_ms": avg_lat,
            "p95_latency_ms": p95_lat,
            "p99_latency_ms": p99_lat,
            "health": health,
            "health_color": health_color,
            "categories": self.category_counts,
            "recent_errors": list(self.recent_errors)[-8:],
            "system": sys_stats
        }

metrics = MetricsRegistry()

# =========================================================
# ASYNC TRAFFIC DISPATCHER WITH CUSTOMER LIFECYCLE
# =========================================================
class TrafficSimulator:
    def __init__(self, base_url: str, target_rps: int, auth_user: str, auth_pass: str):
        self.base_url = base_url.rstrip("/")
        self.target_rps = target_rps
        self.auth_user = auth_user
        self.auth_pass = auth_pass
        self.session: aiohttp.ClientSession = None
        self.running = True
        self.customer_id_pool = list(range(11, 18))
        self.csrf_token = ""

    async def init_session(self):
        timeout = aiohttp.ClientTimeout(total=8.0, connect=3.0)
        connector = aiohttp.TCPConnector(limit=1000, limit_per_host=1000, keepalive_timeout=75.0)
        jar = aiohttp.CookieJar(unsafe=True)
        self.session = aiohttp.ClientSession(connector=connector, timeout=timeout, cookie_jar=jar)
        await self.login_admin()

    async def login_admin(self):
        try:
            login_url = f"{self.base_url}/admin/auth/login"
            login_data = {"username": self.auth_user, "password": self.auth_pass}
            async with self.session.post(login_url, json=login_data, allow_redirects=True) as resp:
                if resp.status != 200:
                    await self.session.post(f"{self.base_url}/login", data=login_data, allow_redirects=True)
            
            # Fetch CSRF token from dashboard
            async with self.session.get(f"{self.base_url}/admin/dashboard") as resp:
                html = await resp.text()
                m = re.search(r'meta name="_csrf" content="([^"]+)"', html)
                if m:
                    self.csrf_token = m.group(1)
        except Exception:
            pass

    async def execute_request(self, category: str, method: str, path: str, data: dict = None, headers: dict = None):
        url = f"{self.base_url}{path}"
        t0 = time.time()
        try:
            req_headers = headers or {}
            if method == "GET":
                async with self.session.get(url, headers=req_headers, allow_redirects=True) as resp:
                    latency = (time.time() - t0) * 1000
                    await resp.read()
                    err = None if 200 <= resp.status < 400 else f"HTTP {resp.status} on {path}"
                    metrics.record_result(category, resp.status, latency, err)
            elif method == "POST":
                async with self.session.post(url, json=data, headers=req_headers, allow_redirects=True) as resp:
                    latency = (time.time() - t0) * 1000
                    await resp.read()
                    err = None if 200 <= resp.status < 400 else f"HTTP {resp.status} on {path}"
                    metrics.record_result(category, resp.status, latency, err)
        except Exception as ex:
            latency = (time.time() - t0) * 1000
            metrics.record_result(category, 0, latency, str(ex)[:80])

    # -----------------------------------------------------
    # NEW CUSTOMER & ORDER LIFECYCLE GENERATOR
    # -----------------------------------------------------
    async def create_new_customer_and_orders(self):
        try:
            cust_name = f"{random.choice(SAMPLE_COMPANIES)} {random.randint(100, 9999)}"
            phone = f"+964 770 {random.randint(100, 999)} {random.randint(1000, 9999)}"
            email = f"sales{random.randint(100, 9999)}@corp.iq"
            city = random.choice(SAMPLE_CITIES)
            product = random.choice(SAMPLE_PRODUCTS)

            headers = {"Content-Type": "application/json"}
            if self.csrf_token:
                headers["X-CSRF-TOKEN"] = self.csrf_token

            cust_payload = {
                "name": cust_name,
                "phoneNumber": phone,
                "email": email,
                "city": city,
                "active": True
            }

            t0 = time.time()
            async with self.session.post(f"{self.base_url}/admin/customers", json=cust_payload, headers=headers) as resp:
                latency = (time.time() - t0) * 1000
                if resp.status == 201:
                    data = await resp.json()
                    new_id = data.get("data", {}).get("id")
                    if new_id:
                        self.customer_id_pool.append(new_id)
                        metrics.customers_created += 1
                        asyncio.create_task(self.insert_order_async(new_id, product))
                    metrics.record_result("followup", resp.status, latency)
                else:
                    metrics.record_result("followup", resp.status, latency, f"Customer create: {resp.status}")
        except Exception as ex:
            metrics.record_result("followup", 0, 0, str(ex)[:80])

    async def insert_order_async(self, cust_id: int, items_summary: str):
        def _insert():
            import subprocess
            amount = round(random.uniform(450.0, 3200.0), 2)
            qty = random.randint(1, 4)
            order_num = f"ORD-{cust_id}-{random.randint(10, 99)}"
            sql = f"""
            INSERT INTO CUSTOMER_ORDERS (CUSTOMER_ID, ITEMS_SUMMARY, ORDER_DATE, ORDER_NUMBER, ORDER_STATUS, TOTAL_AMOUNT, QUANTITY)
            VALUES ({cust_id}, '{items_summary}', NOW(), '{order_num}', 'DELIVERED', {amount}, {qty});
            SET @oid = LAST_INSERT_ID();
            INSERT INTO ORDER_FOLLOWUP_CHECKS (ORDER_ID, COMPLETED, MILESTONE_NAME, STATUS, TARGET_DAYS)
            VALUES (@oid, 1, 'Last 24 Hours', 'DONE', 1),
                   (@oid, 0, 'Last 7 Days', 'PENDING', 7),
                   (@oid, 0, 'Last 30 Days', 'PENDING', 30);
            """
            subprocess.run(['mysql', '-u', 'twekl_user', '-ptwekl_pass_2026', 'twekl_db', '-e', sql], capture_output=True)
        
        loop = asyncio.get_running_loop()
        await loop.run_in_executor(None, _insert)

    # -----------------------------------------------------
    # ACTION GENERATORS ACCORDING TO USER SPECIFICATIONS
    # -----------------------------------------------------
    async def action_followup(self):
        roll = random.random()
        time_filter = random.choice(STATIC_TIME_FILTERS)
        cust_id = random.choice(self.customer_id_pool)

        if roll < 0.20:
            await self.execute_request("followup", "GET", f"/admin/customers?filter={time_filter}")
        elif roll < 0.40:
            await self.execute_request("followup", "GET", f"/admin/customers/stats?filter={time_filter}")
        elif roll < 0.60:
            await self.execute_request("followup", "GET", f"/admin/customers/followups?filter={time_filter}")
        elif roll < 0.80:
            await self.execute_request("followup", "GET", f"/admin/customers/{cust_id}/orders")
        elif roll < 0.92:
            await self.execute_request("followup", "GET", f"/admin/customers/{cust_id}/feedbacks")
        else:
            await self.execute_request("followup", "GET", "/admin/followups")

    async def action_dashboard(self):
        await self.execute_request("dashboard", "GET", "/admin/dashboard")

    async def action_reports(self):
        roll = random.random()
        time_filter = random.choice(STATIC_TIME_FILTERS)
        if roll < 0.40:
            await self.execute_request("reports", "GET", f"/admin/reports/data?filter={time_filter}")
        elif roll < 0.75:
            await self.execute_request("reports", "GET", f"/admin/reports/satisfaction?filter={time_filter}")
        else:
            await self.execute_request("reports", "GET", "/admin/reports")

    async def action_timefilter(self):
        await self.execute_request("timefilter", "GET", "/admin/timelines")

    async def action_admin(self):
        roll = random.random()
        if roll < 0.60:
            await self.execute_request("admin", "GET", "/admin/admins")
        else:
            await self.execute_request("admin", "GET", "/admin/roles")

    async def worker_loop(self, queue: asyncio.Queue):
        while self.running:
            try:
                action_type = await queue.get()
                if action_type == "followup":
                    await self.action_followup()
                elif action_type == "dashboard":
                    await self.action_dashboard()
                elif action_type == "reports":
                    await self.action_reports()
                elif action_type == "timefilter":
                    await self.action_timefilter()
                elif action_type == "admin":
                    await self.action_admin()
                elif action_type == "new_customer":
                    await self.create_new_customer_and_orders()
            except Exception:
                pass
            finally:
                queue.task_done()

    async def generator_loop(self, queue: asyncio.Queue):
        weights = [
            ("followup", 0.58),
            ("dashboard", 0.20),
            ("reports", 0.10),
            ("timefilter", 0.05),
            ("admin", 0.05),
            ("new_customer", 0.02)  # 2% of traffic creates brand new customers with orders
        ]
        types, probs = zip(*weights)
        interval = 0.05
        batch_size = max(2, int(self.target_rps * interval))

        while self.running:
            try:
                t0 = time.time()
                choices = random.choices(types, weights=probs, k=batch_size)
                for c in choices:
                    await queue.put(c)
                
                elapsed = time.time() - t0
                sleep_time = max(0.001, interval - elapsed)
                await asyncio.sleep(sleep_time)
            except Exception:
                await asyncio.sleep(0.05)

    async def start(self):
        await self.init_session()
        queue = asyncio.Queue(maxsize=30000)

        num_workers = min(120, max(30, self.target_rps // 2))
        for _ in range(num_workers):
            asyncio.create_task(self.worker_loop(queue))
        
        asyncio.create_task(self.generator_loop(queue))

        counter = 0
        while self.running:
            await asyncio.sleep(1.0)
            metrics.tick_second()
            counter += 1
            if counter % 300 == 0:
                await self.login_admin()

# =========================================================
# EMBEDDED REAL-TIME WEB DASHBOARD SERVER (Port 9090)
# =========================================================
HTML_DASHBOARD_TEMPLATE = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Twekl Dashboard - Live Traffic & Performance Monitor</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        body { background: #0f172a; color: #f8fafc; padding: 24px; }
        .header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #334155; padding-bottom: 16px; margin-bottom: 24px; }
        .header h1 { font-size: 24px; font-weight: 700; color: #38bdf8; }
        .badge { display: inline-block; padding: 6px 14px; border-radius: 9999px; font-weight: 700; font-size: 13px; text-transform: uppercase; }
        .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin-bottom: 24px; }
        .card { background: #1e293b; border: 1px solid #334155; border-radius: 12px; padding: 20px; }
        .card-title { font-size: 12px; text-transform: uppercase; color: #94a3b8; letter-spacing: 0.05em; margin-bottom: 8px; }
        .card-val { font-size: 30px; font-weight: 800; color: #ffffff; }
        .card-sub { font-size: 12px; color: #64748b; margin-top: 4px; }
        .panel { background: #1e293b; border: 1px solid #334155; border-radius: 12px; padding: 20px; margin-bottom: 24px; }
        .panel-title { font-size: 16px; font-weight: 700; color: #e2e8f0; margin-bottom: 16px; border-bottom: 1px solid #334155; padding-bottom: 8px; }
        table { width: 100%; border-collapse: collapse; font-size: 13px; }
        th, td { text-align: left; padding: 10px; border-bottom: 1px solid #334155; }
        th { color: #94a3b8; font-weight: 600; }
        .status-pill { padding: 3px 8px; border-radius: 4px; font-size: 11px; font-weight: bold; }
        .live-dot { width: 10px; height: 10px; border-radius: 50%; background: #22c55e; display: inline-block; margin-right: 6px; animation: pulse 1.5s infinite; }
        .bar-container { background: #0f172a; border-radius: 6px; height: 8px; width: 100%; margin-top: 8px; overflow: hidden; }
        .bar-fill { height: 100%; background: #38bdf8; transition: width 0.3s; }
        @keyframes pulse { 0% { opacity: 1; transform: scale(1); } 50% { opacity: 0.4; transform: scale(1.2); } 100% { opacity: 1; transform: scale(1); } }
    </style>
</head>
<body>
    <div class="header">
        <div>
            <h1><span class="live-dot"></span> Twekl 30-Day Endurance Simulator & VPS Monitor</h1>
            <p style="color: #94a3b8; font-size: 13px; margin-top: 4px;">Live customer additions, real orders & hardware performance</p>
        </div>
        <div id="healthBadge" class="badge" style="background: #38a169;">HEALTHY</div>
    </div>

    <!-- ROW 1: SIMULATOR TRAFFIC METRICS -->
    <div class="grid">
        <div class="card">
            <div class="card-title">Live Throughput</div>
            <div class="card-val" id="rpsVal" style="color: #38bdf8;">0</div>
            <div class="card-sub">Requests per Second</div>
        </div>
        <div class="card">
            <div class="card-title">Total Requests</div>
            <div class="card-val" id="totalReq">0</div>
            <div class="card-sub" id="uptime">Uptime: 0s</div>
        </div>
        <div class="card">
            <div class="card-title">New Customers Added</div>
            <div class="card-val" id="custCreated" style="color: #22c55e;">0</div>
            <div class="card-sub" id="poolSub">Active Pool: 0 customers</div>
        </div>
        <div class="card">
            <div class="card-title">Response Latency (p95)</div>
            <div class="card-val" id="p95Val" style="color: #fbbf24;">0 ms</div>
            <div class="card-sub" id="avgLat">Avg: 0 ms | p99: 0 ms</div>
        </div>
    </div>

    <!-- ROW 2: LIVE VPS HARDWARE TELEMETRY -->
    <div class="panel">
        <div class="panel-title">Live VPS Hardware Performance</div>
        <div class="grid" style="grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); margin-bottom: 0;">
            <div style="background: #0f172a; padding: 16px; border-radius: 8px;">
                <div style="color: #94a3b8; font-size: 11px; text-transform: uppercase;">VPS CPU Load</div>
                <div style="font-size: 26px; font-weight: 800; margin-top: 4px; color: #a855f7;" id="cpuVal">0%</div>
                <div class="bar-container"><div class="bar-fill" id="cpuBar" style="background: #a855f7; width: 0%;"></div></div>
            </div>
            <div style="background: #0f172a; padding: 16px; border-radius: 8px;">
                <div style="color: #94a3b8; font-size: 11px; text-transform: uppercase;">VPS RAM Usage</div>
                <div style="font-size: 26px; font-weight: 800; margin-top: 4px; color: #3b82f6;" id="ramVal">0%</div>
                <div class="bar-container"><div class="bar-fill" id="ramBar" style="background: #3b82f6; width: 0%;"></div></div>
            </div>
            <div style="background: #0f172a; padding: 16px; border-radius: 8px;">
                <div style="color: #94a3b8; font-size: 11px; text-transform: uppercase;">RAM Memory Detail</div>
                <div style="font-size: 20px; font-weight: 700; margin-top: 6px; color: #e2e8f0;" id="ramDetail">0 GB / 0 GB</div>
                <div style="color: #64748b; font-size: 11px; margin-top: 4px;">Used vs Total Capacity</div>
            </div>
            <div style="background: #0f172a; padding: 16px; border-radius: 8px;">
                <div style="color: #94a3b8; font-size: 11px; text-transform: uppercase;">Tomcat 10.1 Engine</div>
                <div style="font-size: 20px; font-weight: 700; margin-top: 6px; color: #22c55e;" id="tomcatMem">Active</div>
                <div style="color: #64748b; font-size: 11px; margin-top: 4px;">Twekl Java Process RSS</div>
            </div>
        </div>
    </div>

    <!-- ROW 3: TRAFFIC DISTRIBUTION -->
    <div class="panel">
        <div class="panel-title">Traffic Distribution Breakdown</div>
        <div class="grid" style="grid-template-columns: repeat(5, 1fr); margin-bottom: 0;">
            <div style="background: #0f172a; padding: 12px; border-radius: 8px;">
                <div style="color: #94a3b8; font-size: 11px;">Follow-ups (60%)</div>
                <div style="font-size: 18px; font-weight: 700; margin-top: 4px;" id="cntFollowup">0</div>
            </div>
            <div style="background: #0f172a; padding: 12px; border-radius: 8px;">
                <div style="color: #94a3b8; font-size: 11px;">Dashboard SSR (20%)</div>
                <div style="font-size: 18px; font-weight: 700; margin-top: 4px;" id="cntDashboard">0</div>
            </div>
            <div style="background: #0f172a; padding: 12px; border-radius: 8px;">
                <div style="color: #94a3b8; font-size: 11px;">Reports Tabs (10%)</div>
                <div style="font-size: 18px; font-weight: 700; margin-top: 4px;" id="cntReports">0</div>
            </div>
            <div style="background: #0f172a; padding: 12px; border-radius: 8px;">
                <div style="color: #94a3b8; font-size: 11px;">Static Time Filters (5%)</div>
                <div style="font-size: 18px; font-weight: 700; margin-top: 4px;" id="cntTimefilter">0</div>
            </div>
            <div style="background: #0f172a; padding: 12px; border-radius: 8px;">
                <div style="color: #94a3b8; font-size: 11px;">Staff & Roles (5%)</div>
                <div style="font-size: 18px; font-weight: 700; margin-top: 4px;" id="cntAdmin">0</div>
            </div>
        </div>
    </div>

    <!-- ROW 4: ERROR LOG -->
    <div class="panel">
        <div class="panel-title">Recent Server Responses & Dropouts</div>
        <table>
            <thead>
                <tr>
                    <th>Time (UTC)</th>
                    <th>Category</th>
                    <th>Status</th>
                    <th>Details</th>
                </tr>
            </thead>
            <tbody id="errorLogBody">
                <tr><td colspan="4" style="color: #22c55e; text-align: center;">No errors. 100% successful responses.</td></tr>
            </tbody>
        </table>
    </div>

    <script>
        async function fetchMetrics() {
            try {
                const res = await fetch('/api/metrics');
                const d = await res.json();
                
                document.getElementById('rpsVal').textContent = d.current_rps.toLocaleString();
                document.getElementById('totalReq').textContent = d.total_requests;
                document.getElementById('uptime').textContent = 'Uptime: ' + d.uptime;
                document.getElementById('custCreated').textContent = d.customers_created;
                document.getElementById('poolSub').textContent = 'Active Pool: ' + d.customer_pool_size + ' customers';
                document.getElementById('p95Val').textContent = d.p95_latency_ms + ' ms';
                document.getElementById('avgLat').textContent = 'Avg: ' + d.avg_latency_ms + ' ms | p99: ' + d.p99_latency_ms + ' ms';
                
                const badge = document.getElementById('healthBadge');
                badge.textContent = d.health;
                badge.style.background = d.health_color;

                if (d.system) {
                    document.getElementById('cpuVal').textContent = d.system.cpu_percent + '%';
                    document.getElementById('cpuBar').style.width = Math.min(100, d.system.cpu_percent) + '%';
                    document.getElementById('ramVal').textContent = d.system.ram_percent + '%';
                    document.getElementById('ramBar').style.width = Math.min(100, d.system.ram_percent) + '%';
                    document.getElementById('ramDetail').textContent = d.system.ram_used_gb + ' GB / ' + d.system.ram_total_gb + ' GB';
                    document.getElementById('tomcatMem').textContent = d.system.tomcat_memory;
                }

                document.getElementById('cntFollowup').textContent = (d.categories.followup || 0).toLocaleString();
                document.getElementById('cntDashboard').textContent = (d.categories.dashboard || 0).toLocaleString();
                document.getElementById('cntReports').textContent = (d.categories.reports || 0).toLocaleString();
                document.getElementById('cntTimefilter').textContent = (d.categories.timefilter || 0).toLocaleString();
                document.getElementById('cntAdmin').textContent = (d.categories.admin || 0).toLocaleString();

                const tbody = document.getElementById('errorLogBody');
                if (d.recent_errors && d.recent_errors.length > 0) {
                    tbody.innerHTML = d.recent_errors.map(e => `
                        <tr>
                            <td>${e.timestamp}</td>
                            <td>${e.category}</td>
                            <td><span class="status-pill" style="background: #991b1b;">${e.status}</span></td>
                            <td style="color: #f87171;">${e.message}</td>
                        </tr>
                    `).join('');
                } else {
                    tbody.innerHTML = '<tr><td colspan="4" style="color: #22c55e; text-align: center;">No errors. 100% successful responses.</td></tr>';
                }
            } catch (err) {}
        }
        setInterval(fetchMetrics, 1000);
        fetchMetrics();
    </script>
</body>
</html>
"""

simulator_instance = None

async def handle_dashboard_page(request):
    return web.Response(text=HTML_DASHBOARD_TEMPLATE, content_type="text/html")

async def handle_metrics_api(request):
    pool_size = len(simulator_instance.customer_id_pool) if simulator_instance else 0
    return web.json_response(metrics.get_summary(pool_size))

async def run_web_dashboard(port: int):
    app = web.Application()
    app.router.add_get("/", handle_dashboard_page)
    app.router.add_get("/api/metrics", handle_metrics_api)
    runner = web.AppRunner(app)
    await runner.setup()
    site = web.TCPSite(runner, "0.0.0.0", port)
    await site.start()

# =========================================================
# MAIN ENTRYPOINT
# =========================================================
def main():
    global simulator_instance
    parser = argparse.ArgumentParser(description="Twekl Dashboard 24/7 Traffic Simulator")
    parser.add_argument("--host", default="http://localhost:8080", help="Base URL of Twekl Dashboard application")
    parser.add_argument("--rate", type=int, default=35, help="Target Requests Per Second (RPS)")
    parser.add_argument("--port", type=int, default=9090, help="Port for live status web dashboard")
    parser.add_argument("--user", default="twekl_super_admin", help="Admin username")
    parser.add_argument("--password", default="Super@2026", help="Admin password")
    args = parser.parse_args()

    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)

    simulator_instance = TrafficSimulator(base_url=args.host, target_rps=args.rate, auth_user=args.user, auth_pass=args.password)
    
    loop.create_task(run_web_dashboard(args.port))
    loop.create_task(simulator_instance.start())

    try:
        loop.run_forever()
    except KeyboardInterrupt:
        pass
    finally:
        loop.close()

if __name__ == "__main__":
    main()
