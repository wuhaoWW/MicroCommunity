package com.java110.boot.smo.payment.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.boot.properties.WechatAuthProperties;
import com.java110.boot.smo.AppAbstractComponentSMO;
import com.java110.boot.smo.payment.IToPaySMO;
import com.java110.boot.smo.payment.adapt.IPayAdapt;
import com.java110.core.context.IPageData;
import com.java110.core.context.PageData;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.app.AppDto;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.dto.smallWeChat.SmallWeChatDto;
import com.java110.intf.acct.IAccountDetailInnerServiceSMO;
import com.java110.intf.acct.IAccountInnerServiceSMO;
import com.java110.intf.fee.IFeeAccountDetailServiceSMO;
import com.java110.po.account.AccountPo;
import com.java110.po.accountDetail.AccountDetailPo;
import com.java110.po.feeAccountDetail.FeeAccountDetailPo;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.CommonConstant;
import com.java110.utils.constant.WechatConstant;
import com.java110.utils.factory.ApplicationContextFactory;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service("toPaySMOImpl")
public class ToPaySMOImpl extends AppAbstractComponentSMO implements IToPaySMO {
    private static final Logger logger = LoggerFactory.getLogger(AppAbstractComponentSMO.class);


    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RestTemplate outRestTemplate;

    @Autowired
    private WechatAuthProperties wechatAuthProperties;

    @Autowired
    private IAccountInnerServiceSMO accountInnerServiceSMOImpl;

    @Autowired
    private IAccountDetailInnerServiceSMO accountDetailInnerServiceSMOImpl;

    @Autowired
    private IFeeAccountDetailServiceSMO feeAccountDetailServiceSMOImpl;

    @Override
    public ResponseEntity<String> toPay(IPageData pd) {
        return super.businessProcess(pd);
    }

    @Override
    protected void validate(IPageData pd, JSONObject paramIn) {

        Assert.jsonObjectHaveKey(paramIn, "communityId", "????????????????????????communityId??????");
        Assert.jsonObjectHaveKey(paramIn, "cycles", "????????????????????????cycles??????");
        Assert.jsonObjectHaveKey(paramIn, "receivedAmount", "????????????????????????receivedAmount??????");
        Assert.jsonObjectHaveKey(paramIn, "feeId", "????????????????????????feeId??????");
        Assert.jsonObjectHaveKey(paramIn, "feeName", "????????????????????????feeName??????");
        Assert.jsonObjectHaveKey(paramIn, "appId", "????????????????????????appId??????");

    }

