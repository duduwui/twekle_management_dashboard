#!/usr/bin/env python3
"""
TWEKL DASHBOARD - Enterprise Traffic & Load Simulator
24/7 Endurance & Stress Testing Suite (Target: Up to 2,000 req/s)
Includes Built-In Live Real-Time Web Dashboard on Port 9090.
"""

import asyncio
import aiohttp
from aiohttp import web
import argparse
import random
import time
import math
from datetime import datetime, timezone
from collections import deque

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

SAMPLE_PRODUCTS = [
    "Standard Package",
    "Premium ERP Tier",
    "Enterprise Cloud Sync",
    "Custom Integration",
    "Annual Support SLA",
    "Data Migration Add-on"
]

SAMPLE_NAMES = [
    "Ahmed Al-Rashid", "Fatima Mansour", "Omar Farooq", "Layla Al-Khatib", 
    "Khaled Al-Salem", "Noura Al-Hassan", "Yousef Ibrahim", "Sara Al-Otaibi",
    "Mustafa Kamel", "Reem Al-Zahrani", "Tariq Aziz", "Maha Al-Harbi"
]

# =========================================================
# REAL-TIME METRICS REGISTRY
# =========================================================
class MetricsRegistry:
    def __init__(self):
        self.start_time = time.time()
        self.total_requests = 0
        self.total_success = 0
        self.total_errors = 0
        self.recent_latencies = deque(maxlen=5000)
        self.recent_errors = deque(maxlen=50)
        self.rps_history = deque(maxlen=60)
        
        # Breakdown counters
        self.category_counts = {
            "followup": 0,
            "dashboard": 0,
            "reports": 0,
            "timefilter": 0,
            "admin": 0
        }
        
        # Transient window counters for per-second RPS calculation
        self.window_requests = 0
        self.current_rps = 0
        self.last_window_time = time.time()

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

    def get_summary(self):
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
        
        # Server Health Indicator
        if error_rate > 10.0 or (self.current_rps < 10 and self.total_requests > 500):
            health = "CRITICAL / DEGRADED"
            health_color = "#e53e3e"
        elif error_rate > 2.0 or (p95_lat > 2000):
            health = "WARNING / SLOW"
            health_color = "#dd6b20"
        else:
            health = "HEALTHY"
            health_color = "#38a169"

        return {
            "uptime": uptime_str,
            "total_requests": f"{self.total_requests:,}",
            "total_success": f"{self.total_success:,}",
            "total_errors": f"{self.total_errors:,}",
            "error_rate": f"{error_rate}%",
            "current_rps": self.current_rps,
            "avg_latency_ms": avg_lat,
            "p95_latency_ms": p95_lat,
            "p99_latency_ms": p99_lat,
            "health": health,
            "health_color": health_color,
            "categories": self.category_counts,
            "recent_errors": list(self.recent_errors)[-8:]
        }

metrics = MetricsRegistry()

