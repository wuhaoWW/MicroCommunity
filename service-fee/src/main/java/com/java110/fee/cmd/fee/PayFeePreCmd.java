package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.core.smo.IComputeFeeSMO;
import com.java110.dto.account.AccountDto;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.couponUser.CouponUserDto;
import com.java110.dto.fee.FeeAttrDto;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDetailDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.feeDiscount.ComputeDiscountDto;
import com.java110.intf.acct.IAccountDetailInnerServiceSMO;
import com.java110.intf.acct.IAccountInnerServiceSMO;
import com.java110.intf.acct.ICouponUserV1InnerServiceSMO;
import com.java110.intf.community.ICommunityV1InnerServiceSMO;
import com.java110.intf.community.IRepairUserInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.fee.*;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.po.account.AccountPo;
import com.java110.po.accountDetail.AccountDetailPo;
import com.java110.po.feeAccountDetail.FeeAccountDetailPo;
import com.java110.utils.cache.CommonCache;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * ??????????????????
 * ???????????????feePrintPage.deleteFeePrintPage
 * ???????????????/app/feePrintPage.DeleteFeePrintPage
 * add by ????????? at 2021-09-16 22:26:04 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * ?????????http://www.homecommunity.cn
 * ???????????????????????????????????????????????? ???????????????????????????????????????????????????????????? ???????????????????????????????????????
 * // modify by ?????? at 2021-09-12 ???10?????????????????????????????????bug ?????????????????????10???20??? ?????? 20??????30???
 */
