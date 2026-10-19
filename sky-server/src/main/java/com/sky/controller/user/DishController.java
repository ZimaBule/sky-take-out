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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;


    @GetMapping("/list")
    @ApiOperation("根据菜品分类来条件查询包含菜品")
    public Result<List<DishVO>> list(@RequestParam Long categoryId){
        log.info("根据条件查询菜品列表：{}", categoryId);
        List<DishVO> dishVOList = dishService.listWithFlavor(categoryId);
        return Result.success(dishVOList);
    }


}
