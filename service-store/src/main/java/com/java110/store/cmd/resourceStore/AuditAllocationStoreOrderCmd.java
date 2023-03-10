package com.java110.store.cmd.resourceStore;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.allocationStorehouse.AllocationStorehouseDto;
import com.java110.dto.allocationStorehouseApply.AllocationStorehouseApplyDto;
import com.java110.dto.resourceStore.ResourceStoreDto;
import com.java110.intf.common.IAllocationStorehouseUserInnerServiceSMO;
import com.java110.intf.store.*;
import com.java110.po.allocationStorehouseApply.AllocationStorehouseApplyPo;
import com.java110.po.purchase.ResourceStorePo;
import com.java110.po.resourceStoreTimes.ResourceStoreTimesPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;

@Java110Cmd(serviceCode = "resourceStore.auditAllocationStoreOrder")
public class AuditAllocationStoreOrderCmd extends Cmd {

    @Autowired
    private IAllocationStorehouseUserInnerServiceSMO allocationStorehouseUserInnerServiceSMOImpl;

    @Autowired
    private IAllocationStorehouseInnerServiceSMO allocationStorehouseInnerServiceSMOImpl;

    @Autowired
    private IAllocationStorehouseApplyInnerServiceSMO allocationStorehouseApplyInnerServiceSMOImpl;

    @Autowired
    private IResourceStoreInnerServiceSMO resourceStoreInnerServiceSMOImpl;

    @Autowired
    private IResourceStoreV1InnerServiceSMO resourceStoreV1InnerServiceSMOImpl;

