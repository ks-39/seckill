package com.ks39.seckill.dao;

import com.ks39.seckill.domain.MiaoshaUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


@Mapper
public interface MiaoshaUserDao {
	
	@Select("select * from miaosha_user where username = #{username}")
	public MiaoshaUser getById(@Param("username") long id);

}
