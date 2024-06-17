package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.mapper.XcUserRoleMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.model.po.XcUserRole;
import com.xuecheng.ucenter.service.AuthService;
import com.xuecheng.ucenter.service.WxAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service("wx_authservice")
public class WxAuthServiceImpl implements AuthService, WxAuthService {

    @Autowired
    XcUserMapper xcUserMapper;

    @Autowired
    XcUserRoleMapper xcUserRoleMapper;

    @Autowired
    WxAuthServiceImpl currentProxy;

    @Autowired
    RestTemplate restTemplate;

    @Value("${weixin.appid}")
    String appid;
    @Value("${weixin.secret}")
    String secret;

    public XcUser wxAuth(String code){
        //request the access token with the authorization code
        Map<String, String> access_token_map = getAccess_token(code);
        if(access_token_map==null) return null;

        String openid = access_token_map.get("openid");
        String access_token = access_token_map.get("access_token");

        // request the user info with the access token
        Map<String, String> userinfo = getUserinfo(access_token, openid);
        if(userinfo==null) return null;

        // add the user to the database
        XcUser xcUser = currentProxy.addWxUser(userinfo);

        return xcUser;
    }

    /**
     * request the access token with the authorization code
     *
     * @return {
     * "access_token":"ACCESS_TOKEN",
     * "expires_in":7200,
     * "refresh_token":"REFRESH_TOKEN",
     * "openid":"OPENID",
     * "scope":"SCOPE",
     * "unionid": "o6_bmasdasdsad6_2sgVt7hMZOPfL"
     * }
     */
    private Map<String, String> getAccess_token(String code) {
        // URL for requesting the token
        String wxUrl_template = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";
        String wxUrl = String.format(wxUrl_template, appid, secret, code);

        // get the request result in String
        ResponseEntity<String> exchange = restTemplate.exchange(wxUrl, HttpMethod.POST, null, String.class);
        String result = exchange.getBody();

        // return the result in JSON
        return JSON.parseObject(result, Map.class);
    }

    /**
     * get the user info with the access token
     *
     * @return {
     * "openid":"OPENID",
     * "nickname":"NICKNAME",
     * "sex":1,
     * "province":"PROVINCE",
     * "city":"CITY",
     * "country":"COUNTRY",
     * "headimgurl": "HEADIMGURL",
     * "privilege":[
     * "PRIVILEGE1",
     * "PRIVILEGE2"
     * ],
     * "unionid": ""
     * }
     */
    private Map<String,String> getUserinfo(String access_token, String openid) {
        // URL for requesting the user info
        String wxUrl_template = "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s";
        String wxUrl = String.format(wxUrl_template, access_token,openid);

        // get the request result in String
        ResponseEntity<String> exchange = restTemplate.exchange(wxUrl, HttpMethod.POST, null, String.class);
        String result = exchange.getBody();

        //transcode
        String transcoded = new String(result.getBytes(StandardCharsets.ISO_8859_1),StandardCharsets.UTF_8);

        return JSON.parseObject(transcoded, Map.class);
    }

    /**
     * save the user info to the database
     */
    @Transactional
    public XcUser addWxUser(Map<String, String> userInfo_map){
        // search the user by the unionid
        String unionid = userInfo_map.get("unionid").toString();
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getWxUnionid, unionid));
        // if the user already exists, return the user and do not add the user to the database
        if(xcUser != null) return xcUser;

        // add new user
        xcUser = new XcUser();
        xcUser.setId(UUID.randomUUID().toString());
        xcUser.setWxUnionid(unionid);
        xcUser.setUsername(unionid);
        xcUser.setPassword(unionid);
        xcUser.setNickname(userInfo_map.get("nickname").toString());
        xcUser.setName(userInfo_map.get("nickname").toString());
        xcUser.setUserpic(userInfo_map.get("headimgurl").toString());
        xcUser.setUtype("101001");//学生类型
        xcUser.setStatus("1");//用户状态
        xcUser.setCreateTime(LocalDateTime.now());
        xcUserMapper.insert(xcUser);

        // add the user role
        XcUserRole xcUserRole = new XcUserRole();
        xcUserRole.setId(UUID.randomUUID().toString());
        xcUserRole.setUserId(xcUser.getId());
        xcUserRole.setRoleId("17");//学生角色
        xcUserRole.setCreateTime(LocalDateTime.now());
        xcUserRoleMapper.insert(xcUserRole);

        return xcUser;
    }


    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        // search the user by the username
        String username = authParamsDto.getUsername();
        XcUser user = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        if(user==null) throw new RuntimeException("账号不存在");

        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(user,xcUserExt);

        return xcUserExt;
    }
}

