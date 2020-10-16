#!/usr/bin/env bash

REPORT=vessel-runtime/build/reports/jacoco/debug/jacoco.xml
if [ ! -f $REPORT ]; then
  echo "$REPORT not found. Did you run 'jacocoTestReportDebug' first?"
  exit 2
fi

COUNTERS=$(sed 's/>/>\n/g' $REPORT | grep INSTRUCTION | tail -1)
IFS=' '
ARR=($COUNTERS)

MISSED=${ARR[2]}
MISSED=${MISSED#*'"'};
MISSED=${MISSED%'"'*};

COVERED=${ARR[3]}
COVERED=${COVERED#*'"'};
COVERED=${COVERED%'"'*};

TOTAL=$((MISSED + COVERED))
COVERAGE=$(printf %.1f "$(( 10**1 * COVERED*100/TOTAL ))e-1")

COV=$((COVERED*100/TOTAL))
if [ $COV -lt 40 ]; then
  COLOR="red"
elif [ $COV -lt 60 ]; then
  COLOR="orange"
elif [ $COV -lt 80 ]; then
  COLOR="yellow"
else
  COLOR="brightgreen"
fi

# See https://shields.io/endpoint
OUTPUT=.github/badges/coverage.json
cat <<EOF > $OUTPUT
{
  "schemaVersion": 1,
  "label": "Coverage",
  "message": "$COVERAGE%",
  "color": "$COLOR"
}
EOF
echo "Wrote $OUTPUT"
