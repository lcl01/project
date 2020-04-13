package com.changgou.order.feign;
import com.changgou.entity.Result;
import com.changgou.order.pojo.OrderConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/****
 * @Author:sz.itheima
 * @Description:
 * @Date 2019/6/18 13:58
 *****/
@FeignClient(name="order")
@RequestMapping("/orderConfig")
public interface OrderConfigFeign {

    
    /***
     * 多条件搜索品牌数据
     * @param orderConfig
     * @return
     */
    @PostMapping(value = "/search" )
    Result<List<OrderConfig>> findList(@RequestBody(required = false) OrderConfig orderConfig);

    /***
     * 根据ID删除品牌数据
     * @param id
     * @return
     */
    @DeleteMapping(value = "/{id}" )
    Result delete(@PathVariable (value = "id") Integer id);

    /***
     * 修改OrderConfig数据
     * @param orderConfig
     * @param id
     * @return
     */
    @PutMapping(value="/{id}")
    Result update(@RequestBody OrderConfig orderConfig, @PathVariable (value = "id") Integer id);

    /***
     * 新增OrderConfig数据
     * @param orderConfig
     * @return
     */
    @PostMapping
    Result add(@RequestBody OrderConfig orderConfig);

    /***
     * 根据ID查询OrderConfig数据
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    Result<OrderConfig> findById(@PathVariable (value = "id") Integer id);

    /***
     * 查询OrderConfig全部数据
     * @return
     */
    @GetMapping
    Result<List<OrderConfig>> findAll();
}