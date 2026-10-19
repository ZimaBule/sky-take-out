package com.sky.controller.user;


import com.sky.entity.Category;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userCategoryController")
@RequestMapping("/user/category")
@Api(tags = "分类相关接口")
@Slf4j
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/list")
    @ApiOperation("获取分类列表")
    public Result<List<Category>> getCategory(@RequestParam(required = false) Integer type) {
        //当没输入type时，返回所有分类(菜品分类+套餐分类)，所以这里设为非必须参数required = false(不设前端没传时会报错)
        List<Category> categoryList = categoryService.list(type);
        log.info("获取分类列表");
        return Result.success(categoryList);
    }
}
