package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.*;

import java.util.List;

public interface ManageService {

    public List<BaseCatalog1> getCatalog1();

    public List<BaseCatalog2> getCatalog2(String catalog1Id);

    public List<BaseCatalog3> getCatalog3(String catalog2Id);

    public List<BaseAttrInfo> getAttrList(String catalog3Id);

    public List<BaseAttrInfo> getAttrList(List valueIdList);

    public BaseAttrInfo getAttrInfo(String id);

    public void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    public List<SpuInfo> getSpuInfoList(SpuInfo spuInfo);

    public List<BaseSaleAttr> getBaseSaleAttrList();

    public void saveSpuInfo(SpuInfo spuInfo);

    public  List<SpuSaleAttr> getSpuSaleAttrList(String spuId);

    public List<SpuImage> getSpuImageList(String spuId);

    public void saveSkuInfo(SkuInfo skuInfo);

    public SkuInfo getSkuInfo(String skuId);

    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo);

    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId);

}
