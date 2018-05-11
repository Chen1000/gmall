package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.passport.util.JwtUtil;
import com.atguigu.gmall.service.UserService;
import io.jsonwebtoken.SignatureException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import sun.security.krb5.internal.PAData;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {


    @Reference
    UserService userService;

    @Value("${token.key}")
    String TOKEN_KEY;



    @RequestMapping("index")
    public String index(HttpServletRequest httpServletRequest){
        String originUrl = httpServletRequest.getParameter("originUrl");
        httpServletRequest.setAttribute("originUrl",originUrl);

        return "index";
    }

    @RequestMapping(value = "login",method = RequestMethod.POST)
    @ResponseBody
    public String login(HttpServletRequest request){
        //1、调service的后台检查用户身份
        String remoteAddr = request.getHeader("x-forwarded-for");

        String loginName = request.getParameter("loginName");
        String passwd = request.getParameter("passwd");
        if(loginName!=null&&passwd!=null){
            UserInfo userInfo=new UserInfo();
            userInfo.setPasswd(passwd);
            userInfo.setLoginName(loginName);
            UserInfo userInfoLogin = userService.login(userInfo);
            if(userInfoLogin==null){
                return "fail";
            }

            Map map=new HashMap();
            map.put("userId",userInfoLogin.getId());
            map.put("nickName",userInfoLogin.getNickName());
            //2、制作token
            String token = JwtUtil.encode(TOKEN_KEY, map, remoteAddr);
            return token;
        }
        return "fail";

    }


    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request){
        String token = request.getParameter("token");
        String currentIp = request.getParameter("currentIp");
        //1 token 检查

        Map map = null;
        try {
            map = JwtUtil.decode(TOKEN_KEY, token, currentIp);
        } catch (SignatureException e) {
            return "fail";
        }

        //2 redis
        if(map!=null){
            String userId = (String)map.get("userId");
            UserInfo userInfo= userService.verify(userId);
            if(userInfo!=null){
                return "success";
            }
        }
        return "fail";
    }




    /*@RequestMapping("index")
    public String index(HttpServletRequest request) {
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl", originUrl);

        return "index";
    }


    @RequestMapping(value = "login", method = RequestMethod.POST)
    @ResponseBody
    public String login(HttpServletRequest request) {
        //1、调service的后台检查用户身份
        String remoteAddr = request.getHeader("x-forwarded-for");

        String loginName = request.getParameter("loginName");
        String passwd = request.getParameter("passwd");

        if (loginName != null && passwd != null) {
            UserInfo userInfo = new UserInfo();
            userInfo.setLoginName(loginName);
            userInfo.setPasswd(passwd);

            UserInfo userInfoLogin = userService.login(userInfo);

            if (userInfoLogin == null) {
                return "fail";
            }

            Map map = new HashMap();
            map.put("userId", userInfoLogin.getId());
            map.put("nickName", userInfoLogin.getNickName());

            //2、制作token
            String token = JwtUtil.encode(TOKEN_KEY, map, remoteAddr);

            return token;
        }
        return "fail";
    }


    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request) {
        String token = request.getParameter("token");
        String currentIp = request.getParameter("currentIp");

        //1 token 检查
        Map map = null;
        try {
            map = JwtUtil.decode(TOKEN_KEY, token, currentIp);
        } catch (SignatureException e) {
            e.printStackTrace();
            return "fail";
        }

        //2 redis
        if (map != null) {
            String userId = (String) map.get("userId");
            UserInfo userInfo = userService.verify(userId);

            if (userInfo != null) {
                return "success";
            }
        }

        return "fail";
    }
*/

    @Test
    public void contextLoads() {

        String key = "atguigu";
        String ip = "192.168.242.128";

        Map map = new HashMap();
        map.put("userId", "123");
        map.put("nickName", "Chen");

        String token = JwtUtil.encode(key, map, ip);

        System.out.println("token = " + token);

        Map decode = JwtUtil.decode(key, token, ip);

    }


}
