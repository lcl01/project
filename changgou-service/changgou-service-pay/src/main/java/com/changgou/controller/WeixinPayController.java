package com.changgou.controller;

import com.alibaba.fastjson.JSON;
import com.changgou.entity.Result;
import com.changgou.entity.StatusCode;
import com.changgou.service.WeixinPayService;
import com.github.wxpay.sdk.WXPayUtil;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/weixin/pay")
public class WeixinPayController {
    @Value("${mq.pay.exchange.order}")
    private String exchange;
    @Value("${mq.pay.queue.order}")
    private String queue;
    @Value("${mq.pay.routing.key}")
    private String routing;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private WeixinPayService weixinPayService;
    /***
     * 创建二维码
     * @return
     */
    @RequestMapping(value = "/create/native")
//    public Result createNative(String outtradeno, String money){
    public Result createNative(@RequestParam Map<String,String> parameter){
        Map<String,String> resultMap = weixinPayService.createNative(parameter);
        return new Result(true, StatusCode.OK,"创建二维码预付订单成功！",resultMap);
    }
    /***
     * 查询支付状态
     * @param outtradeno
     * @return
     */
    @GetMapping(value = "/status/query")
    public Result queryStatus(String outtradeno){
        Map<String,String> resultMap = weixinPayService.queryPayStatus(outtradeno);
        return new Result(true,StatusCode.OK,"查询状态成功！",resultMap);
    }
    /***
     * 支付回调
     * @param request
     * @return
     */
    @RequestMapping(value = "/notify/url")
    public String notifyUrl(HttpServletRequest request){
        InputStream inStream;
        try {
            //读取支付回调数据
            inStream = request.getInputStream();
            ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = inStream.read(buffer)) != -1) {
                outSteam.write(buffer, 0, len);
            }
            outSteam.close();
            inStream.close();
            // 将支付回调数据转换成xml字符串
            String result = new String(outSteam.toByteArray(), "utf-8");
            //将xml字符串转换成Map结构
            Map<String, String> map = WXPayUtil.xmlToMap(result);
            //获取附加信息
            Map<String,String> attach = JSON.parseObject(map.get("attach"), Map.class);
            System.out.println(attach);
            rabbitTemplate.convertAndSend(exchange,attach.get("queue"), JSON.toJSONString(map));
            //响应数据设置
            Map respMap = new HashMap();
            respMap.put("return_code","SUCCESS");
            respMap.put("return_msg","OK");
            return WXPayUtil.mapToXml(respMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    @RequestMapping("/closePay")
    public Result closePay(@PathVariable Long orderId) throws Exception {
        Map<String, String> closePay = weixinPayService.closePay(orderId);
        return new Result(true,StatusCode.OK,"取消支付",closePay);
    }
}
