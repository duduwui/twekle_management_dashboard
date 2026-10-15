#!/usr/bin/env python3
"""
Twekl Dashboard - 24/7 Endurance Load Simulator & Live VPS Performance Suite
Features:
- Role-based staff activity dispatch across 5 distinct staff admins (Sara, Ahmed, Omar, Kawa, SuperAdmin)
- Automated eCommerce / external API customer and order intake (independent of staff admins)
- Live interactive charts (Throughput & Latency, CPU & Memory Breakdown, Role Distribution)
- Direct Linux kernel telemetry via psutil (Isolated Twekl Java CPU vs Total Host CPU)
- 24/7 endurance runner with automatic re-authentication and zero memory leaks
"""

import asyncio
import aiohttp
from aiohttp import web
import time
import random
import re
import os
import sys
import argparse
from collections import deque
from datetime import datetime, timezone

try:
    import psutil
except ImportError:
    psutil = None

try:
    import pymysql
except ImportError:
    pymysql = None

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
CHART_JS_PATH = os.path.join(SCRIPT_DIR, "chart.js")

# =========================================================
# STAFF ADMIN ACCOUNTS & ROLE DEFINITIONS
# =========================================================
STAFF_ACCOUNTS = [
    {
        "username": "twekl_super_admin",
        "name": "Super Administrator",
        "role": "Super Admin",
        "role_badge": "#ef4444",
        "tasks": ["dashboard", "admin_mgmt", "role_audit"]
    },
    {
        "username": "admin_sara",
        "name": "Sara Al-Hashimi",
        "role": "Customer Success Lead",
        "role_badge": "#ec4899",
        "tasks": ["followups_queue", "feedback_review", "submit_notes_ratings"]
    },
    {
        "username": "admin_ahmed",
        "name": "Ahmed Al-Bayati",
        "role": "Sales Representative",
        "role_badge": "#3b82f6",
        "tasks": ["customer_pipeline", "sales_stats", "customer_orders_view"]
    },
    {
        "username": "admin_omar",
        "name": "Omar Farooq",
        "role": "Product Specialist",
        "role_badge": "#10b981",
        "tasks": ["order_inspection", "feedback_analysis", "product_metrics"]
    },
    {
        "username": "admin_kawa",
        "name": "Kawa Rostam",
        "role": "Systems / Data Analyst",
        "role_badge": "#8b5cf6",
        "tasks": ["analytics_reports", "timeline_filters", "directory_stats"]
    }
]

FOLLOWUP_NOTES = [
    "Customer confirmed receipt of POS terminal. Requested installation guide for counter staff.",
    "Hardware installed successfully. Customer verified barcode scanner operation.",
    "Periodic checkup: Customer satisfied with response speed, requested extra receipt paper rolls.",
    "Customer reported smooth shift transitions today. Feedback noted as highly positive.",
    "Follow-up call completed: Manager confirmed all 3 checkout stations operating without delay.",
    "Routine follow-up: Customer inquired about upcoming multi-branch reporting features.",
    "Delivery verified. Customer completed onboarding questionnaire with excellent satisfaction."
]

SAMPLE_COMPANIES = [
    "Al-Rafidain Hypermarket", "Baghdad Central Mart", "Tigris Tech Solutions",
    "Erbil Retail Hub", "Babylon Electronics", "Basra Commercial Supplies",
    "Nineveh Wholesale Market", "Sulaymaniyah POS Group", "Al-Mansoor Express",
    "Karkh Digital Stores", "Dijlah Superstore", "Mesopotamia Traders"
]

SAMPLE_CITIES = ["Baghdad", "Erbil", "Basra", "Sulaymaniyah", "Najaf", "Duhok", "Karbala", "Kirkuk"]

SAMPLE_PRODUCTS = [
    "Enterprise POS Terminal + Laser Barcode Scanner Bundle",
    "Dual-Screen Cloud POS Terminal + Thermal Printer",
    "Heavy-Duty Cash Drawer & USB Barcode Reader",
    "Touchscreen POS Workstation + 80mm Receipt Printer",
    "Retail Starter Kit: POS Terminal + Thermal Paper Box",
    "Advanced All-in-One POS System + Magnetic Stripe Reader"
]

TIME_FILTERS = ["TODAY", "THIS_WEEK", "THIS_MONTH", "LAST_30_DAYS", "ALL"]

