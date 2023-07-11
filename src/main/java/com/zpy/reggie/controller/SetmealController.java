package com.zpy.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zpy.reggie.common.CustomException;
import com.zpy.reggie.common.R;
import com.zpy.reggie.dto.SetmealDto;
import com.zpy.reggie.entity.Category;
import com.zpy.reggie.entity.Dish;
import com.zpy.reggie.entity.Setmeal;
import com.zpy.reggie.entity.SetmealDish;
import com.zpy.reggie.service.CategoryService;
import com.zpy.reggie.service.DishService;
import com.zpy.reggie.service.SetmealDishService;
import com.zpy.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;


/**
 * 套餐管理
 */
@RestController
@RequestMapping("/setmeal")
@Slf4j
public class SetmealController {

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SetmealDishService setmealDishService;

    /**
     * 新增套餐信息
     * @param setmealDto
     * @return
     */
    @PostMapping
    @CacheEvict(value = "setmealCache", allEntries = true)
    @Transactional
    public R<String> save(@RequestBody SetmealDto setmealDto){
        log.info("新增套餐信息: {}", setmealDto);
        setmealService.saveWithDish(setmealDto);
        return R.success("新增套餐成功");
    }

    /**
     * 套餐分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page<SetmealDto>> page(Integer page, Integer pageSize, String name) {
        // 分页构造器对象
        Page<Setmeal> setmealPage = new Page<>(page, pageSize);

        // 查询条件构造器
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        // 根据name进行模糊查询
        queryWrapper.like(StringUtils.isNotEmpty(name), Setmeal::getName, name);
        // 根据更新时间降序排列
        queryWrapper.orderByDesc(Setmeal::getStatus).orderByDesc(Setmeal::getUpdateTime);
        setmealService.page(setmealPage, queryWrapper);

        // 得到包含分类名称的Dto的page
        Page<SetmealDto> setmealDtoPage = new Page<>();
        // 对象拷贝
        BeanUtils.copyProperties(setmealPage, setmealDtoPage, "records");
        List<Setmeal> records = setmealPage.getRecords();

        List<SetmealDto> newRecords = records.stream().map((item) -> {
            // 将没有包含分类名称的部分信息拷贝给结果集
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(item, setmealDto);
            // 分类id
            Long categoryId = item.getCategoryId();
            // 根据分类id查询分类对象
            Category category = categoryService.getById(categoryId);
            // 设置分类名称
            String categoryName = category.getName();
            setmealDto.setCategoryName(categoryName);
            return setmealDto;
        }).collect(Collectors.toList());

        setmealDtoPage.setRecords(newRecords);
        return R.success(setmealDtoPage);
    }

    /**
     * 删除套餐
     * @param ids
     * @return
     */
    @DeleteMapping
    @CacheEvict(value = "setmealCache", allEntries = true)
    public R<String> delete(@RequestParam List<Long> ids) {
        log.info("待删除的套餐id: {}", ids);
        setmealService.removeWithDish(ids);
        return R.success("套餐删除成功");
    }

    /**
     * 根据条件查询套餐数据
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    @Cacheable(value = "setmealCache", unless = "#setmeal.categoryId == null", key = "#setmeal.categoryId + '_' + #setmeal.status")
    public R<List<Setmeal>> list(Setmeal setmeal) {
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId());
        queryWrapper.eq(setmeal.getStatus() != null, Setmeal::getStatus, setmeal.getStatus());
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        List<Setmeal> setmealList = setmealService.list(queryWrapper);
        return R.success(setmealList);
    }

    /**
     * 根据ids批量/单个修改套餐起/停售状态
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    @CacheEvict(value = "setmealCache", allEntries = true)
    public R<String> status(@PathVariable Integer status, @RequestParam List<Long> ids) {
        // 1. 如果要修改成的状态为停售, 则直接修改套餐的状态, 见 5.

        // 2. 如果要修改成的状态为起售, 遍历每个待修改状态的套餐id
        if (status == 1) {
            for (Long id : ids) {
                // 3. 根据套餐id查询对应的菜品列表
                LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(SetmealDish::getSetmealId, id);
                List<SetmealDish> setmealDishList = setmealDishService.list(queryWrapper);
                // 4. 如果存在菜品的状态为停售状态, 则修改失败, 抛出业务异常
                for (SetmealDish setmealDish : setmealDishList) {
                    Long dishId = setmealDish.getDishId();
                    Dish dish = dishService.getById(dishId);
                    if (dish.getStatus() == 0) {
                        throw new CustomException("套餐中存在停售的菜品, 套餐起售失败");
                    }
                }
            }
        }

        List<Setmeal> setmealList = setmealService.listByIds(ids);
        for (Setmeal setmeal : setmealList) {
            setmeal.setStatus(status);
        }
        // 5. 修改套餐的状态
        setmealService.updateBatchById(setmealList);
        // 6. 删除redis中关于套餐分类的缓存数据, 见方法上的CacheEvict注解 (清理所有的套餐缓存)

        return R.success("修改套餐状态成功");
    }

}
