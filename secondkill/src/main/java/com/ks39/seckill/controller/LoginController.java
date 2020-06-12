package com.ks39.seckill.controller;


import com.ks39.seckill.domain.dto.LoginVo;
import com.ks39.seckill.result.Result;
import com.ks39.seckill.service.MiaoshaUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;


@Controller
@RequestMapping("/")
public class LoginController {

    @Autowired
    MiaoshaUserService userService;

    @RequestMapping("/login")
    public String toLogin() {
        return "login";
    }

    @RequestMapping("/do_login")
    @ResponseBody
    public Result<String> doLogin(HttpServletResponse response, @Valid LoginVo loginVo) {   //@valid使用jsr303校验
        //1. 执行登录
        String token = userService.login(response, loginVo);
        //2. 将token传回前端
        return Result.success(token);
    }
}

