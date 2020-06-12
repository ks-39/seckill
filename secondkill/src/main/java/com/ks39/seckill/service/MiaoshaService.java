package com.ks39.seckill.service;

import com.ks39.seckill.domain.MiaoshaOrder;
import com.ks39.seckill.domain.MiaoshaUser;
import com.ks39.seckill.domain.dto.GoodsVo;
import com.ks39.seckill.redis.Utils.RedisService;
import com.ks39.seckill.redis.key.MiaoshaKey;
import com.ks39.seckill.utils.MD5Util;
import com.ks39.seckill.utils.UUIDUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MiaoshaService {
	
	@Autowired
	GoodsService goodsService;
	
	@Autowired
	OrderService orderService;

	@Autowired
	RedisService redisService;

	@Transactional
	public MiaoshaOrder miaosha(MiaoshaUser user, GoodsVo goods) {

		//减库存 下订单 写入秒杀订单
		boolean success = goodsService.reduceStock(goods);

		if(success) {
			return orderService.createOrder(user, goods);
		}else {
			setGoodsOver(goods.getId());
			return null;
		}
	}

	public long getMiaoshaResult(long userId, long goodsId) {

		MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(userId, goodsId);

		System.out.println(order.getId());
		System.out.println(order.getUserId());

		if(order != null) {//秒杀成功，返回订单id
			return order.getId();
		}else {
			boolean isOver = getGoodsOver(goodsId);
			if(isOver) {
				return -1;
			}else {
				return 0;
			}
		}
	}

	private void setGoodsOver(Long goodsId) {
		redisService.set(MiaoshaKey.isGoodsOver, ""+goodsId, true);
	}
	
	private boolean getGoodsOver(long goodsId) {
		return redisService.exists(MiaoshaKey.isGoodsOver, ""+goodsId);
	}
	
	public void reset(List<GoodsVo> goodsList) {
		goodsService.resetStock(goodsList);
		orderService.deleteOrders();
	}

	//redis缓存判断是否需要更新path
	public boolean checkPath(MiaoshaUser user, long goodsId, String path) {
		if(user == null || path == null) {
			return false;
		}
		//提取旧的path
		String pathOld = redisService.get(MiaoshaKey.getMiaoshaPath, ""+user.getId() + "_"+ goodsId, String.class);
		//返回比较结果，根据结果判断path是否有效
		return path.equals(pathOld);
	}

	//生成path并将path存入redis缓存
	public String createMiaoshaPath(MiaoshaUser user, long goodsId) {
		//1. 判断
		if(user == null || goodsId <=0) {
			return null;
		}
		//2. 生成path
		String str = MD5Util.md5(UUIDUtil.uuid()+"123456");
		//3. 将path存入缓存
		redisService.set(MiaoshaKey.getMiaoshaPath, ""+user.getId() + "_"+ goodsId, str);
		return str;
	}
}
