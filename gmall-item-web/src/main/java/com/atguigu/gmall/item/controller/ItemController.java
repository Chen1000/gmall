package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
public class ItemController {

    @Reference
    ManageService manageService;

    @Reference
    ListService listService;


    @RequestMapping("{skuId}.html")
    public String getSkuInfo(@PathVariable("skuId") String skuId, Model model){
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        model.addAttribute("skuInfo",skuInfo);

        List<SpuSaleAttr> saleAttrList = manageService.getSpuSaleAttrListCheckBySku(skuInfo);
        model.addAttribute("saleAttrList", saleAttrList);

        List<SkuSaleAttrValue> saleAttrValueList = manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());

        String valueIdsKey = "";
        Map valueIdSkuMap = new HashMap();

        for (int i=0; i<saleAttrValueList.size(); i++) {
            SkuSaleAttrValue skuSaleAttrValue = saleAttrValueList.get(i);
            if(valueIdsKey.length() != 0){
                valueIdsKey += "|";
            }
            valueIdsKey += skuSaleAttrValue.getSaleAttrValueId();

            /**
             * 两种情况重新计算key值:
             *      1.循环结束
             *      2.下面的属性值的skuId与当前值的不等
             */
            if(i+1 == saleAttrValueList.size() || !skuSaleAttrValue.getSkuId()
                    .equals(saleAttrValueList.get(i+1).getSkuId())){
                valueIdSkuMap.put(valueIdsKey, skuSaleAttrValue.getSkuId());
                valueIdsKey = "";
            }



        }

        String valueIdSkuJson = JSON.toJSONString(valueIdSkuMap);
        model.addAttribute("valueIdSkuJson", valueIdSkuJson);

        listService.incrHotScore(skuId);

        return "item";
    }



    

}
