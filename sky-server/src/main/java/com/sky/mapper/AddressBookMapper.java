package com.sky.mapper;

import com.sky.entity.AddressBook;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AddressBookMapper {

    List<AddressBook> list(AddressBook addressBook);

    void update(AddressBook addressBook2);

    @Insert("insert into address_book ( user_id,consignee, detail, is_default, label, phone, sex, city_code,city_name, district_code, district_name,province_code,province_name) " +
            "values ( #{userId},#{consignee},#{detail},#{isDefault}, #{label}, #{phone}, #{sex}, #{cityCode}, #{cityName}, #{districtCode}, #{districtName}, #{provinceCode}, #{provinceName})")
    void insert(AddressBook addressBook);

    @Delete("delete from address_book where id = #{id}")
    void deleteById(Long id);


}
