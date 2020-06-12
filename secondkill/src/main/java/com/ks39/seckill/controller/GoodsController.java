package com.ks39.seckill.controller;


import com.ks39.seckill.domain.MiaoshaUser;
import com.ks39.seckill.domain.dto.GoodsVo;
import com.ks39.seckill.redis.Utils.RedisService;
import com.ks39.seckill.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/goods")
public class GoodsController {

	@Autowired
	RedisService redisService;
	
	@Autowired
	GoodsService goodsService;
	
	/**
	 * QPS:
	 * 5000 * 10
	 * QPS:
	 * */
	@RequestMapping(value="/to_list")
	public String list(Model model, MiaoshaUser user) {
		//1. 同步分布式Session，将user传入
		model.addAttribute("user", user);

		List<GoodsVo> goodsList = goodsService.listGoodsVo();
		model.addAttribute("goodsList", goodsList);
		return "goods_list";
	}

	/**
	 * QPS:
	 * 5000 * 10
	 * QPS:
	 * */
	@RequestMapping(value="/detail/{goodsId}")
	public String detail2(Model model,MiaoshaUser user,
						  @PathVariable("goodsId")long goodsId) {
		//1. 同步Session
		model.addAttribute("user", user);

		//2. 再查询数据库
		GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
		model.addAttribute("goods", goods);

		//3. 计算秒杀倒计时
		long startAt = goods.getStartDate().getTime();
		long endAt = goods.getEndDate().getTime();
		long now = System.currentTimeMillis();

		int miaoshaStatus = 0;
		int remainSeconds = 0;
		if(now < startAt ) {//秒杀还没开始，倒计时
			miaoshaStatus = 0;
			remainSeconds = (int)((startAt - now )/1000);
		}else  if(now > endAt){//秒杀已经结束
			miaoshaStatus = 2;
			remainSeconds = -1;
		}else {//秒杀进行中
			miaoshaStatus = 1;
			remainSeconds = 0;
		}

		//5. 添加属性
		model.addAttribute("miaoshaStatus", miaoshaStatus);
		model.addAttribute("remainSeconds", remainSeconds);

		//7. 返回html代码
		return "goods_detail";
	}

}
