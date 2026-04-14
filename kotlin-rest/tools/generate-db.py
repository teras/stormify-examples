#!/usr/bin/env python3
"""Generates a populated SQLite database for the kotlin-rest demo.

Produces data/warehouse.db with realistic fake data across all tables.
Run from the kotlin-rest directory:

    python3 tools/generate-db.py

Re-running overwrites the existing database. A fixed RNG seed keeps the
output reproducible.
"""

import os
import random
import sqlite3
from datetime import datetime, timedelta, timezone
from pathlib import Path

from faker import Faker

SEED = 42
DB_PATH = Path("data/warehouse.db")

# Row counts
N_CATEGORIES = 100
N_SUPPLIERS = 20
N_CUSTOMERS = 50
N_WAREHOUSES = 10
N_PRODUCTS = 300
N_STOCK_ITEMS = 30
N_PURCHASE_ORDERS = 50
N_SALES_ORDERS = 150
N_SHIPMENTS = 42

# Time window: Jan 1 2026 — Jun 30 2026
DATE_START = datetime(2026, 1, 1, tzinfo=timezone.utc)
DATE_END = datetime(2026, 6, 30, 23, 59, 59, tzinfo=timezone.utc)

fake = Faker("en_US")
Faker.seed(SEED)
random.seed(SEED)

# Realistic product vocabularies keyed loosely per category family.
PRODUCT_FAMILIES = [
    ("Audio", ["Bluetooth Speaker", "Wireless Earbuds", "Studio Headphones", "Soundbar", "Portable Radio"]),
    ("Display", ["4K Monitor", "OLED TV", "Digital Signage Panel", "Projector", "Smart Display"]),
    ("Computing", ["Laptop", "Desktop Tower", "Mini PC", "Workstation", "Chromebook"]),
    ("Peripherals", ["Mechanical Keyboard", "Ergonomic Mouse", "Webcam HD", "USB-C Hub", "Docking Station"]),
    ("Storage", ["NVMe SSD", "External HDD", "USB Flash Drive", "RAID Enclosure", "SD Card"]),
    ("Networking", ["Wi-Fi Router", "Mesh System", "Network Switch", "Powerline Adapter", "VPN Gateway"]),
    ("Mobile", ["Smartphone", "Tablet", "E-Reader", "Smartwatch", "Phone Charger"]),
    ("Photography", ["DSLR Camera", "Mirrorless Camera", "Action Cam", "Tripod", "Camera Lens"]),
    ("Gaming", ["Gaming Console", "Game Controller", "VR Headset", "Gaming Chair", "RGB Mousepad"]),
    ("Home Office", ["Standing Desk", "Office Chair", "Desk Lamp", "Paper Shredder", "Label Printer"]),
    ("Kitchen", ["Coffee Maker", "Blender", "Air Fryer", "Electric Kettle", "Toaster Oven"]),
    ("Fitness", ["Treadmill", "Exercise Bike", "Yoga Mat", "Dumbbell Set", "Fitness Tracker"]),
    ("Outdoor", ["Camping Tent", "Sleeping Bag", "Portable Grill", "Hiking Backpack", "Cooler Box"]),
    ("Tools", ["Cordless Drill", "Circular Saw", "Tool Set", "Heat Gun", "Laser Level"]),
    ("Lighting", ["LED Strip", "Smart Bulb", "Pendant Lamp", "Floor Lamp", "Outdoor Spotlight"]),
]

MODEL_SUFFIXES = ["Pro", "Lite", "Max", "Mini", "Plus", "X", "Ultra", "Classic", "Elite", "Studio"]
MODEL_NUMBERS = ["100", "200", "300", "500", "700", "1000", "X1", "X2", "S", "Z"]

CARRIERS = ["DHL", "UPS", "FedEx", "USPS", "GLS", "DPD", "TNT", "Aramex", "PostNL", "Royal Mail"]


def iso_ts(dt: datetime) -> str:
    """Format timestamp so SQLite TEXT column parses back cleanly on the Kotlin side."""
    return dt.astimezone(timezone.utc).isoformat().replace("+00:00", "Z")


def random_dt(start: datetime = DATE_START, end: datetime = DATE_END) -> datetime:
    delta = end - start
    seconds = random.randint(0, int(delta.total_seconds()))
    return start + timedelta(seconds=seconds)


