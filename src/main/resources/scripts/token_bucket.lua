-- token_bucket.lua
-- KEYS[1] = bucket key e.g. "rl:banking:user-abc123"
-- ARGV[1] = capacity (max tokens)
-- ARGV[2] = refillRate (tokens per second)
-- ARGV[3] = now (current time in milliseconds)

local key        = KEYS[1]
local capacity   = tonumber(ARGV[1])
local refillRate = tonumber(ARGV[2])
local now        = tonumber(ARGV[3])

-- Read current bucket state from Redis
local data       = redis.call("HMGET", key, "tokens", "lastRefill")
local tokens     = tonumber(data[1]) or capacity
local lastRefill = tonumber(data[2]) or now

-- Calculate how many tokens have refilled since last request
local elapsed    = (now - lastRefill) / 1000.0
local newTokens  = math.min(capacity, tokens + (elapsed * refillRate))

if newTokens >= 1 then
    -- Consume one token and save state
    redis.call("HMSET", key, "tokens", newTokens - 1, "lastRefill", now)
    redis.call("PEXPIRE", key, 3600000)
    return {1, math.floor(newTokens - 1)}  -- allowed, remaining
else
    -- No tokens, save partial refill state
    redis.call("HMSET", key, "tokens", newTokens, "lastRefill", now)
    return {0, 0}  -- blocked
end