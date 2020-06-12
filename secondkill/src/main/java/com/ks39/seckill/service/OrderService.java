package com.ks39.seckill.service;


import com.ks39.seckill.dao.OrderDao;
import com.ks39.seckill.domain.MiaoshaOrder;
import com.ks39.seckill.domain.MiaoshaUser;
import com.ks39.seckill.domain.dto.GoodsVo;
import com.ks39.seckill.redis.Utils.RedisService;
import com.ks39.seckill.redis.key.OrderKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class OrderService {
	
	@Autowired
	OrderDao orderDao;
	
	@Autowired
	RedisService redisService;
	
	public MiaoshaOrder getMiaoshaOrderByUserIdGoodsId(long userId, long goodsId) {
		return redisService.get(OrderKey.getMiaoshaOrderByUidGid, "" + userId + "_" + goodsId, MiaoshaOrder.class);
	}
	
	public MiaoshaOrder getOrderById(long orderId) {
		return orderDao.getOrderById(orderId);
	}

	@Transactional
	public MiaoshaOrder createOrder(MiaoshaUser user, GoodsVo goods) {
		MiaoshaOrder miaoshaOrder = new MiaoshaOrder();
		miaoshaOrder.setGoodsId(goods.getId());
		miaoshaOrder.setUserId(user.getId());
		orderDao.insertMiaoshaOrder(miaoshaOrder);

		miaoshaOrder = orderDao.getMiaoshaOrderByUserIdGoodsId(miaoshaOrder.getUserId(), miaoshaOrder.getGoodsId());
		System.out.println("新增订单"+miaoshaOrder);

		redisService.set(OrderKey.getMiaoshaOrderByUidGid, ""+user.getId()+"_"+goods.getId(), miaoshaOrder);
		return miaoshaOrder;
	}

	public void deleteOrders() {
		orderDao.deleteMiaoshaOrders();
	}

}
