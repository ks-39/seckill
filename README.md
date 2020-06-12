# 总览：简单的秒杀系统(集成redis缓存和RabbitMQ消息队列)
1. 项目目的：
    1. 学习在项目中使用Redis作为缓存，通过Redis预存储数据库数据和redis实现对象缓存，从而实现在高并发情况下减轻数据库压力的目的。
    2. 学习通过消息队列来实现异步处理请求，达到减轻服务器压力的目的。
    3. 超买超卖问题。
        1. 超买：在订单表中为userid添加唯一索引，防止同一个用户多次抢购
        2. 超卖(数据库层面解决)：在SQL语句添加条件：stock > 0
2. 技术栈：
    1. 后端：SpringBoot、MyBatis
    2. 前端：BootStrap
    3. 中间件(重点)：Redis、RabbitMQ、Shiro
3. 项目Github地址：# 总览：简单的秒杀系统(集成redis缓存和RabbitMQ消息队列)
1. 项目目的：
    1. 学习在项目中使用Redis作为缓存，通过Redis预存储数据库数据和redis实现对象缓存，从而实现在高并发情况下减轻数据库压力的目的。
    2. 学习通过消息队列来实现异步处理请求，达到减轻服务器压力的目的。
    3. 超买超卖问题。
        1. 超买：在订单表中为userid添加唯一索引，防止同一个用户多次抢购
        2. 超卖(数据库层面解决)：在SQL语句添加条件：stock > 0
2. 技术栈：
    1. 后端：SpringBoot、MyBatis
    2. 前端：BootStrap
    3. 中间件(重点)：Redis、RabbitMQ、Shiro
3. 项目Github地址：https://github.com/ks-39/seckill
4. 项目简介：
    1. 登录功能：使用Shiro校验，如果登录成功，将cookies存入Redis缓存
    2. 实现秒杀：
        1. path限流：用户需要先获取path，然后将path与userid绑定，存入redis。然后再通过这个path来执行秒杀
        2. redis预热：redis在项目启动时先存入数据库查找得到的数据。
        3. 内存标记减少redis访问：在预热时将查询得到的goodsId绑定。
        4. redis预减库存：先获取redis中预热存入的库存，执行-1，如果库存小于等于0，返回秒杀已结束
        5. 防止超买：查询数据库中是否有该userid的订单，如果有，返回不能重复抢购
        6. 请求入队
        7. 在MQ中再次确认是否超买(stock是否 <= 0)超卖(订单表中是否有该userid)
        8. MQ异步下单，执行秒杀

# 总结：
## 1. 分布式Session
1. 登录
2. 将session所在的cookie存入redis缓存
3. 将cookie存入response
4. 配置WebConfig，通过UserArgumentResolver获取与user绑定的threadLocal
```
@Service
public class UserArgumentResolver implements HandlerMethodArgumentResolver {

	@Autowired
	MiaoshaUserService userService;
	
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> clazz = parameter.getParameterType();
		return clazz== MiaoshaUser.class;
	}
	//获取user绑定的线程
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		return UserContext.getUser();
	}
}
```
5. 然后通过threadLocalMap.get获取user
```
public class UserContext {
	//1. 为user绑定threadlocal，key为user，value为当前线程
	private static ThreadLocal<MiaoshaUser> userHolder = new ThreadLocal<MiaoshaUser>();
	
	public static void setUser(MiaoshaUser user) {
		userHolder.set(user);
	}
	
	//3. 获取Map中的user
	public static MiaoshaUser getUser() {
		return userHolder.get();
	}

}
```

## 2. 超卖问题(数据库层面解决，性能差，并发低)：当stock > 0时

## 3. 超买问题(数据库层面解决)：为订单的userid添加唯一索引

