package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.enums.ProcessStatus;

import java.util.List;
import java.util.Map;

public interface OrderService {

    public void saveOrder(OrderInfo orderInfo);

    public String genTradeCode(String userId);

    public boolean checkTradeCode(String userId, String tradeCodePage);

    public void delTradeCode(String userId);

    public OrderInfo getOrderInfo(String orderId);

    public void updateOrderStatus(String orderId, ProcessStatus processStatus);

    public void sendOrderStatus(String orderId);

    public String initWareOrder(String orderId);

    public Map initWareOrder(OrderInfo orderInfo);

    public List<OrderInfo> getExpiredOrderList();

    public void execExpiredOrder(OrderInfo orderInfo);

    public List<OrderInfo> splitOrder(String orderId,String wareSkuMap);

}
