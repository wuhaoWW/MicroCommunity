package com.java110.common.cmd.machineTranslate;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.DataFlowContext;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.CommunityMemberDto;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.machine.CarBlackWhiteDto;
import com.java110.dto.machine.CarInoutDto;
import com.java110.dto.machine.MachineDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.entity.center.AppService;
import com.java110.intf.common.*;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.fee.IFeeConfigInnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.fee.IPayFeeV1InnerServiceSMO;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.po.car.CarInoutDetailPo;
import com.java110.po.car.CarInoutPo;
import com.java110.po.fee.PayFeePo;
import com.java110.utils.constant.*;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.vo.api.machine.MachineResDataVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Java110Cmd(serviceCode = "machineTranslate.machineRoadGateOpen")
public class MachineRoadGateOpenCmd extends BaseMachineCmd {
    private static Logger logger = LoggerFactory.getLogger(MachineRoadGateOpenCmd.class);

    private static final String MACHINE_DIRECTION_IN = "3306"; // ??????

    private static final String MACHINE_DIRECTION_OUT = "3307"; //??????

    private static final String HIRE_SELL_OUT = "hireSellOut"; // ???????????????????????????

    private static final String CAR_BLACK = "1111"; // ???????????????
    private static final String CAR_WHITE = "2222"; // ???????????????

    @Autowired
    private IMachineInnerServiceSMO machineInnerServiceSMOImpl;

    @Autowired
    private ICarInoutInnerServiceSMO carInoutInnerServiceSMOImpl;

    @Autowired
    private ICarBlackWhiteInnerServiceSMO carBlackWhiteInnerServiceSMOImpl;

    @Autowired
    private IOwnerCarInnerServiceSMO carInnerServiceSMOImpl;

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    @Autowired
    private ICarInoutV1InnerServiceSMO carInoutV1InnerServiceSMOImpl;

    @Autowired
    private ICarInoutDetailV1InnerServiceSMO carInoutDetailV1InnerServiceSMOImpl;

    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        super.validateMachineHeader(event, reqJson);

        Assert.hasKeyAndValue(reqJson, "carNum", "?????????????????????????????????");
    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
//JSONObject outParam = null;
        ResponseEntity<String> responseEntity = null;
        Map<String, String> reqHeader = context.getReqHeaders();
        String communityId = reqJson.containsKey("communityId") ? reqJson.getString("communityId") : reqHeader.get("communityId");
        String machineCode = reqHeader.get("machinecode");
        HttpHeaders headers = new HttpHeaders();
        for (String key : reqHeader.keySet()) {
            if (key.toLowerCase().equals("content-length")) {
                continue;
            }
            headers.add(key, reqHeader.get(key));
        }
        //???????????????????????? ????????????
        MachineDto machineDto = new MachineDto();
        machineDto.setMachineCode(machineCode);
        machineDto.setCommunityId(communityId);
        List<MachineDto> machineDtos = machineInnerServiceSMOImpl.queryMachines(machineDto);
        if (machineDtos == null || machineDtos.size() < 1) {
            responseEntity = MachineResDataVo.getResData(MachineResDataVo.CODE_ERROR, "????????????" + machineCode + "?????????????????????" + communityId + "?????????");
            context.setResponseEntity(responseEntity);
            return;
        }
        //????????????
        String direction = machineDtos.get(0).getDirection();

