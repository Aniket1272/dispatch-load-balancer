#!/usr/bin/env bash
# Manual end-to-end check. Start the app first:  mvn spring-boot:run
# Orders/vehicles are upserted (not replaced). H2 is in-memory, so restart the app to reset data between scenarios.
set -euo pipefail
BASE="${BASE_URL:-http://localhost:8080}"
J='Content-Type: application/json'

echo "== 1. vehicles";  curl -s -X POST "$BASE/api/dispatch/vehicles" -H "$J" -d @samples/vehicles.json; echo
echo "== 2. orders";    curl -s -X POST "$BASE/api/dispatch/orders"   -H "$J" -d @samples/orders.json;   echo
echo "== 3. plan";      curl -s "$BASE/api/dispatch/plan" | python3 -m json.tool 2>/dev/null || curl -s "$BASE/api/dispatch/plan"
echo
echo "== 4. edge cases (EDGE001 should be unassigned)"
curl -s -X POST "$BASE/api/dispatch/orders" -H "$J" -d @samples/orders-edge-cases.json; echo
curl -s "$BASE/api/dispatch/plan" | python3 -c "import sys,json; p=json.load(sys.stdin); print(json.dumps(p['summary'],indent=2)); print(json.dumps(p['unassignedOrders'],indent=2))" 2>/dev/null || true
echo
echo "== 5. invalid input -> expect HTTP 400"
curl -s -o /dev/null -w "invalid priority: %{http_code}\n" -X POST "$BASE/api/dispatch/orders" -H "$J" \
  -d '{"orders":[{"orderId":"X","latitude":28.6,"longitude":77.2,"address":"Delhi","packageWeight":5,"priority":"URGENT"}]}'
curl -s -o /dev/null -w "latitude 120:      %{http_code}\n" -X POST "$BASE/api/dispatch/orders" -H "$J" \
  -d '{"orders":[{"orderId":"X","latitude":120,"longitude":77.2,"address":"Delhi","packageWeight":5,"priority":"HIGH"}]}'
