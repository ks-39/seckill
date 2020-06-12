package com.ks39.seckill.redis.key;

public class GoodsKey extends BasePrefix{

	private GoodsKey(int expireSeconds, String prefix) {
		super(expireSeconds, prefix);
	}
	public static GoodsKey getGoodsList = new GoodsKey(30, "gl");
	public static GoodsKey getGoodsDetail = new GoodsKey(60, "gd");
	public static GoodsKey getMiaoshaGoodsStock= new GoodsKey(0, "gs");
}
