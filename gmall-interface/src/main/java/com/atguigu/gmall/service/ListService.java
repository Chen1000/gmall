package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParam;
import com.atguigu.gmall.bean.SkuLsResult;

public interface ListService {

    public void saveSkuLsInfo(SkuLsInfo skuLsInfo);

    public SkuLsResult searchSkuInfoList(SkuLsParam skuLsParam);

    public void incrHotScore(String skuId);

}
