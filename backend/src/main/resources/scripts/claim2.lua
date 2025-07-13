-- KEYS[1] = post_id
-- ARGV[1] = user_id
-- ARGV[2] = quota

local setKey = KEYS[1]
local cntKey = KEYS[2]
local uid     = ARGV[1]
local quota   = tonumber(ARGV[2])

-- Performance optimization: Check count first to avoid unnecessary SADD
local currentCount = redis.call('GET', cntKey)
if currentCount and tonumber(currentCount) >= quota then
  return 'QUOTA_EXCEEDED'
end

-- Attempt to add first (1 = added, 0 = existed)
if redis.call('SADD', setKey, uid) == 0 then
  return 'ALREADY_CLAIMED'
end

local newCnt = redis.call('INCR', cntKey)
if newCnt > quota then
  -- Roll back atomically
  redis.call('SREM', setKey, uid)
  redis.call('DECR', cntKey)
  return 'QUOTA_EXCEEDED'
end

-- Optional: Set expiration for cleanup (24 hours = 86400 seconds)
-- Uncomment if you want automatic cleanup
-- redis.call('EXPIRE', setKey, 86400)
-- redis.call('EXPIRE', cntKey, 86400)

return 'SUCCESS'

-- TODO: Add logic to expire the claim after a certain time if needed