package com.changgou.seckill.task;

import com.alibaba.fastjson.JSON;
import com.changgou.entity.IdWorker;
import com.changgou.entity.Message;
import com.changgou.entity.SeckillStatus;
import com.changgou.seckill.dao.SeckillGoodsMapper;
import com.changgou.seckill.pojo.SeckillGoods;
import com.changgou.seckill.pojo.SeckillOrder;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;

@SuppressWarnings("ALL")
@Component
public class MultiThreadingCreateOrder {
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private IdWorker idWorker;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private Environment env;




    /***
     * 多线程下单操作
     */
    @Async
    public void createOrder(){
        SeckillStatus seckillStatus =(SeckillStatus) redisTemplate.boundListOps("SeckillOrderQueue").rightPop();
        try {//从队列中获取一个商品
            Object sgood = redisTemplate.boundListOps("SeckillGoodsCountList_" + seckillStatus.getGoodsId()).rightPop();
            if(sgood==null){
                //清理当前用户的排队信息
                clearQueue(seckillStatus);
                return;
            }

            if (seckillStatus!=null) {
                //时间区间
                String time = seckillStatus.getTime();
                //用户登录名
                String username = seckillStatus.getUsername();
                //用户抢购商品
                Long id = seckillStatus.getGoodsId();

//            //时间区间
//            String time = "2019052510";
//            //用户登录名
//            String username="szitheima";
//            //用户抢购商品
//            Long id = 1131814847898587136L;
                //获取商品数据
                SeckillGoods goods = (SeckillGoods) redisTemplate.boundHashOps("SeckillGoods_" + time).get(id);

//如果没有库存，则直接抛出异常
                if (goods == null || goods.getStockCount() <= 0) {
                    throw new RuntimeException("已售罄!");
                }
                //如果有库存，则创建秒杀商品订单
                SeckillOrder seckillOrder = new SeckillOrder();
                seckillOrder.setId(idWorker.nextId());
                seckillOrder.setSeckillId(id);
                seckillOrder.setMoney(goods.getCostPrice());
                seckillOrder.setUserId(username);
                seckillOrder.setCreateTime(new Date());
                seckillOrder.setStatus("0");

                //将秒杀订单存入到Redis中
                redisTemplate.boundHashOps("SeckillOrder").put(username, seckillOrder);
//库存减少
                goods.setStockCount(goods.getStockCount() - 1);
                //判断当前商品是否还有库存
                if (goods.getStockCount() <= 0) {
                    //并且将商品数据同步到MySQL中
                    seckillGoodsMapper.updateByPrimaryKeySelective(goods);
                    //如果没有库存,则清空Redis缓存中该商品
                    redisTemplate.boundHashOps("SeckillGoods_" + time).delete(id);
                } else {
                    //如果有库存，则直数据重置到Reids中
                    redisTemplate.boundHashOps("SeckillGoods_" + time).put(id, goods);
                }
//            System.out.println("准备执行....");
//            Thread.sleep(20000);
//            System.out.println("开始执行....");
                //抢单成功，更新抢单状态,排队->等待支付
                seckillStatus.setStatus(2);
                seckillStatus.setOrderId(seckillOrder.getId());
                seckillStatus.setMoney(Float.valueOf(seckillOrder.getMoney()));
                sendTimerMessage(seckillStatus);
                redisTemplate.boundHashOps("UserQueueStatus").put(username,seckillStatus);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /***
     * 清理用户排队信息
     * @param seckillStatus
     */
    private void clearQueue(SeckillStatus seckillStatus) {
        //清理排队标示
        redisTemplate.boundHashOps("UserQueueCount").delete(seckillStatus.getUsername());

        //清理抢单标示
        redisTemplate.boundHashOps("UserQueueStatus").delete(seckillStatus.getUsername());
    }
    /***
     * 发送延时消息到RabbitMQ中
     * @param seckillStatus
     */
    public void sendTimerMessage(SeckillStatus seckillStatus){
        rabbitTemplate.convertAndSend(env.getProperty("mq.pay.queue.seckillordertimerdelay"), (Object) JSON.toJSONString(seckillStatus), new MessagePostProcessor() {
            @Override
            public org.springframework.amqp.core.Message postProcessMessage(org.springframework.amqp.core.Message message) throws AmqpException {
                message.getMessageProperties().setExpiration("10000");
                return message;
            }
        });
    }
}
