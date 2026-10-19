package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    @Transactional//对多个表一起操作时，要加声明事务
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        //使用对象拷贝属性时,必须二者的属性顺序和名称一致
        BeanUtils.copyProperties(dishDTO, dish);

        /* 第一张表： 向 '菜品表' 插入1条数据 */
        dishMapper.insert(dish);

        /* 第二张表： 向 '菜品口味表' 插入多条数据 */
        //获取insert语句生成的主键值(dish_id)来设置 flavors数组里dishFlavor对象的属性值dish_id
        Long dishId = dish.getId();
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            for (DishFlavor dishFlavor : flavors) {
                dishFlavor.setDishId(dishId);
            };
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        log.info("分页查询菜品的参数：{}", dishPageQueryDTO);
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        //不能返回Dish，因为Dish中没有categoryName属性，需要返回根据接口文档返回数据专门设计的DishVO
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    @Transactional//对多个表一起操作时，要加声明事务
    public void deleteBatch(List<Long> ids){
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if(dish.getStatus() == StatusConstant.ENABLE) {
                throw new DeletionNotAllowedException("菜品正在出售中，不能删除");
            }
        }
        List<Long> setmeal = dishMapper.getSetmealByDishIds(ids);
        if (setmeal != null && setmeal.size() > 0) {
            throw new DeletionNotAllowedException("菜品已关联套餐，不能删除");
        }
        for (Long id : ids) {
            dishMapper.deleteById(id);
            dishFlavorMapper.deleteByDishId(id);
        }
    };

    @Override
    public DishVO getByIdWithFlavor(Long id) {

        //1.先获取菜品和口味数据
        Dish dish = dishMapper.getById(id);////获取菜品数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);//获取菜品口味数据

        //2.再封装进DishVO返回
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        //没有合适的类来同时传dishDTO的属性和更新时间/操作人：dishMapper.updateWithFlavor(dishDTO);
        //所以可以分2步来更新：

        //1. 更新dish表
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);

        //2. 更新dish_flavor表
        /* 注意：因为可能原来此菜品就有口味(而且可能不止一种)
                所以直接先删除再插入      */
        dishFlavorMapper.deleteByDishId(dishDTO.getId());//先删除
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            for (DishFlavor dishFlavor : flavors) {
                dishFlavor.setDishId(dishDTO.getId());
            };
            dishFlavorMapper.insertBatch(flavors);//再插入
        }
    }

    @Override
    public void updateStatus(Integer status, Long dishId) {
        //根据id修改菜品状态
        Dish dish = new Dish();
        dish.setId(dishId);
        dish.setStatus(status);
        dishMapper.update(dish);

        //如果修改的是停售状态，则需要同时将此菜品关联的口味数据也修改为停售状态
        if(status == StatusConstant.DISABLE){
            List<Long> setmealIds = setmealMapper.getSetmealIdsByDishId(dishId);
            if(setmealIds != null && setmealIds.size() > 0) {
                for (Long setmealId : setmealIds) {
                    Setmeal setmeal = new Setmeal();
                    setmeal.setId(setmealId);
                    setmeal.setStatus(status);
                    setmealMapper.update(setmeal);
                }
            }
        }
    }

    //根据菜品分类categoryId来查询相关菜品
    @Override
    public List<Dish> getDishesByCategoryId(Long categoryId) {
        return dishMapper.getDishesByCategoryId(categoryId);
    }

    @Override
    public List<DishVO> listWithFlavor(Long categoryId) {
        List<Dish> dishList = dishMapper.getDishesByCategoryId(categoryId);
        List<DishVO> dishVOList = new   ArrayList<>();
        dishList.forEach(dish -> {
            //1.创建DishVO对象(共8个属性：categoryId,description,id,image,name,price 在 dish表
            //                        categoryName 在 category表, flavors 在 dish_flavor表)
            DishVO dishVO = new DishVO();
            //2.复制 dish表 中的6个属性到dishVO
            BeanUtils.copyProperties(dish, dishVO);
            //3.获取 dish_flavor表 中的dishFlavor并设置到dishVO中
            List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(dish.getId());
            dishVO.setFlavors(dishFlavors);
            //4.获取 category表 中的categoryName并设置到dishVO中
            Category category = categoryMapper.getById(dish.getCategoryId());
            dishVO.setCategoryName(category.getName());

            dishVOList.add(dishVO);
        });
        return dishVOList;
    }
}