def create_schema(cur: sqlite3.Cursor) -> None:
    cur.executescript(
        """
        CREATE TABLE category (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            description TEXT NOT NULL,
            active INTEGER NOT NULL
        );
        CREATE TABLE supplier (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            contact_name TEXT NOT NULL,
            email TEXT NOT NULL,
            phone TEXT NOT NULL,
            city TEXT NOT NULL,
            country TEXT NOT NULL,
            active INTEGER NOT NULL
        );
        CREATE TABLE customer (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            email TEXT NOT NULL,
            phone TEXT NOT NULL,
            city TEXT NOT NULL,
            country TEXT NOT NULL,
            customer_type INTEGER NOT NULL,
            active INTEGER NOT NULL
        );
        CREATE TABLE warehouse (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            code TEXT NOT NULL,
            name TEXT NOT NULL,
            city TEXT NOT NULL,
            country TEXT NOT NULL,
            active INTEGER NOT NULL
        );
        CREATE TABLE product (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            sku TEXT NOT NULL,
            name TEXT NOT NULL,
            description TEXT NOT NULL,
            category_id INTEGER,
            supplier_id INTEGER,
            unit_price REAL NOT NULL,
            reorder_level INTEGER NOT NULL,
            active INTEGER NOT NULL,
            FOREIGN KEY(category_id) REFERENCES category(id),
            FOREIGN KEY(supplier_id) REFERENCES supplier(id)
        );
        CREATE TABLE stock_item (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            warehouse_id INTEGER,
            product_id INTEGER,
            quantity_on_hand INTEGER NOT NULL,
            quantity_reserved INTEGER NOT NULL,
            last_updated_at TEXT NOT NULL,
            FOREIGN KEY(warehouse_id) REFERENCES warehouse(id),
            FOREIGN KEY(product_id) REFERENCES product(id)
        );
        CREATE TABLE purchase_order (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            order_number TEXT NOT NULL,
            supplier_id INTEGER,
            warehouse_id INTEGER,
            status INTEGER NOT NULL,
            ordered_at TEXT NOT NULL,
            expected_at TEXT NOT NULL,
            received_at TEXT NOT NULL,
            notes TEXT NOT NULL,
            FOREIGN KEY(supplier_id) REFERENCES supplier(id),
            FOREIGN KEY(warehouse_id) REFERENCES warehouse(id)
        );
        CREATE TABLE purchase_order_item (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            purchase_order_id INTEGER,
            product_id INTEGER,
            quantity INTEGER NOT NULL,
            unit_cost REAL NOT NULL,
            line_total REAL NOT NULL,
            FOREIGN KEY(purchase_order_id) REFERENCES purchase_order(id),
            FOREIGN KEY(product_id) REFERENCES product(id)
        );
        CREATE TABLE sales_order (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            order_number TEXT NOT NULL,
            customer_id INTEGER,
            warehouse_id INTEGER,
            status INTEGER NOT NULL,
            ordered_at TEXT NOT NULL,
            confirmed_at TEXT NOT NULL,
            notes TEXT NOT NULL,
            FOREIGN KEY(customer_id) REFERENCES customer(id),
            FOREIGN KEY(warehouse_id) REFERENCES warehouse(id)
        );
        CREATE TABLE sales_order_item (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            sales_order_id INTEGER,
            product_id INTEGER,
            quantity INTEGER NOT NULL,
            unit_price REAL NOT NULL,
            line_total REAL NOT NULL,
            FOREIGN KEY(sales_order_id) REFERENCES sales_order(id),
            FOREIGN KEY(product_id) REFERENCES product(id)
        );
        CREATE TABLE shipment (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            shipment_number TEXT NOT NULL,
            sales_order_id INTEGER,
            warehouse_id INTEGER,
            status INTEGER NOT NULL,
            carrier TEXT NOT NULL,
            tracking_code TEXT NOT NULL,
            shipped_at TEXT NOT NULL,
            delivered_at TEXT NOT NULL,
            FOREIGN KEY(sales_order_id) REFERENCES sales_order(id),
            FOREIGN KEY(warehouse_id) REFERENCES warehouse(id)
        );
        """
    )


def gen_categories(cur: sqlite3.Cursor) -> list[int]:
    ids = []
    used = set()
    # First, one category per family for richer product mapping.
    for family, _ in PRODUCT_FAMILIES:
        if family in used:
            continue
        used.add(family)
        cur.execute(
            "INSERT INTO category (name, description, active) VALUES (?, ?, ?)",
            (family, fake.sentence(nb_words=8), 1),
        )
        ids.append(cur.lastrowid)
    while len(ids) < N_CATEGORIES:
        name = f"{fake.word().capitalize()} {fake.word().capitalize()}"
        if name in used:
            continue
        used.add(name)
        cur.execute(
            "INSERT INTO category (name, description, active) VALUES (?, ?, ?)",
            (name, fake.sentence(nb_words=8), 1 if random.random() > 0.05 else 0),
        )
        ids.append(cur.lastrowid)
    return ids


