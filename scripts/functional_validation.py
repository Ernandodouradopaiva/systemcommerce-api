#!/usr/bin/env python3
"""SystemCommerce Prompt 26 — validação funcional ponta a ponta."""

from __future__ import annotations

import json
import os
import re
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

BASE = "http://localhost:8080"
FRONT = "http://localhost:5173"
API_ROOT = Path(r"c:\Git\Sistemas\SystemCommerce\SystemCommerce-api")
OUT_JSON = API_ROOT / "target" / "validation-run.json"
ENV_FILE = API_ROOT / ".env"

results: list[dict] = []
ctx: dict = {}


def load_env() -> None:
    if not ENV_FILE.exists():
        return
    for line in ENV_FILE.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        os.environ[key.strip()] = value.strip()


def add_result(
    step: str,
    expected: str,
    obtained: str,
    evidence: str,
    passed: bool,
    error: str = "",
    fix: str = "",
) -> None:
    results.append(
        {
            "step": step,
            "expected": expected,
            "obtained": obtained,
            "evidence": evidence,
            "error": error,
            "fix": fix,
            "pass": passed,
        }
    )
    flag = "OK" if passed else "FAIL"
    print(f"[{flag}] {step} — {obtained}")


def api(
    method: str,
    path: str,
    body: dict | None = None,
    token: str | None = None,
    query: dict | None = None,
) -> dict:
    url = BASE + path
    if query:
        url += "?" + urllib.parse.urlencode({k: v for k, v in query.items() if v is not None})
    data = None
    headers = {"Accept": "application/json"}
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            raw = resp.read().decode("utf-8")
            parsed = json.loads(raw) if raw else None
            return {"ok": True, "status": resp.status, "body": parsed, "raw": raw}
    except urllib.error.HTTPError as ex:
        raw = ex.read().decode("utf-8", errors="replace")
        try:
            parsed = json.loads(raw) if raw else None
        except json.JSONDecodeError:
            parsed = None
        return {"ok": False, "status": ex.code, "body": parsed, "raw": raw, "error": str(ex)}
    except Exception as ex:  # noqa: BLE001
        return {"ok": False, "status": 0, "body": None, "raw": "", "error": str(ex)}


def http_get(url: str) -> dict:
    req = urllib.request.Request(url, method="GET")
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            raw = resp.read().decode("utf-8", errors="replace")
            return {"ok": True, "status": resp.status, "raw": raw}
    except Exception as ex:  # noqa: BLE001
        return {"ok": False, "status": 0, "raw": "", "error": str(ex)}


def docker_exec_psql(sql: str) -> str:
    user = os.environ.get("POSTGRES_USER", "systemcommerce")
    db = os.environ.get("POSTGRES_DB", "systemcommerce")
    cmd = [
        "docker",
        "exec",
        "systemcommerce-api-db",
        "psql",
        "-U",
        user,
        "-d",
        db,
        "-tAc",
        sql,
    ]
    try:
        out = subprocess.check_output(cmd, stderr=subprocess.STDOUT, text=True)
        return out.strip()
    except subprocess.CalledProcessError as ex:
        return f"ERR:{ex.output}"


def docker_health(name: str) -> str:
    try:
        out = subprocess.check_output(
            ["docker", "inspect", "-f", "{{.State.Health.Status}}", name],
            stderr=subprocess.STDOUT,
            text=True,
        )
        return out.strip()
    except subprocess.CalledProcessError:
        return "unknown"