## 4. redis预存数据，减少数据库访问
```
	public void afterPropertiesSet() throws Exception {
        //1. 先查询数据库数据
		List<GoodsVo> goodsList = goodsService.listGoodsVo();
		if(goodsList == null) {
			return;
		}
        //2. 遍历数据，存入redis，将goodsId绑定到内存标记
		for(GoodsVo goods : goodsList) {
			redisService.set(GoodsKey.getMiaoshaGoodsStock, ""+goods.getId(), goods.getGoodsStock());
			localOverMap.put(goods.getId(), false);
		}
	}
```

## 5. 内存标记，将goodsId绑定，减少redis缓存访问
1. 先在预存数据时对goodsId进行绑定
```
	public void afterPropertiesSet() throws Exception {
		List<GoodsVo> goodsList = goodsService.listGoodsVo();
		if(goodsList == null) {
			return;
		}
		for(GoodsVo goods : goodsList) {
			redisService.set(GoodsKey.getMiaoshaGoodsStock, ""+goods.getId(), goods.getGoodsStock());
			localOverMap.put(goods.getId(), false);
		}
	}
```

2. 在秒杀时，先判断内存标记是否为true，如果为true，说明已经抢购过一次
```
    @RequestMapping(value="/{path}/do_miaosha", method= RequestMethod.POST)
    @ResponseBody
    public Result<Integer> miaosha(Model model, MiaoshaUser user,
								   @RequestParam("goodsId")long goodsId, @PathVariable("path") String path
                                 ) {

		//1. 同步Session
    	model.addAttribute("user", user);
    	if(user == null) {
    		return Result.error(CodeMsg.SESSION_ERROR);
    	}

    	//2. 验证path
    	boolean check = miaoshaService.checkPath(user, goodsId, path);

    	if(!check){
    		return Result.error(CodeMsg.REQUEST_ILLEGAL);
    	}


		//3. 内存标记，减少redis访问
		boolean over = localOverMap.get(goodsId);
		System.out.println(over);        //此处为true，说明当前用户已经抢购过一次，返回
		if (over) {
			return Result.error(CodeMsg.MIAO_SHA_OVER);
		}

		//4. 预减库存
		long stock = redisService.decr(GoodsKey.getMiaoshaGoodsStock, "" + goodsId);//10
		System.out.println("库存：" + stock);
		if (stock < 0) {
			localOverMap.put(goodsId, true);
			return Result.error(CodeMsg.MIAO_SHA_OVER);
		}
    }
```

## 6. 请求先入队MQ，MQ异步下单
1. 请求入队
```
    @RequestMapping(value="/do_miaosha", method= RequestMethod.POST)
    @ResponseBody
    public Result<Integer> miaosha(Model model, MiaoshaUser user,
                                   @RequestParam("goodsId")long goodsId, @PathVariable("path") String path
                                 ) {
                                    
        .....

    	//6. 请求入队
    	MiaoshaMessage mm = new MiaoshaMessage();
    	mm.setUser(user);
    	mm.setGoodsId(goodsId);
    	sender.sendMiaoshaMessage(mm);
		System.out.println("入队成功");
    	return Result.success(0);//排队中
    }
```

2. Receiver异步下单
```
		@RabbitListener(queues=MQConfig.MIAOSHA_QUEUE)
		public void receive(String message) {
			log.info("receive message:"+message);

			MiaoshaMessage mm  = RedisService.stringToBean(message, MiaoshaMessage.class);
			MiaoshaUser user = mm.getUser();
			long goodsId = mm.getGoodsId();
			
			GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
	    	int stock = goods.getGoodsStock();
	    	if(stock <= 0) {
	    		return;
	    	}

	    	//判断是否已经秒杀到了
	    	MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
	    	if(order != null) {
	    		return;
	    	}

	    	//减库存 下订单 写入秒杀订单
	    	miaoshaService.miaosha(user, goods);
		}
```

3. 返回结果
```
    @RequestMapping(value="/result", method= RequestMethod.GET)
    @ResponseBody
    public Result<Long> miaoshaResult(Model model, MiaoshaUser user,
                                      @RequestParam("goodsId")long goodsId) {

    	model.addAttribute("user", user);
    	if(user == null) {
    		return Result.error(CodeMsg.SESSION_ERROR);
    	}
    	long result  =miaoshaService.getMiaoshaResult(user.getId(), goodsId);
    	return Result.success(result);
    }
```

