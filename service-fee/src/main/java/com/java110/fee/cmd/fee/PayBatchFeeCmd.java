package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.core.smo.IComputeFeeSMO;
import com.java110.dto.fee.FeeAttrDto;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDetailDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.dto.repair.RepairDto;
import com.java110.dto.repair.RepairUserDto;
import com.java110.intf.community.*;
import com.java110.intf.fee.*;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.po.car.OwnerCarPo;
import com.java110.po.fee.PayFeeDetailPo;
import com.java110.po.fee.PayFeePo;
import com.java110.po.owner.RepairPoolPo;
import com.java110.po.owner.RepairUserPo;
import com.java110.utils.constant.FeeFlagTypeConstant;
import com.java110.utils.constant.FeeConfigConstant;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.lock.DistributedLock;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Java110Cmd(serviceCode = "fee.payBatchFee")
public class PayBatchFeeCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(PayBatchFeeCmd.class);


    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;
    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;
    @Autowired
    private IPayFeeDetailV1InnerServiceSMO payFeeDetailNewV1InnerServiceSMOImpl;

    @Autowired
    private IFeeAttrInnerServiceSMO feeAttrInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;
    @Autowired
    private IOwnerCarNewV1InnerServiceSMO ownerCarNewV1InnerServiceSMOImpl;

    @Autowired
    private IFeeReceiptDetailInnerServiceSMO feeReceiptDetailInnerServiceSMOImpl;

    @Autowired
    private IRepairUserInnerServiceSMO repairUserInnerServiceSMO;

    @Autowired
    private IRepairInnerServiceSMO repairInnerServiceSMO;

    @Autowired
    private IApplyRoomDiscountInnerServiceSMO applyRoomDiscountInnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    @Autowired
    private IRepairPoolV1InnerServiceSMO repairPoolNewV1InnerServiceSMOImpl;
    @Autowired
    private IRepairUserV1InnerServiceSMO repairUserNewV1InnerServiceSMOImpl;

    @Autowired
    private IComputeFeeSMO computeFeeSMOImpl;


    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        Assert.jsonObjectHaveKey(reqJson, "communityId", "????????????????????????communityId??????");

        if (!reqJson.containsKey("fees")) {
            throw new CmdException("???????????????");
        }

        JSONArray fees = reqJson.getJSONArray("fees");

        if (fees == null || fees.size() < 1) {
            throw new CmdException("???????????????");
        }
        JSONObject paramInObj = null;
        for (int feeIndex = 0; feeIndex < fees.size(); feeIndex++) {
            paramInObj = fees.getJSONObject(feeIndex);
            //???????????? ???????????????????????????
            FeeDto feeDto = new FeeDto();
            feeDto.setFeeId(paramInObj.getString("feeId"));
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
            feeConfigDto.setCommunityId(paramInObj.getString("communityId"));
            List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);
            if (feeConfigDtos == null || feeConfigDtos.size() != 1) {
                throw new IllegalArgumentException("??????????????????");
            }
            Date maxEndTime = feeDtos.get(0).getDeadlineTime();
            if (!FeeDto.FEE_FLAG_ONCE.equals(feeConfigDtos.get(0).getFeeFlag())) {
                try {
                    maxEndTime = DateUtil.getDateFromString(feeConfigDtos.get(0).getEndTime(), DateUtil.DATE_FORMATE_STRING_A);
                } catch (ParseException e) {
                    logger.error("????????????????????????", e);
                }
                Date newDate = DateUtil.stepMonth(endTime, paramInObj.getInteger("cycles") - 1);
                if (newDate.getTime() > maxEndTime.getTime()) {
                    throw new IllegalArgumentException("?????????????????? ??????????????????");
                }
            }

        }
    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        JSONArray fees = reqJson.getJSONArray("fees");
        JSONObject paramInObj = null;
        JSONArray datas = new JSONArray();
        for (int feeIndex = 0; feeIndex < fees.size(); feeIndex++) {
            try {
                paramInObj = fees.getJSONObject(feeIndex);
                doDeal(paramInObj, reqJson.getString("communityId"), cmdDataFlowContext);
            } catch (Exception e) {
                logger.error("????????????", e);
                throw new CmdException(e.getMessage());
            }
            datas.add(paramInObj.getString("detailId"));
        }

        cmdDataFlowContext.setResponseEntity(ResultVo.createResponseEntity(datas));
    }

    private void doDeal(JSONObject paramObj, String communityId, ICmdDataFlowContext cmdDataFlowContext) throws Exception {
        paramObj.put("communityId", communityId);
        //??????????????????
        //???????????????
        PayFeePo payFeePo = null;
        String requestId = DistributedLock.getLockUUID();
        String key = this.getClass().getSimpleName() + paramObj.get("feeId");
        try {
            DistributedLock.waitGetDistributedLock(key, requestId);
            JSONObject feeDetail = addFeeDetail(paramObj);
            PayFeeDetailPo payFeeDetailPo = BeanConvertUtil.covertBean(feeDetail, PayFeeDetailPo.class);
            payFeeDetailPo.setPayOrderId(payFeeDetailPo.getDetailId());
            int flag = payFeeDetailNewV1InnerServiceSMOImpl.savePayFeeDetailNew(payFeeDetailPo);
            if (flag < 1) {
                throw new CmdException("????????????");
            }
            JSONObject fee = modifyFee(paramObj);
            payFeePo = BeanConvertUtil.covertBean(fee, PayFeePo.class);

            flag = payFeeV1InnerServiceSMOImpl.updatePayFee(payFeePo);
            if (flag < 1) {
                throw new CmdException("????????????");
            }
        } finally {
            DistributedLock.releaseDistributedLock(requestId, key);
        }
        //????????????
        updateOwnerCarEndTime(payFeePo, paramObj);

        //???????????????
        doDealRepairOrder(paramObj);

    }

    /**
     * ???????????????
     *
     * @param paramObj
     */
    private void doDealRepairOrder(JSONObject paramObj) {
        int flag;//???????????????????????????ID
        FeeAttrDto feeAttrDto = new FeeAttrDto();
        feeAttrDto.setCommunityId(paramObj.getString("communityId"));
        feeAttrDto.setFeeId(paramObj.getString("feeId"));
        feeAttrDto.setSpecCd(FeeAttrDto.SPEC_CD_REPAIR);
        List<FeeAttrDto> feeAttrDtos = feeAttrInnerServiceSMOImpl.queryFeeAttrs(feeAttrDto);
        //?????? ????????????
        if (feeAttrDtos != null && feeAttrDtos.size() > 0) {
            RepairDto repairDto = new RepairDto();
            repairDto.setRepairId(feeAttrDtos.get(0).getValue());
            //??????????????????
            List<RepairDto> repairDtos = repairInnerServiceSMO.queryRepairs(repairDto);
            Assert.listOnlyOne(repairDtos, "?????????????????????");
            //??????????????????
            String repairChannel = repairDtos.get(0).getRepairChannel();
            RepairPoolPo repairPoolPo = new RepairPoolPo();
            repairPoolPo.setRepairId(feeAttrDtos.get(0).getValue());
            repairPoolPo.setCommunityId(paramObj.getString("communityId"));
            if (repairChannel.equals("Z")) { //??????????????????????????????????????????????????????
                repairPoolPo.setState(RepairDto.STATE_APPRAISE);
            } else { //?????????????????????????????????????????????????????????????????????
                repairPoolPo.setState(RepairDto.STATE_RETURN_VISIT);
            }
            flag = repairPoolNewV1InnerServiceSMOImpl.updateRepairPoolNew(repairPoolPo);
            if (flag < 1) {
                throw new CmdException("?????????????????????????????????");
            }

            repairDto = new RepairDto();
            repairDto.setRepairId(feeAttrDtos.get(0).getValue());
            //??????????????????
            repairDtos = repairInnerServiceSMO.queryRepairs(repairDto);

            Assert.listOnlyOne(repairDtos, "?????????????????????");
            //??????????????????
            repairChannel = repairDtos.get(0).getRepairChannel();
            RepairUserDto repairUserDto = new RepairUserDto();
            repairUserDto.setRepairId(feeAttrDtos.get(0).getValue());
            repairUserDto.setState(RepairUserDto.STATE_PAY_FEE);
            //??????????????????????????????
            List<RepairUserDto> repairUserDtoList = repairUserInnerServiceSMO.queryRepairUsers(repairUserDto);
            Assert.listOnlyOne(repairUserDtoList, "???????????????");
            RepairUserPo repairUserPo = new RepairUserPo();
            repairUserPo.setRuId(repairUserDtoList.get(0).getRuId());
            if ("Z".equals(repairChannel)) {  //???????????????????????????????????????????????????????????????????????????????????????
                repairUserPo.setState(RepairUserDto.STATE_FINISH_PAY_FEE);
                //????????????????????????????????????????????????
                repairUserPo.setEndTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
                repairUserPo.setContext("?????????" + paramObj.getString("feePrice") + "???");
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
                flag = repairUserNewV1InnerServiceSMOImpl.saveRepairUserNew(repairUser);
                if (flag < 1) {
                    throw new CmdException("?????????????????????????????????");
                }
            } else {  //?????????????????????????????????????????????????????????????????????
                repairUserPo.setState(RepairUserDto.STATE_FINISH_PAY_FEE);
                //????????????????????????????????????????????????
                repairUserPo.setEndTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
                repairUserPo.setContext("?????????" + paramObj.getString("feePrice") + "???");
            }
            flag = repairUserNewV1InnerServiceSMOImpl.updateRepairUserNew(repairUserPo);
            if (flag < 1) {
                throw new CmdException("?????????????????????????????????");
            }
        }
    }

    /**
     * ???????????? ????????????
     *
     * @param payFeePo
     * @param paramObj
     * @throws ParseException
     */
    private void updateOwnerCarEndTime(PayFeePo payFeePo, JSONObject paramObj) throws ParseException {
        FeeDto feeInfo = (FeeDto) paramObj.get("feeInfo");
        int flag;//????????????????????????
        if (!FeeDto.PAYER_OBJ_TYPE_CAR.equals(feeInfo.getPayerObjType())) {
            return;
        }
        Date feeEndTime = DateUtil.getDateFromString(payFeePo.getEndTime(), DateUtil.DATE_FORMATE_STRING_A);
        OwnerCarDto ownerCarDto = new OwnerCarDto();
        ownerCarDto.setCommunityId(feeInfo.getCommunityId());
        ownerCarDto.setCarId(feeInfo.getPayerObjId());
        ownerCarDto.setCarTypeCd("1001"); //????????????
        List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);
        Assert.listOnlyOne(ownerCarDtos, "?????????????????????");
        //????????????id
        String psId = ownerCarDtos.get(0).getPsId();
        //??????????????????(1001 ???????????????2002 ????????????  3003 ????????????)
        String carState = ownerCarDtos.get(0).getState();
        if (!StringUtil.isEmpty(psId) && !StringUtil.isEmpty(carState) && carState.equals("3003")) {
            ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
            parkingSpaceDto.setPsId(psId);
            List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);
            Assert.listOnlyOne(parkingSpaceDtos, "???????????????????????????");
            //??????????????????(?????? S????????? H ????????? F)
            String state = parkingSpaceDtos.get(0).getState();
            if (!StringUtil.isEmpty(state) && !state.equals("F")) {
                throw new IllegalArgumentException("???????????????????????????????????????");
            }
        }
        //??????????????????
        for (OwnerCarDto tmpOwnerCarDto : ownerCarDtos) {
            if (tmpOwnerCarDto.getEndTime().getTime() >= feeEndTime.getTime()) {
                continue;
            }
            OwnerCarPo ownerCarPo = new OwnerCarPo();
            ownerCarPo.setMemberId(tmpOwnerCarDto.getMemberId());
            ownerCarPo.setEndTime(DateUtil.getFormatTimeString(feeEndTime, DateUtil.DATE_FORMATE_STRING_A));
            flag = ownerCarNewV1InnerServiceSMOImpl.updateOwnerCarNew(ownerCarPo);
            if (flag < 1) {
                throw new CmdException("????????????");
            }
        }
    }

    public JSONObject addFeeDetail(JSONObject paramInJson) {
        JSONObject businessFeeDetail = new JSONObject();
        businessFeeDetail.putAll(paramInJson);
        businessFeeDetail.put("detailId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_detailId));
        //????????????
        businessFeeDetail.put("primeRate", paramInJson.getString("primeRate"));
        //?????? ????????????
        FeeDto feeDto = new FeeDto();
        feeDto.setFeeId(paramInJson.getString("feeId"));
        feeDto.setCommunityId(paramInJson.getString("communityId"));
        List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);
        if (feeDtos == null || feeDtos.size() != 1) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "???????????????????????????????????????????????????????????????");
        }
        businessFeeDetail.put("state", FeeDetailDto.STATE_NORMAL);

        feeDto = feeDtos.get(0);
        businessFeeDetail.put("startTime", DateUtil.getFormatTimeString(feeDto.getEndTime(), DateUtil.DATE_FORMATE_STRING_A));
        int hours = 0;
        Date targetEndTime = null;
        BigDecimal cycles = null;
        Map feePriceAll = computeFeeSMOImpl.getFeePrice(feeDto);
        BigDecimal feePrice = new BigDecimal(feePriceAll.get("feePrice").toString());

        targetEndTime = computeFeeSMOImpl.getFeeEndTimeByCycles(feeDto, paramInJson.getString("cycles"));
        cycles = new BigDecimal(Double.parseDouble(paramInJson.getString("cycles")));
        double tmpReceivableAmount = cycles.multiply(feePrice).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
        businessFeeDetail.put("receivableAmount", tmpReceivableAmount);
        businessFeeDetail.put("payableAmount", tmpReceivableAmount);

        businessFeeDetail.put("endTime", DateUtil.getFormatTimeString(targetEndTime, DateUtil.DATE_FORMATE_STRING_A));
        paramInJson.put("feeInfo", feeDto);
        paramInJson.put("detailId", businessFeeDetail.getString("detailId"));

        return businessFeeDetail;
    }

    /**
     * ??????????????????
     *
     * @param paramInJson ???????????????????????????
     * @return ?????????????????????????????????
     */
    public JSONObject modifyFee(JSONObject paramInJson) {
        FeeDto feeInfo = (FeeDto) paramInJson.get("feeInfo");
        Date endTime = feeInfo.getEndTime();
        Calendar endCalender = Calendar.getInstance();
        endCalender.setTime(endTime);
        endCalender.add(Calendar.MONTH, Integer.parseInt(paramInJson.getString("cycles")));
        if (FeeDto.FEE_FLAG_ONCE.equals(feeInfo.getFeeFlag())) {
            if (feeInfo.getDeadlineTime() != null) {
                endCalender.setTime(feeInfo.getDeadlineTime());
            } else if (!StringUtil.isEmpty(feeInfo.getCurDegrees())) {
                endCalender.setTime(feeInfo.getCurReadingTime());
            } else if (feeInfo.getImportFeeEndTime() == null) {
                endCalender.setTime(feeInfo.getConfigEndTime());
            } else {
                endCalender.setTime(feeInfo.getImportFeeEndTime());
            }
            //businessFee.put("state",FeeDto.STATE_FINISH);
            feeInfo.setState(FeeDto.STATE_FINISH);
        }
        feeInfo.setEndTime(endCalender.getTime());
        Date maxEndTime = feeInfo.getDeadlineTime();
        if(FeeDto.FEE_FLAG_CYCLE.equals(feeInfo.getFeeFlag())){
            maxEndTime = feeInfo.getConfigEndTime();
        }
        //?????? ???????????? ???????????? ????????? ????????????????????? ????????????????????? ???????????????????????? ????????????????????? 30??? ????????? ???????????????
        if(maxEndTime != null) {
            if (feeInfo.getEndTime().getTime() - maxEndTime.getTime() > 30 * 24 * 60 * 60 * 1000L) {
                throw new IllegalArgumentException("??????????????? ?????????????????????");
            }
        }
        Map feeMap = BeanConvertUtil.beanCovertMap(feeInfo);
        feeMap.put("startTime", DateUtil.getFormatTimeString(feeInfo.getStartTime(), DateUtil.DATE_FORMATE_STRING_A));
        feeMap.put("endTime", DateUtil.getFormatTimeString(feeInfo.getEndTime(), DateUtil.DATE_FORMATE_STRING_A));
        feeMap.put("cycles", paramInJson.getString("cycles"));
        feeMap.put("configEndTime", feeInfo.getConfigEndTime());
        JSONObject businessFee = new JSONObject();
        businessFee.putAll(feeMap);
        //????????????????????????
        paramInJson.put("carFeeEndTime", feeInfo.getEndTime());
        paramInJson.put("carPayerObjType", feeInfo.getPayerObjType());
        paramInJson.put("carPayerObjId", feeInfo.getPayerObjId());


        // ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        if (!FeeFlagTypeConstant.ONETIME.equals(feeInfo.getFeeFlag())) {
            //?????? ??????????????????
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(maxEndTime);
            calendar.add(Calendar.DAY_OF_MONTH, -5);
            maxEndTime = calendar.getTime();
            if (feeInfo.getEndTime().after(maxEndTime)) {
                businessFee.put("state", FeeConfigConstant.END);
                businessFee.put("endTime", maxEndTime);
            }
        }

        return businessFee;
    }

    private static Calendar getTargetEndTime(Calendar endCalender, Double cycles) {
        if (StringUtil.isInteger(cycles.toString())) {
            endCalender.add(Calendar.MONTH, new Double(cycles).intValue());
            return endCalender;
        }
        if (cycles >= 1) {
            endCalender.add(Calendar.MONTH, new Double(Math.floor(cycles)).intValue());
            cycles = cycles - Math.floor(cycles);
        }
        int futureDay = endCalender.getActualMaximum(Calendar.DAY_OF_MONTH);
        int hours = new Double(cycles * futureDay * 24).intValue();
        endCalender.add(Calendar.HOUR, hours);
        return endCalender;
    }
}
