package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SetmealMapper {

    /**
     * 根据分类id查询套餐的数量
     * @param id
     * @return
     */
    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long id);

    @Select("select setmeal_id from setmeal_dish where dish_id = #{dishId}")
    List<Long> getSetmealIdsByDishId(Long id);

    @AutoFill(value = OperationType.UPDATE)
    void update(Setmeal setmeal);

    @AutoFill(value = OperationType.INSERT)
    @Insert("insert into setmeal (id, name, category_id, price, description, image, create_time, update_time, create_user, update_user) " +
            "values (#{id}, #{name}, #{categoryId}, #{price}, #{description}, #{image}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    //MyBatis 的注解，作用是：在执行完 insert 语句后，把数据库自动生成的主键值"回填"到传入的 servicec层的 Setmeal 对象id属性中。
    void save(Setmeal setmeal);

    Page<Setmeal> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    @Select("select * from setmeal where id = #{id}")
    Setmeal getById(Long id);

    @Delete("delete from setmeal where id = #{id}")
    void delete(Long id);

    @Select("select * from setmeal where category_id = #{categoryId}")
    List<Setmeal> list(Long categoryId);
}