# =========================================================
# GLOBAL METRICS & TELEMETRY REGISTRY
# =========================================================
class MetricsRegistry:
    def __init__(self):
        self.start_time = time.time()
        self.total_requests = 0
        self.total_success = 0
        self.total_errors = 0
        self.automated_customers_created = 0
        self.automated_orders_created = 0
        self.staff_followups_completed = 0
        
        self.current_rps = 0
        self.sec_requests = 0
        self.sec_errors = 0

        self.recent_latencies = deque(maxlen=200)
        self.recent_errors = deque(maxlen=20)

        # Per-admin stats
        self.admin_stats = {
            acc["username"]: {
                "name": acc["name"],
                "role": acc["role"],
                "role_badge": acc["role_badge"],
                "requests": 0,
                "last_action": "Ready",
                "last_timestamp": "-"
            }
            for acc in STAFF_ACCOUNTS
        }

        # Categories
        self.categories = {
            "api_intake": 0,
            "followup": 0,
            "pipeline": 0,
            "orders": 0,
            "reports": 0,
            "admin": 0
        }

        # Time-series history for charts (last 45 points)
        self.history_max = 45
        self.history_timestamps = deque(maxlen=self.history_max)
        self.history_rps = deque(maxlen=self.history_max)
        self.history_latency = deque(maxlen=self.history_max)
        self.history_twekl_cpu = deque(maxlen=self.history_max)
        self.history_host_cpu = deque(maxlen=self.history_max)
        self.history_ram = deque(maxlen=self.history_max)

        # Cached psutil process handle
        self.tomcat_proc = None
        self._find_tomcat_proc()

    def _find_tomcat_proc(self):
        if not psutil:
            return
        try:
            for p in psutil.process_iter(['cmdline']):
                cmd = " ".join(p.info['cmdline'] or [])
                if 'TomcatServer' in cmd:
                    self.tomcat_proc = p
                    self.tomcat_proc.cpu_percent(interval=None)
                    break
        except Exception:
            self.tomcat_proc = None

    def record_admin_activity(self, username: str, category: str, action_desc: str, status: int, latency_ms: float, error: str = None):
        self.total_requests += 1
        self.sec_requests += 1
        self.recent_latencies.append(latency_ms)

        if category in self.categories:
            self.categories[category] += 1

        if username in self.admin_stats:
            adm = self.admin_stats[username]
            adm["requests"] += 1
            adm["last_action"] = action_desc
            adm["last_timestamp"] = datetime.now(timezone.utc).strftime("%H:%M:%S")

        if 200 <= status < 400:
            self.total_success += 1
        else:
            self.total_errors += 1
            self.sec_errors += 1
            if error or status >= 400:
                self.recent_errors.append({
                    "timestamp": datetime.now(timezone.utc).strftime("%H:%M:%S"),
                    "user": username,
                    "action": action_desc,
                    "status": status,
                    "message": error or f"HTTP {status}"
                })

    def record_api_intake(self, status: int, latency_ms: float, error: str = None):
        self.total_requests += 1
        self.sec_requests += 1
        self.categories["api_intake"] += 1
        self.recent_latencies.append(latency_ms)

        if 200 <= status < 400:
            self.total_success += 1
        else:
            self.total_errors += 1
            self.sec_errors += 1
            if error or status >= 400:
                self.recent_errors.append({
                    "timestamp": datetime.now(timezone.utc).strftime("%H:%M:%S"),
                    "user": "AUTOMATED_API",
                    "action": "New Customer & Order Intake",
                    "status": status,
                    "message": error or f"HTTP {status}"
                })

    def tick_second(self):
        self.current_rps = self.sec_requests
        self.sec_requests = 0
        self.sec_errors = 0

        # Collect genuine hardware telemetry once per second
        host_cpu = 0.0
        twekl_cpu = 0.0
        ram_pct = 0.0
        ram_used_gb = 0.0
        ram_total_gb = 0.0
        tomcat_rss = "Active"

        if psutil:
            try:
                host_cpu = round(psutil.cpu_percent(interval=None), 1)
                mem = psutil.virtual_memory()
                ram_pct = round(mem.percent, 1)
                ram_used_gb = round(mem.used / (1024**3), 2)
                ram_total_gb = round(mem.total / (1024**3), 2)

                if self.tomcat_proc:
                    try:
                        raw_proc_cpu = self.tomcat_proc.cpu_percent(interval=None)
                        twekl_cpu = round(raw_proc_cpu / psutil.cpu_count(), 1)
                        rss_mb = self.tomcat_proc.memory_info().rss // (1024 * 1024)
                        tomcat_rss = f"{rss_mb} MB"
                    except Exception:
                        self._find_tomcat_proc()
                else:
                    self._find_tomcat_proc()
            except Exception:
                pass

        self.latest_hardware = {
            "host_cpu_percent": host_cpu,
            "twekl_cpu_percent": twekl_cpu,
            "ram_percent": ram_pct,
            "ram_used_gb": ram_used_gb,
            "ram_total_gb": ram_total_gb,
            "tomcat_rss": tomcat_rss
        }

        latencies = list(self.recent_latencies)
        avg_lat = round(sum(latencies) / len(latencies), 1) if latencies else 0.0

        ts_str = datetime.now(timezone.utc).strftime("%H:%M:%S")
        self.history_timestamps.append(ts_str)
        self.history_rps.append(self.current_rps)
        self.history_latency.append(avg_lat)
        self.history_host_cpu.append(host_cpu)
        self.history_twekl_cpu.append(twekl_cpu)
        self.history_ram.append(ram_pct)

    def get_hardware_telemetry(self):
        if hasattr(self, 'latest_hardware'):
            return dict(self.latest_hardware)
        return {
            "host_cpu_percent": 0.0,
            "twekl_cpu_percent": 0.0,
            "ram_percent": 0.0,
            "ram_used_gb": 0.0,
            "ram_total_gb": 0.0,
            "tomcat_rss": "N/A"
        }

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
        health = "HEALTHY"
        health_color = "#22c55e"
        if error_rate > 5.0:
            health = "CRITICAL / DEGRADED"
            health_color = "#ef4444"
        elif error_rate > 1.0 or p95_lat > 2500:
            health = "WARNING / SLOW"
            health_color = "#f59e0b"

        hw = self.get_hardware_telemetry()

        return {
            "uptime": uptime_str,
            "total_requests": f"{self.total_requests:,}",
            "total_success": f"{self.total_success:,}",
            "total_errors": f"{self.total_errors:,}",
            "automated_customers_created": f"{self.automated_customers_created:,}",
            "automated_orders_created": f"{self.automated_orders_created:,}",
            "staff_followups_completed": f"{self.staff_followups_completed:,}",
            "customer_pool_size": pool_size,
            "error_rate": f"{error_rate}%",
            "current_rps": self.current_rps,
            "avg_latency_ms": avg_lat,
            "p95_latency_ms": p95_lat,
            "p99_latency_ms": p99_lat,
            "health": health,
            "health_color": health_color,
            "categories": self.categories,
            "admin_stats": list(self.admin_stats.values()),
            "recent_errors": list(self.recent_errors)[-6:],
            "system": hw,
            "charts": {
                "timestamps": list(self.history_timestamps),
                "rps": list(self.history_rps),
                "latency": list(self.history_latency),
                "host_cpu": list(self.history_host_cpu),
                "twekl_cpu": list(self.history_twekl_cpu),
                "ram": list(self.history_ram),
                "role_labels": [a["name"].split()[0] for a in STAFF_ACCOUNTS],
                "role_counts": [self.admin_stats[a["username"]]["requests"] for a in STAFF_ACCOUNTS]
            }
        }

metrics = MetricsRegistry()

