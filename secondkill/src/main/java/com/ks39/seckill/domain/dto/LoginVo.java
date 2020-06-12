package com.ks39.seckill.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class LoginVo {
	
	@NotNull
	@Length(min=3)
	private String username;
	
	@NotNull
	@Length(min=3)
	private String password;

}
