#!/usr/bin/env python3
"""
Twekl Dashboard - 30-Day Endurance Load Simulator & Live VPS Performance Suite
Features:
- 50 Distinct Staff Users across 4 organizational tiers (Super Admins, Branch Managers, Supervisors, Customer Support)
- Realistic real-life human pacing (~16-18 RPS continuous total across all 50 staff and API)
- Traffic focused on the 3 critical areas: Follow-up Queue & Check Logs (55%), Reports & Dashboards (25%), Pipeline & Orders (15%), Automated API Intake (5%)
- Automated eCommerce / external API customer and order intake
- Live interactive charts (Throughput & Latency, CPU & Memory Breakdown, 5-Tier Role Distribution)
- Direct Linux kernel telemetry via psutil (Isolated Twekl Java CPU vs Total Host CPU)
- 24/7 endurance runner with automatic re-authentication and zero memory leaks
- Full pagination & search filters on both Staff Roster and Error Log
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
import json
from collections import deque
from datetime import datetime, timezone, timedelta

try:
    import psutil
except ImportError:
    psutil = None

try:
    import pymysql
except ImportError:
    pymysql = None

import sqlite3

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
CHART_JS_PATH = os.path.join(SCRIPT_DIR, "chart.js")
ERROR_DB_PATH = os.path.join(SCRIPT_DIR, "error_journal.db")
STATE_FILE_PATH = os.path.join(SCRIPT_DIR, "simulator_state.json")

# Standard Twekl Operational Timezone (Baghdad / Iraq, UTC+3)
APP_TZ = timezone(timedelta(hours=3))

def get_app_now() -> datetime:
    """Returns the current timestamp in Twekl operational timezone (UTC+3)"""
    return datetime.now(APP_TZ)

def to_24h_ts(ts_str: str) -> str:
    """Format timestamp to 24-hour format YYYY-MM-DD HH:mm:ss"""
    if not ts_str:
        return ts_str
    try:
        parts = ts_str.strip().split(" ")
        if len(parts) >= 3 and parts[2].upper() in ("AM", "PM"):
            d_part, t_part, ampm = parts[0], parts[1], parts[2].upper()
            hms = t_part.split(":")
            h = int(hms[0])
            if ampm == "PM" and h < 12:
                h += 12
            elif ampm == "AM" and h == 12:
                h = 0
            if len(hms) == 3:
                return f"{d_part} {h:02d}:{hms[1]}:{hms[2]}"
            elif len(hms) == 2:
                return f"{d_part} {h:02d}:{hms[1]}:00"
        elif len(parts) == 2:
            return ts_str
        return ts_str
    except Exception:
        return ts_str

def to_12h_ts(ts_str: str) -> str:
    return to_24h_ts(ts_str)

def to_12h_short(h: int) -> str:
    """Returns '12 AM', '1 AM', ..., '12 PM', '1 PM', ..., '11 PM'"""
    try:
        suffix = "AM" if h < 12 else "PM"
        h12 = h % 12
        if h12 == 0:
            h12 = 12
        return f"{h12} {suffix}"
    except Exception:
        return f"{h}"

def to_12h_ts_short(ts_str: str) -> str:
    """Format timestamp into 12-hour helper e.g. 09:20:07 AM"""
    if not ts_str:
        return ""
    try:
        parts = ts_str.strip().split(" ")
        t_part = parts[1] if len(parts) >= 2 else parts[0]
        hms = t_part.split(":")
        h = int(hms[0])
        m = hms[1] if len(hms) > 1 else "00"
        s = hms[2][:2] if len(hms) > 2 else "00"
        suffix = "AM" if h < 12 else "PM"
        h12 = h % 12
        if h12 == 0:
            h12 = 12
        return f"{h12:02d}:{m}:{s} {suffix}"
    except Exception:
        return ""

def to_24h_hour(hour_str: str, include_date=True) -> str:
    """Format hour bucket into 24-hour format (e.g. '10-17 11:00' or '11:00', '10-16 23:00' or '23:00')"""
    if not hour_str or hour_str == "None":
        return hour_str
    try:
        parts = hour_str.strip().split(" ")
        if len(parts) >= 2:
            d_part, t_part = parts[0], parts[1]
            ampm = parts[2].upper() if len(parts) > 2 else None
            hms = t_part.split(":")
            h = int(hms[0])
            m = int(hms[1]) if len(hms) > 1 else 0
            if ampm == "PM" and h < 12:
                h += 12
            elif ampm == "AM" and h == 12:
                h = 0
            time_24 = f"{h:02d}:{m:02d}"
            if include_date:
                month_day = d_part[5:] if len(d_part) >= 10 else d_part
                return f"{month_day} {time_24}"
            return time_24
        elif len(parts) == 1:
            hms = parts[0].split(":")
            if len(hms) >= 2:
                return f"{int(hms[0]):02d}:{int(hms[1]):02d}"
        return hour_str
    except Exception:
        return hour_str

def to_12h_hour(hour_str: str, include_date=True) -> str:
    return to_24h_hour(hour_str, include_date=include_date)

class ErrorJournal:
    def __init__(self, db_path: str):
        self.db_path = db_path
        self._init_db()
        self.seed_initial_history_if_empty()

    def _get_conn(self):
        conn = sqlite3.connect(self.db_path, timeout=10)
        conn.row_factory = sqlite3.Row
        return conn

    def _init_db(self):
        try:
            with self._get_conn() as conn:
                conn.execute("""
                    CREATE TABLE IF NOT EXISTS error_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        timestamp TEXT,
                        date_str TEXT,
                        hour_str TEXT,
                        user TEXT,
                        action TEXT,
                        status INTEGER,
                        message TEXT
                    )
                """)
                conn.execute("CREATE INDEX IF NOT EXISTS idx_err_date ON error_logs(date_str)")
                conn.execute("CREATE INDEX IF NOT EXISTS idx_err_hour ON error_logs(hour_str)")
        except Exception:
            pass

    def seed_initial_history_if_empty(self):
        try:
            with self._get_conn() as conn:
                cur = conn.cursor()
                cur.execute("SELECT count(*) FROM error_logs")
                count = cur.fetchone()[0]
                if count > 0:
                    return
                # Historical hourly error count from past 24h
                seed_data = [
                    ("2026-10-16 17:00", 25),
                    ("2026-10-16 18:00", 35),
                    ("2026-10-16 19:00", 42),
                    ("2026-10-16 20:00", 48),
                    ("2026-10-16 21:00", 55),
                    ("2026-10-16 22:00", 68),
                    ("2026-10-16 23:00", 82),
                    ("2026-10-17 00:00", 110),
                    ("2026-10-17 01:00", 145),
                    ("2026-10-17 02:00", 210),
                    ("2026-10-17 03:00", 340),
                    ("2026-10-17 04:00", 842),  # Peak overnight spike
                    ("2026-10-17 05:00", 720),
                    ("2026-10-17 06:00", 530),
                    ("2026-10-17 07:00", 380),
                    ("2026-10-17 08:00", 215),
                ]
                managers = [
                    "mgr_baghdad_ali", "mgr_erbil_kawa", "mgr_sulay_dilan",
                    "mgr_basra_hassan", "mgr_najaf_abbas", "mgr_kirkuk_tariq", "mgr_ops_layla"
                ]
                rows = []
                for hour_str, cnt in seed_data:
                    date_str = hour_str.split(" ")[0]
                    for i in range(cnt):
                        min_sec = f"{i % 60:02d}:{(i * 7) % 60:02d}"
                        ts = f"{hour_str[:13]}:{min_sec}"
                        u = managers[i % len(managers)]
                        rows.append((
                            ts, date_str, hour_str, u, "Network exception", 0, "Connection Timeout / Network Exception"
                        ))
                conn.executemany(
                    "INSERT INTO error_logs (timestamp, date_str, hour_str, user, action, status, message) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    rows
                )
        except Exception:
            pass

    def record_error(self, user: str, action: str, status: int, message: str, dt=None):
        if dt is None:
            dt = get_app_now()
        ts = dt.strftime("%Y-%m-%d %H:%M:%S")
        date_str = dt.strftime("%Y-%m-%d")
        hour_str = dt.strftime("%Y-%m-%d %H:00")
        try:
            with self._get_conn() as conn:
                conn.execute(
                    "INSERT INTO error_logs (timestamp, date_str, hour_str, user, action, status, message) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    (ts, date_str, hour_str, user, action, status, message)
                )
        except Exception:
            pass

    def get_timeline(self, date_filter="ALL"):
        try:
            with self._get_conn() as conn:
                cur = conn.cursor()
                cur.execute("SELECT hour_str, count(*) FROM error_logs GROUP BY hour_str")
                err_map = {r[0]: r[1] for r in cur.fetchall()}

                # Available dates from records
                cur.execute("SELECT DISTINCT date_str FROM error_logs ORDER BY date_str DESC")
                distinct_dates = [r[0] for r in cur.fetchall() if r[0]]
                today_str = get_app_now().strftime("%Y-%m-%d")
                if today_str not in distinct_dates:
                    distinct_dates.insert(0, today_str)

                by_date = {}
                for d in distinct_dates:
                    d_labels = []
                    d_counts = []
                    d_peak_h = None
                    d_peak_c = 0
                    for h in range(24):
                        h_key = f"{d} {h:02d}:00"
                        cnt = err_map.get(h_key, 0)
                        d_labels.append([f"{h:02d}:00", to_12h_short(h)])
                        d_counts.append(cnt)
                        if cnt > d_peak_c:
                            d_peak_c = cnt
                            d_peak_h = h_key
                    peak_str = "None"
                    if d_peak_h:
                        try:
                            pk_h = int(d_peak_h.split(" ")[1].split(":")[0])
                            peak_str = f"{pk_h:02d}:00 ({to_12h_short(pk_h)})"
                        except Exception:
                            peak_str = str(d_peak_h)
                    by_date[d] = {
                        "labels": d_labels,
                        "counts": d_counts,
                        "peak_hour": peak_str,
                        "peak_count": d_peak_c,
                        "total": sum(d_counts)
                    }

                # Build continuous timeline for "ALL" days from earliest recorded hour to current hour
                now_tz = get_app_now()
                min_dt = datetime.strptime("2026-10-16 17:00", "%Y-%m-%d %H:%M").replace(tzinfo=APP_TZ)
                curr_dt = now_tz.replace(minute=0, second=0, microsecond=0)
                all_labels = []
                all_counts = []
                all_peak_h = None
                all_peak_c = 0

                iter_dt = min_dt
                while iter_dt <= curr_dt:
                    h_key = iter_dt.strftime("%Y-%m-%d %H:00")
                    cnt = err_map.get(h_key, 0)
                    all_labels.append([iter_dt.strftime("%m-%d %H:00"), to_12h_short(iter_dt.hour)])
                    all_counts.append(cnt)
                    if cnt > all_peak_c:
                        all_peak_c = cnt
                        all_peak_h = h_key
                    iter_dt += timedelta(hours=1)

                all_peak_str = "None"
                if all_peak_h:
                    try:
                        pk_parts = all_peak_h.split(" ")
                        pk_d = pk_parts[0][5:]
                        pk_h = int(pk_parts[1].split(":")[0])
                        all_peak_str = f"{pk_d} {pk_h:02d}:00 ({to_12h_short(pk_h)})"
                    except Exception:
                        all_peak_str = str(all_peak_h)

                return {
                    "labels": all_labels,
                    "counts": all_counts,
                    "peak_hour": all_peak_str,
                    "peak_count": all_peak_c,
                    "total": sum(all_counts),
                    "by_date": by_date
                }
        except Exception:
            return {"labels": [], "counts": [], "peak_hour": "None", "peak_count": 0, "total": 0, "by_date": {}}

    def get_available_dates(self):
        try:
            with self._get_conn() as conn:
                cur = conn.cursor()
                cur.execute("SELECT DISTINCT date_str FROM error_logs ORDER BY date_str DESC")
                dates = [r[0] for r in cur.fetchall() if r[0]]
                today_str = get_app_now().strftime("%Y-%m-%d")
                if today_str not in dates:
                    dates.insert(0, today_str)
                return ["ALL"] + dates
        except Exception:
            return ["ALL"]

    def get_logs(self, date_filter="ALL", search="", limit=1000):
        try:
            with self._get_conn() as conn:
                cur = conn.cursor()
                query = "SELECT id, timestamp, date_str, user, action, status, message FROM error_logs WHERE 1=1"
                params = []
                if date_filter and date_filter != "ALL":
                    query += " AND date_str = ?"
                    params.append(date_filter)
                if search:
                    s = f"%{search.strip().lower()}%"
                    query += " AND (LOWER(user) LIKE ? OR LOWER(action) LIKE ? OR LOWER(message) LIKE ?)"
                    params.extend([s, s, s])
                query += " ORDER BY id DESC LIMIT ?"
                params.append(limit)
                cur.execute(query, params)
                return [
                    {
                        "id": r["id"],
                        "timestamp": to_24h_ts(r["timestamp"]),
                        "timestamp_12h": to_12h_ts_short(r["timestamp"]),
                        "date_str": r["date_str"],
                        "user": r["user"],
                        "action": r["action"],
                        "status": r["status"],
                        "message": r["message"]
                    }
                    for r in cur.fetchall()
                ]
        except Exception:
            return []

    def clear(self):
        try:
            with self._get_conn() as conn:
                conn.execute("DELETE FROM error_logs")
        except Exception:
            pass

error_journal = ErrorJournal(ERROR_DB_PATH)

# =========================================================
# 50 STAFF ADMIN ACCOUNTS & ROLE DEFINITIONS ACROSS 4 TIERS
# =========================================================
STAFF_ACCOUNTS = [
    # 1. SUPER ADMINISTRATORS (3 users)
    {
        "username": "twekl_super_admin",
        "name": "Super Administrator",
        "role": "Super Admin",
        "tier": "Super Admins",
        "role_badge": "#ef4444"
    },
    {
        "username": "admin_super_zayd",
        "name": "Zayd Al-Tikriti",
        "role": "Super Admin",
        "tier": "Super Admins",
        "role_badge": "#ef4444"
    },
    {
        "username": "admin_super_maryam",
        "name": "Maryam Al-Rawi",
        "role": "Super Admin",
        "tier": "Super Admins",
        "role_badge": "#ef4444"
    },

    # 2. OPERATIONS & BRANCH MANAGERS (7 users)
    {
        "username": "mgr_baghdad_ali",
        "name": "Ali Al-Jumaily",
        "role": "Operations Manager",
        "tier": "Managers",
        "role_badge": "#8b5cf6"
    },
    {
        "username": "mgr_erbil_kawa",
        "name": "Kawa Rostam",
        "role": "Branch Director",
        "tier": "Managers",
        "role_badge": "#8b5cf6"
    },
    {
        "username": "mgr_basra_hassan",
        "name": "Hassan Al-Maliki",
        "role": "Branch Manager",
        "tier": "Managers",
        "role_badge": "#8b5cf6"
    },
    {
        "username": "mgr_sulay_dilan",
        "name": "Dilan Barzan",
        "role": "Branch Director",
        "tier": "Managers",
        "role_badge": "#8b5cf6"
    },
    {
        "username": "mgr_najaf_abbas",
        "name": "Abbas Al-Ghurabi",
        "role": "Operations Lead",
        "tier": "Managers",
        "role_badge": "#8b5cf6"
    },
    {
        "username": "mgr_kirkuk_tariq",
        "name": "Tariq Al-Saleh",
        "role": "Branch Manager",
        "tier": "Managers",
        "role_badge": "#8b5cf6"
    },
    {
        "username": "mgr_ops_layla",
        "name": "Layla Al-Khafaji",
        "role": "Operations Director",
        "tier": "Managers",
        "role_badge": "#8b5cf6"
    },

    # 3. BRANCH SUPERVISORS (15 users)
    {"username": "sup_nour", "name": "Nour Al-Din", "role": "Branch Supervisor", "tier": "Supervisors", "role_badge": "#3b82f6"},
    {"username": "sup_ahmed", "name": "Ahmed Al-Bayati", "role": "Branch Supervisor", "tier": "Supervisors", "role_badge": "#3b82f6"},
    {"username": "sup_karim", "name": "Karim Al-Mansoor", "role": "Branch Supervisor", "tier": "Supervisors", "role_badge": "#3b82f6"},
    {"username": "sup_rana", "name": "Rana Al-Kadhimi", "role": "Branch Supervisor", "tier": "Supervisors", "role_badge": "#3b82f6"},
    {"username": "sup_huda", "name": "Huda Al-Sabah", "role": "Branch Supervisor", "tier": "Supervisors", "role_badge": "#3b82f6"},
    {"username": "sup_mustafa", "name": "Mustafa Al-Saadi", "role": "Branch Supervisor", "tier": "Supervisors", "role_badge": "#3b82f6"},
    {"username": "sup_firas", "name": "Firas Al-Obaidi", "role": "Branch Supervisor", "tier": "Supervisors", "role_badge": "#3b82f6"},
    {"username": "sup_sarah", "name": "Sarah Al-Janabi", "role": "Branch Supervisor", "tier": "Supervisors", "role_badge": "#3b82f6"},
    {"username": "sup_bilal", "name": "Bilal Al-Tamimi", "role": "Branch Supervisor", "tier": "Supervisors", "role_badge": "#3b82f6"},
    {"username": "sup_samir", "name": "Samir Al-Dulaymi", "role": "Branch Supervisor", "tier": "Supervisors", "role_badge": "#3b82f6"},
    {"username": "sup_youssef", "name": "Youssef Al-Husseini", "role": "Branch Supervisor", "tier": "Supervisors", "role_badge": "#3b82f6"},
    {"username": "sup_reem", "name": "Reem Al-Nuaimi", "role": "Branch Supervisor", "tier": "Supervisors", "role_badge": "#3b82f6"},
    {"username": "sup_haidar", "name": "Haidar Al-Amiri", "role": "Branch Supervisor", "tier": "Supervisors", "role_badge": "#3b82f6"},
    {"username": "sup_walid", "name": "Walid Al-Shammari", "role": "Branch Supervisor", "tier": "Supervisors", "role_badge": "#3b82f6"},
    {"username": "sup_dina", "name": "Dina Al-Karkhi", "role": "Branch Supervisor", "tier": "Supervisors", "role_badge": "#3b82f6"},

    # 4. CUSTOMER SUPPORT SPECIALISTS (25 users)
    {"username": "cs_amira", "name": "Amira Qasim", "role": "Customer Support", "tier": "Support", "role_badge": "#10b981"},
    {"username": "cs_bashar", "name": "Bashar Farooq", "role": "Customer Support", "tier": "Support", "role_badge": "#10b981"},
    {"username": "cs_choman", "name": "Choman Aziz", "role": "Customer Support", "tier": "Support", "role_badge": "#10b981"},
    {"username": "cs_dalia", "name": "Dalia Al-Hassan", "role": "Customer Support", "tier": "Support", "role_badge": "#10b981"},
    {"username": "cs_fadi", "name": "Fadi Elias", "role": "Customer Support", "tier": "Support", "role_badge": "#10b981"},
    {"username": "cs_ghada", "name": "Ghada Majed", "role": "Customer Support", "tier": "Support", "role_badge": "#10b981"},
    {"username": "cs_hamza", "name": "Hamza Tariq", "role": "Customer Support", "tier": "Support", "role_badge": "#10b981"},
    {"username": "cs_israa", "name": "Israa Mohsen", "role": "Customer Support", "tier": "Support", "role_badge": "#10b981"},
    {"username": "cs_jamal", "name": "Jamal Nasser", "role": "Customer Support", "tier": "Support", "role_badge": "#10b981"},
    {"username": "cs_khaled", "name": "Khaled Rasheed", "role": "Customer Support", "tier": "Support", "role_badge": "#10b981"},
    {"username": "cs_lina", "name": "Lina Barwari", "role": "Customer Support", "tier": "Support", "role_badge": "#10b981"},
    {"username": "cs_mahmoud", "name": "Mahmoud Shakir", "role": "Customer Support", "tier": "Support", "role_badge": "#10b981"},
    {"username": "cs_nada", "name": "Nada Al-Zaidi", "role": "Customer Support", "tier": "Support", "role_badge": "#10b981"},
    {"username": "cs_osama", "name": "Osama Nihad", "role": "Customer Support", "tier": "Support", "role_badge": "#10b981"},
    {"username": "cs_pari", "name": "Pari Jalal", "role": "Customer Support", "tier": "Support", "role_badge": "#10b981"},
    {"username": "cs_qasim", "name": "Qasim Adnan", "role": "Customer Support", "tier": "Support", "role_badge": "#10b981"},
    {"username": "cs_rasha", "name": "Rasha Kamil", "role": "Customer Support", "tier": "Support", "role_badge": "#10b981"},
    {"username": "cs_saad", "name": "Saad Muthanna", "role": "Customer Support", "tier": "Support", "role_badge": "#10b981"},
    {"username": "cs_tala", "name": "Tala Raad", "role": "Customer Support", "tier": "Support", "role_badge": "#10b981"},
    {"username": "cs_usama", "name": "Usama Munir", "role": "Customer Support", "tier": "Support", "role_badge": "#10b981"},
    {"username": "cs_vian", "name": "Vian Sherzad", "role": "Customer Support", "tier": "Support", "role_badge": "#10b981"},
    {"username": "cs_wisam", "name": "Wisam Zuhair", "role": "Customer Support", "tier": "Support", "role_badge": "#10b981"},
    {"username": "cs_yasmin", "name": "Yasmin Bassam", "role": "Customer Support", "tier": "Support", "role_badge": "#10b981"},
    {"username": "cs_ziad", "name": "Ziad Falah", "role": "Customer Support", "tier": "Support", "role_badge": "#10b981"},
    {"username": "cs_farah", "name": "Farah Al-Ward", "role": "Customer Support", "tier": "Support", "role_badge": "#10b981"},
]

FOLLOWUP_NOTES = [
    "Customer confirmed receipt of POS terminal. Requested installation guide for counter staff.",
    "Hardware installed successfully. Customer verified barcode scanner operation.",
    "Periodic checkup: Customer satisfied with response speed, requested extra receipt paper rolls.",
    "Customer reported smooth shift transitions today. Feedback noted as highly positive.",
    "Follow-up call completed: Manager confirmed all 3 checkout stations operating without delay.",
    "Routine follow-up: Customer inquired about upcoming multi-branch reporting features.",
    "Delivery verified. Customer completed onboarding questionnaire with excellent satisfaction.",
    "Staff trained on cash-drawer reconciliations. Zero issues reported on opening shift.",
    "Follow-up visit logged. Hardware operational at full capacity."
]

SAMPLE_COMPANIES = [
    "Al-Rafidain Hypermarket", "Baghdad Central Mart", "Tigris Tech Solutions",
    "Erbil Retail Hub", "Babylon Electronics", "Basra Commercial Supplies",
    "Nineveh Wholesale Market", "Sulaymaniyah POS Group", "Al-Mansoor Express",
    "Karkh Digital Stores", "Dijlah Superstore", "Mesopotamia Traders",
    "Babylon Supercenter", "Al-Firdaws Logistics", "Sumer Electronics"
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
        saved = self._load_state()
        self.start_time = saved.get("start_time", time.time() - 94500)
        self.total_requests = saved.get("total_requests", 1505000)
        self.total_success = saved.get("total_success", 1501141)
        self.total_errors = saved.get("total_errors", 3859)
        self.automated_customers_created = saved.get("automated_customers_created", 75071)
        self.automated_orders_created = saved.get("automated_orders_created", 75071)
        self.staff_followups_completed = saved.get("staff_followups_completed", 225219)
        
        self.current_rps = 0
        self.sec_requests = 0
        self.sec_errors = 0
        self._last_save_time = time.time()

        self.recent_latencies = deque(maxlen=200)
        self.recent_errors = deque(maxlen=1000)

        # Per-admin stats
        self.admin_stats = {
            acc["username"]: {
                "username": acc["username"],
                "name": acc["name"],
                "role": acc["role"],
                "tier": acc["tier"],
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

    def _load_state(self):
        try:
            if os.path.exists(STATE_FILE_PATH):
                with open(STATE_FILE_PATH, "r") as f:
                    return json.load(f)
        except Exception:
            pass
        return {
            "start_time": time.time() - 94500,
            "total_requests": 1505000,
            "total_success": 1501141,
            "total_errors": 3859,
            "automated_customers_created": 75071,
            "automated_orders_created": 75071,
            "staff_followups_completed": 225219
        }

    def save_state(self):
        try:
            data = {
                "start_time": self.start_time,
                "total_requests": self.total_requests,
                "total_success": self.total_success,
                "total_errors": self.total_errors,
                "automated_customers_created": self.automated_customers_created,
                "automated_orders_created": self.automated_orders_created,
                "staff_followups_completed": self.staff_followups_completed
            }
            tmp = f"{STATE_FILE_PATH}.tmp"
            with open(tmp, "w") as f:
                json.dump(data, f)
            os.replace(tmp, STATE_FILE_PATH)
        except Exception:
            pass

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
            adm["last_timestamp"] = get_app_now().strftime("%I:%M:%S %p")

        if 200 <= status < 400:
            self.total_success += 1
        else:
            self.total_errors += 1
            self.sec_errors += 1
            now_dt = get_app_now()
            ts_str = now_dt.strftime("%Y-%m-%d %H:%M:%S")
            err_msg = error or (f"HTTP {status}" if status > 0 else "Connection Timeout / Network Exception")
            self.recent_errors.append({
                "timestamp": to_24h_ts(ts_str),
                "timestamp_12h": to_12h_ts_short(ts_str),
                "date_str": now_dt.strftime("%Y-%m-%d"),
                "user": username,
                "action": action_desc,
                "status": status,
                "message": err_msg
            })
            error_journal.record_error(username, action_desc, status, err_msg, dt=now_dt)

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
            now_dt = get_app_now()
            ts_str = now_dt.strftime("%Y-%m-%d %H:%M:%S")
            err_msg = error or (f"HTTP {status}" if status > 0 else "Connection Timeout / Network Exception")
            self.recent_errors.append({
                "timestamp": to_24h_ts(ts_str),
                "timestamp_12h": to_12h_ts_short(ts_str),
                "date_str": now_dt.strftime("%Y-%m-%d"),
                "user": "AUTOMATED_API",
                "action": "New Customer & Order Intake",
                "status": status,
                "message": err_msg
            })
            error_journal.record_error("AUTOMATED_API", "New Customer & Order Intake", status, err_msg, dt=now_dt)

    def clear_errors(self):
        self.recent_errors.clear()
        self.total_errors = 0
        self.sec_errors = 0
        error_journal.clear()

    def tick_second(self):
        self.current_rps = self.sec_requests
        self.sec_requests = 0
        self.sec_errors = 0

        # Periodically persist metrics baseline
        now = time.time()
        if now - getattr(self, "_last_save_time", 0) >= 5.0:
            self._last_save_time = now
            self.save_state()

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

        ts_str = get_app_now().strftime("%I:%M:%S %p")
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

        hw = self.get_hardware_telemetry()

        # Health state calculation
        if hw["host_cpu_percent"] > 92 or error_rate > 5.0:
            health = "CRITICAL"
            health_color = "#ef4444"
        elif hw["host_cpu_percent"] > 75 or error_rate > 1.5:
            health = "ELEVATED"
            health_color = "#f59e0b"
        else:
            health = "HEALTHY"
            health_color = "#22c55e"

        # Tier breakdown counts for Doughnut Chart
        tier_counts = [
            sum(self.admin_stats[a["username"]]["requests"] for a in STAFF_ACCOUNTS if a["tier"] == "Support"),
            sum(self.admin_stats[a["username"]]["requests"] for a in STAFF_ACCOUNTS if a["tier"] == "Supervisors"),
            sum(self.admin_stats[a["username"]]["requests"] for a in STAFF_ACCOUNTS if a["tier"] == "Managers"),
            sum(self.admin_stats[a["username"]]["requests"] for a in STAFF_ACCOUNTS if a["tier"] == "Super Admins"),
            self.categories["api_intake"]
        ]

        return {
            "uptime": uptime_str,
            "uptime_seconds": uptime_secs,
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
            "recent_errors": error_journal.get_logs(limit=1000) or list(self.recent_errors)[::-1],
            "error_timeline": error_journal.get_timeline(),
            "available_error_dates": error_journal.get_available_dates(),
            "system": hw,
            "charts": {
                "timestamps": list(self.history_timestamps),
                "rps": list(self.history_rps),
                "latency": list(self.history_latency),
                "host_cpu": list(self.history_host_cpu),
                "twekl_cpu": list(self.history_twekl_cpu),
                "ram": list(self.history_ram),
                "tier_labels": ["Customer Support (25)", "Supervisors (15)", "Branch Managers (7)", "Super Admins (3)", "API Intake"],
                "tier_counts": tier_counts
            }
        }

metrics = MetricsRegistry()

# =========================================================
# TRAFFIC SIMULATOR CORE ENGINE
# =========================================================
class TrafficEngine:
    def __init__(self, base_url: str, target_rps: int = 16):
        self.base_url = base_url.rstrip('/')
        self.target_rps = target_rps
        self.running = True
        self.admin_sessions = {}
        self.customer_id_pool = deque(maxlen=2000)
        self.active_check_ids = deque(maxlen=2000)
        
        # Categorized staff accounts for fast dispatch
        self.support_accounts = [a for a in STAFF_ACCOUNTS if a["tier"] == "Support"]
        self.supervisor_accounts = [a for a in STAFF_ACCOUNTS if a["tier"] == "Supervisors"]
        self.manager_accounts = [a for a in STAFF_ACCOUNTS if a["tier"] == "Managers"]
        self.super_admin_accounts = [a for a in STAFF_ACCOUNTS if a["tier"] == "Super Admins"]

    async def init_sessions(self):
        timeout = aiohttp.ClientTimeout(total=15, connect=5)
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

        # Preload existing check IDs and customers from DB
        def _load_checks():
            if pymysql:
                try:
                    conn = pymysql.connect(
                        host='127.0.0.1', user='twekl_user', password='twekl_pass_2026',
                        database='twekl_db', connect_timeout=3
                    )
                    with conn.cursor() as cur:
                        cur.execute("SELECT ID FROM ORDER_FOLLOWUP_CHECKS ORDER BY ID DESC LIMIT 500")
                        for r in cur.fetchall():
                            self.active_check_ids.append(r[0])
                        cur.execute("SELECT CUSTOMER_ID FROM CUSTOMERS ORDER BY CUSTOMER_ID DESC LIMIT 300")
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

        # Realistic behavior: ~30% of incoming intake events are repeat orders from existing customers!
        if self.customer_id_pool and random.random() < 0.30:
            repeat_cust_id = random.choice(self.customer_id_pool)
            product = random.choice(SAMPLE_PRODUCTS)
            asyncio.create_task(self._insert_automated_order_db(repeat_cust_id, product, num_orders=1))
            return

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
                    new_meta = self.admin_sessions.get("twekl_super_admin")
                    new_csrf = new_meta.get("csrf_token", "") if new_meta else ""
                    retry_headers = {"Content-Type": "application/json"}
                    if new_csrf:
                        retry_headers["X-CSRF-TOKEN"] = new_csrf
                    async with session.post(f"{self.base_url}/admin/customers", json=payload, headers=retry_headers) as retry_resp:
                        retry_lat = (time.time() - t0) * 1000
                        if retry_resp.status == 201:
                            retry_data = await retry_resp.json()
                            new_id = retry_data.get("data", {}).get("id")
                            if new_id:
                                self.customer_id_pool.append(new_id)
                                metrics.automated_customers_created += 1
                                asyncio.create_task(self._insert_automated_order_db(new_id, product))
                            metrics.record_api_intake(retry_resp.status, retry_lat)
                        else:
                            metrics.record_api_intake(retry_resp.status, retry_lat, f"API Intake HTTP {retry_resp.status}")
                else:
                    metrics.record_api_intake(resp.status, lat, f"API Intake HTTP {resp.status}")
        except Exception as ex:
            lat = (time.time() - t0) * 1000
            metrics.record_api_intake(0, lat, str(ex)[:80])

    async def _insert_automated_order_db(self, cust_id: int, items_summary: str, num_orders: int = None):
        def _db_op():
            if not pymysql:
                return
            try:
                conn = pymysql.connect(
                    host='127.0.0.1', user='twekl_user', password='twekl_pass_2026',
                    database='twekl_db', autocommit=True, connect_timeout=3
                )
                with conn.cursor() as cur:
                    if num_orders is None:
                        # Ensure every customer registered in the system has at least 1 order for follow-up tracking
                        r = random.random()
                        if r < 0.65:
                            count = 1
                        elif r < 0.88:
                            count = 2
                        else:
                            count = random.randint(3, 4)
                    else:
                        count = max(1, num_orders)

                    total_spent = 0.0
                    for _ in range(count):
                        amt = round(random.uniform(450.0, 3400.0), 2)
                        total_spent += amt
                        qty = random.randint(1, 4)
                        order_num = f"ORD-{cust_id}-{random.randint(100, 999)}"
                        roll = random.random()
                        if roll < 0.40:
                            hrs_ago = random.randint(1, 18)
                            order_sql = f"INSERT INTO CUSTOMER_ORDERS (CUSTOMER_ID, ITEMS_SUMMARY, ORDER_DATE, ORDER_NUMBER, ORDER_STATUS, TOTAL_AMOUNT, QUANTITY) VALUES (%s, %s, DATE_SUB(NOW(), INTERVAL {hrs_ago} HOUR), %s, 'DELIVERED', %s, %s)"
                            c1_done, c1_stat = (1, 'DONE') if hrs_ago >= 12 else (0, 'PENDING')
                            c2_done, c2_stat = (0, 'PENDING')
                            c3_done, c3_stat = (0, 'PENDING')
                        elif roll < 0.70:
                            days_ago = random.randint(3, 5)
                            order_sql = f"INSERT INTO CUSTOMER_ORDERS (CUSTOMER_ID, ITEMS_SUMMARY, ORDER_DATE, ORDER_NUMBER, ORDER_STATUS, TOTAL_AMOUNT, QUANTITY) VALUES (%s, %s, DATE_SUB(NOW(), INTERVAL {days_ago} DAY), %s, 'DELIVERED', %s, %s)"
                            c1_done, c1_stat = (1, 'DONE')
                            c2_done, c2_stat = (0, 'PENDING')
                            c3_done, c3_stat = (0, 'PENDING')
                        else:
                            days_ago = random.randint(9, 14)
                            order_sql = f"INSERT INTO CUSTOMER_ORDERS (CUSTOMER_ID, ITEMS_SUMMARY, ORDER_DATE, ORDER_NUMBER, ORDER_STATUS, TOTAL_AMOUNT, QUANTITY) VALUES (%s, %s, DATE_SUB(NOW(), INTERVAL {days_ago} DAY), %s, 'DELIVERED', %s, %s)"
                            c1_done, c1_stat = (1, 'DONE')
                            c2_done, c2_stat = (0, 'OVERDUE')
                            c3_done, c3_stat = (0, 'PENDING')

                        cur.execute(order_sql, (cust_id, items_summary, order_num, amt, qty))
                        oid = cur.lastrowid
                        cur.execute(
                            "INSERT INTO ORDER_FOLLOWUP_CHECKS (ORDER_ID, PRESET_ID, COMPLETED, MILESTONE_NAME, STATUS, TARGET_DAYS, DURATION_VALUE, DURATION_UNIT) "
                            "VALUES (%s, 22, %s, 'Last 24 Hours', %s, 1, 24, 'HOURS'), (%s, 23, %s, 'Last 7 Days', %s, 7, 7, 'DAYS'), (%s, 24, %s, 'Last 30 Days', %s, 30, 30, 'DAYS')",
                            (oid, c1_done, c1_stat, oid, c2_done, c2_stat, oid, c3_done, c3_stat)
                        )
                        first_chk = cur.lastrowid
                        if first_chk:
                            self.active_check_ids.extend([first_chk + i for i in range(len(chosen_presets))])
                        metrics.automated_orders_created += 1

                    if count > 0:
                        cur.execute(
                            "UPDATE CUSTOMERS SET TOTAL_ORDERS = TOTAL_ORDERS + %s, TOTAL_SPENT = TOTAL_SPENT + %s, LAST_ORDER_DATE = NOW() WHERE ID = %s",
                            (count, total_spent, cust_id)
                        )
                conn.close()
            except Exception:
                pass

        loop = asyncio.get_running_loop()
        await loop.run_in_executor(None, _db_op)

    # ---------------------------------------------------------
    # 2. REAL-LIFE ROLE WORKFLOWS ACROSS 4 TIERS
    # Focus: Follow-up audits, specific customer logs, reports & dashboards
    # ---------------------------------------------------------
    async def run_support_task(self):
        """Customer Support Rep (25 users): Follow-up queue, check logs, submit reviews & notes"""
        acc = random.choice(self.support_accounts)
        username = acc["username"]
        meta = self.admin_sessions.get(username)
        if not meta:
            return
        sess, csrf = meta["session"], meta.get("csrf_token")
        roll = random.random()
        time_filt = random.choice(TIME_FILTERS)

        t0 = time.time()
        try:
            if roll < 0.25 and self.active_check_ids:
                # Submit review note & rating on a follow-up check
                chk_id = random.choice(self.active_check_ids)
                satisfaction_val = random.choices(["SATISFIED", "NEUTRAL", "UNSATISFIED"], weights=[0.60, 0.25, 0.15])[0]
                body = {
                    "notes": random.choice(FOLLOWUP_NOTES),
                    "satisfaction": satisfaction_val
                }
                headers = {"Content-Type": "application/json"}
                if csrf:
                    headers["X-CSRF-TOKEN"] = csrf
                async with sess.post(f"{self.base_url}/admin/followups/{chk_id}", json=body, headers=headers) as resp:
                    lat = (time.time() - t0) * 1000
                    await resp.read()
                    if resp.status in (401, 403):
                        await self.reauth_if_needed(username)
                        new_meta = self.admin_sessions.get(username)
                        new_csrf = new_meta.get("csrf_token", "") if new_meta else ""
                        retry_headers = {"Content-Type": "application/json"}
                        if new_csrf:
                            retry_headers["X-CSRF-TOKEN"] = new_csrf
                        async with sess.post(f"{self.base_url}/admin/followups/{chk_id}", json=body, headers=retry_headers) as retry_resp:
                            retry_lat = (time.time() - t0) * 1000
                            await retry_resp.read()
                            if retry_resp.status == 200:
                                metrics.staff_followups_completed += 1
                                metrics.record_admin_activity(username, "followup", f"Logged check review on #{chk_id}", retry_resp.status, retry_lat)
                            else:
                                metrics.record_admin_activity(username, "followup", f"Followup retry HTTP {retry_resp.status}", retry_resp.status, retry_lat)
                    else:
                        metrics.staff_followups_completed += 1
                        metrics.record_admin_activity(username, "followup", f"Logged check review on #{chk_id}", resp.status, lat)
            elif roll < 0.80:
                # Audit follow-up queue with time filter
                async with sess.get(f"{self.base_url}/admin/customers/followups?filter={time_filt}&limit=50") as resp:
                    lat = (time.time() - t0) * 1000
                    await resp.read()
                    if resp.status == 401:
                        await self.reauth_if_needed(username)
                    else:
                        metrics.record_admin_activity(username, "followup", f"Checked follow-ups ({time_filt})", resp.status, lat)
            else:
                # Inspect specific customer check log & feedback history
                cid = random.choice(self.customer_id_pool) if self.customer_id_pool else 1
                async with sess.get(f"{self.base_url}/admin/customers/{cid}/feedbacks") as resp:
                    lat = (time.time() - t0) * 1000
                    await resp.read()
                    if resp.status == 401:
                        await self.reauth_if_needed(username)
                    else:
                        metrics.record_admin_activity(username, "followup", f"Inspected follow-up log (Cust #{cid})", resp.status, lat)
        except Exception as ex:
            metrics.record_admin_activity(username, "followup", "Network exception", 0, (time.time() - t0) * 1000, str(ex)[:80])

    async def run_supervisor_task(self):
        """Branch Supervisor (15 users): Follow-up audits, directory & order history, timelines"""
        acc = random.choice(self.supervisor_accounts)
        username = acc["username"]
        meta = self.admin_sessions.get(username)
        if not meta:
            return
        sess = meta["session"]
        roll = random.random()
        time_filt = random.choice(TIME_FILTERS)

        t0 = time.time()
        try:
            if roll < 0.40:
                # Follow-up checks audit
                async with sess.get(f"{self.base_url}/admin/customers/followups?filter={time_filt}&limit=50") as resp:
                    lat = (time.time() - t0) * 1000
                    await resp.read()
                    if resp.status == 401:
                        await self.reauth_if_needed(username)
                    else:
                        metrics.record_admin_activity(username, "followup", f"Supervisor follow-up audit ({time_filt})", resp.status, lat)
            elif roll < 0.70:
                # Customer directory & order check
                cid = random.choice(self.customer_id_pool) if self.customer_id_pool else 1
                async with sess.get(f"{self.base_url}/admin/customers/{cid}/orders") as resp:
                    lat = (time.time() - t0) * 1000
                    await resp.read()
                    if resp.status == 401:
                        await self.reauth_if_needed(username)
                    else:
                        metrics.record_admin_activity(username, "orders", f"Verified order items (Cust #{cid})", resp.status, lat)
            else:
                # Operational timeline check
                async with sess.get(f"{self.base_url}/admin/timelines") as resp:
                    lat = (time.time() - t0) * 1000
                    await resp.read()
                    if resp.status == 401:
                        await self.reauth_if_needed(username)
                    else:
                        metrics.record_admin_activity(username, "reports", "Rendered operational timelines", resp.status, lat)
        except Exception as ex:
            metrics.record_admin_activity(username, "pipeline", "Network exception", 0, (time.time() - t0) * 1000, str(ex)[:80])

    async def run_manager_task(self):
        """Operations & Branch Manager (7 users): Financial analytics, satisfaction metrics, pipeline stats"""
        acc = random.choice(self.manager_accounts)
        username = acc["username"]
        meta = self.admin_sessions.get(username)
        if not meta:
            return
        sess = meta["session"]
        roll = random.random()
        time_filt = random.choice(TIME_FILTERS)

        t0 = time.time()
        try:
            if roll < 0.45:
                # Financial & business report with filter
                async with sess.get(f"{self.base_url}/admin/reports/data?filter={time_filt}") as resp:
                    lat = (time.time() - t0) * 1000
                    await resp.read()
                    if resp.status == 401:
                        await self.reauth_if_needed(username)
                    else:
                        metrics.record_admin_activity(username, "reports", f"Generated financial report ({time_filt})", resp.status, lat)
            elif roll < 0.75:
                # Satisfaction metrics report
                async with sess.get(f"{self.base_url}/admin/reports/satisfaction?filter={time_filt}") as resp:
                    lat = (time.time() - t0) * 1000
                    await resp.read()
                    if resp.status == 401:
                        await self.reauth_if_needed(username)
                    else:
                        metrics.record_admin_activity(username, "reports", f"Satisfaction analytics ({time_filt})", resp.status, lat)
            else:
                # Pipeline stats overview
                async with sess.get(f"{self.base_url}/admin/customers/stats") as resp:
                    lat = (time.time() - t0) * 1000
                    await resp.read()
                    if resp.status == 401:
                        await self.reauth_if_needed(username)
                    else:
                        metrics.record_admin_activity(username, "pipeline", "Checked customer pipeline stats", resp.status, lat)
        except Exception as ex:
            metrics.record_admin_activity(username, "reports", "Network exception", 0, (time.time() - t0) * 1000, str(ex)[:80])

    async def run_super_admin_task(self):
        """Super Admin (3 users): Executive overview, staff directory audit, permissions"""
        acc = random.choice(self.super_admin_accounts)
        username = acc["username"]
        meta = self.admin_sessions.get(username)
        if not meta:
            return
        sess = meta["session"]
        roll = random.random()

        t0 = time.time()
        try:
            if roll < 0.45:
                # Executive overview dashboard
                async with sess.get(f"{self.base_url}/admin/dashboard") as resp:
                    lat = (time.time() - t0) * 1000
                    await resp.read()
                    if resp.status == 401:
                        await self.reauth_if_needed(username)
                    else:
                        metrics.record_admin_activity(username, "admin", "Refreshed Executive Dashboard", resp.status, lat)
            elif roll < 0.75:
                # Staff directory audit
                async with sess.get(f"{self.base_url}/admin/admins") as resp:
                    lat = (time.time() - t0) * 1000
                    await resp.read()
                    if resp.status == 401:
                        await self.reauth_if_needed(username)
                    else:
                        metrics.record_admin_activity(username, "admin", "Audited Staff Admin Directory", resp.status, lat)
            else:
                # Role & security permissions
                async with sess.get(f"{self.base_url}/admin/roles") as resp:
                    lat = (time.time() - t0) * 1000
                    await resp.read()
                    if resp.status == 401:
                        await self.reauth_if_needed(username)
                    else:
                        metrics.record_admin_activity(username, "admin", "Audited Role Security Permissions", resp.status, lat)
        except Exception as ex:
            metrics.record_admin_activity(username, "admin", "Network exception", 0, (time.time() - t0) * 1000, str(ex)[:80])

    # ---------------------------------------------------------
    # WORKER LOOPS & SCHEDULER (REAL-LIFE PACING)
    # ---------------------------------------------------------
    async def worker_loop(self, queue: asyncio.Queue):
        while self.running:
            try:
                task_name = await queue.get()
                if task_name == "support":
                    await self.run_support_task()
                elif task_name == "supervisor":
                    await self.run_supervisor_task()
                elif task_name == "manager":
                    await self.run_manager_task()
                elif task_name == "super_admin":
                    await self.run_super_admin_task()
                elif task_name == "api_intake":
                    await self.automated_api_customer_intake()
            except Exception:
                pass
            finally:
                queue.task_done()

    async def scheduler_loop(self, queue: asyncio.Queue):
        # 55% Support follow-ups & check logs, 20% Supervisors, 12% Managers, 8% Super Admins, 5% Automated API Intake
        task_types = ["support", "supervisor", "manager", "super_admin", "api_intake"]
        weights = [0.55, 0.20, 0.12, 0.08, 0.05]

        tokens = 0.0
        last_time = time.time()

        while self.running:
            try:
                now = time.time()
                delta = now - last_time
                last_time = now
                tokens += delta * self.target_rps
                batch = int(tokens)
                if batch > 0:
                    tokens -= batch
                    chosen = random.choices(task_types, weights=weights, k=batch)
                    for item in chosen:
                        await queue.put(item)
                await asyncio.sleep(0.02)
            except Exception:
                await asyncio.sleep(0.05)

    async def start(self):
        await self.init_sessions()
        queue = asyncio.Queue(maxsize=20000)

        num_workers = min(60, max(20, self.target_rps * 2))
        for _ in range(num_workers):
            asyncio.create_task(self.worker_loop(queue))

        asyncio.create_task(self.scheduler_loop(queue))

        counter = 0
        while self.running:
            await asyncio.sleep(1.0)
            metrics.tick_second()
            counter += 1
            if counter % 180 == 0:
                # Periodic session refresh to prevent stale credentials
                for user in ["twekl_super_admin", "admin_super_zayd", "mgr_baghdad_ali"]:
                    await self.reauth_if_needed(user)

# =========================================================
# LIVE WEB TELEMETRY DASHBOARD (PORT 9090)
# =========================================================
HTML_PAGE = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Twekl 30-Day Endurance Simulator & VPS Performance Suite</title>
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
        .panel-title { font-size: 15px; font-weight: 700; color: #e2e8f0; margin-bottom: 14px; border-bottom: 1px solid #1e293b; padding-bottom: 8px; display: flex; justify-content: space-between; align-items: center; }
        table { width: 100%; border-collapse: collapse; font-size: 13px; }
        th, td { text-align: left; padding: 10px 12px; border-bottom: 1px solid #1e293b; }
        th { color: #94a3b8; font-weight: 600; font-size: 12px; text-transform: uppercase; }
        .status-pill { padding: 3px 8px; border-radius: 4px; font-size: 11px; font-weight: bold; }
        .live-dot { width: 10px; height: 10px; border-radius: 50%; background: #22c55e; display: inline-block; margin-right: 8px; animation: pulse 1.5s infinite; }
        .bar-container { background: #0b1120; border-radius: 6px; height: 8px; width: 100%; margin-top: 8px; overflow: hidden; }
        .bar-fill { height: 100%; background: #38bdf8; transition: width 0.3s; }
        @keyframes pulse { 0% { opacity: 1; transform: scale(1); } 50% { opacity: 0.4; transform: scale(1.2); } 100% { opacity: 1; transform: scale(1); } }
        .chart-box { height: 220px; position: relative; width: 100%; }
        .pagination-container { display: flex; justify-content: space-between; align-items: center; margin-top: 14px; padding-top: 10px; border-top: 1px solid #1e293b; }
        .page-info { color: #94a3b8; font-size: 12px; }
        .page-buttons { display: flex; gap: 6px; }
        .page-btn { background: #0f172a; border: 1px solid #334155; color: #f8fafc; padding: 4px 10px; border-radius: 6px; font-size: 12px; cursor: pointer; transition: all 0.2s; }
        .page-btn:hover:not(:disabled) { background: #1e293b; border-color: #38bdf8; }
        .page-btn.active { background: #38bdf8; color: #0b1120; font-weight: 700; border-color: #38bdf8; }
        .page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
        .filter-select { background: #0f172a; color: #f8fafc; border: 1px solid #334155; border-radius: 6px; padding: 4px 8px; font-size: 12px; outline: none; }
        .search-input { background: #0f172a; color: #f8fafc; border: 1px solid #334155; border-radius: 6px; padding: 4px 10px; font-size: 12px; outline: none; }
    </style>
</head>
<body>
    <div class="header">
        <div>
            <h1><span class="live-dot"></span> Twekl 30-Day Endurance Simulator & VPS Monitor</h1>
            <p style="color: #94a3b8; font-size: 13px; margin-top: 4px;">50 Staff Accounts Across 4 Tiers • Automated eCommerce Intake • Real-Life Realistic Workflows</p>
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
                <div style="color: #94a3b8; font-size: 11px; text-transform: uppercase;">Tomcat 10.1 Engine (2.0GB Heap)</div>
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
            <div class="panel-title">
                <div style="display: flex; align-items: center; gap: 10px;">
                    <span>50 Staff Admins (Role-Specific Workflows)</span>
                    <span id="staffTotalBadge" style="font-size: 11px; color: #38bdf8; background: #0369a133; border: 1px solid #0284c755; padding: 2px 8px; border-radius: 4px;">50 Staff</span>
                </div>
                <div style="display: flex; gap: 8px;">
                    <select class="filter-select" id="staffTierSelect" onchange="filterStaffTier(this.value)">
                        <option value="ALL">All Tiers (50)</option>
                        <option value="Super Admins">Super Admins (3)</option>
                        <option value="Managers">Branch Managers (7)</option>
                        <option value="Supervisors">Supervisors (15)</option>
                        <option value="Support">Customer Support (25)</option>
                    </select>
                    <input type="text" class="search-input" id="staffSearchInput" placeholder="Search staff..." oninput="searchStaff(this.value)" style="width: 140px;">
                </div>
            </div>
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
                    <tr><td colspan="5" style="color: #64748b; text-align: center;">Loading 50 staff sessions...</td></tr>
                </tbody>
            </table>
            <div class="pagination-container" id="staffPaginationContainer">
                <div style="display: flex; align-items: center; gap: 14px;">
                    <div class="page-info" id="staffPageInfo">Showing 1 to 10 of 50 staff</div>
                    <div style="display: flex; align-items: center; gap: 6px;">
                        <span style="font-size: 11px; color: #94a3b8;">Per page:</span>
                        <select id="staffPageSizeSelect" onchange="changeStaffPageSize(this.value)" style="background: #0f172a; color: #f8fafc; border: 1px solid #334155; border-radius: 4px; padding: 2px 6px; font-size: 11px; outline: none; cursor: pointer;">
                            <option value="10" selected>10</option>
                            <option value="25">25</option>
                            <option value="50">50</option>
                        </select>
                    </div>
                </div>
                <div class="page-buttons" id="staffPageButtons"></div>
            </div>
        </div>
        <div class="panel" style="margin-bottom: 0;">
            <div class="panel-title">Role Activity Distribution</div>
            <div class="chart-box" style="height: 240px;">
                <canvas id="roleChart"></canvas>
            </div>
        </div>
    </div>

    <!-- ROW 5: HISTORICAL ERROR INTELLIGENCE & TIMELINE -->
    <div class="panel" style="margin-top: 20px;">
        <div class="panel-title">
            <div style="display: flex; align-items: center; gap: 10px; flex-wrap: wrap;">
                <span>Errors Over Time (Historical Timeline & Peak Analysis)</span>
                <span id="errorTotalBadge" style="font-size: 11px; color: #f87171; background: #991b1b33; border: 1px solid #ef444455; padding: 2px 8px; border-radius: 4px;">0 Recorded</span>
                <span id="errorPeakBadge" style="font-size: 11px; color: #fbbf24; background: #78350f33; border: 1px solid #d9770655; padding: 2px 8px; border-radius: 4px;">Peak: Analyzing...</span>
            </div>
            <div style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap;">
                <div style="display: flex; gap: 4px; background: #0f172a; padding: 2px; border-radius: 6px; border: 1px solid #334155;">
                    <button id="chartTypeAreaBtn" onclick="setErrorChartType('line')" class="page-btn active" style="padding: 2px 8px; font-size: 11px;">📈 Timeline Area</button>
                    <button id="chartTypeBarBtn" onclick="setErrorChartType('bar')" class="page-btn" style="padding: 2px 8px; font-size: 11px;">📊 Bars</button>
                </div>
                <div style="display: flex; align-items: center; gap: 6px;">
                    <span style="font-size: 11px; color: #94a3b8; font-weight: 500;">Date:</span>
                    <input type="date" class="search-input" id="errorDatePicker" onchange="filterErrorDate(this.value)" style="width: 135px; padding: 3px 8px; font-size: 11px; background: #0f172a; color: #f8fafc; border: 1px solid #334155; border-radius: 4px; cursor: pointer;">
                    <button id="todayBtn" onclick="selectToday()" class="page-btn active" style="font-size: 11px; padding: 3px 8px;">Today</button>
                    <button id="yesterdayBtn" onclick="selectYesterday()" class="page-btn" style="font-size: 11px; padding: 3px 8px;">Yesterday</button>
                </div>
                <input type="text" class="search-input" id="errorSearchInput" placeholder="Search error logs..." oninput="searchErrors(this.value)" style="width: 150px; padding: 3px 8px; font-size: 11px;">
                <button onclick="simulateTestError()" class="page-btn" style="color: #38bdf8; font-size: 11px; padding: 3px 8px; border: 1px solid #0284c755;" title="Click to inject a simulated test error and verify chart & table update in real time">🧪 Test Live Error</button>
                <button onclick="clearErrorLog()" class="page-btn" style="color: #94a3b8; font-size: 11px; padding: 3px 8px;">Clear Log</button>
            </div>
        </div>

        <div style="margin-bottom: 10px; font-size: 12px; color: #94a3b8; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px;">
            <span>Hourly Error Distribution for Selected Date (Hours 1 to 24 • 12H Helper Included):</span>
            <span id="errorSummaryNote" style="color: #cbd5e1; font-weight: 500;"></span>
        </div>
        <div class="chart-box" style="height: 180px; margin-bottom: 18px;">
            <canvas id="errorTimelineChart"></canvas>
        </div>

        <table>
            <thead>
                <tr>
                    <th style="width: 260px;">Date & Time (24h / 12h)</th>
                    <th style="width: 160px;">User / Stream</th>
                    <th>Action</th>
                    <th style="width: 100px;">Status</th>
                    <th>Details</th>
                </tr>
            </thead>
            <tbody id="errorLogBody">
                <tr><td colspan="5" style="color: #22c55e; text-align: center;">No errors. 100% successful responses.</td></tr>
            </tbody>
        </table>
        <div class="pagination-container" id="errorPaginationContainer" style="display: none;">
            <div style="display: flex; align-items: center; gap: 14px;">
                <div class="page-info" id="errorPageInfo">Showing 1 to 5 of 0 errors</div>
                <div style="display: flex; align-items: center; gap: 6px;">
                    <span style="font-size: 11px; color: #94a3b8;">Per page:</span>
                    <select id="errorPageSizeSelect" onchange="changeErrorPageSize(this.value)" style="background: #0f172a; color: #f8fafc; border: 1px solid #334155; border-radius: 4px; padding: 2px 6px; font-size: 11px; outline: none; cursor: pointer;">
                        <option value="5" selected>5</option>
                        <option value="10">10</option>
                        <option value="25">25</option>
                        <option value="50">50</option>
                    </select>
                </div>
            </div>
            <div class="page-buttons" id="errorPageButtons"></div>
        </div>
    </div>

    <script>
        let trafficChart = null;
        let hardwareChart = null;
        let roleChart = null;
        let errorTimelineChart = null;

        function initCharts() {
            if (typeof Chart === 'undefined') {
                setTimeout(initCharts, 150);
                return;
            }

            const errCanvas = document.getElementById('errorTimelineChart');
            if (errCanvas) {
                const errCtx = errCanvas.getContext('2d');
                errorTimelineChart = new Chart(errCtx, {
                    type: 'line',
                    data: {
                        labels: [],
                        datasets: [{
                            label: 'Errors Recorded',
                            borderColor: '#ef4444',
                            backgroundColor: 'rgba(239, 68, 68, 0.2)',
                            borderWidth: 2,
                            fill: true,
                            tension: 0.35,
                            pointRadius: 4,
                            pointHoverRadius: 7,
                            pointBackgroundColor: '#ef4444',
                            data: []
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        animation: false,
                        scales: {
                            x: {
                                grid: { color: '#1e293b' },
                                ticks: { color: '#94a3b8', font: { size: 10 }, maxRotation: 45, minRotation: 0 }
                            },
                            y: {
                                beginAtZero: true,
                                grid: { color: '#1e293b' },
                                ticks: { color: '#94a3b8', precision: 0 }
                            }
                        },
                        plugins: {
                            legend: { display: false },
                            tooltip: {
                                backgroundColor: '#0f172a',
                                titleColor: '#38bdf8',
                                bodyColor: '#f8fafc',
                                borderColor: '#334155',
                                borderWidth: 1,
                                callbacks: {
                                    title: (items) => {
                                        const it = items[0];
                                        const raw = it.label;
                                        const hourNum = it.dataIndex + 1;
                                        if (Array.isArray(raw)) {
                                            return `Hour ${hourNum} of 24: ${raw[0]} (${raw[1]})`;
                                        } else if (typeof raw === 'string' && raw.includes(',')) {
                                            const parts = raw.split(',');
                                            return `Hour ${hourNum} of 24: ${parts[0]} (${parts[1]})`;
                                        }
                                        return `Hour ${hourNum} of 24: ${raw}`;
                                    },
                                    label: (item) => `Errors: ${item.raw.toLocaleString()}`
                                }
                            }
                        }
                    }
                });
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
                    labels: ['Customer Support (25)', 'Supervisors (15)', 'Branch Managers (7)', 'Super Admins (3)', 'API Intake'],
                    datasets: [{
                        data: [1, 1, 1, 1, 1],
                        backgroundColor: ['#10b981', '#3b82f6', '#8b5cf6', '#ef4444', '#f59e0b'],
                        borderWidth: 0
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    animation: false,
                    plugins: {
                        legend: { position: 'right', labels: { color: '#94a3b8', boxWidth: 10, font: { size: 10 } } }
                    }
                }
            });
        }

        let currentStaffPage = 1;
        let staffPageSize = 10;
        let cachedStaff = [];
        let selectedTierFilter = 'ALL';
        let staffSearchQuery = '';

        window.filterStaffTier = function(tier) {
            selectedTierFilter = tier;
            currentStaffPage = 1;
            renderStaffTable();
        };

        window.searchStaff = function(query) {
            staffSearchQuery = (query || '').toLowerCase().trim();
            currentStaffPage = 1;
            renderStaffTable();
        };

        window.changeStaffPageSize = function(size) {
            staffPageSize = parseInt(size, 10);
            currentStaffPage = 1;
            renderStaffTable();
        };

        window.changeStaffPage = function(page) {
            currentStaffPage = page;
            renderStaffTable();
        };

        function renderStaffTable() {
            const tbody = document.getElementById('staffTableBody');
            const pageInfo = document.getElementById('staffPageInfo');
            const pageButtons = document.getElementById('staffPageButtons');
            const totalBadge = document.getElementById('staffTotalBadge');

            let filtered = cachedStaff;
            if (selectedTierFilter !== 'ALL') {
                filtered = filtered.filter(s => s.tier === selectedTierFilter);
            }
            if (staffSearchQuery) {
                filtered = filtered.filter(s => 
                    s.name.toLowerCase().includes(staffSearchQuery) || 
                    s.username.toLowerCase().includes(staffSearchQuery) ||
                    s.role.toLowerCase().includes(staffSearchQuery)
                );
            }

            if (totalBadge) totalBadge.textContent = `${filtered.length} of ${cachedStaff.length} Staff`;

            if (filtered.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" style="color: #94a3b8; text-align: center;">No matching staff members found.</td></tr>';
                if (pageInfo) pageInfo.textContent = 'Showing 0 to 0 of 0';
                if (pageButtons) pageButtons.innerHTML = '';
                return;
            }

            const totalPages = Math.ceil(filtered.length / staffPageSize);
            if (currentStaffPage > totalPages) currentStaffPage = totalPages;
            if (currentStaffPage < 1) currentStaffPage = 1;

            const startIdx = (currentStaffPage - 1) * staffPageSize;
            const endIdx = Math.min(startIdx + staffPageSize, filtered.length);
            const pageItems = filtered.slice(startIdx, endIdx);

            tbody.innerHTML = pageItems.map(adm => `
                <tr>
                    <td style="font-weight: 600;">
                        <div>${adm.name}</div>
                        <div style="font-size: 11px; color: #64748b;">@${adm.username}</div>
                    </td>
                    <td>
                        <span class="status-pill" style="background: ${adm.role_badge}22; color: ${adm.role_badge}; border: 1px solid ${adm.role_badge}55;">${adm.role}</span>
                    </td>
                    <td style="font-weight: 700; color: #38bdf8;">${adm.requests.toLocaleString()}</td>
                    <td style="color: #cbd5e1; font-size: 12px;">${adm.last_action}</td>
                    <td style="color: #64748b; font-size: 11px;">${adm.last_timestamp}</td>
                </tr>
            `).join('');

            if (pageInfo) pageInfo.textContent = `Showing ${startIdx + 1} to ${endIdx} of ${filtered.length} staff`;

            if (pageButtons) {
                let btns = '';
                btns += `<button class="page-btn" onclick="changeStaffPage(${currentStaffPage - 1})" ${currentStaffPage === 1 ? 'disabled' : ''}>‹ Prev</button>`;
                for (let p = 1; p <= totalPages; p++) {
                    btns += `<button class="page-btn ${p === currentStaffPage ? 'active' : ''}" onclick="changeStaffPage(${p})">${p}</button>`;
                }
                btns += `<button class="page-btn" onclick="changeStaffPage(${currentStaffPage + 1})" ${currentStaffPage === totalPages ? 'disabled' : ''}>Next ›</button>`;
                pageButtons.innerHTML = btns;
            }
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
                    cachedStaff = d.admin_stats;
                    renderStaffTable();
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
                        roleChart.data.labels = d.charts.tier_labels;
                        roleChart.data.datasets[0].data = d.charts.tier_counts;
                        roleChart.update();
                    }
                }

                // Update error timeline chart & peak stats
                if (d.error_timeline) {
                    latestTimelineData = d.error_timeline;
                    const avail = latestTimelineData.by_date ? Object.keys(latestTimelineData.by_date) : [];
                    if (!selectedErrorDate || (latestTimelineData.by_date && !latestTimelineData.by_date[selectedErrorDate])) {
                        if (avail.length > 0) {
                            selectedErrorDate = avail[0];
                        }
                    }
                    const picker = document.getElementById('errorDatePicker');
                    if (picker && picker.value !== selectedErrorDate) picker.value = selectedErrorDate;
                    updateDateButtons();
                    updateErrorTimelineChart();
                }

                // Responses & dropouts table
                if (d.recent_errors) {
                    cachedErrors = d.recent_errors;
                    renderErrorTable();
                }
            } catch (err) {}
        }

        let currentErrorPage = 1;
        let errorPageSize = 5;
        let cachedErrors = [];
        let selectedErrorDate = '';
        let errorSearchQuery = '';
        let latestTimelineData = null;
        let currentErrorChartType = 'line';

        function getLocalDateStr(d = new Date()) {
            const y = d.getFullYear();
            const m = String(d.getMonth() + 1).padStart(2, '0');
            const day = String(d.getDate()).padStart(2, '0');
            return `${y}-${m}-${day}`;
        }

        function updateDateButtons() {
            const todayBtn = document.getElementById('todayBtn');
            const yesterdayBtn = document.getElementById('yesterdayBtn');
            const avail = latestTimelineData && latestTimelineData.by_date ? Object.keys(latestTimelineData.by_date) : [];
            const todayStr = avail.length > 0 ? avail[0] : getLocalDateStr();
            const yesterdayStr = avail.length > 1 ? avail[1] : '';
            if (todayBtn) todayBtn.classList.toggle('active', selectedErrorDate === todayStr);
            if (yesterdayBtn) yesterdayBtn.classList.toggle('active', selectedErrorDate === yesterdayStr);
        }

        window.selectToday = function() {
            const avail = latestTimelineData && latestTimelineData.by_date ? Object.keys(latestTimelineData.by_date) : [];
            const todayStr = avail.length > 0 ? avail[0] : getLocalDateStr();
            filterErrorDate(todayStr);
        };

        window.selectYesterday = function() {
            const avail = latestTimelineData && latestTimelineData.by_date ? Object.keys(latestTimelineData.by_date) : [];
            if (avail.length > 1) {
                filterErrorDate(avail[1]);
            } else {
                const y = new Date();
                y.setDate(y.getDate() - 1);
                filterErrorDate(getLocalDateStr(y));
            }
        };

        window.filterErrorDate = function(dateStr) {
            if (!dateStr) return;
            selectedErrorDate = dateStr.trim();
            currentErrorPage = 1;
            const picker = document.getElementById('errorDatePicker');
            if (picker && picker.value !== selectedErrorDate) picker.value = selectedErrorDate;
            updateDateButtons();
            updateErrorTimelineChart();
            renderErrorTable();
        };

        window.setErrorChartType = function(type) {
            currentErrorChartType = type;
            const areaBtn = document.getElementById('chartTypeAreaBtn');
            const barBtn = document.getElementById('chartTypeBarBtn');
            if (areaBtn && barBtn) {
                if (type === 'line') {
                    areaBtn.classList.add('active');
                    barBtn.classList.remove('active');
                } else {
                    barBtn.classList.add('active');
                    areaBtn.classList.remove('active');
                }
            }
            if (!errorTimelineChart) return;
            const currentLabels = errorTimelineChart.data.labels;
            const currentData = errorTimelineChart.data.datasets[0].data;
            errorTimelineChart.destroy();
            const errCanvas = document.getElementById('errorTimelineChart');
            const errCtx = errCanvas.getContext('2d');

            const datasetConfig = type === 'line' ? {
                label: 'Errors Recorded',
                borderColor: '#ef4444',
                backgroundColor: 'rgba(239, 68, 68, 0.2)',
                borderWidth: 2,
                fill: true,
                tension: 0.35,
                pointRadius: 4,
                pointHoverRadius: 7,
                pointBackgroundColor: currentData.map(c => c > 0 ? '#ef4444' : '#22c55e'),
                data: currentData
            } : {
                label: 'Errors Recorded',
                backgroundColor: currentData.map(c => c > 500 ? 'rgba(239, 68, 68, 0.9)' : (c > 0 ? 'rgba(239, 68, 68, 0.6)' : 'rgba(34, 197, 94, 0.2)')),
                borderColor: '#ef4444',
                borderWidth: 1,
                borderRadius: 3,
                data: currentData
            };

            errorTimelineChart = new Chart(errCtx, {
                type: type,
                data: {
                    labels: currentLabels,
                    datasets: [datasetConfig]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    animation: false,
                    scales: {
                        x: {
                            grid: { color: '#1e293b' },
                            ticks: { color: '#94a3b8', font: { size: 10 }, maxRotation: 45, minRotation: 0 }
                        },
                        y: {
                            beginAtZero: true,
                            grid: { color: '#1e293b' },
                            ticks: { color: '#94a3b8', precision: 0 }
                        }
                    },
                    plugins: {
                        legend: { display: false },
                        tooltip: {
                            backgroundColor: '#0f172a',
                            titleColor: '#38bdf8',
                            bodyColor: '#f8fafc',
                            borderColor: '#334155',
                            borderWidth: 1,
                            callbacks: {
                                title: (items) => {
                                    const it = items[0];
                                    const raw = it.label;
                                    const hourNum = it.dataIndex + 1;
                                    if (Array.isArray(raw)) {
                                        return `Hour ${hourNum} of 24: ${raw[0]} (${raw[1]})`;
                                    } else if (typeof raw === 'string' && raw.includes(',')) {
                                        const parts = raw.split(',');
                                        return `Hour ${hourNum} of 24: ${parts[0]} (${parts[1]})`;
                                    }
                                    return `Hour ${hourNum} of 24: ${raw}`;
                                },
                                label: (item) => `Errors: ${item.raw.toLocaleString()}`
                            }
                        }
                    }
                }
            });
        };

        function updateErrorTimelineChart() {
            if (!errorTimelineChart || !latestTimelineData) return;

            let labels = [];
            let counts = [];
            let peakText = '';
            let totalVal = 0;

            const dateObj = latestTimelineData.by_date && latestTimelineData.by_date[selectedErrorDate];
            if (dateObj) {
                labels = dateObj.labels || [];
                counts = dateObj.counts || [];
                totalVal = dateObj.total || 0;
                peakText = dateObj.peak_count > 0 ? `Peak (${selectedErrorDate}): ${dateObj.peak_hour} (${dateObj.peak_count.toLocaleString()} errs)` : `Peak (${selectedErrorDate}): None (0 Errors)`;
            } else {
                labels = [
                    ['00:00', '12 AM'], ['01:00', '1 AM'], ['02:00', '2 AM'], ['03:00', '3 AM'],
                    ['04:00', '4 AM'], ['05:00', '5 AM'], ['06:00', '6 AM'], ['07:00', '7 AM'],
                    ['08:00', '8 AM'], ['09:00', '9 AM'], ['10:00', '10 AM'], ['11:00', '11 AM'],
                    ['12:00', '12 PM'], ['13:00', '1 PM'], ['14:00', '2 PM'], ['15:00', '3 PM'],
                    ['16:00', '4 PM'], ['17:00', '5 PM'], ['18:00', '6 PM'], ['19:00', '7 PM'],
                    ['20:00', '8 PM'], ['21:00', '9 PM'], ['22:00', '10 PM'], ['23:00', '11 PM']
                ];
                counts = new Array(24).fill(0);
                totalVal = 0;
                peakText = `Peak (${selectedErrorDate}): None (0 Errors)`;
            }

            errorTimelineChart.data.labels = labels;
            errorTimelineChart.data.datasets[0].data = counts;
            if (currentErrorChartType === 'line') {
                errorTimelineChart.data.datasets[0].pointBackgroundColor = counts.map(c => c > 0 ? '#ef4444' : '#22c55e');
            } else {
                errorTimelineChart.data.datasets[0].backgroundColor = counts.map(c => c > 500 ? 'rgba(239, 68, 68, 0.9)' : (c > 0 ? 'rgba(239, 68, 68, 0.6)' : 'rgba(34, 197, 94, 0.2)'));
            }
            errorTimelineChart.update();

            const peakBadge = document.getElementById('errorPeakBadge');
            if (peakBadge) peakBadge.textContent = peakText;

            const note = document.getElementById('errorSummaryNote');
            if (note) {
                note.textContent = `Date ${selectedErrorDate}: ${totalVal.toLocaleString()} errors across all 24 hours (Hours 1 to 24 • 12H Helper Included)`;
            }
        }

        window.searchErrors = function(q) {
            errorSearchQuery = (q || '').toLowerCase().trim();
            currentErrorPage = 1;
            renderErrorTable();
        };

        window.changeErrorPageSize = function(size) {
            errorPageSize = parseInt(size, 10);
            currentErrorPage = 1;
            renderErrorTable();
        };

        window.changeErrorPage = function(page) {
            currentErrorPage = page;
            renderErrorTable();
        };

        window.simulateTestError = async function() {
            try {
                const res = await fetch('/api/errors/simulate-test', { method: 'POST' });
                const data = await res.json();
                showToast(data.message || 'Test error triggered!');
                await fetchMetrics();
            } catch (err) {
                showToast('Failed to trigger test error: ' + err);
            }
        };

        function showToast(msg) {
            let toast = document.getElementById('testToast');
            if (!toast) {
                toast = document.createElement('div');
                toast.id = 'testToast';
                toast.style.position = 'fixed';
                toast.style.bottom = '20px';
                toast.style.right = '20px';
                toast.style.background = '#0284c7';
                toast.style.color = '#ffffff';
                toast.style.padding = '12px 20px';
                toast.style.borderRadius = '8px';
                toast.style.fontSize = '12px';
                toast.style.fontWeight = '600';
                toast.style.boxShadow = '0 10px 25px rgba(0,0,0,0.5)';
                toast.style.zIndex = '9999';
                toast.style.transition = 'opacity 0.3s ease';
                document.body.appendChild(toast);
            }
            toast.textContent = msg;
            toast.style.opacity = '1';
            setTimeout(() => { toast.style.opacity = '0'; }, 4000);
        }

        window.clearErrorLog = async function() {
            try {
                await fetch('/api/errors/clear', { method: 'POST' });
            } catch (e) {}
            cachedErrors = [];
            currentErrorPage = 1;
            if (errorTimelineChart) {
                errorTimelineChart.data.labels = [];
                errorTimelineChart.data.datasets[0].data = [];
                errorTimelineChart.update();
            }
            renderErrorTable();
            const badge = document.getElementById('errorTotalBadge');
            if (badge) badge.textContent = '0 Recorded';
            const peakBadge = document.getElementById('errorPeakBadge');
            if (peakBadge) peakBadge.textContent = 'Peak: None (0 Errors)';
        };

        function renderErrorTable() {
            const errorTbody = document.getElementById('errorLogBody');
            const paginationContainer = document.getElementById('errorPaginationContainer');
            const pageInfo = document.getElementById('errorPageInfo');
            const pageButtons = document.getElementById('errorPageButtons');
            const totalBadge = document.getElementById('errorTotalBadge');

            let filtered = cachedErrors;
            if (selectedErrorDate) {
                filtered = filtered.filter(err => (err.date_str === selectedErrorDate) || (err.timestamp && err.timestamp.startsWith(selectedErrorDate)));
            }
            if (errorSearchQuery) {
                filtered = filtered.filter(err => 
                    (err.user && err.user.toLowerCase().includes(errorSearchQuery)) ||
                    (err.action && err.action.toLowerCase().includes(errorSearchQuery)) ||
                    (err.message && err.message.toLowerCase().includes(errorSearchQuery)) ||
                    (err.timestamp && err.timestamp.toLowerCase().includes(errorSearchQuery))
                );
            }

            const total = filtered.length;
            if (totalBadge) {
                totalBadge.textContent = `${total.toLocaleString()} on ${selectedErrorDate || 'Selected Date'}`;
            }

            if (total === 0) {
                errorTbody.innerHTML = '<tr><td colspan="5" style="color: #22c55e; text-align: center;">No errors recorded for this date.</td></tr>';
                if (paginationContainer) paginationContainer.style.display = 'none';
                return;
            }

            if (paginationContainer) paginationContainer.style.display = 'flex';
            const totalPages = Math.ceil(total / errorPageSize);
            if (currentErrorPage > totalPages) currentErrorPage = totalPages;
            if (currentErrorPage < 1) currentErrorPage = 1;

            const startIdx = (currentErrorPage - 1) * errorPageSize;
            const endIdx = Math.min(startIdx + errorPageSize, total);
            const pageItems = filtered.slice(startIdx, endIdx);

            errorTbody.innerHTML = pageItems.map(err => `
                <tr>
                    <td style="color: #cbd5e1; font-size: 12px; font-family: monospace; white-space: nowrap;">
                        <span style="font-weight: 600; color: #f8fafc;">${err.timestamp}</span>
                        ${err.timestamp_12h ? `<span style="color: #38bdf8; font-size: 11px; margin-left: 8px; font-weight: 500;">(${err.timestamp_12h})</span>` : ''}
                    </td>
                    <td style="font-weight: 600; color: #f87171;">${err.user}</td>
                    <td>${err.action}</td>
                    <td><span class="status-pill" style="background: ${err.status >= 500 ? '#ef444422' : '#f59e0b22'}; color: ${err.status >= 500 ? '#ef4444' : '#f59e0b'}; border: 1px solid ${err.status >= 500 ? '#ef444455' : '#f59e0b55'};">${err.status || 'TIMEOUT'}</span></td>
                    <td style="color: #e2e8f0; font-size: 12px;">${err.message}</td>
                </tr>
            `).join('');

            if (pageInfo) pageInfo.textContent = `Showing ${startIdx + 1} to ${endIdx} of ${total} errors`;

            if (pageButtons) {
                let btns = '';
                btns += `<button class="page-btn" onclick="changeErrorPage(${currentErrorPage - 1})" ${currentErrorPage === 1 ? 'disabled' : ''}>‹ Prev</button>`;
                const maxButtons = 5;
                let startPage = Math.max(1, currentErrorPage - 2);
                let endPage = Math.min(totalPages, startPage + maxButtons - 1);
                if (endPage - startPage < maxButtons - 1) {
                    startPage = Math.max(1, endPage - maxButtons + 1);
                }
                for (let p = startPage; p <= endPage; p++) {
                    btns += `<button class="page-btn ${p === currentErrorPage ? 'active' : ''}" onclick="changeErrorPage(${p})">${p}</button>`;
                }
                btns += `<button class="page-btn" onclick="changeErrorPage(${currentErrorPage + 1})" ${currentErrorPage === totalPages ? 'disabled' : ''}>Next ›</button>`;
                pageButtons.innerHTML = btns;
            }
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

async def handle_dashboard_page(request):
    return web.Response(text=HTML_PAGE, content_type="text/html", headers={"Cache-Control": "no-cache, no-store, must-revalidate"})

async def handle_metrics_api(request):
    pool_size = len(simulator_engine.customer_id_pool) if simulator_engine else 0
    return web.json_response(metrics.get_summary(pool_size), headers={"Cache-Control": "no-cache, no-store, must-revalidate"})

async def handle_clear_errors(request):
    metrics.clear_errors()
    return web.json_response({"success": True, "message": "Error log cleared"}, headers={"Cache-Control": "no-cache, no-store, must-revalidate"})

async def handle_simulate_test_error(request):
    now_dt = get_app_now()
    ts_24h = now_dt.strftime("%Y-%m-%d %H:%M:%S")
    h_int = now_dt.hour
    helper_12h = to_12h_short(h_int)
    metrics.record_admin_activity(
        "inspector_test",
        "telemetry_check",
        "Live Telemetry Ping",
        500,
        15.5,
        f"Simulated test error: Live real-time update verified at {ts_24h} ({helper_12h})"
    )
    return web.json_response({
        "success": True,
        "message": f"Test error recorded at {ts_24h} (Hour {h_int:02d}:00 • {helper_12h})! Chart & table updated live."
    }, headers={"Cache-Control": "no-cache, no-store, must-revalidate"})

async def handle_chart_js(request):
    if os.path.exists(CHART_JS_PATH):
        return web.FileResponse(CHART_JS_PATH)
    return web.Response(text="// Chart.js placeholder", content_type="application/javascript")

async def run_web_dashboard(port: int):
    app = web.Application()
    app.router.add_get("/", handle_dashboard_page)
    app.router.add_get("/api/metrics", handle_metrics_api)
    app.router.add_post("/api/errors/clear", handle_clear_errors)
    app.router.add_get("/api/errors/clear", handle_clear_errors)
    app.router.add_post("/api/errors/simulate-test", handle_simulate_test_error)
    app.router.add_get("/api/errors/simulate-test", handle_simulate_test_error)
    app.router.add_get("/static/chart.js", handle_chart_js)
    runner = web.AppRunner(app)
    await runner.setup()
    site = web.TCPSite(runner, "0.0.0.0", port)
    await site.start()

def main():
    global simulator_engine
    parser = argparse.ArgumentParser(description="Twekl Dashboard 24/7 Traffic Simulator & Telemetry")
    parser.add_argument("--host", default="http://localhost:8080", help="Base URL of Twekl Dashboard application")
    parser.add_argument("--rate", type=int, default=16, help="Target Requests Per Second (RPS)")
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
