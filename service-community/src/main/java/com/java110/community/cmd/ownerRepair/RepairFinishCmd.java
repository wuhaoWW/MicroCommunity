package com.java110.community.cmd.ownerRepair;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.fee.FeeAttrDto;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.file.FileDto;
import com.java110.dto.file.FileRelDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.dto.repair.RepairDto;
import com.java110.dto.repair.RepairUserDto;
import com.java110.dto.userStorehouse.UserStorehouseDto;
import com.java110.intf.common.IFileInnerServiceSMO;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.community.*;
import com.java110.intf.fee.IFeeAttrInnerServiceSMO;
import com.java110.intf.fee.IFeeConfigInnerServiceSMO;
import com.java110.intf.fee.IPayFeeV1InnerServiceSMO;
import com.java110.intf.store.IResourceStoreUseRecordV1InnerServiceSMO;
import com.java110.intf.store.IUserStorehouseInnerServiceSMO;
import com.java110.intf.store.IUserStorehouseV1InnerServiceSMO;
import com.java110.intf.user.IOwnerInnerServiceSMO;
import com.java110.intf.user.IOwnerRoomRelInnerServiceSMO;
import com.java110.po.fee.FeeAttrPo;
import com.java110.po.fee.PayFeePo;
import com.java110.po.file.FileRelPo;
import com.java110.po.owner.RepairPoolPo;
import com.java110.po.owner.RepairUserPo;
import com.java110.po.purchase.ResourceStorePo;
import com.java110.po.resourceStoreUseRecord.ResourceStoreUseRecordPo;
import com.java110.po.userStorehouse.UserStorehousePo;
import com.java110.utils.constant.FeeTypeConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "ownerRepair.repairFinish")
public class RepairFinishCmd extends Cmd {

    @Autowired
    private IRepairUserInnerServiceSMO repairUserInnerServiceSMOImpl;

    @Autowired
    private IRepairInnerServiceSMO repairInnerServiceSMOImpl;

    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    @Autowired
    private IOwnerRoomRelInnerServiceSMO ownerRoomRelInnerServiceSMO;

    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMO;

    @Autowired
    private IResourceStoreServiceSMO resourceStoreServiceSMO;

    @Autowired
    private IUserStorehouseInnerServiceSMO userStorehouseInnerServiceSMO;

    @Autowired
    private IUserStorehouseV1InnerServiceSMO userStorehouseV1InnerServiceSMOImpl;

    @Autowired
    private IResourceStoreUseRecordV1InnerServiceSMO resourceStoreUseRecordV1InnerServiceSMOImpl;

    @Autowired
    private IRepairUserV1InnerServiceSMO repairUserV1InnerServiceSMOImpl;

    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    @Autowired
    private IRepairPoolV1InnerServiceSMO repairPoolV1InnerServiceSMOImpl;

    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    @Autowired
    private IFeeAttrInnerServiceSMO feeAttrInnerServiceSMOImpl;

    @Autowired
    private IFileInnerServiceSMO fileInnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "repairId", "????????????????????????");
        Assert.hasKeyAndValue(reqJson, "context", "?????????????????????");
        Assert.hasKeyAndValue(reqJson, "communityId", "?????????????????????");
