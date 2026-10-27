package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.vo.OrderVO;
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

    @Select("SELECT * FROM orders WHERE id = #{id}")
    Orders getById(Long id);

    void update(Orders orders);



    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    @Select("SELECT COUNT(*) FROM orders WHERE status = #{status}")
    Integer countByStatus(Integer status);

    @Select("SELECT * FROM orders WHERE status = #{status} AND order_time < #{ddl}")
    List<Orders> selectByStatusAndOrderTime(Integer status, LocalDateTime ddl);
}
