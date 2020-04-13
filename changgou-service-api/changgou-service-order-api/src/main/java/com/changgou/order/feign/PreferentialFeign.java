package com.changgou.order.feign;
import com.changgou.entity.Result;
import com.changgou.order.pojo.Preferential;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/****
 * @Author:sz.itheima
 * @Description:
 * @Date 2019/6/18 13:58
 *****/
@FeignClient(name="order")
@RequestMapping("/preferential")
public interface PreferentialFeign {

//    /***
//     * Preferential分页条件搜索实现
//     * @param preferential
//     * @param page
//     * @param size
//     * @return
//     */
//    @PostMapping(value = "/search/{page}/{size}" )
//    Result<PageInfo> findPage(@RequestBody(required = false) Preferential preferential, @PathVariable (value = "id") int page, @PathVariable (value = "id") int size);
//
//    /***
//     * Preferential分页搜索实现
//     * @param page:当前页
//     * @param size:每页显示多少条
//     * @return
//     */
//    @GetMapping(value = "/search/{page}/{size}" )
//    Result<PageInfo> findPage(@PathVariable (value = "id") int page, @PathVariable (value = "id") int size);

    /***
     * 多条件搜索品牌数据
     * @param preferential
     * @return
     */
    @PostMapping(value = "/search" )
    Result<List<Preferential>> findList(@RequestBody(required = false) Preferential preferential);

    /***
     * 根据ID删除品牌数据
     * @param id
     * @return
     */
    @DeleteMapping(value = "/{id}" )
    Result delete(@PathVariable (value = "id") Integer id);

    /***
     * 修改Preferential数据
     * @param preferential
     * @param id
     * @return
     */
    @PutMapping(value="/{id}")
    Result update(@RequestBody Preferential preferential, @PathVariable (value = "id") Integer id);

    /***
     * 新增Preferential数据
     * @param preferential
     * @return
     */
    @PostMapping
    Result add(@RequestBody Preferential preferential);

    /***
     * 根据ID查询Preferential数据
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    Result<Preferential> findById(@PathVariable (value = "id") Integer id);

    /***
     * 查询Preferential全部数据
     * @return
     */
    @GetMapping
    Result<List<Preferential>> findAll();
}