package com.changgou.goods.controller;

import com.changgou.entity.Result;
import com.changgou.entity.StatusCode;
import com.changgou.goods.pojo.Brand;
import com.changgou.goods.service.BrandService;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/brand")
@CrossOrigin
public class BrandController {
    @Autowired
    private BrandService brankService;
    /***
     * 查询全部数据
     * @return
     */
    @GetMapping
    public Result<Brand> findAll(){
        List<Brand> brandList = brankService.findAll();
//        int i=1/0;
        return new Result<Brand>(true, StatusCode.OK,"查询成功",brandList);
    }
    /***
     * 根据ID查询品牌数据
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<Brand> findById(@PathVariable Integer id){
//        Id查询
        Brand brand = brankService.findById(id);
        return new Result<Brand>(true,StatusCode.OK,"查询成功",brand);
    }
    /***
     * 新增品牌数据
     * @param brand
     * @return
     */
    @PostMapping
    public Result add(@RequestBody Brand brand){
        brankService.add(brand);
        return new Result(true,StatusCode.OK,"添加成功");
    }
    /***
     * 修改品牌数据
     * @param brand
     * @param id
     * @return
     */
    @PostMapping("/{id}")
    public Result update(@RequestBody Brand brand,@PathVariable Integer id){
        //设置ID
        brand.setId(id);
        //修改数据
        brankService.update(brand);
        return new Result(true,StatusCode.OK,"修改成功");
    }
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id){
        brankService.delete(id);
        return new Result(true,StatusCode.OK,"删除成功");
    }
/***
 * 多条件搜索品牌数据
 * @param brand
 * @return
 */
    @PostMapping("/search")
    public Result<List<Brand>> findList(@RequestBody(required = false) Brand brand){
        List<Brand> list = brankService.findList(brand);
        return new Result<List<Brand>>(true,StatusCode.OK,"查询成功",list);
    }
    /***
     * 分页搜索实现
     * @param page:当前页
     * @param size:每页显示多少条
     * @return
     */
    @GetMapping("/search/{page}/{size}")
    public Result<PageInfo> findPage(@PathVariable int page,@PathVariable int size){
        //分页查询
        PageInfo<Brand> pageInfo = brankService.findPage(page, size);
        return new Result<PageInfo>(true,StatusCode.OK,"查询成功",pageInfo);
    }
    /***
     * 分页搜索实现
     * @param brand
     * @param page
     * @param size
     * @return
     */
    @PostMapping("/search/{page}/{size}")
    public Result<PageInfo> findPage(@RequestBody(required = false) Brand brand,@PathVariable int page,@PathVariable int size){
        PageInfo<Brand> pageInfo = brankService.findPage(brand, page, size);

        return new Result(true,StatusCode.OK,"查询成功",pageInfo);
    }
/***
 * 根据分类实现品牌列表查询
 * /brand/category/{id}  分类ID
 */
@GetMapping("/category/{id}")
    public Result<List<Brand>> findByCategory(@PathVariable(value = "id") Integer categoryId){
    List<Brand> categoryList = brankService.findByCategory(categoryId);
    return new Result<List<Brand>>(true,StatusCode.OK,"查询成功",categoryList);
}

}
