package com.song.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.song.dto.Result;
import com.song.entity.Shop;
import com.song.mapper.ShopMapper;
import com.song.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.song.utils.CacheClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static com.song.utils.RedisConstants.*;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    StringRedisTemplate stringRedisTemplate;

    public ShopServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Resource
    private CacheClient cacheClient;

    @Override
    @Transactional
    public Result update(Shop shop) {
        // 1. check if shop id is null
        if (shop.getId() == null) {
            return Result.fail("shop id is null");
        }

        // First update the shop data in the database, then delete the shop cache.
        // 2. update database
        updateById(shop);
        //3. delete redis cache
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }

    @Override
    public Result queryById(Long id) {
        // 1.solve Cache penetration with null value caching
        Shop shop = cacheClient.queryWithCachePenetration(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.SECONDS);
        if (shop == null) {
            return Result.fail("shop is not exist");
        }
        return Result.ok(shop);
        // 2.solve Cache breakdown with mutex lock
        // Shop shop = cacheClient
        //         .queryWithMutex(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
    }


    // 在逻辑过期方案中，缓存的过期时间并不会直接设置在 Redis 的 TTL 字段中。而是会在缓存的 value 字段中存储一个过期时间戳。
}

