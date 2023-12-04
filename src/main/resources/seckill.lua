-- 1. params: voucherId, userId
local voucherId = ARGV[1]
local userId = ARGV[2]
local id = ARGV[3]

-- 2. data
-- 2.1 stock key
local stockKey = 'seckill:stock:' .. voucherId
-- 2.2 order key
local orderKey = 'seckill:order:' .. voucherId

-- All operations in redis are related to string,need to use tonumber to convert
-- check if the stock is enough
if (tonumber(redis.call('get', stockKey)) <= 0) then
    return 1
end

-- check if the user has already bought the voucher
if (redis.call('sismember', orderKey, userId) == 1) then
    return 2
end
-- deduct the stock and add the order
redis.call('incrby', stockKey, -1)
redis.call('sadd', orderKey, userId)
--
redis.call('xadd','stream.orders','*','userId',userId,'voucherId',voucherId,'id',id)
return 0


