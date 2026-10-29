package com.sky.service.impl;


import com.sky.mapper.OrdersMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        log.info("进入营业额统计");
        // TurnoverReportVO 要返回 String dateList 和 String turnoverList
        // 1. 获取日期列表dateList
        List<LocalDate> dateList = new ArrayList<>();
        if(begin.isAfter(end)){
            throw new RuntimeException("开始日期不能大于结束日期");
        }
        while(!begin.isAfter(end)){
            dateList.add(begin);
            begin = begin.plusDays(1);
        }
        log.info("日期列表：{}", dateList);

        // 2. 获取营业额列表turnoverList

/*         //法一：多次查询数据库
            List<Double> turnoverList = new ArrayList<>();
            for (LocalDate date : dateList) {

            LocalDateTime startTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //select sum(amount) from orders where order_time > startTime and order_time < endTime and status = 5
            //为了代码复用，所以用Map来封装参数
            Map map = new HashMap<>();
            map.put("beginTime", startTime);
            map.put("endTime", endTime);
            map.put("status", 5);
            Double turnover = ordersMapper.sumByMap(map);
            if(turnover == null){ // 如果营业额为null，那么折线统计图会直接断掉，所以要设为0
                turnover = 0.0;
            }
            turnoverList.add(turnover);
        }*/

        /* 法二：一次查询数据库  用哈希表来实现*/
        //select date_format(order_time, '%Y-%m-%d') as date, sum(amount) as turnover
        // from orders where order_time >= ? and order_time <= ? and status = 5 group by date
        //2.1 先用 ‘集合’ 把 ‘数据库查询的一堆成对结果’ 用 ‘一堆映射’ 封装起来
        LocalDate startDate = dateList.get(0);
        LocalDate endDate = dateList.get(dateList.size()-1);
        List<Map> turnoverMapList = ordersMapper.getTurnoverGroupByDay(startDate, endDate.plusDays(1));/* 按天分组时，要加一天，不然会截止到最后一天0:00 */
        /* 查出来的结果List<Map>结构是：[{date=2023-01-01, turnover=100.0}, {date=2023-01-02, turnover=200.0}]
                                    即每个Map包含两个键值对，两个键key分别为date和turnover <由来：SQL语句里写的date和turnover> */
        //2.2 准备用map存放对应日期的营业额 (目的：把上面的2个键值对转成1个键值对的映射)
        Map<String, Double> turnoverMap = new HashMap<>();
        for (Map map : turnoverMapList) {
            // sum(decimal列) 经 JDBC 返回的是 BigDecimal，不能直接强转 Double
            //turnoverMap.put( (String) map.get("date"), (Double) map.get("turnover"));
            turnoverMap.put( (String) map.get("date"), Double.valueOf(map.get("turnover").toString()) );
        }
        //2.3 准备按日期顺序存放营业额
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            turnoverList.add(turnoverMap.getOrDefault(date.toString(), 0.0));//在 key 不存在时返回默认值0.0
        }
        log.info("营业额列表：{}", turnoverList);

        String dateListStr = StringUtils.join(dateList,",");
        String turnoverListStr = StringUtils.join(turnoverList,",");
        TurnoverReportVO turnoverReportVO = TurnoverReportVO.builder()
                .dateList(dateListStr)
                .turnoverList(turnoverListStr)
                .build();
        return turnoverReportVO;
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        log.info("进入用户统计");
        // UserReportVO 要返回 String dateList 、 String totalUserList 和 String newUserList
        // 1. 获取日期列表dateList
        List<LocalDate> dateList = new ArrayList<>();
        if(begin.isAfter(end)){
            throw new RuntimeException("开始日期不能大于结束日期");
        }
        while(!begin.isAfter(end)){
            dateList.add(begin);
            begin = begin.plusDays(1);
        }
        log.info("日期列表：{}", dateList);

        // 2. 获取总用户数列表totalUserList和 newUserList <去User表并用其createTime字段进行统计>
        // 2.1 为了代码复用而使用动态条件查询，所以用Map来封装参数
        Map newMap=new HashMap<>();/* 这样创建出来的map默认是<String,Object>z
                                         但进入数据库后MyBatis 的 TypeHandler：会负责把 Object 里面真实类型转成数据库对应字段类型 */
        newMap.put("beginTime", dateList.get(0));
        newMap.put("endTime", dateList.get(dateList.size()-1).plusDays(1));
        // 2.2 用 ‘集合’ 把 ‘数据库查询的一堆成对结果’ 用 ‘一堆映射’ 封装起来
        List<Map> newUserMapList = userMapper.getUserCountGroupByDay(newMap);
        // 2.3 准备用map存放对应日期的总人数
        Map<String, Integer> newUserMap = new HashMap<>();
        for (Map map : newUserMapList) {
            newUserMap.put( (String) map.get("date"), Integer.valueOf(map.get("userCount").toString()) );
        }

        // 2.4 准备按日期顺序存放总人数和新人数
        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();
        for (LocalDate date : dateList) {

            newUserList.add(newUserMap.getOrDefault(date.toString(), 0));

            //总用户数列表totalUserList必须多次查表了
            Map totalMap = new HashMap<>();
            totalMap.put("endTime", date.plusDays(1));
            Integer totalUser = userMapper.countByMap(totalMap);
            totalUserList.add(totalUser);
        }

        //2.5 将日期列表、总用户数列表和 newUserList 转成字符串后，用 UserReportVO 封装
        String dateListStr = StringUtils.join(dateList,",");
        String totalUserListStr = StringUtils.join(totalUserList,",");
        String newUserListStr = StringUtils.join(newUserList,",");
        UserReportVO userReportVO = UserReportVO.builder()
                .dateList(dateListStr)
                .totalUserList(totalUserListStr)
                .newUserList(newUserListStr)
                .build();
        return userReportVO;


