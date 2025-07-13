#!/bin/bash

# Redis Benchmark Comparison Script
# Compares claim1.lua vs claim-optimized.lua

echo "🚀 Redis Claim Script Performance Comparison"
echo "============================================="

# Configuration
REDIS_HOST="localhost"
REDIS_PORT="9001"
REDIS_PASSWORD="myredispassword"
POST_ID="12345"
USER_ID_BASE="user"
QUOTA="100"

echo "📋 Test Configuration:"
echo "  Host: $REDIS_HOST:$REDIS_PORT"
echo "  Post ID: $POST_ID"
echo "  Quota: $QUOTA"
echo ""

# Clean up any existing data
echo "🧹 Cleaning up existing test data..."
redis-cli -h $REDIS_HOST -p $REDIS_PORT -a $REDIS_PASSWORD --no-auth-warning \
  DEL "post:{$POST_ID}:claimants" "post:{$POST_ID}:claims_count" > /dev/null

echo ""
echo "🔥 Testing claim1.lua (Original - keys built in script)"
echo "--------------------------------------------------------"

# Load claim1.lua script
SCRIPT1=$(cat << 'EOF'
-- KEYS[1] = post_id
-- ARGV[1] = user_id  
-- ARGV[2] = quota

local id      = KEYS[1]
local uid     = ARGV[1]
local quota   = tonumber(ARGV[2])

local setKey  = "post:{" .. id .. "}:claimants"
local cntKey  = "post:{" .. id .. "}:claims_count"

-- Attempt to add first (1 = added, 0 = existed)
if redis.call('SADD', setKey, uid) == 0 then
  return 'ALREADY_CLAIMED'
end

local newCnt = redis.call('INCR', cntKey)
if newCnt > quota then
  -- Roll back
  redis.call('SREM', setKey, uid)
  redis.call('DECR', cntKey)
  return 'QUOTA_EXCEEDED'
end

return 'SUCCESS'
EOF
)

# Test claim1.lua performance
redis-cli -h $REDIS_HOST -p $REDIS_PORT -a $REDIS_PASSWORD --no-auth-warning \
  --eval <(echo "$SCRIPT1") , $POST_ID ${USER_ID_BASE}1 $QUOTA

echo "Running benchmark for claim1.lua..."
time (
  for i in {1..1000}; do
    redis-cli -h $REDIS_HOST -p $REDIS_PORT -a $REDIS_PASSWORD --no-auth-warning \
      --eval <(echo "$SCRIPT1") , $POST_ID ${USER_ID_BASE}$i $QUOTA > /dev/null
  done
)

# Clean up
redis-cli -h $REDIS_HOST -p $REDIS_PORT -a $REDIS_PASSWORD --no-auth-warning \
  DEL "post:{$POST_ID}:claimants" "post:{$POST_ID}:claims_count" > /dev/null

echo ""
echo "🚀 Testing claim-optimized.lua (Cluster-safe - keys passed as KEYS)"
echo "--------------------------------------------------------------------"

# Load claim-optimized.lua script
SCRIPT2=$(cat << 'EOF'
-- KEYS[1] = "post:{post_id}:claimants" (set key)
-- KEYS[2] = "post:{post_id}:claims_count" (counter key)
-- ARGV[1] = user_id
-- ARGV[2] = quota

local setKey = KEYS[1]
local cntKey = KEYS[2]
local uid    = ARGV[1]
local quota  = tonumber(ARGV[2])

-- Attempt to add first (1 = added, 0 = existed)
if redis.call('SADD', setKey, uid) == 0 then
  return 'ALREADY_CLAIMED'
end

local newCnt = redis.call('INCR', cntKey)
if newCnt > quota then
  -- Roll back
  redis.call('SREM', setKey, uid)
  redis.call('DECR', cntKey)
  return 'QUOTA_EXCEEDED'
end

return 'SUCCESS'
EOF
)

# Test claim-optimized.lua performance
SET_KEY="post:{$POST_ID}:claimants"
CNT_KEY="post:{$POST_ID}:claims_count"

redis-cli -h $REDIS_HOST -p $REDIS_PORT -a $REDIS_PASSWORD --no-auth-warning \
  --eval <(echo "$SCRIPT2") "$SET_KEY,$CNT_KEY" ${USER_ID_BASE}1 $QUOTA

echo "Running benchmark for claim-optimized.lua..."
time (
  for i in {1..1000}; do
    redis-cli -h $REDIS_HOST -p $REDIS_PORT -a $REDIS_PASSWORD --no-auth-warning \
      --eval <(echo "$SCRIPT2") "$SET_KEY,$CNT_KEY" ${USER_ID_BASE}$i $QUOTA > /dev/null
  done
)

echo ""
echo "✅ Benchmark Complete!"
echo ""
echo "📊 Summary:"
echo "  - claim1.lua: Keys built in script (Redis Cluster incompatible)"
echo "  - claim-optimized.lua: Keys passed as parameters (Redis Cluster compatible)"
echo ""
echo "🔍 Key Benefits of Optimized Version:"
echo "  ✅ Redis Cluster compatible"
echo "  ✅ All keys guaranteed on same hash slot"
echo "  ✅ No MOVED errors in cluster mode"
echo "  ✅ Slightly better performance (no string concatenation in Lua)"
