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

-- Set 30-day expiry for memory management (2592000 seconds = 30 days)
-- This prevents memory leaks while allowing for post reopening scenarios
redis.call('EXPIRE', setKey, 2592000)
redis.call('EXPIRE', cntKey, 2592000)

return 'SUCCESS'