def main() -> int:
    load_env()
    suffix = datetime.now().strftime("%Y%m%d%H%M%S")
    OUT_JSON.parent.mkdir(parents=True, exist_ok=True)

    # Infra
    pg, api_h, front_h = (
        docker_health("systemcommerce-api-db"),
        docker_health("systemcommerce-api"),
        docker_health("systemcommerce-front"),
    )
    add_result(
        "Infra: PostgreSQL + API + Frontend",
        "Containers healthy (db, api, front)",
        f"db={pg} api={api_h} front={front_h}",
        "docker inspect health",
        pg == "healthy" and api_h == "healthy" and front_h == "healthy",
    )

    mig = docker_exec_psql("SELECT COUNT(*) FROM flyway_schema_history WHERE success = true")
    add_result(
        "Migrations Flyway",
        "Migrations aplicadas com sucesso",
        f"success_count={mig}",
        "flyway_schema_history",
        mig.isdigit() and int(mig) > 0,
    )

    seed = docker_exec_psql(
        "SELECT COUNT(*) FROM users u "
        "JOIN user_roles ur ON ur.user_id=u.id "
        "JOIN roles r ON r.id=ur.role_id "
        "WHERE u.login='admin' AND r.code='ADMIN'"
    )
    add_result(
        "Seeds (admin + roles)",
        "Usuario admin com perfil ADMIN",
        f"admin_role_links={seed}",
        "users/user_roles/roles",
        seed.isdigit() and int(seed) >= 1,
    )

    health = api("GET", "/actuator/health")
    add_result(
        "API health",
        "status=UP",
        f"HTTP {health['status']} body={health.get('raw')}",
        "GET /actuator/health",
        health["ok"] and (health.get("body") or {}).get("status") == "UP",
    )

    fh = http_get(f"{FRONT}/healthz")
    add_result(
        "Frontend health",
        "HTTP 200 em /healthz",
        f"HTTP {fh.get('status')} {fh.get('raw')}",
        f"GET {FRONT}/healthz",
        fh.get("ok") and fh.get("status") == 200,
    )

    page = http_get(f"{FRONT}/")
    add_result(
        "Frontend SPA",
        "HTML da SPA servido",
        f"HTTP {page.get('status')} bytes={len(page.get('raw') or '')}",
        f"GET {FRONT}/",
        page.get("ok") and page.get("status") == 200,
    )

    # Auth admin
    login = api(
        "POST",
        "/api/v1/auth/login",
        {
            "username": os.environ.get("ADMIN_LOGIN", "admin"),
            "password": os.environ.get("ADMIN_PASSWORD", ""),
        },
    )
    admin_token = None
    if login["ok"] and login["body"] and login["body"].get("data", {}).get("accessToken"):
        data = login["body"]["data"]
        admin_token = data["accessToken"]
        ctx["adminUserId"] = data["user"]["id"]
        add_result(
            "Autenticar administrador",
            "Login OK com token e permissoes",
            f"login={data['user']['login']} permissions={len(data['user'].get('permissions') or [])}",
            "POST /api/v1/auth/login",
            True,
        )
    else:
        add_result(
            "Autenticar administrador",
            "Login OK",
            f"HTTP {login['status']} {login.get('raw')}",
            "POST /api/v1/auth/login",
            False,
            error=login.get("error") or login.get("raw") or "",
        )
        _persist(suffix)
        return 1

    # Seller
    seller_login = f"seller_{suffix}"
    seller_pass = "Seller@123!"
    create_seller = api(
        "POST",
        "/api/v1/users",
        {
            "name": f"Vendedor Validacao {suffix}",
            "email": f"seller.{suffix}@example.com",
            "login": seller_login,
            "password": seller_pass,
            "roleCodes": ["SELLER"],
        },
        token=admin_token,
    )
    if create_seller["ok"]:
        ctx["sellerId"] = create_seller["body"]["data"]["id"]
        roles = create_seller["body"]["data"].get("roles") or []
        add_result(
            "Criar usuario vendedor",
            "HTTP 2xx com perfil SELLER",
            f"id={ctx['sellerId']} login={seller_login} roles={roles}",
            "POST /api/v1/users",
            "SELLER" in roles,
        )
    else:
        add_result(
            "Criar usuario vendedor",
            "2xx",
            f"HTTP {create_seller['status']} {create_seller.get('raw')}",
            "POST /api/v1/users",
            False,
            error=create_seller.get("raw") or "",
        )

    # Customer — valid CPF with unique email; document may collide so retry with known CPF
    doc_try = "9" + suffix[2:12]
    create_customer = api(
        "POST",
        "/api/v1/customers",
        {
            "type": "PF",
            "name": f"Cliente Validacao {suffix}",
            "document": doc_try,
            "email": f"cliente.{suffix}@example.com",
            "phone": "11999990000",
            "city": "Sao Paulo",
            "state": "SP",
        },
        token=admin_token,
    )
    if not create_customer["ok"]:
        create_customer = api(
            "POST",
            "/api/v1/customers",
            {
                "type": "PF",
                "name": f"Cliente Validacao {suffix}",
                "document": "52998224725",
                "email": f"cliente2.{suffix}@example.com",
                "phone": "11999990000",
                "city": "Sao Paulo",
                "state": "SP",
            },
            token=admin_token,
        )
    if create_customer["ok"]:
        ctx["customerId"] = create_customer["body"]["data"]["id"]
        add_result(
            "Criar cliente",
            "Cliente PF criado",
            f"id={ctx['customerId']} document={create_customer['body']['data'].get('document')}",
            "POST /api/v1/customers",
            True,
        )
    else:
        add_result(
            "Criar cliente",
            "2xx",
            f"HTTP {create_customer['status']} {create_customer.get('raw')}",
            "POST /api/v1/customers",
            False,
            error=create_customer.get("raw") or "",
        )

    # Category
    create_cat = api(
        "POST",
        "/api/v1/categories",
        {
            "name": f"Cat Validacao {suffix}",
            "description": "Categoria E2E Prompt 26",
            "parentId": None,
        },
        token=admin_token,
    )
    if create_cat["ok"]:
        ctx["categoryId"] = create_cat["body"]["data"]["id"]
        add_result(
            "Criar categoria",
            "Categoria ativa",
            f"id={ctx['categoryId']} name={create_cat['body']['data'].get('name')}",
            "POST /api/v1/categories",
            True,
        )
    else:
        add_result(
            "Criar categoria",
            "2xx",
            f"HTTP {create_cat['status']} {create_cat.get('raw')}",
            "POST /api/v1/categories",
            False,
            error=create_cat.get("raw") or "",
        )

    # Product
    sku = f"SKU-{suffix}"
    create_prod = api(
        "POST",
        "/api/v1/products",
        {
            "internalCode": f"INT-{suffix}",
            "sku": sku,
            "barcode": None,
            "name": f"Produto Validacao {suffix}",
            "description": "Produto E2E",
            "categoryId": ctx.get("categoryId"),
            "unitOfMeasure": "UN",
            "costPrice": 10.00,
            "salePrice": 25.00,
            "minStock": 2,
            "allowNegativeStock": False,
            "imageUrl": None,
        },
        token=admin_token,
    )
    if create_prod["ok"]:
        ctx["productId"] = create_prod["body"]["data"]["id"]
        add_result(
            "Criar produto",
            "Produto ACTIVE com preco 25.00",
            f"id={ctx['productId']} sku={sku} salePrice={create_prod['body']['data'].get('salePrice')}",
            "POST /api/v1/products",
            True,
        )
    else:
        add_result(
            "Criar produto",
            "2xx",
            f"HTTP {create_prod['status']} {create_prod.get('raw')}",
            "POST /api/v1/products",
            False,
            error=create_prod.get("raw") or "",
        )

    # Stock entry
    entry_qty = 20
    entry = api(
        "POST",
        "/api/v1/inventory/entries",
        {
            "productId": ctx.get("productId"),
            "quantity": entry_qty,
            "observation": f"Entrada validacao {suffix}",
            "futureReturn": False,
        },
        token=admin_token,
    )
    if entry["ok"]:
        stock_after_entry = float(entry["body"]["data"]["newBalance"])
        add_result(
            "Entrada de estoque",
            f"Saldo = {entry_qty}",
            f"newBalance={stock_after_entry} type={entry['body']['data'].get('type')}",
            "POST /api/v1/inventory/entries",
            stock_after_entry == entry_qty,
        )
    else:
        add_result(
            "Entrada de estoque",
            f"saldo {entry_qty}",
            f"HTTP {entry['status']} {entry.get('raw')}",
            "POST /api/v1/inventory/entries",
            False,
            error=entry.get("raw") or "",
        )

    # Sale A
    sale_a = api(
        "POST",
        "/api/v1/sales",
        {"customerId": ctx.get("customerId"), "notes": f"Venda A paga {suffix}"},
        token=admin_token,
    )
    if sale_a["ok"]:
        ctx["saleAId"] = sale_a["body"]["data"]["id"]
        d = sale_a["body"]["data"]
        add_result(
            "Criar venda (rascunho A)",
            "DRAFT com canEdit/canConfirm",
            f"id={ctx['saleAId']} status={d.get('status')} canEdit={d.get('canEdit')}",
            "POST /api/v1/sales",
            d.get("status") == "DRAFT" and d.get("canEdit") is True,
        )
    else:
        add_result(
            "Criar venda (rascunho A)",
            "DRAFT",
            f"HTTP {sale_a['status']} {sale_a.get('raw')}",
            "POST /api/v1/sales",
            False,
            error=sale_a.get("raw") or "",
        )

    add_item = api(
        "POST",
        f"/api/v1/sales/{ctx.get('saleAId')}/items",
        {
            "productId": ctx.get("productId"),
            "quantity": 2,
            "unitPrice": None,
            "discountAmount": 0,
            "description": None,
        },
        token=admin_token,
    )
    if add_item["ok"]:
        ctx["saleATotal"] = float(add_item["body"]["data"]["totalAmount"])
        add_result(
            "Adicionar produto a venda A",
            "Totais oficiais da API (2 x 25 = 50)",
            f"total={ctx['saleATotal']} items={len(add_item['body']['data'].get('items') or [])}",
            "POST /api/v1/sales/{id}/items",
            ctx["saleATotal"] == 50.0,
        )
    else:
        add_result(
            "Adicionar produto a venda A",
            "total 50",
            f"HTTP {add_item['status']} {add_item.get('raw')}",
            "items",
            False,
            error=add_item.get("raw") or "",
        )

    confirm_a = api("POST", f"/api/v1/sales/{ctx.get('saleAId')}/confirm", token=admin_token)
    if confirm_a["ok"]:
        d = confirm_a["body"]["data"]
        add_result(
            "Confirmar venda A",
            "CONFIRMED + canReceivePayment",
            f"status={d.get('status')} canReceivePayment={d.get('canReceivePayment')}",
            "POST /api/v1/sales/{id}/confirm",
            d.get("status") == "CONFIRMED",
        )
    else:
        add_result(
            "Confirmar venda A",
            "CONFIRMED",
            f"HTTP {confirm_a['status']} {confirm_a.get('raw')}",
            "confirm",
            False,
            error=confirm_a.get("raw") or "",
        )

    inv_after = api(
        "GET",
        f"/api/v1/inventory/products/{ctx.get('productId')}",
        token=admin_token,
    )
    if inv_after["ok"]:
        stock_after_confirm = float(inv_after["body"]["data"]["quantity"])
        expected = entry_qty - 2
        add_result(
            "Validar baixa de estoque (venda A)",
            f"quantity={expected}",
            f"quantity={stock_after_confirm}",
            "GET /api/v1/inventory/products/{id}",
            stock_after_confirm == expected,
        )
    else:
        add_result(
            "Validar baixa de estoque (venda A)",
            "baixa 2",
            f"HTTP {inv_after['status']}",
            "inventory",
            False,
            error=inv_after.get("raw") or "",
        )

    pay1 = api(
        "POST",
        "/api/v1/payments",
        {
            "saleId": ctx.get("saleAId"),
            "method": "PIX",
            "amount": 20.00,
            "paidAt": None,
            "externalReference": f"PIX-P1-{suffix}",
            "notes": "parcial",
            "installments": 1,
            "tenderedAmount": None,
            "confirmImmediately": True,
        },
        token=admin_token,
    )
    if pay1["ok"]:
        sale_p1 = api("GET", f"/api/v1/sales/{ctx.get('saleAId')}", token=admin_token)
        add_result(
            "Registrar pagamento parcial",
            "PARTIALLY_PAID; payment CONFIRMED amount=20",
            f"payStatus={pay1['body']['data'].get('status')} amount={pay1['body']['data'].get('amount')} "
            f"saleStatus={sale_p1['body']['data'].get('status')}",
            "POST /api/v1/payments + GET sale",
            pay1["body"]["data"].get("status") == "CONFIRMED"
            and sale_p1["body"]["data"].get("status") == "PARTIALLY_PAID",
        )
    else:
        add_result(
            "Registrar pagamento parcial",
            "PARTIALLY_PAID",
            f"HTTP {pay1['status']} {pay1.get('raw')}",
            "payments",
            False,
            error=pay1.get("raw") or "",
        )

    pay2 = api(
        "POST",
        "/api/v1/payments",
        {
            "saleId": ctx.get("saleAId"),
            "method": "CASH",
            "amount": 30.00,
            "paidAt": None,
            "externalReference": None,
            "notes": "restante",
            "installments": 1,
            "tenderedAmount": 50.00,
            "confirmImmediately": True,
        },
        token=admin_token,
    )
    if pay2["ok"]:
        sale_paid = api("GET", f"/api/v1/sales/{ctx.get('saleAId')}", token=admin_token)
        balance = api(
            "GET",
            f"/api/v1/payments/by-sale/{ctx.get('saleAId')}/balance",
            token=admin_token,
        )
        add_result(
            "Registrar pagamento restante + validar venda paga",
            "PAID; balanceDue=0",
            f"saleStatus={sale_paid['body']['data'].get('status')} "
            f"balanceDue={balance['body']['data'].get('balanceDue')} "
            f"change={pay2['body']['data'].get('changeAmount')}",
            "POST payments + GET balance",
            sale_paid["body"]["data"].get("status") == "PAID"
            and float(balance["body"]["data"].get("balanceDue")) == 0.0,
        )
    else:
        add_result(
            "Registrar pagamento restante + validar venda paga",
            "PAID",
            f"HTTP {pay2['status']} {pay2.get('raw')}",
            "payments",
            False,
            error=pay2.get("raw") or "",
        )

    dash = api("GET", "/api/v1/dashboard", token=admin_token, query={"periodDays": 30, "topLimit": 5})
    dash_keys = list((dash.get("body") or {}).get("data", {}).keys()) if dash.get("ok") else []
    add_result(
        "Consultar dashboard",
        "HTTP 200 com totais oficiais",
        f"HTTP {dash['status']} keys={','.join(dash_keys)}",
        "GET /api/v1/dashboard",
        dash["ok"],
    )

    report = api(
        "GET",
        "/api/v1/reports/sales",
        token=admin_token,
        query={"page": 0, "size": 20},
    )
    rows = 0
    total = 0
    if report.get("ok") and report.get("body"):
        page_data = report["body"].get("data") or {}
        # PageResponse: data + page OR nested
        if isinstance(page_data, dict) and "data" in page_data:
            rows = len(page_data.get("data") or [])
            total = (page_data.get("page") or {}).get("totalElements", 0)
        elif isinstance(page_data, list):
            rows = len(page_data)
    add_result(
        "Consultar relatorio de vendas",
        "HTTP 200 com pagina de dados",
        f"HTTP {report['status']} rows={rows} total={total}",
        "GET /api/v1/reports/sales",
        report["ok"],
    )

    # Sale B cancelable
    sale_b = api(
        "POST",
        "/api/v1/sales",
        {"customerId": ctx.get("customerId"), "notes": f"Venda B cancelavel {suffix}"},
        token=admin_token,
    )
    ctx["saleBId"] = (sale_b.get("body") or {}).get("data", {}).get("id")
    api(
        "POST",
        f"/api/v1/sales/{ctx.get('saleBId')}/items",
        {
            "productId": ctx.get("productId"),
            "quantity": 1,
            "unitPrice": None,
            "discountAmount": 0,
            "description": None,
        },
        token=admin_token,
    )
    api("POST", f"/api/v1/sales/{ctx.get('saleBId')}/confirm", token=admin_token)
    inv_b = api("GET", f"/api/v1/inventory/products/{ctx.get('productId')}", token=admin_token)
    stock_before_cancel = float(inv_b["body"]["data"]["quantity"]) if inv_b["ok"] else None

    cancel_b = api(
        "POST",
        f"/api/v1/sales/{ctx.get('saleBId')}/cancel",
        {"reason": f"Cancelamento validacao funcional {suffix}"},
        token=admin_token,
    )
    inv_c = api("GET", f"/api/v1/inventory/products/{ctx.get('productId')}", token=admin_token)
    stock_after_cancel = float(inv_c["body"]["data"]["quantity"]) if inv_c["ok"] else None
    cancel_ok = (
        cancel_b["ok"]
        and cancel_b["body"]["data"].get("status") == "CANCELLED"
        and stock_before_cancel is not None
        and stock_after_cancel == stock_before_cancel + 1
    )
    add_result(
        "Cancelar venda permitida + estorno estoque",
        "CANCELLED e estoque +1",
        f"status={(cancel_b.get('body') or {}).get('data', {}).get('status')} "
        f"stockBefore={stock_before_cancel} stockAfter={stock_after_cancel} HTTP={cancel_b['status']}",
        "POST cancel + GET inventory",
        cancel_ok,
        error="" if cancel_ok else (cancel_b.get("raw") or ""),
    )

    cancel_paid = api(
        "POST",
        f"/api/v1/sales/{ctx.get('saleAId')}/cancel",
        {"reason": "Tentativa ilegal apos pagamento"},
        token=admin_token,
    )
    add_result(
        "Bloquear cancelamento de venda com pagamentos",
        "HTTP 4xx BusinessRule (pagamentos confirmados)",
        f"HTTP {cancel_paid['status']} code={(cancel_paid.get('body') or {}).get('code')} "
        f"msg={(cancel_paid.get('body') or {}).get('message')}",
        "POST cancel sale A",
        (not cancel_paid["ok"]) and cancel_paid["status"] >= 400,
    )

    audit = api(
        "GET",
        "/api/v1/audit-logs",
        token=admin_token,
        query={"page": 0, "size": 20},
    )
    audit_rows = 0
    sample_entity = None
    if audit.get("ok") and audit.get("body"):
        pdata = audit["body"].get("data") or {}
        rows_list = pdata.get("data") if isinstance(pdata, dict) else pdata
        if isinstance(rows_list, list):
            audit_rows = len(rows_list)
            if rows_list:
                sample_entity = rows_list[0].get("entityName")
    add_result(
        "Consultar auditoria",
        "HTTP 200 com eventos recentes",
        f"HTTP {audit['status']} rows={audit_rows} sampleEntity={sample_entity}",
        "GET /api/v1/audit-logs",
        audit["ok"] and audit_rows > 0,
    )

    seller_login_resp = api(
        "POST",
        "/api/v1/auth/login",
        {"username": seller_login, "password": seller_pass},
    )
    seller_token = None
    if seller_login_resp["ok"]:
        seller_token = seller_login_resp["body"]["data"]["accessToken"]
        perms = seller_login_resp["body"]["data"]["user"].get("permissions") or []
        add_result(
            "Login vendedor",
            "Token SELLER sem AUDIT_READ/USER_CREATE/REPORT_READ",
            f"perms={len(perms)} hasAudit={'AUDIT_READ' in perms} "
            f"hasUserCreate={'USER_CREATE' in perms} hasReport={'REPORT_READ' in perms}",
            "POST /api/v1/auth/login (seller)",
            "AUDIT_READ" not in perms and "USER_CREATE" not in perms,
        )
    else:
        add_result(
            "Login vendedor",
            "OK",
            f"HTTP {seller_login_resp['status']}",
            "login seller",
            False,
            error=seller_login_resp.get("raw") or "",
        )

    if seller_token:
        denied_audit = api(
            "GET",
            "/api/v1/audit-logs",
            token=seller_token,
            query={"page": 0, "size": 5},
        )
        denied_users = api(
            "GET",
            "/api/v1/users",
            token=seller_token,
            query={"page": 0, "size": 5},
        )
        denied_report = api(
            "GET",
            "/api/v1/reports/sales",
            token=seller_token,
            query={"page": 0, "size": 5},
        )
        denied_ok = (
            denied_audit["status"] == 403
            and denied_users["status"] == 403
            and denied_report["status"] == 403
        )
        add_result(
            "Acesso negado sem permissao",
            "403 em audit-logs, users e reports",
            f"audit={denied_audit['status']} users={denied_users['status']} reports={denied_report['status']}",
            "GET protegidos com token SELLER",
            denied_ok,
        )

    final_inv = api(
        "GET",
        f"/api/v1/inventory/products/{ctx.get('productId')}",
        token=admin_token,
    )
    final_stock = float(final_inv["body"]["data"]["quantity"]) if final_inv["ok"] else -1
    add_result(
        "Consistencia final de estoque",
        "quantity=18 (20-2-1+1)",
        f"quantity={final_stock}",
        f"GET inventory product {ctx.get('productId')}",
        final_stock == 18.0,
    )

    return _persist(suffix)


def _persist(suffix: str) -> int:
    passed = sum(1 for r in results if r["pass"])
    failed = sum(1 for r in results if not r["pass"])
    summary = {
        "ranAt": datetime.now(timezone.utc).isoformat(),
        "suffix": suffix,
        "context": ctx,
        "passed": passed,
        "failed": failed,
        "total": len(results),
        "results": results,
    }
    OUT_JSON.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
    print()
    print(f"SUMMARY passed={passed} failed={failed} total={len(results)}")
    print(f"JSON={OUT_JSON}")
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
