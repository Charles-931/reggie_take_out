package com.zpy.reggie.dto;

import com.zpy.reggie.entity.Setmeal;
import com.zpy.reggie.entity.SetmealDish;
import lombok.Data;

import java.util.List;

@Data
public class SetmealDto extends Setmeal {

    private List<SetmealDish> setmealDishes;

    private String categoryName;
}
