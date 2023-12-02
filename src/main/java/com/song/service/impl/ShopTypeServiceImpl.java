package com.song.service.impl;

import com.song.entity.ShopType;
import com.song.mapper.ShopTypeMapper;
import com.song.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;


@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

}
