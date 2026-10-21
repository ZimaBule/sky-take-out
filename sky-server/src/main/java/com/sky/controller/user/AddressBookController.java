package com.sky.controller.user;

import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.result.Result;
import com.sky.service.AddressBookService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/addressBook")
@Api(tags = "地址簿相关接口")
@Slf4j
public class AddressBookController {

    @Autowired
    private AddressBookService addressBookService;
    @PostMapping()
    public Result add(@RequestBody AddressBook addressBook) {
        log.info("添加地址簿");
        addressBookService.add(addressBook);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<AddressBook>> list() {
        log.info("查询当前用户地址簿列表");
        Long userId = BaseContext.getCurrentId();
        List<AddressBook> addressBooks = addressBookService.getByUserId(userId);
        return Result.success(addressBooks);
    }

    @GetMapping("/default")
    public Result<AddressBook> getDefault() {
        log.info("查询当前用户默认地址");
        Long userId = BaseContext.getCurrentId();
        AddressBook addressBook = addressBookService.getDefault(userId);
        return Result.success(addressBook);
    }

    @DeleteMapping()
    public Result delete(@RequestParam Long id) {
        log.info("删除地址簿");
        addressBookService.deleteById(id);
        return Result.success();
    }

    @PutMapping
    public Result update(@RequestBody AddressBook addressBook) {
        log.info("修改地址簿");
        addressBookService.update(addressBook);
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("根据id查询地址")
    public Result<AddressBook> getById(@PathVariable Long id) {
        log.info("根据id查询地址");
        AddressBook addressBook = addressBookService.getById(id);
        return Result.success(addressBook);
    }

    @PutMapping("/default")
    public Result setDefault(@RequestBody AddressBook addressBook) {
        log.info("设置默认地址簿");
        addressBookService.setDefault(addressBook.getId());
        return Result.success();
    }


}