@Java110Cmd(serviceCode = "fee.payFeePre")
public class PayFeePreCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(PayFeePreCmd.class);

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    @Autowired
    private IFeeDiscountInnerServiceSMO feeDiscountInnerServiceSMOImpl;

    @Autowired
    private IFeeAttrInnerServiceSMO feeAttrInnerServiceSMOImpl;

    @Autowired
    private IRepairUserInnerServiceSMO repairUserInnerServiceSMO;

    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    @Autowired
    private IApplyRoomDiscountInnerServiceSMO applyRoomDiscountInnerServiceSMOImpl;

    @Autowired
    private IFeeDetailInnerServiceSMO iFeeDetailInnerServiceSMO;

    @Autowired
    private IComputeFeeSMO computeFeeSMOImpl;

    @Autowired
    private ICouponUserV1InnerServiceSMO couponUserV1InnerServiceSMOImpl;

    @Autowired
    private IAccountInnerServiceSMO accountInnerServiceSMOImpl;

    @Autowired
    private ICommunityV1InnerServiceSMO communityV1InnerServiceSMOImpl;

    @Autowired
    private IAccountDetailInnerServiceSMO accountDetailInnerServiceSMOImpl;

    @Autowired
    private IFeeAccountDetailServiceSMO feeAccountDetailServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        Assert.jsonObjectHaveKey(reqJson, "communityId", "????????????????????????communityId??????");
        Assert.jsonObjectHaveKey(reqJson, "cycles", "????????????????????????cycles??????");
        Assert.jsonObjectHaveKey(reqJson, "receivedAmount", "????????????????????????receivedAmount??????");
        Assert.jsonObjectHaveKey(reqJson, "feeId", "????????????????????????feeId??????");
        Assert.jsonObjectHaveKey(reqJson, "appId", "????????????????????????appId??????");

        Assert.hasLength(reqJson.getString("communityId"), "??????ID????????????");
        Assert.hasLength(reqJson.getString("cycles"), "??????????????????");
        Assert.hasLength(reqJson.getString("receivedAmount"), "????????????????????????");
        Assert.hasLength(reqJson.getString("feeId"), "??????ID????????????");
        Assert.hasLength(reqJson.getString("appId"), "appId????????????");


        //???????????? ???????????????????????????
        FeeDto feeDto = new FeeDto();
        feeDto.setFeeId(reqJson.getString("feeId"));
        feeDto.setCommunityId(reqJson.getString("communityId"));
        List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);

        Assert.listOnlyOne(feeDtos, "????????????ID??????");

        feeDto = feeDtos.get(0);

        if (FeeDto.STATE_FINISH.equals(feeDto.getState())) {
            throw new IllegalArgumentException("????????????????????????????????????");
        }

        Date endTime = feeDto.getEndTime();

        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setConfigId(feeDto.getConfigId());
        feeConfigDto.setCommunityId(reqJson.getString("communityId"));
        List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);

        if (feeConfigDtos != null && feeConfigDtos.size() == 1) {
            try {
                Date configEndTime = DateUtil.getDateFromString(feeConfigDtos.get(0).getEndTime(), DateUtil.DATE_FORMATE_STRING_A);
                configEndTime = DateUtil.stepDay(configEndTime, 5);

                Date newDate = DateUtil.stepMonth(endTime, reqJson.getInteger("cycles"));

                if (newDate.getTime() > configEndTime.getTime()) {
                    throw new IllegalArgumentException("?????????????????? ??????????????????");
                }

            } catch (Exception e) {
                logger.error("????????????????????????", e);
            }
        }

    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        logger.debug("ServiceDataFlowEvent : {}", event);

        String appId = cmdDataFlowContext.getReqHeaders().get("app-id");
        reqJson.put("appId", appId);

        FeeDto feeDto = new FeeDto();
        feeDto.setFeeId(reqJson.getString("feeId"));
        feeDto.setCommunityId(reqJson.getString("communityId"));
        List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);
        if (feeDtos == null || feeDtos.size() != 1) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "???????????????????????????????????????????????????????????????");
        }
        feeDto = feeDtos.get(0);
        reqJson.put("feeTypeCd", feeDto.getFeeTypeCd());
        reqJson.put("feeId", feeDto.getFeeId());
        Map feePriceAll = computeFeeSMOImpl.getFeePrice(feeDto);
        BigDecimal receivableAmount = new BigDecimal(feePriceAll.get("feePrice").toString());
        BigDecimal cycles = new BigDecimal(Double.parseDouble(reqJson.getString("cycles")));
        double tmpReceivableAmount = cycles.multiply(receivableAmount).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
        JSONObject paramOut = new JSONObject();
        paramOut.put("receivableAmount", tmpReceivableAmount);
        paramOut.put("oId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_oId));

        //????????????
        BigDecimal tmpReceivedAmout = new BigDecimal(tmpReceivableAmount);

        //???????????????????????????
        double discountPrice = judgeDiscount(reqJson);
        tmpReceivedAmout = tmpReceivedAmout.subtract(new BigDecimal(discountPrice)).setScale(2, BigDecimal.ROUND_HALF_EVEN);
        //2.0 ??????????????????
        double accountPrice = judgeAccount(reqJson);
        tmpReceivedAmout = tmpReceivedAmout.subtract(new BigDecimal(accountPrice)).setScale(2, BigDecimal.ROUND_HALF_EVEN);

        //3.0 ???????????????
        double couponPrice = checkCouponUser(reqJson);
        tmpReceivedAmout = tmpReceivedAmout.subtract(new BigDecimal(couponPrice)).setScale(2, BigDecimal.ROUND_HALF_EVEN);


        double receivedAmount = tmpReceivedAmout.doubleValue();
        //?????? ??????????????????????????????????????????????????????0????????????????????????0
        if (receivedAmount <= 0) {
            receivedAmount = 0.0;
        }
        paramOut.put("receivedAmount", receivedAmount);

        String feeName = getObjName(feeDto);
        paramOut.put("feeName", feeName);

        ResponseEntity<String> responseEntity = new ResponseEntity<>(paramOut.toJSONString(), HttpStatus.OK);
        reqJson.putAll(paramOut);
        CommonCache.setValue("payFeePre" + paramOut.getString("oId"), reqJson.toJSONString(), 24 * 60 * 60);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    private String getObjName(FeeDto feeDto) {
        //??????????????????
        CommunityDto communityDto = new CommunityDto();
        communityDto.setCommunityId(feeDto.getCommunityId());
        List<CommunityDto> communityDtos = communityV1InnerServiceSMOImpl.queryCommunitys(communityDto);

        Assert.listOnlyOne(communityDtos, "???????????????");

        List<FeeAttrDto> feeAttrDtos = feeDto.getFeeAttrDtos();
        if (feeAttrDtos == null || feeAttrDtos.size() < 1) {
            return communityDtos.get(0).getName() + "-" + feeDto.getFeeName();
        }

        for (FeeAttrDto feeAttrDto : feeAttrDtos) {
            if (FeeAttrDto.SPEC_CD_PAY_OBJECT_NAME.equals(feeAttrDto.getSpecCd())) {
                return communityDtos.get(0).getName() + "-" + feeAttrDto.getValue() + "-" + feeDto.getFeeName();
            }
        }

        return communityDtos.get(0).getName() + "-" + feeDto.getFeeName();
    }

    /**
     * ??????????????????
     *
     * @param reqJson
     */
    private double judgeAccount(JSONObject reqJson) {
        if (!reqJson.containsKey("deductionAmount")) {
            reqJson.put("deductionAmount", 0.0);
            return 0.0;
        }

        double deductionAmount = reqJson.getDouble("deductionAmount");
        if (deductionAmount <= 0) {
            reqJson.put("deductionAmount", 0.0);
            return 0.0;
        }

        if (!reqJson.containsKey("selectUserAccount")) {
            reqJson.put("deductionAmount", 0.0);
            return 0.0;
        }

        JSONArray selectUserAccount = reqJson.getJSONArray("selectUserAccount");
        if (selectUserAccount == null || selectUserAccount.size() < 1) {
            reqJson.put("deductionAmount", 0.0);
            return 0.0;
        }
        List<String> acctIds = new ArrayList<>();
        for (int userAccountIndex = 0; userAccountIndex < selectUserAccount.size(); userAccountIndex++) {
            acctIds.add(selectUserAccount.getJSONObject(userAccountIndex).getString("acctId"));
        }

        AccountDto accountDto = new AccountDto();
        accountDto.setAcctIds(acctIds.toArray(new String[acctIds.size()]));
        List<AccountDto> accountDtos = accountInnerServiceSMOImpl.queryAccounts(accountDto);

        if (accountDtos == null || accountDtos.size() < 1) {
            reqJson.put("deductionAmount", 0.0);
            return 0.0;
        }

        BigDecimal money = new BigDecimal(0);
        BigDecimal totalAccountAmount = new BigDecimal(0);
        for (AccountDto tmpAccountDto : accountDtos) {
            if (!StringUtil.isEmpty(tmpAccountDto.getAcctType()) && tmpAccountDto.getAcctType().equals("2004")) { //????????????
                String maximum = "";
                String deduction = "";
                for (int index = 0; index < selectUserAccount.size(); index++) {
                    JSONObject param = selectUserAccount.getJSONObject(index);
                    if (!StringUtil.isEmpty(param.getString("acctType")) && param.getString("acctType").equals("2004")) { //????????????
                        maximum = param.getString("maximumNumber");
                        deduction = param.getString("deductionProportion");
                    }
                }
                //????????????
                BigDecimal amount = new BigDecimal(tmpAccountDto.getAmount());
                //????????????????????????
                BigDecimal maximumNumber = new BigDecimal(maximum);
                //????????????????????????
                BigDecimal deductionProportion = new BigDecimal(deduction);
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
//                AccountPo accountPo = new AccountPo();
//                accountPo.setAcctId(tmpAccountDto.getAcctId());
//                accountPo.setAmount(integralAmount.toString());
//                accountInnerServiceSMOImpl.updateAccount(accountPo);
                //??????????????????
//                AccountDetailPo accountDetailPo = new AccountDetailPo();
//                accountDetailPo.setDetailId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_detailId));
//                accountDetailPo.setAcctId(tmpAccountDto.getAcctId());
//                accountDetailPo.setDetailType("2002"); //1001 ?????? 2002 ??????
//                accountDetailPo.setRelAcctId("-1");
//                accountDetailPo.setAmount(redepositAmount.toString());
//                accountDetailPo.setObjType("6006"); //6006 ?????? 7007 ??????
//                accountDetailPo.setObjId(tmpAccountDto.getObjId());
//                accountDetailPo.setOrderId("-1");
//                accountDetailPo.setbId("-1");
//                accountDetailPo.setRemark("?????????????????????");
//                accountDetailPo.setCreateTime(new Date());
//                accountDetailInnerServiceSMOImpl.saveAccountDetails(accountDetailPo);
                //???????????????????????????
                BigDecimal divide = redepositAmount.divide(deductionProportion);
                BigDecimal dedAmount = new BigDecimal(deductionAmount);
                //??????????????????
                int flag2 = divide.compareTo(dedAmount);
                BigDecimal subtract = new BigDecimal("0.00");
                //????????????????????????
                FeeAccountDetailPo feeAccountDetailPo = new FeeAccountDetailPo();
                if (flag2 == -1) { //????????????????????????????????????
                    //subtract = dedAmount.subtract(divide);
                    BigDecimal multiply = divide.multiply(deductionProportion);
                    feeAccountDetailPo.setAmount(multiply.toString()); //??????????????????
                } else if (flag < 1) { //??????????????????????????????????????????
                    //subtract = dedAmount.subtract(divide);
                    BigDecimal multiply = divide.multiply(deductionProportion);
                    feeAccountDetailPo.setAmount(multiply.toString()); //??????????????????
                } else {
                    BigDecimal multiply = dedAmount.multiply(deductionProportion);
                    feeAccountDetailPo.setAmount(multiply.toString()); //??????????????????
                }
                reqJson.put("receivedMoney", divide);
//                feeAccountDetailPo.setFadId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_fadId));
//                feeAccountDetailPo.setDetailId(accountDetailPo.getDetailId());
//                feeAccountDetailPo.setCommunityId(reqJson.getString("communityId"));
//                feeAccountDetailPo.setState("1003"); //1001 ????????? 1002 ?????????????????? 1003 ?????????????????? 1004 ???????????????
//                feeAccountDetailServiceSMOImpl.saveFeeAccountDetail(feeAccountDetailPo);
                money = divide;
            } else if (!StringUtil.isEmpty(tmpAccountDto.getAcctType()) && tmpAccountDto.getAcctType().equals("2003")) { //????????????
                //????????????
                BigDecimal amount = new BigDecimal(tmpAccountDto.getAmount());
                //??????????????????
                BigDecimal dedAmount = new BigDecimal("0.00");
                if (reqJson.containsKey("receivedMoney") && !StringUtil.isEmpty(reqJson.getString("receivedMoney"))) {
                    dedAmount = new BigDecimal(reqJson.getString("receivedMoney"));
                } else {
                    dedAmount = new BigDecimal(reqJson.getString("deductionAmount"));
                }
                int flag = amount.compareTo(dedAmount);
                BigDecimal redepositAmount = new BigDecimal("0.00");
                BigDecimal integralAmount = new BigDecimal("0.00");
                if (flag == 1) { //?????????????????????????????????????????????????????????
                    redepositAmount = dedAmount;
                    integralAmount = amount.subtract(dedAmount);
                }
                if (flag > -1) { //???????????????????????????????????????????????????????????????
                    redepositAmount = dedAmount;
                    integralAmount = amount.subtract(dedAmount);
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
//                AccountPo accountPo = new AccountPo();
//                accountPo.setAcctId(tmpAccountDto.getAcctId());
//                accountPo.setAmount(integralAmount.toString());
//                accountInnerServiceSMOImpl.updateAccount(accountPo);
                //??????????????????
//                AccountDetailPo accountDetailPo = new AccountDetailPo();
//                accountDetailPo.setDetailId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_detailId));
//                accountDetailPo.setAcctId(tmpAccountDto.getAcctId());
//                accountDetailPo.setDetailType("2002"); //1001 ?????? 2002 ??????
//                accountDetailPo.setRelAcctId("-1");
//                accountDetailPo.setAmount(redepositAmount.toString());
//                accountDetailPo.setObjType("6006"); //6006 ?????? 7007 ??????
//                accountDetailPo.setObjId(tmpAccountDto.getObjId());
//                accountDetailPo.setOrderId("-1");
//                accountDetailPo.setbId("-1");
//                accountDetailPo.setRemark("???????????????????????????");
//                accountDetailPo.setCreateTime(new Date());
//                accountDetailInnerServiceSMOImpl.saveAccountDetails(accountDetailPo);
                //????????????????????????
//                FeeAccountDetailPo feeAccountDetailPo = new FeeAccountDetailPo();
//                feeAccountDetailPo.setFadId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_fadId));
//                feeAccountDetailPo.setDetailId(accountDetailPo.getDetailId());
//                feeAccountDetailPo.setCommunityId(reqJson.getString("communityId"));
//                feeAccountDetailPo.setState("1002"); //1001 ????????? 1002 ?????????????????? 1003 ?????????????????? 1004 ???????????????
//                feeAccountDetailPo.setAmount(redepositAmount.toString()); //??????????????????
//                feeAccountDetailServiceSMOImpl.saveFeeAccountDetail(feeAccountDetailPo);
                money = money.add(redepositAmount);
                /*int flag2 = money.compareTo(amount);
                if (flag2 == 1) { //??????????????????????????????
                    money = money.subtract(amount);
                }
                if (flag2 > -1) { //????????????????????????????????????
                    money = money.subtract(amount);
                }
                if (flag2 == -1) { //??????????????????????????????
                    money = new BigDecimal(0);
                }
                if (flag2 < 1) { //????????????????????????????????????
                    money = new BigDecimal(0);
                }
                if (flag2 == 0) { //??????????????????????????????
                    money = new BigDecimal(0);
                }*/
            }
//            totalAccountAmount = totalAccountAmount.add(new BigDecimal(tmpAccountDto.getAmount()));
        }

       /* double tmpDeductionAmount = totalAccountAmount.subtract(new BigDecimal(deductionAmount)).doubleValue();
        if (tmpDeductionAmount < 0) {
            reqJson.put("deductionAmount", totalAccountAmount.doubleValue());
            reqJson.put("selectUserAccount", BeanConvertUtil.beanCovertJSONArray(accountDtos));
            return totalAccountAmount.doubleValue();
        }
        reqJson.put("deductionAmount", deductionAmount);
        reqJson.put("selectUserAccount", BeanConvertUtil.beanCovertJSONArray(accountDtos));
        return deductionAmount;*/
        reqJson.put("deductionAmount", money.doubleValue());
        reqJson.put("selectUserAccount", BeanConvertUtil.beanCovertJSONArray(accountDtos));
        return money.doubleValue();
    }

    private double checkCouponUser(JSONObject paramObj) {
        JSONArray couponList = paramObj.getJSONArray("couponList");
        BigDecimal couponPrice = new BigDecimal(0.0);
        List<String> couponIds = new ArrayList<String>();

        if (couponList == null || couponList.size() < 1) {
            paramObj.put("couponPrice", couponPrice.doubleValue());
            paramObj.put("couponUserDtos", new JSONArray()); //???????????????
            return couponPrice.doubleValue();
        }
        for (int couponIndex = 0; couponIndex < couponList.size(); couponIndex++) {
            couponIds.add(couponList.getJSONObject(couponIndex).getString("couponId"));
        }
        CouponUserDto couponUserDto = new CouponUserDto();
        couponUserDto.setCouponIds(couponIds.toArray(new String[couponIds.size()]));
        List<CouponUserDto> couponUserDtos = couponUserV1InnerServiceSMOImpl.queryCouponUsers(couponUserDto);
        if (couponUserDtos == null || couponUserDtos.size() < 1) {
            paramObj.put("couponPrice", couponPrice.doubleValue());
            return couponPrice.doubleValue();
        }
        for (CouponUserDto couponUser : couponUserDtos) {
            //?????????????????????????????????
            if (couponUser.getEndTime().compareTo(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_B)) >= 0) {
                couponPrice = couponPrice.add(new BigDecimal(Double.parseDouble(couponUser.getActualPrice())));
            }
        }
        paramObj.put("couponPrice", couponPrice.doubleValue());
        paramObj.put("couponUserDtos", BeanConvertUtil.beanCovertJSONArray(couponUserDtos));
        return couponPrice.doubleValue();
    }


    private double judgeDiscount(JSONObject paramObj) {
        FeeDetailDto feeDetailDto = new FeeDetailDto();
        feeDetailDto.setCommunityId(paramObj.getString("communityId"));
        feeDetailDto.setFeeId(paramObj.getString("feeId"));
        feeDetailDto.setCycles(paramObj.getString("cycles"));
        feeDetailDto.setPayerObjId(paramObj.getString("payerObjId"));
        feeDetailDto.setPayerObjType(paramObj.getString("payerObjType"));
        String endTime = paramObj.getString("endTime");  //????????????????????????
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            feeDetailDto.setStartTime(simpleDateFormat.parse(endTime));
        } catch (ParseException e) {
            throw new CmdException(e.getLocalizedMessage());
        }

        feeDetailDto.setRow(20);
        feeDetailDto.setPage(1);
        List<ComputeDiscountDto> computeDiscountDtos = feeDiscountInnerServiceSMOImpl.computeDiscount(feeDetailDto);

        if (computeDiscountDtos == null || computeDiscountDtos.size() < 1) {
            paramObj.put("discountPrice", 0.0);
            return 0.0;
        }
        BigDecimal discountPrice = new BigDecimal(0);
        for (ComputeDiscountDto computeDiscountDto : computeDiscountDtos) {
            discountPrice = discountPrice.add(new BigDecimal(computeDiscountDto.getDiscountPrice()));
        }
        paramObj.put("discountPrice", discountPrice.doubleValue());
        paramObj.put("computeDiscountDtos", BeanConvertUtil.beanCovertJSONArray(computeDiscountDtos));
        return discountPrice.doubleValue();
    }
}
