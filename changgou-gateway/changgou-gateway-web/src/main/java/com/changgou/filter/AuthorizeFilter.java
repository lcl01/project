package com.changgou.filter;

import com.changgou.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthorizeFilter implements GlobalFilter, Ordered {
    //令牌头名字
    private static final String AUTHORIZE_TOKEN = "Authorization";

    private static final String USER_LOGIN_URL="http://localhost:9001/oauth/login";
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //获取Request、Response对象
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        //获取请求的URI
        String path = request.getURI().getPath();

        //如果是登录、goods等开放的微服务[这里的goods部分开放],则直接放行,这里不做完整演示，完整演示需要设计一套权限系统
        if (path.startsWith("/api/user/login") || path.startsWith("/api/brand/search/")|| !URLFilter.hasAuthorize(path)) {
            //放行
            Mono<Void> filter = chain.filter(exchange);
        return filter;
    }
//获取头文件中的令牌信息
        String tokent = request.getHeaders().getFirst(AUTHORIZE_TOKEN);
        //如果头文件中没有，则从请求参数中获取
        if (StringUtils.isEmpty(tokent)) {
            tokent = request.getQueryParams().getFirst(AUTHORIZE_TOKEN);
        }
        //从cookie获取令牌
        HttpCookie first=request.getCookies().getFirst(AUTHORIZE_TOKEN);
        if (first!=null) {
            tokent=first.getValue();
        }
        //如果为空，则输出错误代码
        if (StringUtils.isEmpty(tokent)) {
            //设置方法不允许被访问，405错误代码
//            response.setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
//            return response.setComplete();
            return needAuthorization(USER_LOGIN_URL+"?FROM="+request.getURI(),exchange);
        }

        //解析令牌数据
        try {
//            Claims claims = JwtUtil.parseJWT(tokent);
            //将令牌数据添加到头文件
//            request.mutate().header(AUTHORIZE_TOKEN,claims.toString());
            request.mutate().header(AUTHORIZE_TOKEN,"Bearer "+tokent);

        } catch (Exception e) {
            e.printStackTrace();
            //解析失败，响应401错误
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        //放行
        return chain.filter(exchange);
    }

    private Mono<Void> needAuthorization(String url, ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.SEE_OTHER);
        response.getHeaders().set("Location",url);

        return exchange.getResponse().setComplete();
    }

    /***
     * 过滤器执行顺序
     * @return
     */
    @Override
    public int getOrder() {
        return 0;
    }
}
