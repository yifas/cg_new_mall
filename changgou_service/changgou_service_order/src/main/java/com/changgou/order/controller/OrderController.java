package com.changgou.order.controller;
import com.changgou.entity.PageResult;
import com.changgou.entity.Result;
import com.changgou.entity.StatusCode;
import com.changgou.order.service.OrderService;
import com.changgou.order.pojo.Order;
import com.github.pagehelper.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
@RestController
@CrossOrigin
@RequestMapping("/order")
public class OrderController {


    @Autowired
    private OrderService orderService;

    /**
     * 查询全部数据
     * @return
     */
    @GetMapping
    public Result findAll(){
        List<Order> orderList = orderService.findAll();
        return new Result(true, StatusCode.OK,"查询成功",orderList) ;
    }

    /***
     * 根据ID查询数据
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result findById(@PathVariable String id){
        Order order = orderService.findById(id);
        return new Result(true,StatusCode.OK,"查询成功",order);
    }


    /***
     * 新增数据
     * @param order
     * @return
     */
    @PostMapping
    public Result add(@RequestBody Order order){
        orderService.add(order);
        return new Result(true,StatusCode.OK,"添加成功");
    }


    /***
     * 修改数据
     * @param order
     * @param id
     * @return
     */
    @PutMapping(value="/{id}")
    public Result update(@RequestBody Order order,@PathVariable String id){
        order.setId(id);
        orderService.update(order);
        return new Result(true,StatusCode.OK,"修改成功");
    }


    /***
     * 根据ID删除品牌数据
     * @param id
     * @return
     */
    @DeleteMapping(value = "/{id}" )
    public Result delete(@RequestParam(required = true) String id){
        orderService.delete(id);
        return new Result(true,StatusCode.OK,"删除成功");
    }

    /***
     * 多条件搜索品牌数据
     * @param searchMap
     * @return
     */
    @GetMapping(value = "/search" )
    public Result findList(@RequestParam Map searchMap){
        List<Order> list = orderService.findList(searchMap);
        return new Result(true,StatusCode.OK,"查询成功",list);
    }


    /***
     * 分页搜索实现
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    @GetMapping(value = "/search/{page}/{size}" )
    public Result findPage(@RequestParam Map searchMap, @PathVariable  int page, @PathVariable  int size){
        Page<Order> pageList = orderService.findPage(searchMap, page, size);
        PageResult pageResult=new PageResult(pageList.getTotal(),pageList.getResult());
        return new Result(true,StatusCode.OK,"查询成功",pageResult);
    }


    //合并订单
    /**
     * 合并订单
     *
     * @param mainOrder
     * @param subOrder
     * @return
     */
    @PutMapping("/mergeOrder/{mainOrderId}/{subOrderId}")
    public Result mergeOrder(@PathVariable("mainorder") String mainOrder, @PathVariable("suborder") String subOrder) {
        try {
            orderService.mergeOrder(mainOrder, subOrder);
            return new Result(true, StatusCode.OK, "订单合并成功");
        } catch (RuntimeException e) {
            return new Result(false, StatusCode.ERROR, e.getMessage());
        }
    }

    /**
     * 确认收货展示所有的未收货的订单
     * "/findByStatus"    localhost:9001/order?pageNum=1&pageSize=20  传统方式
     * /findByStatus/{pageNum}/{pageSize}    localhost:9001/findByStatus/1/20   restful风格
     */
    /**
     *
     * @param map  查询条件{id:"",name:"",xxxxxx}
     * @param pageNum
     * @param pageSize
     * @return
     * 如果需要接收一个Json的参数，转换为对象，那么需要在当前转换对象前面加上一个 @RequestBody
     * @ResponseBody  响应数据转JSON
     * @RequestBody 接收数据转JSON
     */
    @GetMapping("/findByStatus/{pageNum}/{pageSize}")
    @ResponseBody
    public Result findByStatus(@RequestBody Map map,@PathVariable(name ="pageNum") int pageNum,@PathVariable(name ="pageSize")int pageSize){
        Page<Order> orderPage = orderService.findByStatus(map,pageNum,pageSize);
        return new Result(true,StatusCode.OK,"查询成功",new PageResult<Order>(orderPage.getTotal(),orderPage.getResult()));
    }


    @GetMapping("/test")
    public String test1(){
        System.out.println("GET请求");
        return "GET请求";
    }
    @PostMapping("/test")
    public String test2(){
        System.out.println("POST请求");
        return "POST请求";
    }
    @PutMapping("/test")
    public String test3(){
        System.out.println("PUT请求");
        return "PUT请求";
    }
    @DeleteMapping("/test")
    public String test4(){
        System.out.println("DELEETE请求");
        return "DELEETE请求";
    }


    /**
     * GetMapping  --- 查询功能
     * POSTMapping ---- 新增功能
     * PUTMapping --- 修改功能
     * DELETETMapping --- 删除功能
     */
    @PutMapping("/mergeOrder2/{majorOrders}/{fromOrder}")
    public Result mergeOrder2(@PathVariable("majorOrders") int majorOrders ,@PathVariable("fromOrder")int fromOrder ){

        boolean flag = orderService.mergeOrder2(majorOrders,fromOrder);

        return null;
    }



}
