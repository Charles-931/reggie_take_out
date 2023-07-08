package com.zpy.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zpy.reggie.common.CustomException;
import com.zpy.reggie.dto.SetmealDto;
import com.zpy.reggie.entity.Setmeal;
import com.zpy.reggie.entity.SetmealDish;
import com.zpy.reggie.mapper.SetmealMapper;
import com.zpy.reggie.service.SetmealDishService;
import com.zpy.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private SetmealDishService setmealDishService;

    /**
     * 新增套餐, 同时需要保存套餐和菜品的关联关系
     * @param setmealDto
     */
    @Override
    @Transactional
    public void saveWithDish(SetmealDto setmealDto) {
        log.info("新增套餐信息: {}", setmealDto);
        // 保存套餐基本信息 ---setmeal表, insert操作
        this.save(setmealDto);

        // 给setmealDishes附上setmealId属性
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes = setmealDishes.stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());

        // 保存套餐与菜品之间的关系 ---setmeal_dish表, insert操作
        if (setmealDto.getSetmealDishes() != null) {
            setmealDishService.saveBatch(setmealDishes);
        }
    }

    /**
     * 删除套餐, 同时要删除套餐和菜品之间的关联数据
     * @param ids
     */
    @Override
    @Transactional
    public void removeWithDish(List<Long> ids) {
        // select count(*) from setmeal where id in (ids) and status = 1;
        // 查询套餐状态, 确定是否可以删除()是否已停售
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Setmeal::getId, ids);
        queryWrapper.eq(Setmeal::getStatus, 1);

        int count = this.count(queryWrapper);
        //如果不能删除, 抛出一个业务异常
        if (count > 0) {
            throw new CustomException("套餐正在售卖中, 不能删除");
        }

        // 如果可以删除, 先删除套餐表中的数据 --setmeal
        this.removeByIds(ids);

        // delete from setmeal_dish where setmeal_id in (ids);
        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(SetmealDish::getSetmealId, ids);
        // 删除关系表中的数据   --setmeal_dish
        setmealDishService.remove(lambdaQueryWrapper);
    }
}