# =========================================================
# ASYNC TRAFFIC DISPATCHER
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

    async def init_session(self):
        timeout = aiohttp.ClientTimeout(total=5.0, connect=2.0)
        connector = aiohttp.TCPConnector(limit=1000, limit_per_host=1000, keepalive_timeout=60.0)
        jar = aiohttp.CookieJar(unsafe=True)
        self.session = aiohttp.ClientSession(connector=connector, timeout=timeout, cookie_jar=jar)
        await self.login_admin()

    async def login_admin(self):
        try:
            login_url = f"{self.base_url}/admin/auth/login"
            login_data = {"username": self.auth_user, "password": self.auth_pass}
            async with self.session.post(login_url, json=login_data, allow_redirects=True) as resp:
                if resp.status != 200:
                    # Form login fallback
                    await self.session.post(f"{self.base_url}/login", data=login_data, allow_redirects=True)
        except Exception:
            pass

    async def execute_request(self, category: str, method: str, path: str, data: dict = None):
        url = f"{self.base_url}{path}"
        t0 = time.time()
        try:
            if method == "GET":
                async with self.session.get(url, allow_redirects=True) as resp:
                    latency = (time.time() - t0) * 1000
                    await resp.read()
                    err = None if 200 <= resp.status < 400 else f"HTTP {resp.status} on {path}"
                    metrics.record_result(category, resp.status, latency, err)
            elif method == "POST":
                async with self.session.post(url, data=data, allow_redirects=True) as resp:
                    latency = (time.time() - t0) * 1000
                    await resp.read()
                    err = None if 200 <= resp.status < 400 else f"HTTP {resp.status} on {path}"
                    metrics.record_result(category, resp.status, latency, err)
        except Exception as ex:
            latency = (time.time() - t0) * 1000
            metrics.record_result(category, 0, latency, str(ex)[:80])

    # -----------------------------------------------------
    # ACTION GENERATORS ACCORDING TO USER SPECIFICATIONS
    # -----------------------------------------------------
    async def action_followup(self):
        roll = random.random()
        time_filter = random.choice(STATIC_TIME_FILTERS)
        cust_id = random.choice(self.customer_id_pool)

        if roll < 0.25:
            # Query customer directory with static 6-type time filter
            await self.execute_request("followup", "GET", f"/admin/customers?filter={time_filter}")
        elif roll < 0.45:
            # Query customer follow-up metrics with static filter
            await self.execute_request("followup", "GET", f"/admin/customers/stats?filter={time_filter}")
        elif roll < 0.65:
            # Check overdue / active follow-up list with static filter
            await self.execute_request("followup", "GET", f"/admin/customers/followups?filter={time_filter}")
        elif roll < 0.85:
            # View customer order checks & feedback history
            await self.execute_request("followup", "GET", f"/admin/customers/{cust_id}/orders")
        elif roll < 0.95:
            await self.execute_request("followup", "GET", f"/admin/customers/{cust_id}/feedbacks")
        else:
            # SSR Follow-up workspace page
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
        # Query static timeline presets SSR page
        await self.execute_request("timefilter", "GET", "/admin/timelines")

    async def action_admin(self):
        roll = random.random()
        if roll < 0.60:
            await self.execute_request("admin", "GET", "/admin/admins")
        else:
            await self.execute_request("admin", "GET", "/admin/roles")

    async def worker_loop(self, queue: asyncio.Queue):
        while self.running:
            action_type = await queue.get()
            try:
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
            except Exception:
                pass
            finally:
                queue.task_done()

    async def generator_loop(self, queue: asyncio.Queue):
        weights = [
            ("followup", 0.60),
            ("dashboard", 0.20),
            ("reports", 0.10),
            ("timefilter", 0.05),
            ("admin", 0.05)
        ]
        types, probs = zip(*weights)

        interval = 0.05  # dispatch batches every 50ms
        batch_size = int(self.target_rps * interval)

        while self.running:
            t0 = time.time()
            choices = random.choices(types, weights=probs, k=batch_size)
            for c in choices:
                queue.put_nowait(c)
            
            elapsed = time.time() - t0
            sleep_time = max(0.0, interval - elapsed)
            await asyncio.sleep(sleep_time)

    async def start(self):
        await self.init_session()
        queue = asyncio.Queue(maxsize=10000)

        # Scale worker pool dynamically according to target RPS
        num_workers = min(100, max(20, self.target_rps // 20))
        workers = [asyncio.create_task(self.worker_loop(queue)) for _ in range(num_workers)]
        generator = asyncio.create_task(self.generator_loop(queue))

        # Background ticker for RPS calculation
        while self.running:
            await asyncio.sleep(1.0)
            metrics.tick_second()

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
        .card-val { font-size: 32px; font-weight: 800; color: #ffffff; }
        .card-sub { font-size: 12px; color: #64748b; margin-top: 4px; }
        .panel { background: #1e293b; border: 1px solid #334155; border-radius: 12px; padding: 20px; margin-bottom: 24px; }
        .panel-title { font-size: 16px; font-weight: 700; color: #e2e8f0; margin-bottom: 16px; border-bottom: 1px solid #334155; padding-bottom: 8px; }
        table { width: 100%; border-collapse: collapse; font-size: 13px; }
        th, td { text-align: left; padding: 10px; border-bottom: 1px solid #334155; }
        th { color: #94a3b8; font-weight: 600; }
        .status-pill { padding: 3px 8px; border-radius: 4px; font-size: 11px; font-weight: bold; }
        .live-dot { width: 10px; height: 10px; border-radius: 50%; background: #22c55e; display: inline-block; margin-right: 6px; animation: pulse 1.5s infinite; }
        @keyframes pulse { 0% { opacity: 1; transform: scale(1); } 50% { opacity: 0.4; transform: scale(1.2); } 100% { opacity: 1; transform: scale(1); } }
    </style>
</head>
<body>
    <div class="header">
        <div>
            <h1><span class="live-dot"></span> Twekl 30-Day Endurance Simulator</h1>
            <p style="color: #94a3b8; font-size: 13px; margin-top: 4px;">Real-time load test & VPS stability monitoring</p>
        </div>
        <div id="healthBadge" class="badge" style="background: #38a169;">HEALTHY</div>
    </div>

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
            <div class="card-title">Success / Error Rate</div>
            <div class="card-val" id="errRate" style="color: #4ade80;">0.0%</div>
            <div class="card-sub" id="errCounts">0 errors / 0 success</div>
        </div>
        <div class="card">
            <div class="card-title">Response Latency (p95)</div>
            <div class="card-val" id="p95Val" style="color: #fbbf24;">0 ms</div>
            <div class="card-sub" id="avgLat">Avg: 0 ms | p99: 0 ms</div>
        </div>
    </div>

    <div class="panel">
        <div class="panel-title">Traffic Distribution Breakdown</div>
        <div class="grid" style="grid-template-columns: repeat(5, 1fr); margin-bottom: 0;">
            <div style="background: #0f172a; padding: 12px; border-radius: 8px;">
                <div style="color: #94a3b8; font-size: 11px;">Follow-ups & Customers (60%)</div>
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
                <tr><td colspan="4" style="color: #64748b; text-align: center;">No errors reported yet. System operating smoothly.</td></tr>
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
                document.getElementById('errRate').textContent = d.error_rate;
                document.getElementById('errCounts').textContent = d.total_errors + ' errors / ' + d.total_success + ' ok';
                document.getElementById('p95Val').textContent = d.p95_latency_ms + ' ms';
                document.getElementById('avgLat').textContent = 'Avg: ' + d.avg_latency_ms + ' ms | p99: ' + d.p99_latency_ms + ' ms';
                
                const badge = document.getElementById('healthBadge');
                badge.textContent = d.health;
                badge.style.background = d.health_color;

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
                }
            } catch (err) {}
        }
        setInterval(fetchMetrics, 1000);
        fetchMetrics();
    </script>
</body>
</html>
"""

async def handle_dashboard_page(request):
    return web.Response(text=HTML_DASHBOARD_TEMPLATE, content_type="text/html")

async def handle_metrics_api(request):
    return web.json_response(metrics.get_summary())

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
    parser = argparse.ArgumentParser(description="Twekl Dashboard 24/7 Traffic Simulator")
    parser.add_argument("--host", default="http://localhost:8080", help="Base URL of Twekl Dashboard application")
    parser.add_argument("--rate", type=int, default=2000, help="Target Requests Per Second (RPS)")
    parser.add_argument("--port", type=int, default=9090, help="Port for live status web dashboard")
    parser.add_argument("--user", default="twekl_super_admin", help="Admin username")
    parser.add_argument("--password", default="Super@2026", help="Admin password")
    args = parser.parse_args()

    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)

    print("=" * 60)
    print("  TWEKL DASHBOARD - 30-DAY ENDURANCE TRAFFIC SIMULATOR")
    print(f"  Target Server:   {args.host}")
    print(f"  Target Load:     {args.rate} Requests / Second")
    print(f"  Live Dashboard:  http://0.0.0.0:{args.port}")
    print("=" * 60)

    sim = TrafficSimulator(base_url=args.host, target_rps=args.rate, auth_user=args.user, auth_pass=args.password)
    
    loop.create_task(run_web_dashboard(args.port))
    loop.create_task(sim.start())

    try:
        loop.run_forever()
    except KeyboardInterrupt:
        print("\nStopping traffic simulator safely...")
    finally:
        loop.close()

if __name__ == "__main__":
    main()
