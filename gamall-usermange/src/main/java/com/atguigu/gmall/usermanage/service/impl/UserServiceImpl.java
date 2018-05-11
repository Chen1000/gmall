package com.atguigu.gmall.usermanage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.usermanage.mapper.UserAddressMapper;
import com.atguigu.gmall.usermanage.mapper.UserInfoMapper;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserInfoMapper userInfoMapper;

    @Autowired
    RedisUtil redisUtil;


    @Autowired
    UserAddressMapper userAddressMapper;


    private static final String USERKEY_PREFIX="user:";
    private static final String USERINFOKEY_SUFFIX=":info";
    private static final int USERINFO_EXPIRE=60*60;





    @Override
    public List<UserInfo> getUserInfoListAll(){

        List<UserInfo> userInfos = userInfoMapper.selectAll();

        UserInfo userInfoQuery = new UserInfo();
        userInfoQuery.setLoginName("change");
        List<UserInfo> userInfos1 = userInfoMapper.select(userInfoQuery);



        return userInfos;
    }


    @Override
    public void addUser(UserInfo userInfo){

        String md5Hex = DigestUtils.md5Hex(userInfo.getPasswd());
        userInfo.setPasswd(md5Hex);
        userInfoMapper.insertSelective(userInfo);

    }

    public void updateUser(String id,UserInfo userInfo){
        Example example=new Example(UserInfo.class);
        example.createCriteria().andLike("name" ,"张%").andEqualTo("id","3");
        userInfoMapper.updateByExampleSelective(userInfo,example);

    }


    @Override
    public List<UserAddress> getUserAddressList(String userId){

        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);

        List<UserAddress> userAddressList = userAddressMapper.select(userAddress);

        return userAddressList;
    }


    //登录
    /*1、查数据库
    2、放缓存
    3、返回*/
    @Override
    public UserInfo login(UserInfo userInfo){

        String md5Hex = DigestUtils.md5Hex(userInfo.getPasswd());
        userInfo.setPasswd(md5Hex);

        UserInfo userInfoResult = userInfoMapper.selectOne(userInfo);

        if(userInfoResult != null){
            Jedis jedis = redisUtil.getJedis();
            //user:1:info
            String userJson = JSON.toJSONString(userInfoResult);
            jedis.setex(USERKEY_PREFIX + userInfoResult.getId() + USERINFOKEY_SUFFIX, USERINFO_EXPIRE, userJson);
            jedis.close();
            return userInfoResult;
        }
        return null;
    }

    @Override
    public UserInfo verify(String userId) {
        Jedis jedis = redisUtil.getJedis();

        String userKey = USERKEY_PREFIX + userId + USERINFOKEY_SUFFIX;
        String userJson = jedis.get(userKey);
        jedis.expire(userKey, USERINFO_EXPIRE);
        jedis.close();

        if(userJson != null){
            UserInfo userInfo = JSON.parseObject(userJson, UserInfo.class);

            return userInfo;
        }

        return null;
    }


}
