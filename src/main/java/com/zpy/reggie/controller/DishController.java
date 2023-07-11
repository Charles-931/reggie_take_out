package com.zpy.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zpy.reggie.common.CustomException;
import com.zpy.reggie.common.R;
import com.zpy.reggie.dto.DishDto;
import com.zpy.reggie.entity.*;
import com.zpy.reggie.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private SetmealService setmealService;

    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto){
        log.info("新增菜品信息:{}",dishDto);
        dishService.saveWithFlavor(dishDto);

        //        // 清理所有菜品的缓存数据
//        Set keys = redisTemplate.keys("dish_*");
//        redisTemplate.delete(keys);

        // 清理某个分类下面的菜品缓存数据
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        log.info("清理redis中的缓存:{}", key);
        redisTemplate.delete(key);

        return R.success("新增菜品成功");
    }

    /**
     * 菜品分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page<DishDto>> page(Integer page, Integer pageSize, String name) {
        // 构造分页构造器对象
        Page<Dish> pageInfo = new Page<>(page, pageSize);
        Page<DishDto> dishDtoPage = new Page<>();

        // 条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        // 添加过滤条件
        queryWrapper.like(StringUtils.isNotEmpty(name), Dish::getName, name);
        //添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        // 执行分页查询
        dishService.page(pageInfo, queryWrapper);

        // 对象拷贝
        BeanUtils.copyProperties(pageInfo, dishDtoPage, "records");

        // 根据分类id查询对应的分类名
        LambdaQueryWrapper<Category> categoryLambdaQueryWrapper = new LambdaQueryWrapper<>();
        List<Dish> records = pageInfo.getRecords();
        List<DishDto> list = records.stream().map((item) -> {
            DishDto dishDto = new DishDto();

            BeanUtils.copyProperties(item, dishDto);
            // 根据分类id 查询分类对象
            Category category = categoryService.getById(item.getCategoryId());
            if (category != null) {
                String categoryName = category.getName();

                dishDto.setCategoryName(categoryName);
            }

            return dishDto;
        }).collect(Collectors.toList());

        dishDtoPage.setRecords(list);
        return R.success(dishDtoPage);
    }

    /**
     * 根据id 查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id) {
        DishDto dishDto = dishService.getByIdWithFlavor(id);
        return R.success(dishDto);
    }

    /**
     * 修改菜品
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto){
        log.info("新增菜品信息:{}",dishDto);
        dishService.updateWithFlavor(dishDto);

//        // 清理所有菜品的缓存数据
//        Set keys = redisTemplate.keys("dish_*");
//        redisTemplate.delete(keys);

        // 清理某个分类下面的菜品缓存数据
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        log.info("清理redis中的缓存:{}", key);
        redisTemplate.delete(key);

        return R.success("新增菜品成功");
    }

//    /**
//     * 根据条件查询对应的菜品数据
//     * @param dish
//     * @return
//     */
//    @GetMapping("/list")
//    public R<List<Dish>> list(Dish dish) {
//        // 构造查询条件
//        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
//        // 添加条件, 查询状态为1(起售状态)的菜品
//        queryWrapper.eq(Dish::getStatus, 1);
//        //添加排序条件
//        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
//
//        List<Dish> dishList = dishService.list(queryWrapper);
//        return R.success(dishList);
//    }

    /**
     * 根据条件查询对应的菜品数据
     * @param dish
     * @return
     */
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish) {
        // 初始化返回结果
        List<DishDto> dishDtoList = null;
        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();
        // 先从Redis中获取缓存数据
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);

        // 如果存在, 直接返回, 无需查询数据库
        if (dishDtoList != null) {
            return R.success(dishDtoList);
        }

        // 如果不存在, 需要查询数据库
        // 构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
        // 添加条件, 查询状态为1(起售状态)的菜品
        queryWrapper.eq(Dish::getStatus, 1);
        //添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> dishList = dishService.list(queryWrapper);

        dishDtoList = dishList.stream().map((item) -> {
            // dish信息拷贝到dishDto
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);

            // 分类id
            Long categoryId = item.getCategoryId();
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }

            // 菜品id
            Long dishId = item.getId();
            // 根据dish的id查询相关联的口味dishFlavors
            LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = new LambdaQueryWrapper<>();
            dishFlavorLambdaQueryWrapper.eq(DishFlavor::getDishId, dishId);
            List<DishFlavor> flavors = dishFlavorService.list(dishFlavorLambdaQueryWrapper);
            dishDto.setFlavors(flavors);

            return dishDto;
        }).collect(Collectors.toList());

        // 将查询到的菜品数据缓存到Redis
        redisTemplate.opsForValue().set(key, dishDtoList, 60, TimeUnit.MINUTES);

        return R.success(dishDtoList);
    }

    /**
     * 根据id修改菜品起/停售状态
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    @Transactional
    @CacheEvict(value = "setmealCache", condition = "#status == 0", allEntries = true)
    public R<String> status(@PathVariable Integer status, @RequestParam List<Long> ids){
        log.info("需要将菜品: {} 的状态修改为 {}", ids, status);
        // 遍历ids
        for (Long id : ids) {
            // 首先将该菜品的状态设为停售/起售
            Dish newDish = dishService.getById(id);
            if (newDish == null) throw new CustomException("删除失败!");
            newDish.setStatus(status);
            dishService.updateById(newDish);

            // 删除该菜品分类下的菜品缓存
            String key = "dish_" + newDish.getCategoryId() + "_1";
            log.info("清理redis中的缓存:{}", key);
            redisTemplate.delete(key);

            // 如果是将菜品状态设为停售, 则将包含该菜品的套餐状态也设为停售
            if (status == 0) {
                LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(SetmealDish::getDishId, newDish.getId());
                List<SetmealDish> setmealDishList = setmealDishService.list(queryWrapper);
                for (SetmealDish setmealDish : setmealDishList) {
                    Long setmealId = setmealDish.getSetmealId();
                    Setmeal setmeal = setmealService.getById(setmealId);
                    setmeal.setStatus(status);
                    setmealService.updateById(setmeal);
                }
            }
        }

        return R.success("菜品状态修改成功");

    }

    /**
     * 根据ids批量删除/单个删除菜品信息
     * @param ids
     * @return
     */
    @DeleteMapping
    @Transactional
    public R<String> delete(@RequestParam List<Long> ids) {
        log.info("要删除的菜品id为: {}", ids);
        // 1. 遍历菜品id, 查询是否有套餐关联某一菜品
        for (Long id : ids) {
            // 如果有一个菜品有关联的套餐, 则使用事务管理的删除方法一个菜品信息也不删除
            LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SetmealDish::getDishId, id);
            List<SetmealDish> list = setmealDishService.list(queryWrapper);
            if (list.size() > 0) {
                throw new CustomException("该菜品有关联的套餐, 无法删除");
            }
        }

        // 2. 所有待删除的菜品均未关联套餐, 那么就将这些菜品从数据库中删除
        dishService.removeByIds(ids);
        // 3. 同时将所有菜品的分类缓存信息从redis中删除
        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);
        return R.success("菜品信息删除成功");
    }
}
