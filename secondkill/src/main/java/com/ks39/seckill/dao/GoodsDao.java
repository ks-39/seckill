package com.ks39.seckill.dao;

import com.ks39.seckill.domain.MiaoshaGoods;
import com.ks39.seckill.domain.dto.GoodsVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface GoodsDao {
	
	@Select("select * from miaosha_goods")
	public List<GoodsVo> listGoodsVo();

	@Select("select * from miaosha_goods where id = #{goodsId}")
	public GoodsVo getGoodsVoByGoodsId(@Param("goodsId") long goodsId);

	@Update("update miaosha_goods set goods_stock = goods_stock - 1 where id = #{id} and goods_stock > 1")
	public int reduceStock(MiaoshaGoods g);

	@Update("update miaosha_goods set goods_stock = #{goodsStock} where id = #{id}")
	public int resetStock(MiaoshaGoods g);
	
}