# =========================================================
# ASYNC TRAFFIC & ROLE-BASED ENGINE
# =========================================================
class TrafficEngine:
    def __init__(self, base_url: str, target_rps: int):
        self.base_url = base_url.rstrip("/")
        self.target_rps = target_rps
        self.running = True
        self.admin_sessions = {}
        self.customer_id_pool = list(range(1, 20))
        self.active_check_ids = deque(maxlen=300)

    async def wait_for_backend(self):
        print("Waiting for Twekl backend to be fully online...")
        for _ in range(60):
            try:
                timeout = aiohttp.ClientTimeout(total=3.0)
                async with aiohttp.ClientSession(timeout=timeout) as s:
                    async with s.get(f"{self.base_url}/login") as r:
                        if r.status == 200:
                            print("Twekl backend is online and ready!")
                            return True
            except Exception:
                pass
            await asyncio.sleep(1.0)
        print("Twekl backend timeout, proceeding anyway...")
        return False

    async def init_sessions(self):
        await self.wait_for_backend()

        timeout = aiohttp.ClientTimeout(total=8.0, connect=3.0)
        connector = aiohttp.TCPConnector(limit=1000, limit_per_host=1000, keepalive_timeout=75.0)

        for acc in STAFF_ACCOUNTS:
            jar = aiohttp.CookieJar(unsafe=True)
            sess = aiohttp.ClientSession(connector=connector, timeout=timeout, cookie_jar=jar)
            csrf = await self.login_staff(sess, acc["username"], "Super@2026")
            self.admin_sessions[acc["username"]] = {
                "session": sess,
                "csrf_token": csrf,
                "account": acc
            }

        # Preload existing check IDs from DB
        def _load_checks():
            if pymysql:
                try:
                    conn = pymysql.connect(
                        host='127.0.0.1', user='twekl_user', password='twekl_pass_2026',
                        database='twekl_db', connect_timeout=3
                    )
                    with conn.cursor() as cur:
                        cur.execute("SELECT ID FROM ORDER_FOLLOWUP_CHECKS ORDER BY ID DESC LIMIT 200")
                        for r in cur.fetchall():
                            self.active_check_ids.append(r[0])
                        cur.execute("SELECT CUSTOMER_ID FROM CUSTOMERS ORDER BY CUSTOMER_ID DESC LIMIT 100")
                        for r in cur.fetchall():
                            self.customer_id_pool.append(r[0])
                    conn.close()
                except Exception:
                    pass

        loop = asyncio.get_running_loop()
        await loop.run_in_executor(None, _load_checks)

    async def login_staff(self, session: aiohttp.ClientSession, user: str, password: str) -> str:
        csrf_token = ""
        for attempt in range(4):
            try:
                login_url = f"{self.base_url}/admin/auth/login"
                login_data = {"username": user, "password": password}
                async with session.post(login_url, json=login_data, allow_redirects=True) as resp:
                    if resp.status != 200:
                        await session.post(f"{self.base_url}/login", data=login_data, allow_redirects=True)

                async with session.get(f"{self.base_url}/admin/dashboard") as resp:
                    html = await resp.text()
                    m = re.search(r'meta name="_csrf" content="([^"]+)"', html)
                    if m:
                        csrf_token = m.group(1)
                if csrf_token:
                    break
            except Exception:
                await asyncio.sleep(0.5)
        return csrf_token

    async def reauth_if_needed(self, username: str):
        meta = self.admin_sessions.get(username)
        if meta:
            csrf = await self.login_staff(meta["session"], username, "Super@2026")
            if csrf:
                meta["csrf_token"] = csrf

    # ---------------------------------------------------------
    # 1. AUTOMATED EXTERNAL API INTAKE (eCommerce Webhook / Sync)
    # Customers & orders are NOT added by admins; they arrive automatically
    # ---------------------------------------------------------
    async def automated_api_customer_intake(self):
        admin_meta = self.admin_sessions.get("twekl_super_admin")
        if not admin_meta:
            return
        
        session = admin_meta["session"]
        csrf = admin_meta.get("csrf_token", "")

        company = f"{random.choice(SAMPLE_COMPANIES)} #{random.randint(100, 9999)}"
        phone = f"+964 770 {random.randint(100, 999)} {random.randint(1000, 9999)}"
        email = f"order{random.randint(1000, 99999)}@corp.iq"
        city = random.choice(SAMPLE_CITIES)
        product = random.choice(SAMPLE_PRODUCTS)

        payload = {
            "name": company,
            "phoneNumber": phone,
            "email": email,
            "city": city,
            "active": True
        }

        headers = {"Content-Type": "application/json"}
        if csrf:
            headers["X-CSRF-TOKEN"] = csrf

        t0 = time.time()
        try:
            async with session.post(f"{self.base_url}/admin/customers", json=payload, headers=headers) as resp:
                lat = (time.time() - t0) * 1000
                if resp.status == 201:
                    data = await resp.json()
                    new_id = data.get("data", {}).get("id")
                    if new_id:
                        self.customer_id_pool.append(new_id)
                        metrics.automated_customers_created += 1
                        asyncio.create_task(self._insert_automated_order_db(new_id, product))
                    metrics.record_api_intake(resp.status, lat)
                elif resp.status in (401, 403):
                    await self.reauth_if_needed("twekl_super_admin")
                    metrics.record_api_intake(resp.status, lat, f"API Auth Refresh ({resp.status})")
                else:
                    metrics.record_api_intake(resp.status, lat, f"API Intake HTTP {resp.status}")
        except Exception as ex:
            lat = (time.time() - t0) * 1000
            metrics.record_api_intake(0, lat, str(ex)[:80])

    async def _insert_automated_order_db(self, cust_id: int, items_summary: str):
        def _db_op():
            if not pymysql:
                return
            try:
                conn = pymysql.connect(
                    host='127.0.0.1', user='twekl_user', password='twekl_pass_2026',
                    database='twekl_db', autocommit=True, connect_timeout=3
                )
                with conn.cursor() as cur:
                    amt = round(random.uniform(450.0, 3400.0), 2)
                    qty = random.randint(1, 4)
                    order_num = f"ORD-{cust_id}-{random.randint(10, 99)}"
                    cur.execute(
                        "INSERT INTO CUSTOMER_ORDERS (CUSTOMER_ID, ITEMS_SUMMARY, ORDER_DATE, ORDER_NUMBER, ORDER_STATUS, TOTAL_AMOUNT, QUANTITY) "
                        "VALUES (%s, %s, NOW(), %s, 'DELIVERED', %s, %s)",
                        (cust_id, items_summary, order_num, amt, qty)
                    )
                    oid = cur.lastrowid
                    cur.execute(
                        "INSERT INTO ORDER_FOLLOWUP_CHECKS (ORDER_ID, COMPLETED, MILESTONE_NAME, STATUS, TARGET_DAYS) "
                        "VALUES (%s, 1, 'Last 24 Hours', 'DONE', 1), (%s, 0, 'Last 7 Days', 'PENDING', 7), (%s, 0, 'Last 30 Days', 'PENDING', 30)",
                        (oid, oid, oid)
                    )
                    first_chk = cur.lastrowid
                    self.active_check_ids.extend([first_chk, first_chk + 1, first_chk + 2])
                    metrics.automated_orders_created += 1
                conn.close()
            except Exception:
                pass

        loop = asyncio.get_running_loop()
        await loop.run_in_executor(None, _db_op)

    # ---------------------------------------------------------
    # 2. ROLE SPECIFIC STAFF ACTIVITIES
    # ---------------------------------------------------------
    async def run_sara_task(self):
        """Sara (Customer Success Lead): Milestone follow-ups, feedbacks, notes & ratings"""
        meta = self.admin_sessions.get("admin_sara")
        if not meta:
            return
        sess, csrf = meta["session"], meta.get("csrf_token")
        roll = random.random()
        time_filt = random.choice(TIME_FILTERS)

        t0 = time.time()
        try:
            if roll < 0.35 and self.active_check_ids:
                chk_id = random.choice(self.active_check_ids)
                body = {
                    "notes": random.choice(FOLLOWUP_NOTES),
                    "satisfaction": random.choice(["EXCELLENT", "GOOD", "NEUTRAL"])
                }
                headers = {"Content-Type": "application/json"}
                if csrf:
                    headers["X-CSRF-TOKEN"] = csrf
                async with sess.post(f"{self.base_url}/admin/followups/{chk_id}", json=body, headers=headers) as resp:
                    lat = (time.time() - t0) * 1000
                    await resp.read()
                    if resp.status == 401:
                        await self.reauth_if_needed("admin_sara")
                    else:
                        metrics.staff_followups_completed += 1
                        metrics.record_admin_activity("admin_sara", "followup", f"Logged review on Check #{chk_id}", resp.status, lat)
            elif roll < 0.70:
                async with sess.get(f"{self.base_url}/admin/customers/followups?filter={time_filt}") as resp:
                    lat = (time.time() - t0) * 1000
                    await resp.read()
                    if resp.status == 401:
                        await self.reauth_if_needed("admin_sara")
                    else:
                        metrics.record_admin_activity("admin_sara", "followup", f"Audited follow-ups ({time_filt})", resp.status, lat)
            else:
                cid = random.choice(self.customer_id_pool) if self.customer_id_pool else 1
                async with sess.get(f"{self.base_url}/admin/customers/{cid}/feedbacks") as resp:
                    lat = (time.time() - t0) * 1000
                    await resp.read()
                    if resp.status == 401:
                        await self.reauth_if_needed("admin_sara")
                    else:
                        metrics.record_admin_activity("admin_sara", "followup", f"Reviewed feedbacks for Cust #{cid}", resp.status, lat)
        except Exception as ex:
            metrics.record_admin_activity("admin_sara", "followup", "Network exception", 0, (time.time() - t0) * 1000, str(ex)[:80])

    async def run_ahmed_task(self):
        """Ahmed (Sales Rep): Customer pipeline directory, sales stats, order history"""
        meta = self.admin_sessions.get("admin_ahmed")
        if not meta:
            return
        sess = meta["session"]
        roll = random.random()
        time_filt = random.choice(TIME_FILTERS)

        t0 = time.time()
        try:
            if roll < 0.45:
                async with sess.get(f"{self.base_url}/admin/customers?filter={time_filt}") as resp:
                    lat = (time.time() - t0) * 1000
                    await resp.read()
                    if resp.status == 401:
                        await self.reauth_if_needed("admin_ahmed")
                    else:
                        metrics.record_admin_activity("admin_ahmed", "pipeline", f"Checked customer directory ({time_filt})", resp.status, lat)
            elif roll < 0.75:
                async with sess.get(f"{self.base_url}/admin/customers/stats?filter={time_filt}") as resp:
                    lat = (time.time() - t0) * 1000
                    await resp.read()
                    if resp.status == 401:
                        await self.reauth_if_needed("admin_ahmed")
                    else:
                        metrics.record_admin_activity("admin_ahmed", "pipeline", f"Analyzed pipeline stats ({time_filt})", resp.status, lat)
            else:
                cid = random.choice(self.customer_id_pool) if self.customer_id_pool else 1
                async with sess.get(f"{self.base_url}/admin/customers/{cid}/orders") as resp:
                    lat = (time.time() - t0) * 1000
                    await resp.read()
                    if resp.status == 401:
                        await self.reauth_if_needed("admin_ahmed")
                    else:
                        metrics.record_admin_activity("admin_ahmed", "orders", f"Inspected order history (Cust #{cid})", resp.status, lat)
        except Exception as ex:
            metrics.record_admin_activity("admin_ahmed", "pipeline", "Network exception", 0, (time.time() - t0) * 1000, str(ex)[:80])

    async def run_omar_task(self):
        """Omar (Product Specialist): Order item inspections, feedback analysis, product metrics"""
        meta = self.admin_sessions.get("admin_omar")
        if not meta:
            return
        sess = meta["session"]
        roll = random.random()

        t0 = time.time()
        try:
            cid = random.choice(self.customer_id_pool) if self.customer_id_pool else 1
            if roll < 0.60:
                async with sess.get(f"{self.base_url}/admin/customers/{cid}/orders") as resp:
                    lat = (time.time() - t0) * 1000
                    await resp.read()
                    if resp.status == 401:
                        await self.reauth_if_needed("admin_omar")
                    else:
                        metrics.record_admin_activity("admin_omar", "orders", f"Inspected product package (Cust #{cid})", resp.status, lat)
            else:
                async with sess.get(f"{self.base_url}/admin/customers/{cid}/feedbacks") as resp:
                    lat = (time.time() - t0) * 1000
                    await resp.read()
                    if resp.status == 401:
                        await self.reauth_if_needed("admin_omar")
                    else:
                        metrics.record_admin_activity("admin_omar", "orders", f"Evaluated satisfaction marks (Cust #{cid})", resp.status, lat)
        except Exception as ex:
            metrics.record_admin_activity("admin_omar", "orders", "Network exception", 0, (time.time() - t0) * 1000, str(ex)[:80])

    async def run_kawa_task(self):
        """Kawa (Systems / Analyst): Analytical reports, satisfaction metrics, timelines"""
        meta = self.admin_sessions.get("admin_kawa")
        if not meta:
            return
        sess = meta["session"]
        roll = random.random()
        time_filt = random.choice(TIME_FILTERS)

        t0 = time.time()
        try:
            if roll < 0.40:
                async with sess.get(f"{self.base_url}/admin/reports/data?filter={time_filt}") as resp:
                    lat = (time.time() - t0) * 1000
                    await resp.read()
                    if resp.status == 401:
                        await self.reauth_if_needed("admin_kawa")
                    else:
                        metrics.record_admin_activity("admin_kawa", "reports", f"Generated financial report ({time_filt})", resp.status, lat)
            elif roll < 0.70:
                async with sess.get(f"{self.base_url}/admin/reports/satisfaction?filter={time_filt}") as resp:
                    lat = (time.time() - t0) * 1000
                    await resp.read()
                    if resp.status == 401:
                        await self.reauth_if_needed("admin_kawa")
                    else:
                        metrics.record_admin_activity("admin_kawa", "reports", f"Exported satisfaction analytics ({time_filt})", resp.status, lat)
            else:
                async with sess.get(f"{self.base_url}/admin/timelines") as resp:
                    lat = (time.time() - t0) * 1000
                    await resp.read()
                    if resp.status == 401:
                        await self.reauth_if_needed("admin_kawa")
                    else:
                        metrics.record_admin_activity("admin_kawa", "reports", "Rendered operational timelines", resp.status, lat)
        except Exception as ex:
            metrics.record_admin_activity("admin_kawa", "reports", "Network exception", 0, (time.time() - t0) * 1000, str(ex)[:80])

    async def run_super_admin_task(self):
        """Super Admin: Executive dashboard, admin user audits, role configurations"""
        meta = self.admin_sessions.get("twekl_super_admin")
        if not meta:
            return
        sess = meta["session"]
        roll = random.random()

        t0 = time.time()
        try:
            if roll < 0.50:
                async with sess.get(f"{self.base_url}/admin/dashboard") as resp:
                    lat = (time.time() - t0) * 1000
                    await resp.read()
                    if resp.status == 401:
                        await self.reauth_if_needed("twekl_super_admin")
                    else:
                        metrics.record_admin_activity("twekl_super_admin", "admin", "Refreshed Executive Dashboard", resp.status, lat)
            elif roll < 0.75:
                async with sess.get(f"{self.base_url}/admin/admins") as resp:
                    lat = (time.time() - t0) * 1000
                    await resp.read()
                    if resp.status == 401:
                        await self.reauth_if_needed("twekl_super_admin")
                    else:
                        metrics.record_admin_activity("twekl_super_admin", "admin", "Audited Staff Admin Directory", resp.status, lat)
            else:
                async with sess.get(f"{self.base_url}/admin/roles") as resp:
                    lat = (time.time() - t0) * 1000
                    await resp.read()
                    if resp.status == 401:
                        await self.reauth_if_needed("twekl_super_admin")
                    else:
                        metrics.record_admin_activity("twekl_super_admin", "admin", "Verified Security Permissions & Roles", resp.status, lat)
        except Exception as ex:
            metrics.record_admin_activity("twekl_super_admin", "admin", "Network exception", 0, (time.time() - t0) * 1000, str(ex)[:80])

    # ---------------------------------------------------------
    # WORKER LOOPS & SCHEDULER
    # ---------------------------------------------------------
    async def worker_loop(self, queue: asyncio.Queue):
        while self.running:
            try:
                task_name = await queue.get()
                if task_name == "api_intake":
                    await self.automated_api_customer_intake()
                elif task_name == "sara":
                    await self.run_sara_task()
                elif task_name == "ahmed":
                    await self.run_ahmed_task()
                elif task_name == "omar":
                    await self.run_omar_task()
                elif task_name == "kawa":
                    await self.run_kawa_task()
                elif task_name == "super_admin":
                    await self.run_super_admin_task()
            except Exception:
                pass
            finally:
                queue.task_done()

    async def scheduler_loop(self, queue: asyncio.Queue):
        task_types = ["sara", "ahmed", "omar", "kawa", "super_admin", "api_intake"]
        weights = [0.35, 0.25, 0.15, 0.10, 0.10, 0.05]

        interval = 0.05
        batch_size = max(2, int(self.target_rps * interval))

        while self.running:
            try:
                t0 = time.time()
                chosen = random.choices(task_types, weights=weights, k=batch_size)
                for item in chosen:
                    await queue.put(item)
                elapsed = time.time() - t0
                await asyncio.sleep(max(0.001, interval - elapsed))
            except Exception:
                await asyncio.sleep(0.05)

    async def start(self):
        await self.init_sessions()
        queue = asyncio.Queue(maxsize=20000)

        num_workers = min(80, max(25, self.target_rps))
        for _ in range(num_workers):
            asyncio.create_task(self.worker_loop(queue))

        asyncio.create_task(self.scheduler_loop(queue))

        counter = 0
        while self.running:
            await asyncio.sleep(1.0)
            metrics.tick_second()
            counter += 1
            if counter % 300 == 0:
                for username in self.admin_sessions:
                    await self.reauth_if_needed(username)

