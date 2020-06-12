package com.ks39.seckill.redis.key;

//UserKey：用于将user存入redis缓存
public class MiaoshaUserKey extends BasePrefix{

	//1. 设定key的存活时间为2天
	public static final int TOKEN_EXPIRE = 3600*24 * 2;
	private MiaoshaUserKey(int expireSeconds, String prefix) {
		super(expireSeconds, prefix);
	}
	//2. 初始化token前缀为tk
	public static MiaoshaUserKey token = new MiaoshaUserKey(TOKEN_EXPIRE, "tk");
	//3. 初始化user前缀为id
	public static MiaoshaUserKey getById = new MiaoshaUserKey(0, "id");
}
