-- 재고 차감 + Rate Limit 통합 Lua Script
-- KEYS[1] = stock key (예: product:stock:1)
-- KEYS[2] = rate limit key (예: rate:userId:productId)
-- ARGV[1] = 차감할 수량
-- ARGV[2] = Rate Limit 최대 횟수
-- ARGV[3] = Rate Limit 만료 시간 (초)

local stockKey = KEYS[1]
local rateLimitKey = KEYS[2]
local decreaseAmount = tonumber(ARGV[1])
local maxRequests = tonumber(ARGV[2])
local expireSeconds = tonumber(ARGV[3])

-- 1. Rate Limit 체크
local currentRate = tonumber(redis.call("GET", rateLimitKey)) or 0
if currentRate >= maxRequests then
    return -3  -- Rate Limit 초과
end

-- 2. 현재 재고 조회
local currentStock = tonumber(redis.call("GET", stockKey))

-- 재고 키가 없는 경우
if not currentStock then
    return -2  -- 상품 없음
end

-- 재고 부족
if currentStock < decreaseAmount then
    return -1  -- 재고 부족
end

-- 3. Rate Limit 증가
redis.call("INCR", rateLimitKey)
redis.call("EXPIRE", rateLimitKey, expireSeconds)

-- 4. 재고 차감
redis.call("DECRBY", stockKey, decreaseAmount)

-- 남은 재고 반환
return currentStock - decreaseAmount
