package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.cart.constant.CartConst;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.*;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    CartInfoMapper cartInfoMapper;

    @Reference
    ManageService manageService;

    @Autowired
    RedisUtil redisUtil;


    //已经登录成功的前提下，往数据库和缓存中保存
    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        //1 先查cart中是否已经有该商品
        CartInfo cartInfoQuery = new CartInfo();
        cartInfoQuery.setSkuId(skuId);
        cartInfoQuery.setUserId(userId);
        CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfoQuery);

        if (cartInfoExist != null) {
            //购物车中有该商品,更新数量
            cartInfoExist.setSkuName(cartInfoQuery.getSkuName() + skuNum);
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);
        } else {
            //购物车中不存在该商品，保存进购物车
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo = new CartInfo();
            cartInfo.setSkuId(skuId);
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setCartPrice(skuInfo.getPrice());

            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(skuNum);

            cartInfoMapper.insertSelective(cartInfo);
            cartInfoExist = cartInfo;
        }

        //更新缓存 redis
        //1 数据结构才用 hash  2  user:userid:info     user:userid:cart
        String userCartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;

        Jedis jedis = redisUtil.getJedis();
        String cartJson = JSON.toJSONString(cartInfoExist);
        jedis.hset(userCartKey, skuId, cartJson);

        //更新购物车过期时间
        String userInfoKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USERINFO_KEY_SUFFIX;
        Long ttl = jedis.ttl(userInfoKey);
        jedis.expire(userCartKey, ttl.intValue());

        jedis.close();

    }


    @Override
    public List<CartInfo> getCartList(String userId) {
        //1.从redis中取购物车商品
        Jedis jedis = redisUtil.getJedis();
        String userCartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        List<String> cartJsons = jedis.hvals(userCartKey);

        if (cartJsons != null && cartJsons.size() > 0) {
            //如果redis中有
            //redis hash --> java list
            List<CartInfo> cartList = new ArrayList<>(cartJsons.size());
            for (String cartJson : cartJsons) {
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                cartList.add(cartInfo);
            }

            //用 id 排序  倒排
            cartList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return Long.compare(Long.parseLong(o2.getId()), Long.parseLong(o1.getId()));
                }
            });

            return cartList;
        } else {
            //redis中没有，从数据库中查询购物车商品,并放入redis中
            List<CartInfo> cartInfoList = loadCartCache(userId);

            return cartInfoList;
        }
    }


    @Override
    public List<CartInfo> loadCartCache(String userId) {
        //Redis中没有 从 cart_info查   其中cart_price 可能会旧  所以 还要关联 sku_info
        List<CartInfo> cartInfoList = cartInfoMapper.getCartListWithCurPrice(Long.parseLong(userId));

        // Java list -> redis hash
        Jedis jedis = redisUtil.getJedis();

        // Java list -> java map
        Map<String, String> cartMap = new HashMap<>(cartInfoList.size());
        for (CartInfo cartInfo : cartInfoList) {
            String cartJson = JSON.toJSONString(cartInfo);
            cartMap.put(cartInfo.getSkuId(), cartJson);
        }

        String userCartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;

        jedis.hmset(userCartKey, cartMap);
        jedis.close();

        return cartInfoList;
    }


    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartListFromCookie, String userId){
        List<CartInfo> cartListDB = cartInfoMapper.getCartListWithCurPrice(Long.parseLong(userId));

        for (CartInfo cartInfoCK : cartListFromCookie) {
            boolean isMatch = false;

            for (CartInfo cartInfoDB : cartListDB) {
                if(cartInfoCK.getSkuId().equals(cartInfoDB.getSkuId())){
                    cartInfoDB.setSkuNum(cartInfoDB.getSkuNum() + cartInfoCK.getSkuNum());
                    cartInfoMapper.updateByPrimaryKeySelective(cartInfoDB);
                    isMatch = true;
                }
            }

            if(!isMatch){
                cartInfoCK.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfoCK);
            }
        }

        //把cookie中商品的勾选状态 赋给redis
        List<CartInfo> cartInfoList = loadCartCache(userId);
        for (CartInfo cartInfo : cartInfoList) {
            for (CartInfo cartCookie : cartListFromCookie) {
                if(cartInfo.getSkuId().equals(cartCookie.getSkuId())){
                    if(cartCookie.getIsChecked().equals("1")) {  //只有勾中才赋勾选状态
                        cartInfo.setIsChecked(cartCookie.getIsChecked());
                        checkCart(cartInfo.getSkuId(), cartCookie.getIsChecked(), userId);
                    }
                }
            }
        }


        return cartInfoList;
    }


    @Override
    public void checkCart(String skuId, String isChecked, String userId){
        //1 更新购物车中的isCheck标志  redis
        Jedis jedis = redisUtil.getJedis();
        String userCartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        String cartJson = jedis.hget(userCartKey, skuId);

        CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
        cartInfo.setIsChecked(isChecked);
        String cartCheckedJson = JSON.toJSONString(cartInfo);
        jedis.hset(userCartKey, skuId, cartCheckedJson);

        //2 新增到已选中购物车
        String userCheckedKey= CartConst.USER_KEY_PREFIX +userId+CartConst.USER_CHECKED_KEY_SUFFIX;
        if(isChecked.equals("1")){
            jedis.hset(userCheckedKey, skuId, cartCheckedJson);
        }else{
            jedis.hdel(userCheckedKey, skuId);
        }
        jedis.close();
    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        //从redis中获取用户购物车中已选中商品
        String userCheckedKey= CartConst.USER_KEY_PREFIX +userId+CartConst.USER_CHECKED_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedis();

        List<String> cartCheckedJsonList = jedis.hvals(userCheckedKey);
        List<CartInfo> cartCheckedList = new ArrayList<>(cartCheckedJsonList.size());
        for (String cartJson : cartCheckedJsonList) {
            CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
            cartCheckedList.add(cartInfo);
        }

        return cartCheckedList;
    }

}