        //??????
        if (MACHINE_DIRECTION_IN.equals(direction)) {
            dealCarIn(event, context, reqJson, machineDtos.get(0), communityId);
        } else {
            dealCarOut(event, context, reqJson, machineDtos.get(0), communityId);
        }
    }

    /**
     * ????????????
     *
     * @param event
     * @param context
     * @param reqJson
     * @param machineDto
     * @param communityId
     */
    private void dealCarOut(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson, MachineDto machineDto, String communityId) {

        //?????????????????????????????????
        CarInoutDto carInoutDto = new CarInoutDto();
        carInoutDto.setStates(new String[]{"100300", "100400", "100600"});
        carInoutDto.setCommunityId(communityId);
        carInoutDto.setCarNum(reqJson.getString("carNum"));
        List<CarInoutDto> carInoutDtos = carInoutInnerServiceSMOImpl.queryCarInouts(carInoutDto);

        if (carInoutDtos == null || carInoutDtos.size() < 1) {//?????????????????? ????????????
            context.setResponseEntity(MachineResDataVo.getResData(MachineResDataVo.CODE_SUCCESS, "??????????????????????????????????????????????????????"));
            return;
        }

        CarInoutDto tmpCarInoutDto = carInoutDtos.get(0);
        reqJson.put("inoutId", tmpCarInoutDto.getInoutId());

        if (!"100400".equals(tmpCarInoutDto.getState())) {

            dealCarOutIncomplete(event, context, reqJson, tmpCarInoutDto, machineDto);
            return;
        }

        //????????????????????????????????????????????????

        if (judgeCarOutTimeOut(event, context, reqJson, tmpCarInoutDto, machineDto)) {
            JSONObject data = computeHourAndMoney(tmpCarInoutDto.getCommunityId(), new Date(), reqJson.getDate("feeRestartTime"));
            context.setResponseEntity(MachineResDataVo.getResData(MachineResDataVo.CODE_ERROR, "?????????????????????????????????", data));
            return;
        }

        modifyCarInoutInfo(event, context, reqJson, tmpCarInoutDto, machineDto);
        ResponseEntity<String> responseEntity = context.getResponseEntity();

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            context.setResponseEntity(MachineResDataVo.getResData(MachineResDataVo.CODE_ERROR, "????????????????????????" + responseEntity.getBody()));
            return;
        }
        context.setResponseEntity(MachineResDataVo.getResData(MachineResDataVo.CODE_SUCCESS, "??????"));
    }

    /**
     * ????????????????????????????????????
     *
     * @param event
     * @param context
     * @param reqJson
     * @param tmpCarInoutDto
     * @param machineDto
     * @return
     */
    private boolean judgeCarOutTimeOut(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson, CarInoutDto tmpCarInoutDto, MachineDto machineDto) {
        CommunityMemberDto communityMemberDto = new CommunityMemberDto();
        communityMemberDto.setCommunityId(machineDto.getCommunityId());
        communityMemberDto.setMemberTypeCd(CommunityMemberTypeConstant.PROPERTY);
        List<CommunityMemberDto> communityMemberDtos = communityInnerServiceSMOImpl.getCommunityMembers(communityMemberDto);
        String storeId = "-1";
        if (communityMemberDtos != null && communityMemberDtos.size() > 0) {
            storeId = communityMemberDtos.get(0).getMemberId();
        }

        FeeDto feeDto = new FeeDto();
        feeDto.setCommunityId(machineDto.getCommunityId());
        feeDto.setPayerObjId(reqJson.getString("inoutId"));
        feeDto.setIncomeObjId(storeId);
        feeDto.setFeeTypeCd(FeeTypeConstant.FEE_TYPE_TEMP_DOWN_PARKING_SPACE);
        feeDto.setState("2009001");
        feeDto.setFeeFlag("2006012");
        List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);

        if (feeDtos == null || feeDtos.size() < 1) {
            return false;
        }

        FeeDto tmpFeeDto = feeDtos.get(0);


        long dffMin = new Date().getTime() - tmpFeeDto.getEndTime().getTime();

        if (dffMin < 15 * 1000 * 60) {
            return false;
        }

        //???????????? ?????? ???????????? ??????
        //??????????????????
        modifyCarInout(reqJson, tmpCarInoutDto, "100600", null);
        addCarInoutFee(reqJson, tmpCarInoutDto.getCommunityId(), DateUtil.getFormatTimeString(tmpFeeDto.getEndTime(), DateUtil.DATE_FORMATE_STRING_A));

        reqJson.put("feeRestartTime", tmpFeeDto.getEndTime());

        return true;
    }

    /**
     * ??????????????????
     *
     * @param paramInJson     ???????????????????????????
     * @return ?????????????????????????????????
     */
    public void addCarInoutFee(JSONObject paramInJson,  String communityId) {
        addCarInoutFee(paramInJson, communityId, DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
    }

    public void addCarInoutFee(JSONObject paramInJson, String communityId, String startTime) {
        CommunityMemberDto communityMemberDto = new CommunityMemberDto();
        communityMemberDto.setCommunityId(communityId);
        communityMemberDto.setMemberTypeCd(CommunityMemberTypeConstant.PROPERTY);
        List<CommunityMemberDto> communityMemberDtos = communityInnerServiceSMOImpl.getCommunityMembers(communityMemberDto);
        String storeId = "-1";
        if (communityMemberDtos != null && communityMemberDtos.size() > 0) {
            storeId = communityMemberDtos.get(0).getMemberId();
        }

        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setFeeTypeCd(FeeTypeConstant.FEE_TYPE_TEMP_DOWN_PARKING_SPACE);
        feeConfigDto.setIsDefault("T");
        feeConfigDto.setCommunityId(communityId);
        List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);
        if (feeConfigDtos == null || feeConfigDtos.size() != 1) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "????????????????????????????????????????????????");
        }

        feeConfigDto = feeConfigDtos.get(0);

        JSONObject businessUnit = new JSONObject();
        businessUnit.put("feeId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_feeId));
        businessUnit.put("configId", feeConfigDto.getConfigId());
        businessUnit.put("feeTypeCd", FeeTypeConstant.FEE_TYPE_TEMP_DOWN_PARKING_SPACE);
        businessUnit.put("incomeObjId", storeId);
        businessUnit.put("amount", "-1.00");
        businessUnit.put("startTime", startTime);
        businessUnit.put("endTime", DateUtil.getLastTime()); // ??????????????????????????????2038???
        businessUnit.put("communityId", communityId);
        businessUnit.put("payerObjId", paramInJson.getString("inoutId"));
        businessUnit.put("payerObjType", "9999");
        businessUnit.put("feeFlag", "2006012"); // ???????????????
        businessUnit.put("state", "2008001"); // ?????????
        businessUnit.put("userId", "-1");
        PayFeePo payFeePo = BeanConvertUtil.covertBean(businessUnit, PayFeePo.class);
        int flag = payFeeV1InnerServiceSMOImpl.savePayFee(payFeePo);
        if (flag < 1) {
            throw new CmdException("??????????????????");
        }
    }


    public void modifyCarInout(JSONObject reqJson, CarInoutDto carInoutDto, String state, String endTime) {

        JSONObject businessCarInout = new JSONObject();
        businessCarInout.putAll(BeanConvertUtil.beanCovertMap(carInoutDto));
        businessCarInout.put("state", state);
        businessCarInout.put("outTime", endTime);
        CarInoutPo carInoutPo = BeanConvertUtil.covertBean(businessCarInout, CarInoutPo.class);
        int flag = carInoutV1InnerServiceSMOImpl.updateCarInout(carInoutPo);

        if (flag < 1) {
            throw new CmdException("????????????????????????");
        }
    }


    /**
     * ???????????????????????????????????????
     *
     * @param event
     * @param context
     * @param tmpCarInoutDto
     * @param machineDto
     */
    private void dealCarOutIncomplete(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson, CarInoutDto tmpCarInoutDto, MachineDto machineDto) {
        //?????????????????????????????????
        String carNum = reqJson.getString("carNum");
        CarBlackWhiteDto carBlackWhiteDto = new CarBlackWhiteDto();
        carBlackWhiteDto.setCommunityId(tmpCarInoutDto.getCommunityId());
        carBlackWhiteDto.setCarNum(carNum);
        carBlackWhiteDto.setBlackWhite(CAR_WHITE);
        int count = carBlackWhiteInnerServiceSMOImpl.queryCarBlackWhitesCount(carBlackWhiteDto);
        if (count > 0) {
            modifyCarInoutInfo(event, context, reqJson, tmpCarInoutDto, machineDto);
            context.setResponseEntity(MachineResDataVo.getResData(MachineResDataVo.CODE_SUCCESS, "????????????????????????"));
            return;
        }

        //??????????????????????????? ??????????????????
        OwnerCarDto ownerCarDto = new OwnerCarDto();
        ownerCarDto.setCarNum(carNum);
        ownerCarDto.setCommunityId(tmpCarInoutDto.getCommunityId());
        List<OwnerCarDto> ownerCarDtos = carInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);
        Date nowTime = new Date();
        if (ownerCarDtos != null && ownerCarDtos.size() > 0) {
            //????????????????????????????????? ???????????? ???????????????????????????????????????????????????
            OwnerCarDto tmpOwnerCarDto = ownerCarDtos.get(0);
            FeeDto feeDto = new FeeDto();
            feeDto.setPayerObjId(tmpOwnerCarDto.getPsId());
            feeDto.setCommunityId(tmpCarInoutDto.getCommunityId());
            List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);

            FeeDto tmpFeeDto = feeDtos.get(0);

            Date endTime = tmpFeeDto.getEndTime();

            long betweenTime = (endTime.getTime() - nowTime.getTime());

            if (betweenTime > 0) {
                long day = betweenTime / (60 * 60 * 24 * 1000);
                JSONObject data = new JSONObject();
                data.put("day", day);//??????????????????
                modifyCarInoutInfo(event, context, reqJson, tmpCarInoutDto, machineDto, HIRE_SELL_OUT);
                context.setResponseEntity(MachineResDataVo.getResData(MachineResDataVo.CODE_SUCCESS, "??????", data));
                return;
            }
        }

        //??????????????????
        Date inTime = null;
        try {
            inTime = DateUtil.getDateFromString(tmpCarInoutDto.getInTime(), DateUtil.DATE_FORMATE_STRING_A);
        } catch (Exception e) {
            logger.error("???????????????", e);
            context.setResponseEntity(MachineResDataVo.getResData(MachineResDataVo.CODE_ERROR, "?????????????????????????????????"));
            return;
        }

        JSONObject data = computeHourAndMoney(tmpCarInoutDto.getCommunityId(), nowTime, inTime);

        context.setResponseEntity(MachineResDataVo.getResData(MachineResDataVo.CODE_ERROR, "??????????????????????????????", data));
    }

    private JSONObject computeHourAndMoney(String communityId, Date nowTime, Date inTime) {
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setCommunityId(communityId);
        feeConfigDto.setIsDefault("T");
        feeConfigDto.setFeeTypeCd(FeeTypeConstant.FEE_TYPE_TEMP_DOWN_PARKING_SPACE);
        List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);

        FeeConfigDto tmpFeeConfigDto = feeConfigDtos.get(0);
        long diff = nowTime.getTime() - inTime.getTime();
        long nd = 1000 * 24 * 60 * 60;// ??????????????????
        long nh = 1000 * 60 * 60;// ?????????????????????
        long nm = 1000 * 60;// ?????????????????????
        double day = 0;
        double hour = 0;
        double min = 0;
        day = diff / nd;// ??????????????????
        hour = diff % nd / nh + day * 24;// ?????????????????????
        min = diff % nd % nh / nm + day * 24 * 60;// ?????????????????????
        double money = 0.00;
        double newHour = hour;
        if (min > 0) { //???????????????
            newHour += 1;
        }
        if (newHour <= 2) {
            money = Double.parseDouble(tmpFeeConfigDto.getAdditionalAmount());
        } else {
            double lastHour = newHour - 2;
            money = lastHour * Double.parseDouble(tmpFeeConfigDto.getSquarePrice()) + Double.parseDouble(tmpFeeConfigDto.getAdditionalAmount());
        }

        JSONObject data = new JSONObject();
        data.put("money", money);//????????????
        data.put("hour", new Double(hour).intValue());//????????????
        data.put("min", new Double(min).intValue());//????????????
        return data;
    }

    private void modifyCarInoutInfo(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson, CarInoutDto tmpCarInoutDto, MachineDto machineDto) {
        modifyCarInoutInfo(event, context, reqJson, tmpCarInoutDto, machineDto, "");
    }

    private void modifyCarInoutInfo(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson, CarInoutDto tmpCarInoutDto, MachineDto machineDto, String from) {

        //??????????????????
        modifyCarInout(reqJson, tmpCarInoutDto);
        reqJson.put("inoutId", tmpCarInoutDto.getInoutId());
        addCarInoutDetail(reqJson, tmpCarInoutDto.getCommunityId(), machineDto);
        if (HIRE_SELL_OUT.equals(from)) {
            modifyCarInoutFee(reqJson, tmpCarInoutDto.getCommunityId(), machineDto);

        }

    }

    /**
     * ??????????????????
     *
     * @param paramInJson     ???????????????????????????
     * @return ?????????????????????????????????
     */
    public void addCarInoutDetail(JSONObject paramInJson, String communityId, MachineDto machineDto) {

        JSONObject businessCarInoutDetail = new JSONObject();
        businessCarInoutDetail.put("carNum", paramInJson.getString("carNum"));
        businessCarInoutDetail.put("inoutId", paramInJson.getString("inoutId"));
        businessCarInoutDetail.put("communityId", communityId);
        businessCarInoutDetail.put("machineId", machineDto.getMachineId());
        businessCarInoutDetail.put("machineCode", machineDto.getMachineCode());
        businessCarInoutDetail.put("carInout", machineDto.getDirection());
        businessCarInoutDetail.put("detailId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_detailId));
        paramInJson.put("detailId",businessCarInoutDetail.getString("detailId"));
        CarInoutDetailPo carInoutDetailPo = BeanConvertUtil.covertBean(businessCarInoutDetail, CarInoutDetailPo.class);
        int flag = carInoutDetailV1InnerServiceSMOImpl.saveCarInoutDetail(carInoutDetailPo);
        if(flag < 1){
            throw new CmdException("????????????");
        }

    }

    public void modifyCarInout(JSONObject reqJson, CarInoutDto carInoutDto) {
        modifyCarInout(reqJson, carInoutDto, "100500", DateUtil.getFormatTimeString(new Date(), DateUtil.DATE_FORMATE_STRING_A));
    }


    /**
     * ??????????????????
     *
     * @param reqJson
     * @param machineDto
     * @param communityId
     */
    private void dealCarIn(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson, MachineDto machineDto, String communityId) {
        //????????????????????? ??????
        String carNum = reqJson.getString("carNum");
        CarBlackWhiteDto carBlackWhiteDto = new CarBlackWhiteDto();
        carBlackWhiteDto.setCommunityId(communityId);
        carBlackWhiteDto.setCarNum(carNum);
        carBlackWhiteDto.setBlackWhite(CAR_BLACK);
        int count = carBlackWhiteInnerServiceSMOImpl.queryCarBlackWhitesCount(carBlackWhiteDto);
        if (count > 0) {
            context.setResponseEntity(MachineResDataVo.getResData(MachineResDataVo.CODE_ERROR, carNum + "?????????????????????????????????"));
            return;
        }

        HttpHeaders header = new HttpHeaders();
        context.getReqHeaders().put(CommonConstant.HTTP_ORDER_TYPE_CD, "D");
        JSONArray businesses = new JSONArray();


        //??????????????????
        addCarInout(reqJson, communityId);
        addCarInoutDetail(reqJson, communityId, machineDto);
        addCarInoutFee(reqJson, communityId);

        context.setResponseEntity(MachineResDataVo.getResData(MachineResDataVo.CODE_SUCCESS, "??????"));
    }

    /**
     * ??????????????????
     *
     * @param paramInJson     ???????????????????????????
     * @return ?????????????????????????????????
     */
    public void addCarInout(JSONObject paramInJson,  String communityId) {

        if (!paramInJson.containsKey("inoutId") || "-1".equals(paramInJson.getString("inoutId"))) {
            paramInJson.put("inoutId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_inoutId));
        }
        JSONObject businessCarInout = new JSONObject();
        businessCarInout.put("carNum", paramInJson.getString("carNum"));
        businessCarInout.put("inoutId", paramInJson.getString("inoutId"));
        businessCarInout.put("communityId", communityId);
        businessCarInout.put("state", "100300");
        businessCarInout.put("inTime", DateUtil.getFormatTimeString(new Date(), DateUtil.DATE_FORMATE_STRING_A));
        CarInoutPo carInoutPo = BeanConvertUtil.covertBean(businessCarInout, CarInoutPo.class);
        int flag = carInoutV1InnerServiceSMOImpl.saveCarInout(carInoutPo);
        if(flag < 1){
            throw new CmdException("??????????????????");
        }
    }


    /**
     * ??????????????? ????????????
     *
     * @param reqJson
     * @param communityId
     * @param machineDto
     * @return
     */
    private void modifyCarInoutFee(JSONObject reqJson, String communityId, MachineDto machineDto) {

        CommunityMemberDto communityMemberDto = new CommunityMemberDto();
        communityMemberDto.setCommunityId(communityId);
        communityMemberDto.setMemberTypeCd(CommunityMemberTypeConstant.PROPERTY);
        List<CommunityMemberDto> communityMemberDtos = communityInnerServiceSMOImpl.getCommunityMembers(communityMemberDto);
        String storeId = "-1";
        if (communityMemberDtos != null && communityMemberDtos.size() > 0) {
            storeId = communityMemberDtos.get(0).getMemberId();
        }

        FeeDto feeDto = new FeeDto();
        feeDto.setCommunityId(communityId);
        feeDto.setPayerObjId(reqJson.getString("inoutId"));
        feeDto.setIncomeObjId(storeId);
        feeDto.setFeeTypeCd(FeeTypeConstant.FEE_TYPE_TEMP_DOWN_PARKING_SPACE);
        feeDto.setState("2008001");
        feeDto.setFeeFlag("2006012");
        List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);

        if (feeDtos == null || feeDtos.size() < 1) {
            return;
        }

        FeeDto tmpFeeDto = feeDtos.get(0);

        JSONObject businessUnit = new JSONObject();
        businessUnit.putAll(BeanConvertUtil.beanCovertMap(tmpFeeDto));
        businessUnit.put("endTime", DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        businessUnit.put("state", "2009001"); // ?????????

        PayFeePo payFeePo = BeanConvertUtil.covertBean(businessUnit, PayFeePo.class);
        int flag = payFeeV1InnerServiceSMOImpl.updatePayFee(payFeePo);

        if(flag < 1){
            throw new CmdException("??????????????????");
        }

    }
}
