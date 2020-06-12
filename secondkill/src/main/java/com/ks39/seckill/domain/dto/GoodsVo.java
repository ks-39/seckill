package com.ks39.seckill.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class GoodsVo{

	private Long id;
	private String goodsName;
	private String goodsImg;
	private Integer goodsStock;
	private double goodsPrice;
	private double goodsMiaoshaPrice;

	private Date startDate;
	private Date endDate;
}
