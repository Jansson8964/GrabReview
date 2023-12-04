package com.song.service.impl;


import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.song.dto.Result;
import com.song.dto.UserDTO;
import com.song.entity.SeckillVoucher;
import com.song.entity.VoucherOrder;
import com.song.mapper.VoucherOrderMapper;
import com.song.service.ISeckillVoucherService;
import com.song.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.song.utils.SimpleRedisLock;
import com.song.utils.UniqueIDGenerator;
import com.song.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.IdGenerator;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

import static com.song.utils.RedisConstants.SECKILL_VOUCHER_SAVE_QUEUE;


@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private UniqueIDGenerator idGenerator;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }


    @Override
    // no need to apply the @Transactional annotation since it's only a query
    public Result seckillVoucher(Long voucherId) {
        //Long id = UserHolder.getUser().getId();
        Long userId = UserHolder.getUser().getId();
        long orderId = idGenerator.nextId("order");
        // 1. execute lua script
      //  Long result = stringRedisTemplate.execute(SECKILL_SCRIPT, Collections.emptyList(), voucherId.toString(), userId.toString());
        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT,
                Collections.emptyList(), voucherId.toString(),
                UserHolder.getUser().getId().toString(), String.valueOf(orderId));
        assert result != null;
        int r = result.intValue();
        // 2. check if return value is 0

        if (r != 0) {
            // 2.1 if not, can't buy the voucher
            return Result.fail(r == 1 ? "The voucher amount is not enough" : "u have already bought this voucher");

        }
        // 2.2 if yes, can buy the voucher, save the order to queue
        // TODO save the order to queue
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        rabbitTemplate.convertAndSend(SECKILL_VOUCHER_SAVE_QUEUE, voucherOrder);
        log.info("send seckill voucher order to queue: {}", orderId);

        // 3. return the result
        return Result.ok("success");

    }


//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        // 1. check if the voucher exists
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        // 2. check if the voucher is available
//        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
//            return Result.fail("the voucher is not available yet");
//        }
//        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
//            return Result.fail("the voucher is not available anymore");
//        }
//        if (voucher.getStock() < 1) {
//            return Result.fail("the voucher is sold out");
//        }
//        Long userId = UserHolder.getUser().getId();
//        RLock lock = redissonClient.getLock("lock:order:" + userId);
//        // get the lock
//        boolean isLock = lock.tryLock();
//        if (!isLock) {
//            return Result.fail("The system is busy, please try again later");
//        }
//        try {
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        } finally {
//            lock.unlock();
//        }
//    }


    @Transactional // begin transaction
    public Result createVoucherOrder(Long voucherId) {

        // check if the user has already bought this voucher
        // one user can only buy one voucher
        int count = query().eq("voucher_id", voucherId).eq("user_id", UserHolder.getUser().getId()).count();
        if (count > 0) {
            return Result.fail("u have already bought this voucher");
        }

        // 5.deduct the voucher amount
        boolean success = seckillVoucherService.update()
                .setSql("stock=stock-1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();
        if (!success) {
            return Result.fail("The voucher amount is not enough");
        }
        // 6. create voucher order
        VoucherOrder voucherOrder = new VoucherOrder();
        // 6.1. set order id
        long orderId = idGenerator.nextId("order");
        voucherOrder.setId(orderId);
        // 6.2. set user id
        voucherOrder.setUserId(Long.valueOf((RandomUtil.randomNumbers(8))));
        // 6.3. set voucher id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        // 7.
        return Result.ok(orderId);
    }
}
