package com.ks39.seckill.shiro;


import com.ks39.seckill.dao.MiaoshaUserDao;
import com.ks39.seckill.domain.MiaoshaUser;
import com.ks39.seckill.exception.GlobalException;
import com.ks39.seckill.result.CodeMsg;

import com.ks39.seckill.service.MiaoshaUserService;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;


public class CustomRealm extends AuthorizingRealm {

    //0. 注入业务对象
    @Autowired
    MiaoshaUserDao miaoshaUserDao;

    @Autowired
    MiaoshaUserService miaoshaUserService;

    //1. 授权(该项目不需要做)
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        return null;
    }

    //2. 认证
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {

        //2. 将authenticationToken对象转换为UsernamePasswordToken
        UsernamePasswordToken token = (UsernamePasswordToken) authenticationToken;

        System.out.println(token);
        //3. 先查找缓存，再查询数据库
        MiaoshaUser user = miaoshaUserService.getById(Long.parseLong(token.getUsername()));

        if(user == null){
            //如果user为null，返回user不存在
            throw new GlobalException(CodeMsg.USERNAME_NOT_EXIST);
        }

        //4. 认证
        SimpleAuthenticationInfo simpleAuthenticationInfo = new SimpleAuthenticationInfo(user,user.getPassword(),ByteSource.Util.bytes("1a2b3c4d"),getName());
        return simpleAuthenticationInfo;
    }

}