//        Assert.hasKeyAndValue(reqJson, "amount", "???????????????");
        Assert.hasKeyAndValue(reqJson, "feeFlag", "?????????????????????");
    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        String userId = context.getReqHeaders().get("user-id");
        String userName = context.getReqHeaders().get("user-name");
        String publicArea = reqJson.getString("publicArea");
        int flag = 0;
        //??????????????????
        String repairChannel = reqJson.getString("repairChannel");
        //??????????????????
        String maintenanceType = reqJson.getString("maintenanceType");
        //??????????????????
        String choosedGoodsList = reqJson.getString("choosedGoodsList");
        //????????????????????????????????????????????????
        RepairUserDto repairUserDto = new RepairUserDto();
        repairUserDto.setRepairId(reqJson.getString("repairId"));
        repairUserDto.setCommunityId(reqJson.getString("communityId"));
        repairUserDto.setState(RepairUserDto.STATE_DOING);
        repairUserDto.setStaffId(userId);
        List<RepairUserDto> repairUserDtos = repairUserInnerServiceSMOImpl.queryRepairUsers(repairUserDto);
        if (repairUserDtos == null || repairUserDtos.size() != 1) {
            ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "???????????????????????????????????????");
            context.setResponseEntity(responseEntity);
            return;
        }
        JSONArray json = JSONArray.parseArray(choosedGoodsList);
        //??????
        String repairMaterial = "";
        //????????????(?????? * ?????? = ??????)
        String repairFee = "";
        if (json != null && json.size() > 0 && ("1001".equals(maintenanceType) || "1003".equals(maintenanceType))) {
            Object[] objects = json.toArray();
            //??????????????????
            for (int i = 0; i < objects.length; i++) {
                Object object = objects[i];
                JSONObject paramIn = JSONObject.parseObject(String.valueOf(object));
                String isCustom = paramIn.getString("isCustom");//???????????????
                String resId = paramIn.getString("resId");//??????????????????id
                String useNumber = paramIn.getString("useNumber");//??????????????????
                String outLowPrice = "0";//?????????
                String outHighPrice = "0"; //?????????
                List<ResourceStorePo> resourceStorePoList = new ArrayList<>();
                List<UserStorehouseDto> userStorehouseDtoList = new ArrayList<>();

                if (!StringUtil.isEmpty(resId)) {
                    //???????????????????????????
                    ResourceStorePo resourceStorePo = new ResourceStorePo();
                    resourceStorePo.setResId(resId);
                    resourceStorePoList = resourceStoreServiceSMO.getResourceStores(resourceStorePo);//????????????????????????
                    Assert.listOnlyOne(resourceStorePoList, "?????????????????????????????????????????????????????????");
                    outLowPrice = resourceStorePoList.get(0).getOutLowPrice(); //?????????
                    outHighPrice = resourceStorePoList.get(0).getOutHighPrice();//?????????
                }
                if (!StringUtil.isEmpty(useNumber) && !"0".equals(useNumber)
                        && (!StringUtil.isEmpty(isCustom) && isCustom.equals("false"))) {
                    String nowStock = "0";
                    //??????????????????????????????????????????
                    UserStorehouseDto userStorehouseDto = new UserStorehouseDto();
                    userStorehouseDto.setResId(resId);
                    userStorehouseDto.setUserId(userId);
                    //????????????????????????
                    userStorehouseDtoList = userStorehouseInnerServiceSMO.queryUserStorehouses(userStorehouseDto);
                    if (userStorehouseDtoList == null || userStorehouseDtoList.size() < 1) {
                        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "????????????" + userStorehouseDtoList.get(0).getResName() + "???????????????????????????????????????");
                        context.setResponseEntity(responseEntity);
                        return;
                    }
                    if (userStorehouseDtoList.size() == 1) {
                        //????????????????????????
                        nowStock = userStorehouseDtoList.get(0).getMiniStock();
                    }
                    if (Double.parseDouble(nowStock) < Double.parseDouble(useNumber)) {
                        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "????????????" + userStorehouseDtoList.get(0).getResName() + "???????????????????????????????????????");
                        context.setResponseEntity(responseEntity);
                        return;
                    }
                }
                if (maintenanceType.equals("1001") && (!StringUtil.isEmpty(isCustom) && isCustom.equals("false"))) {
                    Double price = Double.parseDouble(paramIn.getString("price")); //????????????
                    Double outLowPrices = Double.parseDouble(outLowPrice);//?????????
                    Double outHighPrices = Double.parseDouble(outHighPrice);//?????????
                    //????????????????????????????????????????????????
                    if (price < outLowPrices || price > outHighPrices) {
                        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "?????????????????????" + userStorehouseDtoList.get(0).getResName() + "????????????????????????????????????");
                        context.setResponseEntity(responseEntity);
                        return;
                    }
                }
            }
            //???????????????????????????????????????
            for (int i = 0; i < objects.length; i++) {
                Object object = objects[i];
                JSONObject paramIn = JSONObject.parseObject(String.valueOf(object));
                String isCustom = paramIn.getString("isCustom");//???????????????
                //??????????????????id
                String resId = paramIn.getString("resId");
                //??????????????????
                String useNumber = paramIn.getString("useNumber");
                //??????
                String totalPrice = "";
                //??????
                String repairMaterials = "";
                //??????
                Double unitPrice = 0.0;
                //??????
                Double useNumber_s = 0.0;
                //????????????
                String repair = "";
                DecimalFormat df = new DecimalFormat("0.00");
                List<ResourceStorePo> resourceStorePoList = new ArrayList<>();
                List<UserStorehouseDto> userStorehouseDtoList = new ArrayList<>();
                if (!StringUtil.isEmpty(paramIn.getString("price")) && !StringUtil.isEmpty(useNumber)) {
                    //???????????? ??????????????????
                    unitPrice = Double.parseDouble(paramIn.getString("price"));
                    //????????????
                    useNumber_s = Double.parseDouble(useNumber);
                    //????????????
                    totalPrice = df.format(unitPrice * useNumber_s);
                    //????????????
                    repair = df.format(unitPrice) + " * " + useNumber_s + " = " + totalPrice;
                }
                if (!StringUtil.isEmpty(resId)) {
                    ResourceStorePo resourceStorePo = new ResourceStorePo();
                    resourceStorePo.setResId(resId);
                    //????????????????????????
                    resourceStorePoList = resourceStoreServiceSMO.getResourceStores(resourceStorePo);
                    Assert.listOnlyOne(resourceStorePoList, "?????????????????????????????????????????????????????????");
                    //??????
                    repairMaterials = resourceStorePoList.get(0).getResName() + "*" + useNumber;
                } else {
                    //??????
                    repairMaterials = paramIn.getString("customGoodsName") + "*" + useNumber;
                }
                if (!StringUtil.isEmpty(useNumber) && !"0".equals(useNumber)
                        && (!StringUtil.isEmpty(isCustom) && isCustom.equals("false"))) {
                    String nowStock = "0";
                    //??????????????????????????????????????????
                    UserStorehouseDto userStorehouseDto = new UserStorehouseDto();
                    userStorehouseDto.setResId(resId);
                    userStorehouseDto.setUserId(userId);
                    //????????????????????????
                    userStorehouseDtoList = userStorehouseInnerServiceSMO.queryUserStorehouses(userStorehouseDto);
                    Assert.listOnlyOne(userStorehouseDtoList, "?????????????????????????????????????????????????????????");
                    if (userStorehouseDtoList.size() == 1) {
                        //??????????????????
                        nowStock = userStorehouseDtoList.get(0).getMiniStock();
                    }
                    //????????????
                    UserStorehousePo userStorehousePo = new UserStorehousePo();
                    //??????????????????????????????????????????
                    BigDecimal num1 = new BigDecimal(Double.parseDouble(nowStock));
                    BigDecimal num2 = new BigDecimal(Double.parseDouble(useNumber));
                    BigDecimal surplusStock = num1.subtract(num2);
                    //????????????????????????
                    double miniUnitStock = Double.parseDouble(userStorehouseDtoList.get(0).getMiniUnitStock());
                    //??????????????????
                    if (StringUtil.isEmpty(userStorehouseDtoList.get(0).getUnitCode())) {
                        throw new IllegalArgumentException("???????????????????????????");
                    }
                    String unitCode = userStorehouseDtoList.get(0).getUnitCode();
                    //??????????????????????????????
                    if (StringUtil.isEmpty(userStorehouseDtoList.get(0).getMiniUnitCode())) {
                        throw new IllegalArgumentException("???????????????????????????????????????");
                    }
                    String miniUnitCode = userStorehouseDtoList.get(0).getMiniUnitCode();
                    if (unitCode.equals(miniUnitCode)) { //??????????????????????????????????????????????????????????????????
                        BigDecimal num3 = new BigDecimal(miniUnitStock);
                        double newStock = surplusStock.divide(num3, 2, BigDecimal.ROUND_HALF_UP).doubleValue();
                        int remainingInventory = new Double(newStock).intValue();
                        userStorehousePo.setStock(String.valueOf(remainingInventory));
                    } else { //??????????????????????????????
                        BigDecimal num3 = new BigDecimal(miniUnitStock);
                        double newStock = surplusStock.divide(num3, 2, BigDecimal.ROUND_HALF_UP).doubleValue();
                        double ceil = Math.ceil(newStock);
                        int remainingInventory = new Double(ceil).intValue();
                        userStorehousePo.setStock(String.valueOf(remainingInventory));
                    }
                    if (useNumber.contains(".") || nowStock.contains(".")) { //??????????????????????????????????????????????????????????????????????????????????????????
                        userStorehousePo.setMiniStock(String.valueOf(surplusStock.doubleValue()));
                    } else { //?????????????????????????????????????????????????????????????????????????????????
                        userStorehousePo.setMiniStock(String.valueOf(surplusStock.intValue()));
                    }
                    userStorehousePo.setUsId(userStorehouseDtoList.get(0).getUsId());
                    userStorehousePo.setResId(resId);
                    userStorehousePo.setUserId(userId);
                    //????????????
                    flag = userStorehouseV1InnerServiceSMOImpl.updateUserStorehouse(userStorehousePo);
                    if (flag < 1) {
                        throw new CmdException("??????????????????");
                    }
                }
                //????????????????????????????????????
                ResourceStoreUseRecordPo resourceStoreUseRecordPo = new ResourceStoreUseRecordPo();
                resourceStoreUseRecordPo.setRsurId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_rsurId));
                resourceStoreUseRecordPo.setRepairId(reqJson.getString("repairId"));
                resourceStoreUseRecordPo.setResourceStoreName(paramIn.getString("resName"));
                if (!StringUtil.isEmpty(isCustom) && isCustom.equals("true")) {
                    resId = "666666";
                    resourceStoreUseRecordPo.setResourceStoreName(paramIn.getString("customGoodsName"));
                }
                resourceStoreUseRecordPo.setResId(resId);
                resourceStoreUseRecordPo.setCommunityId(reqJson.getString("communityId"));
                resourceStoreUseRecordPo.setStoreId(reqJson.getString("storeId"));
                resourceStoreUseRecordPo.setCreateUserId(reqJson.getString("userId"));
                resourceStoreUseRecordPo.setCreateUserName(reqJson.getString("userName"));
                resourceStoreUseRecordPo.setRemark(reqJson.getString("context"));
                resourceStoreUseRecordPo.setQuantity(useNumber);
                resourceStoreUseRecordPo.setState("2002"); //1001 ????????????   2002 ????????????   3003 ????????????
                //????????????
                if (maintenanceType.equals("1001")) {
                    resourceStoreUseRecordPo.setUnitPrice(paramIn.getString("price"));
                    flag = resourceStoreUseRecordV1InnerServiceSMOImpl.saveResourceStoreUseRecord(resourceStoreUseRecordPo);
                    if (flag < 1) {
                        throw new CmdException("????????????");
                    }
                } else if (maintenanceType.equals("1003")) {  //????????????
                    flag = resourceStoreUseRecordV1InnerServiceSMOImpl.saveResourceStoreUseRecord(resourceStoreUseRecordPo);
                    if (flag < 1) {
                        throw new CmdException("????????????");
                    }
                }
                if (!StringUtil.isEmpty(repairMaterials)) {
                    repairMaterial += repairMaterials + "???";
                }
                if (!StringUtil.isEmpty(repair)) {
                    repairFee += repair + "???";
                }
            }
        }
        // 1.0 ??????????????????
        RepairUserPo repairUserPo = new RepairUserPo();
        repairUserPo.setRuId(repairUserDtos.get(0).getRuId());
        repairUserPo.setEndTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        repairUserPo.setState(RepairUserDto.STATE_CLOSE);
        repairUserPo.setContext(reqJson.getString("context"));
        repairUserPo.setCommunityId(reqJson.getString("communityId"));
        flag = repairUserV1InnerServiceSMOImpl.updateRepairUserNew(repairUserPo);
        if (flag < 1) {
            throw new CmdException("??????????????????");
        }
        if ((!StringUtil.isEmpty(repairChannel) && "Z".equals(repairChannel))
                || (!StringUtil.isEmpty(maintenanceType) && "1001".equals(maintenanceType))) {  //??????????????????????????????????????????????????????????????????????????????
            //2.0 ???????????????????????????
            repairUserDto = new RepairUserDto();
            repairUserDto.setRepairId(reqJson.getString("repairId"));
            repairUserDto.setCommunityId(reqJson.getString("communityId"));
            repairUserDto.setRepairEvent(RepairUserDto.REPAIR_EVENT_START_USER);
            repairUserDtos = repairUserInnerServiceSMOImpl.queryRepairUsers(repairUserDto);
            if (repairUserDtos.size() != 1) {
                ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "???????????? ???????????????????????????");
                context.setResponseEntity(responseEntity);
                return;
            }
            repairUserPo = new RepairUserPo();
            repairUserPo.setRuId("-1");
            repairUserPo.setStartTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
            if ("1001".equals(maintenanceType)) { //??????????????????????????????(??????????????????????????????????????????????????????????????????)
                repairUserPo.setState(RepairUserDto.STATE_PAY_FEE);
                repairUserPo.setContext("?????????" + reqJson.getString("totalPrice") + "???");
            } else {
                repairUserPo.setState(RepairUserDto.STATE_EVALUATE);
                repairUserPo.setContext("?????????");
            }
            repairUserPo.setRepairId(reqJson.getString("repairId"));
            if ("Z".equals(repairChannel)) {  //?????????????????????????????????????????????
                repairUserPo.setStaffId(repairUserDtos.get(0).getStaffId());
                repairUserPo.setStaffName(repairUserDtos.get(0).getStaffName());
            } else { //???????????????????????????????????????
                RepairDto repairDto = new RepairDto();
                repairDto.setRepairId(reqJson.getString("repairId"));
                List<RepairDto> repairDtos = repairInnerServiceSMOImpl.queryRepairs(repairDto);
                if (repairDtos.size() != 1) {
                    ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "??????????????????????????????????????????");
                    context.setResponseEntity(responseEntity);
                    return;
                }
                //?????????????????????ID?????????ID
                String roomId = repairDtos.get(0).getRepairObjId();
                OwnerRoomRelDto ownerRoomRelDto = new OwnerRoomRelDto();
                ownerRoomRelDto.setRoomId(roomId);
                //???????????????????????????
                List<OwnerRoomRelDto> ownerRoomRelDtos = ownerRoomRelInnerServiceSMO.queryOwnerRoomRels(ownerRoomRelDto);
                if (ownerRoomRelDtos.size() != 1) {
                    ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "??????????????????????????????????????????");
                    context.setResponseEntity(responseEntity);
                    return;
                }
                //????????????id
                String ownerId = ownerRoomRelDtos.get(0).getOwnerId();
                OwnerDto ownerDto = new OwnerDto();
                ownerDto.setOwnerId(ownerId);
                //????????????id??????????????????
                List<OwnerDto> ownerDtos = ownerInnerServiceSMO.queryOwners(ownerDto);
                if (ownerDtos.size() != 1) {
                    ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "???????????????????????????");
                    context.setResponseEntity(responseEntity);
                    return;
                }
                //??????????????????
                String ownerName = ownerDtos.get(0).getName();
                repairUserPo.setStaffId(ownerId);
                repairUserPo.setStaffName(ownerName);
            }
            repairUserPo.setPreStaffId(repairUserDtos.get(0).getStaffId());
            repairUserPo.setPreStaffName(repairUserDtos.get(0).getStaffName());
            repairUserPo.setPreRuId(repairUserDtos.get(0).getRuId());
            repairUserPo.setRepairEvent(RepairUserDto.REPAIR_EVENT_PAY_USER);
            repairUserPo.setCommunityId(reqJson.getString("communityId"));
            flag = repairUserV1InnerServiceSMOImpl.saveRepairUserNew(repairUserPo);
            if (flag < 1) {
                throw new CmdException("??????????????????");
            }
        }
        //?????????????????????
        if (reqJson.containsKey("beforeRepairPhotos") && !StringUtils.isEmpty(reqJson.getString("beforeRepairPhotos"))) {
            JSONArray beforeRepairPhotos = reqJson.getJSONArray("beforeRepairPhotos");
            for (int _photoIndex = 0; _photoIndex < beforeRepairPhotos.size(); _photoIndex++) {
                String photo = beforeRepairPhotos.getJSONObject(_photoIndex).getString("photo");
                if(photo.length()> 512){
                    FileDto fileDto = new FileDto();
                    fileDto.setFileId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_file_id));
                    fileDto.setFileName(fileDto.getFileId());
                    fileDto.setContext(photo);
                    fileDto.setSuffix("jpeg");
                    fileDto.setCommunityId(reqJson.getString("communityId"));
                    photo = fileInnerServiceSMOImpl.saveFile(fileDto);
                }
                JSONObject businessUnit = new JSONObject();
                businessUnit.put("fileRelId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_fileRelId));
                businessUnit.put("relTypeCd", FileRelDto.BEFORE_REPAIR_PHOTOS);
                businessUnit.put("saveWay", "ftp");
                businessUnit.put("objId", reqJson.getString("repairId"));
                businessUnit.put("fileRealName", photo);
                businessUnit.put("fileSaveName", photo);
                FileRelPo fileRelPo = BeanConvertUtil.covertBean(businessUnit, FileRelPo.class);
                flag = fileRelInnerServiceSMOImpl.saveFileRel(fileRelPo);
                if (flag < 1) {
                    throw new CmdException("??????????????????");
                }
            }
        }
        //?????????????????????
        if (reqJson.containsKey("afterRepairPhotos") && !StringUtils.isEmpty(reqJson.getString("afterRepairPhotos"))) {
            JSONArray afterRepairPhotos = reqJson.getJSONArray("afterRepairPhotos");
            for (int _photoIndex = 0; _photoIndex < afterRepairPhotos.size(); _photoIndex++) {
                String photo = afterRepairPhotos.getJSONObject(_photoIndex).getString("photo");
                if(photo.length()> 512){
                    FileDto fileDto = new FileDto();
                    fileDto.setFileId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_file_id));
                    fileDto.setFileName(fileDto.getFileId());
                    fileDto.setContext(photo);
                    fileDto.setSuffix("jpeg");
                    fileDto.setCommunityId(reqJson.getString("communityId"));
                    photo = fileInnerServiceSMOImpl.saveFile(fileDto);
                }
                JSONObject businessUnit = new JSONObject();
                businessUnit.put("fileRelId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_fileRelId));
                businessUnit.put("relTypeCd", FileRelDto.AFTER_REPAIR_PHOTOS);
                businessUnit.put("saveWay", "ftp");
                businessUnit.put("objId", reqJson.getString("repairId"));
                businessUnit.put("fileRealName", photo);
                businessUnit.put("fileSaveName", photo);
                FileRelPo fileRelPo = BeanConvertUtil.covertBean(businessUnit, FileRelPo.class);
                flag = fileRelInnerServiceSMOImpl.saveFileRel(fileRelPo);
                if (flag < 1) {
                    throw new CmdException("??????????????????");
                }
            }
        }
        if ("F".equals(publicArea) && "1002".equals(reqJson.getString("maintenanceType"))) { //????????????????????????????????????????????????
            //??????r_repair_pool???maintenance_type????????????
            RepairPoolPo repairPoolPo = new RepairPoolPo();
            repairPoolPo.setRepairId(reqJson.getString("repairId"));
            repairPoolPo.setMaintenanceType(reqJson.getString("maintenanceType"));
            flag = repairPoolV1InnerServiceSMOImpl.updateRepairPoolNew(repairPoolPo);
            if (flag < 1) {
                throw new CmdException("????????????");
            }
            if ("Z".equals(repairChannel)) { //????????????????????????????????????????????????????????????????????????
                modifyBusinessRepairDispatch(reqJson, RepairDto.STATE_APPRAISE);
            } else { //?????????????????????????????????????????????????????????
                modifyBusinessRepairDispatch(reqJson, RepairDto.STATE_RETURN_VISIT);
            }
        } else if ("F".equals(publicArea) && "1001".equals(reqJson.getString("maintenanceType"))) { //????????????????????????????????????????????????
            //3.0 ??????????????????
            //?????????????????????
            FeeConfigDto feeConfigDto = new FeeConfigDto();
            feeConfigDto.setCommunityId(reqJson.getString("communityId"));
            feeConfigDto.setFeeTypeCd(FeeTypeConstant.FEE_TYPE_REPAIR);
            feeConfigDto.setIsDefault(FeeConfigDto.DEFAULT_FEE_CONFIG);
            List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);
            if (feeConfigDtos.size() != 1) {
                ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "??????????????????????????????????????????");
                context.setResponseEntity(responseEntity);
                return;
            }
            PayFeePo feePo = new PayFeePo();
            feePo.setAmount(String.valueOf(reqJson.getString("")));
            feePo.setCommunityId(reqJson.getString("communityId"));
            feePo.setConfigId(feeConfigDtos.get(0).getConfigId());
            feePo.setEndTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
            feePo.setFeeFlag(feeConfigDtos.get(0).getFeeFlag());
            feePo.setFeeId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_feeId));
            feePo.setFeeTypeCd(FeeTypeConstant.FEE_TYPE_REPAIR);
            feePo.setIncomeObjId(reqJson.getString("storeId"));
            feePo.setPayerObjType(FeeDto.PAYER_OBJ_TYPE_ROOM);
            feePo.setAmount(reqJson.getString("totalPrice"));
            feePo.setBatchId("-1");
            RepairDto repairDto = new RepairDto();
            repairDto.setCommunityId(reqJson.getString("communityId"));
            repairDto.setRepairId(reqJson.getString("repairId"));
            List<RepairDto> repairDtos = repairInnerServiceSMOImpl.queryRepairs(repairDto);
            if (repairDtos.size() != 1) {
                ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "?????????????????????????????????");
                context.setResponseEntity(responseEntity);
                return;
            }
            feePo.setPayerObjId(repairDtos.get(0).getRepairObjId());
            feePo.setStartTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
            feePo.setState(FeeDto.STATE_DOING);
            feePo.setUserId(userId);
            feePo.setbId("-1");

            flag = payFeeV1InnerServiceSMOImpl.savePayFee(feePo);
            if (flag < 1) {
                throw new CmdException("??????????????????");
            }
            FeeAttrPo feeAttrPo = new FeeAttrPo();
            feeAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
            feeAttrPo.setFeeId(feePo.getFeeId());
            feeAttrPo.setSpecCd(FeeAttrDto.SPEC_CD_REPAIR);
            feeAttrPo.setCommunityId(reqJson.getString("communityId"));
            feeAttrPo.setValue(reqJson.getString("repairId"));
            flag = feeAttrInnerServiceSMOImpl.saveFeeAttr(feeAttrPo);
            if (flag < 1) {
                throw new CmdException("??????????????????");
            }
            //??????r_repair_pool???maintenance_type????????????
            RepairPoolPo repairPoolPo = new RepairPoolPo();
            repairPoolPo.setRepairId(reqJson.getString("repairId"));
            //????????????
            repairPoolPo.setMaintenanceType(reqJson.getString("maintenanceType"));
            //??????
            repairPoolPo.setRepairMaterials(repairMaterial.substring(0, repairMaterial.length() - 1));
            //????????????
            repairPoolPo.setRepairFee(repairFee.substring(0, repairFee.length() - 1));
            //????????????
            repairPoolPo.setPayType(reqJson.getString("payType"));
            flag = repairPoolV1InnerServiceSMOImpl.updateRepairPoolNew(repairPoolPo);
            if (flag < 1) {
                throw new CmdException("????????????");
            }
            modifyBusinessRepairDispatch(reqJson, RepairDto.STATE_PAY);
        } else if ("T".equals(publicArea)) {  //?????????????????????
            //????????????????????????????????????????????????
            if ("1003".equals(maintenanceType)) {
                //??????r_repair_pool???maintenance_type????????????
                RepairPoolPo repairPoolPo = new RepairPoolPo();
                repairPoolPo.setRepairId(reqJson.getString("repairId"));
                //????????????
                repairPoolPo.setMaintenanceType(reqJson.getString("maintenanceType"));
                //??????
                repairPoolPo.setRepairMaterials(repairMaterial.substring(0, repairMaterial.length() - 1));
                flag = repairPoolV1InnerServiceSMOImpl.updateRepairPoolNew(repairPoolPo);
                if (flag < 1) {
                    throw new CmdException("????????????");
                }
            }

            if ("Z".equals(repairChannel)) { //????????????????????????????????????????????????????????????????????????
                modifyBusinessRepairDispatch(reqJson, RepairDto.STATE_APPRAISE);
            } else { //?????????????????????????????????????????????????????????
                modifyBusinessRepairDispatch(reqJson, RepairDto.STATE_RETURN_VISIT);
            }
        }
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_OK, ResultVo.MSG_OK);
        context.setResponseEntity(responseEntity);
    }

    public void modifyBusinessRepairDispatch(JSONObject paramInJson, String state) {
        //???????????????
        RepairDto repairDto = new RepairDto();
        repairDto.setRepairId(paramInJson.getString("repairId"));
        List<RepairDto> repairDtos = repairInnerServiceSMOImpl.queryRepairs(repairDto);
        Assert.isOne(repairDtos, "????????????????????????repairId=" + repairDto.getRepairId());
        JSONObject businessOwnerRepair = new JSONObject();
        businessOwnerRepair.putAll(BeanConvertUtil.beanCovertMap(repairDtos.get(0)));
        businessOwnerRepair.put("state", state);
        //?????? ????????????
        RepairPoolPo repairPoolPo = BeanConvertUtil.covertBean(businessOwnerRepair, RepairPoolPo.class);
        int flag = repairPoolV1InnerServiceSMOImpl.updateRepairPoolNew(repairPoolPo);
        if (flag < 1) {
            throw new CmdException("??????????????????");
        }
    }
}
