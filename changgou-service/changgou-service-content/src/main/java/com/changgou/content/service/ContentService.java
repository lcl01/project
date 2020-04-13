package com.changgou.content.service;
import com.changgou.content.pojo.Content;
import com.github.pagehelper.PageInfo;
import java.util.List;
/****
 * @Author:sz.itheima
 * @Description:Content业务层接口
 * @Date 2019/6/14 0:16
 *****/
public interface ContentService {


    /***
     * 根据categoryId查询广告集合
     * @param id
     * @return
     */
    List<Content> findByCategory(Long id);
}
