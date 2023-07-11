package com.zpy.reggie.dto;

import com.zpy.reggie.entity.OrderDetail;
import com.zpy.reggie.entity.Orders;
import lombok.Data;
import java.util.List;

@Data
public class OrdersDto extends Orders {

    private String userName;

    private String phone;

    private String address;

    private String consignee;

    private List<OrderDetail> orderDetails;
	
}
