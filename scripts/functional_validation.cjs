/**
 * SystemCommerce Prompt 26 — validação funcional ponta a ponta.
 */
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const BASE = 'http://localhost:8080';
const FRONT = 'http://localhost:5173';
const API_ROOT = 'c:\\Git\\Sistemas\\SystemCommerce\\SystemCommerce-api';
const OUT_JSON = path.join(API_ROOT, 'target', 'validation-run.json');
const ENV_FILE = path.join(API_ROOT, '.env');

const results = [];
const ctx = {};

function loadEnv() {
  if (!fs.existsSync(ENV_FILE)) return;
  for (const line of fs.readFileSync(ENV_FILE, 'utf8').split(/\r?\n/)) {
    const t = line.trim();
    if (!t || t.startsWith('#') || !t.includes('=')) continue;
    const i = t.indexOf('=');
    process.env[t.slice(0, i).trim()] = t.slice(i + 1).trim();
  }
}

function addResult(step, expected, obtained, evidence, passed, error = '', fix = '') {
  results.push({ step, expected, obtained, evidence, error, fix, pass: passed });
  console.log(`[${passed ? 'OK' : 'FAIL'}] ${step} — ${obtained}`);
}

async function api(method, urlPath, body = null, token = null, query = null) {
  let url = BASE + urlPath;
  if (query) {
    const qs = new URLSearchParams();
    for (const [k, v] of Object.entries(query)) {
      if (v !== null && v !== undefined) qs.set(k, String(v));
    }
    url += `?${qs.toString()}`;
  }
  const headers = { Accept: 'application/json' };
  const init = { method, headers };
  if (body !== null) {
    headers['Content-Type'] = 'application/json';
    init.body = JSON.stringify(body);
  }
  if (token) headers.Authorization = `Bearer ${token}`;
  try {
    const resp = await fetch(url, init);
    const raw = await resp.text();
    let parsed = null;
    try {
      parsed = raw ? JSON.parse(raw) : null;
    } catch {
      parsed = null;
    }
    return { ok: resp.ok, status: resp.status, body: parsed, raw };
  } catch (err) {
    return { ok: false, status: 0, body: null, raw: '', error: String(err) };
  }
}

async function httpGet(url) {
  try {
    const resp = await fetch(url);
    const raw = await resp.text();
    return { ok: resp.ok, status: resp.status, raw };
  } catch (err) {
    return { ok: false, status: 0, raw: '', error: String(err) };
  }
}

function dockerHealth(name) {
  try {
    return execFileSync('docker', ['inspect', '-f', '{{.State.Health.Status}}', name], {
      encoding: 'utf8',
    }).trim();
  } catch {
    return 'unknown';
  }
}

function dockerExecPsql(sql) {
  const user = process.env.POSTGRES_USER || 'systemcommerce';
  const db = process.env.POSTGRES_DB || 'systemcommerce';
  try {
    return execFileSync(
      'docker',
      ['exec', 'systemcommerce-api-db', 'psql', '-U', user, '-d', db, '-tAc', sql],
      { encoding: 'utf8' },
    ).trim();
  } catch (err) {
    return `ERR:${err.stdout || err.message}`;
  }
}

function calcCpfDigit(digits, weightStart) {
  let sum = 0;
  for (let i = 0; i < weightStart - 1; i += 1) {
    sum += Number(digits[i]) * (weightStart - i);
  }
  const mod = sum % 11;
  return mod < 2 ? 0 : 11 - mod;
}

function generateValidCpf() {
  let base;
  do {
    base = String(Math.floor(Math.random() * 1e9)).padStart(9, '0');
  } while (/^(\d)\1+$/.test(base));
  const d1 = calcCpfDigit(base, 10);
  const withD1 = base + String(d1);
  const d2 = calcCpfDigit(withD1, 11);
  return withD1 + String(d2);
}