def gen_suppliers(cur: sqlite3.Cursor) -> list[int]:
    ids = []
    for _ in range(N_SUPPLIERS):
        cur.execute(
            "INSERT INTO supplier (name, contact_name, email, phone, city, country, active) "
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            (
                fake.company(),
                fake.name(),
                fake.company_email(),
                fake.phone_number(),
                fake.city(),
                fake.country(),
                1 if random.random() > 0.05 else 0,
            ),
        )
        ids.append(cur.lastrowid)
    return ids


def gen_customers(cur: sqlite3.Cursor) -> list[int]:
    ids = []
    for _ in range(N_CUSTOMERS):
        is_wholesale = random.random() < 0.4
        name = fake.company() if is_wholesale else fake.name()
        cur.execute(
            "INSERT INTO customer (name, email, phone, city, country, customer_type, active) "
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            (
                name,
                fake.email(),
                fake.phone_number(),
                fake.city(),
                fake.country(),
                1 if is_wholesale else 0,  # RETAIL=0, WHOLESALE=1
                1 if random.random() > 0.05 else 0,
            ),
        )
        ids.append(cur.lastrowid)
    return ids


def gen_warehouses(cur: sqlite3.Cursor) -> list[int]:
    ids = []
    for i in range(N_WAREHOUSES):
        cur.execute(
            "INSERT INTO warehouse (code, name, city, country, active) VALUES (?, ?, ?, ?, ?)",
            (
                f"WH-{i + 1:03d}",
                f"{fake.city()} Distribution Center",
                fake.city(),
                fake.country(),
                1,
            ),
        )
        ids.append(cur.lastrowid)
    return ids


def gen_products(cur: sqlite3.Cursor, category_ids: list[int], supplier_ids: list[int]) -> list[int]:
    ids = []
    # Map the first len(PRODUCT_FAMILIES) category ids to their families so SKUs make sense.
    family_to_cat = {fam: category_ids[i] for i, (fam, _) in enumerate(PRODUCT_FAMILIES)}
    for n in range(N_PRODUCTS):
        family, bases = random.choice(PRODUCT_FAMILIES)
        base = random.choice(bases)
        suffix = random.choice(MODEL_SUFFIXES)
        number = random.choice(MODEL_NUMBERS)
        name = f"{base} {suffix} {number}"
        sku = f"{family[:3].upper()}-{n + 1:04d}"
        cur.execute(
            "INSERT INTO product (sku, name, description, category_id, supplier_id, unit_price, "
            "reorder_level, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            (
                sku,
                name,
                fake.sentence(nb_words=12),
                family_to_cat[family],
                random.choice(supplier_ids),
                round(random.uniform(9.99, 2499.99), 2),
                random.choice([5, 10, 15, 20, 25, 50]),
                1 if random.random() > 0.05 else 0,
            ),
        )
        ids.append(cur.lastrowid)
    return ids


