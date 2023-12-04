package com.song.service;

import com.song.dto.Result;
import com.song.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;


public interface IShopService extends IService<Shop> {

    Result update(Shop shop);

    Result queryById(Long id);

    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);
}
