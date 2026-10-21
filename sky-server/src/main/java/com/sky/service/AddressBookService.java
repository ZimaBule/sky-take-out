package com.sky.service;

import com.sky.entity.AddressBook;

import java.util.List;

public interface AddressBookService {

    void add(AddressBook addressBook);

    List<AddressBook> getByUserId(Long userId);

    AddressBook getDefault(Long userId);

    void deleteById(Long id);

    void update(AddressBook addressBook);

    AddressBook getById(Long id);

    void setDefault(Long id);
}
