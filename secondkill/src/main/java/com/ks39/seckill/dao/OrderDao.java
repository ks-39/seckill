package com.ks39.seckill.dao;

import com.ks39.seckill.domain.MiaoshaOrder;
import org.apache.ibatis.annotations.*;

@Mapper
public interface OrderDao {
	
	@Select("select * from miaosha_order where user_id=#{userId} and goods_id=#{goodsId}")
	public MiaoshaOrder getMiaoshaOrderByUserIdGoodsId(@Param("userId") long userId, @Param("goodsId") long goodsId);

	@Select("select * from miaosha_order where id = #{orderId}")
	public MiaoshaOrder getOrderById(@Param("orderId") long orderId);

	@Insert("insert into miaosha_order (user_id, goods_id)values(#{userId}, #{goodsId})")
	public int insertMiaoshaOrder(MiaoshaOrder miaoshaOrder);

	@Delete("delete from miaosha_order")
	public void deleteMiaoshaOrders();

}
