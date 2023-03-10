package com.java110.core.smo.impl;

import com.alibaba.fastjson.JSONObject;
import com.java110.config.properties.code.Java110Properties;
import com.java110.core.context.Environment;
import com.java110.core.log.LoggerFactory;
import com.java110.core.smo.IComputeFeeSMO;
import com.java110.dto.RoomAttrDto;
import com.java110.dto.RoomDto;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.contract.ContractDto;
import com.java110.dto.contractRoom.ContractRoomDto;
import com.java110.dto.fee.*;
import com.java110.dto.integralRuleConfig.IntegralRuleConfigDto;
import com.java110.dto.machine.CarInoutDetailDto;
import com.java110.dto.machine.CarInoutDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.dto.report.ReportCarDto;
import com.java110.dto.report.ReportFeeDto;
import com.java110.dto.report.ReportRoomDto;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.fee.IFeeAttrInnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.fee.ITempCarFeeConfigAttrInnerServiceSMO;
import com.java110.intf.fee.ITempCarFeeConfigInnerServiceSMO;
import com.java110.intf.store.IContractInnerServiceSMO;
import com.java110.intf.store.IContractRoomInnerServiceSMO;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.intf.user.IOwnerInnerServiceSMO;
import com.java110.po.feeReceiptDetail.FeeReceiptDetailPo;
import com.java110.utils.constant.FeeConfigConstant;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * ???????????? ?????????
 * <p>
 * add by wuxw 2020-09-23
 *
 * @openSource https://gitee.com/wuxw7/MicroCommunity.git
 */

@Service
public class ComputeFeeSMOImpl implements IComputeFeeSMO {

    protected static final Logger logger = LoggerFactory.getLogger(ComputeFeeSMOImpl.class);

    @Autowired(required = false)
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired(required = false)
    private IFeeAttrInnerServiceSMO feeAttrInnerServiceSMOImpl;

    @Autowired(required = false)
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired(required = false)
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    @Autowired(required = false)
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    @Autowired(required = false)
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    @Autowired(required = false)
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    @Autowired(required = false)
    private IContractRoomInnerServiceSMO contractRoomInnerServiceSMOImpl;

    @Autowired(required = false)
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    @Autowired(required = false)
    private ITempCarFeeConfigInnerServiceSMO tempCarFeeConfigInnerServiceSMOImpl;

    @Autowired(required = false)
    private ITempCarFeeConfigAttrInnerServiceSMO tempCarFeeConfigAttrInnerServiceSMOImpl;

    @Autowired
    private Java110Properties java110Properties;

    @Override
    public Date getFeeEndTime() {
        return null;
    }

    /**
     * ????????????????????????
     *
     * @param tmpFeeDto
     */
    public void computeEveryOweFee(FeeDto tmpFeeDto) {
        computeEveryOweFee(tmpFeeDto, null);
    }

    @Override
    public void computeEveryOweFee(FeeDto tmpFeeDto, RoomDto roomDto) {
        computeFeePrice(tmpFeeDto, roomDto);
    }


    /**
     * ??????????????????
     *
     * @param tmpFeeDto
     */
    public void computeOweFee(FeeDto tmpFeeDto) {
        String billType = tmpFeeDto.getBillType();

        if (FeeConfigDto.BILL_TYPE_EVERY.equals(billType)) {
            computeFeePrice(tmpFeeDto, null);
            return;
        }
        BillDto billDto = new BillDto();
        billDto.setCommunityId(tmpFeeDto.getCommunityId());
        billDto.setConfigId(tmpFeeDto.getConfigId());
        billDto.setCurBill("T");
        List<BillDto> billDtos = feeInnerServiceSMOImpl.queryBills(billDto);
        if (billDtos == null || billDtos.size() < 1) {
            tmpFeeDto.setFeePrice(0.00);
            return;
        }
        BillOweFeeDto billOweFeeDto = new BillOweFeeDto();
        billOweFeeDto.setCommunityId(tmpFeeDto.getCommunityId());
        billOweFeeDto.setFeeId(tmpFeeDto.getFeeId());
        billOweFeeDto.setState(BillOweFeeDto.STATE_WILL_FEE);
        billOweFeeDto.setBillId(billDtos.get(0).getBillId());
        List<BillOweFeeDto> billOweFeeDtos = feeInnerServiceSMOImpl.queryBillOweFees(billOweFeeDto);
        if (billOweFeeDtos == null || billOweFeeDtos.size() < 1) {
            tmpFeeDto.setFeePrice(0.00);
            return;
        }
        try {
            tmpFeeDto.setDeadlineTime(DateUtil.getDateFromString(billOweFeeDtos.get(0).getDeadlineTime(), DateUtil.DATE_FORMATE_STRING_A));
        } catch (ParseException e) {
            logger.error("????????????????????????", e);
        }
        tmpFeeDto.setFeePrice(Double.parseDouble(billOweFeeDtos.get(0).getAmountOwed()));
    }

    private void computeFeePrice(FeeDto feeDto, RoomDto roomDto) {

        if (FeeDto.PAYER_OBJ_TYPE_ROOM.equals(feeDto.getPayerObjType())) { //????????????
            computeFeePriceByRoom(feeDto, roomDto);
        } else if (FeeDto.PAYER_OBJ_TYPE_CAR.equals(feeDto.getPayerObjType())) {//????????????
            computeFeePriceByParkingSpace(feeDto);
        } else if (FeeDto.PAYER_OBJ_TYPE_CONTRACT.equals(feeDto.getPayerObjType())) { //????????????
            computeFeePriceByContract(feeDto, roomDto);
        }
    }

    private void computeFeePriceByParkingSpace(FeeDto feeDto) {
        Map<String, Object> targetEndDateAndOweMonth = getTargetEndDateAndOweMonth(feeDto);
        Date targetEndDate = (Date) targetEndDateAndOweMonth.get("targetEndDate");
        double oweMonth = (double) targetEndDateAndOweMonth.get("oweMonth");
        OwnerCarDto ownerCarDto = new OwnerCarDto();
        ownerCarDto.setCommunityId(feeDto.getCommunityId());
        ownerCarDto.setCarId(feeDto.getPayerObjId());
        List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);

        if (ownerCarDtos == null || ownerCarDtos.size() < 1) { //???????????????
            return;
        }

        String computingFormula = feeDto.getComputingFormula();
        Map feePriceAll = getFeePrice(feeDto);

        feeDto.setFeePrice(Double.parseDouble(feePriceAll.get("feePrice").toString()));
        BigDecimal price = new BigDecimal(feeDto.getFeePrice());
        price = price.multiply(new BigDecimal(oweMonth));
        feeDto.setFeePrice(price.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
        feeDto.setDeadlineTime(targetEndDate);

        //????????????
        if ("4004".equals(computingFormula)
                && FeeDto.FEE_FLAG_ONCE.equals(feeDto.getFeeFlag())
                && !FeeDto.STATE_FINISH.equals(feeDto.getState())) {
            feeDto.setAmountOwed(feeDto.getFeePrice() + "");
            //feeDto.setDeadlineTime(DateUtil.getCurrentDate()); ???????????????????????????
        }

    }

