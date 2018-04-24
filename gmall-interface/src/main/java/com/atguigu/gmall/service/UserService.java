package com.atguigu.gmall.service;


import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;

import java.util.List;

public interface UserService {


    public List<UserInfo> getUserInfoListAll();




    public List<UserAddress> getUserAddressList(String userId);
}