def gen_stock_items(cur: sqlite3.Cursor, warehouse_ids: list[int], product_ids: list[int]) -> None:
    seen: set[tuple[int, int]] = set()
    while len(seen) < N_STOCK_ITEMS:
        pair = (random.choice(warehouse_ids), random.choice(product_ids))
        if pair in seen:
            continue
        seen.add(pair)
        on_hand = random.randint(0, 500)
        cur.execute(
            "INSERT INTO stock_item (warehouse_id, product_id, quantity_on_hand, quantity_reserved, "
            "last_updated_at) VALUES (?, ?, ?, ?, ?)",
            (
                pair[0],
                pair[1],
                on_hand,
                random.randint(0, max(0, on_hand // 4)),
                iso_ts(random_dt()),
            ),
        )


def gen_purchase_orders(
    cur: sqlite3.Cursor,
    supplier_ids: list[int],
    warehouse_ids: list[int],
    product_ids: list[int],
    product_prices: dict[int, float],
) -> list[int]:
    po_ids = []
    statuses = [0, 1, 1, 2, 2, 2, 3]  # weighted: more RECEIVED
    for i in range(N_PURCHASE_ORDERS):
        ordered = random_dt(DATE_START, DATE_END - timedelta(days=30))
        expected = ordered + timedelta(days=random.randint(3, 21))
        status = random.choice(statuses)
        received = expected + timedelta(days=random.randint(-2, 5)) if status == 2 else ordered
        cur.execute(
            "INSERT INTO purchase_order (order_number, supplier_id, warehouse_id, status, ordered_at, "
            "expected_at, received_at, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            (
                f"PO-2026-{i + 1:04d}",
                random.choice(supplier_ids),
                random.choice(warehouse_ids),
                status,
                iso_ts(ordered),
                iso_ts(expected),
                iso_ts(received),
                fake.sentence(nb_words=6) if random.random() < 0.3 else "",
            ),
        )
        po_id = cur.lastrowid
        po_ids.append(po_id)
        # 1–8 line items
        for _ in range(random.randint(1, 8)):
            pid = random.choice(product_ids)
            qty = random.randint(1, 50)
            unit_cost = round(product_prices[pid] * random.uniform(0.55, 0.75), 2)
            cur.execute(
                "INSERT INTO purchase_order_item (purchase_order_id, product_id, quantity, unit_cost, "
                "line_total) VALUES (?, ?, ?, ?, ?)",
                (po_id, pid, qty, unit_cost, round(unit_cost * qty, 2)),
            )
    return po_ids


def gen_sales_orders(
    cur: sqlite3.Cursor,
    customer_ids: list[int],
    warehouse_ids: list[int],
    product_ids: list[int],
    product_prices: dict[int, float],
) -> list[int]:
    so_ids = []
    statuses = [0, 1, 1, 2, 2, 2, 3]  # weighted: more SHIPPED
    for i in range(N_SALES_ORDERS):
        ordered = random_dt()
        status = random.choice(statuses)
        confirmed = ordered + timedelta(hours=random.randint(1, 48)) if status >= 1 else ordered
        cur.execute(
            "INSERT INTO sales_order (order_number, customer_id, warehouse_id, status, ordered_at, "
            "confirmed_at, notes) VALUES (?, ?, ?, ?, ?, ?, ?)",
            (
                f"SO-2026-{i + 1:04d}",
                random.choice(customer_ids),
                random.choice(warehouse_ids),
                status,
                iso_ts(ordered),
                iso_ts(confirmed),
                fake.sentence(nb_words=6) if random.random() < 0.3 else "",
            ),
        )
        so_id = cur.lastrowid
        so_ids.append(so_id)
        for _ in range(random.randint(1, 8)):
            pid = random.choice(product_ids)
            qty = random.randint(1, 20)
            unit_price = round(product_prices[pid] * random.uniform(0.95, 1.1), 2)
            cur.execute(
                "INSERT INTO sales_order_item (sales_order_id, product_id, quantity, unit_price, "
                "line_total) VALUES (?, ?, ?, ?, ?)",
                (so_id, pid, qty, unit_price, round(unit_price * qty, 2)),
            )
    return so_ids


def gen_shipments(cur: sqlite3.Cursor, so_ids: list[int], warehouse_ids: list[int]) -> None:
    # Only create shipments for sales orders that have been confirmed or shipped.
    eligible = [sid for sid in so_ids]
    random.shuffle(eligible)
    for i, sid in enumerate(eligible[:N_SHIPMENTS]):
        shipped = random_dt()
        status = random.choice([0, 1, 1, 2, 2, 2, 3])
        delivered = shipped + timedelta(days=random.randint(1, 10)) if status == 2 else shipped
        cur.execute(
            "INSERT INTO shipment (shipment_number, sales_order_id, warehouse_id, status, carrier, "
            "tracking_code, shipped_at, delivered_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            (
                f"SHP-2026-{i + 1:04d}",
                sid,
                random.choice(warehouse_ids),
                status,
                random.choice(CARRIERS),
                fake.bothify(text="??######").upper(),
                iso_ts(shipped),
                iso_ts(delivered),
            ),
        )


def main() -> None:
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    if DB_PATH.exists():
        DB_PATH.unlink()
    conn = sqlite3.connect(DB_PATH)
    conn.execute("PRAGMA foreign_keys = ON")
    cur = conn.cursor()
    create_schema(cur)

    print("Seeding categories, suppliers, customers, warehouses...")
    category_ids = gen_categories(cur)
    supplier_ids = gen_suppliers(cur)
    customer_ids = gen_customers(cur)
    warehouse_ids = gen_warehouses(cur)

    print("Seeding products...")
    product_ids = gen_products(cur, category_ids, supplier_ids)
    product_prices = {pid: unit_price for pid, unit_price in cur.execute("SELECT id, unit_price FROM product")}

    print("Seeding stock items...")
    gen_stock_items(cur, warehouse_ids, product_ids)

    print("Seeding purchase orders...")
    gen_purchase_orders(cur, supplier_ids, warehouse_ids, product_ids, product_prices)

    print("Seeding sales orders...")
    so_ids = gen_sales_orders(cur, customer_ids, warehouse_ids, product_ids, product_prices)

    print("Seeding shipments...")
    gen_shipments(cur, so_ids, warehouse_ids)

    conn.commit()
    # Compact and index-friendly final form.
    conn.execute("VACUUM")
    conn.close()
    size_kb = DB_PATH.stat().st_size // 1024
    print(f"Wrote {DB_PATH} ({size_kb} KB)")


if __name__ == "__main__":
    main()