    /**
     * ????????????????????????
     *
     * @param feeDto
     */
    private void computeFeePriceByRoom(FeeDto feeDto, RoomDto roomDto) {
        Map<String, Object> targetEndDateAndOweMonth = getTargetEndDateAndOweMonth(feeDto);
        Date targetEndDate = (Date) targetEndDateAndOweMonth.get("targetEndDate");
        double oweMonth = (double) targetEndDateAndOweMonth.get("oweMonth");

        String computingFormula = feeDto.getComputingFormula();
        Map feePriceAll = getFeePrice(feeDto, roomDto);
        feeDto.setFeePrice(Double.parseDouble(feePriceAll.get("feePrice").toString()));
        //double month = dayCompare(feeDto.getEndTime(), DateUtil.getCurrentDate());
        BigDecimal price = new BigDecimal(feeDto.getFeePrice());
        price = price.multiply(new BigDecimal(oweMonth));
        feeDto.setFeePrice(price.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
        feeDto.setDeadlineTime(targetEndDate);

        //????????????
        if ("4004".equals(computingFormula)
                && FeeDto.FEE_FLAG_ONCE.equals(feeDto.getFeeFlag())
                && !FeeDto.STATE_FINISH.equals(feeDto.getState())) {
            feeDto.setAmountOwed(feeDto.getFeePrice() + "");
            //feeDto.setDeadlineTime(DateUtil.getCurrentDate()); ???????????????????????????
        }
    }

    /**
     * ????????????????????????
     *
     * @param feeDto
     */
    private void computeFeePriceByContract(FeeDto feeDto, RoomDto roomDto) {
        Map<String, Object> targetEndDateAndOweMonth = getTargetEndDateAndOweMonth(feeDto);
        Date targetEndDate = (Date) targetEndDateAndOweMonth.get("targetEndDate");
        double oweMonth = (double) targetEndDateAndOweMonth.get("oweMonth");

        String computingFormula = feeDto.getComputingFormula();
        Map feePriceAll = getFeePrice(feeDto, roomDto);
        feeDto.setFeePrice(Double.parseDouble(feePriceAll.get("feePrice").toString()));
        //double month = dayCompare(feeDto.getEndTime(), DateUtil.getCurrentDate());
        BigDecimal price = new BigDecimal(feeDto.getFeePrice());
        price = price.multiply(new BigDecimal(oweMonth));
        feeDto.setFeePrice(price.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
        feeDto.setDeadlineTime(targetEndDate);

        //????????????
        if ("4004".equals(computingFormula)
                && FeeDto.FEE_FLAG_ONCE.equals(feeDto.getFeeFlag())
                && !FeeDto.STATE_FINISH.equals(feeDto.getState())) {
            feeDto.setAmountOwed(feeDto.getFeePrice() + "");
            //feeDto.setDeadlineTime(DateUtil.getCurrentDate()); ???????????????????????????
        }
    }


    /**
     * ?????? ????????????
     *
     * @param feeDto
     * @param feeReceiptDetailPo
     */
    @Override
    public void freshFeeReceiptDetail(FeeDto feeDto, FeeReceiptDetailPo feeReceiptDetailPo) {
        if (FeeDto.PAYER_OBJ_TYPE_ROOM.equals(feeDto.getPayerObjType())) { //????????????
            String computingFormula = feeDto.getComputingFormula();
            RoomDto roomDto = new RoomDto();
            roomDto.setRoomId(feeDto.getPayerObjId());
            roomDto.setCommunityId(feeDto.getCommunityId());
            List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);
            if (roomDtos == null || roomDtos.size() != 1) {
                return;
            }
            if ("1001".equals(computingFormula)) { //??????*??????+?????????
                feeReceiptDetailPo.setArea(roomDtos.get(0).getBuiltUpArea());
                feeReceiptDetailPo.setSquarePrice(feeDto.getSquarePrice() + "/" + feeDto.getAdditionalAmount());
            } else if ("2002".equals(computingFormula)) { // ????????????
                feeReceiptDetailPo.setArea("");
                feeReceiptDetailPo.setSquarePrice(feeDto.getAdditionalAmount());
            } else if ("3003".equals(computingFormula)) { // ????????????
                feeReceiptDetailPo.setArea(roomDtos.get(0).getRoomArea());
                feeReceiptDetailPo.setSquarePrice(feeDto.getSquarePrice() + "/" + feeDto.getAdditionalAmount());
            } else if ("1101".equals(computingFormula)) { // ??????
                feeReceiptDetailPo.setArea("");
                feeReceiptDetailPo.setSquarePrice(roomDto.getRoomRent());
            } else if ("1102".equals(computingFormula)) { // ??????
                feeReceiptDetailPo.setArea("");
                feeReceiptDetailPo.setSquarePrice(roomDto.getRoomRent());
            } else if ("4004".equals(computingFormula)) {
            } else if ("5005".equals(computingFormula)) {
                if (StringUtil.isEmpty(feeDto.getCurDegrees())) {
                } else {
                    BigDecimal curDegree = new BigDecimal(Double.parseDouble(feeDto.getCurDegrees()));
                    BigDecimal preDegree = new BigDecimal(Double.parseDouble(feeDto.getPreDegrees()));
                    BigDecimal sub = curDegree.subtract(preDegree).setScale(2, BigDecimal.ROUND_HALF_UP);
                    feeReceiptDetailPo.setArea(sub.doubleValue() + "");
                    feeReceiptDetailPo.setSquarePrice(feeDto.getSquarePrice() + "/" + feeDto.getAdditionalAmount());
                }
            } else if ("6006".equals(computingFormula)) {
                String value = "";
                List<FeeAttrDto> feeAttrDtos = feeDto.getFeeAttrDtos();
                for (FeeAttrDto feeAttrDto : feeAttrDtos) {
                    if (feeAttrDto.getSpecCd().equals(FeeAttrDto.SPEC_CD_PROXY_CONSUMPTION)) {
                        value = feeAttrDto.getValue();
                    }
                }
                feeReceiptDetailPo.setArea(value);
                feeReceiptDetailPo.setSquarePrice(feeDto.getSquarePrice() + "/" + feeDto.getAdditionalAmount());
            } else if ("7007".equals(computingFormula)) { //???????????????
                feeReceiptDetailPo.setArea(roomDtos.get(0).getBuiltUpArea());
                feeReceiptDetailPo.setSquarePrice(feeDto.getComputingFormulaText());
            } else if ("9009".equals(computingFormula)) {
                if (StringUtil.isEmpty(feeDto.getCurDegrees())) {
                } else {
                    BigDecimal curDegree = new BigDecimal(Double.parseDouble(feeDto.getCurDegrees()));
                    BigDecimal preDegree = new BigDecimal(Double.parseDouble(feeDto.getPreDegrees()));
                    BigDecimal sub = curDegree.subtract(preDegree).setScale(2, BigDecimal.ROUND_HALF_UP);
                    feeReceiptDetailPo.setArea(sub.doubleValue() + "");
                    feeReceiptDetailPo.setSquarePrice(feeDto.getMwPrice() + "/" + feeDto.getAdditionalAmount());
                }
            } else if ("1101".equals(computingFormula)) { //??????
                feeReceiptDetailPo.setArea(roomDtos.get(0).getBuiltUpArea());
                feeReceiptDetailPo.setSquarePrice(roomDtos.get(0).getRoomRent());
            } else if ("1102".equals(computingFormula)) { //??????
                feeReceiptDetailPo.setArea(roomDtos.get(0).getBuiltUpArea());
                feeReceiptDetailPo.setSquarePrice(roomDtos.get(0).getRoomRent());
            } else {
            }
        } else if (FeeDto.PAYER_OBJ_TYPE_CAR.equals(feeDto.getPayerObjType())) {//????????????
            String computingFormula = feeDto.getComputingFormula();
            OwnerCarDto ownerCarDto = new OwnerCarDto();
            ownerCarDto.setCommunityId(feeDto.getCommunityId());
            ownerCarDto.setCarId(feeDto.getPayerObjId());
            List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);
            Assert.listOnlyOne(ownerCarDtos, "?????????????????????");
            if ("1001".equals(computingFormula)) { //??????*??????+?????????
                ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
                parkingSpaceDto.setCommunityId(feeDto.getCommunityId());
                parkingSpaceDto.setPsId(ownerCarDtos.get(0).getPsId());
                List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);
                if (parkingSpaceDtos == null || parkingSpaceDtos.size() < 1) { //???????????????
                    return;
                }
                feeReceiptDetailPo.setArea(parkingSpaceDtos.get(0).getArea());
                feeReceiptDetailPo.setSquarePrice(feeDto.getSquarePrice() + "/" + feeDto.getAdditionalAmount());
            } else if ("2002".equals(computingFormula)) { // ????????????
                feeReceiptDetailPo.setArea("");
                feeReceiptDetailPo.setSquarePrice(feeDto.getAdditionalAmount());
            } else if ("3003".equals(computingFormula)) { // ????????????
                feeReceiptDetailPo.setArea("");
                feeReceiptDetailPo.setSquarePrice("0");
            } else if ("1101".equals(computingFormula)) { // ??????
                feeReceiptDetailPo.setArea("");
                feeReceiptDetailPo.setSquarePrice("0");
            } else if ("1102".equals(computingFormula)) { // ??????????????????
                feeReceiptDetailPo.setArea("");
                feeReceiptDetailPo.setSquarePrice("0");
            } else if ("4004".equals(computingFormula)) {
            } else if ("5005".equals(computingFormula)) {
                if (StringUtil.isEmpty(feeDto.getCurDegrees())) {
                } else {
                    BigDecimal curDegree = new BigDecimal(Double.parseDouble(feeDto.getCurDegrees()));
                    BigDecimal preDegree = new BigDecimal(Double.parseDouble(feeDto.getPreDegrees()));
                    BigDecimal sub = curDegree.subtract(preDegree).setScale(2, BigDecimal.ROUND_HALF_UP);
                    feeReceiptDetailPo.setArea(sub.doubleValue() + "");
                    feeReceiptDetailPo.setSquarePrice(feeDto.getSquarePrice() + "/" + feeDto.getAdditionalAmount());
                }
            } else if ("6006".equals(computingFormula)) {
                String value = "";
                List<FeeAttrDto> feeAttrDtos = feeDto.getFeeAttrDtos();
                for (FeeAttrDto feeAttrDto : feeAttrDtos) {
                    if (feeAttrDto.getSpecCd().equals(FeeAttrDto.SPEC_CD_PROXY_CONSUMPTION)) {
                        value = feeAttrDto.getValue();
                    }
                }
                feeReceiptDetailPo.setArea(value);
                feeReceiptDetailPo.setSquarePrice(feeDto.getSquarePrice() + "/" + feeDto.getAdditionalAmount());
            } else if ("7007".equals(computingFormula)) { //???????????????
                ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
                parkingSpaceDto.setCommunityId(feeDto.getCommunityId());
                parkingSpaceDto.setPsId(ownerCarDtos.get(0).getPsId());
                List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);
                if (parkingSpaceDtos == null || parkingSpaceDtos.size() < 1) { //???????????????
                    return;
                }
                feeReceiptDetailPo.setArea(parkingSpaceDtos.get(0).getArea());
                feeReceiptDetailPo.setSquarePrice(feeDto.getComputingFormulaText());
            } else if ("9009".equals(computingFormula)) {
                if (StringUtil.isEmpty(feeDto.getCurDegrees())) {
                } else {
                    BigDecimal curDegree = new BigDecimal(Double.parseDouble(feeDto.getCurDegrees()));
                    BigDecimal preDegree = new BigDecimal(Double.parseDouble(feeDto.getPreDegrees()));
                    BigDecimal sub = curDegree.subtract(preDegree).setScale(2, BigDecimal.ROUND_HALF_UP);
                    feeReceiptDetailPo.setArea(sub.doubleValue() + "");
                    feeReceiptDetailPo.setSquarePrice(feeDto.getMwPrice() + "/" + feeDto.getAdditionalAmount());
                }
            } else {

            }
        } else if (FeeDto.PAYER_OBJ_TYPE_CONTRACT.equals(feeDto.getPayerObjType())) {//????????????
            String computingFormula = feeDto.getComputingFormula();
            ContractRoomDto contractRoomDto = new ContractRoomDto();
            contractRoomDto.setContractId(feeDto.getPayerObjId());
            contractRoomDto.setCommunityId(feeDto.getCommunityId());
            List<ContractRoomDto> contractRoomDtos = contractRoomInnerServiceSMOImpl.queryContractRooms(contractRoomDto);
            if (contractRoomDtos == null || contractRoomDtos.size() == 0) {
                feeReceiptDetailPo.setArea("");
                feeReceiptDetailPo.setSquarePrice(feeDto.getSquarePrice() + "/" + feeDto.getAdditionalAmount());
                return;
            }
            if ("1001".equals(computingFormula)) { //??????*??????+?????????
                BigDecimal builtUpArea = new BigDecimal(0);
                for (ContractRoomDto tmpContractRoomDto : contractRoomDtos) {
                    builtUpArea = builtUpArea.add(new BigDecimal(Double.parseDouble(tmpContractRoomDto.getBuiltUpArea())));
                }
                feeReceiptDetailPo.setArea(builtUpArea.doubleValue() + "");
                feeReceiptDetailPo.setSquarePrice(feeDto.getSquarePrice() + "/" + feeDto.getAdditionalAmount());
            } else if ("2002".equals(computingFormula)) { // ????????????
                feeReceiptDetailPo.setArea("");
                feeReceiptDetailPo.setSquarePrice(feeDto.getAdditionalAmount());
            } else if ("3003".equals(computingFormula)) { // ????????????
                BigDecimal builtUpArea = new BigDecimal(0);
                for (ContractRoomDto tmpContractRoomDto : contractRoomDtos) {
                    builtUpArea = builtUpArea.add(new BigDecimal(Double.parseDouble(tmpContractRoomDto.getRoomArea())));
                }
                feeReceiptDetailPo.setArea(builtUpArea.doubleValue() + "");
                feeReceiptDetailPo.setSquarePrice(feeDto.getSquarePrice() + "/" + feeDto.getAdditionalAmount());
            } else if ("1101".equals(computingFormula)) { // ??????
                BigDecimal builtUpArea = new BigDecimal(0);
                for (ContractRoomDto tmpContractRoomDto : contractRoomDtos) {
                    builtUpArea = builtUpArea.add(new BigDecimal(Double.parseDouble(tmpContractRoomDto.getRoomRent())));
                }
                feeReceiptDetailPo.setArea("");
                feeReceiptDetailPo.setSquarePrice(builtUpArea.doubleValue() + "");
            } else if ("1102".equals(computingFormula)) { // ??????
                BigDecimal builtUpArea = new BigDecimal(0);
                for (ContractRoomDto tmpContractRoomDto : contractRoomDtos) {
                    builtUpArea = builtUpArea.add(new BigDecimal(Double.parseDouble(tmpContractRoomDto.getRoomRent())));
                }
                feeReceiptDetailPo.setArea("");
                feeReceiptDetailPo.setSquarePrice(builtUpArea.doubleValue() + "");
            } else if ("4004".equals(computingFormula)) {
            } else if ("5005".equals(computingFormula)) {
                if (StringUtil.isEmpty(feeDto.getCurDegrees())) {
                } else {
                    BigDecimal curDegree = new BigDecimal(Double.parseDouble(feeDto.getCurDegrees()));
                    BigDecimal preDegree = new BigDecimal(Double.parseDouble(feeDto.getPreDegrees()));
                    BigDecimal sub = curDegree.subtract(preDegree).setScale(2, BigDecimal.ROUND_HALF_UP);
                    feeReceiptDetailPo.setArea(sub.doubleValue() + "");
                    feeReceiptDetailPo.setSquarePrice(feeDto.getSquarePrice() + "/" + feeDto.getAdditionalAmount());
                }
            } else if ("6006".equals(computingFormula)) {
                String value = "";
                List<FeeAttrDto> feeAttrDtos = feeDto.getFeeAttrDtos();
                for (FeeAttrDto feeAttrDto : feeAttrDtos) {
                    if (feeAttrDto.getSpecCd().equals(FeeAttrDto.SPEC_CD_PROXY_CONSUMPTION)) {
                        value = feeAttrDto.getValue();
                    }
                }
                feeReceiptDetailPo.setArea(value);
                feeReceiptDetailPo.setSquarePrice(feeDto.getSquarePrice() + "/" + feeDto.getAdditionalAmount());
            } else if ("7007".equals(computingFormula)) { //???????????????
                BigDecimal builtUpArea = new BigDecimal(0);
                for (ContractRoomDto tmpContractRoomDto : contractRoomDtos) {
                    builtUpArea = builtUpArea.add(new BigDecimal(Double.parseDouble(tmpContractRoomDto.getBuiltUpArea())));
                }
                feeReceiptDetailPo.setArea(builtUpArea.doubleValue() + "");
                feeReceiptDetailPo.setSquarePrice(feeDto.getComputingFormulaText());
            } else if ("9009".equals(computingFormula)) {
                if (StringUtil.isEmpty(feeDto.getCurDegrees())) {
                } else {
                    BigDecimal curDegree = new BigDecimal(Double.parseDouble(feeDto.getCurDegrees()));
                    BigDecimal preDegree = new BigDecimal(Double.parseDouble(feeDto.getPreDegrees()));
                    BigDecimal sub = curDegree.subtract(preDegree).setScale(2, BigDecimal.ROUND_HALF_UP);
                    feeReceiptDetailPo.setArea(sub.doubleValue() + "");
                    feeReceiptDetailPo.setSquarePrice(feeDto.getMwPrice() + "/" + feeDto.getAdditionalAmount());
                }
            } else {
            }
        }
    }

    /**
     * ????????????????????????
     *
     * @param feeDto
     * @return
     */
    @Override
    public String getFeeObjName(FeeDto feeDto) {
        String objName = "";
        if (FeeDto.PAYER_OBJ_TYPE_ROOM.equals(feeDto.getPayerObjType())) { //????????????
            RoomDto roomDto = new RoomDto();
            roomDto.setRoomId(feeDto.getPayerObjId());
            roomDto.setCommunityId(feeDto.getCommunityId());
            List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);
            if (roomDtos == null || roomDtos.size() != 1) {
                return objName;
            }
            roomDto = roomDtos.get(0);
            if (RoomDto.ROOM_TYPE_ROOM.equals(roomDto.getRoomType())) {
                objName = roomDto.getFloorNum() + "-" + roomDto.getUnitNum() + "-" + roomDto.getRoomNum();
            } else {
                objName = roomDto.getFloorNum() + "-" + roomDto.getRoomNum();
            }
        } else if (FeeDto.PAYER_OBJ_TYPE_CAR.equals(feeDto.getPayerObjType())) {//????????????

            OwnerCarDto ownerCarDto = new OwnerCarDto();
            ownerCarDto.setCommunityId(feeDto.getCommunityId());
            ownerCarDto.setCarId(feeDto.getPayerObjId());
            List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);
            if (ownerCarDtos == null || ownerCarDtos.size() < 1) {
                return objName;
            }

            objName = ownerCarDtos.get(0).getCarNum();
            ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
            parkingSpaceDto.setCommunityId(feeDto.getCommunityId());
            parkingSpaceDto.setPsId(ownerCarDtos.get(0).getPsId());
            List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);
            if (parkingSpaceDtos == null || parkingSpaceDtos.size() < 1) { //???????????????
                return objName;
            }
            objName = objName + "-" + parkingSpaceDtos.get(0).getAreaNum() + "?????????" + "-" + parkingSpaceDtos.get(0).getNum() + "??????";
        } else if (FeeDto.PAYER_OBJ_TYPE_CONTRACT.equals(feeDto.getPayerObjType())) {
            ContractDto contractDto = new ContractDto();
            contractDto.setContractId(feeDto.getPayerObjId());
            contractDto.setCommunityId(feeDto.getCommunityId());
            List<ContractDto> contractDtos = contractInnerServiceSMOImpl.queryContracts(contractDto);
            if (contractDtos == null || contractDtos.size() < 1) { //???????????????
                return objName;
            }
            objName = contractDtos.get(0).getContractCode();

        }
        return objName;
    }

    @Override
    public OwnerDto getFeeOwnerDto(FeeDto feeDto) {
        OwnerDto ownerDto = getOwnerDtoByFeeAttr(feeDto);
        if (ownerDto != null) {
            return ownerDto;
        }

        if (FeeDto.PAYER_OBJ_TYPE_ROOM.equals(feeDto.getPayerObjType())) { //????????????
            ownerDto = new OwnerDto();
            ownerDto.setRoomId(feeDto.getPayerObjId());
            ownerDto.setCommunityId(feeDto.getCommunityId());
            List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryOwners(ownerDto);
            Assert.listOnlyOne(ownerDtos, "???????????????");
            return ownerDtos.get(0);
        }

        if (FeeDto.PAYER_OBJ_TYPE_CAR.equals(feeDto.getPayerObjType())) {
            OwnerCarDto ownerCarDto = new OwnerCarDto();
            ownerCarDto.setCarId(feeDto.getPayerObjId());
            ownerCarDto.setCommunityId(feeDto.getCommunityId());
            List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);

            Assert.listOnlyOne(ownerCarDtos, "???????????????");
            ownerDto = new OwnerDto();
            ownerDto.setOwnerId(ownerCarDtos.get(0).getOwnerId());
            ownerDto.setCommunityId(feeDto.getCommunityId());
            List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryOwners(ownerDto);
            Assert.listOnlyOne(ownerDtos, "???????????????");
            return ownerDtos.get(0);
        }

        if (FeeDto.PAYER_OBJ_TYPE_CONTRACT.equals(feeDto.getPayerObjType())) {
            ContractDto contractDto = new ContractDto();
            contractDto.setContractId(feeDto.getPayerObjId());
            contractDto.setCommunityId(feeDto.getCommunityId());
            List<ContractDto> contractDtos = contractInnerServiceSMOImpl.queryContracts(contractDto);

            Assert.listOnlyOne(contractDtos, "???????????????");
            ownerDto = new OwnerDto();
            ownerDto.setOwnerId(contractDtos.get(0).getObjId());
            ownerDto.setCommunityId(feeDto.getCommunityId());
            List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryOwners(ownerDto);
            Assert.listOnlyOne(ownerDtos, "???????????????");
            return ownerDtos.get(0);
        }
        return null;
    }

    private OwnerDto getOwnerDtoByFeeAttr(FeeDto feeDto) {
        List<FeeAttrDto> feeAttrDtos = feeDto.getFeeAttrDtos();

        if (feeAttrDtos == null || feeAttrDtos.size() < 1) {
            return null;
        }

        OwnerDto ownerDto = new OwnerDto();
        for (FeeAttrDto feeAttrDto : feeAttrDtos) {
            if (feeAttrDto.getSpecCd().equals(FeeAttrDto.SPEC_CD_OWNER_ID)) {
                ownerDto.setOwnerId(feeAttrDto.getValue());
            }

            if (feeAttrDto.getSpecCd().equals(FeeAttrDto.SPEC_CD_OWNER_NAME)) {
                ownerDto.setName(feeAttrDto.getValue());
            }

            if (feeAttrDto.getSpecCd().equals(FeeAttrDto.SPEC_CD_OWNER_LINK)) {
                ownerDto.setLink(feeAttrDto.getValue());
            }
        }

        if (StringUtil.isEmpty(ownerDto.getOwnerId())) {
            return null;
        }

        return ownerDto;
    }

    @Override
    public void freshFeeObjName(List<FeeDto> feeDtos) {

        List<String> roomIds = new ArrayList<>();
        List<String> carIds = new ArrayList<>();
        for (FeeDto feeDto : feeDtos) {
            if (FeeDto.PAYER_OBJ_TYPE_ROOM.equals(feeDto.getPayerObjType())) {
                roomIds.add(feeDto.getPayerObjId());
            } else if (FeeDto.PAYER_OBJ_TYPE_CAR.equals(feeDto.getPayerObjType())) {
                carIds.add(feeDto.getPayerObjId());
            }
        }

        // ?????????????????? ??????????????????
        freshFeeObjNameByRoomId(feeDtos, roomIds);

        // ??????????????? ??? ????????????
        freshFeeObjNameByCarId(feeDtos, carIds);

    }

    /**
     * ?????????
     *
     * @param feeDtos
     * @param carIds
     */
    private void freshFeeObjNameByCarId(List<FeeDto> feeDtos, List<String> carIds) {

        if (carIds.size() < 1) {
            return;
        }


        OwnerCarDto ownerCarDto = new OwnerCarDto();
        ownerCarDto.setCommunityId(feeDtos.get(0).getCommunityId());
        ownerCarDto.setCarIds(carIds.toArray(new String[carIds.size()]));
        List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);

        if (ownerCarDtos == null || ownerCarDtos.size() < 1) {
            return;
        }

        List<String> psIds = new ArrayList<>();

        for (OwnerCarDto tmpOwnerCarDto : ownerCarDtos) {
            if (StringUtil.isEmpty(tmpOwnerCarDto.getPsId()) || tmpOwnerCarDto.getPsId().startsWith("-")) {
                continue;
            }
            psIds.add(tmpOwnerCarDto.getPsId());
        }

        //?????????????????????
        if (psIds.size() < 1) {
            for (OwnerCarDto tmpOwnerCarDto : ownerCarDtos) {
                for (FeeDto feeDto : feeDtos) {
                    if (!FeeDto.PAYER_OBJ_TYPE_CAR.equals(feeDto.getPayerObjType())) {
                        continue;
                    }

                    if (feeDto.getPayerObjId().equals(tmpOwnerCarDto.getCarId())) {
                        feeDto.setPayerObjName(tmpOwnerCarDto.getCarNum());
                    }
                }
            }
            return;
        }


        ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
        parkingSpaceDto.setCommunityId(feeDtos.get(0).getCommunityId());
        parkingSpaceDto.setPsIds(psIds.toArray(new String[psIds.size()]));
        List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);
        for (OwnerCarDto tmpOwnerCarDto : ownerCarDtos) {
            for (ParkingSpaceDto tmpParkingSpaceDto : parkingSpaceDtos) {
                if (tmpParkingSpaceDto.getPsId().equals(tmpOwnerCarDto.getPsId())) {
                    tmpOwnerCarDto.setAreaNum(tmpParkingSpaceDto.getAreaNum());
                    tmpOwnerCarDto.setNum(tmpParkingSpaceDto.getNum());
                }
            }
        }
        for (OwnerCarDto tmpOwnerCarDto : ownerCarDtos) {
            for (FeeDto feeDto : feeDtos) {
                if (!FeeDto.PAYER_OBJ_TYPE_CAR.equals(feeDto.getPayerObjType())) {
                    continue;
                }

                if (feeDto.getPayerObjId().equals(tmpOwnerCarDto.getCarId())) {
                    feeDto.setPayerObjName(tmpOwnerCarDto.getCarNum() + "(" + tmpOwnerCarDto.getAreaNum() + "?????????" + tmpOwnerCarDto.getNum() + "??????)");
                }
            }
        }

    }

    /**
     * ?????????????????????????????????
     *
     * @param feeDtos
     * @param roomIds
     */
    private void freshFeeObjNameByRoomId(List<FeeDto> feeDtos, List<String> roomIds) {

        if (roomIds.size() < 1) {
            return;
        }

        RoomDto roomDto = new RoomDto();
        roomDto.setRoomIds(roomIds.toArray(new String[roomIds.size()]));
        roomDto.setCommunityId(feeDtos.get(0).getCommunityId());
        List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);
        String objName = "";
        for (RoomDto tmpRoomDto : roomDtos) {
            for (FeeDto feeDto : feeDtos) {
                if (!FeeDto.PAYER_OBJ_TYPE_ROOM.equals(feeDto.getPayerObjType())) {
                    continue;
                }
                if (tmpRoomDto.getRoomId().equals(feeDto.getPayerObjId())) {
                    objName = tmpRoomDto.getFloorNum() + "-" + tmpRoomDto.getUnitNum() + "-" + tmpRoomDto.getRoomNum();
                    feeDto.setPayerObjName(objName);
                }
            }
        }
    }

    /**
     * ???????????? ??????????????????
     *
     * @param feeDto
     * @param cycles
     * @return
     */
    public String getFeeStateByCycles(FeeDto feeDto, String cycles) {
        double cycle = Double.parseDouble(cycles);
        Date endTime = feeDto.getEndTime();
        Calendar endCalender = Calendar.getInstance();
        endCalender.setTime(endTime);
        endCalender.add(Calendar.MONTH, new Double(Math.floor(cycle)).intValue());
//        Calendar futureDate = Calendar.getInstance();
//        futureDate.setTime(endCalender.getTime());
//        futureDate.add(Calendar.MONTH, 1);
        int futureDay = endCalender.getActualMaximum(Calendar.DAY_OF_MONTH);
        int hours = new Double((cycle - Math.floor(cycle)) * futureDay * 24).intValue();
        endCalender.add(Calendar.HOUR, hours);
        if (FeeDto.FEE_FLAG_ONCE.equals(feeDto.getFeeFlag())) {
            return FeeDto.STATE_FINISH;
        } else if (FeeDto.FEE_FLAG_CYCLE_ONCE.equals(feeDto.getFeeFlag())) {
            if ((endCalender.getTime()).after(feeDto.getDeadlineTime())) {
                return FeeDto.STATE_FINISH;
            }
        } else {
            if ((endCalender.getTime()).after(feeDto.getConfigEndTime())) {
                return FeeDto.STATE_FINISH;
            }
        }
        return FeeDto.STATE_DOING;
    }

    public Date getFeeEndTimeByCycles(FeeDto feeDto, String cycles) {
        double cycle = Double.parseDouble(cycles);

        Date endTime = feeDto.getEndTime();
        Calendar endCalender = Calendar.getInstance();
        endCalender.setTime(endTime);
        endCalender.add(Calendar.MONTH, new Double(Math.floor(cycle)).intValue());
//        Calendar futureDate = Calendar.getInstance();
//        futureDate.setTime(endCalender.getTime());
//        futureDate.add(Calendar.MONTH, 1);
        int futureDay = endCalender.getActualMaximum(Calendar.DAY_OF_MONTH);
        int hours = new Double((cycle - Math.floor(cycle)) * futureDay * 24).intValue();
        endCalender.add(Calendar.HOUR, hours);
        if (FeeDto.FEE_FLAG_ONCE.equals(feeDto.getFeeFlag())) {
            if (feeDto.getDeadlineTime() != null) {
                endCalender.setTime(feeDto.getDeadlineTime());
            } else if (!StringUtil.isEmpty(feeDto.getCurDegrees())) {
                endCalender.setTime(feeDto.getCurReadingTime());
            } else if (feeDto.getImportFeeEndTime() == null) {
                endCalender.setTime(feeDto.getConfigEndTime());
            } else {
                endCalender.setTime(feeDto.getImportFeeEndTime());
            }
        } else if (FeeDto.FEE_FLAG_CYCLE_ONCE.equals(feeDto.getFeeFlag())) {
            if (feeDto.getDeadlineTime() == null) {
                throw new IllegalArgumentException("????????????????????????????????????");
            }
            if ((endCalender.getTime()).after(feeDto.getDeadlineTime())) {
                endCalender.setTime(feeDto.getDeadlineTime());
            }
        } else {
            if ((endCalender.getTime()).after(feeDto.getConfigEndTime())) {
                endCalender.setTime(feeDto.getConfigEndTime());
            }
        }

        return endCalender.getTime();
    }


    @Override
    public double getCycle() {
        return 0;
    }

    @Override
    public double getReportFeePrice(ReportFeeDto tmpReportFeeDto, ReportRoomDto reportRoomDto, ReportCarDto reportCarDto) {
        BigDecimal feePrice = new BigDecimal(0.0);
        if (FeeDto.PAYER_OBJ_TYPE_ROOM.equals(tmpReportFeeDto.getPayerObjType())) { //????????????
            String computingFormula = tmpReportFeeDto.getComputingFormula();

            if ("1001".equals(computingFormula)) { //??????*??????+?????????
                //feePrice = Double.parseDouble(feeDto.getSquarePrice()) * Double.parseDouble(roomDtos.get(0).getBuiltUpArea()) + Double.parseDouble(feeDto.getAdditionalAmount());
                BigDecimal squarePrice = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getSquarePrice()));
                BigDecimal builtUpArea = new BigDecimal(Double.parseDouble(reportRoomDto.getBuiltUpArea()));
                BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getAdditionalAmount()));
                feePrice = squarePrice.multiply(builtUpArea).add(additionalAmount).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
            } else if ("2002".equals(computingFormula)) { // ????????????
                //feePrice = Double.parseDouble(feeDto.getAdditionalAmount());
                BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getAdditionalAmount()));
                feePrice = additionalAmount.setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
            } else if ("3003".equals(computingFormula)) { // ????????????
                BigDecimal squarePrice = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getSquarePrice()));
                BigDecimal builtUpArea = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getRoomArea()));
                BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getAdditionalAmount()));
                feePrice = squarePrice.multiply(builtUpArea).add(additionalAmount).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
            } else if ("1101".equals(computingFormula)) { // ??????
                BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getRoomRent()));
                feePrice = additionalAmount.setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
            } else if ("1102".equals(computingFormula)) { // ??????
                BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getRoomRent()));
                feePrice = additionalAmount.setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
            } else if ("4004".equals(computingFormula)) {
                feePrice = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getAmount()));
            } else if ("5005".equals(computingFormula)) {
                if (StringUtil.isEmpty(tmpReportFeeDto.getCurDegrees())) {
                    //throw new IllegalArgumentException("??????????????????");
                } else {
                    BigDecimal curDegree = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getCurDegrees()));
                    BigDecimal preDegree = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getPreDegrees()));
                    BigDecimal squarePrice = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getSquarePrice()));
                    BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getAdditionalAmount()));
                    BigDecimal sub = curDegree.subtract(preDegree);
                    feePrice = sub.multiply(squarePrice)
                            .add(additionalAmount)
                            .setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("6006".equals(computingFormula)) {
                feePrice = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getAmount()));
            } else if ("7007".equals(computingFormula)) { //???????????????
                feePrice = computeRoomCustomizeFormula(BeanConvertUtil.covertBean(tmpReportFeeDto, FeeDto.class), BeanConvertUtil.covertBean(reportRoomDto, RoomDto.class));
            } else if ("9009".equals(computingFormula)) {
                if (StringUtil.isEmpty(tmpReportFeeDto.getCurDegrees())) {
                    //throw new IllegalArgumentException("??????????????????");
                } else {
                    BigDecimal curDegree = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getCurDegrees()));
                    BigDecimal preDegree = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getPreDegrees()));
                    BigDecimal squarePrice = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getMwPrice()));
                    BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getAdditionalAmount()));
                    BigDecimal sub = curDegree.subtract(preDegree);
                    feePrice = sub.multiply(squarePrice)
                            .add(additionalAmount)
                            .setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else {
                throw new IllegalArgumentException("????????????????????????");
            }
        } else if (FeeDto.PAYER_OBJ_TYPE_CAR.equals(tmpReportFeeDto.getPayerObjType())) {//????????????
            String computingFormula = tmpReportFeeDto.getComputingFormula();

            if ("1001".equals(computingFormula)) { //??????*??????+?????????
                BigDecimal squarePrice = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getSquarePrice()));
                BigDecimal builtUpArea = new BigDecimal(Double.parseDouble("0"));
                BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getAdditionalAmount()));
                feePrice = squarePrice.multiply(builtUpArea).add(additionalAmount).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
            } else if ("2002".equals(computingFormula)) { // ????????????
                //feePrice = Double.parseDouble(feeDto.getAdditionalAmount());
                BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getAdditionalAmount()));
                feePrice = additionalAmount.setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
            } else if ("3003".equals(computingFormula)) { // ????????????
                //feePrice = Double.parseDouble(feeDto.getAdditionalAmount());
                feePrice = new BigDecimal(0);
            } else if ("1101".equals(computingFormula)) { // ??????
                feePrice = new BigDecimal(0);
            } else if ("1102".equals(computingFormula)) { // ??????
                feePrice = new BigDecimal(0);
            } else if ("4004".equals(computingFormula)) {
                feePrice = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getAmount()));
            } else if ("5005".equals(computingFormula)) {
                if (StringUtil.isEmpty(tmpReportFeeDto.getCurDegrees())) {
                    throw new IllegalArgumentException("??????????????????");
                } else {
                    BigDecimal curDegree = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getCurDegrees()));
                    BigDecimal preDegree = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getPreDegrees()));
                    BigDecimal squarePrice = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getSquarePrice()));
                    BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getAdditionalAmount()));
                    BigDecimal sub = curDegree.subtract(preDegree);
                    feePrice = sub.multiply(squarePrice)
                            .add(additionalAmount)
                            .setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("6006".equals(computingFormula)) {
                feePrice = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getAmount()));
            } else if ("7007".equals(computingFormula)) { //???????????????
                feePrice = computeCarCustomizeFormula(BeanConvertUtil.covertBean(tmpReportFeeDto, FeeDto.class), BeanConvertUtil.covertBean(reportCarDto, OwnerCarDto.class));
            } else if ("9009".equals(computingFormula)) {
                if (StringUtil.isEmpty(tmpReportFeeDto.getCurDegrees())) {
                    throw new IllegalArgumentException("??????????????????");
                } else {
                    BigDecimal curDegree = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getCurDegrees()));
                    BigDecimal preDegree = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getPreDegrees()));
                    BigDecimal squarePrice = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getMwPrice()));
                    BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(tmpReportFeeDto.getAdditionalAmount()));
                    BigDecimal sub = curDegree.subtract(preDegree);
                    feePrice = sub.multiply(squarePrice)
                            .add(additionalAmount)
                            .setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else {
                throw new IllegalArgumentException("????????????????????????");
            }
        }
        return feePrice.setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    @Override
    public Map getFeePrice(FeeDto feeDto) {
        return getFeePrice(feeDto, null);
    }

    /**
     * //????????????
     * //                    Map<String, Object> cycleResults = dateDiff(feeDto.getEndTime(), feeDto.getCustEndTime());
     * //                    //????????????0
     * //                    Integer months = Integer.valueOf(cycleResults.get("months").toString());
     * //                    Integer days = Integer.valueOf(cycleResults.get("days").toString());
     * //                    Integer startMonthDays = Integer.valueOf(cycleResults.get("startMonthDays").toString());
     * //                    Integer endMonthDays = Integer.valueOf(cycleResults.get("endMonthDays").toString());
     * //                    String isOneMonth = cycleResults.get("isOneMonth").toString();
     * //                    //?????????
     * //                    if (months > 0 && days == 0) {
     * //                        BigDecimal cycle = new BigDecimal(months);
     * //                        feeTotalPrice = (squarePrice.multiply(builtUpArea).add(additionalAmount)).multiply(cycle).setScale(3, BigDecimal.ROUND_HALF_UP);
     * //                    }
     * //                    //???????????????   ?????????*??????+????????????*??????+((??????*??????+?????????)/?????????)*????????????
     * //                    if (months > 0 && days > 0) {
     * //                        BigDecimal cycle = new BigDecimal(months);
     * //                        BigDecimal endMonthDayss = new BigDecimal(endMonthDays);
     * //                        BigDecimal dayss = new BigDecimal(days);
     * //                        BigDecimal monthPrice = squarePrice.multiply(builtUpArea).add(additionalAmount);
     * //                        feeTotalPrice = (monthPrice).multiply(cycle).add(monthPrice.divide(endMonthDayss).multiply(dayss)).setScale(3, BigDecimal.ROUND_HALF_UP);
     * //                    }
     * //                    //????????? ????????????  ((??????*??????+?????????)/?????????????????????)*????????????+((??????*??????+?????????)/?????????????????????)*????????????
     * //                    if (months == 0 && days > 0 && "true".equals(isOneMonth)) {
     * //                        BigDecimal startEndOfMonth = new BigDecimal(cycleResults.get("startEndOfMonth").toString());
     * //                        BigDecimal endBeginningOfMonth = new BigDecimal(cycleResults.get("endBeginningOfMonth").toString());
     * //                        BigDecimal endMonthDayss = new BigDecimal(endMonthDays);
     * //                        BigDecimal startMonthDayss = new BigDecimal(startMonthDays);
     * //                        BigDecimal monthPrice = squarePrice.multiply(builtUpArea).add(additionalAmount);
     * //                        feeTotalPrice = monthPrice.divide(startMonthDayss, 4, BigDecimal.ROUND_HALF_UP).multiply(startEndOfMonth).add(monthPrice.divide(endMonthDayss, 4, BigDecimal.ROUND_HALF_UP).multiply(endBeginningOfMonth)).setScale(3, BigDecimal.ROUND_HALF_UP);
     * //                    }
     * //                    //???????????? ????????????  (??????*??????+?????????/?????????????????????)*????????????
     * //                    if (months == 0 && days > 0 && "false".equals(isOneMonth)) {
     * //                        BigDecimal cycle = new BigDecimal(days);
     * //                        BigDecimal startMonthDayss = new BigDecimal(startMonthDays);
     * //                        BigDecimal monthPrice = squarePrice.multiply(builtUpArea).add(additionalAmount);
     * //                        feeTotalPrice = monthPrice.divide(startMonthDayss, 4, BigDecimal.ROUND_HALF_UP).multiply(cycle).setScale(3, BigDecimal.ROUND_HALF_UP);
     * //                    }
     *
     * @param feeDto
     * @param roomDto
     * @return
     */
    @Override
    public Map getFeePrice(FeeDto feeDto, RoomDto roomDto) {
        BigDecimal feePrice = new BigDecimal("0.0");
        BigDecimal feeTotalPrice = new BigDecimal(0.0);
        Map<String, Object> feeAmount = new HashMap<>();
        if (Environment.isOwnerPhone(java110Properties)) {
            return getOwnerPhoneFee(feeAmount);
        }
        if (FeeDto.PAYER_OBJ_TYPE_ROOM.equals(feeDto.getPayerObjType())) { //????????????
            String computingFormula = feeDto.getComputingFormula();
            if (roomDto == null) {
                roomDto = new RoomDto();
                roomDto.setRoomId(feeDto.getPayerObjId());
                roomDto.setCommunityId(feeDto.getCommunityId());
                List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);
                if (roomDtos == null || roomDtos.size() != 1) {
                    throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "?????????????????????????????????????????? roomId=" + feeDto.getPayerObjId());
                }
                roomDto = roomDtos.get(0);
            }
            if ("1001".equals(computingFormula)) { //??????*??????+?????????
                //feePrice = Double.parseDouble(feeDto.getSquarePrice()) * Double.parseDouble(roomDtos.get(0).getBuiltUpArea()) + Double.parseDouble(feeDto.getAdditionalAmount());
                BigDecimal squarePrice = new BigDecimal(Double.parseDouble(feeDto.getSquarePrice()));
                BigDecimal builtUpArea = new BigDecimal(Double.parseDouble(roomDto.getBuiltUpArea()));
                BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(feeDto.getAdditionalAmount()));
                feePrice = squarePrice.multiply(builtUpArea).add(additionalAmount).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                BigDecimal cycle = null;
                if (!StringUtil.isEmpty(feeDto.getCycle())) {
                    cycle = new BigDecimal(feeDto.getCycle());
                }
                if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                    cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                }
                if (cycle == null) {
                    feeTotalPrice = new BigDecimal(0);
                } else {
                    feeTotalPrice = (squarePrice.multiply(builtUpArea).add(additionalAmount)).multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("2002".equals(computingFormula)) { // ????????????
                //feePrice = Double.parseDouble(feeDto.getAdditionalAmount());
                BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(feeDto.getAdditionalAmount()));
                feePrice = additionalAmount.setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                BigDecimal cycle = null;
                if (!StringUtil.isEmpty(feeDto.getCycle())) {
                    cycle = new BigDecimal(feeDto.getCycle());
                }
                if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                    cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                }
                if (cycle == null) {
                    feeTotalPrice = new BigDecimal(0);
                } else {
                    feeTotalPrice = additionalAmount.multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("3003".equals(computingFormula)) { // ????????????
                BigDecimal squarePrice = new BigDecimal(Double.parseDouble(feeDto.getSquarePrice()));
                BigDecimal builtUpArea = new BigDecimal(Double.parseDouble(roomDto.getRoomArea()));
                BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(feeDto.getAdditionalAmount()));
                feePrice = squarePrice.multiply(builtUpArea).add(additionalAmount).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                BigDecimal cycle = null;
                if (!StringUtil.isEmpty(feeDto.getCycle())) {
                    cycle = new BigDecimal(feeDto.getCycle());
                }
                if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                    cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                }
                if (cycle == null) {
                    feeTotalPrice = new BigDecimal(0);
                } else {
                    feeTotalPrice = (squarePrice.multiply(builtUpArea).add(additionalAmount)).multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("1101".equals(computingFormula)) { // ??????
                BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(roomDto.getRoomRent()));
                feePrice = additionalAmount.setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                BigDecimal cycle = null;
                if (!StringUtil.isEmpty(feeDto.getCycle())) {
                    cycle = new BigDecimal(feeDto.getCycle());
                }
                if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                    cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                }
                if (cycle == null) {
                    feeTotalPrice = new BigDecimal(0);
                } else {
                    feeTotalPrice = additionalAmount.multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("1102".equals(computingFormula)) { // ??????
                BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(roomDto.getRoomRent()));
                feePrice = additionalAmount.setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                BigDecimal cycle = null;
                if (!StringUtil.isEmpty(feeDto.getCycle())) {
                    cycle = new BigDecimal(feeDto.getCycle());
                }
                if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                    cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                }
                if (cycle == null) {
                    feeTotalPrice = new BigDecimal(0);
                } else {
                    feeTotalPrice = additionalAmount.multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("4004".equals(computingFormula)) {  //????????????
                feePrice = new BigDecimal(Double.parseDouble(feeDto.getAmount()));
                BigDecimal cycle = null;
                if (!StringUtil.isEmpty(feeDto.getCycle())) {
                    cycle = new BigDecimal(feeDto.getCycle());
                }
                if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                    cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                }
                if (cycle == null) {
                    feeTotalPrice = new BigDecimal(0);
                } else {
                    feeTotalPrice = feePrice.multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("5005".equals(computingFormula)) {  //(????????????-????????????)*??????+?????????
                if (StringUtil.isEmpty(feeDto.getCurDegrees())) {
                    //throw new IllegalArgumentException("??????????????????");
                } else {
                    BigDecimal curDegree = new BigDecimal(Double.parseDouble(feeDto.getCurDegrees()));
                    BigDecimal preDegree = new BigDecimal(Double.parseDouble(feeDto.getPreDegrees()));
                    BigDecimal squarePrice = new BigDecimal(Double.parseDouble(feeDto.getSquarePrice()));
                    BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(feeDto.getAdditionalAmount()));
                    BigDecimal sub = curDegree.subtract(preDegree);
                    feePrice = sub.multiply(squarePrice)
                            .add(additionalAmount)
                            .setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);

                    BigDecimal cycle = null;
                    if (!StringUtil.isEmpty(feeDto.getCycle())) {
                        cycle = new BigDecimal(feeDto.getCycle());
                    }
                    if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                        cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                    }
                    if (cycle == null) {
                        feeTotalPrice = new BigDecimal(0);
                    } else {
                        feeTotalPrice = (sub.multiply(squarePrice).add(additionalAmount)).multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                    }
                }
            } else if ("6006".equals(computingFormula)) {
                feePrice = new BigDecimal(Double.parseDouble(feeDto.getAmount()));

                BigDecimal cycle = null;
                if (!StringUtil.isEmpty(feeDto.getCycle())) {
                    cycle = new BigDecimal(feeDto.getCycle());
                }
                if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                    cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                }
                if (cycle == null) {
                    feeTotalPrice = new BigDecimal(0);
                } else {
                    feeTotalPrice = feePrice.multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("7007".equals(computingFormula)) { //???????????????
                if (roomDto == null) {
                    roomDto = new RoomDto();
                    roomDto.setRoomId(feeDto.getPayerObjId());
                    roomDto.setCommunityId(feeDto.getCommunityId());
                    List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);
                    if (roomDtos == null || roomDtos.size() != 1) {
                        throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "?????????????????????????????????????????? roomId=" + feeDto.getPayerObjId());
                    }
                    roomDto = roomDtos.get(0);
                }
                feePrice = computeRoomCustomizeFormula(feeDto, roomDto);
                BigDecimal cycle = null;
                if (!StringUtil.isEmpty(feeDto.getCycle())) {
                    cycle = new BigDecimal(feeDto.getCycle());
                }
                if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                    cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                }
                if (cycle == null) {
                    feeTotalPrice = new BigDecimal(0);
                } else {
                    feeTotalPrice = feePrice.multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("8008".equals(computingFormula)) {  //??????????????????
                feePrice = new BigDecimal(Double.parseDouble(feeDto.getAmount()));
                BigDecimal cycle = null;
                if (!StringUtil.isEmpty(feeDto.getCycle())) {
                    cycle = new BigDecimal(feeDto.getCycle());
                }
                if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                    cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                }
                if (cycle == null) {
                    feeTotalPrice = new BigDecimal(0);
                } else {
                    feeTotalPrice = feePrice.multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("9009".equals(computingFormula)) {  //(????????????-????????????)*????????????+?????????
                if (StringUtil.isEmpty(feeDto.getCurDegrees())) {
                    //throw new IllegalArgumentException("??????????????????");
                } else {
                    BigDecimal curDegree = new BigDecimal(Double.parseDouble(feeDto.getCurDegrees()));
                    BigDecimal preDegree = new BigDecimal(Double.parseDouble(feeDto.getPreDegrees()));
                    BigDecimal squarePrice = new BigDecimal(Double.parseDouble(feeDto.getMwPrice()));
                    BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(feeDto.getAdditionalAmount()));
                    BigDecimal sub = curDegree.subtract(preDegree);
                    feePrice = sub.multiply(squarePrice)
                            .add(additionalAmount)
                            .setScale(2, BigDecimal.ROUND_HALF_UP);
                    BigDecimal cycle = null;
                    if (!StringUtil.isEmpty(feeDto.getCycle())) {
                        cycle = new BigDecimal(feeDto.getCycle());
                    }
                    if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                        cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                    }
                    if (cycle == null) {
                        feeTotalPrice = new BigDecimal(0);
                    } else {
                        feeTotalPrice = (sub.multiply(squarePrice).add(additionalAmount)).multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                    }
                }
            } else {
                throw new IllegalArgumentException("????????????????????????");
            }
        } else if (FeeDto.PAYER_OBJ_TYPE_CAR.equals(feeDto.getPayerObjType())) {//????????????
            String computingFormula = feeDto.getComputingFormula();

            OwnerCarDto ownerCarDto = new OwnerCarDto();
            ownerCarDto.setCarTypeCd("1001"); //????????????
            ownerCarDto.setCommunityId(feeDto.getCommunityId());
            ownerCarDto.setCarId(feeDto.getPayerObjId());
            List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);
            Assert.listOnlyOne(ownerCarDtos, "?????????????????????");
            if ("1001".equals(computingFormula)) { //??????*??????+?????????
                ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
                parkingSpaceDto.setCommunityId(feeDto.getCommunityId());
                parkingSpaceDto.setPsId(ownerCarDtos.get(0).getPsId());
                List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);
                if (parkingSpaceDtos == null || parkingSpaceDtos.size() < 1) { //???????????????
                    throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "?????????????????????????????????????????????");
                }
                BigDecimal squarePrice = new BigDecimal(Double.parseDouble(feeDto.getSquarePrice()));
                BigDecimal builtUpArea = new BigDecimal(Double.parseDouble(parkingSpaceDtos.get(0).getArea()));
                BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(feeDto.getAdditionalAmount()));
                feePrice = squarePrice.multiply(builtUpArea).add(additionalAmount).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                BigDecimal cycle = null;
                if (!StringUtil.isEmpty(feeDto.getCycle())) {
                    cycle = new BigDecimal(feeDto.getCycle());
                }
                if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                    cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                }
                if (cycle == null) {
                    feeTotalPrice = new BigDecimal(0);
                } else {
                    feeTotalPrice = (squarePrice.multiply(builtUpArea).add(additionalAmount)).multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("2002".equals(computingFormula)) { // ????????????
                //feePrice = Double.parseDouble(feeDto.getAdditionalAmount());
                BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(feeDto.getAdditionalAmount()));
                feePrice = additionalAmount.setScale(4, BigDecimal.ROUND_HALF_UP);

                BigDecimal cycle = null;
                if (!StringUtil.isEmpty(feeDto.getCycle())) {
                    cycle = new BigDecimal(feeDto.getCycle());
                }
                if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                    cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                }
                if (cycle == null) {
                    feeTotalPrice = new BigDecimal(0);
                } else {
                    feeTotalPrice = additionalAmount.multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("3003".equals(computingFormula)) { // ????????????
                BigDecimal squarePrice = new BigDecimal(Double.parseDouble(feeDto.getSquarePrice()));
                BigDecimal builtUpArea = new BigDecimal(Double.parseDouble(roomDto.getRoomArea()));
                BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(feeDto.getAdditionalAmount()));
                feePrice = squarePrice.multiply(builtUpArea).add(additionalAmount).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);

                BigDecimal cycle = null;
                if (!StringUtil.isEmpty(feeDto.getCycle())) {
                    cycle = new BigDecimal(feeDto.getCycle());
                }
                if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                    cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                }
                if (cycle == null) {
                    feeTotalPrice = new BigDecimal(0);
                } else {
                    feeTotalPrice = (squarePrice.multiply(builtUpArea).add(additionalAmount)).multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("1101".equals(computingFormula)) { // ??????
                BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(roomDto.getRoomRent()));
                feePrice = additionalAmount.setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);

                BigDecimal cycle = null;
                if (!StringUtil.isEmpty(feeDto.getCycle())) {
                    cycle = new BigDecimal(feeDto.getCycle());
                }
                if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                    cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                }
                if (cycle == null) {
                    feeTotalPrice = new BigDecimal(0);
                } else {
                    feeTotalPrice = additionalAmount.multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("1102".equals(computingFormula)) { // ??????
                BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(roomDto.getRoomRent()));
                feePrice = additionalAmount.setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);

                BigDecimal cycle = null;
                if (!StringUtil.isEmpty(feeDto.getCycle())) {
                    cycle = new BigDecimal(feeDto.getCycle());
                }
                if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                    cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                }
                if (cycle == null) {
                    feeTotalPrice = new BigDecimal(0);
                } else {
                    feeTotalPrice = additionalAmount.multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("4004".equals(computingFormula)) {
                feePrice = new BigDecimal(Double.parseDouble(feeDto.getAmount()));
                BigDecimal cycle = null;
                if (!StringUtil.isEmpty(feeDto.getCycle())) {
                    cycle = new BigDecimal(feeDto.getCycle());
                }
                if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                    cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                }
                if (cycle == null) {
                    feeTotalPrice = new BigDecimal(0);
                } else {
                    feeTotalPrice = feePrice.multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("5005".equals(computingFormula)) {
                if (StringUtil.isEmpty(feeDto.getCurDegrees())) {
                    throw new IllegalArgumentException("??????????????????");
                } else {
                    BigDecimal curDegree = new BigDecimal(Double.parseDouble(feeDto.getCurDegrees()));
                    BigDecimal preDegree = new BigDecimal(Double.parseDouble(feeDto.getPreDegrees()));
                    BigDecimal squarePrice = new BigDecimal(Double.parseDouble(feeDto.getSquarePrice()));
                    BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(feeDto.getAdditionalAmount()));
                    BigDecimal sub = curDegree.subtract(preDegree);
                    feePrice = sub.multiply(squarePrice)
                            .add(additionalAmount)
                            .setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);

                    BigDecimal cycle = null;
                    if (!StringUtil.isEmpty(feeDto.getCycle())) {
                        cycle = new BigDecimal(feeDto.getCycle());
                    }
                    if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                        cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                    }
                    if (cycle == null) {
                        feeTotalPrice = new BigDecimal(0);
                    } else {
                        feeTotalPrice = (sub.multiply(squarePrice).add(additionalAmount)).multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                    }
                }
            } else if ("6006".equals(computingFormula)) {
                feePrice = new BigDecimal(Double.parseDouble(feeDto.getAmount()));

                BigDecimal cycle = null;
                if (!StringUtil.isEmpty(feeDto.getCycle())) {
                    cycle = new BigDecimal(feeDto.getCycle());
                }
                if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                    cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                }
                if (cycle == null) {
                    feeTotalPrice = new BigDecimal(0);
                } else {
                    feeTotalPrice = feePrice.multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("7007".equals(computingFormula)) { //???????????????
                feePrice = computeCarCustomizeFormula(feeDto, ownerCarDtos.get(0));

                BigDecimal cycle = null;
                if (!StringUtil.isEmpty(feeDto.getCycle())) {
                    cycle = new BigDecimal(feeDto.getCycle());
                }
                if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                    cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                }
                if (cycle == null) {
                    feeTotalPrice = new BigDecimal(0);
                } else {
                    feeTotalPrice = feePrice.multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("9009".equals(computingFormula)) {  //(????????????-????????????)*????????????+?????????
                if (StringUtil.isEmpty(feeDto.getCurDegrees())) {
                    //throw new IllegalArgumentException("??????????????????");
                } else {
                    BigDecimal curDegree = new BigDecimal(Double.parseDouble(feeDto.getCurDegrees()));
                    BigDecimal preDegree = new BigDecimal(Double.parseDouble(feeDto.getPreDegrees()));
                    BigDecimal squarePrice = new BigDecimal(Double.parseDouble(feeDto.getMwPrice()));
                    BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(feeDto.getAdditionalAmount()));
                    BigDecimal sub = curDegree.subtract(preDegree);
                    feePrice = sub.multiply(squarePrice)
                            .add(additionalAmount)
                            .setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);

                    BigDecimal cycle = null;
                    if (!StringUtil.isEmpty(feeDto.getCycle())) {
                        cycle = new BigDecimal(feeDto.getCycle());
                    }
                    if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                        cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                    }
                    if (cycle == null) {
                        feeTotalPrice = new BigDecimal(0);
                    } else {
                        feeTotalPrice = (sub.multiply(squarePrice).add(additionalAmount)).multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                    }
                }
            } else {
                throw new IllegalArgumentException("????????????????????????");
            }
        } else if (FeeDto.PAYER_OBJ_TYPE_CONTRACT.equals(feeDto.getPayerObjType())) { //????????????
            String computingFormula = feeDto.getComputingFormula();

            //????????????????????????
            ContractRoomDto contractRoomDto = new ContractRoomDto();
            contractRoomDto.setContractId(feeDto.getPayerObjId());
            contractRoomDto.setCommunityId(feeDto.getCommunityId());
            List<ContractRoomDto> contractRoomDtos = contractRoomInnerServiceSMOImpl.queryContractRooms(contractRoomDto);

            if ("1001".equals(computingFormula)) { //??????*??????+?????????
                //feePrice = Double.parseDouble(feeDto.getSquarePrice()) * Double.parseDouble(roomDtos.get(0).getBuiltUpArea()) + Double.parseDouble(feeDto.getAdditionalAmount());
                BigDecimal squarePrice = new BigDecimal(Double.parseDouble(feeDto.getSquarePrice()));
                BigDecimal builtUpArea = new BigDecimal(0);
                for (ContractRoomDto tmpContractRoomDto : contractRoomDtos) {
                    builtUpArea = builtUpArea.add(new BigDecimal(Double.parseDouble(tmpContractRoomDto.getBuiltUpArea())));
                }
                feeDto.setBuiltUpArea(builtUpArea.doubleValue() + "");
                BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(feeDto.getAdditionalAmount()));
                feePrice = squarePrice.multiply(builtUpArea).add(additionalAmount).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                BigDecimal cycle = null;
                if (!StringUtil.isEmpty(feeDto.getCycle())) {
                    cycle = new BigDecimal(feeDto.getCycle());
                }
                if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                    cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                }
                if (cycle == null) {
                    feeTotalPrice = new BigDecimal(0);
                } else {
                    feeTotalPrice = (squarePrice.multiply(builtUpArea).add(additionalAmount)).multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("2002".equals(computingFormula)) { // ????????????
                //feePrice = Double.parseDouble(feeDto.getAdditionalAmount());
                BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(feeDto.getAdditionalAmount()));
//                BigDecimal roomDount = new BigDecimal(contractRoomDtos.size());
//                additionalAmount = additionalAmount.multiply(roomDount);
                feePrice = additionalAmount.setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);

                BigDecimal cycle = null;
                if (!StringUtil.isEmpty(feeDto.getCycle())) {
                    cycle = new BigDecimal(feeDto.getCycle());
                }
                if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                    cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                }
                if (cycle == null) {
                    feeTotalPrice = new BigDecimal(0);
                } else {
                    feeTotalPrice = additionalAmount.multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("3003".equals(computingFormula)) { // ????????????
                BigDecimal squarePrice = new BigDecimal(Double.parseDouble(feeDto.getSquarePrice()));
                BigDecimal builtUpArea = new BigDecimal(Double.parseDouble(roomDto.getRoomArea()));
                BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(feeDto.getAdditionalAmount()));
                feePrice = squarePrice.multiply(builtUpArea).add(additionalAmount).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);

                BigDecimal cycle = null;
                if (!StringUtil.isEmpty(feeDto.getCycle())) {
                    cycle = new BigDecimal(feeDto.getCycle());
                }
                if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                    cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                }
                if (cycle == null) {
                    feeTotalPrice = new BigDecimal(0);
                } else {
                    feeTotalPrice = (squarePrice.multiply(builtUpArea).add(additionalAmount)).multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("1101".equals(computingFormula)) { // ??????
                BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(roomDto.getRoomRent()));
                feePrice = additionalAmount.setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);

                BigDecimal cycle = null;
                if (!StringUtil.isEmpty(feeDto.getCycle())) {
                    cycle = new BigDecimal(feeDto.getCycle());
                }
                if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                    cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                }
                if (cycle == null) {
                    feeTotalPrice = new BigDecimal(0);
                } else {
                    feeTotalPrice = additionalAmount.multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("1102".equals(computingFormula)) { // ??????
                BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(roomDto.getRoomRent()));
                feePrice = additionalAmount.setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);

                BigDecimal cycle = null;
                if (!StringUtil.isEmpty(feeDto.getCycle())) {
                    cycle = new BigDecimal(feeDto.getCycle());
                }
                if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                    cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                }
                if (cycle == null) {
                    feeTotalPrice = new BigDecimal(0);
                } else {
                    feeTotalPrice = additionalAmount.multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("4004".equals(computingFormula)) {  //????????????
                feePrice = new BigDecimal(Double.parseDouble(feeDto.getAmount()));

                BigDecimal cycle = null;
                if (!StringUtil.isEmpty(feeDto.getCycle())) {
                    cycle = new BigDecimal(feeDto.getCycle());
                }
                if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                    cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                }
                if (cycle == null) {
                    feeTotalPrice = new BigDecimal(0);
                } else {
                    feeTotalPrice = feePrice.multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("5005".equals(computingFormula)) {  //(????????????-????????????)*??????+?????????
                if (StringUtil.isEmpty(feeDto.getCurDegrees())) {
                    //throw new IllegalArgumentException("??????????????????");
                } else {
                    BigDecimal curDegree = new BigDecimal(Double.parseDouble(feeDto.getCurDegrees()));
                    BigDecimal preDegree = new BigDecimal(Double.parseDouble(feeDto.getPreDegrees()));
                    BigDecimal squarePrice = new BigDecimal(Double.parseDouble(feeDto.getSquarePrice()));
                    BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(feeDto.getAdditionalAmount()));
                    BigDecimal sub = curDegree.subtract(preDegree);
                    feePrice = sub.multiply(squarePrice)
                            .add(additionalAmount)
                            .setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);


                    BigDecimal cycle = null;
                    if (!StringUtil.isEmpty(feeDto.getCycle())) {
                        cycle = new BigDecimal(feeDto.getCycle());
                    }
                    if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                        cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                    }
                    if (cycle == null) {
                        feeTotalPrice = new BigDecimal(0);
                    } else {
                        feeTotalPrice = (sub.multiply(squarePrice).add(additionalAmount)).multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                    }
                }
            } else if ("6006".equals(computingFormula)) {
                feePrice = new BigDecimal(Double.parseDouble(feeDto.getAmount()));

                BigDecimal cycle = null;
                if (!StringUtil.isEmpty(feeDto.getCycle())) {
                    cycle = new BigDecimal(feeDto.getCycle());
                }
                if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                    cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                }
                if (cycle == null) {
                    feeTotalPrice = new BigDecimal(0);
                } else {
                    feeTotalPrice = feePrice.multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("7007".equals(computingFormula)) { //???????????????
                feePrice = computeContractCustomizeFormula(feeDto, contractRoomDtos);

                BigDecimal cycle = null;
                if (!StringUtil.isEmpty(feeDto.getCycle())) {
                    cycle = new BigDecimal(feeDto.getCycle());
                }
                if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                    cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                }
                if (cycle == null) {
                    feeTotalPrice = new BigDecimal(0);
                } else {
                    feeTotalPrice = feePrice.multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("8008".equals(computingFormula)) {  //??????????????????
                feePrice = new BigDecimal(Double.parseDouble(feeDto.getAmount()));

                BigDecimal cycle = null;
                if (!StringUtil.isEmpty(feeDto.getCycle())) {
                    cycle = new BigDecimal(feeDto.getCycle());
                }
                if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                    cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                }
                if (cycle == null) {
                    feeTotalPrice = new BigDecimal(0);
                } else {
                    feeTotalPrice = feePrice.multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                }
            } else if ("9009".equals(computingFormula)) {  //(????????????-????????????)*????????????+?????????
                if (StringUtil.isEmpty(feeDto.getCurDegrees())) {
                    //throw new IllegalArgumentException("??????????????????");
                } else {
                    BigDecimal curDegree = new BigDecimal(Double.parseDouble(feeDto.getCurDegrees()));
                    BigDecimal preDegree = new BigDecimal(Double.parseDouble(feeDto.getPreDegrees()));
                    BigDecimal squarePrice = new BigDecimal(Double.parseDouble(feeDto.getMwPrice()));
                    BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(feeDto.getAdditionalAmount()));
                    BigDecimal sub = curDegree.subtract(preDegree);
                    feePrice = sub.multiply(squarePrice)
                            .add(additionalAmount)
                            .setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);

                    BigDecimal cycle = null;
                    if (!StringUtil.isEmpty(feeDto.getCycle())) {
                        cycle = new BigDecimal(feeDto.getCycle());
                    }
                    if (!StringUtil.isEmpty(feeDto.getCustEndTime())) {
                        cycle = new BigDecimal(dayCompare(feeDto.getEndTime(), DateUtil.getDateFromStringB(feeDto.getCustEndTime())));
                    }
                    if (cycle == null) {
                        feeTotalPrice = new BigDecimal(0);
                    } else {
                        feeTotalPrice = sub.multiply(squarePrice).add(additionalAmount).multiply(cycle).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                    }
                }
            } else {
                throw new IllegalArgumentException("????????????????????????");
            }
        }

        feePrice.setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP).doubleValue();
        feeAmount.put("feePrice", feePrice);
        feeAmount.put("feeTotalPrice", feeTotalPrice);
        return feeAmount;
    }

    /**
     * C ??????????????????????????????
     * <p>
     * R ??????????????????
     *
     * @param feeDto
     * @param ownerCarDto
     * @return
     */
    private BigDecimal computeCarCustomizeFormula(FeeDto feeDto, OwnerCarDto ownerCarDto) {
        String value = feeDto.getComputingFormulaText();
        value = value.replace("\n", "")
                .replace("\r", "")
                .trim();

        if (value.contains("C")) { //??????????????????
            CommunityDto communityDto = new CommunityDto();
            communityDto.setCommunityId(feeDto.getCommunityId());
            List<CommunityDto> communityDtos = communityInnerServiceSMOImpl.queryCommunitys(communityDto);
            if (communityDtos == null || communityDtos.size() < 1) {
                value = value.replace("C", "0");
            } else {
                value = value.replace("C", communityDtos.get(0).getCommunityArea());
            }
        } else if (value.contains("R")) { //?????? ????????????
            ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
            parkingSpaceDto.setCommunityId(feeDto.getCommunityId());
            parkingSpaceDto.setPsId(ownerCarDto.getPsId());
            List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);
            if (parkingSpaceDtos == null || parkingSpaceDtos.size() < 1) { //???????????????
                //throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "?????????????????????????????????????????????");
                value = value.replace("R", "0");
            } else {
                value = value.replace("R", parkingSpaceDtos.get(0).getArea());
            }
        }

        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");
        BigDecimal valueObj = null;
        try {
            value = engine.eval(value).toString();
            valueObj = new BigDecimal(Double.parseDouble(value));
        } catch (Exception e) {
            //throw new IllegalArgumentException("?????????????????????????????????" + feeDto.getComputingFormulaText() + "???,?????? ???" + value + "?????????");
            valueObj = new BigDecimal(0);
        }

        if (valueObj.doubleValue() < 0) {
            return new BigDecimal(0);
        }

        return valueObj;
    }

    /**
     * ?????????????????????
     *
     * @param feeDto
     * @return C ??????????????????????????????
     * F ??????????????????????????????
     * U ??????????????????????????????
     * R ??????????????????
     * X ???????????????????????????????????????????????????
     * L ??????????????????
     */
    private BigDecimal computeContractCustomizeFormula(FeeDto feeDto, List<ContractRoomDto> contractRoomDtos) {

        BigDecimal total = new BigDecimal(0.0);
        for (ContractRoomDto contractRoomDto : contractRoomDtos) {
            total = total.add(computeRoomCustomizeFormula(feeDto, contractRoomDto));
        }
        return total;
    }

    /**
     * ?????????????????????
     *
     * @param feeDto
     * @param roomDto
     * @return C ??????????????????????????????
     * F ??????????????????????????????
     * U ??????????????????????????????
     * R ??????????????????
     * X ???????????????????????????????????????????????????
     * L ??????????????????
     */
    private BigDecimal computeRoomCustomizeFormula(FeeDto feeDto, RoomDto roomDto) {

        String value = feeDto.getComputingFormulaText();

        if (StringUtil.isEmpty(value)) {
            return new BigDecimal(0);
        }

        value = value.replace("\n", "")
                .replace("\r", "")
                .trim();

        if (value.contains("C")) { //??????????????????
            CommunityDto communityDto = new CommunityDto();
            communityDto.setCommunityId(feeDto.getCommunityId());
            List<CommunityDto> communityDtos = communityInnerServiceSMOImpl.queryCommunitys(communityDto);
            if (communityDtos == null || communityDtos.size() < 1) {
                value = value.replace("C", "0");
            } else {
                value = value.replace("C", communityDtos.get(0).getCommunityArea());
            }
        } else if (value.contains("F")) { //????????????
            value = value.replace("F", roomDto.getFloorArea());
        } else if (value.contains("U")) { //????????????
            value = value.replace("U", roomDto.getUnitArea());
        } else if (value.contains("RL")) {
            List<RoomAttrDto> roomAttrDtos = roomDto.getRoomAttrDto();
            if (roomAttrDtos != null && roomAttrDtos.size() > 0) {
                for (RoomAttrDto roomAttrDto : roomAttrDtos) {
                    value = value.replace("RL" + roomAttrDto.getSpecCd(), roomAttrDto.getValue());
                }
            }
        } else if (value.contains("R")) { //?????? ????????????
            value = value.replace("R", roomDto.getBuiltUpArea());
        } else if (value.contains("X")) {// ?????? ????????????
            value = value.replace("X", roomDto.getFeeCoefficient());
        } else if (value.contains("L")) {//??????????????????
            value = value.replace("L", roomDto.getLayer());
        }

        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");
        BigDecimal valueObj = null;
        try {
            value = engine.eval(value).toString();
            valueObj = new BigDecimal(Double.parseDouble(value));
        } catch (Exception e) {
            //throw new IllegalArgumentException("?????????????????????????????????" + feeDto.getComputingFormulaText() + "???,?????? ???" + value + "?????????");
            valueObj = new BigDecimal(0);
        }

        if (valueObj.doubleValue() < 0) {
            return new BigDecimal(0);
        }

        return valueObj;

    }

    /**
     * ?????? ????????????????????? ???????????????????????????????????????
     *
     * @param feeDto
     * @param ownerCarDto
     * @return
     */
    public Map getTargetEndDateAndOweMonth(FeeDto feeDto, OwnerCarDto ownerCarDto) {
        Date targetEndDate = null;
        double oweMonth = 0.0;

        Map<String, Object> targetEndDateAndOweMonth = new HashMap<>();
        //?????????????????????????????????
        if (FeeDto.STATE_FINISH.equals(feeDto.getState())) {
            targetEndDate = feeDto.getEndTime();
            targetEndDateAndOweMonth.put("oweMonth", oweMonth);
            targetEndDateAndOweMonth.put("targetEndDate", targetEndDate);
            return targetEndDateAndOweMonth;
        }
        //??????????????????????????????
        Date maxEndTime = feeDto.getConfigEndTime();
        if (FeeDto.FEE_FLAG_ONCE.equals(feeDto.getFeeFlag())) {
            //?????? deadlineTime
            if (feeDto.getDeadlineTime() != null) {
                targetEndDate = feeDto.getDeadlineTime();
            } else if (!StringUtil.isEmpty(feeDto.getCurDegrees())) {
                targetEndDate = feeDto.getCurReadingTime();
            } else if (feeDto.getImportFeeEndTime() == null) {
                targetEndDate = maxEndTime;
            } else {
                targetEndDate = feeDto.getImportFeeEndTime();
            }
            //???????????????????????????????????????
            oweMonth = 1.0;
        } else if (FeeDto.FEE_FLAG_CYCLE_ONCE.equals(feeDto.getFeeFlag())) {
            if (feeDto.getDeadlineTime() != null) {
                maxEndTime = feeDto.getDeadlineTime();
            }
            Date billEndTime = DateUtil.getCurrentDate();
            //????????????
            Date startDate = feeDto.getStartTime();
            //??????????????????
            Date endDate = feeDto.getEndTime();
            //????????????
            long paymentCycle = Long.parseLong(feeDto.getPaymentCycle());
            // ???????????? - ????????????  = ??????
            double mulMonth = 0.0;
            mulMonth = dayCompare(startDate, billEndTime);

            // ??????/ ?????? = ????????????????????????
            double round = 0.0;
            if ("1200".equals(feeDto.getPaymentCd())) { // 1200?????????
                round = Math.floor(mulMonth / paymentCycle) + 1;
            } else { //2100?????????
                round = Math.floor(mulMonth / paymentCycle);
            }
            // ?????? * ?????? * 30 + ???????????? = ?????? ????????????
            targetEndDate = getTargetEndTime(round * paymentCycle, startDate);//??????????????????
            //????????????????????????<?????????????????????  ??????????????????   ???????????????????????????
            if (maxEndTime.getTime() < targetEndDate.getTime()) {
                targetEndDate = maxEndTime;
            }
            //????????????
            if (endDate.getTime() < targetEndDate.getTime()) {
                // ?????????????????? - ???????????? = ????????????
                oweMonth = dayCompare(endDate, targetEndDate);
            }

            if (feeDto.getEndTime().getTime() > targetEndDate.getTime()) {
                targetEndDate = feeDto.getEndTime();
            }
        } else { //???????????????
            //????????????
            Date billEndTime = DateUtil.getCurrentDate();
            //????????????
            Date startDate = feeDto.getStartTime();
            //??????????????????
            Date endDate = feeDto.getEndTime();
            //????????????
            long paymentCycle = Long.parseLong(feeDto.getPaymentCycle());
            // ???????????? - ????????????  = ??????
            double mulMonth = 0.0;
            mulMonth = dayCompare(startDate, billEndTime);

            // ??????/ ?????? = ????????????????????????
            double round = 0.0;
            if ("1200".equals(feeDto.getPaymentCd())) { // 1200?????????
                round = Math.floor(mulMonth / paymentCycle) + 1;
            } else { //2100?????????
                round = Math.floor(mulMonth / paymentCycle);
            }
            // ?????? * ?????? * 30 + ???????????? = ?????? ????????????
            targetEndDate = getTargetEndTime(round * paymentCycle, startDate);//??????????????????
            //????????????????????????<?????????????????????  ??????????????????   ???????????????????????????
            if (maxEndTime.getTime() < targetEndDate.getTime()) {
                targetEndDate = maxEndTime;
            }
            //????????????
            if (endDate.getTime() < targetEndDate.getTime()) {
                // ?????????????????? - ???????????? = ????????????
                oweMonth = dayCompare(endDate, targetEndDate);
            }

            if (feeDto.getEndTime().getTime() > targetEndDate.getTime()) {
                targetEndDate = feeDto.getEndTime();
            }
        }

        targetEndDateAndOweMonth.put("oweMonth", oweMonth);
        targetEndDateAndOweMonth.put("targetEndDate", targetEndDate);
        return targetEndDateAndOweMonth;
    }

    public Map getTargetEndDateAndOweMonth(FeeDto feeDto) {

//        if (FeeDto.PAYER_OBJ_TYPE_CAR.equals(feeDto.getPayerObjType())) {
//            OwnerCarDto ownerCarDto = new OwnerCarDto();
//            ownerCarDto.setCommunityId(feeDto.getCommunityId());
//            ownerCarDto.setCarId(feeDto.getPayerObjId());
//            List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);
//            return getTargetEndDateAndOweMonth(feeDto, ownerCarDtos == null || ownerCarDtos.size() < 1 ? null : ownerCarDtos.get(0));
//        }
        return getTargetEndDateAndOweMonth(feeDto, null);
    }

    /**
     * ?????? ?????????????????????
     *
     * @param fromDate ????????????
     * @param toDate   ????????????
     * @return
     */
    @Override
    public double dayCompare(Date fromDate, Date toDate) {
        double resMonth = 0.0;
        Calendar from = Calendar.getInstance();
        from.setTime(fromDate);
        Calendar to = Calendar.getInstance();
        to.setTime(toDate);
        //??????????????? ??????????????? ????????????
        int result = to.get(Calendar.MONTH) - from.get(Calendar.MONTH);
        //????????????
        int month = (to.get(Calendar.YEAR) - from.get(Calendar.YEAR)) * 12;

        //?????? ????????????
        result = result + month;

        //????????????  2021-06-01  2021-08-05   result = 2    2021-08-01
        Calendar newFrom = Calendar.getInstance();
        newFrom.setTime(fromDate);
        newFrom.add(Calendar.MONTH, result);
        //?????????????????? ????????????????????? ????????? ?????? -1 ?????? 12-19  21-01-10
        //???????????????????????????????????????
        if (newFrom.getTime().getTime() > toDate.getTime()) {
            newFrom.setTime(fromDate);
            result = result - 1;
            newFrom.add(Calendar.MONTH, result);
        }

        // t1 2021-08-01   t2 2021-08-05
        long t1 = newFrom.getTime().getTime();
        long t2 = to.getTime().getTime();
        //????????????
        double days = (t2 - t1) * 1.00 / (24 * 60 * 60 * 1000);
        BigDecimal tmpDays = new BigDecimal(days); //????????????
        BigDecimal monthDay = null;
        Calendar newFromMaxDay = Calendar.getInstance();
        newFromMaxDay.set(newFrom.get(Calendar.YEAR), newFrom.get(Calendar.MONTH), 1, 0, 0, 0);
        newFromMaxDay.add(Calendar.MONTH, 1); //?????????1???
        //??????????????? ???????????????
        if (toDate.getTime() < newFromMaxDay.getTime().getTime()) {
            monthDay = new BigDecimal(newFrom.getActualMaximum(Calendar.DAY_OF_MONTH));
            return tmpDays.divide(monthDay, 4, BigDecimal.ROUND_HALF_UP).add(new BigDecimal(result)).doubleValue();
        }
        // ????????????
        days = (newFromMaxDay.getTimeInMillis() - t1) * 1.00 / (24 * 60 * 60 * 1000);
        tmpDays = new BigDecimal(days);
        monthDay = new BigDecimal(newFrom.getActualMaximum(Calendar.DAY_OF_MONTH));
        BigDecimal preRresMonth = tmpDays.divide(monthDay, 4, BigDecimal.ROUND_HALF_UP);

        //????????????
        days = (t2 - newFromMaxDay.getTimeInMillis()) * 1.00 / (24 * 60 * 60 * 1000);
        tmpDays = new BigDecimal(days);
        monthDay = new BigDecimal(newFromMaxDay.getActualMaximum(Calendar.DAY_OF_MONTH));
        resMonth = tmpDays.divide(monthDay, 4, BigDecimal.ROUND_HALF_UP).add(new BigDecimal(result)).add(preRresMonth).doubleValue();
        return resMonth;
    }


    //?????????????????????
    public Map getOwnerPhoneFee(Map feeAmount) {
        feeAmount.put("feePrice", new BigDecimal(1.00 / 100));
        feeAmount.put("feeTotalPrice", new BigDecimal(1.00 / 100));
        return feeAmount;
    }


    /**
     * ?????? *?????????????????????????????????
     */
    public long daysBetween(Date smdate, Date bdate) {
        long between_days = 0;
        Calendar cal = Calendar.getInstance();
        cal.setTime(smdate);
        long time1 = cal.getTimeInMillis();
        cal.setTime(bdate);
        long time2 = cal.getTimeInMillis();
        between_days = (time2 - time1) / (1000 * 3600 * 24);

        return between_days;
    }

    @Override
    public Date getTargetEndTime(double month, Date startDate) {
        Calendar endDate = Calendar.getInstance();
        endDate.setTime(startDate);

        Double intMonth = Math.floor(month);
        endDate.add(Calendar.MONTH, intMonth.intValue());
        double doubleMonth = month - intMonth;
        if (doubleMonth <= 0) {
            return endDate.getTime();
        }
        int futureDay = endDate.getActualMaximum(Calendar.DAY_OF_MONTH);
        Double hour = doubleMonth * futureDay * 24;
        endDate.add(Calendar.HOUR_OF_DAY, hour.intValue());
        return endDate.getTime();
    }


    @Override
    public List<CarInoutDto> computeTempCarStopTimeAndFee(List<CarInoutDto> carInoutDtos) {

        if (carInoutDtos == null || carInoutDtos.size() < 1) {
            return null;
        }


        carInoutDtos = tempCarFeeConfigInnerServiceSMOImpl.computeTempCarFee(carInoutDtos);

        return carInoutDtos;

    }

    @Override
    public List<CarInoutDetailDto> computeTempCarInoutDetailStopTimeAndFee(List<CarInoutDetailDto> carInoutDtos) {
        if (carInoutDtos == null || carInoutDtos.size() < 1) {
            return null;
        }


        carInoutDtos = tempCarFeeConfigInnerServiceSMOImpl.computeTempCarInoutDetailFee(carInoutDtos);

        return carInoutDtos;
    }