    @Autowired
    private IAllocationStorehouseApplyV1InnerServiceSMO allocationStorehouseApplyV1InnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "applyId", "?????????????????????");
        Assert.hasKeyAndValue(reqJson, "taskId", "????????????????????????ID");
        Assert.hasKeyAndValue(reqJson, "state", "??????????????????????????????");
        Assert.hasKeyAndValue(reqJson, "remark", "????????????????????????");
    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        int flag = 0;
        AllocationStorehouseApplyDto allocationStorehouseDto = new AllocationStorehouseApplyDto();
        allocationStorehouseDto.setTaskId(reqJson.getString("taskId"));
        allocationStorehouseDto.setApplyId(reqJson.getString("applyId"));
        allocationStorehouseDto.setStoreId(reqJson.getString("storeId"));
        allocationStorehouseDto.setAuditCode(reqJson.getString("state"));
        allocationStorehouseDto.setAuditMessage(reqJson.getString("remark"));
        allocationStorehouseDto.setCurrentUserId(reqJson.getString("userId"));
        allocationStorehouseDto.setNoticeState(reqJson.getString("noticeState"));
        AllocationStorehouseApplyDto tmpAllocationStorehouseDto = new AllocationStorehouseApplyDto();
        tmpAllocationStorehouseDto.setApplyId(reqJson.getString("applyId"));
        tmpAllocationStorehouseDto.setStoreId(reqJson.getString("storeId"));
        List<AllocationStorehouseApplyDto> allocationStorehouseDtos = allocationStorehouseApplyInnerServiceSMOImpl.queryAllocationStorehouseApplys(tmpAllocationStorehouseDto);
        Assert.listOnlyOne(allocationStorehouseDtos, "???????????????????????????");
        allocationStorehouseDto.setStartUserId(allocationStorehouseDtos.get(0).getStartUserId());

        boolean isLastTask = allocationStorehouseUserInnerServiceSMOImpl.completeTask(allocationStorehouseDto);
        ResponseEntity<String> responseEntity = new ResponseEntity<String>("??????", HttpStatus.OK);
        if (isLastTask) {
            updateAllocationStorehouse(reqJson, context);
        } else if (reqJson.getString("state").equals("1100")) {  //??????????????????????????????????????????  1100????????????
            String procure = reqJson.getString("procure");
            AllocationStorehouseApplyPo allocationStorehouseApplyPo = new AllocationStorehouseApplyPo();
            allocationStorehouseApplyPo.setApplyId(allocationStorehouseDtos.get(0).getApplyId());
            allocationStorehouseApplyPo.setState(AllocationStorehouseDto.STATE_AUDIT);
            if (!StringUtil.isEmpty(procure) && procure.equals("true")) {
                allocationStorehouseApplyPo.setState(AllocationStorehouseDto.STATE_REVIEWED);
            }
            flag = allocationStorehouseApplyV1InnerServiceSMOImpl.updateAllocationStorehouseApply(allocationStorehouseApplyPo);
            if (flag < 1) {
                throw new CmdException("????????????");
            }
        } else if (reqJson.getString("state").equals("1200")) {  //??????????????????????????????????????????  1200????????????
            revokeAllocationStorehouse(reqJson);
        }
    }

    /**
     * @param paramInJson ???????????????????????????
     * @return ?????????????????????????????????
     */
    private void updateAllocationStorehouse(JSONObject paramInJson, ICmdDataFlowContext context) {

        AllocationStorehouseApplyDto tmpAllocationStorehouseApplyDto = new AllocationStorehouseApplyDto();
        tmpAllocationStorehouseApplyDto.setApplyId(paramInJson.getString("applyId"));
        tmpAllocationStorehouseApplyDto.setStoreId(paramInJson.getString("storeId"));
        List<AllocationStorehouseApplyDto> allocationStorehouseApplyDtos = allocationStorehouseApplyInnerServiceSMOImpl.queryAllocationStorehouseApplys(tmpAllocationStorehouseApplyDto);

        Assert.listOnlyOne(allocationStorehouseApplyDtos, "???????????????????????????????????????" + tmpAllocationStorehouseApplyDto.getApplyId());

        JSONObject businessComplaint = new JSONObject();
        businessComplaint.putAll(BeanConvertUtil.beanCovertMap(allocationStorehouseApplyDtos.get(0)));
        businessComplaint.put("state", AllocationStorehouseDto.STATE_SUCCESS);
        AllocationStorehouseApplyPo allocationStorehouseApplyPo = BeanConvertUtil.covertBean(businessComplaint, AllocationStorehouseApplyPo.class);
        int flag = 0;
        if (allocationStorehouseApplyDtos.get(0).getState().equals("1201") || allocationStorehouseApplyDtos.get(0).getState().equals("1204")) {
            flag = allocationStorehouseApplyV1InnerServiceSMOImpl.updateAllocationStorehouseApply(allocationStorehouseApplyPo);
            if (flag < 1) {
                throw new CmdException("????????????");
            }
            //????????????
            AllocationStorehouseDto tmpAllocationStorehouseDto = new AllocationStorehouseDto();
            tmpAllocationStorehouseDto.setApplyId(paramInJson.getString("applyId"));
            tmpAllocationStorehouseDto.setStoreId(paramInJson.getString("storeId"));
            List<AllocationStorehouseDto> allocationStorehouseDtos = allocationStorehouseInnerServiceSMOImpl.queryAllocationStorehouses(tmpAllocationStorehouseDto);

            for (AllocationStorehouseDto allocationStorehouseDto : allocationStorehouseDtos) {
                //?????????????????????????????????
                ResourceStoreDto resourceStoreDto = new ResourceStoreDto();
                resourceStoreDto.setShId(allocationStorehouseDto.getShIdz());
                resourceStoreDto.setResCode(allocationStorehouseDto.getResCode());
                List<ResourceStoreDto> resourceStoreDtos = resourceStoreInnerServiceSMOImpl.queryResourceStores(resourceStoreDto);
                ResourceStorePo resourceStorePo = new ResourceStorePo();

                if (resourceStoreDtos != null && resourceStoreDtos.size() == 1) {
                    //??????????????????????????????
                    ResourceStoreDto originalResourceStoreDto = new ResourceStoreDto();
                    originalResourceStoreDto.setShId(allocationStorehouseDto.getShIda());
                    originalResourceStoreDto.setResId(allocationStorehouseDto.getResId());
                    List<ResourceStoreDto> originalResourceStoreDtos = resourceStoreInnerServiceSMOImpl.queryResourceStores(originalResourceStoreDto);
                    Assert.listOnlyOne(originalResourceStoreDtos, "????????????????????????");
                    ResourceStoreDto resourceStoreDto1 = resourceStoreDtos.get(0);
                    //????????????
                    BigDecimal stock1 = new BigDecimal(resourceStoreDtos.get(0).getStock());
                    BigDecimal stock2 = new BigDecimal(allocationStorehouseDto.getStock());
                    resourceStoreDto1.setStock(stock1.add(stock2).toString());
                    //????????????????????????
                    if (StringUtil.isEmpty(resourceStoreDtos.get(0).getMiniStock())) {
                        throw new IllegalArgumentException("?????????????????????????????????");
                    }
                    BigDecimal miniStock = new BigDecimal(resourceStoreDtos.get(0).getMiniStock()); //???????????????????????????
                    if (StringUtil.isEmpty(resourceStoreDtos.get(0).getMiniUnitStock())) {
                        throw new IllegalArgumentException("???????????????????????????????????????");
                    }
                    BigDecimal miniUnitStock = new BigDecimal(resourceStoreDtos.get(0).getMiniUnitStock()); //??????????????????????????????
                    BigDecimal nowMiniStock = stock2.multiply(miniUnitStock); //?????????????????????????????????
                    BigDecimal newMiniStock = miniStock.add(nowMiniStock);
                    resourceStoreDto1.setMiniStock(String.valueOf(newMiniStock));
                    resourceStorePo = BeanConvertUtil.covertBean(resourceStoreDto1, ResourceStorePo.class);
                    resourceStorePo.setAveragePrice(originalResourceStoreDtos.get(0).getAveragePrice());
                    resourceStorePo.setOutLowPrice(originalResourceStoreDtos.get(0).getOutLowPrice());
                    resourceStorePo.setOutHighPrice(originalResourceStoreDtos.get(0).getOutHighPrice());
                    resourceStorePo.setRstId(originalResourceStoreDtos.get(0).getRstId());
                    resourceStorePo.setParentRstId(originalResourceStoreDtos.get(0).getParentRstId());
                    resourceStorePo.setRssId(originalResourceStoreDtos.get(0).getRssId());
                    if (!StringUtil.isEmpty(originalResourceStoreDtos.get(0).getMiniUnitCode()) && !StringUtil.isEmpty(originalResourceStoreDtos.get(0).getMiniUnitStock())) {
                        resourceStorePo.setMiniUnitCode(originalResourceStoreDtos.get(0).getMiniUnitCode());
                        resourceStorePo.setMiniUnitStock(originalResourceStoreDtos.get(0).getMiniUnitStock());
                    }
                    flag = resourceStoreV1InnerServiceSMOImpl.updateResourceStore(resourceStorePo);
                    if (flag < 1) {
                        throw new CmdException("????????????");
                    }
                } else if (resourceStoreDtos != null && resourceStoreDtos.size() > 1) {
                    ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "??????????????????????????????????????????????????????");
                    context.setResponseEntity(responseEntity);
                    return;
                } else {
                    //?????????????????????????????????
                    resourceStoreDto = new ResourceStoreDto();
                    resourceStoreDto.setShId(allocationStorehouseDto.getShIda());
                    resourceStoreDto.setResId(allocationStorehouseDto.getResId());
                    resourceStoreDtos = resourceStoreInnerServiceSMOImpl.queryResourceStores(resourceStoreDto);
                    Assert.listOnlyOne(resourceStoreDtos, "????????????????????????");
                    resourceStorePo = BeanConvertUtil.covertBean(allocationStorehouseDto, resourceStorePo);
                    resourceStorePo.setResId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_resId));
                    resourceStorePo.setStock(allocationStorehouseDto.getStock());
                    //??????????????????????????????
                    if (StringUtil.isEmpty(resourceStoreDtos.get(0).getMiniUnitStock())) {
                        throw new IllegalArgumentException("???????????????????????????????????????");
                    }
                    BigDecimal miniUnitStock = new BigDecimal(resourceStoreDtos.get(0).getMiniUnitStock());
                    BigDecimal stock = new BigDecimal(allocationStorehouseDto.getStock());
                    //??????????????????????????????
                    BigDecimal miniStock = stock.multiply(miniUnitStock);
                    resourceStorePo.setMiniStock(String.valueOf(miniStock));
                    resourceStorePo.setShId(allocationStorehouseDto.getShIdz());
                    resourceStorePo.setPrice(resourceStoreDtos.get(0).getPrice());
                    resourceStorePo.setDescription("");
                    resourceStorePo.setUnitCode(resourceStoreDtos.get(0).getUnitCode());
                    resourceStorePo.setOutLowPrice(resourceStoreDtos.get(0).getOutLowPrice());
                    resourceStorePo.setOutHighPrice(resourceStoreDtos.get(0).getOutHighPrice());
                    resourceStorePo.setShowMobile(resourceStoreDtos.get(0).getShowMobile());
                    resourceStorePo.setWarningStock(resourceStoreDtos.get(0).getWarningStock());
                    resourceStorePo.setAveragePrice(resourceStoreDtos.get(0).getAveragePrice());
                    resourceStorePo.setRstId(resourceStoreDtos.get(0).getRstId());
                    resourceStorePo.setParentRstId(resourceStoreDtos.get(0).getParentRstId());
                    resourceStorePo.setRssId(resourceStoreDtos.get(0).getRssId());
                    resourceStorePo.setMiniUnitCode(resourceStoreDtos.get(0).getMiniUnitCode());
                    resourceStorePo.setMiniUnitStock(resourceStoreDtos.get(0).getMiniUnitStock());
                    flag = resourceStoreV1InnerServiceSMOImpl.saveResourceStore(resourceStorePo);
                    if (flag < 1) {
                        throw new CmdException("????????????");
                    }
                }
            }
        } else if (allocationStorehouseApplyDtos.get(0).getState().equals("1203")) {
            allocationStorehouseApplyPo.setState(AllocationStorehouseDto.STATE_FAIL);
            flag = allocationStorehouseApplyV1InnerServiceSMOImpl.updateAllocationStorehouseApply(allocationStorehouseApplyPo);
            if (flag < 1) {
                throw new CmdException("????????????");
            }
        }
    }


    /**
     * @param paramInJson ???????????????????????????
     * @return ?????????????????????????????????
     */
    private void revokeAllocationStorehouse(JSONObject paramInJson) {

        AllocationStorehouseApplyDto tmpAllocationStorehouseApplyDto = new AllocationStorehouseApplyDto();
        tmpAllocationStorehouseApplyDto.setApplyId(paramInJson.getString("applyId"));
        tmpAllocationStorehouseApplyDto.setStoreId(paramInJson.getString("storeId"));
        List<AllocationStorehouseApplyDto> allocationStorehouseApplyDtos = allocationStorehouseApplyInnerServiceSMOImpl.queryAllocationStorehouseApplys(tmpAllocationStorehouseApplyDto);
        Assert.listOnlyOne(allocationStorehouseApplyDtos, "???????????????????????????????????????" + tmpAllocationStorehouseApplyDto.getApplyId());

        JSONObject businessComplaint = new JSONObject();
        businessComplaint.putAll(BeanConvertUtil.beanCovertMap(allocationStorehouseApplyDtos.get(0)));
        businessComplaint.put("state", AllocationStorehouseDto.STATE_FAIL);
        AllocationStorehouseApplyPo allocationStorehouseApplyPo = BeanConvertUtil.covertBean(businessComplaint, AllocationStorehouseApplyPo.class);
        int flag = allocationStorehouseApplyV1InnerServiceSMOImpl.updateAllocationStorehouseApply(allocationStorehouseApplyPo);
        if (flag < 1) {
            throw new CmdException("????????????");
        }        //??????????????????
        AllocationStorehouseDto tmpAllocationStorehouseDto = new AllocationStorehouseDto();
        tmpAllocationStorehouseDto.setApplyId(paramInJson.getString("applyId"));
        tmpAllocationStorehouseDto.setStoreId(paramInJson.getString("storeId"));
        List<AllocationStorehouseDto> allocationStorehouseDtos = allocationStorehouseInnerServiceSMOImpl.queryAllocationStorehouses(tmpAllocationStorehouseDto);

        for (AllocationStorehouseDto allocationStorehouseDto : allocationStorehouseDtos) {
            //?????????????????????????????????
            ResourceStoreDto resourceStoreDto = new ResourceStoreDto();
            resourceStoreDto.setResId(allocationStorehouseDto.getResId());
            List<ResourceStoreDto> resourceStoreDtos = resourceStoreInnerServiceSMOImpl.queryResourceStores(resourceStoreDto);
            Assert.listOnlyOne(resourceStoreDtos, "????????????????????????");
            ResourceStorePo resourceStorePo = new ResourceStorePo();
            resourceStorePo.setResId(allocationStorehouseDto.getResId());
            //??????????????????
            BigDecimal resourceStoreStock = new BigDecimal(resourceStoreDtos.get(0).getStock());
            //?????????????????????
            BigDecimal storehouseStock = new BigDecimal(allocationStorehouseDto.getStock());
            //????????????
            BigDecimal stock = resourceStoreStock.add(storehouseStock);
            resourceStorePo.setStock(String.valueOf(stock));
            //????????????????????????
            if (StringUtil.isEmpty(resourceStoreDtos.get(0).getMiniStock())) {
                throw new IllegalArgumentException("?????????????????????????????????");
            }
            BigDecimal miniStock = new BigDecimal(resourceStoreDtos.get(0).getMiniStock()); //????????????????????????????????????
            if (StringUtil.isEmpty(resourceStoreDtos.get(0).getMiniUnitStock())) {
                throw new IllegalArgumentException("???????????????????????????????????????");
            }
            BigDecimal miniUnitStock = new BigDecimal(resourceStoreDtos.get(0).getMiniUnitStock()); //??????????????????????????????
            BigDecimal stock1 = new BigDecimal(allocationStorehouseDto.getStock()); //??????????????????????????????
            BigDecimal nowMiniStock = stock1.multiply(miniUnitStock); //?????????????????????????????????
            BigDecimal newMiniStock = miniStock.add(nowMiniStock);
            resourceStorePo.setMiniStock(String.valueOf(newMiniStock));
            flag = resourceStoreV1InnerServiceSMOImpl.updateResourceStore(resourceStorePo);
            if (flag < 1) {
                throw new CmdException("????????????");
            }
        }
    }
}
