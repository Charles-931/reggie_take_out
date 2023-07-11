package com.zpy.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zpy.reggie.common.R;
import com.zpy.reggie.dto.OrdersDto;
import com.zpy.reggie.entity.OrderDetail;
import com.zpy.reggie.entity.Orders;
import com.zpy.reggie.service.OrderDetailService;
import com.zpy.reggie.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    /**
     * 用户下单
     * @param orders
     * @return
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders) {
        log.info("订单数据: {}", orders);
        orderService.submit(orders);
        return R.success("下单成功");
    }

    /**
     * 获取用户最近订单详细信息
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/userPage")
    public R<Page<OrdersDto>> userPage(Integer page, Integer pageSize, HttpSession session) {
        Page<Orders> ordersPage = new Page<>(page, pageSize);
        Page<OrdersDto> ordersDtoPage = new Page<>();
        // 获取用户id
        Long userId = (Long) session.getAttribute("user");
        // 查询用户最近订单
        LambdaQueryWrapper<Orders> ordersLambdaQueryWrapper = new LambdaQueryWrapper<>();
        ordersLambdaQueryWrapper.eq(Orders::getUserId, userId);
        ordersLambdaQueryWrapper.orderByDesc(Orders::getOrderTime);
        orderService.page(ordersPage, ordersLambdaQueryWrapper);

        // 将ordersPage中的信息 除records外 全部拷贝到 ordersDtoPage
        BeanUtils.copyProperties(ordersPage, ordersDtoPage, "records");

        // 获取最近一个订单的订单号
        String orderNumber = ordersPage.getRecords().get(0).getNumber();

        // 根据订单号查询该订单明细
        LambdaQueryWrapper<OrderDetail> orderDetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
        orderDetailLambdaQueryWrapper.eq(OrderDetail::getOrderId, orderNumber);
        List<OrderDetail> orderDetailList = orderDetailService.list(orderDetailLambdaQueryWrapper);

        List<OrdersDto> records = ordersPage.getRecords().stream().map((item) -> {
            OrdersDto ordersDto = new OrdersDto();
            BeanUtils.copyProperties(item, ordersDto);
            ordersDto.setOrderDetails(orderDetailList);
            return ordersDto;
        }).collect(Collectors.toList());

        ordersDtoPage.setRecords(records);
        return R.success(ordersDtoPage);
    }
}
