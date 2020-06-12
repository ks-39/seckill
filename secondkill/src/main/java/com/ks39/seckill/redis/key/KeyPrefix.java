package com.ks39.seckill.redis.key;

public interface KeyPrefix {
		
	public int expireSeconds();
	
	public String getPrefix();
	
}
