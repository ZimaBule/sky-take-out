package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.mapper.AddressBookMapper;
import com.sky.service.AddressBookService;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressBookServiceImpl implements AddressBookService {

    @Autowired
    private AddressBookMapper addressBookMapper;
    @Override
    public void add(AddressBook addressBook) {

        // 检查数据库表约束的不能为空的字段：is_default、user_id和 phone(phone由前端检查)
        // 新增地址时前端没有传入：is_default、user_id，直接设置
        addressBook.setIsDefault(0);;
        addressBook.setUserId(BaseContext.getCurrentId());

        addressBookMapper.insert(addressBook);
    }

    @Override
    public List<AddressBook> getByUserId(Long userId) {
        AddressBook addressBook = AddressBook.builder()
                                                .userId(userId)
                                                .build();
        return addressBookMapper.list(addressBook);
    }

    @Override
    public AddressBook getDefault(Long userId) {
        AddressBook addressBook = AddressBook.builder()
                                                .userId(userId)
                                                .isDefault(1)
                                                .build();
        return addressBookMapper.list(addressBook).get(0);
    }

    @Override
    public void deleteById(Long id) {
        addressBookMapper.deleteById(id);
    }

    @Override
    public void update(AddressBook addressBook) {
        addressBookMapper.update(addressBook);
    }

    @Override
    public AddressBook getById(Long id) {
        AddressBook addressBook = AddressBook.builder()
                                                .id(id)
                                                .build();
        List<AddressBook> addressBooks = addressBookMapper.list(addressBook);
        return addressBooks.get(0);
    }

    @Override
    public void setDefault(Long id) {

        AddressBook addressBook1 = AddressBook.builder()
                                                .userId(BaseContext.getCurrentId())
                                                .isDefault(1)
                                                .build();
        List<AddressBook> list = addressBookMapper.list(addressBook1);
        if (list != null && list.size() > 0) {
            AddressBook addressBook2 = list.get(0);
            addressBook2.setIsDefault(0);
            addressBookMapper.update(addressBook2);
        }

        AddressBook addressBook3 = AddressBook.builder()
                                                .id(id)
                                                .isDefault(1)
                                                .build();
        addressBookMapper.update(addressBook3);
    }
}
