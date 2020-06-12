package com.ks39.seckill.access;

import com.ks39.seckill.domain.MiaoshaUser;
import com.ks39.seckill.redis.Utils.RedisService;
import com.ks39.seckill.redis.key.AccessKey;
import com.ks39.seckill.service.MiaoshaUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service
public class AccessInterceptor  extends HandlerInterceptorAdapter {
	
	@Autowired
	MiaoshaUserService userService;
	
	@Autowired
	RedisService redisService;
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		//当注解是作用在方法上，才会执行以下代码
		if(handler instanceof HandlerMethod) {
			//1. 通过getUser方法()创建User对象
			MiaoshaUser user = getUser(request, response);
			//2. 保存用户，通过ThreadLocal将当前用户Set，key为user，value为当前线程
			// 在多线程情况下，实现线程安全
			UserContext.setUser(user);
			//3. 创建handler对象
			HandlerMethod hm = (HandlerMethod)handler;
			//4. 获取添加了注解的方法
			AccessLimit accessLimit = hm.getMethodAnnotation(AccessLimit.class);
			if(accessLimit == null) {
				return true;
			}
			int seconds = accessLimit.seconds();
			int maxCount = accessLimit.maxCount();
			boolean needLogin = accessLimit.needLogin();
			//获取URI
			String key = request.getRequestURI();
			//5. 如果需要登陆，重新登陆
			if(needLogin) {
				if(user == null) {
					return false;
				}
				//key拼接userid
				key += "_" + user.getId();
			}else {
				//do nothing
			}
			//6. 生成AccessKey
			AccessKey ak = AccessKey.withExpire(seconds);
			//7. 先判断缓存中剩余的accesskey
			Integer count = redisService.get(ak, key, Integer.class);
			//8. 如果count为空，说明缓存过期或者是第一次请求，设置maxcount为1
	    	if(count  == null) {
	    		 redisService.set(ak, key, 1);
	    	//9. 如果count小于maxCount，缓存继续存入
	    	}else if(count < maxCount) {
	    		 redisService.incr(ak, key);
	    	}else {
	    		return false;
	    	}
		}
		return true;
	}


	//同步Session，获取token
	private MiaoshaUser getUser(HttpServletRequest request, HttpServletResponse response) {
		//1. 获取参数中的token
		String paramToken = request.getParameter(MiaoshaUserService.COOKI_NAME_TOKEN);
		//2. 获取Cookie中的token
		String cookieToken = getCookieValue(request, MiaoshaUserService.COOKI_NAME_TOKEN);
		//3. 如果两个token都为空，返回null
		if(StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)) {
			return null;
		}
		//4. 否则提取不为空的token
		String token = StringUtils.isEmpty(paramToken)?cookieToken:paramToken;
		//5. 传入token，获取token
		return userService.getByToken(response, token);
	}

	//获取cookie
	private String getCookieValue(HttpServletRequest request, String cookiName) {
		Cookie[]  cookies = request.getCookies();
		if(cookies == null || cookies.length <= 0){
			return null;
		}
		for(Cookie cookie : cookies) {
			if(cookie.getName().equals(cookiName)) {
				return cookie.getValue();
			}
		}
		return null;
	}
}
