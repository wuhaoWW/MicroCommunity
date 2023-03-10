package com.java110.fee.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.DataFlowContext;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.applyRoomDiscount.ApplyRoomDiscountDto;
import com.java110.dto.applyRoomDiscountType.ApplyRoomDiscountTypeDto;
import com.java110.dto.fee.FeeDetailDto;
import com.java110.dto.feeDiscount.FeeDiscountRuleDto;
import com.java110.fee.bmo.account.IUpdateAccountBMO;
import com.java110.fee.bmo.applyRoomDiscount.IAuditApplyRoomDiscountBMO;
import com.java110.fee.bmo.applyRoomDiscount.IDeleteApplyRoomDiscountBMO;
import com.java110.fee.bmo.applyRoomDiscount.IGetApplyRoomDiscountBMO;
import com.java110.fee.bmo.applyRoomDiscount.ISaveApplyRoomDiscountBMO;
import com.java110.fee.bmo.applyRoomDiscount.IUpdateApplyRoomDiscountBMO;
import com.java110.fee.bmo.applyRoomDiscountType.IDeleteApplyRoomDiscountTypeBMO;
import com.java110.fee.bmo.applyRoomDiscountType.IGetApplyRoomDiscountTypeBMO;
import com.java110.fee.bmo.applyRoomDiscountType.ISaveApplyRoomDiscountTypeBMO;
import com.java110.fee.bmo.applyRoomDiscountType.IUpdateApplyRoomDiscountTypeBMO;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.fee.IApplyRoomDiscountInnerServiceSMO;
import com.java110.intf.fee.IFeeDetailInnerServiceSMO;
import com.java110.intf.fee.IFeeDiscountRuleInnerServiceSMO;
import com.java110.po.applyRoomDiscount.ApplyRoomDiscountPo;
import com.java110.po.applyRoomDiscountType.ApplyRoomDiscountTypePo;
import com.java110.po.file.FileRelPo;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping(value = "/applyRoomDiscount")
public class ApplyRoomDiscountApi {

    private static final String SPEC_RATE = "89002020980013"; // ?????????

    @Autowired
    private ISaveApplyRoomDiscountBMO saveApplyRoomDiscountBMOImpl;

    @Autowired
    private IUpdateApplyRoomDiscountBMO updateApplyRoomDiscountBMOImpl;

    @Autowired
    private IDeleteApplyRoomDiscountBMO deleteApplyRoomDiscountBMOImpl;

    @Autowired
    private IGetApplyRoomDiscountBMO getApplyRoomDiscountBMOImpl;

    @Autowired
    private IAuditApplyRoomDiscountBMO auditApplyRoomDiscountBMOImpl;

    @Autowired
    private ISaveApplyRoomDiscountTypeBMO saveApplyRoomDiscountTypeBMOImpl;

    @Autowired
    private IUpdateApplyRoomDiscountTypeBMO updateApplyRoomDiscountTypeBMOImpl;

    @Autowired
    private IDeleteApplyRoomDiscountTypeBMO deleteApplyRoomDiscountTypeBMOImpl;

    @Autowired
    private IGetApplyRoomDiscountTypeBMO getApplyRoomDiscountTypeBMOImpl;

    @Autowired
    private IApplyRoomDiscountInnerServiceSMO applyRoomDiscountInnerServiceSMOImpl;

    @Autowired
    private IFeeDiscountRuleInnerServiceSMO feeDiscountRuleInnerServiceSMOImpl;

    @Autowired
    private IFeeDetailInnerServiceSMO feeDetailInnerServiceSMOImpl;

