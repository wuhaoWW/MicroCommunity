package com.java110.boot.smo.assetImport.impl;

import com.java110.boot.smo.DefaultAbstractComponentSMO;
import com.java110.boot.smo.assetImport.IImportOwnerRoomSMO;
import com.java110.core.context.IPageData;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.RoomDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.entity.assetImport.ImportOwnerRoomDto;
import com.java110.entity.component.ComponentValidateResult;
import com.java110.intf.community.IImportOwnerRoomInnerServiceSMO;
import com.java110.utils.util.Assert;
import com.java110.utils.util.CommonUtil;
import com.java110.utils.util.ImportExcelUtils;
import com.java110.utils.util.StringUtil;
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

@Service("importOwnerRoomSMOImpl")
public class ImportOwnerRoomSMOImpl extends DefaultAbstractComponentSMO implements IImportOwnerRoomSMO {

    private final static Logger logger = LoggerFactory.getLogger(ImportOwnerRoomSMOImpl.class);

    public static final int DEFAULT_ROWS = 500;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private IImportOwnerRoomInnerServiceSMO importOwnerRoomInnerServiceSMOImpl;


    @Override
    public ResponseEntity<String> importExcelData(IPageData pd, MultipartFile uploadFile) throws Exception {
        try {
            ComponentValidateResult result = this.validateStoreStaffCommunityRelationship(pd, restTemplate);
            //InputStream is = uploadFile.getInputStream();
            Workbook workbook = ImportExcelUtils.createWorkbook(uploadFile);  //?????????
            List<ImportOwnerRoomDto> ownerRooms = new ArrayList<ImportOwnerRoomDto>();
            //????????????
            getOwnerRooms(workbook, ownerRooms, result);

            //??????????????????
            validateRoomInfo(ownerRooms);

            // ????????????
            return dealExcelData(pd, ownerRooms, result);
        } catch (Exception e) {
            logger.error("???????????? ", e);
            return new ResponseEntity<String>("????????????????????????????????????????????????" + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    private boolean hasSpecialCharacters(String str) {
        if (str.contains("-") || str.contains("#") || str.contains("?") || str.contains("&")) {
            return true;
        }

        return false;
    }

    /**
     * ??????????????????
     *
     * @param ownerRooms
     */
    private void validateRoomInfo(List<ImportOwnerRoomDto> ownerRooms) {
        ImportOwnerRoomDto importOwnerRoomDto = null;
        ImportOwnerRoomDto tmpImportOwnerRoomDto = null;
        boolean hasOwnerType = false;
        for (int roomIndex = 0; roomIndex < ownerRooms.size(); roomIndex++) {
            importOwnerRoomDto = ownerRooms.get(roomIndex);
            // 1????????????????????? ???????????? -  #  ??? & ???????????????
            if (hasSpecialCharacters(importOwnerRoomDto.getFloorNum())) {
                throw new IllegalArgumentException((roomIndex + 1) + "??????????????????????????????  -  #  ??? & ????????????");
            }
            if (hasSpecialCharacters(importOwnerRoomDto.getUnitNum())) {
                throw new IllegalArgumentException((roomIndex + 1) + "??????????????????????????????  -  #  ??? & ????????????");
            }
            if (hasSpecialCharacters(importOwnerRoomDto.getRoomNum())) {
                throw new IllegalArgumentException((roomIndex + 1) + "??????????????????????????????  -  #  ??? & ????????????");
            }

//            if (!StringUtil.isNumber(importOwnerRoomDto.getLayer())) {
//                throw new IllegalArgumentException((roomIndex + 1) + "???????????????????????????");
//            }

            if (!StringUtil.isNumber(importOwnerRoomDto.getLayerCount())) {
                throw new IllegalArgumentException((roomIndex + 1) + "??????????????????????????????");
            }
            if (!StringUtil.isNumber(importOwnerRoomDto.getRoomSubType())) {
                throw new IllegalArgumentException((roomIndex + 1) + "?????????????????????????????????");
            }

            if (StringUtil.isEmpty(importOwnerRoomDto.getSection())) {
                throw new IllegalArgumentException((roomIndex + 1) + "???????????????????????????");
            }

            if (StringUtil.isEmpty(importOwnerRoomDto.getBuiltUpArea())) {
                throw new IllegalArgumentException((roomIndex + 1) + "???????????????????????????");
            }

            if (StringUtil.isEmpty(importOwnerRoomDto.getRoomArea())) {
                throw new IllegalArgumentException((roomIndex + 1) + "???????????????????????????");
            }
            if (StringUtil.isEmpty(importOwnerRoomDto.getRoomRent())) {
                throw new IllegalArgumentException((roomIndex + 1) + "?????????????????????");
            }
            // ?????????????????? ?????????????????????????????????
            if (StringUtil.isEmpty(importOwnerRoomDto.getOwnerName())) {
                continue;
            }

            if (StringUtil.isEmpty(importOwnerRoomDto.getSex())) {
                throw new IllegalArgumentException((roomIndex + 1) + "?????????????????????");
            }

            if (StringUtil.isEmpty(importOwnerRoomDto.getAge())) {
                throw new IllegalArgumentException((roomIndex + 1) + "?????????????????????");
            }
            //???????????? ??????????????? ????????????????????????????????? ?????? ???????????????????????????????????? ????????????????????????
            //??????????????????????????????
            if (StringUtil.isEmpty(importOwnerRoomDto.getTel())) {
                throw new IllegalArgumentException((roomIndex + 1) + "????????????????????????");
            }

            if (StringUtil.isEmpty(importOwnerRoomDto.getIdCard())) {
                throw new IllegalArgumentException((roomIndex + 1) + "???????????????????????????");
            }

            if (importOwnerRoomDto.getIdCard().length() > 18) {
                throw new IllegalArgumentException((roomIndex + 1) + " ??????????????????18???,?????????");
            }
            if (!StringUtil.isNumber(importOwnerRoomDto.getOwnerTypeCd())) {
                throw new IllegalArgumentException((roomIndex + 1) + "???????????????????????????");
            }

            if(RoomDto.STATE_FREE.equals(importOwnerRoomDto.getRoomState()) && !StringUtil.isEmpty(importOwnerRoomDto.getOwnerName())){
                throw new IllegalArgumentException((roomIndex + 1) + "????????????????????????????????????????????????????????????");
            }

            if(!RoomDto.STATE_FREE.equals(importOwnerRoomDto.getRoomState()) && StringUtil.isEmpty(importOwnerRoomDto.getOwnerName())){
                throw new IllegalArgumentException((roomIndex + 1) + "???????????????????????????????????????????????????????????????");
            }

            // ??????????????? ??????
            if (OwnerDto.OWNER_TYPE_CD_OWNER.equals(importOwnerRoomDto.getOwnerTypeCd())) {
                continue;
            }
            // ?????????????????????????????? ????????????
            hasOwnerType = false;
            for (int preRoomIndex = 0; preRoomIndex < roomIndex; preRoomIndex++) {
                tmpImportOwnerRoomDto = ownerRooms.get(preRoomIndex);

                if (tmpImportOwnerRoomDto.getFloorNum().equals(importOwnerRoomDto.getFloorNum())
                        && tmpImportOwnerRoomDto.getUnitNum().equals(importOwnerRoomDto.getUnitNum())
                        && tmpImportOwnerRoomDto.getRoomNum().equals(importOwnerRoomDto.getRoomNum())
                        && OwnerDto.OWNER_TYPE_CD_OWNER.equals(tmpImportOwnerRoomDto.getOwnerTypeCd())) {
                    hasOwnerType = true;
                }
            }

            if (!hasOwnerType) {
                throw new IllegalArgumentException((roomIndex + 1) + "??????????????????????????????????????? ?????? ?????????????????????????????? ??????????????????????????????????????? ????????????");
            }
        }
    }

    /**
     * ????????????????????????
     *
     * @param workbook
     * @param ownerRoomDtos
     */
    private void getOwnerRooms(Workbook workbook, List<ImportOwnerRoomDto> ownerRoomDtos, ComponentValidateResult result) throws ParseException {
        Sheet sheet = null;
        sheet = ImportExcelUtils.getSheet(workbook, "????????????");
        List<Object[]> oList = ImportExcelUtils.listFromSheet(sheet);
        ImportOwnerRoomDto importOwnerRoomDto = null;
        for (int osIndex = 0; osIndex < oList.size(); osIndex++) {

            Object[] os = oList.get(osIndex);

            if (osIndex == 0) { // ???????????? ???????????? ????????????
                continue;
            }
            if (os == null || StringUtil.isNullOrNone(os[0])) {
                continue;
            }
            Assert.hasValue(os[0], (osIndex + 1) + "?????????????????????");
            Assert.hasValue(os[1], (osIndex + 1) + "?????????????????????");
            Assert.hasValue(os[2], (osIndex + 1) + "?????????????????????");
            Assert.hasValue(os[3], (osIndex + 1) + "?????????????????????");
            Assert.hasValue(os[4], (osIndex + 1) + "????????????????????????");
            Assert.hasValue(os[5], (osIndex + 1) + "??????????????????????????????");
            Assert.hasValue(os[6], (osIndex + 1) + "???????????????????????????");
            Assert.hasValue(os[7], (osIndex + 1) + "???????????????????????????");
            Assert.hasValue(os[8], (osIndex + 1) + "???????????????????????????");
            Assert.hasValue(os[9], (osIndex + 1) + "???????????????????????????");
            Assert.hasValue(os[10], (osIndex + 1) + "?????????????????????");
            Assert.hasValue(os[11], (osIndex + 1) + "???????????????????????????");
            if (os.length > 12 && !StringUtil.isNullOrNone(os[12])) {
                Assert.hasValue(os[12], (osIndex + 1) + "???????????????????????????");
                Assert.hasValue(os[13], (osIndex + 1) + "?????????????????????");
                Assert.hasValue(os[14], (osIndex + 1) + "?????????????????????");
                Assert.hasValue(os[15], (osIndex + 1) + "????????????????????????");
                Assert.hasValue(os[16], (osIndex + 1) + "????????????????????????");
                Assert.hasValue(os[17], (osIndex + 1) + "???????????????????????????");
            }

            importOwnerRoomDto = new ImportOwnerRoomDto();
            importOwnerRoomDto.setCommunityId(result.getCommunityId());
            importOwnerRoomDto.setUserId(result.getUserId());
            importOwnerRoomDto.setFloorNum(os[0].toString().trim());
            importOwnerRoomDto.setUnitNum(os[1].toString().trim());
            importOwnerRoomDto.setRoomNum(os[2].toString().trim());
            importOwnerRoomDto.setLayer(os[3].toString().trim());
            importOwnerRoomDto.setLayerCount(os[4].toString().trim());
            //importOwnerRoomDto.setLift(os[5].toString().trim());
            importOwnerRoomDto.setLift("???".equals(os[5].toString().trim()) ? "1010" : "2020");
            importOwnerRoomDto.setRoomSubType(os[6].toString().trim());
            importOwnerRoomDto.setSection(os[7].toString().trim());
            importOwnerRoomDto.setBuiltUpArea(os[8].toString().trim());
            importOwnerRoomDto.setRoomArea(os[9].toString().trim());
            importOwnerRoomDto.setRoomRent(os[10].toString().trim());
            importOwnerRoomDto.setRoomState(os[11].toString().trim());
            if (os.length > 12 && !StringUtil.isNullOrNone(os[12])) {
                importOwnerRoomDto.setOwnerName(os[12].toString().trim());
                importOwnerRoomDto.setSex("???".equals(os[13].toString().trim()) ? "0" : "1");
                String age = StringUtil.isNullOrNone(os[14]) ? CommonUtil.getAgeByCertId(os[16].toString().trim()) : os[14].toString().trim();
                importOwnerRoomDto.setAge(age);
                importOwnerRoomDto.setTel(os[15].toString().trim());
                importOwnerRoomDto.setIdCard(os[16].toString().trim());
                importOwnerRoomDto.setOwnerTypeCd(os[17].toString().trim());
            }



            ownerRoomDtos.add(importOwnerRoomDto);

        }
    }


    /**
     * ??????????????????
     */
    private ResponseEntity<String> dealExcelData(IPageData pd, List<ImportOwnerRoomDto> ownerRoomDtos, ComponentValidateResult result) {
        ResponseEntity<String> responseEntity = null;

        List<ImportOwnerRoomDto> tmpImportOwnerRoomDtos = new ArrayList<>();
        int flag = 0;

        int successCount = 0;
        try {
            for (int roomIndex = 0; roomIndex < ownerRoomDtos.size(); roomIndex++) {
                tmpImportOwnerRoomDtos.add(ownerRoomDtos.get(roomIndex));
                if (tmpImportOwnerRoomDtos.size() > DEFAULT_ROWS) {
                    flag = importOwnerRoomInnerServiceSMOImpl.saveOwnerRooms(tmpImportOwnerRoomDtos);
                    if (flag < 1) {
                        throw new IllegalArgumentException("????????????");
                    }
                    tmpImportOwnerRoomDtos = new ArrayList<>();

                    successCount += flag;
                }
            }

            if (tmpImportOwnerRoomDtos.size() > 0) {
                flag = importOwnerRoomInnerServiceSMOImpl.saveOwnerRooms(tmpImportOwnerRoomDtos);
                if (flag < 1) {
                    throw new IllegalArgumentException("????????????");
                }
                successCount += flag;
            }
        } catch (Exception e) {
            logger.error("????????????", e);
            //????????????????????????
            throw e;
        }

        return ResultVo.createResponseEntity("????????????:" + ownerRoomDtos.size() + ";???????????????" + successCount + ";???????????????" + (ownerRoomDtos.size() - successCount));
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
