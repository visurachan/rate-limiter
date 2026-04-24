-- token_bucket.lua
-- KEYS[1] = bucket key e.g. "rl:banking:user-abc123"
-- ARGV[1] = capacity (max tokens)
-- ARGV[2] = refillRate (tokens per second)
-- ARGV[3] = now (current time in milliseconds)

local key          = KEYS[1]
local capacity     = tonumber(ARGV[1])
local refillRate   = tonumber(ARGV[2])
local now          = tonumber(ARGV[3])

-- 1. Get current state
local data         = redis.call("HMGET", key, "tokens", "lastRefill")
local lastTokens   = tonumber(data[1]) or capacity
local lastRefill   = tonumber(data[2]) or now

-- 2. Calculate refill based on time passed
local elapsed      = math.max(0, now - lastRefill)
local refillAmount = (elapsed * refillRate) / 1000.0
local tokens       = math.min(capacity, lastTokens + refillAmount)

-- 3. Decision Logic
if tokens >= 1 then
    -- Consumed: Update everything
    redis.call("HMSET", key, "tokens", tokens - 1, "lastRefill", now)
    redis.call("PEXPIRE", key, 3600000) -- 1 hour TTL
    return {1, math.floor(tokens - 1)}
else
    -- Blocked: DO NOT update lastRefill
    -- This ensures the next token arrives exactly when it's supposed to
    return {0, math.floor(tokens)}
end