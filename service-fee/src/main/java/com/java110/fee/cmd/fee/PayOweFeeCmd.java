package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.DataFlowContext;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.core.smo.IComputeFeeSMO;
import com.java110.dto.app.AppDto;
import com.java110.dto.fee.FeeAttrDto;
import com.java110.dto.fee.FeeDetailDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.feeReceipt.FeeReceiptDetailDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.repair.RepairDto;
import com.java110.dto.repair.RepairUserDto;
import com.java110.intf.community.*;
import com.java110.intf.fee.*;
import com.java110.po.fee.PayFeeDetailPo;
import com.java110.po.fee.PayFeePo;
import com.java110.po.feeReceipt.FeeReceiptPo;
import com.java110.po.feeReceiptDetail.FeeReceiptDetailPo;
import com.java110.po.owner.RepairPoolPo;
import com.java110.po.owner.RepairUserPo;
import com.java110.utils.constant.BusinessTypeConstant;
import com.java110.utils.constant.CommonConstant;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;

@Java110Cmd(serviceCode = "fee.payOweFee")
public class PayOweFeeCmd extends Cmd {
    private static Logger logger = LoggerFactory.getLogger(PayOweFeeCmd.class);
    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;
    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IFeeAttrInnerServiceSMO feeAttrInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    @Autowired
    private IFeeReceiptInnerServiceSMO feeReceiptInnerServiceSMOImpl;

    @Autowired
    private IFeeReceiptDetailInnerServiceSMO feeReceiptDetailInnerServiceSMOImpl;

    @Autowired
    private IRepairUserInnerServiceSMO repairUserInnerServiceSMO;

    @Autowired
    private IRepairInnerServiceSMO repairInnerServiceSMO;

    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    @Autowired
    private IPayFeeDetailV1InnerServiceSMO payFeeDetailV1InnerServiceSMOImpl;


    @Autowired
    private IRepairPoolV1InnerServiceSMO repairPoolV1InnerServiceSMOImpl;

    @Autowired
    private IRepairUserV1InnerServiceSMO repairUserV1InnerServiceSMOImpl;

    @Autowired
    private IComputeFeeSMO computeFeeSMOImpl;


    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        Assert.hasKey(reqJson, "fees", "????????????????????????????????????");

        JSONArray fees = reqJson.getJSONArray("fees");

        JSONObject feeObject = null;

