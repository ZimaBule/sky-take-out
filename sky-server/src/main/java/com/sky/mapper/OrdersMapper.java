package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.vo.OrderVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    Double sumByMap(Map map);

    List<Map> getTurnoverGroupByDay(LocalDate beginTime, LocalDate endTime);

    List<Map> getOrderCountGroupByDay(Map map);

    @Select("select od.name,sum(od.number) as number from order_detail od,orders o "+
            "where od.order_id=o.id and o.status=5 and o.order_time between #{begin} and #{end} " +
            "group by od.name " +
            "order by number desc "+
            "limit 10")
    List<Map> getTop10(LocalDate begin, LocalDate end);

    //List<Map> getValidOrderCountGroupByDay(LocalDate beginTime, LocalDate endTime);
}
