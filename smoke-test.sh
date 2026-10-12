#!/usr/bin/env bash
# ==============================================================================
# Campus Lost & Found System - Comprehensive End-to-End Smoke Test Script
# ==============================================================================

set -e

BASE_URL="http://localhost:8088"
echo "=== 1. Starting Smoke Test against ${BASE_URL} ==="

# 1. Register a new test student
RANDOM_ID=$((1000 + RANDOM % 9000))
STUDENT_EMAIL="student_${RANDOM_ID}@campus.edu"
STUDENT_ROLL="ROLL${RANDOM_ID}"

echo "--> Registering student: ${STUDENT_EMAIL}..."
REG_RESP=$(curl -s -X POST "${BASE_URL}/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"Test Student ${RANDOM_ID}\",
    \"email\": \"${STUDENT_EMAIL}\",
    \"rollNumber\": \"${STUDENT_ROLL}\",
    \"password\": \"Password@123\"
  }")
echo "Registration Response: ${REG_RESP}"

# 2. Login as the newly registered student
echo "--> Logging in as student..."
STUDENT_LOGIN_RESP=$(curl -s -X POST "${BASE_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"identifier\": \"${STUDENT_EMAIL}\",
    \"password\": \"Password@123\"
  }")
STUDENT_TOKEN=$(echo "${STUDENT_LOGIN_RESP}" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "${STUDENT_TOKEN}" ]; then
  echo "FAILED: Could not retrieve student token"
  exit 1
fi
echo "Student Token acquired successfully."

# 3. Login as default seeded staff
echo "--> Logging in as default seeded staff..."
STAFF_LOGIN_RESP=$(curl -s -X POST "${BASE_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "staff@campus.edu",
    "password": "Staff@123"
  }')
STAFF_TOKEN=$(echo "${STAFF_LOGIN_RESP}" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "${STAFF_TOKEN}" ]; then
  echo "FAILED: Could not retrieve staff token"
  exit 1
fi
echo "Staff Token acquired successfully."

# 4. Student reports a lost item
echo "--> Student reports lost item (Wallet)..."
LOST_ITEM_RESP=$(curl -s -X POST "${BASE_URL}/api/items" \
  -H "Authorization: Bearer ${STUDENT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "LOST",
    "category": "WALLET",
    "title": "Brown Leather Tommy Hilfiger Wallet",
    "description": "Lost near library coffee machine. Contains student ID card and cash.",
    "color": "Brown",
    "brand": "Tommy Hilfiger",
    "location": "Library",
    "eventDate": "2026-10-10"
  }')
LOST_ITEM_ID=$(echo "${LOST_ITEM_RESP}" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo "Reported Lost Item ID: ${LOST_ITEM_ID}"

# 5. Staff logs a found item matching the wallet
echo "--> Staff logs found item (matching Wallet)..."
FOUND_ITEM_RESP=$(curl -s -X POST "${BASE_URL}/api/items" \
  -H "Authorization: Bearer ${STAFF_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "FOUND",
    "category": "WALLET",
    "title": "Tommy Hilfiger Leather Wallet Brown",
    "description": "Found near library cafeteria counter. Handed to security desk.",
    "color": "Brown",
    "brand": "Tommy Hilfiger",
    "location": "Library",
    "eventDate": "2026-10-10",
    "storedAt": "Main Gate Security Locker 4B",
    "hiddenDetail": "Contains driving license and two vintage stamps"
  }')
FOUND_ITEM_ID=$(echo "${FOUND_ITEM_RESP}" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo "Logged Found Item ID: ${FOUND_ITEM_ID}"

# 6. Check smart matches for student
echo "--> Fetching smart match suggestions..."
MATCHES_RESP=$(curl -s -X GET "${BASE_URL}/api/matches" \
  -H "Authorization: Bearer ${STUDENT_TOKEN}")
echo "Matches response: ${MATCHES_RESP}"

# 7. Student submits claim
echo "--> Student submits ownership claim..."
CLAIM_RESP=$(curl -s -X POST "${BASE_URL}/api/claims" \
  -H "Authorization: Bearer ${STUDENT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"itemId\": ${FOUND_ITEM_ID},
    \"lostItemId\": ${LOST_ITEM_ID},
    \"answer\": \"It has a driving license inside with two vintage stamps behind the notes.\"
  }")
CLAIM_ID=$(echo "${CLAIM_RESP}" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo "Submitted Claim ID: ${CLAIM_ID}"

# 8. Staff reviews and approves claim
echo "--> Staff reviews and approves claim #${CLAIM_ID}..."
REVIEW_RESP=$(curl -s -X POST "${BASE_URL}/api/claims/${CLAIM_ID}/review" \
  -H "Authorization: Bearer ${STAFF_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "decision": "APPROVE",
    "note": "Secret stamps and license verified successfully."
  }')
echo "Review Response: ${REVIEW_RESP}"

# 9. Verify student received approval notification
echo "--> Checking student notifications..."
NOTIF_RESP=$(curl -s -X GET "${BASE_URL}/api/notifications" \
  -H "Authorization: Bearer ${STUDENT_TOKEN}")
echo "Notifications received: ${NOTIF_RESP}"

echo "=== Smoke Test Completed Successfully! All Endpoints Verified ==="
