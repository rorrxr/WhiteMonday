-- 재고 복구 Lua Script
-- KEYS[1] = stock key (예: product:stock:1)
-- ARGV[1] = 복구할 수량

local stockKey = KEYS[1]
local restoreAmount = tonumber(ARGV[1])

-- 재고 증가 (키가 없으면 생성)
local newStock = redis.call("INCRBY", stockKey, restoreAmount)

return newStock
