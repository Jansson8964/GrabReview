package com.song.listener;

import cn.hutool.json.JSONUtil;
import com.song.entity.VoucherOrder;
import com.song.service.ISeckillVoucherService;
import com.song.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


import static com.song.utils.RedisConstants.SECKILL_VOUCHER_SAVE_QUEUE;

@Component
@Slf4j
public class AsynSaveVoucherListener {
    @Resource
    private IVoucherOrderService voucherOrderService;
    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @RabbitListener(queuesToDeclare = {@Queue(name = SECKILL_VOUCHER_SAVE_QUEUE)})
    public void AsyncSave(VoucherOrder voucherOrder) {
        //  log.info("接收到存储订单信息的消息,{}", JSONUtil.toJsonStr(voucherOrder));
        log.info("receive message from queue:{}", JSONUtil.toJsonStr(voucherOrder));

        boolean success = seckillVoucherService.update().setSql("stock=stock-1")
                .eq("voucher_id", voucherOrder.getVoucherId()).gt("stock", 0).update();
        voucherOrderService.save(voucherOrder);
        // log.info("订单信息存储完成?{}", success);
        log.info("save order info success{}", success);

    }

}
