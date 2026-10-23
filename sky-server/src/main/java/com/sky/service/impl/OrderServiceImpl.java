package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderDetailMapper ordersDetailMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Override
    @Transactional//要改变多个表，别忘了声明事务
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO){
        /* 1.先处理异常(检查：地址、购物车数据是否为空)*/
        Long addressId = ordersSubmitDTO.getAddressBookId();
        AddressBook addressBook1 = AddressBook.builder()
                                             .id(addressId)
                                             .build();
        log.info("addressBook: {}", addressBook1);
        List<AddressBook> addressBooks = addressBookMapper.list(addressBook1);
        if (addressBooks == null ||addressBooks.size() == 0){
            throw new ShoppingCartBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        log.info("此订单收货信息addressBook2: {}", addressBooks.get(0));

        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                                             .userId(userId)
                                             .build();
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.list(shoppingCart);
        if (shoppingCarts == null || shoppingCarts.size() == 0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        /* 2.向表插入数据 */
        //2.1向Order订单表插入1条数据
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, order);
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setStatus(Orders.PENDING_PAYMENT);
        order.setUserId(userId);
        order.setOrderTime(LocalDateTime.now());
        order.setPayStatus(Orders.UN_PAID);
        AddressBook addressBook2 = addressBooks.get(0);
        order.setPhone(addressBook2.getPhone());
        order.setAddress(addressBook2.getDetail());
        order.setConsignee(addressBook2.getConsignee());

        ordersMapper.insert(order);//SQL语句记得添加返回主键，否则后面的order.getId()获取的是null


        //2.2.向OrderDetail订单明细表插入多条数据
        List<OrderDetail> orderDetails = new ArrayList<>();
        for (ShoppingCart cart : shoppingCarts) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(order.getId());
            orderDetails.add(orderDetail);
        }
        ordersDetailMapper.batchInsert(orderDetails);

        /* 3.清空购物车数据 */
        shoppingCartMapper.deleteByUserId(BaseContext.getCurrentId());

        /* 4.返回结果OrderSubmitVO */
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                                               .id(order.getId())
                                               .orderAmount(order.getAmount())
                                               .orderNumber(order.getNumber())
                                               .orderTime(LocalDateTime.now())
                                               .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     */
    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

/*        //真实微信支付成功——s1:先调用微信支付接口，生成并返回预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );
        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));
        return vo;*/


        // 模拟微信支付成功——返回预支付交易标识(模拟的标识是空的)
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code","ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        // 模拟微信支付成功——paySuccess()中更新订单状态
        Orders order = ordersMapper.getByNumber(ordersPaymentDTO.getOrderNumber());
        order.setPayStatus(Orders.PAID);//支付状态，已支付
        order.setStatus(Orders.TO_BE_CONFIRMED);//订单状态，待接单
        order.setCheckoutTime(LocalDateTime.now());//支付时间
        ordersMapper.update(order);

        return vo;


    }

    /**
     * 真实支付成功，修改订单状态
     */
    @Override
    public void paySuccess(String outTradeNo) {
        // 根据订单号查询订单
        Orders ordersDB = ordersMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        ordersMapper.update(orders);
    }

}
