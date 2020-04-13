package com.changgou.order.consumer;

import com.alibaba.fastjson.JSON;
import com.changgou.order.service.OrderService;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RabbitListener(queues = {"${mq.pay.queue.order}"})
public class OrderPayMessageListener {
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private OrderService orderService;
    /***
     * 接收消息
     */
    @RabbitHandler
    public void consumeMessage(String msg){
        //将数据转成Map
        Map<String,String> result = JSON.parseObject(msg, Map.class);
        //return_code=SUCCESS
        String return_code = result.get("return_code");
        //业务结果
        String result_code = result.get("result_code");
        if (return_code.equalsIgnoreCase("success")) {
            //获取订单号
            String outtradeno = result.get("out_trade_no");
            if (result_code.equalsIgnoreCase("success")) {
                if (outtradeno!=null) {
                    //修改订单状态  out_trade_no
                    orderService.updateStatus(outtradeno,result.get("transaction_id"));
                }
            }else {
                orderService.deleteOrder(outtradeno);
            }
        }
    }
}
