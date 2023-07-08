package com.zpy.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zpy.reggie.common.CustomException;
import com.zpy.reggie.entity.Category;
import com.zpy.reggie.entity.Dish;
import com.zpy.reggie.entity.Setmeal;
import com.zpy.reggie.mapper.CategoryMapper;
import com.zpy.reggie.service.CategoryService;
import com.zpy.reggie.service.DishService;
import com.zpy.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService{

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealService setmealService;

    /**
     * 根据id删除分类， 删除之前需要进行判断
     *
     * @param id
     */
    @Override
    public void remove(Long id) {

        // 查询对应的分类是否关联了菜品, 如果已经关联, 抛出一个业务异常
        LambdaQueryWrapper<Dish> dishQueryWrapper = new LambdaQueryWrapper<>();
        // 添加查询条件, 根据分类id进行查询
        dishQueryWrapper.eq(Dish::getCategoryId, id);
        int dishCount = dishService.count(dishQueryWrapper);
        // 查询对应的分类是否关联了菜品, 如果已经关联, 抛出一个业务异常
        if (dishCount > 0) {
            // 已经关联菜品, 抛出一个业务异常
            throw new CustomException("当前分类下关联了菜品, 不能删除");
        }
        // 查询对应的分类是否关联了套餐, 如果已经关联, 抛出一个业务异常
        LambdaQueryWrapper<Setmeal> setmealQueryWrapper = new LambdaQueryWrapper<>();
        // 添加查询条件, 根据分类id进行查询
        setmealQueryWrapper.eq(Setmeal::getCategoryId, id);
        int setmealCount = setmealService.count(setmealQueryWrapper);
        // 查询对应的分类是否关联了菜品, 如果已经关联, 抛出一个业务异常
        if (setmealCount > 0) {
            // 已经关联套餐, 抛出一个业务异常
            throw new CustomException("当前分类下关联了套餐, 不能删除");
        }
        // 正常删除分类
        super.removeById(id);
    }
}
