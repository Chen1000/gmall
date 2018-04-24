package com.atguigu.gmall.usermanage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.usermanage.mapper.UserAddressMapper;
import com.atguigu.gmall.usermanage.mapper.UserInfoMapper;
import com.atguigu.gmall.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserInfoMapper userInfoMapper;

    @Autowired
    UserAddressMapper userAddressMapper;

    public List<UserInfo> getUserInfoListAll(){

        List<UserInfo> userInfos = userInfoMapper.selectAll();

        UserInfo userInfoQuery = new UserInfo();
        userInfoQuery.setLoginName("change");
        List<UserInfo> userInfos1 = userInfoMapper.select(userInfoQuery);



        return userInfos;
    }




    public List<UserAddress> getUserAddressList(String userId){

        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);

        List<UserAddress> userAddressList = userAddressMapper.select(userAddress);

        return userAddressList;
    }

}
