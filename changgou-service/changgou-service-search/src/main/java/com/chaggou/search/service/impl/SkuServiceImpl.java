package com.chaggou.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.chaggou.search.dao.SkuEsMapper;
import com.chaggou.search.service.SkuService;
import com.changgou.entity.Result;
import com.changgou.goods.feign.SkuFeign;
import com.changgou.goods.pojo.Sku;
import com.changgou.search.pojo.SkuInfo;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("ALL")
@Service
public class SkuServiceImpl implements SkuService {
    @Autowired
    private ElasticsearchTemplate esTemplate;
    @Autowired
    private SkuFeign skuFeign;
    @Autowired
    private SkuEsMapper skuEsMapper;
    /**
     * 导入sku数据到es
     */
    @Override
    public void importSku() {
//调用changgou-service-goods微服务
        Result<List<Sku>> skuListResult = skuFeign.findByStatus("1");
        //将数据转成search.Sku
        List<SkuInfo> skuInfos=  JSON.parseArray(JSON.toJSONString(skuListResult.getData()),SkuInfo.class);
        for(SkuInfo skuInfo:skuInfos){
            Map<String, Object> specMap= JSON.parseObject(skuInfo.getSpec()) ;
            skuInfo.setSpecMap(specMap);
        }
        skuEsMapper.saveAll(skuInfos);
    }
    /**
     * 搜索数据
     * @param searchMap
     * @return
     */
    @Override
    public Map search(Map<String, String> searchMap) {
        //1.条件构建
        NativeSearchQueryBuilder builder = buildBasicQuery(searchMap);
//2.搜索列表
        Map resultMap = searchList(builder);
//3.分组搜索
        if (searchMap==null || searchMap.get("category")==null) {
            List<String> categoryList = searchCategoryList(builder);
            resultMap.put("categoryList",categoryList);
        }

        //4.查询分类对应的品牌
        if (searchMap==null || searchMap.get("brand")==null) {
            List<String> brandList = searchBrandList(builder);
            resultMap.put("brandList",brandList);
        }
        //5.查询规格数据
        Map<String, Set<String>> specMap = searchSpec(builder);
        resultMap.put("specList",specMap);
        //分页数据保存
        resultMap.put("pageNum",builder.build().getPageable().getPageNumber()+1);
        resultMap.put("pageSize",builder.build().getPageable().getPageSize());
        return resultMap;
    }
    /**
     * 查询规格列表
     * @param queryBuilder
     * @return
     */
    private Map<String, Set<String>> searchSpec(NativeSearchQueryBuilder queryBuilder) {
        //查询聚合品牌  skuBrandGroupby给聚合分组结果起个别名
        String skuSpec = "skuSpec";
        queryBuilder.addAggregation(AggregationBuilders.terms(skuSpec).field("spec.keyword"));
        //执行搜索
        AggregatedPage<SkuInfo> result = esTemplate.queryForPage(queryBuilder.build(), SkuInfo.class);
        //获取聚合规格数据结果
        Aggregations aggs = result.getAggregations();
        //获取分组结果
        StringTerms terms = aggs.get(skuSpec);
        //返回规格数据名称
        List<String> sku_specList = terms.getBuckets().stream().map(b -> b.getKeyAsString()).collect(Collectors.toList());
        //将规格转成Map
        Map<String, Set<String>> specMap = specPutAll(sku_specList);

        return specMap;
    }
    /***
     * 将所有规格数据转入到Map中
     * @param specList
     * @return
     */
    private Map<String, Set<String>> specPutAll(List<String> specList) {
        //新建一个Map
        Map<String,Set<String>> specMap = new HashMap<String,Set<String>>();
        for (String specString : specList) {
            //将Map数据转成Map
            Map<String,String> map = JSON.parseObject(specString, Map.class);
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();//规格名字
                String value = entry.getValue(); //规格选项值
                //获取当前规格名字对应的规格数据
                Set<String> specValues = specMap.get(key);
                if(specValues==null){
                    specValues = new HashSet<String>();
                }
                //将当前规格加入到集合中
                specValues.add(value);
                //将数据存入到specMap中
                specMap.put(key,specValues);
            }
        }
        return specMap;
    }

    /***
     * 数据搜索
     * @param builder
     * @return
     */
    private Map searchList(NativeSearchQueryBuilder builder){
        Map resultMap=new HashMap();//返回结果
        //高亮域配置
        HighlightBuilder.Field field = new HighlightBuilder.
                Field("name").                      //指定的高亮域
                preTags("<span style=\"color:red\">").   //前缀
                postTags("</span>").                      //后缀
                fragmentSize(100);
        //添加高亮域
        builder.withHighlightFields(field);

        //查询解析器
        NativeSearchQuery searchQuery = builder.build();
//        Page<SkuInfo> skuPage = esTemplate.queryForPage(searchQuery,SkuInfo.class);
        //分页搜索
        AggregatedPage<SkuInfo> skuPage = esTemplate.queryForPage(searchQuery, SkuInfo.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
                //定义一个集合存储所有高亮数据
                List<T> list = new ArrayList<T>();

                //循环所有数据
                for (SearchHit hit : response.getHits()) {
                    //获取非高亮数据           例如：小白真美丽      {"name":"张三","age":27}
                    SkuInfo skuInfo = JSON.parseObject(hit.getSourceAsString(), SkuInfo.class);

                    //获取高亮数据            例如：小白真 <span style="color:red;">美丽</span>
                    HighlightField titleHighlight = hit.getHighlightFields().get("name");      //获取标题的高亮数据

                    if (titleHighlight != null) {
                        //定义一个字符接收高亮数据
                        StringBuffer buffer = new StringBuffer();
                        //循环获取高亮数据
                        for (Text text : titleHighlight.getFragments()) {
                            //text.toString():   小白真<span style="color:red;">美丽</span>啊
                            buffer.append(text.toString());
                        }
                        //将非高亮数据替换成高亮数据    小白真美丽-->小白真 <span style="color:red;">美丽</span>
                        skuInfo.setName(buffer.toString());
                    }

                    //将高亮数据返回
                    list.add((T) skuInfo);
                }
                //1：返回的集合数据   2：分页数据   3：总记录数
                return new AggregatedPageImpl<T>(list, pageable, response.getHits().getTotalHits());
            }
        });

        //存储对应数据
        resultMap.put("rows",skuPage.getContent());
        resultMap.put("totalPages",skuPage.getTotalPages());
        return resultMap;
    }
    /**
     * 构建基本查询
     * @param searchMap
     * @return
     */
    private NativeSearchQueryBuilder buildBasicQuery(Map<String,String> searchMap) {
        // 查询构建器
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        //构建布尔查询
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();

        if(searchMap!=null){
            //1.关键字查询
            if(!StringUtils.isEmpty(searchMap.get("keywords"))){
                nativeSearchQueryBuilder.withQuery(QueryBuilders.matchQuery("name",searchMap.get("keywords")));
            }
            //分类筛选
            if(!StringUtils.isEmpty(searchMap.get("category"))){
                queryBuilder.must(QueryBuilders.matchQuery("categoryName",searchMap.get("category")));
            }

            //品牌
            if(!StringUtils.isEmpty(searchMap.get("brand"))){
                queryBuilder.must(QueryBuilders.termQuery("brandName",searchMap.get("brand")));
            }
            //规格
            for (String key : searchMap.keySet()) {
                //如果是规格
                if (key.startsWith("spec_")) {
                    String value = searchMap.get(key).replace("\\", "");
                    queryBuilder.must(QueryBuilders.matchQuery("specMap."+key.substring(5)+".keyword",searchMap.get(key)));
                }

            }
            //价格区间过滤
            String price = searchMap.get("price");
            if(!StringUtils.isEmpty(price)){
                //去掉元和以上
                price=price.replace("元","").replace("以上","");
                //根据-分割
                String[] array = price.split("-");
                //x<price
                queryBuilder.must(QueryBuilders.rangeQuery("price").gt(array[0]));
                if(array.length==2){
                    //price<=y
                    queryBuilder.must(QueryBuilders.rangeQuery("price").lte(array[1]));
                }
            }
            //排序实现
            String sortRule=searchMap.get("sortRule");//排序规则 ASC  DESC
            String sortField=searchMap.get("sortField");//排序字段  price
            if(!StringUtils.isEmpty(sortField)){
                nativeSearchQueryBuilder.withSort(SortBuilders.fieldSort(sortField).order(SortOrder.valueOf(sortRule)));
            }


        }
        //分页
        Integer pageNo =pageConvert(searchMap);//页码
        Integer pageSize = 3;//页大小
        PageRequest pageRequest = PageRequest.of( pageNo- 1, pageSize);
        nativeSearchQueryBuilder.withPageable(pageRequest);
        nativeSearchQueryBuilder.withQuery(queryBuilder);
        return nativeSearchQueryBuilder;
    }

    private Integer pageConvert(Map<String, String> searchMap) {
        try {
            return Integer.parseInt(searchMap.get("pageNum"));
        } catch (Exception e) {
        }
        return 1;
    }

    /***
     * 搜索分类分组数据
     */
    public List<String> searchCategoryList(NativeSearchQueryBuilder builder){
        /***
         * 指定分类域，并根据分类域配置聚合查询
         * 1:给分组查询取别名
         * 2:指定分组查询的域
         */
        builder.addAggregation(AggregationBuilders.terms("skuCategory").field("categoryName"));

        //执行搜索
        AggregatedPage<SkuInfo> skuPage = esTemplate.queryForPage(builder.build(), SkuInfo.class);

        //获取所有分组查询的数据
        Aggregations aggregations = skuPage.getAggregations();
        //从所有数据中获取别名为skuCategory的数据
        StringTerms terms = aggregations.get("skuCategory");

        //分装List集合，将搜索结果存入到List集合中
        List<String> categoryList = new ArrayList<String>();
        for (StringTerms.Bucket bucket : terms.getBuckets()) {
            categoryList.add(bucket.getKeyAsString());
        }
        return categoryList;
    }
    /**
     * 查询品牌列表
     * @param queryBuilder
     * @return
     */
    public List<String> searchBrandList(NativeSearchQueryBuilder queryBuilder) {
        //查询聚合品牌  skuBrandGroupby给聚合分组结果起个别名
        String skuBrand = "skuBrand";
        queryBuilder.addAggregation(AggregationBuilders.terms(skuBrand).field("brandName"));

        //执行搜索
        AggregatedPage<SkuInfo> result = esTemplate.queryForPage(queryBuilder.build(), SkuInfo.class);
        //获取聚合品牌结果
        Aggregations aggs = result.getAggregations();
        //获取分组结果
        StringTerms terms = aggs.get(skuBrand);

        //返回品牌名称
        List<String> sku_brandList = terms.getBuckets().stream().map(b -> b.getKeyAsString()).collect(Collectors.toList());
        return sku_brandList;
    }
    /****
     * 分组搜索实现
     * @param nativeSearchQueryBuilder
     * @return
     */
    public Map groupList(NativeSearchQueryBuilder nativeSearchQueryBuilder, Map<String,String> searchMap){
        /****
         * 根据分类名字|品牌名字|规格进行分组查询
         * 1:给当前分组取一个别名
         * 2:分组的域的名字
         */
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuSpec").field("spec.keyword"));
        if(searchMap==null || StringUtils.isEmpty(searchMap.get("category"))){
            //分类
            nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuCategory").field("categoryName"));
        }
        if(searchMap==null || StringUtils.isEmpty(searchMap.get("brand"))){
            //品牌合并分组查询
            nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuBrand").field("brandName"));
        }

        //实现分组搜索
        AggregatedPage<SkuInfo> categoryPage = esTemplate.queryForPage(nativeSearchQueryBuilder.build(), SkuInfo.class);
        //获取所有分组数据
        Aggregations aggregations = categoryPage.getAggregations();

        //存储所有数据
        Map groupMap = new HashMap();

        //获取规格数据
        List<String> specList = getGroupData(aggregations, "skuSpec");
        Map<String, Set<String>> specMap = putAllSpec(specList);
        groupMap.put("specList",specMap);

        //分类分组实现
        if(searchMap==null || StringUtils.isEmpty(searchMap.get("category"))){
            List<String> categoryList = getGroupData(aggregations,"skuCategory");
            groupMap.put("categoryList",categoryList);
        }

        //品牌分组实现
        if(searchMap==null || StringUtils.isEmpty(searchMap.get("brand"))){
            List<String> brandList = getGroupData(aggregations,"skuBrand");
            groupMap.put("brandList",brandList);
        }
        return groupMap;
    }

    private Map<String, Set<String>> putAllSpec(List<String> specList) {
        return null;
    }

    private List<String> getGroupData(Aggregations aggregations, String skuSpec) {
        return null;
    }
}
