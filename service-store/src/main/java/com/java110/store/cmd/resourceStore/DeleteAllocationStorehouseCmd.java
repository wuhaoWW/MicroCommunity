package com.java110.store.cmd.resourceStore;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.allocationStorehouse.AllocationStorehouseDto;
import com.java110.dto.allocationStorehouseApply.AllocationStorehouseApplyDto;
import com.java110.dto.purchaseApply.PurchaseApplyDto;
import com.java110.intf.community.IResourceStoreServiceSMO;
import com.java110.intf.store.*;
import com.java110.po.allocationStorehouse.AllocationStorehousePo;
import com.java110.po.allocationStorehouseApply.AllocationStorehouseApplyPo;
import com.java110.po.purchase.ResourceStorePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;

@Java110Cmd(serviceCode = "resourceStore.deleteAllocationStorehouse")
public class DeleteAllocationStorehouseCmd extends Cmd {

    @Autowired
    private IAllocationStorehouseApplyInnerServiceSMO allocationStorehouseApplyInnerServiceSMOImpl;

    @Autowired
    private IAllocationStorehouseInnerServiceSMO allocationStorehouseInnerServiceSMOImpl;

    @Autowired
    private IAllocationStorehouseV1InnerServiceSMO allocationStorehouseV1InnerServiceSMOImpl;

    @Autowired
    private IResourceStoreServiceSMO resourceStoreServiceSMOImpl;

    @Autowired
    private IResourceStoreV1InnerServiceSMO resourceStoreV1InnerServiceSMOImpl;

    @Autowired
    private IPurchaseApplyInnerServiceSMO purchaseApplyInnerServiceSMOImpl;

    @Autowired
    private IAllocationStorehouseApplyV1InnerServiceSMO allocationStorehouseApplyV1InnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        Assert.hasKeyAndValue(reqJson, "applyId", "????????????????????????");
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        //????????????id
        String applyId = reqJson.getString("applyId");
        AllocationStorehouseApplyDto allocationStorehouseApplyDto = new AllocationStorehouseApplyDto();
        allocationStorehouseApplyDto.setApplyId(applyId);
        //????????????????????????
        List<AllocationStorehouseApplyDto> allocationStorehouseApplyDtos = allocationStorehouseApplyInnerServiceSMOImpl.queryAllocationStorehouseApplys(allocationStorehouseApplyDto);
        Assert.listOnlyOne(allocationStorehouseApplyDtos, "??????????????????????????????");
        //????????????
        String state = allocationStorehouseApplyDtos.get(0).getState();
        if (!StringUtil.isEmpty(state) && state.equals("1200")) { //1200????????????????????????
            deleteAllocationStorehouse(reqJson);
        } else {
            ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "??????????????????????????????????????????????????????????????????");
            context.setResponseEntity(responseEntity);
            return;
        }
    }

    public void deleteAllocationStorehouse(JSONObject paramInJson) {

        AllocationStorehouseDto allocationStorehouseDto = new AllocationStorehouseDto();
        allocationStorehouseDto.setApplyId(paramInJson.getString("applyId"));
        allocationStorehouseDto.setStoreId(paramInJson.getString("storeId"));

        List<AllocationStorehouseDto> allocationStorehouseDtos = allocationStorehouseInnerServiceSMOImpl.queryAllocationStorehouses(allocationStorehouseDto);
        int flag = 0;
        for (AllocationStorehouseDto tmpAllocationStorehouseDto : allocationStorehouseDtos) {
            AllocationStorehousePo allocationStorehousePo = BeanConvertUtil.covertBean(tmpAllocationStorehouseDto, AllocationStorehousePo.class);
            allocationStorehousePo.setStatusCd("1");
            flag = allocationStorehouseV1InnerServiceSMOImpl.deleteAllocationStorehouse(allocationStorehousePo);

            if (flag < 1) {
                throw new CmdException("????????????");
            }
            ResourceStorePo resourceStorePo = new ResourceStorePo();
            resourceStorePo.setResId(allocationStorehousePo.getResId());
            //?????????????????????
            List<ResourceStorePo> resourceStores = resourceStoreServiceSMOImpl.getResourceStores(resourceStorePo);
            Assert.listOnlyOne(resourceStores, "????????????????????????");
            //??????????????????
            BigDecimal resourceStoreStock = new BigDecimal(resourceStores.get(0).getStock());
            //?????????????????????
            BigDecimal storehouseStock = new BigDecimal(allocationStorehousePo.getStock());
            //????????????
            BigDecimal stock = resourceStoreStock.add(storehouseStock);
            resourceStorePo.setStock(String.valueOf(stock));
            //????????????????????????
            if (StringUtil.isEmpty(resourceStores.get(0).getMiniStock())) {
                throw new IllegalArgumentException("?????????????????????????????????");
            }
            BigDecimal miniStock = new BigDecimal(resourceStores.get(0).getMiniStock()); //????????????????????????????????????
            if (StringUtil.isEmpty(resourceStores.get(0).getMiniUnitStock())) {
                throw new IllegalArgumentException("???????????????????????????????????????");
            }
            BigDecimal miniUnitStock = new BigDecimal(resourceStores.get(0).getMiniUnitStock()); //??????????????????????????????
            BigDecimal stock2 = new BigDecimal(allocationStorehousePo.getStock()); //??????????????????????????????
            BigDecimal nowMiniStock = stock2.multiply(miniUnitStock); //?????????????????????????????????
            BigDecimal newMiniStock = miniStock.add(nowMiniStock);
            resourceStorePo.setMiniStock(String.valueOf(newMiniStock));
            flag = resourceStoreV1InnerServiceSMOImpl.updateResourceStore(resourceStorePo);
            if (flag < 1) {
                throw new CmdException("????????????");
            }
            //??????????????????
            //????????????
            PurchaseApplyDto purchaseDto = new PurchaseApplyDto();
            purchaseDto.setBusinessKey(tmpAllocationStorehouseDto.getApplyId());
            List<PurchaseApplyDto> purchaseApplyDtoList = purchaseApplyInnerServiceSMOImpl.getActRuTaskId(purchaseDto);
            if (purchaseApplyDtoList != null && purchaseApplyDtoList.size() > 0) {
                PurchaseApplyDto purchaseDto1 = new PurchaseApplyDto();
                purchaseDto1.setActRuTaskId(purchaseApplyDtoList.get(0).getActRuTaskId());
                purchaseDto1.setAssigneeUser("999999");
                purchaseApplyInnerServiceSMOImpl.updateActRuTaskById(purchaseDto1);
            }
        }

        AllocationStorehouseApplyPo allocationStorehouseApplyPo = new AllocationStorehouseApplyPo();
        allocationStorehouseApplyPo.setApplyId(allocationStorehouseDto.getApplyId());
        allocationStorehouseApplyPo.setStoreId(allocationStorehouseDto.getStoreId());
        allocationStorehouseApplyPo.setStatusCd("1");
        flag = allocationStorehouseApplyV1InnerServiceSMOImpl.updateAllocationStorehouseApply(allocationStorehouseApplyPo);
        if (flag < 1) {
            throw new CmdException("????????????");
        }

    }
}
