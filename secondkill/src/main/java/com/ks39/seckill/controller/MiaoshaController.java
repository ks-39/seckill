package com.ks39.seckill.controller;

import com.ks39.seckill.access.AccessLimit;
import com.ks39.seckill.domain.MiaoshaOrder;
import com.ks39.seckill.domain.MiaoshaUser;
import com.ks39.seckill.domain.dto.GoodsVo;
import com.ks39.seckill.rabbitmq.MQSender;
import com.ks39.seckill.rabbitmq.MiaoshaMessage;
import com.ks39.seckill.redis.Utils.RedisService;
import com.ks39.seckill.redis.key.GoodsKey;
import com.ks39.seckill.redis.key.MiaoshaKey;
import com.ks39.seckill.redis.key.OrderKey;
import com.ks39.seckill.result.CodeMsg;
import com.ks39.seckill.result.Result;
import com.ks39.seckill.service.GoodsService;
import com.ks39.seckill.service.MiaoshaService;
import com.ks39.seckill.service.MiaoshaUserService;
import com.ks39.seckill.service.OrderService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;

@Controller
@RequestMapping("/miaosha")
public class MiaoshaController implements InitializingBean {

	@Autowired
	MiaoshaUserService userService;
	
	@Autowired
	RedisService redisService;
	
	@Autowired
	GoodsService goodsService;
	
	@Autowired
	OrderService orderService;
	
	@Autowired
	MiaoshaService miaoshaService;
	
	@Autowired
	MQSender sender;
	
	private HashMap<Long, Boolean> localOverMap =  new HashMap<Long, Boolean>();
	
	/**
	 * 系统初始化，预加载数据库的数据
	 * */
	public void afterPropertiesSet() throws Exception {
		List<GoodsVo> goodsList = goodsService.listGoodsVo();
		if(goodsList == null) {
			return;
		}
		for(GoodsVo goods : goodsList) {
			redisService.set(GoodsKey.getMiaoshaGoodsStock, ""+goods.getId(), goods.getGoodsStock());
			localOverMap.put(goods.getId(), false);
		}
	}

	
	/**
	 * QPS:1306
	 * 5000 * 10
	 *
	 * QPS: 2114
	 * */
    @RequestMapping(value="/{path}/do_miaosha", method= RequestMethod.POST)
    @ResponseBody
    public Result<Integer> miaosha(Model model, MiaoshaUser user,
								   @RequestParam("goodsId")long goodsId, @PathVariable("path") String path
                                 ) {

		//1. 同步Session
    	model.addAttribute("user", user);
    	if(user == null) {
    		return Result.error(CodeMsg.SESSION_ERROR);
    	}

    	//2. 验证path
    	boolean check = miaoshaService.checkPath(user, goodsId, path);

    	if(!check){
    		return Result.error(CodeMsg.REQUEST_ILLEGAL);
    	}


			//3. 内存标记，减少redis访问
			boolean over = localOverMap.get(goodsId);
			System.out.println(over);        //此处为true，说明当前用户已经抢购过一次，返回
			if (over) {
				return Result.error(CodeMsg.MIAO_SHA_OVER);
			}

			//4. 预减库存
			long stock = redisService.decr(GoodsKey.getMiaoshaGoodsStock, "" + goodsId);//10
			System.out.println("库存：" + stock);
			if (stock < 0) {
				localOverMap.put(goodsId, true);
				return Result.error(CodeMsg.MIAO_SHA_OVER);
			}


    	//5. 判断是否已经秒杀到了
    	MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
    	if(order != null) {
    		return Result.error(CodeMsg.REPEATE_MIAOSHA);
    	}

    	//6. 请求入队
    	MiaoshaMessage mm = new MiaoshaMessage();
    	mm.setUser(user);
    	mm.setGoodsId(goodsId);
    	sender.sendMiaoshaMessage(mm);
		System.out.println("入队成功");
    	return Result.success(0);//排队中
    }
    
    /**
     * orderId：成功
     * -1：秒杀失败
     * 0： 排队中
     * */
    @RequestMapping(value="/result", method= RequestMethod.GET)
    @ResponseBody
    public Result<Long> miaoshaResult(Model model, MiaoshaUser user,
                                      @RequestParam("goodsId")long goodsId) {

    	model.addAttribute("user", user);
    	if(user == null) {
    		return Result.error(CodeMsg.SESSION_ERROR);
    	}
    	long result  =miaoshaService.getMiaoshaResult(user.getId(), goodsId);
    	return Result.success(result);
    }


    //限流
    @AccessLimit(seconds=5, maxCount=5, needLogin=true)
    @RequestMapping(value="/path", method= RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaPath(HttpServletRequest request, MiaoshaUser user,
										 @RequestParam("goodsId")long goodsId) {
		//1. 同步Session
		if(user == null) {
			return Result.error(CodeMsg.SESSION_ERROR);
		}

		//3. 根据user和goodsId生成path
		String path  =miaoshaService.createMiaoshaPath(user, goodsId);
		return Result.success(path);
    }

}
