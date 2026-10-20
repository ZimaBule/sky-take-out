package com.sky.controller.user;


import com.sky.entity.Setmeal;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("userSetmealController")
@RequestMapping("/user/setmeal")
@Api(tags = "套餐相关接口")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @GetMapping("/list")
    @ApiOperation("根据分类id查询套餐")
    @Cacheable(value = "setmealCache", key = "#categoryId")//用‘SpringEL表达式’指定与形参相关的动态key useCache::id
    //那个key对应的value就是方法的返回值Result
    public Result<List<Setmeal>> list(@RequestParam Long categoryId) {
        List<Setmeal> setmealList = setmealService.list(categoryId);
        return Result.success(setmealList);
    }

    @GetMapping("/dish/{id}")
    @ApiOperation("根据套餐id查询包含的菜品")
    public Result<List<DishItemVO>> getById(@PathVariable Long id) {
        List<DishItemVO> dishItemVOList = setmealService.getDishItemVOBySetmealId(id);
        return Result.success(dishItemVOList);
    }

}
