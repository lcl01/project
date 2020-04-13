import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {
    /****
     * 创建Jwt令牌
     */
    @Test
    public void testCreateJwt(){
        JwtBuilder builder= Jwts.builder()
                .setId("888")             //设置唯一编号
                .setSubject("小白")       //设置主题  可以是JSON数据
                .setIssuedAt(new Date())  //设置签发日期
//                .setExpiration(new Date()) //用于设置过期时间 ，参数为Date类型数据
                .signWith(SignatureAlgorithm.HS256,"itcast");//设置签名 使用HS256算法，并设置SecretKey(字符串)
        Map<String, Object> map = new HashMap<>();
        map.put("name","张三");
        builder.addClaims(map);

        //构建 并返回一个字符串
        System.out.println( builder.compact() );
    }
    @Test
    public void testPara(){
        String compactJwt="eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI4ODgiLCJzdWIiOiLlsI_nmb0iLCJpYXQiOjE1ODYzMTI1OTcsIm5hbWUiOiLlvKDkuIkifQ.Mpyu0ntuNwguC9HAhFNMqqs5WgssSlR9fXwmdlCBBWc";
        Claims claims = Jwts.parser()
                .setSigningKey("itcast").
                        parseClaimsJws(compactJwt).
                        getBody();
        System.out.println(claims);

    }
}