/*        List<Integer> totalUserList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime startTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            *//* select sum(amount) from user where create_time > startTime and create_time < endTime
            为了代码复用，所以用Map来封装参数 *//*
            Map map1 = new HashMap<>();
            map1.put("beginTime", startTime);
            map1.put("endTime", endTime);
            Integer totalUser = user.countByMap(map1);

            Map map2 = new HashMap<>();
            map2.put("endTime", endTime);
            Integer newUser = user.countByMap(map2);

            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }*/

    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        log.info("进入订单统计");
        List<LocalDate> dateList = new ArrayList<>();
        if(begin.isAfter(end)){
            throw new RuntimeException("开始日期不能大于结束日期");
        }
        while(!begin.isAfter(end)){
            dateList.add(begin);
            begin = begin.plusDays(1);
        }
        log.info("日期列表：{}", dateList);

        LocalDate startDate = dateList.get(0);//形参的begin已经变了，所以要重新获取开始日期
        LocalDate endDate = dateList.get(dateList.size()-1);
        Map allMap = new HashMap<>();
        allMap.put("beginTime", startDate);
        allMap.put("endTime", endDate.plusDays(1));
        List<Map> orderMapList = ordersMapper.getOrderCountGroupByDay(allMap);
        Map orderMap = new HashMap<>();
        for (Map map : orderMapList) {
            // COUNT() 经 JDBC 返回的是 Long，不能直接强转 Integer
            orderMap.put( (String) map.get("date"), Integer.valueOf(map.get("order_count").toString()) );
        }
        List<Integer> orderList = new ArrayList<>();
        for (LocalDate localDate : dateList) {
            orderList.add( (Integer)orderMap.getOrDefault(localDate.toString(), 0));
        }


        Map validMap = new HashMap<>();
        validMap.put("beginTime", startDate);
        validMap.put("endTime", endDate.plusDays(1));
        validMap.put("status", 5);
        List<Map> validOrderMapList = ordersMapper.getOrderCountGroupByDay(validMap);
        Map validOrderMap = new HashMap<>();
        for (Map map : validOrderMapList) {
            validOrderMap.put( (String) map.get("date"), Integer.valueOf(map.get("order_count").toString()) );
        }
        List<Integer> validOrderList = new ArrayList<>();
        for (LocalDate localDate : dateList) {
            validOrderList.add( (Integer)validOrderMap.getOrDefault(localDate.toString(), 0));
        }

        Integer totalOrderCount =0;
        for (Integer n : orderList) {
            totalOrderCount += n;
        }
        Integer ValidOrderCount =0;
        for (Integer n : validOrderList) {
            ValidOrderCount += n;
        }
        Double orderCompletionRate = totalOrderCount==0? 0.0 : ValidOrderCount.doubleValue()/totalOrderCount;

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .orderCountList(StringUtils.join(orderList,","))
                .validOrderCountList(StringUtils.join(validOrderList,","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(ValidOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        log.info("进入销售top10统计");
        List<Map> top10List = ordersMapper.getTop10(begin, end.plusDays(1));
        List<String> nameList = new ArrayList<>();
        List<Integer> numberList = new ArrayList<>();
        for (Map map : top10List) {
            nameList.add((String) map.get("name"));
            // sum(decimal列) 经 JDBC 返回的是 BigDecimal，不能直接强转
            //numberList.add((Integer) map.get("number"));
            numberList.add(Integer.valueOf(map.get("number").toString()));
        }
        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList,","))
                .numberList(StringUtils.join(numberList,","))
                .build();

    }


}
