package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
public class CartController {


    @Reference
    CartService cartService;

    @Autowired
    CartCookieHandler cartCookieHandler;

    @Reference
    ManageService manageService;


    @RequestMapping(value = "addToCart", method = RequestMethod.POST)
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response) {
        //1.获得参数 skuId userId skuNum
        String skuId = request.getParameter("skuId");
        String skuNum = request.getParameter("skuNum");

        String userId = (String) request.getAttribute("userId");
        if (userId != null) {
            //用户已登录，保存商品到数据库
            cartService.addToCart(skuId, userId, Integer.parseInt(skuNum));
        } else {
            //用户未登录，保存商品到cookie
            cartCookieHandler.addToCart(request, response, skuId, userId, Integer.parseInt(skuNum));
        }

        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        request.setAttribute("skuInfo", skuInfo);
        request.setAttribute("skuNum", skuNum);

        return "success";
    }


    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request, HttpServletResponse response) {
        //1.检查用户登录状态
        String userId = (String) request.getAttribute("userId");

        if (userId != null) {
            //有userId 从后台（redis 或者 mysql）获取
            //登录后先查询cookie,进行合并
            List<CartInfo> cartListFromCookie = cartCookieHandler.getCartList(request);
            List<CartInfo> cartList = null;

            if(cartListFromCookie != null && cartListFromCookie.size() > 0){
                //cookie中有
                //1.合并到后台数据库
                cartList = cartService.mergeToCartList(cartListFromCookie, userId);
                //2.删除cookie中的数据
                cartCookieHandler.deleteCartList(request, response);
            }else{
                //cookie中没有，直接查询数据库
                cartList = cartService.getCartList(userId);
            }

            request.setAttribute("cartList", cartList);

        } else {
            //没有userId从cookie中读取
            List<CartInfo> cartList = cartCookieHandler.getCartList(request);
            request.setAttribute("cartList", cartList);
        }

        return "cartList";
    }


    @RequestMapping(value = "checkCart",method = RequestMethod.POST)
    @ResponseBody
    @LoginRequire(autoRedirect = false)
    public void checkCart(HttpServletRequest request, HttpServletResponse response){
        String skuId = request.getParameter("skuId");
        String isChecked = request.getParameter("isChecked");
        String userId = (String)request.getAttribute("userId");

        if(userId != null){
            cartService.checkCart(skuId, isChecked, userId);
        }else{
            cartCookieHandler.checkCart(request,response,skuId,isChecked);
        }

        return;
    }



    @RequestMapping("toTrade")
    @LoginRequire
    public String toTrade(HttpServletRequest request, HttpServletResponse response){
        String userId = (String)request.getAttribute("userId");
        List<CartInfo> cartListFromCookie = cartCookieHandler.getCartList(request);

        if(cartListFromCookie != null && cartListFromCookie.size() > 0){
            //1.合并到后台数据库和redis
            List<CartInfo> cartList = cartService.mergeToCartList(cartListFromCookie, userId);

            //2.删除chookie中的数据
            cartCookieHandler.deleteCartList(request, response);
        }

        return "redirect://order.gmall.com/trade";
    }

}
