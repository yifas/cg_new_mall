package com.changgou.order.service.impl;

import com.changgou.order.dao.OrderItemMapper;
import com.changgou.order.dao.OrderMapper;
import com.changgou.order.pojo.OrderItem;
import com.changgou.order.service.OrderService;
import com.changgou.order.pojo.Order;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 查询全部列表
     * @return
     */
    @Override
    public List<Order> findAll() {
        return orderMapper.selectAll();
    }

    /**
     * 根据ID查询
     * @param id
     * @return
     */
    @Override
    public Order findById(String id){
        return  orderMapper.selectByPrimaryKey(id);
    }


    /**
     * 增加
     * @param order
     */
    @Override
    public void add(Order order){
        orderMapper.insert(order);
    }


    /**
     * 修改
     * @param order
     */
    @Override
    public void update(Order order){
        orderMapper.updateByPrimaryKey(order);
    }

    /**
     * 删除
     * @param id
     */
    @Override
    public void delete(String id){
        orderMapper.deleteByPrimaryKey(id);
    }


    /**
     * 条件查询
     * @param searchMap
     * @return
     */
    @Override
    public List<Order> findList(Map<String, Object> searchMap){
        Example example = createExample(searchMap);
        return orderMapper.selectByExample(example);
    }

    /**
     * 分页查询
     * @param page
     * @param size
     * @return
     */
    @Override
    public Page<Order> findPage(int page, int size){
        PageHelper.startPage(page,size);
        return (Page<Order>)orderMapper.selectAll();
    }

    /**
     * 条件+分页查询
     * @param searchMap 查询条件
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    @Override
    public Page<Order> findPage(Map<String,Object> searchMap, int page, int size){
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        return (Page<Order>)orderMapper.selectByExample(example);
    }

    @Autowired
    private OrderItemMapper orderItemMapper;

    /**
     * 合并订单业务实现
     * @param mainOrder  主订单，
     * @param subOrder  从订单
     */
    @Override
    @Transactional
    public void mergeOrder(String mainOrder, String subOrder) {
        //1.查询主订单信息
        Order order1 = orderMapper.selectByPrimaryKey(mainOrder);
        //2.查询从订单信息
        Order order2 = orderMapper.selectByPrimaryKey(subOrder);
        //业务判断省略
        //3.根据订单ID查询主订单明细
        Example ex1 = new Example(OrderItem.class);
        Example.Criteria criteria = ex1.createCriteria();
        criteria.andEqualTo("orderId",mainOrder);
        List<OrderItem> orderItems1 = orderItemMapper.selectByExample(ex1);
        CopyOnWriteArrayList<OrderItem> mainOrderItem = new CopyOnWriteArrayList(orderItems1);
        //4.根据订单ID查询从订单明细
        Example ex2 = new Example(OrderItem.class);
        Example.Criteria criteria2 = ex2.createCriteria();
        criteria2.andEqualTo("orderId",mainOrder);
        List<OrderItem> orderItems2 = orderItemMapper.selectByExample(ex2);
        CopyOnWriteArrayList<OrderItem> subOrderItem = new CopyOnWriteArrayList(orderItems2);
        //遍历订单明细查询订单是否相等
        for(OrderItem orderItemMain:mainOrderItem){   //   1   2（合并）  3    4      5
            for (OrderItem orderItemSub:subOrderItem){//
                //判断订单是否相等，如果相等，合并数量以及金额
                //如果sku相等，代表是同一件商品，那么需要合并数量和金额
                if(orderItemMain.getSkuId().equals(orderItemSub.getSkuId())){
                    orderItemMain.setNum(orderItemMain.getNum()+orderItemSub.getNum());
                    orderItemMain.setPayMoney(orderItemMain.getPayMoney()+orderItemSub.getPayMoney());
                    //..........参数合并。
                    //如果合并成功，代表此次循环找到的相同的商品。那么删除从订单该商品。并且跳出循环
                    subOrderItem.remove(orderItemSub);//JUC
                    break;
                }
            }
        }
        //subOrderItem 集合在遍历后最终所剩下的商品 就是主订单明细中不存在的商品信息
        //当循环结束后， subOrderItem 剩余的商品就是orderItemMain 不存在的订单明细。那么更改差值即可
        for(OrderItem orderItemSub:subOrderItem){
            //复制当前bean对象，放入到新数组中，或者更改订单指向都可以（建议使用前者）
            OrderItem orderItem = new OrderItem();
            BeanUtils.copyProperties(orderItemSub,orderItem);//复制一个相同的对象，强引用（深克隆）
            orderItem.setId(null);//清空主键
            orderItem.setOrderId(mainOrder);//更改订单指向
            mainOrderItem.add(orderItem); //添加到胡订单集合中
        }
        //mainOrderItem 所有的数据就已经合并结束
        //遍历mainOrderItem 计算商品总额或者使用主从订单合并金额和数量
        order1.setTotalNum(order1.getTotalNum()+order2.getTotalNum());
        order1.setTotalMoney(order1.getTotalMoney()+order2.getTotalMoney());
        //............其他数据合并省略
        //最后把新的主订单明细数组插入到数据库 选择先删后增原则
        Example ex3 = new Example(OrderItem.class);
        Example.Criteria criteria3 = ex1.createCriteria();
        criteria3.andEqualTo("orderId",mainOrder);
        orderItemMapper.deleteByExample(criteria3);//删除掉原有数据
        //循环新增
        for(OrderItem orderItemMain:mainOrderItem){
            orderItemMapper.insert(orderItemMain);
        }
        //进行从主订单的逻辑删除
        //。。。。。

    }

    @Override
    public Page<Order> findByStatus(Map map, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        //需要进行条件查询
        Example example = new Example(Order.class);
        Example.Criteria criteria = example.createCriteria();
        //查询是状态为已发货状态的所有的数据
        criteria.andEqualTo("consignStatus","1");

        //为当前查询添加条件
        if(map.get("id")!=null &&map.get("id")!=""){//判断一个条件不为空
            criteria.andEqualTo("id",map.get("id"));
        }
        if(map.get("receiverContact")!=null &&map.get("receiverContact")!=""){//判断一个条件不为空
            criteria.andEqualTo("receiverContact",map.get("receiverContact"));
        }
        if(map.get("updateTime")!=null &&map.get("updateTime")!=""){//判断一个条件不为空
            criteria.andEqualTo("updateTime",map.get("updateTime"));
        }
        //mybatis 分页插件 PageHelper
        Page<Order> orders = (Page<Order>) orderMapper.selectByExample(example);//条件查询 ，需要携带一个条件  -- 分页查询

        return orders;
    }

    @Override
    public boolean mergeOrder2(int majorOrders, int fromOrders) {
        //1.查询主订单 和 主订单明细   一堆多
        Order majorOrder = orderMapper.findByOrderId(majorOrders); //主订单
        List<OrderItem> majorOrderItems = orderItemMapper.findByOrderId(majorOrders);
        //2.查询从订单和从订单明细
        Order fromOrder = orderMapper.selectByPrimaryKey(fromOrders);//从订单
        Example example = new Example(OrderItem.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("orderId",fromOrders);
        List<OrderItem> fromOrderItems = orderItemMapper.selectByExample(example);//通用mapper 提供的方法，需要传输一个条件对象
        //3.判断当当前商品是否在主订单中存在
        for(OrderItem cOrderItem:fromOrderItems){   ///3
            for(OrderItem zOrderItem:majorOrderItems){ //3
                if(cOrderItem.getSkuId() == zOrderItem.getSkuId()){//代表是同样的
                    //如果订单商品SKUID一样 ,那么我是不是就可以进行数量的增减
                    //把从订单的商品数量和金额,合并到从订单中
                    zOrderItem.setNum(zOrderItem.getNum() + cOrderItem.getNum());//合并明细数量
                    zOrderItem.setPayMoney(zOrderItem.getPayMoney() + cOrderItem.getPayMoney());//合并金额
                    //.....
                    break;  //当表当前循环已经结束,继续后续的循环

                }else{  //没有找到
                    //改变当前从订单的OrderId 外键的指向,指向主订单
                    cOrderItem.setOrderId(majorOrders+""); //更改明细的指向
                }
            }
        }
        //4.同步当前的数据库数据了.  更新主订单,删除从订单  以后进行商品一堆的操作的时候,
        //那么使用先删后增原则
        //先删
        Example example2 = new Example(OrderItem.class);
        Example.Criteria criteria1 = example2.createCriteria();
        criteria1.andEqualTo("orderId",majorOrders);
        orderMapper.deleteByExample(example2);//delete from tb_orderItem where  order_id = '111111'
        //后增
        for(OrderItem orderItem:majorOrderItems){
            orderItemMapper.insert(orderItem);
        }

        orderItemMapper.deleteByPrimaryKey(fromOrders);


        return false;
    }

    /**
     * 构建查询对象
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Order.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 订单id
            if(searchMap.get("id")!=null && !"".equals(searchMap.get("id"))){
                criteria.andEqualTo("id",searchMap.get("id"));
           	}
            // 支付类型，1、在线支付、0 货到付款
            if(searchMap.get("payType")!=null && !"".equals(searchMap.get("payType"))){
                criteria.andEqualTo("payType",searchMap.get("payType"));
           	}
            // 物流名称
            if(searchMap.get("shippingName")!=null && !"".equals(searchMap.get("shippingName"))){
                criteria.andLike("shippingName","%"+searchMap.get("shippingName")+"%");
           	}
            // 物流单号
            if(searchMap.get("shippingCode")!=null && !"".equals(searchMap.get("shippingCode"))){
                criteria.andLike("shippingCode","%"+searchMap.get("shippingCode")+"%");
           	}
            // 用户名称
            if(searchMap.get("username")!=null && !"".equals(searchMap.get("username"))){
                criteria.andLike("username","%"+searchMap.get("username")+"%");
           	}
            // 买家留言
            if(searchMap.get("buyerMessage")!=null && !"".equals(searchMap.get("buyerMessage"))){
                criteria.andLike("buyerMessage","%"+searchMap.get("buyerMessage")+"%");
           	}
            // 是否评价
            if(searchMap.get("buyerRate")!=null && !"".equals(searchMap.get("buyerRate"))){
                criteria.andLike("buyerRate","%"+searchMap.get("buyerRate")+"%");
           	}
            // 收货人
            if(searchMap.get("receiverContact")!=null && !"".equals(searchMap.get("receiverContact"))){
                criteria.andLike("receiverContact","%"+searchMap.get("receiverContact")+"%");
           	}
            // 收货人手机
            if(searchMap.get("receiverMobile")!=null && !"".equals(searchMap.get("receiverMobile"))){
                criteria.andLike("receiverMobile","%"+searchMap.get("receiverMobile")+"%");
           	}
            // 收货人地址
            if(searchMap.get("receiverAddress")!=null && !"".equals(searchMap.get("receiverAddress"))){
                criteria.andLike("receiverAddress","%"+searchMap.get("receiverAddress")+"%");
           	}
            // 订单来源：1:web，2：app，3：微信公众号，4：微信小程序  5 H5手机页面
            if(searchMap.get("sourceType")!=null && !"".equals(searchMap.get("sourceType"))){
                criteria.andEqualTo("sourceType",searchMap.get("sourceType"));
           	}
            // 交易流水号
            if(searchMap.get("transactionId")!=null && !"".equals(searchMap.get("transactionId"))){
                criteria.andLike("transactionId","%"+searchMap.get("transactionId")+"%");
           	}
            // 订单状态
            if(searchMap.get("orderStatus")!=null && !"".equals(searchMap.get("orderStatus"))){
                criteria.andEqualTo("orderStatus",searchMap.get("orderStatus"));
           	}
            // 支付状态
            if(searchMap.get("payStatus")!=null && !"".equals(searchMap.get("payStatus"))){
                criteria.andEqualTo("payStatus",searchMap.get("payStatus"));
           	}
            // 发货状态
            if(searchMap.get("consignStatus")!=null && !"".equals(searchMap.get("consignStatus"))){
                criteria.andEqualTo("consignStatus",searchMap.get("consignStatus"));
           	}
            // 是否删除
            if(searchMap.get("isDelete")!=null && !"".equals(searchMap.get("isDelete"))){
                criteria.andEqualTo("isDelete",searchMap.get("isDelete"));
           	}

            // 数量合计
            if(searchMap.get("totalNum")!=null ){
                criteria.andEqualTo("totalNum",searchMap.get("totalNum"));
            }
            // 金额合计
            if(searchMap.get("totalMoney")!=null ){
                criteria.andEqualTo("totalMoney",searchMap.get("totalMoney"));
            }
            // 优惠金额
            if(searchMap.get("preMoney")!=null ){
                criteria.andEqualTo("preMoney",searchMap.get("preMoney"));
            }
            // 邮费
            if(searchMap.get("postFee")!=null ){
                criteria.andEqualTo("postFee",searchMap.get("postFee"));
            }
            // 实付金额
            if(searchMap.get("payMoney")!=null ){
                criteria.andEqualTo("payMoney",searchMap.get("payMoney"));
            }

        }
        return example;
    }

}
