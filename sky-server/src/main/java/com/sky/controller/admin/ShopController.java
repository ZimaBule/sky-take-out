package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")//()里填写此控制器在MVC容器作为Bean的别名，
                                      // 防止在MVC中和User的ShopController控制器名字冲突
@RequestMapping("/admin/shop")
@Api(tags = "管理端店铺相关接口")
@Slf4j
public class ShopController {

    @Autowired
    private RedisTemplate redisTemplate;
    @PutMapping("/{status}")
    @ApiOperation("修改店铺状态")
    public Result updateStatus(@PathVariable Integer status) {
        log.info("当前店铺状态为:{}", status==1 ? "营业中" : "打烊中");
        redisTemplate.opsForValue().set("shopStatus", status);
        return Result.success();
    }

    @ApiOperation("获取店铺状态")
    @GetMapping("/status")
    public Result getStatus() {
        Integer shopStatus = (Integer) redisTemplate.opsForValue().get("shopStatus");
        log.info("当前店铺状态为:{}", shopStatus==1 ? "营业中" : "打烊中");
        return Result.success(shopStatus);
    }
}
