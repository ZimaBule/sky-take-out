package com.sky.controller.user;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/list")
    @ApiOperation("根据菜品分类来条件查询包含菜品")
    public Result<List<DishVO>> list(@RequestParam Long categoryId){
        log.info("根据条件查询菜品列表：{}", categoryId);

        //1.构造redis中的key，格式为：categoryId
        String key="category_"+categoryId;

        //2.查询redis中是否存在该分类下的相关菜品数据
        List<DishVO> dishVOList = (List<DishVO>) redisTemplate.opsForValue().get(key);
        /* RedisTemplate 能存、能取回 List<DishVO>，靠的是其默认的内置 JDK 序列化器：
        存的时候把整个对象图连同"类型元数据"序列化成字节，取的时候按元数据原样重建对象。
        前提是相关类实现了 Serializable。 */
        if(dishVOList != null && dishVOList.size() > 0){
        //3.1如果存在，直接返回
            return Result.success(dishVOList);
        }

        //3.2如果不存在，查询数据库，并将结果存入redis
        dishVOList = dishService.listWithFlavor(categoryId);
        redisTemplate.opsForValue().set(key, dishVOList);
        return Result.success(dishVOList);
    }
}
