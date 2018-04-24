package com.atguigu.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParam;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.util.RedisUtil;
import io.searchbox.client.JestClient;
import io.searchbox.core.*;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Service
public class ListServiceImpl implements ListService {

    @Autowired
    JestClient jestClient;

    @Autowired
    RedisUtil redisUtil;

    public static final String ES_INDEX = "gmall";

    public static final String ES_TYPE = "SkuInfo";




    @Test
    public void testEs() throws IOException {
        String query = "{\n" +
                "  \"query\": {\n" +
                "    \"match\": {\n" +
                "      \"name\": \"湄公河\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        Search search = new Search.Builder(query).addIndex("movies_index_chn")
                        .addType("movies_type_chn").build();

        SearchResult searchResult = jestClient.execute(search);

        List<SearchResult.Hit<HashMap, Void>> hits = searchResult.getHits(HashMap.class);

        for (SearchResult.Hit<HashMap, Void> hit : hits) {
            HashMap source = hit.source;
            String name = (String) source.get("name");
            System.out.println("name = " + name);
        }

    }


    @Override
    public void saveSkuLsInfo(SkuLsInfo skuLsInfo){

        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();

        try {
            DocumentResult result = jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ;
    }


    @Test
    public void testSearchQuery(){
        SkuLsParam skuLsParam=new SkuLsParam();
        skuLsParam.setCatalog3Id("61");
        skuLsParam.setValueId(new String[]{"10","13"});

        skuLsParam.setKeyword("小米");

        skuLsParam.setPageNo(2);

        skuLsParam.setPageSize(2);



        makeQueryStringForSearch(  skuLsParam);
    }


    @Override
    public SkuLsResult searchSkuInfoList(SkuLsParam skuLsParam){
        String query = makeQueryStringForSearch(skuLsParam);

        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();

        SearchResult searchResult = null;
        try {
            searchResult = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }

        SkuLsResult skuLsResult = makeResultForSearch(skuLsParam, searchResult);

        return skuLsResult;
    }


    public String makeQueryStringForSearch(SkuLsParam skuLsParam){
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //复合查询
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        //关键词过滤
        if(skuLsParam.getKeyword() != null){
            //关键词
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", skuLsParam.getKeyword());
            boolQueryBuilder.must(matchQueryBuilder);

            //高亮
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.preTags("<span style='color:red'>");
            highlightBuilder.postTags("</span>");
            highlightBuilder.field("skuName");
            searchSourceBuilder.highlight(highlightBuilder);
        }

        //三级分类过滤
        if(skuLsParam.getCatalog3Id() != null){
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", skuLsParam.getCatalog3Id());
            boolQueryBuilder.filter(termQueryBuilder);
        }

        //平台属性过滤
        if(skuLsParam.getValueId() != null && skuLsParam.getValueId().length > 0){

            for (int i = 0; i < skuLsParam.getValueId().length; i++) {
                String valueId = skuLsParam.getValueId()[i];

                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", valueId);
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }


        searchSourceBuilder.query(boolQueryBuilder);


        //分页
        int from = (skuLsParam.getPageNo() - 1) * skuLsParam.getPageSize();

        searchSourceBuilder.from(from);
        searchSourceBuilder.size(skuLsParam.getPageSize());

        //排序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);

        //聚合
        TermsBuilder groupby_valueId = AggregationBuilders.terms("groupby_valueId").field("skuAttrValueList.valueId");
        searchSourceBuilder.aggregation(groupby_valueId);

        System.out.println("searchSourceBuilder.toString() = " + searchSourceBuilder.toString());


        return searchSourceBuilder.toString();
    }


    public SkuLsResult makeResultForSearch(SkuLsParam skuLsParam, SearchResult searchResult){
        SkuLsResult skuLsResult = new SkuLsResult();

        List<SkuLsInfo> skuLsInfoList = new ArrayList<>(skuLsParam.getPageSize());
        //获取sku列表
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
            SkuLsInfo skuLsInfo = hit.source;

            if(skuLsParam.getKeyword() != null){
                List<String> list = hit.highlight.get("skuName");
                //把带有高亮标签的字符串替换skuName
                String skuNameH1 = list.get(0);
                skuLsInfo.setSkuName(skuNameH1);
            }

            skuLsInfoList.add(skuLsInfo);
        }

        skuLsResult.setSkuLsInfoList(skuLsInfoList);
        skuLsResult.setTotal(searchResult.getTotal().intValue());

        //取记录个数并计算出总页数
        long totalPage = (searchResult.getTotal()%skuLsParam.getPageSize() == 0)?
                searchResult.getTotal()/skuLsParam.getPageSize():searchResult.getTotal()%skuLsParam.getPageSize()+1;
        skuLsResult.setTotalPage(totalPage);

        //取出涉及的属性值id
        MetricAggregation aggregations = searchResult.getAggregations();
        TermsAggregation groupby_valueId = aggregations.getTermsAggregation("groupby_valueId");
        List<TermsAggregation.Entry> buckets = groupby_valueId.getBuckets();

        List<String> valueIdList = new ArrayList<>(buckets.size());
        for (TermsAggregation.Entry bucket : buckets) {
            String valueId = bucket.getKey();
            valueIdList.add(valueId);
        }
        skuLsResult.setValueIdList(valueIdList);



        return skuLsResult;
    }


    @Override
    public void incrHotScore(String skuId){

        Jedis jedis = redisUtil.getJedis();

        Double hotScore = jedis.zincrby("hotScore", 1, skuId);

        if(hotScore%10 == 0){

            updateHotScore(skuId, hotScore);

        }
    }



    public void updateHotScore(String skuId, Double hotScore){

        String updateJson = "{\n" +
                "  \"doc\": {\n" +
                "    \"hotScore\":\""+hotScore+"\"\n" +
                "  }\n" +
                "}";

        Update update = new Update.Builder(updateJson).index(ES_INDEX).type(ES_TYPE).id(skuId).build();

        try {
            DocumentResult result = jestClient.execute(update);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ;
    }


}