## 7. 接口限流
1. 先获取path
```
    //限流
    @AccessLimit(seconds=5, maxCount=5, needLogin=true)
    @RequestMapping(value="/path", method= RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaPath(MiaoshaUser user,
                                         @RequestParam("goodsId")long goodsId) {
		//1. 同步Session
		if(user == null) {
			return Result.error(CodeMsg.SESSION_ERROR);
		}

		//3. 根据user和goodsId生成path
		String path  =miaoshaService.createMiaoshaPath(user, goodsId);
		return Result.success(path);
    }
```

2. 根据生成的path执行do_miaosha

3. access拦截器
```
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
```
4. 项目简介：
    1. 登录功能：使用Shiro校验，如果登录成功，将cookies存入Redis缓存
    2. 实现秒杀：
        1. path限流：用户需要先获取path，然后将path与userid绑定，存入redis。然后再通过这个path来执行秒杀
        2. redis预热：redis在项目启动时先存入数据库查找得到的数据。
        3. 内存标记减少redis访问：在预热时将查询得到的goodsId绑定。
        4. redis预减库存：先获取redis中预热存入的库存，执行-1，如果库存小于等于0，返回秒杀已结束
        5. 防止超买：查询数据库中是否有该userid的订单，如果有，返回不能重复抢购
        6. 请求入队
        7. 在MQ中再次确认是否超买(stock是否 <= 0)超卖(订单表中是否有该userid)
        8. MQ异步下单，执行秒杀

# 总结：
## 1. 分布式Session
1. 登录
2. 将session所在的cookie存入redis缓存
3. 将cookie存入response
4. 配置WebConfig，通过UserArgumentResolver获取与user绑定的threadLocal
```
@Service
public class UserArgumentResolver implements HandlerMethodArgumentResolver {

	@Autowired
	MiaoshaUserService userService;
	
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> clazz = parameter.getParameterType();
		return clazz== MiaoshaUser.class;
	}
	//获取user绑定的线程
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		return UserContext.getUser();
	}
}
```
5. 然后通过threadLocalMap.get获取user
```
public class UserContext {
	//1. 为user绑定threadlocal，key为user，value为当前线程
	private static ThreadLocal<MiaoshaUser> userHolder = new ThreadLocal<MiaoshaUser>();
	
	public static void setUser(MiaoshaUser user) {
		userHolder.set(user);
	}
	
	//3. 获取Map中的user
	public static MiaoshaUser getUser() {
		return userHolder.get();
	}

}
```

## 2. 超卖问题(数据库层面解决，性能差，并发低)：当stock > 0时

## 3. 超买问题(数据库层面解决)：为订单的userid添加唯一索引

## 4. redis预存数据，减少数据库访问
```
	public void afterPropertiesSet() throws Exception {
        //1. 先查询数据库数据
		List<GoodsVo> goodsList = goodsService.listGoodsVo();
		if(goodsList == null) {
			return;
		}
        //2. 遍历数据，存入redis，将goodsId绑定到内存标记
		for(GoodsVo goods : goodsList) {
			redisService.set(GoodsKey.getMiaoshaGoodsStock, ""+goods.getId(), goods.getGoodsStock());
			localOverMap.put(goods.getId(), false);
		}
	}
```

## 5. 内存标记，将goodsId绑定，减少redis缓存访问
1. 先在预存数据时对goodsId进行绑定
```
	public void afterPropertiesSet() throws Exception {
		List<GoodsVo> goodsList = goodsService.listGoodsVo();
		if(goodsList == null) {
			return;
		}
		for(GoodsVo goods : goodsList) {
			redisService.set(GoodsKey.getMiaoshaGoodsStock, ""+goods.getId(), goods.getGoodsStock());
			localOverMap.put(goods.getId(), false);
		}
	}
```

