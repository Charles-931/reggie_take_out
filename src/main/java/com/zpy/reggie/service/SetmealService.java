package com.zpy.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zpy.reggie.dto.SetmealDto;
import com.zpy.reggie.entity.Setmeal;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {

    /**
     * 新增套餐, 同时需要保存套餐和菜品的关联关系
     * @param setmealDto
     */
    void saveWithDish(SetmealDto setmealDto);

    /**
     * 删除套餐, 同时要删除套餐和菜品之间的关联数据
     * @param ids
     */
    void removeWithDish(List<Long> ids);
}
