package com.song.controller;


import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.song.dto.Result;
import com.song.entity.ShopType;
import com.song.service.IShopTypeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.song.utils.RedisConstants.CACHE_SHOP_TTL;
import static com.song.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;


@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping("list")
    public Result queryTypeList() {
        // 1. check if type list is existed in redis
        String typeListJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_TYPE_KEY);
        // 2. if existed, return data directly
        if (StrUtil.isNotBlank(typeListJson)) {
            // deserialize json string to type list
            List<ShopType> typeList = JSONUtil.toList(typeListJson, ShopType.class);
            return Result.ok(typeList);
        }
        // 3. if not existed, query from database
        else {
            List<ShopType> typeList = typeService
                    .query().orderByAsc("sort").list();
            // 4. if existed in database, save to redis
            // serialize type list to json string
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_TYPE_KEY, JSONUtil.toJsonStr(typeList));
            // 5. return data
            return Result.ok(typeList);
        }
    }
}
