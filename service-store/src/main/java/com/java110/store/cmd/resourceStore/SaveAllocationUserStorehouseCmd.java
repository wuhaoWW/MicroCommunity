package com.java110.store.cmd.resourceStore;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.DataFlowContext;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.resourceStore.ResourceStoreDto;
import com.java110.dto.user.UserDto;
import com.java110.dto.userStorehouse.UserStorehouseDto;
import com.java110.intf.store.*;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.po.allocationUserStorehouse.AllocationUserStorehousePo;
import com.java110.po.resourceStoreUseRecord.ResourceStoreUseRecordPo;
import com.java110.po.userStorehouse.UserStorehousePo;
import com.java110.utils.constant.BusinessTypeConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

@Java110Cmd(serviceCode = "resourceStore.saveAllocationUserStorehouse")
public class SaveAllocationUserStorehouseCmd extends Cmd {

    @Autowired
    private IUserStorehouseInnerServiceSMO userStorehouseInnerServiceSMOImpl;

    @Autowired
    private IUserStorehouseV1InnerServiceSMO userStorehouseV1InnerServiceSMOImpl;

    @Autowired
    private IResourceStoreInnerServiceSMO resourceStoreInnerServiceSMOImpl;

    @Autowired
    private IResourceStoreUseRecordV1InnerServiceSMO resourceStoreUseRecordV1InnerServiceSMOImpl;

