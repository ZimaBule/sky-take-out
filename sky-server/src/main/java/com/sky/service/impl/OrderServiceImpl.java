package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Value("${sky.shop.address}")
    private String shopAddress;

    @Value("${sky.baidu.ak}")
    private String ak;

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

        // 检查地址是否超出配送范围
        checkOutOfRange(addressBooks.get(0).getCityName()+addressBooks.get(0).getDistrictName()+addressBooks.get(0).getDetail());

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

    @Override
    public PageResult historyOrdersUser(Integer page, Integer pageSize, Integer status) {
        log.info("分页查询当前用户历史订单,page:{},pageSize:{}",page,pageSize);
        PageHelper.startPage(page,pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        Page<Orders> page1 = ordersMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> orderVOList = new ArrayList<>();
        for (Orders order : page1.getResult()) {
            OrderVO orderVO= new OrderVO();
            BeanUtils.copyProperties(order, orderVO);
            List<OrderDetail> orderDetailList = ordersDetailMapper.getByOrderId(order.getId());
            orderVO.setOrderDetailList(orderDetailList);
            orderVOList.add(orderVO);
        }

        PageResult pageResult = new PageResult(page1.getTotal(), orderVOList);
        return pageResult;
    }

    @Override
    public OrderVO getOrderDetail(Long id) {
        log.info("根据id查询订单详情，id:{}",id);
        Orders order = ordersMapper.getById(id);
        List<OrderDetail> orderDetail = ordersDetailMapper.getByOrderId(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);
        orderVO.setOrderDetailList(orderDetail);
        log.info("订单详情：{}",orderVO);
        return orderVO;
    }

    @Override
    public void cancel(Long id) {
        log.info("取消订单，id:{}",id);
        //1.根据id查询订单是否存在
        Orders order = ordersMapper.getById(id);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //2.根据订单状态来处理
        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        //1~2： 可以取消，且2需要退款并更改支付状态
        //3~6： 订单状态异常，拒绝取消
        if (order.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }else if(order.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            log.info("处于2待接单状态，需要退款并更改支付状态");
            /*
            //调用微信支付退款接口
            weChatPayUtil.refund(
                    order.getNumber(), //商户订单号
                    order.getNumber(), //商户退款单号
                    new BigDecimal(0.01),//退款金额，单位 元
                    new BigDecimal(0.01));//原订单金额
            */
            //支付状态(0未支付 1已支付 2退款)修改为 退款
            order.setPayStatus(Orders.REFUND); //支付状态修改为 退款
        }

        //3.更新订单状态、取消原因、取消时间
        order.setStatus(Orders.CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason("用户取消订单");
        log.info("订单取消，orderId:{}，状态：{}",id,order.getStatus());
        ordersMapper.update(order);

    }

    @Override
    public void repetition(Long orderId) {
        log.info("订单重复提交，orderId:{}",orderId);
        //再来一单就是将原订单中的商品重新加入到购物车中
        List<OrderDetail> orderDetails = ordersDetailMapper.getByOrderId(orderId);
        for (OrderDetail orderDetail : orderDetails) {
            ShoppingCart shoppingCart = new ShoppingCart();
            // 拷贝同名属性，忽略 id（id 由数据库自增生成）
            BeanUtils.copyProperties(orderDetail, shoppingCart, "id");
            shoppingCart.setUserId(BaseContext.getCurrentId());
            shoppingCartMapper.add(shoppingCart);
        }
    }

    @Override
    public PageResult search(OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("分页查询订单列表，ordersPageQueryDTO:{}",ordersPageQueryDTO);
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        
        //ordersPageQueryDTO.setUserId(BaseContext.getCurrentId()); 管理端查询不需要设置用户ID
        
        Page<Orders> page = ordersMapper.pageQuery(ordersPageQueryDTO);
        
        List<OrderVO> orderVOList = new ArrayList<>();
        for (Orders order : page.getResult()) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order, orderVO);

            // 获取订单明细
            List<OrderDetail> orderDetailList = ordersDetailMapper.getByOrderId(order.getId());
            StringBuilder dishNames = new StringBuilder();//累加字符串必须用 StringBuilder，
                                                          // 因为String 不可变，+= 会不断创建新对象
            for (OrderDetail orderDetail : orderDetailList) {
                dishNames.append(orderDetail.getName())
                         .append("*")
                         .append(orderDetail.getNumber())
                         .append(";");
            }
            orderVO.setOrderDishes(dishNames.toString());
            log.info("此订单菜品：{}",dishNames);
            orderVOList.add(orderVO);
        }
        
        return new PageResult(page.getTotal(), orderVOList);
    }

    @Override
    public OrderStatisticsVO orderStatistics() {
        Integer confirmed = ordersMapper.countByStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = ordersMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS);
        Integer toBeConfirmed = ordersMapper.countByStatus(Orders.TO_BE_CONFIRMED);
        return OrderStatisticsVO.builder()
                .toBeConfirmed(toBeConfirmed)
                .confirmed(confirmed)
                .deliveryInProgress(deliveryInProgress)
                .build();
    }

    @Override
    public void confirm(Long orderId) {
        log.info("确认订单，id:{}",orderId);
        Orders order = Orders.builder()
                             .id(orderId)
                             .status(Orders.CONFIRMED)
                             .build();

        ordersMapper.update(order);
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        log.info("拒绝接单，ordersRejectionDTO:{}",ordersRejectionDTO);
        //1.排除异常：
        // 订单只有存在 且 状态为2（待接单）才可以拒单
        Orders order = ordersMapper.getById(ordersRejectionDTO.getId());
        if (order == null || !order.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders order2 = Orders.builder()
                .id(ordersRejectionDTO.getId())
                .status(Orders.CANCELLED)
                .cancelReason(ordersRejectionDTO.getRejectionReason())
                .cancelTime(LocalDateTime.now())
                .build();

        //2.若用户已支付，需要退款
        if (order.getPayStatus().equals(Orders.PAID)) {

            /* 模拟退款 */
            order2.setPayStatus(Orders.REFUND);
            /* 真实退款
                    String refund = weChatPayUtil.refund(
                    order.getNumber(),
                    order.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));*/
            log.info("申请退款");
        }

        ordersMapper.update(order2);
    }

    @Override
    public void cancelAdmin(OrdersCancelDTO ordersCancelDTO) {
        log.info("管理端取消订单，ordersCancelDTO:{}",ordersCancelDTO);
        //1.根据id查询订单是否存在
        Orders order = ordersMapper.getById(ordersCancelDTO.getId());
        if (order == null||order.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders order2 = Orders.builder()
                .id(ordersCancelDTO.getId())
                .status(Orders.CANCELLED)
                .cancelReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();

        //2.若用户已支付，需要退款
        if (order.getPayStatus().equals(Orders.PAID)) {

            /* 模拟退款 */
            order2.setPayStatus(Orders.REFUND);
            /* 真实退款
                    String refund = weChatPayUtil.refund(
                    order.getNumber(),
                    order.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));*/
            log.info("申请退款");
        }

        ordersMapper.update(order2);
    }

    @Override
    public void delivery(Long orderId) {
        log.info("骑手接单，orderId:{}",orderId);
        //1.排除异常：
        // 订单只有存在 且 状态为3（已接单）才可以派送
        Orders order = ordersMapper.getById(orderId);
        if (order == null || !order.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //2.更新订单状态为派送中
        Orders order2 = Orders.builder()
                             .id(orderId)
                             .status(Orders.DELIVERY_IN_PROGRESS)
                             .build();
        ordersMapper.update(order2);
    }

    @Override
    public void complete(Long orderId) {
        log.info("订单完成，orderId:{}",orderId);
        //1.排除异常：
        // 订单只有存在 且 状态为4（派送中）才可以完成
        Orders order = ordersMapper.getById(orderId);
        if (order == null || !order.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //2.更新订单状态为完成
        Orders order2 = Orders.builder()
                             .id(orderId)
                             .status(Orders.COMPLETED)
                             .deliveryTime(LocalDateTime.now())
                             .build();
        ordersMapper.update(order2);
    }


    /*
     * 检查客户的收货地址是否超出配送范围
     */
    private void checkOutOfRange(String address) {
        /* 1.获取店铺的经纬度坐标 */
        Map map = new HashMap();
        map.put("address",shopAddress);
        map.put("output","json");
        map.put("ak",ak);

        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("店铺地址解析失败");
        }

        //数据解析
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lng = location.getString("lng");
        String lat = location.getString("lat");
        //店铺经纬度坐标
        String shopLngLat = lng + "," +lat ;
        log.info("店铺经纬度坐标：{}", shopLngLat);

        /* 2.获取用户收货地址的经纬度坐标 */
        map.put("address",address);
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        jsonObject = JSON.parseObject(userCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("收货地址解析失败");
        }

        //数据解析
        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lng = location.getString("lng");
        lat = location.getString("lat");
        //用户收货地址经纬度坐标
        String userLngLat = lng + "," +lat;
        log.info("用户收货地址经纬度坐标：{}", userLngLat);

        /* 3.路线规划+计算距离 */
        map.put("origin",shopLngLat);
        map.put("destination",userLngLat);
        map.put("steps_info","0");
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        jsonObject = JSON.parseObject(json);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("配送路线规划失败");
        }

        //数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");
        if(distance > 5000){
            //配送距离超过5000米
            throw new OrderBusinessException("超出配送范围");
        }
    }
}
