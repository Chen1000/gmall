package com.atguigu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {

    @Reference
    ManageService manageService;

    @Reference
    ListService listService;




    @RequestMapping("list.html")
    public String list(SkuLsParam skuLsParam, HttpServletRequest request){

        skuLsParam.setPageSize(1);

        //查询的结果
        SkuLsResult skuLsResult = listService.searchSkuInfoList(skuLsParam);

        //从结果中取涉及到的平台属性值列表
        List<String> valueIdList = skuLsResult.getValueIdList();
        List<BaseAttrInfo> attrList = manageService.getAttrList(valueIdList);

        //已选择的属性值的列表
        List<BaseAttrValue> selectedValueList = new ArrayList<>(valueIdList.size());

        String urlParam = makeUrlParam(skuLsParam);

        for (Iterator<BaseAttrInfo> iterator = attrList.iterator(); iterator.hasNext(); ) {
            BaseAttrInfo baseAttrInfo =  iterator.next();
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            
            for (BaseAttrValue baseAttrValue : attrValueList) {

                baseAttrValue.setUrlParam(urlParam);

                if(skuLsParam.getValueId()!=null&&skuLsParam.getValueId().length>0){
                    for (String valueId : skuLsParam.getValueId()) {
                        //选中的属性值和查询结果的属性值
                        if(valueId.equals(baseAttrValue.getId())){

                            iterator.remove();

                            //构造面包屑列表
                            BaseAttrValue attrValueSelected = new BaseAttrValue();
                            attrValueSelected.setValueName(baseAttrInfo.getAttrName() + ":" + baseAttrValue.getValueName());
                            attrValueSelected.setId(valueId);
                            attrValueSelected.setUrlParam(makeUrlParam(skuLsParam, valueId));
                            selectedValueList.add(attrValueSelected);
                        }
                    }
                }
            }
        }

        long totalPages = (skuLsResult.getTotal() + skuLsParam.getPageSize() - 1) / skuLsParam.getPageSize();

        request.setAttribute("totalPages",totalPages);

        request.setAttribute("pageNo",skuLsParam.getPageNo());

        request.setAttribute("urlParam",urlParam);

        request.setAttribute("selectedValueList", selectedValueList);

        request.setAttribute("keyword", skuLsParam.getKeyword());

        request.setAttribute("attrList", attrList);

        List<SkuLsInfo> skuLsInfoList = skuLsResult.getSkuLsInfoList();
        request.setAttribute("skuLsInfoList", skuLsInfoList);

        return "list";
    }


    private String makeUrlParam(SkuLsParam skuLsParam, String... excludeValueIds){
        String urlParam = "";

        if(skuLsParam.getKeyword() != null){
            urlParam += "keyword=" + skuLsParam.getKeyword();
        }

        if(skuLsParam.getCatalog3Id() != null){

            if(urlParam.length() > 0){
                urlParam += "&";
            }

            urlParam += "catalog3Id=" + skuLsParam.getCatalog3Id();
        }

        //构造属性值参数
        if(skuLsParam.getValueId() != null && skuLsParam.getValueId().length > 0){
            for (int i = 0; i < skuLsParam.getValueId().length; i++) {
                String valueId = skuLsParam.getValueId()[i];

                //排除选中的属性值
                if(excludeValueIds != null && excludeValueIds.length > 0){
                    String excludeValueId = excludeValueIds[0];
                    if(excludeValueId.equals(valueId)){
                        continue;
                    }
                }

                if(urlParam.length() > 0){
                    urlParam += "&";
                }

                urlParam += "valueId=" + valueId;
            }
        }

        return urlParam;
    }

}