    @Autowired
    private IAllocationUserStorehouseV1InnerServiceSMO allocationUserStorehouseV1InnerServiceSMOImpl;

    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;



    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        //Assert.hasKeyAndValue(reqJson, "xxx", "xxx");
        //Assert.hasKeyAndValue(reqJson, "acceptUserId", "????????????????????????acceptUserId");
        Assert.hasKeyAndValue(reqJson, "storeId", "????????????????????????storeId");
    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        String userId = reqJson.getString("userId");
        if(StringUtil.isEmpty(userId)){
            userId = context.getReqHeaders().get("user-id");
        }
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        userDto.setPage(1);
        userDto.setRow(1);
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);

        Assert.listOnlyOne(userDtos,"???????????????");

        reqJson.put("userName",userDtos.get(0).getName());

        String acceptUserId = reqJson.getString("acceptUserId");
        if(!StringUtil.isEmpty(userId) && !StringUtil.isEmpty(acceptUserId) && acceptUserId.equals(userId)){
            ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_ERROR, "??????????????????????????????????????????????????????????????????");
            context.setResponseEntity(responseEntity);
            return;
        }

        addAllocationUserStorehouse(reqJson,context);
    }

    public void addAllocationUserStorehouse(JSONObject paramInJson, ICmdDataFlowContext dataFlowContext) {
        String resourceStores = paramInJson.getString("resourceStores");
        JSONArray json = JSONArray.parseArray(resourceStores);
        int flag1 = 0;
        if (json.size() > 0) {
            Object[] objects = json.toArray();
            String flag = paramInJson.getString("flag");
            if (!StringUtil.isEmpty(flag) && flag.equals("1")) { //??????
                for (int i = 0; i < objects.length; i++) {
                    Object object = objects[i];
                    JSONObject paramIn = JSONObject.parseObject(String.valueOf(object));
                    ResourceStoreUseRecordPo resourceStoreUseRecordPo = new ResourceStoreUseRecordPo();
                    resourceStoreUseRecordPo.setRsurId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_rsurId));
                    resourceStoreUseRecordPo.setRepairId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_repairId)); //????????????
                    resourceStoreUseRecordPo.setResId(paramIn.getString("resId")); //????????????id
                    resourceStoreUseRecordPo.setCommunityId(paramInJson.getString("communityId")); //??????id
                    resourceStoreUseRecordPo.setStoreId(paramInJson.getString("storeId")); //??????id
                    resourceStoreUseRecordPo.setQuantity(paramIn.getString("giveQuantity")); //????????????
                    //??????????????????id????????????????????????
                    ResourceStoreDto resourceStoreDto = new ResourceStoreDto();
                    resourceStoreDto.setResId(paramIn.getString("resId"));
                    List<ResourceStoreDto> resourceStoreDtos = resourceStoreInnerServiceSMOImpl.queryResourceStores(resourceStoreDto);
                    Assert.listOnlyOne(resourceStoreDtos, "???????????????????????????");
                    resourceStoreUseRecordPo.setUnitPrice(resourceStoreDtos.get(0).getPrice()); //??????????????????
                    resourceStoreUseRecordPo.setCreateUserId(paramInJson.getString("userId")); //?????????id
                    resourceStoreUseRecordPo.setCreateUserName(paramInJson.getString("userName")); //???????????????
                    resourceStoreUseRecordPo.setRemark(paramIn.getString("purchaseRemark")); //??????
                    resourceStoreUseRecordPo.setResourceStoreName(paramIn.getString("resName")); //????????????
                    resourceStoreUseRecordPo.setState(paramIn.getString("state")); //1001 ????????????   2002 ????????????   3003 ????????????
                    flag1 = resourceStoreUseRecordV1InnerServiceSMOImpl.saveResourceStoreUseRecord(resourceStoreUseRecordPo);
                    if(flag1 <1){
                        throw new CmdException("????????????");
                    }
                    //??????????????????
                    UserStorehouseDto userStorehouseDto = new UserStorehouseDto();
                    userStorehouseDto.setUserId(paramInJson.getString("userId"));
                    userStorehouseDto.setResId(paramIn.getString("resId"));
                    //????????????????????????
                    List<UserStorehouseDto> userStorehouseDtos = userStorehouseInnerServiceSMOImpl.queryUserStorehouses(userStorehouseDto);
                    Assert.listOnlyOne(userStorehouseDtos, "?????????????????????????????????");
                    //????????????????????????id
                    String usId = userStorehouseDtos.get(0).getUsId();
                    //??????????????????
                    if (StringUtil.isEmpty(userStorehouseDtos.get(0).getUnitCode())) {
                        throw new IllegalArgumentException("????????????????????????");
                    }
                    String unitCode = userStorehouseDtos.get(0).getUnitCode(); //????????????
                    //??????????????????????????????
                    if (StringUtil.isEmpty(userStorehouseDtos.get(0).getMiniUnitCode())) {
                        throw new IllegalArgumentException("????????????????????????????????????");
                    }
                    String miniUnitCode = userStorehouseDtos.get(0).getMiniUnitCode(); //????????????????????????
                    UserStorehousePo userStorehousePo = new UserStorehousePo();
                    userStorehousePo.setUsId(usId); //????????????id
                    //??????????????????????????????
                    String miniStock = userStorehouseDtos.get(0).getMiniStock();
                    //??????????????????????????????
                    String miniUnitStock = paramIn.getString("miniUnitStock");
                    //??????????????????
                    String giveQuantity = paramIn.getString("giveQuantity");
                    //?????????????????????????????????????????????????????????
                    BigDecimal num1 = new BigDecimal(miniStock);
                    BigDecimal num2 = new BigDecimal(giveQuantity);
                    BigDecimal quantity = num1.subtract(num2);
                    if (quantity.doubleValue() == 0.0) { //???????????????????????????0????????????????????????????????????????????????????????????0
                        userStorehousePo.setMiniStock("0");
                        userStorehousePo.setStock("0");
                    } else {
                        userStorehousePo.setMiniStock(String.valueOf(quantity)); //????????????????????????????????????????????????
                        BigDecimal reduceNum = num1.subtract(num2);
                        if (unitCode.equals(miniUnitCode)) { //??????????????????????????????????????????????????????????????????
                            userStorehousePo.setStock(String.valueOf(reduceNum));
                        } else { //?????????????????????????????????????????????????????????????????????
                            //????????????????????????????????????????????????????????????????????????????????????????????????????????????
                            BigDecimal num3 = new BigDecimal(miniUnitStock);
                            BigDecimal unitStock = reduceNum.divide(num3, 2, BigDecimal.ROUND_HALF_UP);
                            Integer stockNumber = (int) Math.ceil(unitStock.doubleValue());
                            userStorehousePo.setStock(String.valueOf(stockNumber)); //??????????????????????????????????????????
                        }
                    }
                    flag1 = userStorehouseV1InnerServiceSMOImpl.updateUserStorehouse(userStorehousePo);
                    if(flag1 <1){
                        throw new CmdException("????????????");
                    }
                }
            } else { //??????
                for (int i = 0; i < objects.length; i++) {
                    Object object = objects[i];
                    JSONObject paramIn = JSONObject.parseObject(String.valueOf(object));
                    String stock = paramIn.getString("stock");
                    //????????????????????????
                    String miniStock = paramIn.getString("miniStock");
                    //??????????????????????????????
                    String miniUnitStock = paramIn.getString("miniUnitStock");
                    //??????????????????
                    String giveQuantity = paramIn.getString("giveQuantity");
                    //????????????id
                    String resId = paramIn.getString("resId");
                    //????????????id
                    String resCode = paramIn.getString("resCode");
                    //??????????????????
                    String resName = paramIn.getString("resName");
                    //??????????????????id
                    String userId = paramInJson.getString("userId");
                    //????????????????????????id
                    String acceptUserId = paramInJson.getString("acceptUserId");
                    //??????????????????????????????
                    String acceptUserName = paramInJson.getString("acceptUserName");
                    //????????????id
                    String storeId = paramInJson.getString("storeId");
                    JSONObject allocationUserStorehouseJson = new JSONObject();
                    allocationUserStorehouseJson.put("ausId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_ausId));
                    allocationUserStorehouseJson.put("resId", resId);
                    allocationUserStorehouseJson.put("resCode", resCode);
                    allocationUserStorehouseJson.put("resName", resName);
                    allocationUserStorehouseJson.put("storeId", storeId);
                    allocationUserStorehouseJson.put("stock", stock);
                    allocationUserStorehouseJson.put("giveQuantity", giveQuantity);
                    allocationUserStorehouseJson.put("startUserId", userId);
                    allocationUserStorehouseJson.put("startUserName", paramInJson.getString("userName"));
                    allocationUserStorehouseJson.put("acceptUserId", acceptUserId);
                    allocationUserStorehouseJson.put("acceptUserName", acceptUserName);
                    allocationUserStorehouseJson.put("createTime", new Date());
                    allocationUserStorehouseJson.put("remark", paramInJson.getString("description"));
                    AllocationUserStorehousePo allocationUserStorehousePo = BeanConvertUtil.covertBean(allocationUserStorehouseJson, AllocationUserStorehousePo.class);
                    flag1 = allocationUserStorehouseV1InnerServiceSMOImpl.saveAllocationUserStorehouse(allocationUserStorehousePo);
                    if(flag1 <1){
                        throw new CmdException("????????????");
                    }
                    UserStorehouseDto userStorehouseDto = new UserStorehouseDto();
                    userStorehouseDto.setUserId(userId);
                    userStorehouseDto.setResId(resId);
                    List<UserStorehouseDto> userStorehouseDtos = userStorehouseInnerServiceSMOImpl.queryUserStorehouses(userStorehouseDto);
                    Assert.listOnlyOne(userStorehouseDtos, "?????????????????????????????????");
                    //????????????????????????id
                    String usId = userStorehouseDtos.get(0).getUsId();
                    //??????????????????
                    if (StringUtil.isEmpty(userStorehouseDtos.get(0).getUnitCode())) {
                        throw new IllegalArgumentException("????????????????????????");
                    }
                    String unitCode = userStorehouseDtos.get(0).getUnitCode();
                    //??????????????????????????????
                    if (StringUtil.isEmpty(userStorehouseDtos.get(0).getMiniUnitCode())) {
                        throw new IllegalArgumentException("????????????????????????????????????");
                    }
                    String miniUnitCode = userStorehouseDtos.get(0).getMiniUnitCode();
                    UserStorehousePo userStorehousePo = new UserStorehousePo();
                    userStorehousePo.setUsId(usId);
                    //???????????????????????????????????????
                    BigDecimal num1 = new BigDecimal(miniStock);
                    BigDecimal num2 = new BigDecimal(giveQuantity);
                    BigDecimal quantity = num1.subtract(num2);
                    if (quantity.doubleValue() == 0.0) {
                        userStorehousePo.setMiniStock("0");
                        userStorehousePo.setStock("0");
                    } else {
                        userStorehousePo.setMiniStock(String.valueOf(quantity));
                        BigDecimal reduceNum = num1.subtract(num2);
                        if (unitCode.equals(miniUnitCode)) { //??????????????????????????????????????????????????????????????????
                            userStorehousePo.setStock(String.valueOf(reduceNum));
                        } else { //?????????????????????????????????????????????????????????????????????
                            //????????????????????????????????????????????????????????????????????????????????????????????????????????????
                            BigDecimal num3 = new BigDecimal(miniUnitStock);
                            BigDecimal unitStock = reduceNum.divide(num3, 2, BigDecimal.ROUND_HALF_UP);
                            Integer stockNumber = (int) Math.ceil(unitStock.doubleValue());
                            userStorehousePo.setStock(String.valueOf(stockNumber));
                        }
                    }
                    //???????????????????????????
                    flag1 = userStorehouseV1InnerServiceSMOImpl.updateUserStorehouse(userStorehousePo);
                    if(flag1 <1){
                        throw new CmdException("????????????");
                    }
                    UserStorehouseDto userStorehouse = new UserStorehouseDto();
                    userStorehouse.setUserId(acceptUserId);
                    userStorehouse.setResCode(resCode);
                    //??????????????????????????????????????????
                    List<UserStorehouseDto> userStorehouses = userStorehouseInnerServiceSMOImpl.queryUserStorehouses(userStorehouse);
                    if (userStorehouses != null && userStorehouses.size() == 1) {
                        UserStorehousePo userStorePo = new UserStorehousePo();
                        //???????????????????????????????????????
                        BigDecimal num4 = new BigDecimal(userStorehouses.get(0).getMiniStock());
                        BigDecimal num5 = new BigDecimal(giveQuantity);
                        BigDecimal addNum = num4.add(num5);
                        BigDecimal acceptMiniStock = num4.add(num5);
                        userStorePo.setMiniStock(String.valueOf(acceptMiniStock));
                        //??????????????????
                        if (StringUtil.isEmpty(userStorehouses.get(0).getUnitCode())) {
                            throw new IllegalArgumentException("????????????????????????");
                        }
                        String unitCode1 = userStorehouses.get(0).getUnitCode();
                        //??????????????????????????????
                        if (StringUtil.isEmpty(userStorehouses.get(0).getMiniUnitCode())) {
                            throw new IllegalArgumentException("????????????????????????????????????");
                        }
                        String miniUnitCode1 = userStorehouses.get(0).getMiniUnitCode();
                        //?????????????????????????????????
                        BigDecimal num6 = new BigDecimal(miniUnitStock);
                        BigDecimal unitStock = addNum.divide(num6, 2, BigDecimal.ROUND_HALF_UP);
                        if (unitCode1.equals(miniUnitCode1)) { //????????????????????????????????????????????????????????????????????????
                            //???????????????????????????????????????????????????????????????????????????????????????
                            userStorePo.setStock(String.valueOf(acceptMiniStock));
                        } else { //?????????????????????????????????????????????????????????????????????
                            Integer stockNumber = (int) Math.ceil(unitStock.doubleValue());
                            userStorePo.setStock(String.valueOf(stockNumber));
                        }
                        userStorePo.setUsId(userStorehouses.get(0).getUsId());
                        //?????????????????????????????????
                        flag1 = userStorehouseV1InnerServiceSMOImpl.updateUserStorehouse(userStorePo);
                        if(flag1 <1){
                            throw new CmdException("????????????");
                        }
                    } else if (userStorehouses != null && userStorehouses.size() > 1) {
                        throw new IllegalArgumentException("?????????????????????????????????");
                    } else {
                        //???????????????????????????
                        BigDecimal num7 = new BigDecimal(giveQuantity);
                        BigDecimal num8 = new BigDecimal(miniUnitStock);
                        BigDecimal unitStock = num7.divide(num8, 2, BigDecimal.ROUND_HALF_UP);
                        UserStorehousePo userStorePo = new UserStorehousePo();
                        userStorePo.setUsId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_usId));
                        userStorePo.setResId(resId);
                        userStorePo.setResName(resName);
                        userStorePo.setStoreId(storeId);
                        userStorePo.setResCode(resCode);
                        if (unitCode.equals(miniUnitCode)) { //????????????????????????????????????????????????????????????????????????
                            userStorePo.setStock(String.valueOf(num7));
                        } else { //?????????????????????????????????????????????????????????????????????
                            Integer stockNumber = (int) Math.ceil(unitStock.doubleValue());
                            userStorePo.setStock(String.valueOf(stockNumber));
                        }
                        userStorePo.setMiniStock(giveQuantity);
                        userStorePo.setUserId(acceptUserId);
                        //??????????????????????????????????????????
                        flag1 = userStorehouseV1InnerServiceSMOImpl.saveUserStorehouse(userStorePo);
                        if(flag1 <1){
                            throw new CmdException("????????????");
                        }                    }
                }
            }
        }
    }
}
