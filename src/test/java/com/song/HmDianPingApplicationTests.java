package com.song;

import com.song.mapper.ShopMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HmDianPingApplicationTests {
    @Autowired
    private ShopMapper shopMapper;
    // getShopById


    public ShopMapper getShopMapper() {
        return shopMapper;
    }
}