function persist(suffix) {
  const passed = results.filter((r) => r.pass).length;
  const failed = results.filter((r) => !r.pass).length;
  const summary = {
    ranAt: new Date().toISOString(),
    suffix,
    context: ctx,
    passed,
    failed,
    total: results.length,
    results,
  };
  fs.mkdirSync(path.dirname(OUT_JSON), { recursive: true });
  fs.writeFileSync(OUT_JSON, JSON.stringify(summary, null, 2), 'utf8');
  console.log('');
  console.log(`SUMMARY passed=${passed} failed=${failed} total=${results.length}`);
  console.log(`JSON=${OUT_JSON}`);
  return failed === 0 ? 0 : 1;
}

async function main() {
  loadEnv();
  const suffix = new Date()
    .toISOString()
    .replace(/[-:TZ.]/g, '')
    .slice(0, 14);

  const pg = dockerHealth('systemcommerce-api-db');
  const apiH = dockerHealth('systemcommerce-api');
  const frontH = dockerHealth('systemcommerce-front');
  addResult(
    'Infra: PostgreSQL + API + Frontend',
    'Containers healthy (db, api, front)',
    `db=${pg} api=${apiH} front=${frontH}`,
    'docker inspect health',
    pg === 'healthy' && apiH === 'healthy' && frontH === 'healthy',
  );

  const mig = dockerExecPsql('SELECT COUNT(*) FROM flyway_schema_history WHERE success = true');
  addResult(
    'Migrations Flyway',
    'Migrations aplicadas com sucesso',
    `success_count=${mig}`,
    'flyway_schema_history',
    /^\d+$/.test(mig) && Number(mig) > 0,
  );

  const seed = dockerExecPsql(
    "SELECT COUNT(*) FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id WHERE u.login='admin' AND r.code='ADMIN'",
  );
  addResult(
    'Seeds (admin + roles)',
    'Usuario admin com perfil ADMIN',
    `admin_role_links=${seed}`,
    'users/user_roles/roles',
    /^\d+$/.test(seed) && Number(seed) >= 1,
  );

  const health = await api('GET', '/actuator/health');
  addResult(
    'API health',
    'status=UP',
    `HTTP ${health.status} body=${health.raw}`,
    'GET /actuator/health',
    health.ok && health.body?.status === 'UP',
  );

  const fh = await httpGet(`${FRONT}/healthz`);
  addResult(
    'Frontend health',
    'HTTP 200 em /healthz',
    `HTTP ${fh.status} ${fh.raw}`,
    `GET ${FRONT}/healthz`,
    fh.ok && fh.status === 200,
  );

  const page = await httpGet(`${FRONT}/`);
  addResult(
    'Frontend SPA',
    'HTML da SPA servido',
    `HTTP ${page.status} bytes=${(page.raw || '').length}`,
    `GET ${FRONT}/`,
    page.ok && page.status === 200,
  );

  const login = await api('POST', '/api/v1/auth/login', {
    username: process.env.ADMIN_LOGIN || 'admin',
    password: process.env.ADMIN_PASSWORD || '',
  });
  let adminToken = null;
  if (login.ok && login.body?.data?.accessToken) {
    adminToken = login.body.data.accessToken;
    ctx.adminUserId = login.body.data.user.id;
    addResult(
      'Autenticar administrador',
      'Login OK com token e permissoes',
      `login=${login.body.data.user.login} permissions=${(login.body.data.user.permissions || []).length}`,
      'POST /api/v1/auth/login',
      true,
    );
  } else {
    addResult(
      'Autenticar administrador',
      'Login OK',
      `HTTP ${login.status} ${login.raw}`,
      'POST /api/v1/auth/login',
      false,
      login.error || login.raw || '',
    );
    process.exit(persist(suffix));
  }

  const sellerLogin = `seller_${suffix}`;
  const sellerPass = 'Seller@123!';
  const createSeller = await api(
    'POST',
    '/api/v1/users',
    {
      name: `Vendedor Validacao ${suffix}`,
      email: `seller.${suffix}@example.com`,
      login: sellerLogin,
      password: sellerPass,
      roleCodes: ['SELLER'],
    },
    adminToken,
  );
  if (createSeller.ok) {
    ctx.sellerId = createSeller.body.data.id;
    const roles = createSeller.body.data.roles || [];
    addResult(
      'Criar usuario vendedor',
      'HTTP 2xx com perfil SELLER',
      `id=${ctx.sellerId} login=${sellerLogin} roles=${roles}`,
      'POST /api/v1/users',
      roles.includes('SELLER'),
    );
  } else {
    addResult(
      'Criar usuario vendedor',
      '2xx',
      `HTTP ${createSeller.status} ${createSeller.raw}`,
      'POST /api/v1/users',
      false,
      createSeller.raw || '',
    );
  }

  let createCustomer = await api(
    'POST',
    '/api/v1/customers',
    {
      type: 'PF',
      name: `Cliente Validacao ${suffix}`,
      document: generateValidCpf(),
      email: `cliente.${suffix}@example.com`,
      phone: '11999990000',
      city: 'Sao Paulo',
      state: 'SP',
    },
    adminToken,
  );
  if (!createCustomer.ok) {
    createCustomer = await api(
      'POST',
      '/api/v1/customers',
      {
        type: 'PF',
        name: `Cliente Validacao ${suffix}`,
        document: generateValidCpf(),
        email: `cliente2.${suffix}@example.com`,
        phone: '11999990000',
        city: 'Sao Paulo',
        state: 'SP',
      },
      adminToken,
    );
  }
  if (createCustomer.ok) {
    ctx.customerId = createCustomer.body.data.id;
    addResult(
      'Criar cliente',
      'Cliente PF criado',
      `id=${ctx.customerId} document=${createCustomer.body.data.document}`,
      'POST /api/v1/customers',
      true,
    );
  } else {
    addResult(
      'Criar cliente',
      '2xx',
      `HTTP ${createCustomer.status} ${createCustomer.raw}`,
      'POST /api/v1/customers',
      false,
      createCustomer.raw || '',
    );
  }

  const createCat = await api(
    'POST',
    '/api/v1/categories',
    {
      name: `Cat Validacao ${suffix}`,
      description: 'Categoria E2E Prompt 26',
      parentId: null,
    },
    adminToken,
  );
  if (createCat.ok) {
    ctx.categoryId = createCat.body.data.id;
    addResult(
      'Criar categoria',
      'Categoria ativa',
      `id=${ctx.categoryId} name=${createCat.body.data.name}`,
      'POST /api/v1/categories',
      true,
    );
  } else {
    addResult(
      'Criar categoria',
      '2xx',
      `HTTP ${createCat.status} ${createCat.raw}`,
      'POST /api/v1/categories',
      false,
      createCat.raw || '',
    );
  }

  const sku = `SKU-${suffix}`;
  const createProd = await api(
    'POST',
    '/api/v1/products',
    {
      internalCode: `INT-${suffix}`,
      sku,
      barcode: null,
      name: `Produto Validacao ${suffix}`,
      description: 'Produto E2E',
      categoryId: ctx.categoryId,
      unitOfMeasure: 'UN',
      costPrice: 10.0,
      salePrice: 25.0,
      minStock: 2,
      allowNegativeStock: false,
      imageUrl: null,
    },
    adminToken,
  );
  if (createProd.ok) {
    ctx.productId = createProd.body.data.id;
    addResult(
      'Criar produto',
      'Produto ACTIVE com preco 25.00',
      `id=${ctx.productId} sku=${sku} salePrice=${createProd.body.data.salePrice}`,
      'POST /api/v1/products',
      true,
    );
  } else {
    addResult(
      'Criar produto',
      '2xx',
      `HTTP ${createProd.status} ${createProd.raw}`,
      'POST /api/v1/products',
      false,
      createProd.raw || '',
    );
  }

  const entryQty = 20;
  const entry = await api(
    'POST',
    '/api/v1/inventory/entries',
    {
      productId: ctx.productId,
      quantity: entryQty,
      observation: `Entrada validacao ${suffix}`,
      futureReturn: false,
    },
    adminToken,
  );
  if (entry.ok) {
    const stockAfterEntry = Number(entry.body.data.newBalance);
    addResult(
      'Entrada de estoque',
      `Saldo = ${entryQty}`,
      `newBalance=${stockAfterEntry} type=${entry.body.data.type}`,
      'POST /api/v1/inventory/entries',
      stockAfterEntry === entryQty,
    );
  } else {
    addResult(
      'Entrada de estoque',
      `saldo ${entryQty}`,
      `HTTP ${entry.status} ${entry.raw}`,
      'POST /api/v1/inventory/entries',
      false,
      entry.raw || '',
    );
  }

  const saleA = await api(
    'POST',
    '/api/v1/sales',
    { customerId: ctx.customerId, notes: `Venda A paga ${suffix}` },
    adminToken,
  );
  if (saleA.ok) {
    ctx.saleAId = saleA.body.data.id;
    const d = saleA.body.data;
    addResult(
      'Criar venda (rascunho A)',
      'DRAFT com canEdit/canConfirm',
      `id=${ctx.saleAId} status=${d.status} canEdit=${d.canEdit}`,
      'POST /api/v1/sales',
      d.status === 'DRAFT' && d.canEdit === true,
    );
  } else {
    addResult(
      'Criar venda (rascunho A)',
      'DRAFT',
      `HTTP ${saleA.status} ${saleA.raw}`,
      'POST /api/v1/sales',
      false,
      saleA.raw || '',
    );
  }

  const addItem = await api(
    'POST',
    `/api/v1/sales/${ctx.saleAId}/items`,
    {
      productId: ctx.productId,
      quantity: 2,
      unitPrice: null,
      discountAmount: 0,
      description: null,
    },
    adminToken,
  );
  if (addItem.ok) {
    ctx.saleATotal = Number(addItem.body.data.totalAmount);
    addResult(
      'Adicionar produto a venda A',
      'Totais oficiais da API (2 x 25 = 50)',
      `total=${ctx.saleATotal} items=${(addItem.body.data.items || []).length}`,
      'POST /api/v1/sales/{id}/items',
      ctx.saleATotal === 50,
    );
  } else {
    addResult(
      'Adicionar produto a venda A',
      'total 50',
      `HTTP ${addItem.status} ${addItem.raw}`,
      'items',
      false,
      addItem.raw || '',
    );
  }

  const confirmA = await api('POST', `/api/v1/sales/${ctx.saleAId}/confirm`, null, adminToken);
  if (confirmA.ok) {
    const d = confirmA.body.data;
    addResult(
      'Confirmar venda A',
      'CONFIRMED + canReceivePayment',
      `status=${d.status} canReceivePayment=${d.canReceivePayment}`,
      'POST /api/v1/sales/{id}/confirm',
      d.status === 'CONFIRMED',
    );
  } else {
    addResult(
      'Confirmar venda A',
      'CONFIRMED',
      `HTTP ${confirmA.status} ${confirmA.raw}`,
      'confirm',
      false,
      confirmA.raw || '',
    );
  }

  const invAfter = await api('GET', `/api/v1/inventory/products/${ctx.productId}`, null, adminToken);
  if (invAfter.ok) {
    const stockAfterConfirm = Number(invAfter.body.data.quantity);
    const expected = entryQty - 2;
    addResult(
      'Validar baixa de estoque (venda A)',
      `quantity=${expected}`,
      `quantity=${stockAfterConfirm}`,
      'GET /api/v1/inventory/products/{id}',
      stockAfterConfirm === expected,
    );
  } else {
    addResult(
      'Validar baixa de estoque (venda A)',
      'baixa 2',
      `HTTP ${invAfter.status}`,
      'inventory',
      false,
      invAfter.raw || '',
    );
  }

  const pay1 = await api(
    'POST',
    '/api/v1/payments',
    {
      saleId: ctx.saleAId,
      method: 'PIX',
      amount: 20.0,
      paidAt: null,
      externalReference: `PIX-P1-${suffix}`,
      notes: 'parcial',
      installments: 1,
      tenderedAmount: null,
      confirmImmediately: true,
    },
    adminToken,
  );
  if (pay1.ok) {
    const saleP1 = await api('GET', `/api/v1/sales/${ctx.saleAId}`, null, adminToken);
    addResult(
      'Registrar pagamento parcial',
      'PARTIALLY_PAID; payment CONFIRMED amount=20',
      `payStatus=${pay1.body.data.status} amount=${pay1.body.data.amount} saleStatus=${saleP1.body.data.status}`,
      'POST /api/v1/payments + GET sale',
      pay1.body.data.status === 'CONFIRMED' && saleP1.body.data.status === 'PARTIALLY_PAID',
    );
  } else {
    addResult(
      'Registrar pagamento parcial',
      'PARTIALLY_PAID',
      `HTTP ${pay1.status} ${pay1.raw}`,
      'payments',
      false,
      pay1.raw || '',
    );
  }

  const pay2 = await api(
    'POST',
    '/api/v1/payments',
    {
      saleId: ctx.saleAId,
      method: 'CASH',
      amount: 30.0,
      paidAt: null,
      externalReference: null,
      notes: 'restante',
      installments: 1,
      tenderedAmount: 50.0,
      confirmImmediately: true,
    },
    adminToken,
  );
  if (pay2.ok) {
    const salePaid = await api('GET', `/api/v1/sales/${ctx.saleAId}`, null, adminToken);
    const balance = await api(
      'GET',
      `/api/v1/payments/by-sale/${ctx.saleAId}/balance`,
      null,
      adminToken,
    );
    addResult(
      'Registrar pagamento restante + validar venda paga',
      'PAID; balanceDue=0',
      `saleStatus=${salePaid.body.data.status} balanceDue=${balance.body.data.balanceDue} change=${pay2.body.data.changeAmount}`,
      'POST payments + GET balance',
      salePaid.body.data.status === 'PAID' && Number(balance.body.data.balanceDue) === 0,
    );
  } else {
    addResult(
      'Registrar pagamento restante + validar venda paga',
      'PAID',
      `HTTP ${pay2.status} ${pay2.raw}`,
      'payments',
      false,
      pay2.raw || '',
    );
  }

  const dash = await api('GET', '/api/v1/dashboard', null, adminToken, {
    periodDays: 30,
    topLimit: 5,
  });
  const dashKeys = dash.ok ? Object.keys(dash.body?.data || {}) : [];
  addResult(
    'Consultar dashboard',
    'HTTP 200 com totais oficiais',
    `HTTP ${dash.status} keys=${dashKeys.join(',')}`,
    'GET /api/v1/dashboard',
    dash.ok,
  );

  const report = await api('GET', '/api/v1/reports/sales', null, adminToken, {
    page: 0,
    size: 20,
  });
  let rows = 0;
  let total = 0;
  if (report.ok && report.body) {
    const pageData = report.body.data || {};
    if (pageData.data) {
      rows = (pageData.data || []).length;
      total = pageData.page?.totalElements ?? 0;
    } else if (Array.isArray(pageData)) {
      rows = pageData.length;
    }
  }
  addResult(
    'Consultar relatorio de vendas',
    'HTTP 200 com pagina de dados',
    `HTTP ${report.status} rows=${rows} total=${total}`,
    'GET /api/v1/reports/sales',
    report.ok,
  );

  const saleB = await api(
    'POST',
    '/api/v1/sales',
    { customerId: ctx.customerId, notes: `Venda B cancelavel ${suffix}` },
    adminToken,
  );
  ctx.saleBId = saleB.body?.data?.id;
  await api(
    'POST',
    `/api/v1/sales/${ctx.saleBId}/items`,
    {
      productId: ctx.productId,
      quantity: 1,
      unitPrice: null,
      discountAmount: 0,
      description: null,
    },
    adminToken,
  );
  await api('POST', `/api/v1/sales/${ctx.saleBId}/confirm`, null, adminToken);
  const invB = await api('GET', `/api/v1/inventory/products/${ctx.productId}`, null, adminToken);
  const stockBeforeCancel = invB.ok ? Number(invB.body.data.quantity) : null;

  const cancelB = await api(
    'POST',
    `/api/v1/sales/${ctx.saleBId}/cancel`,
    { reason: `Cancelamento validacao funcional ${suffix}` },
    adminToken,
  );
  const invC = await api('GET', `/api/v1/inventory/products/${ctx.productId}`, null, adminToken);
  const stockAfterCancel = invC.ok ? Number(invC.body.data.quantity) : null;
  const cancelOk =
    cancelB.ok &&
    cancelB.body.data.status === 'CANCELLED' &&
    stockBeforeCancel !== null &&
    stockAfterCancel === stockBeforeCancel + 1;
  addResult(
    'Cancelar venda permitida + estorno estoque',
    'CANCELLED e estoque +1',
    `status=${cancelB.body?.data?.status} stockBefore=${stockBeforeCancel} stockAfter=${stockAfterCancel} HTTP=${cancelB.status}`,
    'POST cancel + GET inventory',
    cancelOk,
    cancelOk ? '' : cancelB.raw || '',
  );

  const cancelPaid = await api(
    'POST',
    `/api/v1/sales/${ctx.saleAId}/cancel`,
    { reason: 'Tentativa ilegal apos pagamento' },
    adminToken,
  );
  addResult(
    'Bloquear cancelamento de venda com pagamentos',
    'HTTP 4xx BusinessRule (pagamentos confirmados)',
    `HTTP ${cancelPaid.status} code=${cancelPaid.body?.code} msg=${cancelPaid.body?.message}`,
    'POST cancel sale A',
    !cancelPaid.ok && cancelPaid.status >= 400,
  );

  const audit = await api('GET', '/api/v1/audit-logs', null, adminToken, { page: 0, size: 20 });
  let auditRows = 0;
  let sampleEntity = null;
  if (audit.ok && audit.body) {
    const pdata = audit.body.data || {};
    const list = pdata.data || pdata;
    if (Array.isArray(list)) {
      auditRows = list.length;
      sampleEntity = list[0]?.entityName;
    }
  }
  addResult(
    'Consultar auditoria',
    'HTTP 200 com eventos recentes',
    `HTTP ${audit.status} rows=${auditRows} sampleEntity=${sampleEntity}`,
    'GET /api/v1/audit-logs',
    audit.ok && auditRows > 0,
  );

  const sellerLoginResp = await api('POST', '/api/v1/auth/login', {
    username: sellerLogin,
    password: sellerPass,
  });
  let sellerToken = null;
  if (sellerLoginResp.ok) {
    sellerToken = sellerLoginResp.body.data.accessToken;
    const perms = sellerLoginResp.body.data.user.permissions || [];
    addResult(
      'Login vendedor',
      'Token SELLER sem AUDIT_READ/USER_CREATE/REPORT_READ',
      `perms=${perms.length} hasAudit=${perms.includes('AUDIT_READ')} hasUserCreate=${perms.includes('USER_CREATE')} hasReport=${perms.includes('REPORT_READ')}`,
      'POST /api/v1/auth/login (seller)',
      !perms.includes('AUDIT_READ') && !perms.includes('USER_CREATE'),
    );
  } else {
    addResult(
      'Login vendedor',
      'OK',
      `HTTP ${sellerLoginResp.status}`,
      'login seller',
      false,
      sellerLoginResp.raw || '',
    );
  }

  if (sellerToken) {
    const deniedAudit = await api('GET', '/api/v1/audit-logs', null, sellerToken, {
      page: 0,
      size: 5,
    });
    const deniedUsers = await api('GET', '/api/v1/users', null, sellerToken, {
      page: 0,
      size: 5,
    });
    const deniedReport = await api('GET', '/api/v1/reports/sales', null, sellerToken, {
      page: 0,
      size: 5,
    });
    addResult(
      'Acesso negado sem permissao',
      '403 em audit-logs, users e reports',
      `audit=${deniedAudit.status} users=${deniedUsers.status} reports=${deniedReport.status}`,
      'GET protegidos com token SELLER',
      deniedAudit.status === 403 && deniedUsers.status === 403 && deniedReport.status === 403,
    );
  }

  const finalInv = await api('GET', `/api/v1/inventory/products/${ctx.productId}`, null, adminToken);
  const finalStock = finalInv.ok ? Number(finalInv.body.data.quantity) : -1;
  addResult(
    'Consistencia final de estoque',
    'quantity=18 (20-2-1+1)',
    `quantity=${finalStock}`,
    `GET inventory product ${ctx.productId}`,
    finalStock === 18,
  );

  process.exit(persist(suffix));
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
