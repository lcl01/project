package com.changgou.feign;

import com.changgou.entity.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient(name = "pay")
@RequestMapping("/weixin/pay")
public interface WeixinPayFeign {
    @RequestMapping("/closePay")
    public Result closePay(@PathVariable Long orderId);
}
