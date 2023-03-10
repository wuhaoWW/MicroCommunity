package com.java110.boot.smo.assetImport.impl;

import com.alibaba.fastjson.JSONObject;
import com.java110.boot.smo.DefaultAbstractComponentSMO;
import com.java110.boot.smo.assetImport.IImportOwnerCarSMO;
import com.java110.core.context.IPageData;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.FloorDto;
import com.java110.dto.RoomDto;
import com.java110.dto.UnitDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.dto.parking.ParkingAreaDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.entity.component.ComponentValidateResult;
import com.java110.intf.community.*;
import com.java110.intf.user.IOwnerCarV1InnerServiceSMO;
import com.java110.intf.user.IOwnerInnerServiceSMO;
import com.java110.intf.user.IOwnerRoomRelInnerServiceSMO;
import com.java110.po.car.OwnerCarPo;
import com.java110.po.parking.ParkingAreaPo;
import com.java110.po.parking.ParkingSpacePo;
import com.java110.utils.util.*;
import com.java110.vo.ResultVo;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Service("importOwnerCarSMOImpl")
public class ImportOwnerCarSMOImpl extends DefaultAbstractComponentSMO implements IImportOwnerCarSMO {

    private final static Logger logger = LoggerFactory.getLogger(ImportOwnerCarSMOImpl.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private IOwnerCarV1InnerServiceSMO ownerCarV1InnerServiceSMOImpl;

    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    @Autowired
    private IUnitInnerServiceSMO unitInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IOwnerRoomRelInnerServiceSMO ownerRoomRelInnerServiceSMOImpl;

    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    @Autowired
    private IParkingAreaInnerServiceSMO parkingAreaInnerServiceSMOImpl;

    @Autowired
    private IParkingAreaV1InnerServiceSMO parkingAreaV1InnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceV1InnerServiceSMO parkingSpaceV1InnerServiceSMOImpl;

    @Override
    public ResponseEntity<String> importExcelData(IPageData pd, MultipartFile uploadFile) throws Exception {
        try {
            ComponentValidateResult result = this.validateStoreStaffCommunityRelationship(pd, restTemplate);
            //InputStream is = uploadFile.getInputStream();
            Workbook workbook = ImportExcelUtils.createWorkbook(uploadFile);  //?????????
            List<OwnerCarDto> ownerCars = new ArrayList<OwnerCarDto>();
            //???????????????
            getOwnerCars(workbook, ownerCars);
            // ????????????
            return dealExcelData(pd, ownerCars, result);
        } catch (Exception e) {
            logger.error("???????????? ", e);
            return new ResponseEntity<String>("????????????????????????????????????????????????" + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * ????????????????????????
     *
     * @param workbook
     * @param ownerCarDtos
     */
    private void getOwnerCars(Workbook workbook, List<OwnerCarDto> ownerCarDtos) throws ParseException {
        Sheet sheet = null;
        sheet = ImportExcelUtils.getSheet(workbook, "??????????????????");
        List<Object[]> oList = ImportExcelUtils.listFromSheet(sheet);
        OwnerCarDto importOwnerCar = null;
        for (int osIndex = 0; osIndex < oList.size(); osIndex++) {
            Object[] os = oList.get(osIndex);
            if (osIndex == 0 || osIndex == 1) { // ???????????? ???????????? ????????????
                continue;
            }
            if (StringUtil.isNullOrNone(os[0])) {
                continue;
            }
            Assert.hasValue(os[0], (osIndex + 1) + "?????????????????????");
            Assert.hasValue(os[1], (osIndex + 1) + "?????????????????????");
            Assert.hasValue(os[2], (osIndex + 1) + "????????????????????????");
            Assert.hasValue(os[3], (osIndex + 1) + "????????????????????????");
            Assert.hasValue(os[4], (osIndex + 1) + "??????????????????");
            Assert.hasValue(os[5], (osIndex + 1) + "??????????????????");
            Assert.hasValue(os[6], (osIndex + 1) + "??????????????????");
            Assert.hasValue(os[7], (osIndex + 1) + "????????????????????????");
            Assert.hasValue(os[8], (osIndex + 1) + "????????????????????????");
            Assert.hasValue(os[9], (osIndex + 1) + "???????????????????????????");
            Assert.hasValue(os[10], (osIndex + 1) + "????????????????????????");
            String startTime = excelDoubleToDate(os[7].toString());
            String endTime = excelDoubleToDate(os[8].toString());
            Assert.isDate(startTime, DateUtil.DATE_FORMATE_STRING_A, (osIndex + 1) + "??????????????????????????? ?????????YYYY-MM-DD HH:mm:ss????????????");
            Assert.isDate(endTime, DateUtil.DATE_FORMATE_STRING_A, (osIndex + 1) + "??????????????????????????? ?????????YYYY-MM-DD HH:mm:ss????????????");
            importOwnerCar = new OwnerCarDto();
            importOwnerCar.setCarNum(os[0].toString());
            importOwnerCar.setRoomName(os[1].toString());
            importOwnerCar.setCarBrand(os[2].toString());
            importOwnerCar.setCarType(os[3].toString());
            importOwnerCar.setCarColor(os[4].toString());
            importOwnerCar.setOwnerName(os[5].toString());
            //????????????
            String parkingLot = os[6].toString();
            if(!parkingLot.contains("-")){
                throw new IllegalArgumentException((osIndex + 1) +"????????????????????? ????????????????????????-????????????????????????????????????1????????????");
            }
            String[] split = parkingLot.split("-",2);
            if(split.length != 2){
                throw new IllegalArgumentException((osIndex + 1) +"????????????????????? ????????????????????????-????????????????????????????????????1????????????");
            }
            importOwnerCar.setAreaNum(split[0]);
            importOwnerCar.setNum(split[1]);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            importOwnerCar.setStartTime(simpleDateFormat.parse(startTime));
            importOwnerCar.setEndTime(simpleDateFormat.parse(endTime));
            importOwnerCar.setTypeCd(os[9].toString());
            importOwnerCar.setSpaceSate(os[10].toString());
            ownerCarDtos.add(importOwnerCar);


        }
    }


    /**
     * ??????ExcelData??????
     */
    private ResponseEntity<String> dealExcelData(IPageData pd, List<OwnerCarDto> ownerCarDtos, ComponentValidateResult result) {
        ResponseEntity<String> responseEntity = null;
        //?????????????????? ??? ????????????
        responseEntity = savedOwnerCars(pd, ownerCarDtos, result);
        if (responseEntity == null || responseEntity.getStatusCode() != HttpStatus.OK) {
            return responseEntity;
        }
        return responseEntity;
    }

    private ResponseEntity<String> savedOwnerCars(IPageData pd, List<OwnerCarDto> ownerCars, ComponentValidateResult result) {
        JSONObject reqJson = JSONObject.parseObject(pd.getReqData());
        if (ownerCars.size() < 1) {
            throw new IllegalArgumentException("????????????????????????");
        }
        String psId = "";
        String paId = "";

        validateOwnerData(ownerCars, reqJson);
        for (OwnerCarDto ownerCarDto : ownerCars) {
            OwnerCarPo ownerCarPo = BeanConvertUtil.covertBean(ownerCarDto, OwnerCarPo.class);
            //??????????????????
            ownerCarPo.setCarTypeCd("1001"); //?????????
            ParkingAreaDto parkingAreaDto = new ParkingAreaDto();
            parkingAreaDto.setNum(ownerCarDto.getAreaNum());
            parkingAreaDto.setTypeCd(ownerCarDto.getTypeCd());
            //???????????????
            List<ParkingAreaDto> parkingAreaDtos = parkingAreaInnerServiceSMOImpl.queryParkingAreas(parkingAreaDto);
            //Assert.listOnlyOne(parkingAreaDtos, "????????????????????????");
            if (parkingAreaDtos == null || parkingAreaDtos.size() < 1) {
                paId = GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_paId);
                ParkingAreaPo parkingAreaPo = new ParkingAreaPo();
                parkingAreaPo.setCommunityId(reqJson.getString("communityId"));
                parkingAreaPo.setNum(ownerCarDto.getAreaNum());
                parkingAreaPo.setPaId(paId);
                parkingAreaPo.setTypeCd(ownerCarDto.getTypeCd());
                parkingAreaPo.setRemark("????????????");
                parkingAreaV1InnerServiceSMOImpl.saveParkingArea(parkingAreaPo);
            } else {
                paId = parkingAreaDtos.get(0).getPaId();
            }
            ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
            parkingSpaceDto.setNum(ownerCarDto.getNum());
            parkingSpaceDto.setPaId(paId);
            //???????????????
            List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);
            String state = "";
            if (parkingSpaceDtos == null || parkingSpaceDtos.size() < 1) {
                psId = GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_psId);
                ParkingSpacePo parkingSpacePo = new ParkingSpacePo();
                parkingSpacePo.setCommunityId(reqJson.getString("communityId"));
                parkingSpacePo.setNum(ownerCarDto.getNum());
                parkingSpacePo.setPaId(paId);
                parkingSpacePo.setArea("1");
                parkingSpacePo.setParkingType(ParkingSpaceDto.TYPE_CD_COMMON);
                parkingSpacePo.setState(ParkingSpaceDto.STATE_FREE);
                parkingSpacePo.setPsId(psId);
                parkingSpacePo.setRemark("????????????");
                parkingSpaceV1InnerServiceSMOImpl.saveParkingSpace(parkingSpacePo);
                state = ParkingSpaceDto.STATE_FREE;
            } else {
                psId = parkingSpaceDtos.get(0).getPsId();
                //?????????????????????(?????? S????????? H ????????? F)
                state = parkingSpaceDtos.get(0).getState();
            }

            if (!StringUtil.isEmpty(state) && !state.equals("F")) {
                throw new IllegalArgumentException(ownerCarDto.getAreaNum() + "?????????-" + ownerCarDto.getNum() + "??????????????????????????????");
            }
            ownerCarPo.setPsId(psId);
            ownerCarPo.setOwnerId(ownerCarDto.getOwnerId());
            ownerCarPo.setUserId("-1");
            ownerCarPo.setCommunityId(reqJson.getString("communityId"));
            ownerCarPo.setCarId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_carId));
            ownerCarPo.setMemberId(ownerCarPo.getCarId());
            ownerCarPo.setState("1001"); //1001 ???????????????2002 ???????????????????????????3003 ????????????
            ownerCarPo.setLeaseType(ownerCarDto.getSpaceSate());
            ownerCarV1InnerServiceSMOImpl.saveOwnerCar(ownerCarPo);
            ParkingSpacePo parkingSpacePo = new ParkingSpacePo();
            parkingSpacePo.setPsId(psId); //??????id
            parkingSpacePo.setState(ownerCarDto.getSpaceSate());
            parkingSpaceInnerServiceSMOImpl.updateParkingSpace(parkingSpacePo);
        }
        return ResultVo.success();
    }

    private void validateOwnerData(List<OwnerCarDto> ownerCars, JSONObject reqJson) {

        for (OwnerCarDto ownerCarDto : ownerCars) {

            if(!"1001".equals(ownerCarDto.getTypeCd()) && !"2002".equals(ownerCarDto.getTypeCd())){
                throw new IllegalArgumentException(ownerCarDto.getCarNum()+"???????????????????????? 1001(???????????????)?????? 2002 (???????????????)");
            }
            if(!"H".equals(ownerCarDto.getSpaceSate()) && !"S".equals(ownerCarDto.getSpaceSate())){
                throw new IllegalArgumentException(ownerCarDto.getCarNum()+"????????????????????? S?????????????????? H ????????????");
            }
            //??????????????????
            String roomName = ownerCarDto.getRoomName().trim();
            if(!roomName.contains("-")){
                throw new IllegalArgumentException(ownerCarDto.getCarNum()+"????????????????????? ?????????????????????-??????-???????????????????????? ??????-0-????????????");
            }
            String[] split = roomName.split("-", 3);
            if(split.length != 3){
                throw new IllegalArgumentException(ownerCarDto.getCarNum()+"????????????????????? ?????????????????????-??????-???????????????????????? ??????-0-????????????");
            }
            String floorNum = split[0];
            String unitNum = split[1];
            String roomNum = split[2];
            FloorDto floorDto = new FloorDto();
            floorDto.setCommunityId(reqJson.getString("communityId"));
            floorDto.setFloorNum(floorNum);
            //????????????
            List<FloorDto> floorDtos = floorInnerServiceSMOImpl.queryFloors(floorDto);
            Assert.listOnlyOne(floorDtos, ownerCarDto.getCarNum() + "?????????????????????");
            UnitDto unitDto = new UnitDto();
            unitDto.setUnitNum(unitNum);
            unitDto.setFloorId(floorDtos.get(0).getFloorId());
            //????????????
            List<UnitDto> unitDtos = unitInnerServiceSMOImpl.queryUnits(unitDto);
            Assert.listOnlyOne(unitDtos, ownerCarDto.getCarNum() + "?????????????????????");
            RoomDto roomDto = new RoomDto();
            roomDto.setRoomNum(roomNum);
            roomDto.setUnitId(unitDtos.get(0).getUnitId());
            //????????????
            List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);
            Assert.listOnlyOne(roomDtos, ownerCarDto.getCarNum() + "?????????????????????");
            OwnerRoomRelDto ownerRoomRelDto = new OwnerRoomRelDto();
            ownerRoomRelDto.setRoomId(roomDtos.get(0).getRoomId());
            //???????????????????????????
            List<OwnerRoomRelDto> ownerRoomRelDtos = ownerRoomRelInnerServiceSMOImpl.queryOwnerRoomRels(ownerRoomRelDto);
            Assert.listOnlyOne(ownerRoomRelDtos, ownerCarDto.getCarNum() + "?????????????????????????????????");
            OwnerDto ownerDto = new OwnerDto();
            ownerDto.setOwnerId(ownerRoomRelDtos.get(0).getOwnerId());
            ownerDto.setName(ownerCarDto.getOwnerName());
            //????????????
            List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryOwners(ownerDto);
            Assert.listOnlyOne(ownerDtos, ownerCarDto.getCarNum() + "???????????????????????????");
            ownerCarDto.setOwnerId(ownerRoomRelDtos.get(0).getOwnerId());
        }

    }

    //??????Excel????????????
    public static String excelDoubleToDate(String strDate) {
        if (strDate.length() == 5) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date tDate = DoubleToDate(Double.parseDouble(strDate));
                return sdf.format(tDate);
            } catch (Exception e) {
                e.printStackTrace();
                return strDate;
            }
        }
        return strDate;
    }

    //??????Excel????????????
    public static Date DoubleToDate(Double dVal) {
        Date tDate = new Date();
        long localOffset = tDate.getTimezoneOffset() * 60000; //?????????????????? 1900/1/1 ??? 1970/1/1 ??? 25569 ???
        tDate.setTime((long) ((dVal - 25569) * 24 * 3600 * 1000 + localOffset));
        return tDate;
    }
}
