package com.ks39.seckill.service;


import com.ks39.seckill.dao.GoodsDao;
import com.ks39.seckill.domain.MiaoshaGoods;
import com.ks39.seckill.domain.dto.GoodsVo;
import com.ks39.seckill.redis.Utils.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoodsService {
	
	@Autowired
	GoodsDao goodsDao;

	@Autowired
	RedisService redisService;

	//1. goods_list
	public List<GoodsVo> listGoodsVo(){
		return goodsDao.listGoodsVo();
	}

	//2. goods_detail
	public GoodsVo getGoodsVoByGoodsId(long goodsId) {
		return goodsDao.getGoodsVoByGoodsId(goodsId);
	}

	//3. miaosha
	public boolean reduceStock(GoodsVo goods) {
		MiaoshaGoods g = new MiaoshaGoods();
		g.setId(goods.getId());
		int ret = goodsDao.reduceStock(g);
		return ret > 0;
	}

	//4. miaosha
	public void resetStock(List<GoodsVo> goodsList) {
		for(GoodsVo goods : goodsList ) {
			MiaoshaGoods g = new MiaoshaGoods();
			g.setId(goods.getId());
			g.setGoodsStock(goods.getGoodsStock());
			goodsDao.resetStock(g);
		}
	}

}
