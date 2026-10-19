package com.sky.service.impl;


import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    private UserMapper userMapper;

    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        log.info("用户登录");
        //1.准备访问微信服务器登录功能所需的请求参数
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", userLoginDTO.getCode());
        map.put("grant_type", "authorization_code");

        //2.调用微信登录功能拿到返回数据(字符串类型，但字符串顺序是JSON键值对顺序来的),再转为JSON格式
        String responseJson = HttpClientUtil.doGet("https://api.weixin.qq.com/sns/jscode2session", map);
        JSONObject jsonObject = JSONObject.parseObject(responseJson);

        //3.拿到后面返回User对象要用的openid
        //返回的responseJson数据中(session_key、unionid、openid、errcode、errmsg)只有openid对向上返回User有用
        String openid = jsonObject.getString("openid");

        //4.检查是否登录成功
        if(openid == null){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        //5.根据openid查询用户表，判断用户是否已存在
        User user = userMapper.getByOpenid(openid);
        if(user == null){
            //6.如果用户不存在，需要注册一个
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }


        return user;
    }
}
