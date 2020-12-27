package com.changgou.order.dao;

import com.changgou.order.pojo.Order;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

public interface OrderMapper extends Mapper<Order> {
    //mybatis
    //通用mapper  ---帮你检查CRUD开发
    @Select("select * from tb_order where id = #{majorOrders}")
    Order findByOrderId(int majorOrders);
}
