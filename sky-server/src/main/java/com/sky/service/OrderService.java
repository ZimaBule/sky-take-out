package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {
    OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     */
    void paySuccess(String outTradeNo);


    PageResult historyOrdersUser(Integer page, Integer pageSize, Integer status);

    OrderVO getOrderDetail(Long id);

    void cancel(Long id);

    void repetition(Long id);

    PageResult search(OrdersPageQueryDTO ordersPageQueryDTO);

    OrderStatisticsVO orderStatistics();

    void confirm(Long orderId);

    void rejection(OrdersRejectionDTO ordersRejectionDTO);

    void cancelAdmin(OrdersCancelDTO ordersCancelDTO);

    void delivery(Long orderId);

    void complete(Long orderId);


    void reminder(Long id);
}
