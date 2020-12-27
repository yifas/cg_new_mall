package com.changgou.order.dao;

import com.changgou.order.pojo.Order;
import com.changgou.order.pojo.OrderItem;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface OrderItemMapper extends Mapper<OrderItem> {

    @Select("select * from tb_order_item where order_id  = #{majorOrders}")
    List<OrderItem> findByOrderId(int majorOrders);
}