        for (int feeIndex = 0; feeIndex < fees.size(); feeIndex++) {
            feeObject = fees.getJSONObject(feeIndex);
            Assert.hasKeyAndValue(feeObject, "feeId", "?????????????????????");
            Assert.hasKeyAndValue(feeObject, "startTime", "?????????????????????");
            Assert.hasKeyAndValue(feeObject, "endTime", "?????????????????????");
            Assert.hasKeyAndValue(feeObject, "receivedAmount", "?????????????????????");
        }
    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext dataFlowContext, JSONObject paramObj) throws CmdException, ParseException {
        logger.info("======??????????????????======???" + JSONArray.toJSONString(paramObj));

        //??????????????????
        List<FeeReceiptPo> feeReceiptPos = new ArrayList<>();
        List<FeeReceiptDetailPo> feeReceiptDetailPos = new ArrayList<>();
        JSONArray fees = paramObj.getJSONArray("fees");
        JSONObject feeObj = null;
        String appId = dataFlowContext.getReqHeaders().get("app-id");

        for (int feeIndex = 0; feeIndex < fees.size(); feeIndex++) {
            feeObj = fees.getJSONObject(feeIndex);
            feeObj.put("communityId", paramObj.getString("communityId"));
            if(paramObj.containsKey("oId")) {
                feeObj.put("oId", paramObj.getString("oId"));
            }
            String remark = paramObj.getString("remark");
            feeObj.put("remark", remark);
            if (!feeObj.containsKey("primeRate") && AppDto.OWNER_WECHAT_PAY.equals(appId)) {  //?????????????????????
                feeObj.put("primeRate", "5");
                feeObj.put("remark", "?????????????????????");
            }

            getFeeReceiptDetailPo(dataFlowContext, feeObj, feeReceiptDetailPos, feeReceiptPos);
        }

        //?????????????????? ???????????????????????? ?????????????????????????????????????????? ??????????????? ????????????????????? ???????????? ???????????? ???????????????????????????????????? ?????????????????????
//        feeReceiptDetailInnerServiceSMOImpl.saveFeeReceiptDetails(feeReceiptDetailPos);
//
//        feeReceiptInnerServiceSMOImpl.saveFeeReceipts(feeReceiptPos);

        //????????????ID ??????????????????
        List<String> detailIds = new ArrayList<>();

        for (FeeReceiptDetailPo feeReceiptDetailPo : feeReceiptDetailPos) {
            detailIds.add(feeReceiptDetailPo.getDetailId());
        }

        FeeReceiptDetailDto feeReceiptDetailDto = new FeeReceiptDetailDto();
        feeReceiptDetailDto.setDetailIds(detailIds.toArray(new String[detailIds.size()]));
        feeReceiptDetailDto.setCommunityId(paramObj.getString("communityId"));
        try {
            Thread.currentThread().sleep(2000);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        List<FeeReceiptDetailDto> feeReceiptDetailDtos = feeReceiptDetailInnerServiceSMOImpl.queryFeeReceiptDetails(feeReceiptDetailDto);


        dataFlowContext.setResponseEntity(ResultVo.createResponseEntity(feeReceiptDetailDtos));
    }

    private void getFeeReceiptDetailPo(ICmdDataFlowContext dataFlowContext, JSONObject paramObj,
                                       List<FeeReceiptDetailPo> feeReceiptDetailPos,
                                       List<FeeReceiptPo> feeReceiptPos) {
        int flag = 0;
        if (!paramObj.containsKey("primeRate")) {
            paramObj.put("primeRate", "6");
        }
        String appId = dataFlowContext.getReqHeaders().get("app-id");
        logger.info("======????????????======???" + appId + "+======+" + paramObj.containsKey("primeRate") + "======:" + JSONArray.toJSONString(dataFlowContext));
        if (AppDto.OWNER_WECHAT_PAY.equals(appId)
                && FeeDetailDto.PRIME_REATE_WECHAT.equals(paramObj.getString("primeRate"))) {  //????????????????????????????????????????????????????????????????????????
            paramObj.put("remark", "?????????????????????");
        } else if (AppDto.OWNER_WECHAT_PAY.equals(appId)
                && FeeDetailDto.PRIME_REATE_WECHAT_APP.equals(paramObj.getString("primeRate"))) {
            paramObj.put("remark", "?????????????????????");
        }
        paramObj.put("state", "1400");
        addOweFeeDetail(paramObj, dataFlowContext, feeReceiptDetailPos, feeReceiptPos);
        modifyOweFee(paramObj, dataFlowContext);

        //???????????????????????????ID
        FeeAttrDto feeAttrDto = new FeeAttrDto();
        feeAttrDto.setCommunityId(paramObj.getString("communityId"));
        feeAttrDto.setFeeId(paramObj.getString("feeId"));
        feeAttrDto.setSpecCd(FeeAttrDto.SPEC_CD_REPAIR);
        List<FeeAttrDto> feeAttrDtos = feeAttrInnerServiceSMOImpl.queryFeeAttrs(feeAttrDto);

        //?????? ????????????
        if (feeAttrDtos != null && feeAttrDtos.size() > 0) {

            RepairPoolPo repairPoolPo = new RepairPoolPo();
            repairPoolPo.setRepairId(feeAttrDtos.get(0).getValue());
            repairPoolPo.setCommunityId(paramObj.getString("communityId"));
            repairPoolPo.setState(RepairDto.STATE_APPRAISE);
            flag = repairPoolV1InnerServiceSMOImpl.updateRepairPoolNew(repairPoolPo);
            if(flag < 1){
                throw new CmdException("????????????");
            }
        }
        //?????? ??????????????????
        if (feeAttrDtos != null && feeAttrDtos.size() > 0) {

            RepairDto repairDto = new RepairDto();
            repairDto.setRepairId(feeAttrDtos.get(0).getValue());
            //??????????????????
            List<RepairDto> repairDtos = repairInnerServiceSMO.queryRepairs(repairDto);
            Assert.listOnlyOne(repairDtos, "?????????????????????");
            //??????????????????
            String repairChannel = repairDtos.get(0).getRepairChannel();
            RepairUserDto repairUserDto = new RepairUserDto();
            repairUserDto.setRepairId(feeAttrDtos.get(0).getValue());
            repairUserDto.setState(RepairUserDto.STATE_PAY_FEE);
            //??????????????????????????????
            List<RepairUserDto> repairUserDtoList = repairUserInnerServiceSMO.queryRepairUsers(repairUserDto);
            Assert.listOnlyOne(repairUserDtoList, "???????????????");
            RepairUserPo repairUserPo = new RepairUserPo();
            repairUserPo.setRuId(repairUserDtoList.get(0).getRuId());
            if (repairChannel.equals("Z")) {  //???????????????????????????????????????????????????????????????????????????????????????
                repairUserPo.setState(RepairUserDto.STATE_FINISH_PAY_FEE);
                //????????????????????????????????????????????????
                repairUserPo.setEndTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
                repairUserPo.setContext("?????????" + paramObj.getString("receivedAmount") + "???");
                //?????????????????????
                RepairUserPo repairUser = new RepairUserPo();
                repairUser.setRuId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_ruId));
                repairUser.setStartTime(repairUserPo.getEndTime());
                repairUser.setState(RepairUserDto.STATE_EVALUATE);
                repairUser.setContext("?????????");
                repairUser.setCommunityId(paramObj.getString("communityId"));
                repairUser.setCreateTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
                repairUser.setRepairId(repairUserDtoList.get(0).getRepairId());
                repairUser.setStaffId(repairUserDtoList.get(0).getStaffId());
                repairUser.setStaffName(repairUserDtoList.get(0).getStaffName());
                repairUser.setPreStaffId(repairUserDtoList.get(0).getStaffId());
                repairUser.setPreStaffName(repairUserDtoList.get(0).getStaffName());
                repairUser.setPreRuId(repairUserDtoList.get(0).getRuId());
                repairUser.setRepairEvent("auditUser");
                flag = repairUserV1InnerServiceSMOImpl.saveRepairUserNew(repairUserPo);
                if(flag < 1){
                    throw new CmdException("????????????");
                }
            } else {  //?????????????????????????????????????????????????????????????????????
                repairUserPo.setState(RepairUserDto.STATE_FINISH_PAY_FEE);
                //????????????????????????????????????????????????
                repairUserPo.setEndTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
                repairUserPo.setContext("?????????" + paramObj.getString("receivedAmount") + "???");
                flag = repairUserV1InnerServiceSMOImpl.updateRepairUserNew(repairUserPo);
                if(flag < 1){
                    throw new CmdException("????????????");
                }
            }
        }


    }

    /**
     * ??????????????????
     *
     * @param paramInJson     ???????????????????????????
     * @param dataFlowContext ???????????????
     * @return ?????????????????????????????????
     */
    public void modifyOweFee(JSONObject paramInJson, ICmdDataFlowContext dataFlowContext) {

        JSONObject businessFee = new JSONObject();
        FeeDto feeInfo = (FeeDto) paramInJson.get("feeInfo");
        Map feeMap = BeanConvertUtil.beanCovertMap(feeInfo);
        feeMap.put("startTime", DateUtil.getFormatTimeString(feeInfo.getStartTime(), DateUtil.DATE_FORMATE_STRING_A));
        feeMap.put("endTime", paramInJson.getString("endTime"));
        feeMap.put("cycles", paramInJson.getString("cycles"));
        feeMap.put("configEndTime", feeInfo.getConfigEndTime());
        if (FeeDto.FEE_FLAG_ONCE.equals(feeInfo.getFeeFlag())) { //????????????
            feeMap.put("state", FeeDto.STATE_FINISH);
        }
        Date maxEndTime = feeInfo.getConfigEndTime();
        if (!FeeDto.FEE_FLAG_CYCLE.equals(feeInfo.getFeeFlag())) {
            maxEndTime = feeInfo.getDeadlineTime();
        }
        if(maxEndTime != null) { //??????????????????????????????
            Date endTime = DateUtil.getDateFromStringA(paramInJson.getString("endTime"));
            if (endTime.getTime() >= maxEndTime.getTime()) {
                feeMap.put("state", FeeDto.STATE_FINISH);
            }
        }

        businessFee.putAll(feeMap);
        PayFeePo payFeePo =  BeanConvertUtil.covertBean(businessFee,PayFeePo.class);
        int flag = payFeeV1InnerServiceSMOImpl.updatePayFee(payFeePo);
        if (flag < 1) {
            throw new CmdException("????????????");
        }
    }

    public void addOweFeeDetail(JSONObject paramInJson, ICmdDataFlowContext dataFlowContext,
                                      List<FeeReceiptDetailPo> feeReceiptDetailPos,
                                      List<FeeReceiptPo> feeReceiptPos) {

        JSONObject businessFeeDetail = new JSONObject();
        businessFeeDetail.putAll(paramInJson);
        businessFeeDetail.put("detailId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_detailId));
        businessFeeDetail.put("primeRate", paramInJson.getString("primeRate"));
        //?????? ????????????
        FeeDto feeDto = new FeeDto();
        feeDto.setFeeId(paramInJson.getString("feeId"));
        feeDto.setCommunityId(paramInJson.getString("communityId"));
        List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);
        if (feeDtos == null || feeDtos.size() != 1) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "???????????????????????????????????????????????????????????????");
        }
        if (!businessFeeDetail.containsKey("state") || StringUtil.isEmpty(businessFeeDetail.getString("state"))) {
            businessFeeDetail.put("state", "1400");
        }
        feeDto = feeDtos.get(0);
        businessFeeDetail.put("startTime", paramInJson.getString("startTime"));
        BigDecimal cycles = null;
        Map feePriceAll = computeFeeSMOImpl.getFeePrice(feeDto);
        BigDecimal feePrice = new BigDecimal(feePriceAll.get("feePrice").toString());
        Date endTime = feeDto.getEndTime();
        Calendar endCalender = Calendar.getInstance();
        endCalender.setTime(endTime);
        BigDecimal receivedAmount = new BigDecimal(Double.parseDouble(paramInJson.getString("receivedAmount")));
        cycles = receivedAmount.divide(feePrice, 4, BigDecimal.ROUND_HALF_EVEN);
        businessFeeDetail.put("cycles", receivedAmount.divide(feePrice, 2, BigDecimal.ROUND_HALF_UP).doubleValue());

        if(!paramInJson.containsKey("receivableAmount") || StringUtil.isEmpty(paramInJson.getString("receivableAmount"))){
            paramInJson.put("receivableAmount",paramInJson.getString("receivedAmount"));
        }

        businessFeeDetail.put("receivableAmount", paramInJson.getString("receivableAmount"));
        businessFeeDetail.put("receivedAmount", paramInJson.getString("receivedAmount"));
        businessFeeDetail.put("payableAmount", paramInJson.getString("receivedAmount"));
        businessFeeDetail.put("endTime", paramInJson.getString("endTime"));
        paramInJson.put("feeInfo", feeDto);
        paramInJson.put("cycles", cycles.doubleValue());
        PayFeeDetailPo payFeeDetailPo = BeanConvertUtil.covertBean(businessFeeDetail, PayFeeDetailPo.class);
        if(paramInJson.containsKey("oId")) {
            payFeeDetailPo.setPayOrderId(paramInJson.getString("oId"));
        }else{
            payFeeDetailPo.setPayOrderId(payFeeDetailPo.getDetailId());

        }

        int flag = payFeeDetailV1InnerServiceSMOImpl.savePayFeeDetailNew(payFeeDetailPo);

        if (flag < 1) {
            throw new CmdException("??????????????????");
        }

        paramInJson.put("detailId", businessFeeDetail.getString("detailId"));
        FeeReceiptPo feeReceiptPo = new FeeReceiptPo();
        FeeReceiptDetailPo feeReceiptDetailPo = new FeeReceiptDetailPo();
        feeReceiptDetailPo.setAmount(businessFeeDetail.getString("receivedAmount"));
        feeReceiptDetailPo.setCommunityId(feeDto.getCommunityId());
        feeReceiptDetailPo.setCycle(businessFeeDetail.getString("cycles"));
        feeReceiptDetailPo.setDetailId(businessFeeDetail.getString("detailId"));
        feeReceiptDetailPo.setEndTime(businessFeeDetail.getString("endTime"));
        feeReceiptDetailPo.setFeeId(feeDto.getFeeId());
        feeReceiptDetailPo.setFeeName(StringUtil.isEmpty(feeDto.getImportFeeName()) ? feeDto.getFeeName() : feeDto.getImportFeeName());
        feeReceiptDetailPo.setStartTime(businessFeeDetail.getString("startTime"));
        feeReceiptDetailPo.setReceiptId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_receiptId));

        computeFeeSMOImpl.freshFeeReceiptDetail(feeDto, feeReceiptDetailPo);
        //??????????????????
        OwnerDto ownerDto = computeFeeSMOImpl.getFeeOwnerDto(feeDto);

        feeReceiptDetailPos.add(feeReceiptDetailPo);
        feeReceiptPo.setAmount(feeReceiptDetailPo.getAmount());
        feeReceiptPo.setCommunityId(feeReceiptDetailPo.getCommunityId());
        feeReceiptPo.setReceiptId(feeReceiptDetailPo.getReceiptId());
        feeReceiptPo.setObjType(feeDto.getPayerObjType());
        feeReceiptPo.setObjId(feeDto.getPayerObjId());
        feeReceiptPo.setObjName(computeFeeSMOImpl.getFeeObjName(feeDto));
        feeReceiptPo.setPayObjId(ownerDto.getOwnerId());
        feeReceiptPo.setPayObjName(ownerDto.getName());
        feeReceiptPos.add(feeReceiptPo);
    }

}
