package com.ks39.seckill.domain;

import lombok.Data;

import java.util.Date;

@Data
public class MiaoshaGoods {

	private Long id;

	private String goodsName;
	private String goodsImg;
	private Integer goodsStock;
	private double goodsPrice;
	private double goodsMiaoshaPrice;
	private Date startDate;
	private Date endDate;

}
