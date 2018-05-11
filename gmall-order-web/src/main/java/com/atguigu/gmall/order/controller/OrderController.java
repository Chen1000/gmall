package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.enums.OrderStatus;
import com.atguigu.gmall.enums.ProcessStatus;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.HttpClientUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class OrderController {

    @Reference
    UserService userService;

    @Reference
    CartService cartService;

    @Reference
    OrderService orderService;

    @Reference
    ManageService manageService;

    @RequestMapping("trade")
    @LoginRequire
    public String trade(HttpServletRequest request){
        String userId = (String)request.getAttribute("userId");
        List<UserAddress> userAddressList = userService.getUserAddressList(userId);
        request.setAttribute("userAddressList", userAddressList);

        List<CartInfo> cartCheckedList = cartService.getCartCheckedList(userId);

        List<OrderDetail> orderDetailList = new ArrayList<>(cartCheckedList.size());

        for (CartInfo cartInfo : cartCheckedList) {
            OrderDetail orderDetail = new OrderDetail();

            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setOrderPrice(cartInfo.getSkuPrice());

            orderDetailList.add(orderDetail);
        }
        request.setAttribute("orderDetailList", orderDetailList);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        orderInfo.sumTotalAmount();
        request.setAttribute("totalAmount", orderInfo.getTotalAmount());

        String tradeCode = orderService.genTradeCode(userId);
        request.setAttribute("tradeCode", tradeCode);

        return "trade";
    }



    @RequestMapping("submitOrder")
    @LoginRequire
    public String submitOrder(OrderInfo orderInfo, HttpServletRequest request){
        //1.检查tradeCode
        String userId = (String)request.getAttribute("userId");

        String tradeCode = request.getParameter("tradeCode");
        boolean existsTradeCode = orderService.checkTradeCode(userId, tradeCode);
        if(!existsTradeCode){
            request.setAttribute("errMsg","该页面已失效，请重新结算!");
            return "tradeFail";
        }

        //2.初始化参数
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        orderInfo.sumTotalAmount();
        orderInfo.setUserId(userId);

        //3.校验 验价
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            SkuInfo skuInfo = manageService.getSkuInfo(orderDetail.getSkuId());
            if(!skuInfo.getPrice().equals(orderDetail.getOrderPrice())){
                request.setAttribute("errMsg","您选择的商品可能存在价格变动，请重新下单!");
                cartService.loadCartCache(userId);

                return "tradeFail";
            }

            boolean hasStock = checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
            if(!hasStock){
                request.setAttribute("errMsg","您的商品【"+orderDetail.getSkuName()+"】库存不足，请重新下单。。。");

                return "tradeFail";
            }

        }

        //4.保存
        orderService.saveOrder(orderInfo);
        orderService.delTradeCode(userId);

        //5.重定向
        return "redirect://payment.gmall.com/index?orderId=" + orderInfo.getId();
    }


    private boolean checkStock(String skuId, Integer skuNum){
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);
        if("1".equals(result)){
            return true;
        }
        return false;
    }


    @RequestMapping(value = "orderSplit",method = RequestMethod.POST)
    @ResponseBody
    public String orderSplit(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");
        final OrderService orderService = this.orderService;
        List<OrderInfo> subOrderInfoList= orderService.splitOrder(orderId,wareSkuMap);
        List<Map> wareMapList=new ArrayList<>();
        for (OrderInfo orderInfo : subOrderInfoList) {
            Map wareMap = orderService.initWareOrder(orderInfo);
            wareMapList.add(wareMap);
        }

        String jsonString = JSON.toJSONString(wareMapList);
        return jsonString;
    }

}
