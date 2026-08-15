#!/usr/bin/env bash
# AccessZero Bash Administration Script

BASE_URL="http://localhost:8080/api/v1"

echo "================================================================="
echo "     AccessZero — Identity Breach Containment Platform (Bash)    "
echo "================================================================="

case "$1" in
    list)
        curl -s "${BASE_URL}/identities" | jq .
        ;;
    analyze)
        curl -s "${BASE_URL}/identities/username/$2/blast-radius" | jq .
        ;;
    simulate)
        curl -s -X POST "${BASE_URL}/identities/username/$2/simulate" | jq .
        ;;
    contain)
        curl -s -X POST "${BASE_URL}/containment/request" \
            -H "Content-Type: application/json" \
            -d "{\"username\": \"$2\", \"requestedBy\": \"${3:-anil.admin}\", \"emergencyOverride\": false}" | jq .
        ;;
    approve)
        curl -s -X POST "${BASE_URL}/containment/$2/approve" \
            -H "Content-Type: application/json" \
            -d "{\"approvedBy\": \"${3:-priya.security}\", \"notes\": \"Approved via Bash CLI\"}" | jq .
        ;;
    verify)
        curl -s "${BASE_URL}/verification/username/$2?verifiedBy=${3:-it.verifier}" | jq .
        ;;
    rollback)
        curl -s -X POST "${BASE_URL}/containment/$2/rollback" \
            -H "Content-Type: application/json" \
            -d "{\"rolledBackBy\": \"${3:-anil.admin}\"}" | jq .
        ;;
    audit)
        curl -s "${BASE_URL}/audit/events" | jq .
        ;;
    *)
        echo "Usage: $0 {list|analyze <user>|simulate <user>|contain <user> [admin]|approve <opId> [admin]|verify <user>|rollback <opId>|audit}"
        exit 1
        ;;
esac
