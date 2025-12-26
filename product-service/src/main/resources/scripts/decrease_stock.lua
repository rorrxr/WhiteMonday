-- 재고 차감 Lua Script
-- KEYS[1] = stock key (예: product:stock:1)
-- ARGV[1] = 차감할 수량

local stockKey = KEYS[1]
local decreaseAmount = tonumber(ARGV[1])

-- 현재 재고 조회
local currentStock = tonumber(redis.call("GET", stockKey))

-- 재고 키가 없는 경우
if not currentStock then
    return -2
end

-- 재고 부족
if currentStock < decreaseAmount then
    return -1
end

-- 재고 차감
redis.call("DECRBY", stockKey, decreaseAmount)

-- 남은 재고 반환
return currentStock - decreaseAmount