    @Autowired
    private IUpdateAccountBMO updateAccountBMOImpl;

    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    /**
     * ????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /applyRoomDiscount/saveApplyRoomDiscount
     * @path /app/applyRoomDiscount/saveApplyRoomDiscount
     */
    @RequestMapping(value = "/saveApplyRoomDiscount", method = RequestMethod.POST)
    public ResponseEntity<String> saveApplyRoomDiscount(@RequestBody JSONObject reqJson) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Assert.hasKeyAndValue(reqJson, "communityId", "????????????????????????communityId");
        Assert.hasKeyAndValue(reqJson, "roomId", "????????????????????????roomId");
        Assert.hasKeyAndValue(reqJson, "roomName", "????????????????????????roomName");
        Assert.hasKeyAndValue(reqJson, "startTime", "????????????????????????startTime");
        Assert.hasKeyAndValue(reqJson, "endTime", "????????????????????????endTime");
        Assert.hasKeyAndValue(reqJson, "applyType", "????????????????????????applyType");
        if (!StringUtil.isEmpty(reqJson.getString("startTime"))) {
            String startTime = reqJson.getString("startTime") + " 00:00:00";
            reqJson.put("startTime", startTime);
        }
        if (!StringUtil.isEmpty(reqJson.getString("endTime"))) {
            String endTime = reqJson.getString("endTime") + " 23:59:59";
            reqJson.put("endTime", endTime);
        }
        ApplyRoomDiscountPo applyRoomDiscountPo = BeanConvertUtil.covertBean(reqJson, ApplyRoomDiscountPo.class);
        ApplyRoomDiscountDto applyRoomDiscountDto = new ApplyRoomDiscountDto();
        applyRoomDiscountDto.setCommunityId(applyRoomDiscountPo.getCommunityId());
        applyRoomDiscountDto.setRoomId(applyRoomDiscountPo.getRoomId());
        applyRoomDiscountDto.setFeeId(applyRoomDiscountPo.getFeeId());
        //????????????????????????????????????????????????????????????????????????
        List<ApplyRoomDiscountDto> applyRoomDiscountDtos = applyRoomDiscountInnerServiceSMOImpl.queryFirstApplyRoomDiscounts(applyRoomDiscountDto);
        //?????????????????????????????????
        Date startDate = simpleDateFormat.parse(applyRoomDiscountPo.getStartTime());
        if (applyRoomDiscountDtos.size() == 0) {
            //?????????????????????
            applyRoomDiscountPo.setInUse("0");
            applyRoomDiscountPo.setArdId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_ardId));
            applyRoomDiscountPo.setState(ApplyRoomDiscountDto.STATE_APPLY);
            saveFile(applyRoomDiscountPo);
            return saveApplyRoomDiscountBMOImpl.save(applyRoomDiscountPo);
        } else if (applyRoomDiscountDtos.size() > 0) {
            //??????????????????
            String endTime = applyRoomDiscountDtos.get(0).getEndTime();
            Date finishTime = simpleDateFormat.parse(endTime);
            if (startDate.getTime() - finishTime.getTime() >= 0) {
                //?????????????????????
                applyRoomDiscountPo.setInUse("0");
                applyRoomDiscountPo.setArdId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_ardId));
                applyRoomDiscountPo.setState(ApplyRoomDiscountDto.STATE_APPLY);
                saveFile(applyRoomDiscountPo);
                return saveApplyRoomDiscountBMOImpl.save(applyRoomDiscountPo);
            } else {
                throw new UnsupportedOperationException("??????????????????????????????????????????????????????????????????????????????????????????");
            }
        } else {
            throw new UnsupportedOperationException("????????????");
        }
    }

    /**
     * ????????????
     *
     * @param applyRoomDiscountPo
     */
    public void saveFile(ApplyRoomDiscountPo applyRoomDiscountPo) {
        //????????????
        List<String> photos = applyRoomDiscountPo.getPhotos();
        FileRelPo fileRelPo = new FileRelPo();
        fileRelPo.setFileRelId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_relId));
        fileRelPo.setObjId(applyRoomDiscountPo.getArdId());
        //table??????????????? ftp??????ftp????????????
        fileRelPo.setSaveWay("ftp");
        fileRelPo.setCreateTime(new Date());
        //????????????
        if (photos != null && photos.size() > 0) {
            //19000??????????????????
            fileRelPo.setRelTypeCd("19000");
            for (String photo : photos) {
                fileRelPo.setFileRealName(photo);
                fileRelPo.setFileSaveName(photo);
                fileRelInnerServiceSMOImpl.saveFileRel(fileRelPo);
            }
        }
    }

    /**
     * ????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /applyRoomDiscount/updateApplyRoomDiscount
     * @path /app/applyRoomDiscount/updateApplyRoomDiscount
     */
    @RequestMapping(value = "/updateApplyRoomDiscount", method = RequestMethod.POST)
    public ResponseEntity<String> updateApplyRoomDiscount(@RequestBody JSONObject reqJson, @RequestHeader(value = "user-id") String userId) {
        Assert.hasKeyAndValue(reqJson, "communityId", "????????????????????????communityId");
        Assert.hasKeyAndValue(reqJson, "state", "????????????????????????????????????");
        Assert.hasKeyAndValue(reqJson, "startTime", "????????????????????????????????????");
        Assert.hasKeyAndValue(reqJson, "endTime", "????????????????????????????????????");
        Assert.hasKeyAndValue(reqJson, "checkRemark", "????????????????????????????????????");
        Assert.hasKeyAndValue(reqJson, "ardId", "ardId????????????");
        ApplyRoomDiscountDto applyRoomDiscountDto = new ApplyRoomDiscountDto();
        applyRoomDiscountDto.setArdId(reqJson.getString("ardId"));
        //??????????????????????????????
        List<ApplyRoomDiscountDto> applyRoomDiscountDtos = applyRoomDiscountInnerServiceSMOImpl.queryApplyRoomDiscounts(applyRoomDiscountDto);
        Assert.listOnlyOne(applyRoomDiscountDtos, "?????????????????????????????????");
        //??????????????????????????????
        String state = applyRoomDiscountDtos.get(0).getState();
        if (!StringUtil.isEmpty(state) && !state.equals("1")) {
            throw new IllegalArgumentException("???????????????????????????????????????????????????");
        }
        reqJson.put("checkUserId", userId);
        ApplyRoomDiscountPo applyRoomDiscountPo = BeanConvertUtil.covertBean(reqJson, ApplyRoomDiscountPo.class);
        return updateApplyRoomDiscountBMOImpl.update(applyRoomDiscountPo);
    }

    /**
     * ??????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /applyRoomDiscount/editApplyRoomDiscount
     * @path /app/applyRoomDiscount/editApplyRoomDiscount
     */
    @RequestMapping(value = "/editApplyRoomDiscount", method = RequestMethod.POST)
    public ResponseEntity<String> editApplyRoomDiscount(@RequestBody JSONObject reqJson, @RequestHeader(value = "user-id") String userId) {
        Assert.hasKeyAndValue(reqJson, "communityId", "????????????????????????communityId");
        Assert.hasKeyAndValue(reqJson, "state", "????????????????????????????????????");
        Assert.hasKeyAndValue(reqJson, "startTime", "????????????????????????????????????");
        Assert.hasKeyAndValue(reqJson, "endTime", "????????????????????????????????????");
        Assert.hasKeyAndValue(reqJson, "ardId", "ardId????????????");
        ApplyRoomDiscountPo applyRoomDiscountPo = BeanConvertUtil.covertBean(reqJson, ApplyRoomDiscountPo.class);
        return updateApplyRoomDiscountBMOImpl.update(applyRoomDiscountPo);
    }

    /**
     * ????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /applyRoomDiscount/updateReviewApplyRoomDiscount
     * @path /app/applyRoomDiscount/updateReviewApplyRoomDiscount
     */
    @RequestMapping(value = "/updateReviewApplyRoomDiscount", method = RequestMethod.POST)
    @Java110Transactional
    public ResponseEntity<String> updateReviewApplyRoomDiscount(@RequestBody JSONObject reqJson, @RequestHeader(value = "user-id") String userId, DataFlowContext dataFlowContext) {
        Assert.hasKeyAndValue(reqJson, "communityId", "????????????????????????communityId");
        Assert.hasKeyAndValue(reqJson, "state", "????????????????????????????????????");
        Assert.hasKeyAndValue(reqJson, "startTime", "????????????????????????????????????");
        Assert.hasKeyAndValue(reqJson, "endTime", "????????????????????????????????????");
        Assert.hasKeyAndValue(reqJson, "reviewRemark", "????????????????????????????????????");
        Assert.hasKeyAndValue(reqJson, "ardId", "ardId????????????");
        ApplyRoomDiscountDto applyRoomDiscountDto = new ApplyRoomDiscountDto();
        applyRoomDiscountDto.setArdId(reqJson.getString("ardId"));
        //??????????????????????????????
        List<ApplyRoomDiscountDto> applyRoomDiscountDtos = applyRoomDiscountInnerServiceSMOImpl.queryApplyRoomDiscounts(applyRoomDiscountDto);
        Assert.listOnlyOne(applyRoomDiscountDtos, "?????????????????????????????????");
        //??????????????????????????????
        String state = applyRoomDiscountDtos.get(0).getState();
        String stateNow = reqJson.getString("state");
        String returnWay = reqJson.getString("returnWay");
        if (!StringUtil.isEmpty(state) && state.equals("4")) {
            throw new IllegalArgumentException("???????????????????????????????????????????????????");
        }
        if (reqJson.containsKey("selectedFees") && !StringUtil.isEmpty(reqJson.getString("selectedFees")) && stateNow.equals("4") && "1002".equals(returnWay)) {
            //?????????????????????????????????
            String selectedFees = reqJson.getString("selectedFees");
            JSONArray feeDetailIds = JSON.parseArray(selectedFees);
            //????????????
            JSONArray discounts = reqJson.getJSONArray("discounts");
            BigDecimal cashBackAmount = new BigDecimal("0.00");//???????????????
            for (int i = 0; i < discounts.size(); i++) {
                JSONObject discountObject = discounts.getJSONObject(i);
                if (!reqJson.getString("discountId").equals(discountObject.getString("discountId"))) {
                    continue;
                }
                JSONArray feeDiscountSpecs = discountObject.getJSONArray("feeDiscountSpecs");
                //????????????id
                String ruleId = discounts.getJSONObject(i).getString("ruleId");
                FeeDiscountRuleDto feeDiscountRuleDto = new FeeDiscountRuleDto();
                feeDiscountRuleDto.setRuleId(ruleId);
                List<FeeDiscountRuleDto> feeDiscountRuleDtos = feeDiscountRuleInnerServiceSMOImpl.queryFeeDiscountRules(feeDiscountRuleDto);
                Assert.listOnlyOne(feeDiscountRuleDtos, "???????????????????????????");
                //??????????????????(1: ??????  2:??????  3:?????????  4:???????????????  5:???????????????)
                String discountSmallType = feeDiscountRuleDtos.get(0).getDiscountSmallType();
                //????????????
                //String specValue = feeDiscountSpecs.getJSONObject(1).getString("specValue");
                String specValue = getRateSpecValueByFeeDiscountSpecs(feeDiscountSpecs);
                if (!StringUtil.isEmpty(discountSmallType) && (discountSmallType.equals("1") || discountSmallType.equals("4"))) { //??????
                    for (int index = 0; index < feeDetailIds.size(); index++) {
                        String feeDetailId = String.valueOf(feeDetailIds.get(index));
                        FeeDetailDto feeDetailDto = new FeeDetailDto();
                        feeDetailDto.setDetailId(feeDetailId);
                        List<FeeDetailDto> feeDetailDtos = feeDetailInnerServiceSMOImpl.queryFeeDetails(feeDetailDto);
                        Assert.listOnlyOne(feeDetailDtos, "??????????????????????????????");

                        BigDecimal receivedAmount = new BigDecimal(feeDetailDtos.get(0).getReceivedAmount());//??????????????????
                        BigDecimal spec = new BigDecimal(specValue);//??????
                        //??????????????????????????????
                        BigDecimal money = receivedAmount.multiply(spec);
                        cashBackAmount = cashBackAmount.add(receivedAmount.subtract(money)); //?????????????????????
                    }
                } else if (!StringUtil.isEmpty(discountSmallType) && (discountSmallType.equals("2") || discountSmallType.equals("5"))) { //??????
                    for (int index = 0; index < feeDetailIds.size(); index++) {
                        String feeDetailId = String.valueOf(feeDetailIds.get(index));
                        FeeDetailDto feeDetailDto = new FeeDetailDto();
                        feeDetailDto.setDetailId(feeDetailId);
                        List<FeeDetailDto> feeDetailDtos = feeDetailInnerServiceSMOImpl.queryFeeDetails(feeDetailDto);
                        Assert.listOnlyOne(feeDetailDtos, "??????????????????????????????");

                        BigDecimal spec = new BigDecimal(specValue);//????????????
                        cashBackAmount = cashBackAmount.add(spec); //?????????????????????
                    }
                }
                DecimalFormat df = new DecimalFormat("0.00");
                reqJson.put("cashBackAmount", df.format(cashBackAmount));
                //??????????????????
                JSONArray businesses = new JSONArray();
                updateAccountBMOImpl.cashBackAccount(reqJson, dataFlowContext, businesses);
                reqJson.put("inUse", 1);
                reqJson.put("returnAmount", df.format(cashBackAmount));
            }
        } else {
            reqJson.put("inUse", 0);
        }
        reqJson.put("reviewUserId", userId);
        ApplyRoomDiscountPo applyRoomDiscountPo = BeanConvertUtil.covertBean(reqJson, ApplyRoomDiscountPo.class);
        return updateApplyRoomDiscountBMOImpl.update(applyRoomDiscountPo);
    }

    /**
     * 89002020980001	102020001	??????
     * 89002020980002	102020001	?????????
     * 89002020980003	102020002	??????
     * 89002020980004	102020002	????????????
     * 89002020980005	102020003	?????????
     * 89002020980006	102020004	?????????
     * 89002020980007	102020005	??????
     * 89002020980008	102020005	?????????
     * 89002020980009	102020005	????????????
     * 89002020980010	102020006	??????
     * 89002020980011	102020006	????????????
     * 89002020980012	102020007	??????
     * 89002020980013	102020007	?????????
     *
     * @param feeDiscountSpecs
     * @return
     */
    private String getRateSpecValueByFeeDiscountSpecs(JSONArray feeDiscountSpecs) {

        for (int specIndex = 0; specIndex < feeDiscountSpecs.size(); specIndex++) {
            if (SPEC_RATE.equals(feeDiscountSpecs.getJSONObject(specIndex).getString("specId"))) {
                return feeDiscountSpecs.getJSONObject(specIndex).getString("specValue");
            }
            if ("89002020980002".equals(feeDiscountSpecs.getJSONObject(specIndex).getString("specId"))) {
                return feeDiscountSpecs.getJSONObject(specIndex).getString("specValue");
            }
            if ("89002020980004".equals(feeDiscountSpecs.getJSONObject(specIndex).getString("specId"))) {
                return feeDiscountSpecs.getJSONObject(specIndex).getString("specValue");
            }
            if ("89002020980008".equals(feeDiscountSpecs.getJSONObject(specIndex).getString("specId"))) {
                return feeDiscountSpecs.getJSONObject(specIndex).getString("specValue");
            }
            if ("89002020980011".equals(feeDiscountSpecs.getJSONObject(specIndex).getString("specId"))) {
                return feeDiscountSpecs.getJSONObject(specIndex).getString("specValue");
            }
        }

        throw new IllegalArgumentException("????????? ????????????");
    }

    /**
     * ????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /applyRoomDiscount/auditApplyRoomDiscount
     * @path /app/applyRoomDiscount/auditApplyRoomDiscount
     */
    @RequestMapping(value = "/auditApplyRoomDiscount", method = RequestMethod.POST)
    public ResponseEntity<String> auditApplyRoomDiscount(@RequestBody JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "communityId", "????????????????????????????????????");
        Assert.hasKeyAndValue(reqJson, "state", "????????????????????????????????????");
        Assert.hasKeyAndValue(reqJson, "reviewRemark", "????????????????????????????????????");
        Assert.hasKeyAndValue(reqJson, "ardId", "ardId????????????");
        ApplyRoomDiscountPo applyRoomDiscountPo = BeanConvertUtil.covertBean(reqJson, ApplyRoomDiscountPo.class);
        return auditApplyRoomDiscountBMOImpl.audit(applyRoomDiscountPo);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /applyRoomDiscount/deleteApplyRoomDiscount
     * @path /app/applyRoomDiscount/deleteApplyRoomDiscount
     */
    @RequestMapping(value = "/deleteApplyRoomDiscount", method = RequestMethod.POST)
    public ResponseEntity<String> deleteApplyRoomDiscount(@RequestBody JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "communityId", "??????ID????????????");
        Assert.hasKeyAndValue(reqJson, "ardId", "ardId????????????");
        ApplyRoomDiscountPo applyRoomDiscountPo = BeanConvertUtil.covertBean(reqJson, ApplyRoomDiscountPo.class);
        return deleteApplyRoomDiscountBMOImpl.delete(applyRoomDiscountPo);
    }

    /**
     * ??????????????????
     *
     * @param communityId ??????ID
     * @return
     * @serviceCode /applyRoomDiscount/queryApplyRoomDiscount
     * @path /app/applyRoomDiscount/queryApplyRoomDiscount
     */
    @RequestMapping(value = "/queryApplyRoomDiscount", method = RequestMethod.GET)
    public ResponseEntity<String> queryApplyRoomDiscount(@RequestParam(value = "communityId") String communityId,
                                                         @RequestParam(value = "ardId", required = false) String ardId,
                                                         @RequestParam(value = "roomName", required = false) String roomName,
                                                         @RequestParam(value = "roomId", required = false) String roomId,
                                                         @RequestParam(value = "state", required = false) String state,
                                                         @RequestParam(value = "applyType", required = false) String applyType,
                                                         @RequestParam(value = "page") int page,
                                                         @RequestParam(value = "row") int row) {
        ApplyRoomDiscountDto applyRoomDiscountDto = new ApplyRoomDiscountDto();
        applyRoomDiscountDto.setArdId(ardId);
        applyRoomDiscountDto.setPage(page);
        applyRoomDiscountDto.setRow(row);
        applyRoomDiscountDto.setCommunityId(communityId);
        applyRoomDiscountDto.setRoomName(roomName);
        applyRoomDiscountDto.setRoomId(roomId);
        applyRoomDiscountDto.setState(state);
        applyRoomDiscountDto.setApplyType(applyType);
        return getApplyRoomDiscountBMOImpl.get(applyRoomDiscountDto);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /applyRoomDiscount/saveApplyRoomDiscountType
     * @path /app/applyRoomDiscount/saveApplyRoomDiscountType
     */
    @RequestMapping(value = "/saveApplyRoomDiscountType", method = RequestMethod.POST)
    public ResponseEntity<String> saveApplyRoomDiscountType(@RequestBody JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "communityId", "????????????????????????communityId");
        Assert.hasKeyAndValue(reqJson, "typeName", "????????????????????????typeName");
        ApplyRoomDiscountTypePo applyRoomDiscountTypePo = BeanConvertUtil.covertBean(reqJson, ApplyRoomDiscountTypePo.class);
        return saveApplyRoomDiscountTypeBMOImpl.save(applyRoomDiscountTypePo);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /applyRoomDiscount/updateApplyRoomDiscountType
     * @path /app/applyRoomDiscount/updateApplyRoomDiscountType
     */
    @RequestMapping(value = "/updateApplyRoomDiscountType", method = RequestMethod.POST)
    public ResponseEntity<String> updateApplyRoomDiscountType(@RequestBody JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "communityId", "????????????????????????communityId");
        Assert.hasKeyAndValue(reqJson, "typeName", "????????????????????????typeName");
        Assert.hasKeyAndValue(reqJson, "applyType", "applyType????????????");
        ApplyRoomDiscountTypePo applyRoomDiscountTypePo = BeanConvertUtil.covertBean(reqJson, ApplyRoomDiscountTypePo.class);
        return updateApplyRoomDiscountTypeBMOImpl.update(applyRoomDiscountTypePo);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /applyRoomDiscount/deleteApplyRoomDiscountType
     * @path /app/applyRoomDiscount/deleteApplyRoomDiscountType
     */
    @RequestMapping(value = "/deleteApplyRoomDiscountType", method = RequestMethod.POST)
    public ResponseEntity<String> deleteApplyRoomDiscountType(@RequestBody JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "communityId", "??????ID????????????");
        Assert.hasKeyAndValue(reqJson, "applyType", "applyType????????????");
        ApplyRoomDiscountTypePo applyRoomDiscountTypePo = BeanConvertUtil.covertBean(reqJson, ApplyRoomDiscountTypePo.class);
        return deleteApplyRoomDiscountTypeBMOImpl.delete(applyRoomDiscountTypePo);
    }

    /**
     * ????????????????????????
     *
     * @param communityId ??????ID
     * @return
     * @serviceCode /applyRoomDiscount/queryApplyRoomDiscountType
     * @path /app/applyRoomDiscount/queryApplyRoomDiscountType
     */
    @RequestMapping(value = "/queryApplyRoomDiscountType", method = RequestMethod.GET)
    public ResponseEntity<String> queryApplyRoomDiscountType(@RequestParam(value = "communityId") String communityId,
                                                             @RequestParam(value = "applyType", required = false) String applyType,
                                                             @RequestParam(value = "typeName", required = false) String typeName,
                                                             @RequestParam(value = "page") int page,
                                                             @RequestParam(value = "row") int row) {
        ApplyRoomDiscountTypeDto applyRoomDiscountTypeDto = new ApplyRoomDiscountTypeDto();
        applyRoomDiscountTypeDto.setPage(page);
        applyRoomDiscountTypeDto.setRow(row);
        applyRoomDiscountTypeDto.setCommunityId(communityId);
        applyRoomDiscountTypeDto.setApplyType(applyType);
        applyRoomDiscountTypeDto.setTypeName(typeName);
        return getApplyRoomDiscountTypeBMOImpl.get(applyRoomDiscountTypeDto);
    }
}