//    public static void main(String[] args) {
//        BigDecimal squarePrice = new BigDecimal(Double.parseDouble("4.50"));
//        BigDecimal builtUpArea = new BigDecimal(Double.parseDouble("52.69"));
//        BigDecimal additionalAmount = new BigDecimal(Double.parseDouble("0"));
//            BigDecimal cycle = new BigDecimal(Double.parseDouble("3"));
//        BigDecimal  feeTotalPrice = (squarePrice.multiply(builtUpArea).add(additionalAmount)).multiply(cycle).setScale(3, BigDecimal.ROUND_HALF_DOWN);
//        System.out.println(feeTotalPrice.doubleValue());
//    }

    //    public static void main(String[] args) {
//        ComputeFeeSMOImpl computeFeeSMO = new ComputeFeeSMOImpl();
//        try {
//            Date startTime = DateUtil.getDateFromString("2020-12-31 00:00:00", DateUtil.DATE_FORMATE_STRING_A);
//            Date endTime = DateUtil.getDateFromString("2021-1-2 00:00:00", DateUtil.DATE_FORMATE_STRING_A);
//            double day = (endTime.getTime() - startTime.getTime()) * 1.00 / (24 * 60 * 60 * 1000);
//
//            System.out.println(day);
//
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
//    }
    static int[] getDiff(LocalDate start, LocalDate end) {

        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Start must not be before end.");
        }

        int year1 = start.getYear();
        int month1 = start.getMonthValue();
        int day1 = start.getDayOfMonth();

        int year2 = end.getYear();
        int month2 = end.getMonthValue();
        int day2 = end.getDayOfMonth();

        int yearDiff = year2 - year1;     // yearDiff >= 0
        int monthDiff = month2 - month1;

        int dayDiff = day2 - day1;

        if (dayDiff < 0) {
            LocalDate endMinusOneMonth = end.minusMonths(1);   // end ???????????????
            int monthDays = endMinusOneMonth.lengthOfMonth();  // ???????????????

            dayDiff += monthDays;  // ??????????????????????????????????????????????????????

            if (monthDiff > 0) {   // eg. start is 2018-04-03, end is 2018-08-01
                monthDiff--;

            } else {  // eg. start is 2018-04-03, end is 2019-02-01
                monthDiff += 11;
                yearDiff--;

            }
        }

        int[] diff = new int[2];

        diff[0] = yearDiff * 12 + monthDiff;
        diff[1] = dayDiff;

        return diff;
    }

    /**
     * ??????????????????????????????
     *
     * @param startDate
     * @param endDate
     * @return
     */
    public Map<String, Object> dateDiff(Date startDate, String endDate) {
        Map<String, Object> cycle = new HashMap<>();

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date endDates = null;
        try {
            endDates = format.parse(endDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        LocalDate start = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(endDates);
        calendar.add(calendar.DATE, 1);
        Date date = calendar.getTime();
        LocalDate end = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        int[] diff = getDiff(start, end);
        cycle.put("months", diff[0]);//?????????
        cycle.put("days", diff[1]);//??????
        String startDateString = format.format(startDate);
        String endDateString = format.format(endDates);
        cycle.put("startMonthDays", getDayOfMonth(startDateString));//??????????????????
        cycle.put("endMonthDays", getDayOfMonth(endDateString));//??????????????????
        cycle.put("isOneMonth", false);// false ????????? true??????
        if (diff[0] == 0) {
            //????????????????????????
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
            String date1 = sdf.format(startDate);
            String date2 = sdf.format(endDates);
            if (!date1.equals(date2)) {
                cycle.put("isOneMonth", true);
                //??????????????????????????????????????????
                SimpleDateFormat sdfday = new SimpleDateFormat("dd");
                Integer startDate1 = Integer.valueOf(sdfday.format(startDate));
                String endDates2 = sdfday.format(endDates);
                cycle.put("startEndOfMonth", getDayOfMonth(startDateString) - startDate1 + 1);//??????????????????
                cycle.put("endBeginningOfMonth", Integer.valueOf(endDates2));//??????????????????
            }
        }
        return cycle;

    }

    /**
     * ????????????????????????
     *
     * @param dateStr
     * @return
     */
    public int getDayOfMonth(String dateStr) {
        int year = Integer.parseInt(dateStr.substring(0, 4));
        int month = Integer.parseInt(dateStr.substring(5, 7));
        Calendar c = Calendar.getInstance();
        c.set(year, month, 0); //???????????????int??????
        return c.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * @param feeDto
     * @param cycle
     */
    public void dealRentRateCycle(FeeDto feeDto, double cycle) {

        if (!FeeConfigDto.COMPUTING_FORMULA_RANT_RATE.equals(feeDto.getComputingFormula())) {
            return;
        }

        Date endTime = feeDto.getEndTime();
        Date date = getTargetEndTime(cycle, endTime);
        feeDto.setDeadlineTime(date);
        dealRentRate(feeDto);

    }

    /**
     * @param feeDto
     * @param custEndTime
     */
    public void dealRentRateCustEndTime(FeeDto feeDto, Date custEndTime) {

        if (!FeeConfigDto.COMPUTING_FORMULA_RANT_RATE.equals(feeDto.getComputingFormula())) {
            return;
        }

        feeDto.setDeadlineTime(custEndTime);
        dealRentRate(feeDto);

    }

    @Override
    public long computeOneIntegralQuantity(IntegralRuleConfigDto integralRuleConfigDto, JSONObject reqJson) {
        String computingFormula = integralRuleConfigDto.getComputingFormula();
        BigDecimal amountDec = null;
        long amount = 0;
        if (IntegralRuleConfigDto.COMPUTING_FORMULA_AREA.equals(computingFormula)) { //??????????????????
            BigDecimal areaDec = new BigDecimal(Double.parseDouble(reqJson.getString("area")));
            BigDecimal squarePriceDec = new BigDecimal(Double.parseDouble(integralRuleConfigDto.getSquarePrice()));
            amountDec = areaDec.multiply(squarePriceDec).setScale(2, BigDecimal.ROUND_HALF_UP);
        } else if (IntegralRuleConfigDto.COMPUTING_FORMULA_MONEY.equals(computingFormula)) { // ??????????????????
            BigDecimal aDec = new BigDecimal(Double.parseDouble(reqJson.getString("amount")));
            BigDecimal squarePriceDec = new BigDecimal(Double.parseDouble(integralRuleConfigDto.getSquarePrice()));
            amountDec = aDec.multiply(squarePriceDec).setScale(2, BigDecimal.ROUND_HALF_UP);
        } else if (IntegralRuleConfigDto.COMPUTING_FORMULA_FIXED.equals(computingFormula)) { // ????????????
            amountDec = new BigDecimal(Double.parseDouble(integralRuleConfigDto.getAdditionalAmount()));
        } else {
            amountDec = new BigDecimal(0);
        }

        if (IntegralRuleConfigDto.SCALE_UP.equals(integralRuleConfigDto.getScale())) {
            amount = new Double(Math.ceil(amountDec.doubleValue())).longValue();
        } else {
            amount = new Double(Math.floor(amountDec.doubleValue())).longValue();
        }
        integralRuleConfigDto.setQuantity(amount + "");
        return amount;
    }

    /**
     * ????????????
     *
     * @param feeDto
     */
    public void dealRentRate(FeeDto feeDto) {
        if (!FeeConfigDto.COMPUTING_FORMULA_RANT_RATE.equals(feeDto.getComputingFormula())) {
            return;
        }

        //??????????????????
        FeeAttrDto feeAttrDto = new FeeAttrDto();
        feeAttrDto.setFeeId(feeDto.getFeeId());
        feeAttrDto.setCommunityId(feeDto.getCommunityId());
        List<FeeAttrDto> feeAttrDtos = feeAttrInnerServiceSMOImpl.queryFeeAttrs(feeAttrDto);

        if (feeAttrDtos == null || feeAttrDtos.size() < 1) {
            return;
        }
        int rateCycle = 0;
        double rate = 0.0;
        Date rateStartTime = null;
        try {
            for (FeeAttrDto tmpFeeAttrDto : feeAttrDtos) {
                if (FeeAttrDto.SPEC_CD_RATE.equals(tmpFeeAttrDto.getSpecCd())) {
                    feeDto.setRate(tmpFeeAttrDto.getValue().trim());
                    rate = Double.parseDouble(tmpFeeAttrDto.getValue().trim());
                }
                if (FeeAttrDto.SPEC_CD_RATE_CYCLE.equals(tmpFeeAttrDto.getSpecCd())) {
                    feeDto.setRateCycle(tmpFeeAttrDto.getValue().trim());
                    rateCycle = Integer.parseInt(tmpFeeAttrDto.getValue().trim());
                }
                if (FeeAttrDto.SPEC_CD_RATE_START_TIME.equals(tmpFeeAttrDto.getSpecCd())) {
                    feeDto.setRateStartTime(tmpFeeAttrDto.getValue().trim());
                    rateStartTime = DateUtil.getDateFromString(tmpFeeAttrDto.getValue().trim(), DateUtil.DATE_FORMATE_STRING_B);
                }
            }
        } catch (Exception e) {
            logger.error("??????????????????", e);
            return;
        }

        if (!FeeDto.STATE_DOING.equals(feeDto.getState())) {
            return;
        }

        if (rateCycle == 0 || rate == 0) {
            return;
        }

        if (feeDto.getDeadlineTime().getTime() <= rateStartTime.getTime()) {
            return;
        }

        BigDecimal oweAmountDec = new BigDecimal(0);
        //?????? ?????????????????? ??? rateStartTime ????????????
        double curOweMonth = 0;
        BigDecimal curFeePrice = new BigDecimal(feeDto.getFeePrice());
        if (feeDto.getEndTime().getTime() < rateStartTime.getTime()) {
            curOweMonth = dayCompare(feeDto.getEndTime(), rateStartTime);
            oweAmountDec = curFeePrice.multiply(new BigDecimal(curOweMonth)).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
        }

        curOweMonth = dayCompare(rateStartTime, feeDto.getDeadlineTime());

        double maxCycle = Math.ceil(curOweMonth / rateCycle);

        // ??????????????????
        BigDecimal addTotalAmount = oweAmountDec;
        BigDecimal preCycleAmount = curFeePrice.multiply(new BigDecimal(rateCycle));
        BigDecimal rateDec = null; //????????????????????????
        BigDecimal lastRateAmountDec = null;
        double curCycle = 0;
        BigDecimal curAmount = null; // ????????????
        Date curEndTime = null;
        for (int cycleIndex = 0; cycleIndex < maxCycle; cycleIndex++) {
            //??????????????????
            rateDec = preCycleAmount.multiply(new BigDecimal(rate)).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
            //?????????????????????
            curCycle = (cycleIndex + 1) * rateCycle;

            // ??????????????? ??????????????????
            Calendar curEndTimeCalender = Calendar.getInstance();
            curEndTimeCalender.setTime(rateStartTime);
            curEndTimeCalender.add(Calendar.MONTH, new Double(curCycle).intValue());
            curEndTime = curEndTimeCalender.getTime();
            if (curCycle > curOweMonth) {
                //???????????????????????????
                rateDec = new BigDecimal(curOweMonth / rateCycle - Math.floor(curOweMonth / rateCycle)).multiply(rateDec).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                lastRateAmountDec = new BigDecimal(curOweMonth / rateCycle - Math.floor(curOweMonth / rateCycle)).multiply(preCycleAmount).setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
                addTotalAmount = addTotalAmount.add(rateDec).add(lastRateAmountDec);
                continue;
            }
            //????????????
            curAmount = rateDec.add(preCycleAmount);// ???????????? + ???????????????
            //???????????????????????? ?????? ?????????????????? ???????????? ?????????
            if (feeDto.getEndTime().getTime() < curEndTime.getTime()) {
                addTotalAmount = addTotalAmount.add(curAmount); // ???????????? ?????????
            }
            preCycleAmount = curAmount;
        }
        feeDto.setAmountOwed(addTotalAmount.doubleValue() + "");
        feeDto.setFeeTotalPrice(addTotalAmount.doubleValue());
    }
}

