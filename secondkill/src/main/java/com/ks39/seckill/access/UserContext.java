package com.ks39.seckill.access;

import com.ks39.seckill.domain.MiaoshaUser;

public class UserContext {

	//1. 为user绑定threadlocal，key为user，value为当前线程
	private static ThreadLocal<MiaoshaUser> userHolder = new ThreadLocal<MiaoshaUser>();
	
	public static void setUser(MiaoshaUser user) {
		userHolder.set(user);
	}
	
	public static MiaoshaUser getUser() {
		return userHolder.get();
	}

}
