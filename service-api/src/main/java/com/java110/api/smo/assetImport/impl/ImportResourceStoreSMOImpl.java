package com.java110.api.smo.assetImport.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.api.smo.DefaultAbstractComponentSMO;
import com.java110.api.smo.assetImport.IImportResourceStoreSMO;
import com.java110.core.context.IPageData;
import com.java110.core.log.LoggerFactory;
import com.java110.entity.assetImport.ImportOwnerRoomDto;
import com.java110.entity.assetImport.ImportResourceStoreDto;
import com.java110.entity.component.ComponentValidateResult;
import com.java110.intf.community.IImportOwnerRoomInnerServiceSMO;
import com.java110.utils.util.Assert;
import com.java110.utils.util.ImportExcelUtils;
import com.java110.utils.util.StringUtil;
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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service("importResourceStoreSMOImpl")
public class ImportResourceStoreSMOImpl extends DefaultAbstractComponentSMO implements IImportResourceStoreSMO {

    private final static Logger logger = LoggerFactory.getLogger(ImportResourceStoreSMOImpl.class);

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
            List<ImportResourceStoreDto> resourceStoreDtos = new ArrayList<ImportResourceStoreDto>();
            //????????????
            getResourceStores(workbook, resourceStoreDtos, result);

            //??????????????????
            validateRoomInfo(resourceStoreDtos);

            // ????????????
            return dealExcelData(pd, resourceStoreDtos, result);
        } catch (Exception e) {
            logger.error("???????????? ", e);
            return new ResponseEntity<String>("????????????????????????????????????????????????" + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    /**
     * ??????????????????
     *
     * @param resourceStoreDtos
     */
    private void validateRoomInfo(List<ImportResourceStoreDto> resourceStoreDtos) {
        ImportOwnerRoomDto importOwnerRoomDto = null;
        ImportOwnerRoomDto tmpImportOwnerRoomDto = null;
        boolean hasOwnerType = false;

    }

    /**
     * ????????????????????????
     *
     * @param workbook
     * @param resourceStoreDtos
     */
    private void getResourceStores(Workbook workbook, List<ImportResourceStoreDto> resourceStoreDtos, ComponentValidateResult result) throws ParseException {
        Sheet sheet = null;
        sheet = ImportExcelUtils.getSheet(workbook, "????????????");
        List<Object[]> oList = ImportExcelUtils.listFromSheet(sheet);
        ImportResourceStoreDto resourceStoreDto = null;
        for (int osIndex = 0; osIndex < oList.size(); osIndex++) {

            Object[] os = oList.get(osIndex);

            if (osIndex == 0 || osIndex == 1) { // ???????????? ???????????? ????????????
                continue;
            }
            if (os == null || StringUtil.isNullOrNone(os[0])) {
                continue;
            }
            Assert.hasValue(os[1], (osIndex + 1) + "???????????????????????????");
            Assert.hasValue(os[2], (osIndex + 1) + "???????????????????????????");
            Assert.hasValue(os[3], (osIndex + 1) + "???????????????????????????");
            Assert.hasValue(os[4], (osIndex + 1) + "???????????????????????????");
            Assert.hasValue(os[5], (osIndex + 1) + "???????????????????????????");
            Assert.hasValue(os[6], (osIndex + 1) + "???????????????????????????????????????");
            Assert.hasValue(os[7], (osIndex + 1) + "???????????????????????????????????????");
            Assert.hasValue(os[8], (osIndex + 1) + "?????????????????????????????????");
            Assert.hasValue(os[9], (osIndex + 1) + "???????????????????????????");
            Assert.hasValue(os[10], (osIndex + 1) + "?????????????????????????????????");
            resourceStoreDto = new ImportResourceStoreDto();
            resourceStoreDto.setCommunityId(result.getCommunityId());
            resourceStoreDto.setResName(os[1].toString());
            resourceStoreDto.setResCode(os[2].toString());
            resourceStoreDto.setPrice(os[3].toString());
            resourceStoreDto.setStock(os[4].toString());
            resourceStoreDto.setUnitCode(os[5].toString());
            resourceStoreDto.setOutLowPrice(os[6].toString());
            resourceStoreDto.setOutHighPrice(os[7].toString());
            resourceStoreDto.setWarningStock(os[8].toString());
            resourceStoreDto.setRstName(os[9].toString());
            resourceStoreDto.setIsFixed(os[10].toString());

            if (os.length > 11) {
                resourceStoreDto.setRemark(os[11].toString());
            }
            resourceStoreDtos.add(resourceStoreDto);

        }
    }


    /**
     * ??????????????????
     */
    private ResponseEntity<String> dealExcelData(IPageData pd, List<ImportResourceStoreDto> resourceStoreDtos, ComponentValidateResult result) {
        ResponseEntity<String> responseEntity = null;

        List<ImportResourceStoreDto> tmpImportResourceStoreDtos = new ArrayList<>();
        int flag = 0;

        JSONObject paramIn = JSONObject.parseObject(pd.getReqData());

        int successCount = 0;
        JSONObject data = new JSONObject();
        data.put("userId", result.getUserId());
        data.put("userName", result.getUserName());
        data.put("storeId", result.getStoreId());
        data.put("communityId", result.getCommunityId());
        data.put("shId", paramIn.getString("shId"));

        try {
            for (int roomIndex = 0; roomIndex < resourceStoreDtos.size(); roomIndex++) {
                tmpImportResourceStoreDtos.add(resourceStoreDtos.get(roomIndex));
                if (tmpImportResourceStoreDtos.size() > DEFAULT_ROWS) {
                    flag = saveResourceStore(pd, tmpImportResourceStoreDtos, data);
                    tmpImportResourceStoreDtos = new ArrayList<>();

                    successCount += flag;
                }
            }

            if (tmpImportResourceStoreDtos.size() > 0) {
                flag = saveResourceStore(pd, tmpImportResourceStoreDtos, data);

                successCount += flag;
            }
        } catch (Exception e) {
            logger.error("????????????", e);
            //????????????????????????
            throw e;
        }

        return ResultVo.createResponseEntity("????????????:" + tmpImportResourceStoreDtos.size() + ";???????????????" + successCount + ";???????????????" + (tmpImportResourceStoreDtos.size() - successCount));
    }

    /**
     * ????????????
     *
     * @param pd
     * @param tmpImportResourceStoreDtos
     */
    private int saveResourceStore(IPageData pd, List<ImportResourceStoreDto> tmpImportResourceStoreDtos, JSONObject data) {

        JSONArray importRoomFees = JSONArray.parseArray(JSONObject.toJSONString(tmpImportResourceStoreDtos));
        data.put("importResourceStoreDtos", importRoomFees);

        String apiUrl = "resourceStore.importResourceStore";

        ResponseEntity<String> responseEntity = this.callCenterService(restTemplate, pd, data.toJSONString(), apiUrl, HttpMethod.POST);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new IllegalArgumentException(responseEntity.getBody());
        }

        JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
        if (ResultVo.CODE_OK != paramOut.getInteger("code")) {
            throw new IllegalArgumentException(paramOut.getString("msg"));
        }

        return paramOut.getIntValue("data");

    }

    //??????Excel????????????
    public static Date DoubleToDate(Double dVal) {
        Date tDate = new Date();
        long localOffset = tDate.getTimezoneOffset() * 60000; //?????????????????? 1900/1/1 ??? 1970/1/1 ??? 25569 ???
        tDate.setTime((long) ((dVal - 25569) * 24 * 3600 * 1000 + localOffset));
        return tDate;
    }
}