2. 在秒杀时，先判断内存标记是否为true，如果为true，说明已经抢购过一次
```
    @RequestMapping(value="/{path}/do_miaosha", method= RequestMethod.POST)
    @ResponseBody
    public Result<Integer> miaosha(Model model, MiaoshaUser user,
								   @RequestParam("goodsId")long goodsId, @PathVariable("path") String path
                                 ) {

		//1. 同步Session
    	model.addAttribute("user", user);
    	if(user == null) {
    		return Result.error(CodeMsg.SESSION_ERROR);
    	}

    	//2. 验证path
    	boolean check = miaoshaService.checkPath(user, goodsId, path);

    	if(!check){
    		return Result.error(CodeMsg.REQUEST_ILLEGAL);
    	}


		//3. 内存标记，减少redis访问
		boolean over = localOverMap.get(goodsId);
		System.out.println(over);        //此处为true，说明当前用户已经抢购过一次，返回
		if (over) {
			return Result.error(CodeMsg.MIAO_SHA_OVER);
		}

		//4. 预减库存
		long stock = redisService.decr(GoodsKey.getMiaoshaGoodsStock, "" + goodsId);//10
		System.out.println("库存：" + stock);
		if (stock < 0) {
			localOverMap.put(goodsId, true);
			return Result.error(CodeMsg.MIAO_SHA_OVER);
		}
    }
```

## 6. 请求先入队MQ，MQ异步下单
1. 请求入队
```
    @RequestMapping(value="/do_miaosha", method= RequestMethod.POST)
    @ResponseBody
    public Result<Integer> miaosha(Model model, MiaoshaUser user,
                                   @RequestParam("goodsId")long goodsId, @PathVariable("path") String path
                                 ) {
                                    
        .....

    	//6. 请求入队
    	MiaoshaMessage mm = new MiaoshaMessage();
    	mm.setUser(user);
    	mm.setGoodsId(goodsId);
    	sender.sendMiaoshaMessage(mm);
		System.out.println("入队成功");
    	return Result.success(0);//排队中
    }
```

2. Receiver异步下单
```
		@RabbitListener(queues=MQConfig.MIAOSHA_QUEUE)
		public void receive(String message) {
			log.info("receive message:"+message);

			MiaoshaMessage mm  = RedisService.stringToBean(message, MiaoshaMessage.class);
			MiaoshaUser user = mm.getUser();
			long goodsId = mm.getGoodsId();
			
			GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
	    	int stock = goods.getGoodsStock();
	    	if(stock <= 0) {
	    		return;
	    	}

	    	//判断是否已经秒杀到了
	    	MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
	    	if(order != null) {
	    		return;
	    	}

	    	//减库存 下订单 写入秒杀订单
	    	miaoshaService.miaosha(user, goods);
		}
```

3. 返回结果
```
    @RequestMapping(value="/result", method= RequestMethod.GET)
    @ResponseBody
    public Result<Long> miaoshaResult(Model model, MiaoshaUser user,
                                      @RequestParam("goodsId")long goodsId) {

    	model.addAttribute("user", user);
    	if(user == null) {
    		return Result.error(CodeMsg.SESSION_ERROR);
    	}
    	long result  =miaoshaService.getMiaoshaResult(user.getId(), goodsId);
    	return Result.success(result);
    }
```

## 7. 接口限流
1. 先获取path
```
    //限流
    @AccessLimit(seconds=5, maxCount=5, needLogin=true)
    @RequestMapping(value="/path", method= RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaPath(MiaoshaUser user,
                                         @RequestParam("goodsId")long goodsId) {
		//1. 同步Session
		if(user == null) {
			return Result.error(CodeMsg.SESSION_ERROR);
		}

		//3. 根据user和goodsId生成path
		String path  =miaoshaService.createMiaoshaPath(user, goodsId);
		return Result.success(path);
    }
```

2. 根据生成的path执行do_miaosha

3. access拦截器
```
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
```
