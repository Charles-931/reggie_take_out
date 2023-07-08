package com.zpy.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zpy.reggie.entity.Orders;

public interface OrderService extends IService<Orders> {
    /**
     * 用户下单
     * @param orders
     */
    void submit(Orders orders);
}
