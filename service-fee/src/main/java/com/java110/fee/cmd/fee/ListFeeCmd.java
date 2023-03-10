package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.CommunitySettingFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.core.smo.IComputeFeeSMO;
import com.java110.dto.FloorDto;
import com.java110.dto.RoomDto;
import com.java110.dto.UnitDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.intf.community.IFloorInnerServiceSMO;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.community.IUnitInnerServiceSMO;
import com.java110.intf.fee.IFeeAttrInnerServiceSMO;
import com.java110.intf.fee.IFeeConfigInnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.intf.user.IOwnerRoomRelV1InnerServiceSMO;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.*;
import com.java110.vo.api.fee.ApiFeeDataVo;
import com.java110.vo.api.fee.ApiFeeVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Java110Cmd(serviceCode = "fee.listFee")
public class ListFeeCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListFeeCmd.class);

    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IFeeAttrInnerServiceSMO feeAttrInnerServiceSMOImpl;

    @Autowired
    private IComputeFeeSMO computeFeeSMOImpl;

    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    @Autowired
    private IUnitInnerServiceSMO unitInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IOwnerRoomRelV1InnerServiceSMO ownerRoomRelV1InnerServiceSMOImpl;

    //???
    public static final String DOMAIN_COMMON = "DOMAIN.COMMON";

    //???
    public static final String TOTAL_FEE_PRICE = "TOTAL_FEE_PRICE";

    //???
    public static final String RECEIVED_AMOUNT_SWITCH = "RECEIVED_AMOUNT_SWITCH";

    //?????????????????????????????????
    public static final String OFFLINE_PAY_FEE_SWITCH = "OFFLINE_PAY_FEE_SWITCH";

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        super.validatePageInfo(reqJson);
        Assert.hasKeyAndValue(reqJson, "communityId", "???????????????ID");
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        if (reqJson.containsKey("roomNum") && !StringUtil.isEmpty(reqJson.getString("roomNum"))) {
            String[] roomNums = reqJson.getString("roomNum").split("-", 3);
            if (roomNums == null || roomNums.length != 3) {
                throw new IllegalArgumentException("???????????????????????????");
            }
            String floorNum = roomNums[0];
            String unitNum = roomNums[1];
            String roomNum = roomNums[2];
            FloorDto floorDto = new FloorDto();
            floorDto.setFloorNum(floorNum);
            floorDto.setCommunityId(reqJson.getString("communityId"));
            List<FloorDto> floorDtos = floorInnerServiceSMOImpl.queryFloors(floorDto);
            if (floorDtos != null && floorDtos.size() > 0) {
                for (FloorDto floor : floorDtos) {
                    UnitDto unitDto = new UnitDto();
                    unitDto.setFloorId(floor.getFloorId());
                    unitDto.setUnitNum(unitNum);
                    List<UnitDto> unitDtos = unitInnerServiceSMOImpl.queryUnits(unitDto);
                    if (unitDtos != null && unitDtos.size() > 0) {
                        for (UnitDto unit : unitDtos) {
                            RoomDto roomDto = new RoomDto();
                            roomDto.setUnitId(unit.getUnitId());
                            roomDto.setRoomNum(roomNum);
                            roomDto.setCommunityId(reqJson.getString("communityId"));
                            List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);
                            Assert.listOnlyOne(roomDtos, "?????????????????????");
                            reqJson.put("payerObjId", roomDtos.get(0).getRoomId());
                        }
                    }
                }
            }
        }

        FeeDto feeDto = BeanConvertUtil.covertBean(reqJson, FeeDto.class);
        List<ApiFeeDataVo> fees = new ArrayList<>();
        if (reqJson.containsKey("ownerId") && !StringUtil.isEmpty(reqJson.getString("ownerId"))) {
            OwnerRoomRelDto ownerRoomRelDto = new OwnerRoomRelDto();
            ownerRoomRelDto.setRoomId(reqJson.getString("payerObjId"));
            ownerRoomRelDto.setOwnerId(reqJson.getString("ownerId"));
            List<OwnerRoomRelDto> ownerRoomRelDtos = ownerRoomRelV1InnerServiceSMOImpl.queryOwnerRoomRels(ownerRoomRelDto);
            if (ownerRoomRelDtos == null || ownerRoomRelDtos.size() < 1) {
                ApiFeeVo apiFeeVo = new ApiFeeVo();
                apiFeeVo.setTotal(0);
                apiFeeVo.setRecords((int) Math.ceil((double) 0 / (double) reqJson.getInteger("row")));
                apiFeeVo.setFees(fees);
                ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiFeeVo), HttpStatus.OK);
                cmdDataFlowContext.setResponseEntity(responseEntity);
            }
        }
        int count = feeInnerServiceSMOImpl.queryFeesCount(feeDto);
        if (count > 0) {
            List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);//??????????????????
            computeFeePrice(feeDtos);//????????????
            List<ApiFeeDataVo> apiFeeDataVos = BeanConvertUtil.covertBeanList(feeDtos, ApiFeeDataVo.class);
            for (ApiFeeDataVo apiFeeDataVo : apiFeeDataVos) {
                //????????????????????????
                String payerObjType = apiFeeDataVo.getPayerObjType();
                if (!StringUtil.isEmpty(payerObjType) && payerObjType.equals("6666")) {
                    apiFeeDataVo.setCarTypeCd("1001");
                }
                fees.add(apiFeeDataVo);
            }
            freshFeeAttrs(fees, feeDtos);
        } else {
            fees = new ArrayList<>();
        }
        ApiFeeVo apiFeeVo = new ApiFeeVo();
        apiFeeVo.setTotal(count);
        apiFeeVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        apiFeeVo.setFees(fees);
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiFeeVo), HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    private void freshFeeAttrs(List<ApiFeeDataVo> fees, List<FeeDto> feeDtos) {
        for (ApiFeeDataVo apiFeeDataVo : fees) {
            for (FeeDto feeDto : feeDtos) {
                if (apiFeeDataVo.getFeeId().equals(feeDto.getFeeId())) {
                    apiFeeDataVo.setFeeAttrs(feeDto.getFeeAttrDtos());
                }
            }
        }
    }

    private void computeFeePrice(List<FeeDto> feeDtos) {

        if (feeDtos == null || feeDtos.size() < 1) {
            return;
        }
        String val = CommunitySettingFactory.getValue(feeDtos.get(0).getCommunityId(), TOTAL_FEE_PRICE);
        if (StringUtil.isEmpty(val)) {
            val = MappingCache.getValue(DOMAIN_COMMON, TOTAL_FEE_PRICE);
        }

        //???????????????????????????????????? ??? ?????????
        String received_amount_switch = CommunitySettingFactory.getValue(feeDtos.get(0).getCommunityId(), RECEIVED_AMOUNT_SWITCH);
        if (StringUtil.isEmpty(received_amount_switch)) {
            received_amount_switch = MappingCache.getValue(DOMAIN_COMMON, RECEIVED_AMOUNT_SWITCH);
        }

        //???????????????????????????????????? ??? ?????????
        String offlinePayFeeSwitch = CommunitySettingFactory.getValue(feeDtos.get(0).getCommunityId(), OFFLINE_PAY_FEE_SWITCH);
        if (StringUtil.isEmpty(offlinePayFeeSwitch)) {
            offlinePayFeeSwitch = MappingCache.getValue(DOMAIN_COMMON, OFFLINE_PAY_FEE_SWITCH);
        }

        for (FeeDto feeDto : feeDtos) {
            try {
                // ?????? * ?????? * 30 + ???????????? = ?????? ????????????
                Map<String, Object> targetEndDateAndOweMonth = computeFeeSMOImpl.getTargetEndDateAndOweMonth(feeDto);
                Date targetEndDate = (Date) targetEndDateAndOweMonth.get("targetEndDate");
                double oweMonth = (double) targetEndDateAndOweMonth.get("oweMonth");
                feeDto.setCycle(feeDto.getPaymentCycle());
                feeDto.setDeadlineTime(targetEndDate);
                if (FeeDto.PAYER_OBJ_TYPE_ROOM.equals(feeDto.getPayerObjType())) { //????????????
                    computeFeePriceByRoom(feeDto, oweMonth);
                } else if (FeeDto.PAYER_OBJ_TYPE_CAR.equals(feeDto.getPayerObjType())) {//????????????
                    computeFeePriceByCar(feeDto, oweMonth);
                } else if (FeeDto.PAYER_OBJ_TYPE_CONTRACT.equals(feeDto.getPayerObjType())) {//????????????
                    computeFeePriceByContract(feeDto, oweMonth);
                }

                feeDto.setVal(val);
                //?????? ??????????????????
                if (StringUtil.isEmpty(received_amount_switch)) {
                    feeDto.setReceivedAmountSwitch("1");//??????????????????????????????
                } else {
                    feeDto.setReceivedAmountSwitch(received_amount_switch);
                }
                feeDto.setOfflinePayFeeSwitch(offlinePayFeeSwitch);

            } catch (Exception e) {
                logger.error("?????????????????? ?????????????????????", e);
            }

            //????????????0
            feeDto.setSquarePrice(Double.parseDouble(feeDto.getSquarePrice()) + "");
            feeDto.setAdditionalAmount(Double.parseDouble(feeDto.getAdditionalAmount())+"");
        }
    }

    private void computeFeePriceByCar(FeeDto feeDto, double oweMonth) {
        OwnerCarDto ownerCarDto = new OwnerCarDto();
        ownerCarDto.setCarTypeCd("1001"); //????????????
        ownerCarDto.setCommunityId(feeDto.getCommunityId());
        ownerCarDto.setCarId(feeDto.getPayerObjId());
        List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);
        if (ownerCarDtos == null || ownerCarDtos.size() < 1) { //???????????????
            return;
        }
        ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
        parkingSpaceDto.setCommunityId(feeDto.getCommunityId());
        parkingSpaceDto.setPsId(ownerCarDtos.get(0).getPsId());
        List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);
        if (parkingSpaceDtos == null || parkingSpaceDtos.size() < 1) { //???????????????
            return;
        }
        String computingFormula = feeDto.getComputingFormula();
        DecimalFormat df = new DecimalFormat("0.00");
        Map feePriceAll = computeFeeSMOImpl.getFeePrice(feeDto);
        feeDto.setFeePrice(Double.parseDouble(feePriceAll.get("feePrice").toString()));
        feeDto.setFeeTotalPrice(Double.parseDouble(feePriceAll.get("feeTotalPrice").toString()));

        BigDecimal curFeePrice = new BigDecimal(feeDto.getFeePrice());
        curFeePrice = curFeePrice.multiply(new BigDecimal(oweMonth));
        feeDto.setAmountOwed(df.format(curFeePrice));
        //????????????
        if ("4004".equals(computingFormula)
                && FeeDto.FEE_FLAG_ONCE.equals(feeDto.getFeeFlag())
                && !FeeDto.STATE_FINISH.equals(feeDto.getState())) {
            feeDto.setAmountOwed(df.format(curFeePrice));
            feeDto.setDeadlineTime(DateUtil.getCurrentDate());
        }

        //??????????????????
        computeFeeSMOImpl.dealRentRate(feeDto);
    }

    /**
     * ????????????????????????
     *
     * @param feeDto
     */
    private void computeFeePriceByRoom(FeeDto feeDto, double oweMonth) {
        String computingFormula = feeDto.getComputingFormula();
        Map feePriceAll = computeFeeSMOImpl.getFeePrice(feeDto);
        feeDto.setFeePrice(Double.parseDouble(feePriceAll.get("feePrice").toString()));
        //BigDecimal feeTotalPrice = new BigDecimal(Double.parseDouble(feePriceAll.get("feeTotalPrice").toString()));
        feeDto.setFeeTotalPrice(MoneyUtil.computePriceScale(Double.parseDouble(feePriceAll.get("feeTotalPrice").toString()),
                feeDto.getScale(),
                Integer.parseInt(feeDto.getDecimalPlace())));
        BigDecimal curFeePrice = new BigDecimal(feeDto.getFeePrice());
        curFeePrice = curFeePrice.multiply(new BigDecimal(oweMonth));
        feeDto.setAmountOwed(MoneyUtil.computePriceScale(curFeePrice.doubleValue(), feeDto.getScale(), Integer.parseInt(feeDto.getDecimalPlace())) + "");
        //????????????
        if ("4004".equals(computingFormula)
                && FeeDto.FEE_FLAG_ONCE.equals(feeDto.getFeeFlag())
                && !FeeDto.STATE_FINISH.equals(feeDto.getState())
                && feeDto.getDeadlineTime() == null) {
            feeDto.setDeadlineTime(DateUtil.getCurrentDate());
        }

        //??????????????????
        computeFeeSMOImpl.dealRentRate(feeDto);

    }


    /**
     * ????????????????????????
     *
     * @param feeDto
     */
    private void computeFeePriceByContract(FeeDto feeDto, double oweMonth) {
        String computingFormula = feeDto.getComputingFormula();
        Map feePriceAll = computeFeeSMOImpl.getFeePrice(feeDto);
        feeDto.setFeePrice(Double.parseDouble(feePriceAll.get("feePrice").toString()));
        feeDto.setFeeTotalPrice(MoneyUtil.computePriceScale(Double.parseDouble(feePriceAll.get("feeTotalPrice").toString()),
                feeDto.getScale(),
                Integer.parseInt(feeDto.getDecimalPlace())));
        BigDecimal curFeePrice = new BigDecimal(feeDto.getFeePrice());
        curFeePrice = curFeePrice.multiply(new BigDecimal(oweMonth));
        feeDto.setAmountOwed(MoneyUtil.computePriceScale(curFeePrice.doubleValue(), feeDto.getScale(), Integer.parseInt(feeDto.getDecimalPlace())) + "");
        //????????????
        if ("4004".equals(computingFormula)
                && FeeDto.FEE_FLAG_ONCE.equals(feeDto.getFeeFlag())
                && !FeeDto.STATE_FINISH.equals(feeDto.getState())) {
            feeDto.setAmountOwed(MoneyUtil.computePriceScale(curFeePrice.doubleValue(), feeDto.getScale(), Integer.parseInt(feeDto.getDecimalPlace())) + "");
            feeDto.setDeadlineTime(DateUtil.getCurrentDate());
        }
        //??????????????????
        computeFeeSMOImpl.dealRentRate(feeDto);
    }
}