    @Override
    protected ResponseEntity<String> doBusinessProcess(IPageData pd, JSONObject paramIn) throws Exception {

        ResponseEntity<String> responseEntity = null;

        SmallWeChatDto smallWeChatDto = getSmallWechat(pd, paramIn);

        if (smallWeChatDto == null) { //???????????????????????? ?????????????????????
            smallWeChatDto = new SmallWeChatDto();
            smallWeChatDto.setAppId(wechatAuthProperties.getAppId());
            smallWeChatDto.setAppSecret(wechatAuthProperties.getSecret());
            smallWeChatDto.setMchId(wechatAuthProperties.getMchId());
            smallWeChatDto.setPayPassword(wechatAuthProperties.getKey());
        }

        //????????????ID
        paramIn.put("userId", pd.getUserId());
        String url = "fee.payFeePre";
        responseEntity = super.callCenterService(restTemplate, pd, paramIn.toJSONString(), url, HttpMethod.POST);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            return responseEntity;
        }
        JSONObject orderInfo = JSONObject.parseObject(responseEntity.getBody().toString());
        String orderId = orderInfo.getString("oId");
        String feeName = orderInfo.getString("feeName");
        double money = Double.parseDouble(orderInfo.getString("receivedAmount"));
        //???????????????????????? == 0 ??????0 ???????????????????????????
        if (money <= 0) {
            JSONObject paramOut = new JSONObject();
            paramOut.put("oId", orderId);
            String urlOut = "fee.payFeeConfirm";
            responseEntity = this.callCenterService(getHeaders("-1", pd.getAppId()), paramOut.toJSONString(), urlOut, HttpMethod.POST);
            JSONObject param = new JSONObject();
            if (responseEntity.getStatusCode() != HttpStatus.OK) {
                param.put("code", "101");
                param.put("msg", "?????????0????????????");
                return new ResponseEntity(JSONObject.toJSONString(param), HttpStatus.OK);
            }
            JSONObject result = JSONObject.parseObject(responseEntity.getBody());
            if (ResultVo.CODE_OK != result.getInteger("code")) {
                return responseEntity;
            }
            if (paramIn.containsKey("selectUserAccount") && !StringUtil.isEmpty(paramIn.getString("selectUserAccount"))) {
                String selectUserAccount = paramIn.getString("selectUserAccount");
                JSONArray params = JSONArray.parseArray(selectUserAccount);
                for (int paramIndex = 0; paramIndex < params.size(); paramIndex++) {
                    JSONObject paramObj = params.getJSONObject(paramIndex);
                    if (!StringUtil.isEmpty(paramObj.getString("acctType")) && paramObj.getString("acctType").equals("2004")) { //????????????
                        //????????????
                        BigDecimal amount = new BigDecimal(paramObj.getString("amount"));
                        //????????????????????????
                        BigDecimal maximumNumber = new BigDecimal(paramObj.getString("maximumNumber"));
                        //????????????????????????
                        BigDecimal deductionProportion = new BigDecimal(paramObj.getString("deductionProportion"));
                        int flag = amount.compareTo(maximumNumber);
                        BigDecimal redepositAmount = new BigDecimal("0.00");
                        BigDecimal integralAmount = new BigDecimal("0.00");
                        if (flag == 1) { //?????????????????????????????????????????????????????????????????????
                            redepositAmount = maximumNumber;
                            integralAmount = amount.subtract(maximumNumber);
                        }
                        if (flag > -1) { //???????????????????????????????????????????????????????????????????????????
                            redepositAmount = maximumNumber;
                            integralAmount = amount.subtract(maximumNumber);
                        }
                        if (flag == -1) { //???????????????????????????????????????????????????????????????
                            redepositAmount = amount;
                        }
                        if (flag < 1) { //?????????????????????????????????????????????????????????????????????
                            redepositAmount = amount;
                        }
                        if (flag == 0) { //????????????????????????????????????
                            redepositAmount = amount;
                        }
                        //??????????????????
                        AccountPo accountPo = new AccountPo();
                        accountPo.setAcctId(paramObj.getString("acctId"));
                        accountPo.setAmount(integralAmount.toString());
                        accountInnerServiceSMOImpl.updateAccount(accountPo);
                        //??????????????????
                        AccountDetailPo accountDetailPo = new AccountDetailPo();
                        accountDetailPo.setDetailId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_detailId));
                        accountDetailPo.setAcctId(paramObj.getString("acctId"));
                        accountDetailPo.setDetailType("2002"); //1001 ?????? 2002 ??????
                        accountDetailPo.setRelAcctId("-1");
                        accountDetailPo.setAmount(redepositAmount.toString());
                        accountDetailPo.setObjType("6006"); //6006 ?????? 7007 ??????
                        accountDetailPo.setObjId(paramObj.getString("objId"));
                        accountDetailPo.setOrderId("-1");
                        accountDetailPo.setbId("-1");
                        accountDetailPo.setRemark("?????????????????????");
                        accountDetailPo.setCreateTime(new Date());
                        accountDetailInnerServiceSMOImpl.saveAccountDetails(accountDetailPo);
                        //???????????????????????????
                        BigDecimal divide = redepositAmount.divide(deductionProportion);
                        BigDecimal deductionAmount = new BigDecimal(paramIn.getString("deductionAmount"));
                        //??????????????????
                        int flag2 = divide.compareTo(deductionAmount);
                        BigDecimal subtract = new BigDecimal("0.00");
                        //????????????????????????
                        FeeAccountDetailPo feeAccountDetailPo = new FeeAccountDetailPo();
                        if (flag2 == -1) { //????????????????????????????????????
                            subtract = deductionAmount.subtract(divide);
                            BigDecimal multiply = divide.multiply(deductionProportion);
                            feeAccountDetailPo.setAmount(multiply.toString()); //??????????????????
                        } else if (flag < 1) { //??????????????????????????????????????????
                            subtract = deductionAmount.subtract(divide);
                            BigDecimal multiply = divide.multiply(deductionProportion);
                            feeAccountDetailPo.setAmount(multiply.toString()); //??????????????????
                        } else {
                            BigDecimal multiply = deductionAmount.multiply(deductionProportion);
                            feeAccountDetailPo.setAmount(multiply.toString()); //??????????????????
                        }
                        paramIn.put("receivedMoney", subtract);
//                    payFeeDetailPo.setReceivedAmount(subtract.toString());
                        feeAccountDetailPo.setFadId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_fadId));
                        feeAccountDetailPo.setDetailId(accountDetailPo.getDetailId());
                        feeAccountDetailPo.setCommunityId(paramObj.getString("communityId"));
                        feeAccountDetailPo.setState("1003"); //1001 ????????? 1002 ?????????????????? 1003 ?????????????????? 1004 ???????????????
                        feeAccountDetailServiceSMOImpl.saveFeeAccountDetail(feeAccountDetailPo);
                    } else if (!StringUtil.isEmpty(paramObj.getString("acctType")) && paramObj.getString("acctType").equals("2003")) { //????????????
                        //????????????
                        BigDecimal amount = new BigDecimal(paramObj.getString("amount"));
                        //??????????????????
                        BigDecimal deductionAmount = new BigDecimal("0.00");
                        if (paramIn.containsKey("receivedMoney") && !StringUtil.isEmpty(paramIn.getString("receivedMoney"))) {
                            deductionAmount = new BigDecimal(paramIn.getString("receivedMoney"));
                        } else {
                            deductionAmount = new BigDecimal(paramIn.getString("deductionAmount"));
                        }
                        int flag = amount.compareTo(deductionAmount);
                        BigDecimal redepositAmount = new BigDecimal("0.00");
                        BigDecimal integralAmount = new BigDecimal("0.00");
                        if (flag == 1) { //?????????????????????????????????????????????????????????
                            redepositAmount = deductionAmount;
                            integralAmount = amount.subtract(deductionAmount);
                        }
                        if (flag > -1) { //???????????????????????????????????????????????????????????????
                            redepositAmount = deductionAmount;
                            integralAmount = amount.subtract(deductionAmount);
                        }
                        if (flag == -1) { //?????????????????????????????????????????????????????????
                            redepositAmount = amount;
                        }
                        if (flag < 1) { //???????????????????????????????????????????????????????????????
                            redepositAmount = amount;
                        }
                        if (flag == 0) { //??????????????????????????????
                            redepositAmount = amount;
                        }
                        //??????????????????
                        AccountPo accountPo = new AccountPo();
                        accountPo.setAcctId(paramObj.getString("acctId"));
                        accountPo.setAmount(integralAmount.toString());
                        accountInnerServiceSMOImpl.updateAccount(accountPo);
                        //??????????????????
                        AccountDetailPo accountDetailPo = new AccountDetailPo();
                        accountDetailPo.setDetailId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_detailId));
                        accountDetailPo.setAcctId(paramObj.getString("acctId"));
                        accountDetailPo.setDetailType("2002"); //1001 ?????? 2002 ??????
                        accountDetailPo.setRelAcctId("-1");
                        accountDetailPo.setAmount(redepositAmount.toString());
                        accountDetailPo.setObjType("6006"); //6006 ?????? 7007 ??????
                        accountDetailPo.setObjId(paramObj.getString("objId"));
                        accountDetailPo.setOrderId("-1");
                        accountDetailPo.setbId("-1");
                        accountDetailPo.setRemark("???????????????????????????");
                        accountDetailPo.setCreateTime(new Date());
                        accountDetailInnerServiceSMOImpl.saveAccountDetails(accountDetailPo);
                        //????????????????????????
                        FeeAccountDetailPo feeAccountDetailPo = new FeeAccountDetailPo();
                        feeAccountDetailPo.setFadId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_fadId));
                        feeAccountDetailPo.setDetailId(accountDetailPo.getDetailId());
                        feeAccountDetailPo.setCommunityId(paramIn.getString("communityId"));
                        feeAccountDetailPo.setState("1002"); //1001 ????????? 1002 ?????????????????? 1003 ?????????????????? 1004 ???????????????
                        feeAccountDetailPo.setAmount(redepositAmount.toString()); //??????????????????
                        feeAccountDetailServiceSMOImpl.saveFeeAccountDetail(feeAccountDetailPo);
                    }
                }
            }
            param.put("code", "100");
            param.put("msg", "?????????0????????????");
            return new ResponseEntity(JSONObject.toJSONString(param), HttpStatus.OK);
        }
        String appType = OwnerAppUserDto.APP_TYPE_WECHAT_MINA;
        if (AppDto.WECHAT_OWNER_APP_ID.equals(pd.getAppId())) {
            appType = OwnerAppUserDto.APP_TYPE_WECHAT;
        } else if (AppDto.WECHAT_MINA_OWNER_APP_ID.equals(pd.getAppId())) {
            appType = OwnerAppUserDto.APP_TYPE_WECHAT_MINA;
        } else {
            appType = OwnerAppUserDto.APP_TYPE_APP;
        }

        Map tmpParamIn = new HashMap();
        tmpParamIn.put("userId", pd.getUserId());
        tmpParamIn.put("appType", appType);
        responseEntity = super.getOwnerAppUser(pd, restTemplate, tmpParamIn);
        logger.debug("?????????????????????????????????" + responseEntity);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new IllegalArgumentException("???????????????????????????" + tmpParamIn);
        }
        JSONObject userResult = JSONObject.parseObject(responseEntity.getBody().toString());
        int total = userResult.getIntValue("total");
        if (total < 1) {
            //????????????????????????
            throw new IllegalArgumentException("?????????????????????");
        }

        JSONObject realUserInfo = userResult.getJSONArray("data").getJSONObject(0);
        String openId = realUserInfo.getString("openId");
        String payAdapt = MappingCache.getValue(WechatConstant.WECHAT_DOMAIN, WechatConstant.PAY_ADAPT);
        payAdapt = StringUtil.isEmpty(payAdapt) ? DEFAULT_PAY_ADAPT : payAdapt;
        //???????????????
        IPayAdapt tPayAdapt = ApplicationContextFactory.getBean(payAdapt, IPayAdapt.class);
        Map result = tPayAdapt.java110Payment(outRestTemplate, feeName, paramIn.getString("tradeType"), orderId, money, openId, smallWeChatDto);
        responseEntity = new ResponseEntity(JSONObject.toJSONString(result), HttpStatus.OK);

        return responseEntity;
    }

    private Map<String, String> getHeaders(String userId, String appId) {
        Map<String, String> headers = new HashMap<>();
        headers.put(CommonConstant.HTTP_APP_ID.toLowerCase(), appId);
        headers.put(CommonConstant.HTTP_USER_ID.toLowerCase(), userId);
        headers.put(CommonConstant.HTTP_TRANSACTION_ID.toLowerCase(), UUID.randomUUID().toString());
        headers.put(CommonConstant.HTTP_REQ_TIME.toLowerCase(), DateUtil.getDefaultFormateTimeString(new Date()));
        headers.put(CommonConstant.HTTP_SIGN.toLowerCase(), "");
        return headers;
    }

    private SmallWeChatDto getSmallWechat(IPageData pd, JSONObject paramIn) {

        ResponseEntity responseEntity = null;

        pd = PageData.newInstance().builder(pd.getUserId(), "", "", pd.getReqData(),
                "", "", "", "",
                pd.getAppId());
        responseEntity = this.callCenterService(restTemplate, pd, "",
                "smallWeChat.listSmallWeChats?appId="
                        + paramIn.getString("appId") + "&page=1&row=1&communityId=" + paramIn.getString("communityId"), HttpMethod.GET);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            return null;
        }
        JSONObject smallWechatObj = JSONObject.parseObject(responseEntity.getBody().toString());
        JSONArray smallWeChats = smallWechatObj.getJSONArray("smallWeChats");

        if (smallWeChats == null || smallWeChats.size() < 1) {
            return null;
        }

        return BeanConvertUtil.covertBean(smallWeChats.get(0), SmallWeChatDto.class);
    }


}
