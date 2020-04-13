package com.changgou.order.feign;
import com.changgou.entity.Result;
import com.changgou.order.pojo.CategoryReport;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/****
 * @Author:sz.itheima
 * @Description:
 * @Date 2019/6/18 13:58
 *****/
@FeignClient(name="order")
@RequestMapping("/categoryReport")
public interface CategoryReportFeign {
    

    /***
     * 多条件搜索品牌数据
     * @param categoryReport
     * @return
     */
    @PostMapping(value = "/search" )
    Result<List<CategoryReport>> findList(@RequestBody(required = false) CategoryReport categoryReport);

    /***
     * 根据ID删除品牌数据
     * @param id
     * @return
     */
    @DeleteMapping(value = "/{id}" )
    Result delete(@PathVariable (value = "id") Date id);

    /***
     * 修改CategoryReport数据
     * @param categoryReport
     * @param id
     * @return
     */
    @PutMapping(value="/{id}")
    Result update(@RequestBody CategoryReport categoryReport, @PathVariable (value = "id") Date id);

    /***
     * 新增CategoryReport数据
     * @param categoryReport
     * @return
     */
    @PostMapping
    Result add(@RequestBody CategoryReport categoryReport);

    /***
     * 根据ID查询CategoryReport数据
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    Result<CategoryReport> findById(@PathVariable (value = "id") Date id);

    /***
     * 查询CategoryReport全部数据
     * @return
     */
    @GetMapping
    Result<List<CategoryReport>> findAll();
}