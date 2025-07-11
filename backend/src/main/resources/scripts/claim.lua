-- OPTIMIZED VERSION: Minimal operations for maximum performance
-- KEYS[1]: post_id
-- ARGV[1]: user_id
-- ARGV[2]: quota

local postId = KEYS[1]
local userId = ARGV[1] 
local quota = tonumber(ARGV[2])

local claimantsKey = "post:{" .. postId .. "}:claimants"
local claimsCountKey = "post:{" .. postId .. "}:claims_count"

-- Fast check: if user already claimed, return immediately
if redis.call('SISMEMBER', claimantsKey, userId) == 1 then
  return 'ALREADY_CLAIMED'
end

-- Use EVAL with conditional logic to minimize Redis calls
local currentClaims = redis.call('GET', claimsCountKey)
if currentClaims == false then
  currentClaims = 0
else 
  currentClaims = tonumber(currentClaims)
end

if currentClaims >= quota then
  return 'QUOTA_EXCEEDED'
end

-- Only increment if we're definitely going to succeed
redis.call('INCR', claimsCountKey)
redis.call('SADD', claimantsKey, userId)
return 'SUCCESS'
