package com.ks39.seckill.service;

import com.ks39.seckill.dao.MiaoshaUserDao;
import com.ks39.seckill.domain.MiaoshaUser;
import com.ks39.seckill.domain.dto.LoginVo;
import com.ks39.seckill.exception.GlobalException;
import com.ks39.seckill.redis.Utils.RedisService;
import com.ks39.seckill.redis.key.MiaoshaUserKey;
import com.ks39.seckill.result.CodeMsg;
import com.ks39.seckill.utils.UUIDUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.pam.UnsupportedTokenException;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

@Service
public class MiaoshaUserService {
	//token前缀常量
	public static final String COOKI_NAME_TOKEN = "token";
	@Autowired
	MiaoshaUserDao miaoshaUserDao;
	
	@Autowired
	RedisService redisService;

	//1. 登录方法(使用Shiro校验)
	public String login(HttpServletResponse response, LoginVo loginVo) {

		//1. 如果封装的参数为空，返回服务器发生错误
		if(loginVo == null) {
			throw new GlobalException(CodeMsg.SERVER_ERROR);
		}

		//3. 获取Subject对象
		Subject subject = SecurityUtils.getSubject();
		//4. Token封装前端数据
		UsernamePasswordToken token = new UsernamePasswordToken(loginVo.getUsername(),loginVo.getPassword());
		System.out.println(token);
		//5. subject执行login方法，进行校验
		try{
			subject.login(token);
		}catch (UnsupportedTokenException e){
			//如果用户名不匹配，返回用户名错误
			throw new GlobalException(CodeMsg.USERNAME_ERROR);
		}catch (IncorrectCredentialsException e){
			//如果密码不匹配，返回密码错误
			throw new GlobalException(CodeMsg.PASSWORD_ERROR);
		}

		//6. 生成cookie，token存入redis
		MiaoshaUser user = getById(Long.parseLong(loginVo.getUsername()));
		String tokenCheck = UUIDUtil.uuid();
		addCookie(response, tokenCheck, user);
		return tokenCheck;
	}

	//2. 先查询缓存，再查询数据库
	public MiaoshaUser getById(long id) {

		//1. 先查询缓存
		MiaoshaUser user = redisService.get(MiaoshaUserKey.getById, ""+id, MiaoshaUser.class);
		if(user != null) {
			//如果缓存中有，直接返回查询结果
			return user;
		}

		//2. 如果缓存中没有，查询数据库
		user = miaoshaUserDao.getById(id);
		if(user != null) {
			//将查询结果存入缓存
			redisService.set(MiaoshaUserKey.getById, ""+id, user);
		}
		return user;
	}

	//3. 生成cookie，将cookie存入缓存
	private void addCookie(HttpServletResponse response, String token, MiaoshaUser user) {
		//1. 将token存入缓存
		redisService.set(MiaoshaUserKey.token, token, user);
		//2. 生成cookie
		Cookie cookie = new Cookie(COOKI_NAME_TOKEN, token);
		cookie.setMaxAge(MiaoshaUserKey.token.expireSeconds());
		cookie.setPath("/");
		//3. 将cookie存入response
		response.addCookie(cookie);
	}

	public MiaoshaUser getByToken(HttpServletResponse response, String token) {
		if(StringUtils.isEmpty(token)) {
			return null;
		}
		MiaoshaUser user = redisService.get(MiaoshaUserKey.token, token, MiaoshaUser.class);
		//延长有效期
		if(user != null) {
			addCookie(response, token, user);
		}
		return user;
	}
}
