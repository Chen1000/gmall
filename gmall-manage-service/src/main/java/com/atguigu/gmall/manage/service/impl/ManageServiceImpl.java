package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.constant.ManageConst;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    BaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired
    BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    BaseCatalog3Mapper baseCatalog3Mapper;

    @Autowired
    SpuInfoMapper spuInfoMapper;

    @Autowired
    SpuImageMapper spuImageMapper;

    @Autowired
    SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    SkuInfoMapper skuInfoMapper;

    @Autowired
    SkuImageMapper skuImageMapper;

    @Autowired
    SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    RedisUtil redisUtil;



    @Override
    public List<BaseCatalog1> getCatalog1() {

        List<BaseCatalog1> baseCatalog1List = baseCatalog1Mapper.selectAll();

        return baseCatalog1List;
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {

        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(catalog1Id);

        List<BaseCatalog2> baseCatalog2List = baseCatalog2Mapper.select(baseCatalog2);

        return baseCatalog2List;
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {

        BaseCatalog3 baseCatalog3 = new BaseCatalog3();
        baseCatalog3.setCatalog2Id(catalog2Id);

        List<BaseCatalog3> baseCatalog3List = baseCatalog3Mapper.select(baseCatalog3);

        return baseCatalog3List;
    }

    @Override
    public List<BaseAttrInfo> getAttrList(String catalog3Id) {

        List<BaseAttrInfo> baseAttrInfoList = baseAttrInfoMapper.getBaseAttrInfoListBycatalog3Id(Long.parseLong(catalog3Id));

        return baseAttrInfoList;
    }

    @Override
    public List<BaseAttrInfo> getAttrList(List valueIdList) {

        String valueIds = StringUtils.join(valueIdList, ",");
        List<BaseAttrInfo> baseAttrInfoListByValueIds = baseAttrInfoMapper.getBaseAttrInfoListByValueIds(valueIds);

        return baseAttrInfoListByValueIds;
    }

    @Override
    public BaseAttrInfo getAttrInfo(String id) {
        //查询属性基本信息
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(id);

        //查询属性对应的属性值
        BaseAttrValue baseAttrValue4Query =new BaseAttrValue();
        baseAttrValue4Query.setAttrId(baseAttrInfo.getId());
        List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.select(baseAttrValue4Query);

        baseAttrInfo.setAttrValueList(baseAttrValueList);
        return baseAttrInfo;
    }

    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {

        //如果有主键就进行更新，如果没有就插入
        if(baseAttrInfo.getId()!=null&&baseAttrInfo.getId().length()>0){
            baseAttrInfoMapper.updateByPrimaryKey(baseAttrInfo);
        }else{
            //防止主键被赋上一个空字符串
            if(baseAttrInfo.getId().length()==0){
                baseAttrInfo.setId(null);
            }
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }
        //把原属性值全部清空
        BaseAttrValue baseAttrValue4Del = new BaseAttrValue();
        baseAttrValue4Del.setAttrId(baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValue4Del);

        //重新插入属性
        if(baseAttrInfo.getAttrValueList()!=null&&baseAttrInfo.getAttrValueList().size()>0) {
            for (BaseAttrValue attrValue : baseAttrInfo.getAttrValueList()) {
                //防止主键被赋上一个空字符串
                if(attrValue.getId()!=null&&attrValue.getId().length()==0){
                    attrValue.setId(null);
                }
                attrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insertSelective(attrValue);
            }
        }
    }

    @Override
    public List<SpuInfo> getSpuInfoList(SpuInfo spuInfo){
        List<SpuInfo> spuInfoList = spuInfoMapper.select(spuInfo);
        return  spuInfoList;
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return  baseSaleAttrMapper.selectAll();
    }

    @Override
    public void saveSpuInfo(SpuInfo spuInfo){
        if(spuInfo.getId()==null||spuInfo.getId().length()==0){
            spuInfo.setId(null);
            spuInfoMapper.insertSelective(spuInfo);
        }else{
            spuInfoMapper.updateByPrimaryKey(spuInfo);
        }

        Example spuImageExample=new Example(SpuImage.class);
        spuImageExample.createCriteria().andEqualTo("spuId",spuInfo.getId());
        spuImageMapper.deleteByExample(spuImageExample);

        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if(spuImageList!=null) {
            for (SpuImage spuImage : spuImageList) {
                if(spuImage.getId()!=null&&spuImage.getId().length()==0){
                    spuImage.setId(null);
                }
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insertSelective(spuImage);
            }
        }

        Example spuSaleAttrExample=new Example(SpuSaleAttr.class);
        spuSaleAttrExample.createCriteria().andEqualTo("spuId",spuInfo.getId());
        spuSaleAttrMapper.deleteByExample(spuSaleAttrExample);


        Example spuSaleAttrValueExample=new Example(SpuSaleAttrValue.class);
        spuSaleAttrValueExample.createCriteria().andEqualTo("spuId",spuInfo.getId());
        spuSaleAttrValueMapper.deleteByExample(spuSaleAttrValueExample);

        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if(spuSaleAttrList!=null) {
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                if(spuSaleAttr.getId()!=null&&spuSaleAttr.getId().length()==0){
                    spuSaleAttr.setId(null);
                }
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insertSelective(spuSaleAttr);
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                    if(spuSaleAttrValue.getId()!=null&&spuSaleAttrValue.getId().length()==0){
                        spuSaleAttrValue.setId(null);
                    }
                    spuSaleAttrValue.setSpuId(spuInfo.getId());
                    spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                }
            }

        }
    }


    @Override
    public  List<SpuSaleAttr> getSpuSaleAttrList(String spuId){

        List<SpuSaleAttr> spuSaleAttrList = spuSaleAttrMapper.selectSpuSaleAttrList(Long.parseLong(spuId));
        return spuSaleAttrList;

    }


    @Override
    public List<SpuImage> getSpuImageList(String spuId){

        SpuImage spuImageQuery = new SpuImage();
        spuImageQuery.setSpuId(spuId);
        List<SpuImage> spuImageList = spuImageMapper.select(spuImageQuery);

        return spuImageList;
    }


    @Override
    public void saveSkuInfo(SkuInfo skuInfo){
        if(skuInfo.getId()!=null&&skuInfo.getId().length()==0){
            skuInfo.setId(null);
        }
        skuInfoMapper.insertSelective(skuInfo);

        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        for (SkuImage skuImage : skuImageList) {
            skuImage.setSkuId(skuInfo.getId());

            skuImageMapper.insertSelective(skuImage);
        }

        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        for (SkuAttrValue skuAttrValue : skuAttrValueList) {
            skuAttrValue.setSkuId(skuInfo.getId());
            skuAttrValueMapper.insertSelective(skuAttrValue);
        }

        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {

            skuSaleAttrValue.setSkuId(skuInfo.getId());
            skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
        }

    }


    @Override
    public SkuInfo getSkuInfo(String skuId){

        SkuInfo skuInfo = null;

        try {
            Jedis jedis = redisUtil.getJedis();
            String skuInfoKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;
            String skuInfoJson = jedis.get(skuInfoKey);

            if (skuInfoJson == null || skuInfoJson.length() == 0) {
                System.err.println(Thread.currentThread().getName() + "缓存未命中！");

                String skuLockKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKULOCK_SUFFIX;
                String lock = jedis.set(skuLockKey, "OK", "NX", "PX", ManageConst.SKULOCK_EXPIRE_PX);

                if ("OK".equals(lock)) {
                    System.err.println(Thread.currentThread().getName() + "获得分布式锁！");

                    skuInfo = getSkuInfoFromDB(skuId);
                    if (skuInfo == null) {
                        jedis.setex(skuInfoKey, ManageConst.SKUKEY_TIMEOUT, "empty");
                        return null;
                    }

                    String skuInfoJsonNew = JSON.toJSONString(skuInfo);
                    jedis.setex(skuInfoKey, ManageConst.SKUKEY_TIMEOUT, skuInfoJsonNew);
                    jedis.close();

                    return skuInfo;
                } else {
                    System.err.println(Thread.currentThread().getName() + "未获得分布式锁，开始自旋！");

                    Thread.sleep(1000);
                    jedis.close();

                    return getSkuInfo(skuId);
                }

            }else if(skuInfoJson.equals("empty")){
                return null;
            } else {
                System.err.println(Thread.currentThread().getName()+"缓存已命中！！！！！！！！！！！！！！！！！！！");

                skuInfo = JSON.parseObject(skuInfoJson, SkuInfo.class);
                jedis.close();
                return skuInfo;
            }


        }catch (JedisConnectionException e){
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        return getSkuInfoFromDB(skuId);
    }


    public SkuInfo getSkuInfoFromDB(String skuId){
        System.err.println(Thread.currentThread().getName()+"查询数据库！");

        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);

        if(skuInfo!=null) {
            SkuImage skuImageQuery = new SkuImage();
            skuImageQuery.setSkuId(skuId);
            List<SkuImage> skuImageList = skuImageMapper.select(skuImageQuery);

            skuInfo.setSkuImageList(skuImageList);
        }

        if(skuInfo!=null){
            SkuAttrValue skuAttrValueQuery=new SkuAttrValue();
            skuAttrValueQuery.setSkuId(skuId);
            List<SkuAttrValue> skuAttrValueList = skuAttrValueMapper.select(skuAttrValueQuery);

            skuInfo.setSkuAttrValueList(skuAttrValueList);
        }

        if(skuInfo!=null){
            SkuSaleAttrValue skuSaleAttrValueQuery=new SkuSaleAttrValue();
            skuSaleAttrValueQuery.setSkuId(skuId);
            List<SkuSaleAttrValue> skuSaleAttrValueList = skuSaleAttrValueMapper.select(skuSaleAttrValueQuery);

            skuInfo.setSkuSaleAttrValueList(skuSaleAttrValueList);
        }

        return skuInfo;
    }


    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {

        List<SpuSaleAttr> saleAttrList = spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku
                (Long.parseLong(skuInfo.getId()), Long.parseLong(skuInfo.getSpuId()));

        return saleAttrList;
    }

    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId) {

        List<SkuSaleAttrValue> skuSaleAttrValueList = skuSaleAttrValueMapper
                .selectSkuSaleAttrValueListBySpu(Long.parseLong(spuId));

        return skuSaleAttrValueList;

    }


}
