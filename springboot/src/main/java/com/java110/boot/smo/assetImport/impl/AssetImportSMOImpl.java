package com.java110.boot.smo.assetImport.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.boot.smo.DefaultAbstractComponentSMO;
import com.java110.boot.smo.assetImport.IAssetImportSMO;
import com.java110.core.context.IPageData;
import com.java110.core.log.LoggerFactory;
import com.java110.core.smo.ISaveTransactionLogSMO;
import com.java110.dto.RoomDto;
import com.java110.dto.assetImportLog.AssetImportLogDto;
import com.java110.dto.assetImportLogDetail.AssetImportLogDetailDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.entity.assetImport.*;
import com.java110.entity.component.ComponentValidateResult;
import com.java110.utils.util.*;
import com.java110.vo.ResultVo;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @ClassName AssetImportSmoImpl
 * @Description TODO
 * @Author wuxw
 * @Date 2019/9/23 23:14
 * @Version 1.0
 * add by wuxw 2019/9/23
 **/
@Service("assetImportSMOImpl")
public class AssetImportSMOImpl extends DefaultAbstractComponentSMO implements IAssetImportSMO {
    private final static Logger logger = LoggerFactory.getLogger(AssetImportSMOImpl.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ISaveTransactionLogSMO saveTransactionLogSMOImpl;

    @Override
    public ResponseEntity<String> importExcelData(IPageData pd, MultipartFile uploadFile) throws Exception {

        try {
            ComponentValidateResult result = this.validateStoreStaffCommunityRelationship(pd, restTemplate);

            //InputStream is = uploadFile.getInputStream();

            Workbook workbook = null;  //?????????
            //?????????
            String[] headers = null;   //????????????
            List<ImportFloor> floors = new ArrayList<ImportFloor>();
            List<ImportOwner> owners = new ArrayList<ImportOwner>();
            List<ImportFee> fees = new ArrayList<>();
            List<ImportRoom> rooms = new ArrayList<ImportRoom>();
            List<ImportParkingSpace> parkingSpaces = new ArrayList<ImportParkingSpace>();
            workbook = ImportExcelUtils.createWorkbook(uploadFile);
            //???????????????
            getFloors(workbook, floors);
            //??????????????????
            getOwners(workbook, owners);


            getFee(workbook, fees);

            //??????????????????
            getRooms(workbook, rooms, floors, owners);

            //??????????????????
            getParkingSpaces(workbook, parkingSpaces, owners);

            //????????????
            importExcelDataValidate(floors, owners, rooms, fees, parkingSpaces);

            // ????????????
            return dealExcelData(pd, floors, owners, rooms, parkingSpaces, fees, result);
        } catch (Exception e) {
            logger.error("???????????? ", e);
            return new ResponseEntity<String>("????????????????????????????????????????????????" + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * ??????ExcelData??????
     *
     * @param floors        ??????????????????
     * @param owners        ????????????
     * @param rooms         ????????????
     * @param parkingSpaces ????????????
     */
    private ResponseEntity<String> dealExcelData(IPageData pd,
                                                 List<ImportFloor> floors,
                                                 List<ImportOwner> owners,
                                                 List<ImportRoom> rooms,
                                                 List<ImportParkingSpace> parkingSpaces,
                                                 List<ImportFee> fees,
                                                 ComponentValidateResult result) {
        ResponseEntity<String> responseEntity = null;
        //?????????????????? ??? ????????????
        responseEntity = savedFloorAndUnitInfo(pd, floors, result);

        if (responseEntity == null || responseEntity.getStatusCode() != HttpStatus.OK) {
            return responseEntity;
        }

        // ??????????????????
        responseEntity = savedOwnerInfo(pd, owners, result);
        if (responseEntity == null || responseEntity.getStatusCode() != HttpStatus.OK) {
            return responseEntity;
        }

        // ???????????????
        responseEntity = savedFeeInfo(pd, fees, result);
        if (responseEntity == null || responseEntity.getStatusCode() != HttpStatus.OK) {
            return responseEntity;
        }

        //????????????
        responseEntity = savedRoomInfo(pd, rooms, fees, result);
        if (responseEntity == null || responseEntity.getStatusCode() != HttpStatus.OK) {
            return responseEntity;
        }

        //????????????
        responseEntity = savedParkingSpaceInfo(pd, parkingSpaces, result);

        if (responseEntity == null || responseEntity.getStatusCode() != HttpStatus.OK) {
            return responseEntity;
        }

        return responseEntity;
    }

    private ResponseEntity<String> savedFeeInfo(IPageData pd, List<ImportFee> fees, ComponentValidateResult result) {
        String apiUrl = "";
        JSONObject paramIn = null;
        ResponseEntity<String> responseEntity = new ResponseEntity<String>("??????", HttpStatus.OK);
        ImportOwner owner = null;

        AssetImportLogDto assetImportLogDto = new AssetImportLogDto();
        assetImportLogDto.setSuccessCount(0L);
        assetImportLogDto.setErrorCount(0L);
        assetImportLogDto.setCommunityId(result.getCommunityId());
        assetImportLogDto.setLogType(AssetImportLogDto.LOG_TYPE_FEE_IMPORT);
        List<AssetImportLogDetailDto> assetImportLogDetailDtos = new ArrayList<>();
        assetImportLogDto.setAssetImportLogDetailDtos(assetImportLogDetailDtos);
        long successCount = 0L;
        long failCount = 0L;
        AssetImportLogDetailDto assetImportLogDetailDto = null;
        try {
            for (ImportFee fee : fees) {
                JSONObject savedFeeConfigInfo = getExistsFee(pd, result, fee);
                if (savedFeeConfigInfo != null) {
                    successCount += 1;
                    assetImportLogDto.setSuccessCount(successCount);
                    assetImportLogDto.setErrorCount(failCount);
                    continue;
                }
                //paramIn = new JSONObject();
                //?????? ?????????

                apiUrl = "feeConfig.saveFeeConfig";

                paramIn = JSONObject.parseObject(JSONObject.toJSONString(fee));
                paramIn.put("communityId", result.getCommunityId());

                responseEntity = this.callCenterService(restTemplate, pd, paramIn.toJSONString(), apiUrl, HttpMethod.POST);
                if (responseEntity.getStatusCode() != HttpStatus.OK) {
                    /***************************************??????????????????****************************************************/
                    failCount += 1;
                    assetImportLogDetailDto = new AssetImportLogDetailDto();
                    assetImportLogDetailDto.setCommunityId(assetImportLogDto.getCommunityId());
                    if (Assert.isJsonObject(responseEntity.getBody())) {
                        JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
                        assetImportLogDetailDto.setState(paramOut.getString("code"));
                        assetImportLogDetailDto.setMessage(paramOut.getString("msg"));
                    } else {
                        assetImportLogDetailDto.setState("F");
                        assetImportLogDetailDto.setMessage(responseEntity.getBody());
                    }
                    assetImportLogDetailDto.setObjName(fee.getFeeName());
                    assetImportLogDetailDtos.add(assetImportLogDetailDto);
                } else {
                    JSONObject body = JSONObject.parseObject(responseEntity.getBody());
                    if (body.containsKey("code") && body.getIntValue("code") != 0) {
                        assetImportLogDetailDto = new AssetImportLogDetailDto();
                        assetImportLogDetailDto.setCommunityId(assetImportLogDto.getCommunityId());
                        assetImportLogDetailDto.setState(body.getString("code"));
                        assetImportLogDetailDto.setMessage(body.getString("msg"));
                        assetImportLogDetailDto.setObjName(fee.getFeeName());
                        assetImportLogDetailDtos.add(assetImportLogDetailDto);
                        failCount += 1;
                    } else {
                        successCount += 1;
                    }
                }
                assetImportLogDto.setSuccessCount(successCount);
                assetImportLogDto.setErrorCount(failCount);
            }
        } finally {
            saveTransactionLogSMOImpl.saveAssetImportLog(assetImportLogDto);
        }

        return responseEntity;
    }

    /**
     * ??????????????????
     *
     * @param pd
     * @param parkingSpaces
     * @param result
     * @return
     */
    private ResponseEntity<String> savedParkingSpaceInfo(IPageData pd, List<ImportParkingSpace> parkingSpaces, ComponentValidateResult result) {
        String apiUrl = "";
        JSONObject paramIn = null;
        ResponseEntity<String> responseEntity = new ResponseEntity<String>("??????", HttpStatus.OK);
        ImportOwner owner = null;
        AssetImportLogDto assetImportLogDto = new AssetImportLogDto();
        assetImportLogDto.setSuccessCount(0L);
        assetImportLogDto.setErrorCount(0L);
        assetImportLogDto.setCommunityId(result.getCommunityId());
        assetImportLogDto.setLogType(AssetImportLogDto.LOG_TYPE_AREA_PARKING_IMPORT);
        List<AssetImportLogDetailDto> assetImportLogDetailDtos = new ArrayList<>();
        assetImportLogDto.setAssetImportLogDetailDtos(assetImportLogDetailDtos);
        long successCount = 0L;
        long failCount = 0L;
        AssetImportLogDetailDto assetImportLogDetailDto = null;
        try {
            for (ImportParkingSpace parkingSpace : parkingSpaces) {
                responseEntity = ResultVo.success();
                JSONObject savedParkingAreaInfo = getExistsParkingArea(pd, result, parkingSpace);
                paramIn = new JSONObject();
                // ???????????????????????????
                if (savedParkingAreaInfo == null) {
                    apiUrl = "parkingArea.saveParkingArea";
                    paramIn.put("communityId", result.getCommunityId());
                    paramIn.put("typeCd", parkingSpace.getTypeCd());
                    paramIn.put("num", parkingSpace.getPaNum());

                    responseEntity = this.callCenterService(restTemplate, pd, paramIn.toJSONString(), apiUrl, HttpMethod.POST);
                    savedParkingAreaInfo = getExistsParkingArea(pd, result, parkingSpace);
                }
                if (responseEntity != null && responseEntity.getStatusCode() != HttpStatus.OK) { //?????? ??????????????????
                    failCount += 1;
                    assetImportLogDetailDto = new AssetImportLogDetailDto();
                    assetImportLogDetailDto.setCommunityId(assetImportLogDto.getCommunityId());
                    if (Assert.isJsonObject(responseEntity.getBody())) {
                        JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
                        assetImportLogDetailDto.setState(paramOut.getString("code"));
                        assetImportLogDetailDto.setMessage(paramOut.getString("msg"));
                    } else {
                        assetImportLogDetailDto.setState("F");
                        assetImportLogDetailDto.setMessage(responseEntity.getBody());
                    }
                    assetImportLogDetailDto.setObjName(parkingSpace.getPaNum());
                    assetImportLogDetailDtos.add(assetImportLogDetailDto);
                    assetImportLogDto.setSuccessCount(successCount);
                    assetImportLogDto.setErrorCount(failCount);
                    continue;
                } else {
                    JSONObject body = JSONObject.parseObject(responseEntity.getBody());
                    if (body.containsKey("code") && body.getIntValue("code") != 0) {
                        assetImportLogDetailDto = new AssetImportLogDetailDto();
                        assetImportLogDetailDto.setCommunityId(assetImportLogDto.getCommunityId());
                        assetImportLogDetailDto.setState(body.getString("code"));
                        assetImportLogDetailDto.setMessage(body.getString("msg"));
                        assetImportLogDetailDto.setObjName(parkingSpace.getPaNum());
                        assetImportLogDetailDtos.add(assetImportLogDetailDto);
                        failCount += 1;
                        assetImportLogDto.setSuccessCount(successCount);
                        assetImportLogDto.setErrorCount(failCount);
                        continue;
                    } else {
                        successCount += 1;
                    }
                    assetImportLogDto.setSuccessCount(successCount);
                    assetImportLogDto.setErrorCount(failCount);
                }

                JSONObject savedParkingSpaceInfo = getExistsParkSpace(pd, result, parkingSpace);
                if (savedParkingSpaceInfo != null) {
                    continue;
                }

                apiUrl = "parkingSpace.saveParkingSpace";

                paramIn.put("paId", savedParkingAreaInfo.getString("paId"));
                paramIn.put("communityId", result.getCommunityId());
                paramIn.put("userId", result.getUserId());
                paramIn.put("num", parkingSpace.getPsNum());
                paramIn.put("area", parkingSpace.getArea());
                paramIn.put("typeCd", parkingSpace.getTypeCd());
                paramIn.put("parkingType", "1");

                responseEntity = this.callCenterService(restTemplate, pd, paramIn.toJSONString(), apiUrl, HttpMethod.POST);
                if (responseEntity.getStatusCode() != HttpStatus.OK) {
                    assetImportLogDetailDto = new AssetImportLogDetailDto();
                    assetImportLogDetailDto.setCommunityId(assetImportLogDto.getCommunityId());
                    if (Assert.isJsonObject(responseEntity.getBody())) {
                        JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
                        assetImportLogDetailDto.setState(paramOut.getString("code"));
                        assetImportLogDetailDto.setMessage(paramOut.getString("msg"));
                    } else {
                        assetImportLogDetailDto.setState("F");
                        assetImportLogDetailDto.setMessage(responseEntity.getBody());
                    }
                    assetImportLogDetailDto.setObjName(parkingSpace.getPaNum());
                    assetImportLogDetailDtos.add(assetImportLogDetailDto);
                    failCount += 1;
                    successCount = successCount > 0 ? successCount - 1 : successCount;
                    assetImportLogDto.setSuccessCount(successCount);
                    assetImportLogDto.setErrorCount(failCount);
                    continue;
                } else {
                    JSONObject body = JSONObject.parseObject(responseEntity.getBody());
                    if (body.containsKey("code") && body.getIntValue("code") != 0) {
                        assetImportLogDetailDto = new AssetImportLogDetailDto();
                        assetImportLogDetailDto.setCommunityId(assetImportLogDto.getCommunityId());
                        assetImportLogDetailDto.setState(body.getString("code"));
                        assetImportLogDetailDto.setMessage(body.getString("msg"));
                        assetImportLogDetailDto.setObjName(parkingSpace.getPaNum());
                        assetImportLogDetailDtos.add(assetImportLogDetailDto);
                        assetImportLogDto.setSuccessCount(successCount);
                        assetImportLogDto.setErrorCount(failCount);
                        failCount += 1;
                        successCount = successCount > 0 ? successCount - 1 : successCount;
                        assetImportLogDto.setSuccessCount(successCount);
                        assetImportLogDto.setErrorCount(failCount);
                        continue;
                    }
                }

                savedParkingSpaceInfo = getExistsParkSpace(pd, result, parkingSpace);
                if (savedParkingSpaceInfo == null) {
                    continue;
                }

                //?????????????????????
                if (parkingSpace.getImportOwner() == null) {
                    continue;
                }

                paramIn.clear();

                paramIn.put("communityId", result.getCommunityId());
                paramIn.put("ownerId", parkingSpace.getImportOwner().getOwnerId());
                paramIn.put("userId", result.getUserId());
                paramIn.put("carNum", parkingSpace.getCarNum());
                paramIn.put("carBrand", parkingSpace.getCarBrand());
                paramIn.put("carType", parkingSpace.getCarType());
                paramIn.put("carColor", parkingSpace.getCarColor());
                paramIn.put("psId", savedParkingSpaceInfo.getString("psId"));
                paramIn.put("storeId", result.getStoreId());
                paramIn.put("sellOrHire", parkingSpace.getSellOrHire());
                paramIn.put("startTime", parkingSpace.getStartTime());
                paramIn.put("endTime", parkingSpace.getEndTime());

                if ("H".equals(parkingSpace.getSellOrHire())) {
                    paramIn.put("cycles", "0");
                }

                apiUrl = "parkingSpace.sellParkingSpace";
                responseEntity = this.callCenterService(restTemplate, pd, paramIn.toJSONString(), apiUrl, HttpMethod.POST);

                if (responseEntity.getStatusCode() != HttpStatus.OK) {
                    /***************************************??????????????????****************************************************/
                    assetImportLogDetailDto = new AssetImportLogDetailDto();
                    assetImportLogDetailDto.setCommunityId(assetImportLogDto.getCommunityId());
                    if (Assert.isJsonObject(responseEntity.getBody())) {
                        JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
                        assetImportLogDetailDto.setState(paramOut.getString("code"));
                        assetImportLogDetailDto.setMessage(paramOut.getString("msg"));
                    } else {
                        assetImportLogDetailDto.setState("F");
                        assetImportLogDetailDto.setMessage(responseEntity.getBody());
                    }
                    assetImportLogDetailDto.setObjName(parkingSpace.getCarNum());
                    assetImportLogDetailDtos.add(assetImportLogDetailDto);
                    failCount += 1;
                    successCount = successCount > 0 ? successCount - 1 : successCount;
                    assetImportLogDto.setSuccessCount(successCount);
                    assetImportLogDto.setErrorCount(failCount);
                } else {
                    JSONObject body = JSONObject.parseObject(responseEntity.getBody());
                    if (body.containsKey("code") && body.getIntValue("code") != 0) {
                        assetImportLogDetailDto = new AssetImportLogDetailDto();
                        assetImportLogDetailDto.setCommunityId(assetImportLogDto.getCommunityId());
                        assetImportLogDetailDto.setState(body.getString("code"));
                        assetImportLogDetailDto.setMessage(body.getString("msg"));
                        assetImportLogDetailDto.setObjName(parkingSpace.getCarNum());
                        assetImportLogDetailDtos.add(assetImportLogDetailDto);
                        failCount += 1;
                        successCount = successCount > 0 ? successCount - 1 : successCount;
                        assetImportLogDto.setSuccessCount(successCount);
                        assetImportLogDto.setErrorCount(failCount);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("??????????????????", e);
            saveTransactionLogSMOImpl.saveAssetImportLog(assetImportLogDto);
            throw e;
        }

        return responseEntity;
    }


    /**
     * ??????????????????
     *
     * @param pd
     * @param rooms
     * @param result
     * @return
     */
    private ResponseEntity<String> savedRoomInfo(IPageData pd, List<ImportRoom> rooms, List<ImportFee> fees, ComponentValidateResult result) {
        String apiUrl = "";
        JSONObject paramIn = null;
        ResponseEntity<String> responseEntity = new ResponseEntity<String>("??????", HttpStatus.OK);
        ImportOwner owner = null;
        AssetImportLogDto assetImportLogDto = new AssetImportLogDto();
        assetImportLogDto.setSuccessCount(0L);
        assetImportLogDto.setErrorCount(0L);
        assetImportLogDto.setCommunityId(result.getCommunityId());
        assetImportLogDto.setLogType(AssetImportLogDto.LOG_TYPE_ROOM_IMPORT);
        List<AssetImportLogDetailDto> assetImportLogDetailDtos = new ArrayList<>();
        assetImportLogDto.setAssetImportLogDetailDtos(assetImportLogDetailDtos);
        long successCount = 0L;
        long failCount = 0L;
        AssetImportLogDetailDto assetImportLogDetailDto = null;
        try {
            for (ImportRoom room : rooms) {
                paramIn = new JSONObject();
                JSONObject savedRoomInfo = getExistsRoom(pd, result, room);
                if (savedRoomInfo != null) {
                    //????????????????????????
                    if (RoomDto.STATE_FREE.equals(savedRoomInfo.getString("state")) && room.getImportOwner() != null) {
                        paramIn.clear();
                        apiUrl = "room.sellRoom";
                        paramIn.put("communityId", result.getCommunityId());
                        paramIn.put("ownerId", room.getImportOwner().getOwnerId());
                        paramIn.put("roomId", savedRoomInfo.getString("roomId"));
                        paramIn.put("state", "2001");
                        paramIn.put("storeId", result.getStoreId());
                        if (!StringUtil.isEmpty(room.getRoomFeeId()) && "0".equals(room.getRoomFeeId())) {
                            paramIn.put("feeEndDate", room.getFeeEndDate());
                        }
                        responseEntity = this.callCenterService(restTemplate, pd, paramIn.toJSONString(), apiUrl, HttpMethod.POST);
                    }
                    continue;
                }
                //?????? ??????
                apiUrl = "room.saveRoom";

                paramIn.put("communityId", result.getCommunityId());
                paramIn.put("unitId", room.getFloor().getUnitId());
                paramIn.put("roomNum", room.getRoomNum());
                paramIn.put("layer", room.getLayer());
                paramIn.put("section", "1");
                paramIn.put("apartment", room.getSection());
                paramIn.put("state", "2002");
                paramIn.put("builtUpArea", room.getBuiltUpArea());
                paramIn.put("feeCoefficient", "1.00");
                paramIn.put("roomSubType", room.getRoomSubType());
                paramIn.put("roomArea", room.getRoomArea());
                paramIn.put("roomRent", room.getRoomRent());
                paramIn.put("roomType", "0".equals(room.getFloor().getUnitNum()) ? RoomDto.ROOM_TYPE_SHOPS : RoomDto.ROOM_TYPE_SHOPS);


                responseEntity = this.callCenterService(restTemplate, pd, paramIn.toJSONString(), apiUrl, HttpMethod.POST);
                if (responseEntity.getStatusCode() != HttpStatus.OK) {
                    failCount += 1;
                    assetImportLogDetailDto = new AssetImportLogDetailDto();
                    assetImportLogDetailDto.setCommunityId(assetImportLogDto.getCommunityId());
                    if (Assert.isJsonObject(responseEntity.getBody())) {
                        JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
                        assetImportLogDetailDto.setState(paramOut.getString("code"));
                        assetImportLogDetailDto.setMessage(paramOut.getString("msg"));
                    } else {
                        assetImportLogDetailDto.setState("F");
                        assetImportLogDetailDto.setMessage(responseEntity.getBody());
                    }
                    assetImportLogDetailDto.setObjName(room.getRoomNum() + "???");
                    assetImportLogDetailDtos.add(assetImportLogDetailDto);
                    assetImportLogDto.setSuccessCount(successCount);
                    assetImportLogDto.setErrorCount(failCount);
                    continue;
                } else {
                    JSONObject body = JSONObject.parseObject(responseEntity.getBody());
                    if (body.containsKey("code") && body.getIntValue("code") != 0) {
                        assetImportLogDetailDto = new AssetImportLogDetailDto();
                        assetImportLogDetailDto.setCommunityId(assetImportLogDto.getCommunityId());
                        assetImportLogDetailDto.setState(body.getString("code"));
                        assetImportLogDetailDto.setMessage(body.getString("msg"));
                        assetImportLogDetailDto.setObjName(room.getRoomNum() + "???");
                        assetImportLogDetailDtos.add(assetImportLogDetailDto);
                        failCount += 1;
                        assetImportLogDto.setSuccessCount(successCount);
                        assetImportLogDto.setErrorCount(failCount);
                        continue;
                    } else {
                        successCount += 1;
                        assetImportLogDto.setSuccessCount(successCount);
                        assetImportLogDto.setErrorCount(failCount);
                    }
                }

                savedRoomInfo = getExistsRoom(pd, result, room);
                if (savedRoomInfo == null) {
                    continue;
                }
                if (room.getImportOwner() == null) {
                    continue;
                }
                paramIn.clear();
                apiUrl = "room.sellRoom";
                paramIn.put("communityId", result.getCommunityId());
                paramIn.put("ownerId", room.getImportOwner().getOwnerId());
                paramIn.put("roomId", savedRoomInfo.getString("roomId"));
                paramIn.put("state", "2001");
                paramIn.put("storeId", result.getStoreId());
                if (!StringUtil.isEmpty(room.getRoomFeeId()) && "0".equals(room.getRoomFeeId())) {
                    paramIn.put("feeEndDate", room.getFeeEndDate());
                }
                responseEntity = this.callCenterService(restTemplate, pd, paramIn.toJSONString(), apiUrl, HttpMethod.POST);
                if (responseEntity.getStatusCode() != HttpStatus.OK) {
                    assetImportLogDetailDto = new AssetImportLogDetailDto();
                    assetImportLogDetailDto.setCommunityId(assetImportLogDto.getCommunityId());
                    if (Assert.isJsonObject(responseEntity.getBody())) {
                        JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
                        assetImportLogDetailDto.setState(paramOut.getString("code"));
                        assetImportLogDetailDto.setMessage(paramOut.getString("msg"));
                    } else {
                        assetImportLogDetailDto.setState("F");
                        assetImportLogDetailDto.setMessage(responseEntity.getBody());
                    }
                    assetImportLogDetailDto.setObjName(room.getRoomNum() + "???");
                    assetImportLogDetailDtos.add(assetImportLogDetailDto);
                    continue;
                } else {
                    JSONObject body = JSONObject.parseObject(responseEntity.getBody());
                    if (body.containsKey("code") && body.getIntValue("code") != 0) {
                        assetImportLogDetailDto = new AssetImportLogDetailDto();
                        assetImportLogDetailDto.setCommunityId(assetImportLogDto.getCommunityId());
                        assetImportLogDetailDto.setState(body.getString("code"));
                        assetImportLogDetailDto.setMessage(body.getString("msg"));
                        assetImportLogDetailDto.setObjName(room.getRoomNum() + "???");
                        assetImportLogDetailDtos.add(assetImportLogDetailDto);
                        continue;
                    }
                }
                //????????????
                if (StringUtil.isEmpty(room.getRoomFeeId()) || "0".equals(room.getRoomFeeId())) {
                    continue;
                }
                String[] feeIds = room.getRoomFeeId().split("#");

                for (int feeIndex = 0; feeIndex < feeIds.length; feeIndex++) {
                    String feeId = feeIds[feeIndex];
                    ImportFee tmpFee = null;
                    for (ImportFee fee : fees) {
                        if (feeId.equals(fee.getId())) {
                            tmpFee = fee;
                        }
                    }

                    if (tmpFee == null) {
                        continue;//?????????????????????????????????
                    }

                    JSONObject ttFee = getExistsFee(pd, result, tmpFee);

                    if (ttFee == null) {
                        continue;//?????????????????????????????????
                    }

                    apiUrl = "fee.saveRoomCreateFee";
                    paramIn.put("communityId", result.getCommunityId());
                    paramIn.put("locationTypeCd", "3000");
                    paramIn.put("locationObjId", savedRoomInfo.getString("roomId"));
                    paramIn.put("configId", ttFee.getString("configId"));
                    paramIn.put("storeId", result.getStoreId());
                    paramIn.put("feeEndDate", room.getFeeEndDate().split("#")[feeIndex]);
                    paramIn.put("startTime", paramIn.getString("feeEndDate"));

                    responseEntity = this.callCenterService(restTemplate, pd, paramIn.toJSONString(), apiUrl, HttpMethod.POST);

                    if (responseEntity.getStatusCode() != HttpStatus.OK) {
                        /***************************************??????????????????****************************************************/
                        assetImportLogDetailDto = new AssetImportLogDetailDto();
                        assetImportLogDetailDto.setCommunityId(assetImportLogDto.getCommunityId());
                        if (Assert.isJsonObject(responseEntity.getBody())) {
                            JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
                            assetImportLogDetailDto.setState(paramOut.getString("code"));
                            assetImportLogDetailDto.setMessage(paramOut.getString("msg"));
                        } else {
                            assetImportLogDetailDto.setState("F");
                            assetImportLogDetailDto.setMessage(responseEntity.getBody());
                        }
                        assetImportLogDetailDto.setObjName(room.getRoomNum() + "???");
                        assetImportLogDetailDtos.add(assetImportLogDetailDto);
                        failCount += 1;
                        successCount = successCount > 0 ? successCount - 1 : successCount;
                        assetImportLogDto.setSuccessCount(successCount);
                        assetImportLogDto.setErrorCount(failCount);
                    } else {
                        JSONObject body = JSONObject.parseObject(responseEntity.getBody());
                        if (body.containsKey("code") && body.getIntValue("code") != 0) {
                            assetImportLogDetailDto = new AssetImportLogDetailDto();
                            assetImportLogDetailDto.setCommunityId(assetImportLogDto.getCommunityId());
                            assetImportLogDetailDto.setState(body.getString("code"));
                            assetImportLogDetailDto.setMessage(body.getString("msg"));
                            assetImportLogDetailDto.setObjName(room.getRoomNum() + "???");
                            assetImportLogDetailDtos.add(assetImportLogDetailDto);
                            failCount += 1;
                            successCount = successCount > 0 ? successCount - 1 : successCount;
                            assetImportLogDto.setSuccessCount(successCount);
                            assetImportLogDto.setErrorCount(failCount);
                        }
                    }
                }

            }
        } finally {
            saveTransactionLogSMOImpl.saveAssetImportLog(assetImportLogDto);
        }

        return responseEntity;
    }

    /**
     * ???????????????????????????
     * room.queryRooms
     *
     * @param pd
     * @param result
     * @param parkingSpace
     * @return
     */
    private JSONObject getExistsParkSpace(IPageData pd, ComponentValidateResult result, ImportParkingSpace parkingSpace) {
        String apiUrl = "";
        ResponseEntity<String> responseEntity = null;
        apiUrl = "parkingSpace.queryParkingSpaces?page=1&row=1&communityId=" + result.getCommunityId()
                + "&num=" + parkingSpace.getPsNum() + "&areaNum=" + parkingSpace.getPaNum();
        responseEntity = this.callCenterService(restTemplate, pd, "", apiUrl, HttpMethod.GET);

        if (responseEntity.getStatusCode() != HttpStatus.OK) { //?????? ??????????????????
            return null;
        }

        JSONObject savedParkingSpaceInfoResults = JSONObject.parseObject(responseEntity.getBody());


        if (!savedParkingSpaceInfoResults.containsKey("parkingSpaces") || savedParkingSpaceInfoResults.getJSONArray("parkingSpaces").size() != 1) {
            return null;
        }


        JSONObject savedParkingSpaceInfo = savedParkingSpaceInfoResults.getJSONArray("parkingSpaces").getJSONObject(0);

        return savedParkingSpaceInfo;
    }

    /**
     * ???????????????????????????
     * room.queryRooms
     *
     * @param pd
     * @param result
     * @param fee
     * @return
     */
    private JSONObject getExistsFee(IPageData pd, ComponentValidateResult result, ImportFee fee) {
        String apiUrl = "";
        ResponseEntity<String> responseEntity = null;
        apiUrl = "feeConfig.listFeeConfigs?page=1&row=1&communityId=" + result.getCommunityId()
                + "&feeName=" + fee.getFeeName();
        responseEntity = this.callCenterService(restTemplate, pd, "", apiUrl, HttpMethod.GET);

        if (responseEntity.getStatusCode() != HttpStatus.OK) { //?????? ??????????????????
            return null;
        }

        JSONObject savedRoomInfoResults = JSONObject.parseObject(responseEntity.getBody());


        if (!savedRoomInfoResults.containsKey("feeConfigs") || savedRoomInfoResults.getJSONArray("feeConfigs").size() != 1) {
            return null;
        }


        JSONObject savedFeeConfigInfo = savedRoomInfoResults.getJSONArray("feeConfigs").getJSONObject(0);

        return savedFeeConfigInfo;
    }


    /**
     * ???????????????????????????
     * room.queryRooms
     *
     * @param pd
     * @param result
     * @param room
     * @return
     */
    private JSONObject getExistsRoom(IPageData pd, ComponentValidateResult result, ImportRoom room) {
        String apiUrl = "";
        ResponseEntity<String> responseEntity = null;
        apiUrl = "room.queryRooms?page=1&row=1&communityId=" + result.getCommunityId()
                + "&floorId=" + room.getFloor().getFloorId() + "&unitId=" + room.getFloor().getUnitId() + "&roomNum=" + room.getRoomNum();
        responseEntity = this.callCenterService(restTemplate, pd, "", apiUrl, HttpMethod.GET);

        if (responseEntity.getStatusCode() != HttpStatus.OK) { //?????? ??????????????????
            return null;
        }

        JSONObject savedRoomInfoResults = JSONObject.parseObject(responseEntity.getBody());


        if (!savedRoomInfoResults.containsKey("rooms") || savedRoomInfoResults.getJSONArray("rooms").size() != 1) {
            return null;
        }


        JSONObject savedRoomInfo = savedRoomInfoResults.getJSONArray("rooms").getJSONObject(0);

        return savedRoomInfo;
    }

    /**
     * ??????????????????
     *
     * @param pd
     * @param owners
     * @param result
     * @return
     */
    private ResponseEntity<String> savedOwnerInfo(IPageData pd, List<ImportOwner> owners, ComponentValidateResult result) {
        String apiUrl = "";
        JSONObject paramIn = null;
        ResponseEntity<String> responseEntity = new ResponseEntity<String>("??????", HttpStatus.OK);
        AssetImportLogDto assetImportLogDto = new AssetImportLogDto();
        assetImportLogDto.setSuccessCount(0L);
        assetImportLogDto.setErrorCount(0L);
        assetImportLogDto.setCommunityId(result.getCommunityId());
        assetImportLogDto.setLogType(AssetImportLogDto.LOG_TYPE_OWENR_IMPORT);
        List<AssetImportLogDetailDto> assetImportLogDetailDtos = new ArrayList<>();
        assetImportLogDto.setAssetImportLogDetailDtos(assetImportLogDetailDtos);
        long successCount = 0L;
        long failCount = 0L;
        AssetImportLogDetailDto assetImportLogDetailDto = null;
        try {
            for (ImportOwner owner : owners) {
                JSONObject savedOwnerInfo = getExistsOwner(pd, result, owner);

                if (savedOwnerInfo != null) {
                    owner.setOwnerId(savedOwnerInfo.getString("ownerId"));
                    continue;
                }
                paramIn = new JSONObject();

                apiUrl = "owner.saveOwner";

                paramIn.put("communityId", result.getCommunityId());
                paramIn.put("userId", result.getUserId());
                paramIn.put("name", owner.getOwnerName());
                paramIn.put("age", owner.getAge());
                paramIn.put("link", owner.getTel());
                paramIn.put("sex", owner.getSex());
                paramIn.put("ownerTypeCd", owner.getOwnerTypeCd());
                paramIn.put("idCard", owner.getIdCard());
                paramIn.put("source", "BatchImport");
                if (!OwnerDto.OWNER_TYPE_CD_OWNER.equals(owner.getOwnerTypeCd())) {
                    //????????????ID
                    paramIn.put("ownerId", getOwnerId(owners, owner));
                }
                responseEntity = this.callCenterService(restTemplate, pd, paramIn.toJSONString(), apiUrl, HttpMethod.POST);

                /***************************************??????????????????****************************************************/
                if (responseEntity.getStatusCode() == HttpStatus.OK) {
                    JSONObject body = JSONObject.parseObject(responseEntity.getBody());
                    if (body.containsKey("code") && body.getIntValue("code") != 0) {
                        assetImportLogDetailDto = new AssetImportLogDetailDto();
                        assetImportLogDetailDto.setCommunityId(assetImportLogDto.getCommunityId());
                        assetImportLogDetailDto.setState(body.getString("code"));
                        assetImportLogDetailDto.setMessage(body.getString("msg"));
                        assetImportLogDetailDto.setObjName(owner.getOwnerName());
                        assetImportLogDetailDtos.add(assetImportLogDetailDto);
                        failCount += 1;
                        assetImportLogDto.setSuccessCount(successCount);
                        assetImportLogDto.setErrorCount(failCount);
                        throw new IllegalArgumentException(body.getString("msg"));
                    }
                    savedOwnerInfo = getExistsOwner(pd, result, owner);
                    owner.setOwnerId(savedOwnerInfo.getString("ownerId"));
                    successCount += 1;
                } else {
                    failCount += 1;
                    assetImportLogDetailDto = new AssetImportLogDetailDto();
                    assetImportLogDetailDto.setCommunityId(assetImportLogDto.getCommunityId());
                    if (Assert.isJsonObject(responseEntity.getBody())) {
                        JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
                        assetImportLogDetailDto.setState(paramOut.getString("code"));
                        assetImportLogDetailDto.setMessage(paramOut.getString("msg"));
                    } else {
                        assetImportLogDetailDto.setState("F");
                        assetImportLogDetailDto.setMessage(responseEntity.getBody());
                    }
                    assetImportLogDetailDto.setObjName(owner.getOwnerName());
                    assetImportLogDetailDtos.add(assetImportLogDetailDto);
                }
                assetImportLogDto.setSuccessCount(successCount);
                assetImportLogDto.setErrorCount(failCount);
            }
        } finally {
            saveTransactionLogSMOImpl.saveAssetImportLog(assetImportLogDto);
        }

        return responseEntity;
    }

    private String getOwnerId(List<ImportOwner> owners, ImportOwner owner) {
        for (ImportOwner owner1 : owners) {
            if (owner1.getOwnerNum().equals(owner.getParentOwnerId())) {
                return owner1.getOwnerId();
            }
        }
        throw new IllegalArgumentException("????????????????????????????????????,??????????????????????????????????????????");
    }

    /**
     * ?????? ????????? ????????????
     *
     * @param pd
     * @param floors
     * @param result
     * @return
     */
    private ResponseEntity<String> savedFloorAndUnitInfo(IPageData pd, List<ImportFloor> floors, ComponentValidateResult result) {
        String apiUrl = "";
        JSONObject paramIn = null;
        ResponseEntity<String> responseEntity = new ResponseEntity<String>("??????", HttpStatus.OK);

        AssetImportLogDto assetImportLogDto = new AssetImportLogDto();
        assetImportLogDto.setSuccessCount(0L);
        assetImportLogDto.setErrorCount(0L);
        assetImportLogDto.setCommunityId(result.getCommunityId());
        assetImportLogDto.setLogType(AssetImportLogDto.LOG_TYPE_FLOOR_UNIT_IMPORT);
        List<AssetImportLogDetailDto> assetImportLogDetailDtos = new ArrayList<>();
        assetImportLogDto.setAssetImportLogDetailDtos(assetImportLogDetailDtos);
        long successCount = 0L;
        long failCount = 0L;
        AssetImportLogDetailDto assetImportLogDetailDto = null;
        try {
            for (ImportFloor importFloor : floors) {
                paramIn = new JSONObject();
                //????????? ????????????
                JSONObject savedFloorInfo = getExistsFloor(pd, result, importFloor);
                // ???????????????????????????
                if (savedFloorInfo == null) {
                    apiUrl = "floor.saveFloor";
                    paramIn.put("communityId", result.getCommunityId());
                    paramIn.put("floorNum", importFloor.getFloorNum());
                    paramIn.put("userId", result.getUserId());
                    paramIn.put("name", importFloor.getFloorNum() + "???");
                    paramIn.put("floorArea", 1.00);

                    responseEntity = this.callCenterService(restTemplate, pd, paramIn.toJSONString(), apiUrl, HttpMethod.POST);
                    savedFloorInfo = getExistsFloor(pd, result, importFloor);
                }

                /***************************************??????????????????****************************************************/
                if (responseEntity != null && responseEntity.getStatusCode() != HttpStatus.OK) { //?????? ??????????????????
                    failCount += 1;
                    assetImportLogDetailDto = new AssetImportLogDetailDto();
                    assetImportLogDetailDto.setCommunityId(assetImportLogDto.getCommunityId());
                    if (Assert.isJsonObject(responseEntity.getBody())) {
                        JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
                        assetImportLogDetailDto.setState(paramOut.getString("code"));
                        assetImportLogDetailDto.setMessage(paramOut.getString("msg"));
                    } else {
                        assetImportLogDetailDto.setState("F");
                        assetImportLogDetailDto.setMessage(responseEntity.getBody());
                    }
                    assetImportLogDetailDto.setObjName(importFloor.getFloorNum() + "???");
                    assetImportLogDetailDtos.add(assetImportLogDetailDto);
                    assetImportLogDto.setSuccessCount(successCount);
                    assetImportLogDto.setErrorCount(failCount);
                    continue;
                } else {
                    successCount += 1;
                    assetImportLogDto.setSuccessCount(successCount);
                    assetImportLogDto.setErrorCount(failCount);
                }


                if (savedFloorInfo == null) {
                    continue;
                }
                importFloor.setFloorId(savedFloorInfo.getString("floorId"));
                paramIn.clear();
                //?????????????????????????????????????????????????????????????????????unit.queryUnits
                JSONObject savedUnitInfo = getExistsUnit(pd, result, importFloor);
                if (savedUnitInfo != null) {
                    importFloor.setUnitId(savedUnitInfo.getString("unitId"));
                    continue;
                }

                apiUrl = "unit.saveUnit";

                paramIn.put("communityId", result.getCommunityId());
                paramIn.put("floorId", savedFloorInfo.getString("floorId"));
                paramIn.put("unitNum", importFloor.getUnitNum());
                paramIn.put("layerCount", importFloor.getLayerCount());
                paramIn.put("lift", importFloor.getLift());
                paramIn.put("unitArea", 1.00);
                responseEntity = this.callCenterService(restTemplate, pd, paramIn.toJSONString(), apiUrl, HttpMethod.POST);
                /****************************** ???????????????????????? *******************************/
                if (responseEntity != null && responseEntity.getStatusCode() != HttpStatus.OK) { //?????? ??????????????????
                    assetImportLogDetailDto = new AssetImportLogDetailDto();
                    assetImportLogDetailDto.setCommunityId(assetImportLogDto.getCommunityId());
                    if (Assert.isJsonObject(responseEntity.getBody())) {
                        JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
                        assetImportLogDetailDto.setState(paramOut.getString("code"));
                        assetImportLogDetailDto.setMessage(paramOut.getString("msg"));
                    } else {
                        assetImportLogDetailDto.setState("F");
                        assetImportLogDetailDto.setMessage(responseEntity.getBody());
                    }
                    assetImportLogDetailDto.setObjName(importFloor.getFloorNum() + "???" + importFloor.getUnitNum() + "??????");
                    assetImportLogDetailDtos.add(assetImportLogDetailDto);
                    failCount += 1;
                    successCount = successCount > 0 ? successCount - 1 : successCount;
                    assetImportLogDto.setSuccessCount(successCount);
                    assetImportLogDto.setErrorCount(failCount);
                } else {
                    JSONObject body = JSONObject.parseObject(responseEntity.getBody());
                    if (body.containsKey("code") && body.getIntValue("code") != 0) {
                        assetImportLogDetailDto = new AssetImportLogDetailDto();
                        assetImportLogDetailDto.setCommunityId(assetImportLogDto.getCommunityId());
                        assetImportLogDetailDto.setState(body.getString("code"));
                        assetImportLogDetailDto.setMessage(body.getString("msg"));
                        assetImportLogDetailDto.setObjName(importFloor.getFloorNum() + "???" + importFloor.getUnitNum() + "??????");
                        assetImportLogDetailDtos.add(assetImportLogDetailDto);
                        failCount += 1;
                        successCount = successCount > 0 ? successCount - 1 : successCount;
                        assetImportLogDto.setSuccessCount(successCount);
                        assetImportLogDto.setErrorCount(failCount);
                    }
                }
                //???unitId ??????ImportFloor??????
                savedUnitInfo = getExistsUnit(pd, result, importFloor);
                importFloor.setUnitId(savedUnitInfo.getString("unitId"));

            }
        } finally {
            saveTransactionLogSMOImpl.saveAssetImportLog(assetImportLogDto);
        }
        return responseEntity;
    }

    private JSONObject getExistsUnit(IPageData pd, ComponentValidateResult result, ImportFloor importFloor) {
        String apiUrl = "";
        ResponseEntity<String> responseEntity = null;
        apiUrl = "unit.queryUnits?communityId=" + result.getCommunityId()
                + "&floorId=" + importFloor.getFloorId() + "&unitNum=" + importFloor.getUnitNum();
        responseEntity = this.callCenterService(restTemplate, pd, "", apiUrl, HttpMethod.GET);

        if (responseEntity.getStatusCode() != HttpStatus.OK) { //?????? ??????????????????
            return null;
        }

        JSONArray savedFloorInfoResults = JSONArray.parseArray(responseEntity.getBody());

        if (savedFloorInfoResults == null || savedFloorInfoResults.size() != 1) {
            return null;
        }

        JSONObject savedUnitInfo = savedFloorInfoResults.getJSONObject(0);

        return savedUnitInfo;
    }

    private JSONObject getExistsFloor(IPageData pd, ComponentValidateResult result, ImportFloor importFloor) {
        String apiUrl = "";
        ResponseEntity<String> responseEntity = null;
        apiUrl = "floor.queryFloors?page=1&row=1&communityId=" + result.getCommunityId() + "&floorNum=" + importFloor.getFloorNum();
        responseEntity = this.callCenterService(restTemplate, pd, "", apiUrl, HttpMethod.GET);

        if (responseEntity.getStatusCode() != HttpStatus.OK) { //?????? ??????????????????
            return null;
        }

        JSONObject savedFloorInfoResult = JSONObject.parseObject(responseEntity.getBody());

        if (!savedFloorInfoResult.containsKey("apiFloorDataVoList") || savedFloorInfoResult.getJSONArray("apiFloorDataVoList").size() != 1) {
            return null;
        }

        JSONObject savedFloorInfo = savedFloorInfoResult.getJSONArray("apiFloorDataVoList").getJSONObject(0);

        return savedFloorInfo;
    }

    /**
     * ?????????????????????
     *
     * @param pd
     * @param result
     * @param importOwner
     * @return
     */
    private JSONObject getExistsOwner(IPageData pd, ComponentValidateResult result, ImportOwner importOwner) {
        String apiUrl = "";
        ResponseEntity<String> responseEntity = null;
        apiUrl = "owner.queryOwners?page=1&row=1&communityId=" + result.getCommunityId()
                + "&ownerTypeCd=" + importOwner.getOwnerTypeCd() + "&name=" + importOwner.getOwnerName() + "&link=" + importOwner.getTel();
        responseEntity = this.callCenterService(restTemplate, pd, "", apiUrl, HttpMethod.GET);

        if (responseEntity.getStatusCode() != HttpStatus.OK) { //?????? ??????????????????
            return null;
        }

        JSONObject savedOwnerInfoResult = JSONObject.parseObject(responseEntity.getBody());

        if (!savedOwnerInfoResult.containsKey("owners") || savedOwnerInfoResult.getJSONArray("owners").size() != 1) {
            return null;
        }

        JSONObject savedOwnerInfo = savedOwnerInfoResult.getJSONArray("owners").getJSONObject(0);

        return savedOwnerInfo;
    }

    /**
     * ????????????????????????
     *
     * @param pd
     * @param result
     * @param parkingSpace
     * @return
     */
    private JSONObject getExistsParkingArea(IPageData pd, ComponentValidateResult result, ImportParkingSpace parkingSpace) {
        String apiUrl = "";
        ResponseEntity<String> responseEntity = null;
        apiUrl = "parkingArea.listParkingAreas?page=1&row=1&communityId=" + result.getCommunityId()
                + "&num=" + parkingSpace.getPaNum();
        responseEntity = this.callCenterService(restTemplate, pd, "", apiUrl, HttpMethod.GET);

        if (responseEntity.getStatusCode() != HttpStatus.OK) { //?????? ??????????????????
            return null;
        }

        JSONObject savedParkingAreaInfoResult = JSONObject.parseObject(responseEntity.getBody());

        if (!savedParkingAreaInfoResult.containsKey("parkingAreas") || savedParkingAreaInfoResult.getJSONArray("parkingAreas").size() != 1) {
            return null;
        }

        JSONObject savedParkingAreaInfo = savedParkingAreaInfoResult.getJSONArray("parkingAreas").getJSONObject(0);

        return savedParkingAreaInfo;
    }

    /**
     * ??????????????????
     *
     * @param floors
     * @param owners
     * @param rooms
     * @param parkingSpaces
     */
    private void importExcelDataValidate(List<ImportFloor> floors, List<ImportOwner> owners, List<ImportRoom> rooms, List<ImportFee> fees, List<ImportParkingSpace> parkingSpaces) {

    }

    /**
     * ??????????????????
     *
     * @param workbook
     * @param parkingSpaces
     */
    private void getParkingSpaces(Workbook workbook, List<ImportParkingSpace> parkingSpaces, List<ImportOwner> owners) {
        Sheet sheet = null;
        sheet = ImportExcelUtils.getSheet(workbook, "????????????");
        List<Object[]> oList = ImportExcelUtils.listFromSheet(sheet);
        ImportParkingSpace importParkingSpace = null;
        for (int osIndex = 0; osIndex < oList.size(); osIndex++) {
            Object[] os = oList.get(osIndex);
            if (osIndex == 0) { // ???????????? ???????????? ????????????
                continue;
            }
            if (StringUtil.isNullOrNone(os[0])) {
                continue;
            }
            Assert.hasValue(os[0], "?????????????????????" + (osIndex + 1) + "????????????????????????");
            Assert.hasValue(os[1], "?????????????????????" + (osIndex + 1) + "?????????????????????");
            Assert.hasValue(os[2], "?????????????????????" + (osIndex + 1) + "????????????????????????");
            Assert.hasValue(os[3], "?????????????????????" + (osIndex + 1) + "?????????????????????????????????????????? ???10");
            importParkingSpace = new ImportParkingSpace();
            importParkingSpace.setPaNum(os[0].toString());
            importParkingSpace.setPsNum(os[1].toString());
            importParkingSpace.setTypeCd(os[2].toString());
            importParkingSpace.setArea(Double.parseDouble(os[3].toString()));
            if (os.length < 5 || StringUtil.isNullOrNone(os[4])) {
                parkingSpaces.add(importParkingSpace);
                continue;
            }
            ImportOwner importOwner = getImportOwner(owners, os[4].toString());
            importParkingSpace.setImportOwner(importOwner);
            if (importOwner != null) {
                importParkingSpace.setCarNum(os[5].toString());
                importParkingSpace.setCarBrand(os[6].toString());
                importParkingSpace.setCarType(os[7].toString());
                importParkingSpace.setCarColor(os[8].toString());
                importParkingSpace.setSellOrHire(os[9].toString());

                String startTime = excelDoubleToDate(os[10].toString());
                String endTime = excelDoubleToDate(os[11].toString());
                Assert.isDate(startTime, DateUtil.DATE_FORMATE_STRING_B, (osIndex + 1) + "??????????????????????????? ?????????YYYY-MM-DD ????????????");
                Assert.isDate(endTime, DateUtil.DATE_FORMATE_STRING_B, (osIndex + 1) + "??????????????????????????? ?????????YYYY-MM-DD ????????????");
                importParkingSpace.setStartTime(startTime);
                importParkingSpace.setEndTime(endTime);
            }

            parkingSpaces.add(importParkingSpace);
        }
    }

    //??????Excel????????????
    public static String excelDoubleToDate(String strDate) {
        if (strDate.length() == 5) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
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

    /**
     * ?????? ????????????
     *
     * @param workbook
     * @param rooms
     */
    private void getRooms(Workbook workbook, List<ImportRoom> rooms,
                          List<ImportFloor> floors, List<ImportOwner> owners) {
        Sheet sheet = null;
        sheet = ImportExcelUtils.getSheet(workbook, "????????????");
        List<Object[]> oList = ImportExcelUtils.listFromSheet(sheet);
        ImportRoom importRoom = null;
        for (int osIndex = 0; osIndex < oList.size(); osIndex++) {
            try {
                Object[] os = oList.get(osIndex);
                if (osIndex == 0) { // ???????????? ???????????? ????????????
                    continue;
                }
                if (StringUtil.isNullOrNone(os[0])) {
                    continue;
                }
                Assert.hasValue(os[1], "?????????????????????" + (osIndex + 1) + "?????????????????????");
                Assert.hasValue(os[2], "?????????????????????" + (osIndex + 1) + "?????????????????????");
                Assert.hasValue(os[3], "?????????????????????" + (osIndex + 1) + "?????????????????????");
                Assert.hasValue(os[4], "?????????????????????" + (osIndex + 1) + "?????????????????????");
                Assert.hasValue(os[5], "?????????????????????" + (osIndex + 1) + "?????????????????????");
                if (os.length > 6 && !StringUtil.isNullOrNone(os[6])) {
                    Assert.hasValue(os[7], "?????????????????????" + (osIndex + 1) + "?????????????????????");
                    Assert.hasValue(os[8], "?????????????????????" + (osIndex + 1) + "???????????????????????????");
                }
                Assert.hasValue(os[9], "?????????????????????" + (osIndex + 1) + "?????????????????????");
                Assert.hasValue(os[10], "?????????????????????" + (osIndex + 1) + "?????????????????????");
                Assert.hasValue(os[11], "?????????????????????" + (osIndex + 1) + "???????????????");
                importRoom = new ImportRoom();
                importRoom.setRoomNum(os[0].toString());
                importRoom.setFloor(getImportFloor(floors, os[1].toString(), os[2].toString()));
                importRoom.setLayer(Integer.parseInt(os[3].toString()));
                importRoom.setSection(os[4].toString());
                importRoom.setBuiltUpArea(Double.parseDouble(os[5].toString()));
                importRoom.setRoomSubType(os[9].toString());
                importRoom.setRoomArea(os[10].toString());
                importRoom.setRoomRent(os[11].toString());


                if (os.length > 6 && !StringUtil.isNullOrNone(os[6])) {
                    importRoom.setRoomFeeId(os[7].toString());
                    String feeEndDate = excelDoubleToDate(os[8].toString());
                    importRoom.setFeeEndDate(feeEndDate);
                }
                if (os.length < 7 || StringUtil.isNullOrNone(os[6])) {
                    rooms.add(importRoom);
                    continue;
                }
                importRoom.setImportOwner(getImportOwner(owners, os[6].toString()));
                rooms.add(importRoom);
            } catch (Exception e) {
                logger.error("????????????????????????", e);
                throw new IllegalArgumentException("????????????sheet??????" + (osIndex + 1) + "???????????????????????????" + e.getLocalizedMessage());
            }
        }
    }

    /**
     * ?????? ????????????
     *
     * @param workbook
     * @param importFees
     */
    private void getFee(Workbook workbook, List<ImportFee> importFees) {
        Sheet sheet = null;
        sheet = ImportExcelUtils.getSheet(workbook, "????????????");
        List<Object[]> oList = ImportExcelUtils.listFromSheet(sheet);
        ImportFee importFee = null;
        for (int osIndex = 0; osIndex < oList.size(); osIndex++) {
            Object[] os = oList.get(osIndex);
            if (osIndex == 0) { // ???????????? ???????????? ????????????
                continue;
            }
            if (StringUtil.isNullOrNone(os[0])) {
                continue;
            }
            Assert.hasValue(os[0], "?????????????????????" + (osIndex + 1) + "?????????????????????");
            Assert.hasValue(os[1], "?????????????????????" + (osIndex + 1) + "?????????????????????");
            Assert.hasValue(os[2], "?????????????????????" + (osIndex + 1) + "?????????????????????");
            Assert.hasValue(os[3], "?????????????????????" + (osIndex + 1) + "?????????????????????");
            Assert.hasValue(os[4], "?????????????????????" + (osIndex + 1) + "?????????????????????");
            Assert.hasValue(os[5], "?????????????????????" + (osIndex + 1) + "?????????????????????");
            Assert.isInteger(os[5].toString(), "?????????????????????" + (osIndex + 1) + "??????????????????????????????");
            Assert.hasValue(os[6], "?????????????????????" + (osIndex + 1) + "?????????????????????");
            Assert.isDate(os[7].toString(), DateUtil.DATE_FORMATE_STRING_B, "?????????????????????" + (osIndex + 1) + "????????????????????????????????????????????? ???????????????2020-06-01");
            Assert.isDate(os[8].toString(), DateUtil.DATE_FORMATE_STRING_B, "?????????????????????" + (osIndex + 1) + "????????????????????????????????????????????? ???????????????2037-01-01");
            Assert.hasValue(os[9], "?????????????????????" + (osIndex + 1) + "?????????????????????");
            if (!"1001".equals(os[9].toString()) && !"2002".equals(os[9].toString())) {
                throw new IllegalArgumentException("?????????????????????" + (osIndex + 1) + "????????????????????? ?????????1001 ??????2002");
            }
            Assert.hasValue(os[10], "?????????????????????" + (osIndex + 1) + "?????????????????????");
            Assert.hasValue(os[11], "?????????????????????" + (osIndex + 1) + "?????????????????????");
            importFee = new ImportFee();
            importFee.setId(os[0].toString());
            importFee.setFeeTypeCd("?????????".equals(os[1]) ? "888800010001" : "888800010002");
            importFee.setFeeName(os[2].toString());
            importFee.setFeeFlag("???????????????".equals(os[3]) ? "1003006" : "2006012");
            importFee.setPaymentCd("?????????".equals(os[4]) ? "1200" : "2100");
            String billType = "";
            if ("??????1???1???".equals(os[6])) {
                billType = "001";
            } else if ("??????1???".equals(os[6])) {
                billType = "002";
            } else if ("??????".equals(os[6])) {
                billType = "003";
            } else {
                billType = "004";
            }
            importFee.setBillType(billType);
            importFee.setPaymentCycle(os[5].toString());
            importFee.setStartTime(os[7].toString());
            importFee.setEndTime(os[8].toString());
            importFee.setComputingFormula(os[9].toString());
            importFee.setSquarePrice(os[10].toString());
            importFee.setAdditionalAmount(os[11].toString());
            importFees.add(importFee);
        }
    }

    /**
     * ???????????????????????????????????????????????????????????? null
     *
     * @param owners
     * @param ownerNum
     * @return
     */
    private ImportOwner getImportOwner(List<ImportOwner> owners, String ownerNum) {
        for (ImportOwner importOwner : owners) {
            if (ownerNum.equals(importOwner.getOwnerNum())) {
                return importOwner;
            }
        }

        return null;

    }

    /**
     * get ??????????????????
     *
     * @param floors
     */
    private ImportFloor getImportFloor(List<ImportFloor> floors, String floorNum, String unitNum) {
        for (ImportFloor importFloor : floors) {
            if (floorNum.equals(importFloor.getFloorNum())
                    && unitNum.equals(importFloor.getUnitNum())) {
                return importFloor;
            }
        }

        throw new IllegalArgumentException("???????????????sheet????????????????????????[" + floorNum + "],????????????[" + unitNum + "]??????");
    }

    /**
     * ??????????????????
     *
     * @param workbook
     * @param owners
     */
    private void getOwners(Workbook workbook, List<ImportOwner> owners) {
        Sheet sheet = null;
        sheet = ImportExcelUtils.getSheet(workbook, "????????????");
        List<Object[]> oList = ImportExcelUtils.listFromSheet(sheet);
        ImportOwner importOwner = null;
        for (int osIndex = 0; osIndex < oList.size(); osIndex++) {
            try {
                Object[] os = oList.get(osIndex);
                if (osIndex == 0) { // ???????????? ???????????? ????????????
                    continue;
                }
                if (StringUtil.isNullOrNone(os[0])) {
                    continue;
                }
                Assert.hasValue(os[0], "?????????????????????" + (osIndex + 1) + "?????????????????????");
                Assert.hasValue(os[1], "?????????????????????" + (osIndex + 1) + "?????????????????????");
                Assert.hasValue(os[2], "?????????????????????" + (osIndex + 1) + "?????????????????????");
                String tel = StringUtil.isNullOrNone(os[4]) ? "19999999999" : os[4].toString();
                String idCard = StringUtil.isNullOrNone(os[5]) ? "10000000000000000001" : os[5].toString();
                String ownerTypeCd = StringUtil.isNullOrNone(os[6]) ? "1001" : os[6].toString();
                String parentOwnerId = StringUtil.isNullOrNone(os[7]) ? os[0].toString() : os[7].toString();

                if (os[4].toString().length() > 11) {
                    throw new IllegalArgumentException(os[1].toString() + " ??????????????????11???,?????????");
                }
                if (os[5].toString().length() > 18) {
                    throw new IllegalArgumentException(os[1].toString() + " ??????????????????18???,?????????");
                }

                String age = StringUtil.isNullOrNone(os[3]) ? CommonUtil.getAgeByCertId(idCard) : os[3].toString();
                importOwner = new ImportOwner();
                importOwner.setOwnerNum(os[0].toString());
                importOwner.setOwnerName(os[1].toString());
                importOwner.setSex("???".equals(os[2].toString()) ? "0" : "1");
                importOwner.setAge(Integer.parseInt(age));
                importOwner.setTel(tel);
                importOwner.setIdCard(idCard);
                importOwner.setOwnerTypeCd(ownerTypeCd);
                importOwner.setParentOwnerId(parentOwnerId);
                owners.add(importOwner);
            } catch (Exception e) {
                logger.error("???" + (osIndex + 1) + "?????????????????????", e);
                throw new IllegalArgumentException("???" + (osIndex + 1) + "?????????????????????" + e.getLocalizedMessage(), e);
            }
        }
    }

    /**
     * ????????????
     *
     * @param workbook
     * @param floors
     */
    private void getFloors(Workbook workbook, List<ImportFloor> floors) {
        Sheet sheet = null;
        sheet = ImportExcelUtils.getSheet(workbook, "????????????");
        List<Object[]> oList = ImportExcelUtils.listFromSheet(sheet);
        ImportFloor importFloor = null;
        for (int osIndex = 0; osIndex < oList.size(); osIndex++) {
            Object[] os = oList.get(osIndex);
            if (osIndex == 0) { // ???????????? ???????????? ????????????
                continue;
            }

            if (StringUtil.isNullOrNone(os[0])) {
                continue;
            }

            Assert.hasValue(os[0], "?????????????????????" + (osIndex + 1) + "??????????????????");
            Assert.hasValue(os[1], "?????????????????????" + (osIndex + 1) + "?????????????????????");
            Assert.hasValue(os[2], "?????????????????????" + (osIndex + 1) + "??????????????????");
            Assert.hasValue(os[3], "?????????????????????" + (osIndex + 1) + "????????????????????????");
            importFloor = new ImportFloor();
            importFloor.setFloorNum(os[0].toString());
            importFloor.setUnitNum(os[1].toString());
            importFloor.setLayerCount(os[2].toString());
            importFloor.setLift("???".equals(os[3].toString()) ? "1010" : "2020");
            floors.add(importFloor);
        }
    }


    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
}
