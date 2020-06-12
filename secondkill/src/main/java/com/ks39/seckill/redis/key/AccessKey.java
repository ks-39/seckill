package com.ks39.seckill.redis.key;

public class AccessKey extends BasePrefix{

	private AccessKey(int expireSeconds, String prefix) {
		super(expireSeconds, prefix);
	}

	//存活时间为添加注解时设置
	public static AccessKey withExpire(int expireSeconds) {
		return new AccessKey(expireSeconds, "access");
	}
}
