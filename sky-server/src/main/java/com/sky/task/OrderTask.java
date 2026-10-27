package com.sky.task;


import com.sky.entity.Orders;
import com.sky.mapper.OrdersMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    OrdersMapper orderMapper;

    //处理超时订单
    @Scheduled(cron = "0 * * * * ?") //每分钟执行一次检查
    public void processTimeoutOrders(){
        log.info("处理超时<15分钟>未支付订单");

        LocalDateTime ddl=LocalDateTime.now().minusMinutes(15);//当前时间减去15分钟

        // select * from orders where status = ? and order_time < (当前时间-15分钟)
        List<Orders> orders = orderMapper.selectByStatusAndOrderTime(Orders.PENDING_PAYMENT,ddl);

        if(orders!=null && orders.size()>0){
            for (Orders order : orders) {
                order.setStatus(Orders.CANCELLED);
                order.setCancelTime(LocalDateTime.now());
                order.setCancelReason("订单超时未支付，系统自动取消");
                orderMapper.update(order);
                log.info("订单{}超时未支付已自动取消",order.getId());
            }
        }
    }

    //处理打烊了还一直在派送中的订单
    @Scheduled(cron = "0 0 1 * * ?") //每天凌晨1点执行一次检查
    public void processDeliveryOrders(){
        log.info("处理打烊了还一直在派送中的订单");
        LocalDateTime ddl=LocalDateTime.now().minusMinutes(60);
        List<Orders> orders = orderMapper.selectByStatusAndOrderTime(Orders.DELIVERY_IN_PROGRESS,ddl);
        if(orders!=null && orders.size()>0){
            for (Orders order : orders) {
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
                log.info("订单{}已自动完成",order.getId());
            }
        }
    }
}