# =========================================================
# WEB DASHBOARD (HTML, CHART.JS & REAL-TIME WEBSOCKET/POLL)
# =========================================================
HTML_PAGE = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Twekl Dashboard - Endurance Suite & VPS Telemetry</title>
    <!-- Local Chart.js for instant zero-latency rendering -->
    <script src="/static/chart.js"></script>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        body { background: #0b1120; color: #f8fafc; padding: 24px; min-height: 100vh; }
        .header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #1e293b; padding-bottom: 18px; margin-bottom: 24px; }
        .header h1 { font-size: 22px; font-weight: 700; color: #38bdf8; display: flex; align-items: center; }
        .badge { display: inline-block; padding: 6px 16px; border-radius: 9999px; font-weight: 700; font-size: 13px; text-transform: uppercase; }
        .grid-4 { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin-bottom: 20px; }
        .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px; }
        .grid-3 { display: grid; grid-template-columns: 2fr 1fr; gap: 16px; margin-bottom: 20px; }
        @media (max-width: 900px) { .grid-2, .grid-3 { grid-template-columns: 1fr; } }
        .card { background: #131d31; border: 1px solid #1e293b; border-radius: 12px; padding: 18px; }
        .card-title { font-size: 11px; text-transform: uppercase; color: #94a3b8; letter-spacing: 0.05em; margin-bottom: 6px; }
        .card-val { font-size: 28px; font-weight: 800; color: #ffffff; }
        .card-sub { font-size: 12px; color: #64748b; margin-top: 4px; }
        .panel { background: #131d31; border: 1px solid #1e293b; border-radius: 12px; padding: 20px; margin-bottom: 20px; }
        .panel-title { font-size: 15px; font-weight: 700; color: #e2e8f0; margin-bottom: 14px; border-bottom: 1px solid #1e293b; padding-bottom: 8px; display: flex; justify-content: space-between; }
        table { width: 100%; border-collapse: collapse; font-size: 13px; }
        th, td { text-align: left; padding: 10px 12px; border-bottom: 1px solid #1e293b; }
        th { color: #94a3b8; font-weight: 600; font-size: 12px; text-transform: uppercase; }
        .status-pill { padding: 3px 8px; border-radius: 4px; font-size: 11px; font-weight: bold; }
        .live-dot { width: 10px; height: 10px; border-radius: 50%; background: #22c55e; display: inline-block; margin-right: 8px; animation: pulse 1.5s infinite; }
        .bar-container { background: #0b1120; border-radius: 6px; height: 8px; width: 100%; margin-top: 8px; overflow: hidden; }
        .bar-fill { height: 100%; background: #38bdf8; transition: width 0.3s; }
        @keyframes pulse { 0% { opacity: 1; transform: scale(1); } 50% { opacity: 0.4; transform: scale(1.2); } 100% { opacity: 1; transform: scale(1); } }
        .chart-box { height: 220px; position: relative; width: 100%; }
    </style>
</head>
<body>
    <div class="header">
        <div>
            <h1><span class="live-dot"></span> Twekl 30-Day Endurance Simulator & VPS Monitor</h1>
            <p style="color: #94a3b8; font-size: 13px; margin-top: 4px;">Role-Specific Staff Workflows • Automated eCommerce Intake • Real-time Hardware Telemetry</p>
        </div>
        <div id="healthBadge" class="badge" style="background: #22c55e;">HEALTHY</div>
    </div>

    <!-- ROW 1: TOP SUMMARY STATS -->
    <div class="grid-4">
        <div class="card">
            <div class="card-title">Live Throughput</div>
            <div class="card-val" id="rpsVal" style="color: #38bdf8;">0</div>
            <div class="card-sub" id="latencySub">Avg: 0 ms | p95: 0 ms</div>
        </div>
        <div class="card">
            <div class="card-title">Total Requests Handled</div>
            <div class="card-val" id="totalReq">0</div>
            <div class="card-sub" id="uptime">Uptime: 0s • Errors: 0</div>
        </div>
        <div class="card">
            <div class="card-title">Automated API Intake (eCommerce)</div>
            <div class="card-val" id="custCreated" style="color: #22c55e;">0</div>
            <div class="card-sub" id="ordersCreatedSub">Orders synced: 0 • Pool: 0</div>
        </div>
        <div class="card">
            <div class="card-title">Staff Follow-ups Completed</div>
            <div class="card-val" id="followupsCompleted" style="color: #f59e0b;">0</div>
            <div class="card-sub">Reviews, notes & ratings logged</div>
        </div>
    </div>

    <!-- ROW 2: HARDWARE TELEMETRY -->
    <div class="panel">
        <div class="panel-title">
            <span>Genuine VPS Telemetry (Isolated Twekl vs Host)</span>
            <span style="font-size: 12px; color: #64748b; font-weight: normal;">Polled via Linux /proc & psutil</span>
        </div>
        <div class="grid-4" style="margin-bottom: 0;">
            <div style="background: #0b1120; padding: 14px; border-radius: 8px;">
                <div style="color: #94a3b8; font-size: 11px; text-transform: uppercase;">Twekl Java Engine CPU</div>
                <div style="font-size: 24px; font-weight: 800; margin-top: 4px; color: #38bdf8;" id="tweklCpuVal">0%</div>
                <div class="bar-container"><div class="bar-fill" id="tweklCpuBar" style="background: #38bdf8; width: 0%;"></div></div>
            </div>
            <div style="background: #0b1120; padding: 14px; border-radius: 8px;">
                <div style="color: #94a3b8; font-size: 11px; text-transform: uppercase;">VPS Total Host CPU</div>
                <div style="font-size: 24px; font-weight: 800; margin-top: 4px; color: #a855f7;" id="hostCpuVal">0%</div>
                <div class="bar-container"><div class="bar-fill" id="hostCpuBar" style="background: #a855f7; width: 0%;"></div></div>
            </div>
            <div style="background: #0b1120; padding: 14px; border-radius: 8px;">
                <div style="color: #94a3b8; font-size: 11px; text-transform: uppercase;">VPS RAM Utilization</div>
                <div style="font-size: 24px; font-weight: 800; margin-top: 4px; color: #3b82f6;" id="ramVal">0%</div>
                <div class="bar-container"><div class="bar-fill" id="ramBar" style="background: #3b82f6; width: 0%;"></div></div>
                <div style="color: #64748b; font-size: 11px; margin-top: 6px;" id="ramDetail">0 GB / 0 GB</div>
            </div>
            <div style="background: #0b1120; padding: 14px; border-radius: 8px;">
                <div style="color: #94a3b8; font-size: 11px; text-transform: uppercase;">Tomcat 10.1 Engine (1.5GB Heap)</div>
                <div style="font-size: 22px; font-weight: 700; margin-top: 4px; color: #22c55e;" id="tomcatMem">Active</div>
                <div style="color: #64748b; font-size: 11px; margin-top: 6px;">Resident Memory (RSS)</div>
            </div>
        </div>
    </div>

    <!-- ROW 3: INTERACTIVE LIVE CHARTS -->
    <div class="grid-2">
        <div class="panel" style="margin-bottom: 0;">
            <div class="panel-title">Real-Time Throughput & Latency (Last 45s)</div>
            <div class="chart-box">
                <canvas id="trafficChart"></canvas>
            </div>
        </div>
        <div class="panel" style="margin-bottom: 0;">
            <div class="panel-title">Hardware Telemetry History (Twekl vs Host CPU)</div>
            <div class="chart-box">
                <canvas id="hardwareChart"></canvas>
            </div>
        </div>
    </div>

    <!-- ROW 4: STAFF ADMIN ROSTER & ACTIVITIES -->
    <div class="grid-3" style="margin-top: 20px;">
        <div class="panel" style="margin-bottom: 0;">
            <div class="panel-title">5 Distinct Staff Admins (Role-Specific Workflows)</div>
            <table>
                <thead>
                    <tr>
                        <th>Staff Member</th>
                        <th>Assigned Role</th>
                        <th>Requests</th>
                        <th>Last Action Performed</th>
                        <th>Time</th>
                    </tr>
                </thead>
                <tbody id="staffTableBody">
                    <tr><td colspan="5" style="color: #64748b; text-align: center;">Loading staff sessions...</td></tr>
                </tbody>
            </table>
        </div>
        <div class="panel" style="margin-bottom: 0;">
            <div class="panel-title">Role Activity Distribution</div>
            <div class="chart-box" style="height: 180px;">
                <canvas id="roleChart"></canvas>
            </div>
        </div>
    </div>

    <!-- ROW 5: RECENT RESPONSES / ERRORS -->
    <div class="panel" style="margin-top: 20px;">
        <div class="panel-title">Recent Server Responses & Dropouts</div>
        <table>
            <thead>
                <tr>
                    <th>Time</th>
                    <th>User / Stream</th>
                    <th>Action</th>
                    <th>Status</th>
                    <th>Details</th>
                </tr>
            </thead>
            <tbody id="errorLogBody">
                <tr><td colspan="5" style="color: #22c55e; text-align: center;">No errors. 100% successful responses.</td></tr>
            </tbody>
        </table>
    </div>

    <script>
        let trafficChart = null;
        let hardwareChart = null;
        let roleChart = null;

        function initCharts() {
            if (typeof Chart === 'undefined') {
                setTimeout(initCharts, 150);
                return;
            }

            const trafficCtx = document.getElementById('trafficChart').getContext('2d');
            trafficChart = new Chart(trafficCtx, {
                type: 'line',
                data: {
                    labels: [],
                    datasets: [
                        {
                            label: 'Throughput (RPS)',
                            borderColor: '#38bdf8',
                            backgroundColor: 'rgba(56, 189, 248, 0.1)',
                            borderWidth: 2,
                            fill: true,
                            data: [],
                            yAxisID: 'y'
                        },
                        {
                            label: 'Latency (ms)',
                            borderColor: '#f59e0b',
                            borderWidth: 2,
                            borderDash: [4, 4],
                            fill: false,
                            data: [],
                            yAxisID: 'y1'
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    animation: false,
                    scales: {
                        x: { display: false },
                        y: { type: 'linear', position: 'left', beginAtZero: true, grid: { color: '#1e293b' } },
                        y1: { type: 'linear', position: 'right', beginAtZero: true, grid: { drawOnChartArea: false } }
                    },
                    plugins: { legend: { labels: { color: '#94a3b8', boxWidth: 12 } } }
                }
            });

            const hwCtx = document.getElementById('hardwareChart').getContext('2d');
            hardwareChart = new Chart(hwCtx, {
                type: 'line',
                data: {
                    labels: [],
                    datasets: [
                        {
                            label: 'Twekl Engine CPU %',
                            borderColor: '#38bdf8',
                            backgroundColor: 'rgba(56, 189, 248, 0.15)',
                            borderWidth: 2,
                            fill: true,
                            data: []
                        },
                        {
                            label: 'Host Total CPU %',
                            borderColor: '#a855f7',
                            borderWidth: 2,
                            borderDash: [3, 3],
                            fill: false,
                            data: []
                        },
                        {
                            label: 'RAM %',
                            borderColor: '#3b82f6',
                            borderWidth: 1.5,
                            fill: false,
                            data: []
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    animation: false,
                    scales: {
                        x: { display: false },
                        y: { min: 0, max: 100, grid: { color: '#1e293b' } }
                    },
                    plugins: { legend: { labels: { color: '#94a3b8', boxWidth: 12 } } }
                }
            });

            const roleCtx = document.getElementById('roleChart').getContext('2d');
            roleChart = new Chart(roleCtx, {
                type: 'doughnut',
                data: {
                    labels: ['SuperAdmin', 'Sara', 'Ahmed', 'Omar', 'Kawa'],
                    datasets: [{
                        data: [1, 1, 1, 1, 1],
                        backgroundColor: ['#ef4444', '#ec4899', '#3b82f6', '#10b981', '#8b5cf6'],
                        borderWidth: 0
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    animation: false,
                    plugins: {
                        legend: { position: 'right', labels: { color: '#94a3b8', boxWidth: 10, font: { size: 11 } } }
                    }
                }
            });
        }

        async function fetchMetrics() {
            try {
                const res = await fetch('/api/metrics');
                if (!res.ok) return;
                const d = await res.json();

                // Top stats
                document.getElementById('rpsVal').textContent = d.current_rps.toLocaleString();
                document.getElementById('latencySub').textContent = `Avg: ${d.avg_latency_ms} ms | p95: ${d.p95_latency_ms} ms`;
                document.getElementById('totalReq').textContent = d.total_requests;
                document.getElementById('uptime').textContent = `Uptime: ${d.uptime} • Errors: ${d.total_errors}`;
                document.getElementById('custCreated').textContent = d.automated_customers_created;
                document.getElementById('ordersCreatedSub').textContent = `Orders: ${d.automated_orders_created} • Pool: ${d.customer_pool_size}`;
                document.getElementById('followupsCompleted').textContent = d.staff_followups_completed;

                // Health status badge
                const badge = document.getElementById('healthBadge');
                badge.textContent = d.health;
                badge.style.background = d.health_color;

                // Genuine hardware telemetry
                if (d.system) {
                    document.getElementById('tweklCpuVal').textContent = d.system.twekl_cpu_percent + '%';
                    document.getElementById('tweklCpuBar').style.width = Math.min(100, d.system.twekl_cpu_percent) + '%';
                    document.getElementById('hostCpuVal').textContent = d.system.host_cpu_percent + '%';
                    document.getElementById('hostCpuBar').style.width = Math.min(100, d.system.host_cpu_percent) + '%';
                    document.getElementById('ramVal').textContent = d.system.ram_percent + '%';
                    document.getElementById('ramBar').style.width = Math.min(100, d.system.ram_percent) + '%';
                    document.getElementById('ramDetail').textContent = `${d.system.ram_used_gb} GB / ${d.system.ram_total_gb} GB`;
                    document.getElementById('tomcatMem').textContent = d.system.tomcat_rss;
                }

                // Staff roster table
                if (d.admin_stats && d.admin_stats.length > 0) {
                    const staffHtml = d.admin_stats.map(adm => `
                        <tr>
                            <td style="font-weight: 600;">${adm.name}</td>
                            <td><span class="status-pill" style="background: ${adm.role_badge}22; color: ${adm.role_badge}; border: 1px solid ${adm.role_badge}55;">${adm.role}</span></td>
                            <td style="font-weight: 700;">${adm.requests.toLocaleString()}</td>
                            <td style="color: #cbd5e1;">${adm.last_action}</td>
                            <td style="color: #64748b; font-size: 11px;">${adm.last_timestamp}</td>
                        </tr>
                    `).join('');
                    document.getElementById('staffTableBody').innerHTML = staffHtml;
                }

                // Update charts dynamically
                if (d.charts) {
                    if (trafficChart) {
                        trafficChart.data.labels = d.charts.timestamps;
                        trafficChart.data.datasets[0].data = d.charts.rps;
                        trafficChart.data.datasets[1].data = d.charts.latency;
                        trafficChart.update();
                    }
                    if (hardwareChart) {
                        hardwareChart.data.labels = d.charts.timestamps;
                        hardwareChart.data.datasets[0].data = d.charts.twekl_cpu;
                        hardwareChart.data.datasets[1].data = d.charts.host_cpu;
                        hardwareChart.data.datasets[2].data = d.charts.ram;
                        hardwareChart.update();
                    }
                    if (roleChart) {
                        roleChart.data.labels = d.charts.role_labels;
                        roleChart.data.datasets[0].data = d.charts.role_counts;
                        roleChart.update();
                    }
                }

                // Responses & dropouts table
                const errorTbody = document.getElementById('errorLogBody');
                if (d.recent_errors && d.recent_errors.length > 0) {
                    errorTbody.innerHTML = d.recent_errors.map(e => `
                        <tr>
                            <td>${e.timestamp}</td>
                            <td style="font-weight: 600;">${e.user}</td>
                            <td>${e.action}</td>
                            <td><span class="status-pill" style="background: #991b1b;">${e.status}</span></td>
                            <td style="color: #f87171;">${e.message}</td>
                        </tr>
                    `).join('');
                } else {
                    errorTbody.innerHTML = '<tr><td colspan="5" style="color: #22c55e; text-align: center;">No errors. 100% successful responses.</td></tr>';
                }
            } catch (err) {}
        }

        window.addEventListener('DOMContentLoaded', () => {
            initCharts();
            fetchMetrics();
            setInterval(fetchMetrics, 1000);
        });
    </script>
</body>
</html>
"""

simulator_engine = None

async def handle_dashboard_page(request):
    return web.Response(text=HTML_PAGE, content_type="text/html")

async def handle_metrics_api(request):
    pool_size = len(simulator_engine.customer_id_pool) if simulator_engine else 0
    return web.json_response(metrics.get_summary(pool_size))

async def handle_chart_js(request):
    if os.path.exists(CHART_JS_PATH):
        return web.FileResponse(CHART_JS_PATH)
    return web.Response(text="// Chart.js placeholder", content_type="application/javascript")

async def run_web_dashboard(port: int):
    app = web.Application()
    app.router.add_get("/", handle_dashboard_page)
    app.router.add_get("/api/metrics", handle_metrics_api)
    app.router.add_get("/static/chart.js", handle_chart_js)
    runner = web.AppRunner(app)
    await runner.setup()
    site = web.TCPSite(runner, "0.0.0.0", port)
    await site.start()

def main():
    global simulator_engine
    parser = argparse.ArgumentParser(description="Twekl Dashboard 24/7 Traffic Simulator & Telemetry")
    parser.add_argument("--host", default="http://localhost:8080", help="Base URL of Twekl Dashboard application")
    parser.add_argument("--rate", type=int, default=30, help="Target Requests Per Second (RPS)")
    parser.add_argument("--port", type=int, default=9090, help="Port for live status web dashboard")
    parser.add_argument("--user", default=None, help="Legacy username argument (kept for compatibility)")
    parser.add_argument("--password", default=None, help="Legacy password argument (kept for compatibility)")
    args = parser.parse_args()

    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)

    simulator_engine = TrafficEngine(base_url=args.host, target_rps=args.rate)
    loop.create_task(run_web_dashboard(args.port))
    loop.create_task(simulator_engine.start())

    try:
        loop.run_forever()
    except KeyboardInterrupt:
        pass
    finally:
        loop.close()

if __name__ == "__main__":
    main()
