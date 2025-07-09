-- KEYS[1]: post_id
-- ARGV[1]: user_id
-- ARGV[2]: quota

local postId = KEYS[1]
local userId = ARGV[1]
local quota = tonumber(ARGV[2])

local claimantsKey = "post:{" .. postId .. "}:claimants"
local claimsCountKey = "post:{" .. postId .. "}:claims_count"

-- Check if user has already claimed
if redis.call('SISMEMBER', claimantsKey, userId) == 1 then
  return 'ALREADY_CLAIMED'
end

local currentClaims = redis.call('INCR', claimsCountKey)

if currentClaims > quota then
  -- Rollback increment if quota is exceeded
  redis.call('DECR', claimsCountKey)
  return 'QUOTA_EXCEEDED'
else
  redis.call('SADD', claimantsKey, userId)
  return 'SUCCESS'
end
