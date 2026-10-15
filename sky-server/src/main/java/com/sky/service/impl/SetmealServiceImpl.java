package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sky.mapper.SetmealMapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private DishMapper dishMapper;

    @Transactional
    @Override
    public void save(SetmealDTO setmealDTO) {
        log.info("套餐信息保存");

        //分两步：
        //第一步：保存套餐表信息
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.save(setmeal);

        //第二步：保存套餐和菜品的关联表信息
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        //获取生成的套餐id
        /*setmealMapper的save方法要加：
             @Options(useGeneratedKeys = true, keyProperty = "id")
             service中才能拿到数据库自增的主键值id      */
        Long setmealId = setmeal.getId();
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(setmealId);
        }
        setmealDishMapper.saveBatch(setmealDishes);

    }

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        log.info("套餐分页查询");
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<Setmeal> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        PageResult pageResult = new PageResult( page.getTotal(),page.getResult());
        return pageResult;
    }

    @Override
    public void delete(List<Long> ids) {
        log.info("套餐信息删除");
        for (Long id : ids) {
            Setmeal setmeal = setmealMapper.getById(id);
            if (setmeal.getStatus() == StatusConstant.ENABLE) {
                throw new DeletionNotAllowedException("套餐已启用，不能删除");
            }
        }
        for (Long setmealId : ids) {
            setmealMapper.delete(setmealId);
            setmealDishMapper.deleteBySetmealId(setmealId);

        }
    }

    @Override
    public SetmealVO getById(Long id) {
        log.info("根据id查询套餐");
        Setmeal setmeal = setmealMapper.getById(id);
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);

        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    @Override
    public void update(SetmealDTO setmealDTO) {

        //分两步
        //第一步：修改套餐表信息
        log.info("套餐信息修改");
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);

        //第二步：修改套餐和菜品的关联表信息
        setmealDishMapper.deleteBySetmealId(setmealDTO.getId());
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(setmealDTO.getId());
        }
        setmealDishMapper.saveBatch(setmealDishes);
    }

    @Override
    public void updateStatus(Integer status, Long setmealId) {
        log.info("套餐状态修改");
        Setmeal setmeal = new Setmeal();
        setmeal.setStatus(status);
        setmeal.setId(setmealId);//只修改部分状态也别忘了设置id，才能让mapper层的update识别

        //分两种情况：
        //情况一：如果修改的是停售状态，则直接更新
        //情况二：如果修改的是起售状态，则需要判断套餐中的菜品是否都已起售
        if(status == StatusConstant.DISABLE){
            setmealMapper.update(setmeal);
        }else{
            List<Long> dishIds = setmealDishMapper.getDishIdsBySetmealId(setmealId);
            if(dishIds != null && dishIds.size() > 0) {
                for (Long dishId : dishIds) {
                    Dish dish = dishMapper.getById(dishId);
                    if(dish.getStatus() == StatusConstant.DISABLE) {
                        throw new SetmealEnableFailedException("有菜品停售，不能启用套餐");
                    }
                }
            }
            setmealMapper.update(setmeal);
        }
    }
}
