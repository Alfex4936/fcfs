-- KEYS[1]: post_id
-- ARGV[1]: user_id

local postId = KEYS[1]
local userId = ARGV[1]

local claimantsKey = "post:{" .. postId .. "}:claimants"
local claimsCountKey = "post:{" .. postId .. "}:claims_count"

-- Check if user has already claimed
if redis.call('SISMEMBER', claimantsKey, userId) == 0 then
  return 'NOT_CLAIMED'
end

redis.call('SREM', claimantsKey, userId)
redis.call('DECR', claimsCountKey)

return 'SUCCESS'
