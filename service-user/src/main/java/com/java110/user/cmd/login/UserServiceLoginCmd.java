package com.java110.user.cmd.login;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.AuthenticationFactory;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.store.StoreDto;
import com.java110.dto.store.StoreUserDto;
import com.java110.dto.userLogin.UserLoginDto;
import com.java110.intf.store.IStoreInnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.intf.user.IUserLoginInnerServiceSMO;
import com.java110.po.userLogin.UserLoginPo;
import com.java110.service.context.DataQuery;
import com.java110.service.smo.IQueryServiceSMO;
import com.java110.utils.constant.CommonConstant;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.constant.ServiceCodeConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.SMOException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Java110Cmd(serviceCode = "user.service.login")
public class UserServiceLoginCmd extends Cmd{

    private final static Logger logger = LoggerFactory.getLogger(UserServiceLoginCmd.class);

    @Autowired
    private IQueryServiceSMO queryServiceSMOImpl;

    @Autowired
    private IUserLoginInnerServiceSMO userLoginInnerServiceSMOImpl;

    @Autowired
    private IStoreInnerServiceSMO storeInnerServiceSMOImpl;

    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.jsonObjectHaveKey(reqJson, "username", "????????????????????????username??????????????????" + reqJson);
        Assert.jsonObjectHaveKey(reqJson, "passwd", "????????????????????????passwd??????????????????" + reqJson);
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        DataQuery dataQuery = new DataQuery();
        dataQuery.setServiceCode(ServiceCodeConstant.SERVICE_CODE_QUERY_USER_LOGIN);
        JSONObject param = new JSONObject();
        param.put("userCode", reqJson.getString("username"));
        param.put("pwd", reqJson.getString("passwd"));
        param.put("levelCdTag","1");
        dataQuery.setRequestParams(param);
        queryServiceSMOImpl.commonQueryService(dataQuery);
        ResponseEntity<String> responseEntity = dataQuery.getResponseEntity();
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            context.setResponseEntity(new ResponseEntity<>("???????????????", HttpStatus.FORBIDDEN));
            return;
        }
        Assert.isJsonObject(responseEntity.getBody(), "????????????????????????,????????????????????????????????????json?????? " + responseEntity.getBody());

        JSONObject resultInfo = JSONObject.parseObject(responseEntity.getBody());
        if (!resultInfo.containsKey("user") || !resultInfo.getJSONObject("user").containsKey("userPwd")
                || !resultInfo.getJSONObject("user").containsKey("userId")) {
            responseEntity = new ResponseEntity<String>("?????????????????????", HttpStatus.UNAUTHORIZED);
            context.setResponseEntity(responseEntity);
            return;
        }

        JSONObject userInfo = resultInfo.getJSONObject("user");
        String userPwd = userInfo.getString("userPwd");
        if (!userPwd.equals(reqJson.getString("passwd"))) {
            responseEntity = new ResponseEntity<String>("????????????", HttpStatus.UNAUTHORIZED);
            context.setResponseEntity(responseEntity);
            return;
        }


        //??????????????????
        StoreUserDto storeUserDto = new StoreUserDto();
        storeUserDto.setUserId(userInfo.getString("userId"));
        List<StoreUserDto> storeUserDtos = storeInnerServiceSMOImpl.getStoreUserInfo(storeUserDto);

        if (storeUserDtos != null && storeUserDtos.size() > 0) {
            String state = storeUserDtos.get(0).getState();
            if ("48002".equals(state)) {
                responseEntity = new ResponseEntity<String>("?????????????????????????????????????????????", HttpStatus.UNAUTHORIZED);
                context.setResponseEntity(responseEntity);
                return;
            }

            StoreDto storeDto = new StoreDto();
            storeDto.setStoreId(storeUserDtos.get(0).getStoreId());
            List<StoreDto> storeDtos = storeInnerServiceSMOImpl.getStores(storeDto);
            if (storeDtos != null && storeDtos.size() > 0) {
                userInfo.put("storeType", storeDtos.get(0).getStoreTypeCd());
            }
        }

        try {
            Map userMap = new HashMap();
            userMap.put(CommonConstant.LOGIN_USER_ID, userInfo.getString("userId"));
            userMap.put(CommonConstant.LOGIN_USER_NAME, userInfo.getString("userName"));
            String token = AuthenticationFactory.createAndSaveToken(userMap);
            userInfo.remove("userPwd");
            userInfo.put("token", token);
            //??????????????????
            UserLoginPo userLoginPo = new UserLoginPo();
            userLoginPo.setLoginId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_loginId));
            userLoginPo.setLoginTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
            userLoginPo.setPassword(userPwd);
            userLoginPo.setSource(UserLoginDto.SOURCE_WEB);
            userLoginPo.setToken(token);
            userLoginPo.setUserId(userInfo.getString("userId"));
            userLoginPo.setUserName(userInfo.getString("userName"));
            userLoginInnerServiceSMOImpl.saveUserLogin(userLoginPo);
            responseEntity = new ResponseEntity<String>(userInfo.toJSONString(), HttpStatus.OK);
            context.setResponseEntity(responseEntity);
        } catch (Exception e) {
            logger.error("???????????????", e);
            throw new SMOException(ResponseConstant.RESULT_CODE_INNER_ERROR, "???????????????????????????????????????");
        }
    }

    /**
     * ?????????????????????
     *
     * @param paramIn
     * @return
     */
    private JSONObject refreshParamIn(String paramIn) {
        JSONObject paramObj = JSONObject.parseObject(paramIn);
        paramObj.put("userId", "-1");
        paramObj.put("levelCd", "0");

        return paramObj;
    }
}
