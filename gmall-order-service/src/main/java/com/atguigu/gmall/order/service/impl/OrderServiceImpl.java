package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.enums.ProcessStatus;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import com.atguigu.gmall.util.ActiveMQUtil;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import javax.jms.Queue;

@Service
@org.springframework.stereotype.Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    OrderInfoMapper orderInfoMapper;

    @Autowired
    OrderDetailMapper orderDetailMapper;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Reference
    PaymentService paymentService;


    @Override
    public void saveOrder(OrderInfo orderInfo) {
        orderInfo.setCreateTime(new Date());

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);

        orderInfo.setExpireTime(calendar.getTime());

        String outTradeNo = "ATGUIGU" + System.currentTimeMillis() + "" + new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);


        orderInfoMapper.insertSelective(orderInfo);

        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }
    }


    @Override
    public String genTradeCode(String userId) {
        Jedis jedis = redisUtil.getJedis();

        String tradeNoKey = "user:" + userId + ":tradeCode";
        String tradeCode = UUID.randomUUID().toString();

        jedis.setex(tradeNoKey, 10 * 60, tradeCode);
        jedis.close();

        return tradeCode;
    }


    @Override
    public boolean checkTradeCode(String userId, String tradeCodePage) {
        Jedis jedis = redisUtil.getJedis();

        String tradeNoKey = "user:" + userId + ":tradeCode";
        String tradeCode = jedis.get(tradeNoKey);
        jedis.close();

        if (tradeCode != null && tradeCode.equals(tradeCodePage)) {
            return true;
        } else {
            return false;
        }
    }


    @Override
    public void delTradeCode(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey = "user:" + userId + ":tradeCode";
        jedis.del(tradeNoKey);
        jedis.close();
    }


    @Override
    public OrderInfo getOrderInfo(String orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);

        OrderDetail orderDetailQuery = new OrderDetail();
        orderDetailQuery.setOrderId(orderId);

        List<OrderDetail> orderDetailList = orderDetailMapper.select(orderDetailQuery);

        orderInfo.setOrderDetailList(orderDetailList);

        return orderInfo;
    }


    @Override
    public void updateOrderStatus(String orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();

        orderInfo.setProcessStatus(processStatus);

        orderInfo.setOrderStatus(processStatus.getOrderStatus());

        orderInfo.setId(orderId);
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);

    }


    @Override
    public void sendOrderStatus(String orderId) {
        String orderJson = initWareOrder(orderId);
        Connection connection = activeMQUtil.getConnection();

        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue orderStatusQueue = session.createQueue("ORDER_RESULT_QUEUE");
            MessageProducer producer = session.createProducer(orderStatusQueue);

            TextMessage textMessage = new ActiveMQTextMessage();
            textMessage.setText(orderJson);

            producer.send(textMessage);

            session.commit();

            session.close();
            producer.close();
            connection.close();


        } catch (JMSException e) {
            e.printStackTrace();
        }


    }


    @Override
    public String initWareOrder(String orderId) {
        OrderInfo orderInfo = getOrderInfo(orderId);
        Map map = initWareOrder(orderInfo);
        return JSON.toJSONString(map);
    }


    @Override
    public Map  initWareOrder(OrderInfo orderInfo){

        Map map=new HashMap();
        map.put("orderId",orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        map.put("orderBody",orderInfo.getTradeBody());
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        map.put("paymentWay","2");
        map.put("wareId",orderInfo.getWareId());

        List detailList=new ArrayList();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            Map  detailMap =new HashMap();
            detailMap.put("skuId",orderDetail.getSkuId());
            detailMap.put("skuName",orderDetail.getSkuName());
            detailMap.put("skuNum",orderDetail.getSkuNum());

            detailList.add(detailMap);
        }

        map.put("details",detailList);

        return  map;

    }


    @Override
    public List<OrderInfo> getExpiredOrderList() {
        Example example = new Example(OrderInfo.class);
        example.createCriteria().andLessThan("expireTime", new Date()).andEqualTo("processStatus", ProcessStatus.UNPAID);


        List<OrderInfo> orderInfos = orderInfoMapper.selectByExample(example);
        return orderInfos;
    }

    @Override
    @Async
    public void execExpiredOrder(OrderInfo orderInfo) {
        updateOrderStatus(orderInfo.getId(), ProcessStatus.CLOSED);
        paymentService.closePayment(orderInfo.getId());
    }


    @Override
    public List<OrderInfo> splitOrder(String orderId, String wareSkuMap) {
        List<OrderInfo> subOrderInfoList = new ArrayList<>();

        //1、先查询出原始订单信息
        OrderInfo orderInfoOrigin = getOrderInfo(orderId);

        //2 wareSkuMap 反序列化
        List<Map> maps = JSON.parseArray(wareSkuMap, Map.class);

        //3 遍历拆单方案  每个仓库与商品的对照 形成一个子订单
        for (Map map : maps) {
            String wareId = (String) map.get("wareId");
            List<String> skuIds = (List) map.get("skuIds");
            //4  生成子订单主表  从原始订单复制    新的订单号  父订单
            OrderInfo subOrderInfo = new OrderInfo();

            try {
                BeanUtils.copyProperties(subOrderInfo, orderInfoOrigin);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

            subOrderInfo.setId(null);
            subOrderInfo.setParentOrderId(orderInfoOrigin.getId());
            subOrderInfo.setWareId(wareId);

            //5 原始订单 订单主表中的订单状态标志为拆单


            //6 明细表  根据拆单方案中的skuids进行匹配 到不同的子订单
            List<OrderDetail> subOrderDetailList = new ArrayList<>();
            List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
            for (String skuId : skuIds) {
                for (OrderDetail orderDetail : orderDetailList) {
                    if (skuId.equals(orderDetail.getSkuId())) {
                        orderDetail.setId(null);
                        subOrderDetailList.add(orderDetail);
                    }
                }
            }

            subOrderInfo.setOrderDetailList(subOrderDetailList);

            subOrderInfo.sumTotalAmount();
            //7 保存到数据库中。
            saveOrder(subOrderInfo);

            subOrderInfoList.add(subOrderInfo);


        }

        updateOrderStatus(orderId, ProcessStatus.SPLIT);
        //       8 返回一个 新生成的子订单列表
        return subOrderInfoList;

    }


}
