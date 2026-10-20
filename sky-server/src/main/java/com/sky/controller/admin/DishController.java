package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Category;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品：{}", dishDTO);
        dishService.saveWithFlavor(dishDTO);

        //清理缓存
        Long categoryId = dishDTO.getCategoryId();
        String key = "category_" + categoryId;
        cleanCache(key);

        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("分页查询菜品")
    public Result<PageResult> pageQuery(DishPageQueryDTO dishPageQueryDTO){
        log.info("分页查询菜品：{}", dishPageQueryDTO);
        //调用service层的分页查询方法
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    @DeleteMapping
    @ApiOperation("删除菜品")
    public Result delete(@RequestParam List<Long> ids){
        //加上@RequestParam后，MVC能自动解析Query传过来的字符串“1，2，3”，然后存放在集合中
        log.info("删除菜品：{}", ids);
        dishService.deleteBatch(ids);

        //清理缓存
        cleanCache("category_*");//删除所有菜品缓存(因为可能是批量删除，所以直接删除所有菜品缓存)

        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("根据id查询菜品：{}", id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    @GetMapping("/list")
    @ApiOperation("根据菜品分类来条件查询包含菜品")
    public Result<List<Dish>> list(@RequestParam Long categoryId){
        log.info("根据条件查询菜品列表：{}", categoryId);
        List<Dish> dishList = dishService.getDishesByCategoryId(categoryId);
        return Result.success(dishList);
    }

    @PutMapping
    @ApiOperation("修改菜品")
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("修改菜品：{}", dishDTO);
        dishService.updateWithFlavor(dishDTO);

        //清理缓存
        cleanCache("category_*");//删除所有菜品缓存(因为可能修改菜品的分类，所以直接删除所有菜品缓存)

        return Result.success();
    }

    @PostMapping("/status/{status}")
    @ApiOperation("修改菜品状态")
    public Result updateStatus(@PathVariable Integer status, @RequestParam Long id){
        log.info("修改菜品状态：{},{}", status, id);
        dishService.updateStatus(status, id);

        //清理缓存
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        Long categoryId = dishVO.getCategoryId();
        String key = "category_" + categoryId;
        cleanCache(key);

        return Result.success();
    }

    private void cleanCache(String pattern){
            Set keys = redisTemplate.keys(pattern);
            redisTemplate.delete(keys);
    }
}
