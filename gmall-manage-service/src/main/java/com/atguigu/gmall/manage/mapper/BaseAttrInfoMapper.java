package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.BaseAttrInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseAttrInfoMapper extends Mapper<BaseAttrInfo> {

    List<BaseAttrInfo> getBaseAttrInfoListBycatalog3Id(Long catalog3Id);

    List<BaseAttrInfo> getBaseAttrInfoListByValueIds(@Param("valueIds") String valueIds);

}
