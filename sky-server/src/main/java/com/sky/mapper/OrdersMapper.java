package com.sky.mapper;

import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrdersMapper {

    void insert(Orders order);

    @Select("SELECT * FROM orders WHERE number = #{outTradeNo}")
    Orders getByNumber(String outTradeNo);

    void update(Orders orders);


}
