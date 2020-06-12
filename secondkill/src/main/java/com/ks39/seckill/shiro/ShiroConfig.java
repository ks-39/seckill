package com.ks39.seckill.shiro;


import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;


@Configuration
public class ShiroConfig {

        //1. 注入CustomRealm
        @Bean
        public CustomRealm getCustomRealm(){
            CustomRealm customRealm = new CustomRealm();
            //1. Md5加密
            customRealm.setCredentialsMatcher(hashedCredentialsMatcher());
            return customRealm;
        }


    //2. 创建DefaultWebSecurityManager安全管理器
    @Bean
    public DefaultWebSecurityManager getSecurityManager(){
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        //2.  关联CustomRealm（这一步必须放在最后，不然会不执行授权操作）
        securityManager.setRealm(getCustomRealm());
        return securityManager;
    }

    //3. 创建ShiroFilterFactoryBean拦截器
    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean(){
        ShiroFilterFactoryBean bean = new ShiroFilterFactoryBean();
        //设置安全管理器
        bean.setSecurityManager(getSecurityManager());
        /*
        anon:无需认证
        authc:认证
        user:rememberMe功能
         */
        //配置权限
        Map<String,String> filterMap = new LinkedHashMap<String,String>();
        filterMap.put("/admin/login","anon");
        filterMap.put("/admin/logout", "logout");

        //这一步必须放在最后
        filterMap.put("/admin/**","authc");

        //设置登录页面
        bean.setLoginUrl("/admin/login");
        //设置未授权页面
        bean.setUnauthorizedUrl("/blog/error");
        bean.setFilterChainDefinitionMap(filterMap);
        return bean;
    }

    //4. 比较器:比较用户登录时输入的密码,跟数据库密码配合盐值salt解密后是否一致
    @Bean
    public HashedCredentialsMatcher hashedCredentialsMatcher() {
        HashedCredentialsMatcher hashedCredentialsMatcher = new HashedCredentialsMatcher();
        hashedCredentialsMatcher.setHashAlgorithmName("md5"); //散列算法
        hashedCredentialsMatcher.setHashIterations(2); //散列的次数
        return hashedCredentialsMatcher;
    }

}
