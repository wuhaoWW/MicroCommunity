package com.java110.store.api;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.allocationStorehouse.AllocationStorehouseDto;
import com.java110.dto.allocationStorehouseApply.AllocationStorehouseApplyDto;
import com.java110.dto.purchaseApply.PurchaseApplyDto;
import com.java110.dto.resourceStore.ResourceStoreDto;
import com.java110.dto.storehouse.StorehouseDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.store.*;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.purchase.PurchaseApplyDetailPo;
import com.java110.po.purchase.PurchaseApplyPo;
import com.java110.po.purchase.ResourceStorePo;
import com.java110.store.bmo.purchase.IPurchaseApplyBMO;
import com.java110.store.bmo.purchase.IResourceEnterBMO;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping(value = "/purchase")
public class PurchaseApi {

    @Autowired
    private IPurchaseApplyBMO purchaseApplyBMOImpl;

    @Autowired
    private IResourceEnterBMO resourceEnterBMOImpl;

    @Autowired
    private IResourceStoreInnerServiceSMO resourceStoreInnerServiceSMOImpl;

    @Autowired
    private IStorehouseInnerServiceSMO storehouseInnerServiceSMOImpl;

    @Autowired
    private IAllocationStorehouseInnerServiceSMO allocationStorehouseInnerServiceSMOImpl;

    @Autowired
    private IAllocationStorehouseApplyInnerServiceSMO allocationStorehouseApplyInnerServiceSMOImpl;

    @Autowired
    private IPurchaseApplyInnerServiceSMO purchaseApplyInnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    //???
    public static final String DOMAIN_COMMON = "DOMAIN.COMMON";

    //???
    public static final String URGRNT_NUMBER = "URGRNT_NUMBER";

    /**
     * ????????????
     * <p>
     * {"resourceStores":[{"resId":"852020061636590016","resName":"?????????","resCode":"003","price":"100.00","stock":"0","description":"ada","quantity":"1"},
     * {"resId":"852020061729120031","resName":"?????????","resCode":"002","price":"33.00","stock":"0","description":"??????","quantity":"1"}],
     * "description":"123123","endUserName":"1","endUserTel":"17797173942","file":"","resOrderType":"10000","staffId":"","staffName":""}
     *
     * @param reqJson
     * @return
     */
    @RequestMapping(value = "/purchaseApply", method = RequestMethod.POST)
    public ResponseEntity<String> purchaseApply(@RequestBody JSONObject reqJson,
                                                @RequestHeader(value = "user-id") String userId,
                                                @RequestHeader(value = "store-id") String storeId) {
        Assert.hasKeyAndValue(reqJson, "resourceStores", "???????????????????????????????????????");
        Assert.hasKeyAndValue(reqJson, "description", "????????????????????????????????????");
        Assert.hasKeyAndValue(reqJson, "resOrderType", "??????????????????????????????");
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        Assert.listOnlyOne(userDtos,"???????????????");


        String userName  = userDtos.get(0).getName();
        PurchaseApplyPo purchaseApplyPo = new PurchaseApplyPo();
        purchaseApplyPo.setApplyOrderId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_applyOrderId));
        purchaseApplyPo.setDescription(reqJson.getString("description"));
        purchaseApplyPo.setUserId(userId);
        purchaseApplyPo.setUserName(userName);
        purchaseApplyPo.setEndUserName(reqJson.getString("endUserName"));
        purchaseApplyPo.setEndUserTel(reqJson.getString("endUserTel"));
        purchaseApplyPo.setStoreId(storeId);
        purchaseApplyPo.setResOrderType(PurchaseApplyDto.RES_ORDER_TYPE_ENTER);
        purchaseApplyPo.setState(PurchaseApplyDto.STATE_WAIT_DEAL);
        purchaseApplyPo.setCreateTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        purchaseApplyPo.setCreateUserId(userId);
        purchaseApplyPo.setCreateUserName(userName);
        purchaseApplyPo.setWarehousingWay(PurchaseApplyDto.WAREHOUSING_TYPE_APPLY);
        purchaseApplyPo.setCommunityId(reqJson.getString("communityId"));
        JSONArray resourceStores = reqJson.getJSONArray("resourceStores");
        List<PurchaseApplyDetailPo> purchaseApplyDetailPos = new ArrayList<>();
        for (int resourceStoreIndex = 0; resourceStoreIndex < resourceStores.size(); resourceStoreIndex++) {
            JSONObject resourceStore = resourceStores.getJSONObject(resourceStoreIndex);
            resourceStore.remove("price");//?????????????????????
            resourceStore.put("originalStock", resourceStore.getString("stock"));
            PurchaseApplyDetailPo purchaseApplyDetailPo = BeanConvertUtil.covertBean(resourceStore, PurchaseApplyDetailPo.class);
            purchaseApplyDetailPo.setId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_applyOrderId));
            purchaseApplyDetailPos.add(purchaseApplyDetailPo);
        }
        purchaseApplyPo.setPurchaseApplyDetailPos(purchaseApplyDetailPos);
        return purchaseApplyBMOImpl.apply(purchaseApplyPo,reqJson);
    }

    //?????????cmd ??????
//    @RequestMapping(value = "/resourceEnter", method = RequestMethod.POST)
//    public ResponseEntity<String> resourceEnter(@RequestBody JSONObject reqJson) {
//        Assert.hasKeyAndValue(reqJson, "applyOrderId", "??????ID??????");
//        PurchaseApplyDto purchaseApplyDto = new PurchaseApplyDto();
//        purchaseApplyDto.setApplyOrderId(reqJson.getString("applyOrderId"));
//        purchaseApplyDto.setStatusCd("0");
//        List<PurchaseApplyDto> purchaseApplyDtoList = purchaseApplyInnerServiceSMOImpl.queryPurchaseApplys(purchaseApplyDto);
//        if(purchaseApplyDtoList!=null && PurchaseApplyDto.STATE_AUDITED.equals(purchaseApplyDtoList.get(0).getState())){
//            throw new IllegalArgumentException("??????????????????????????????????????????????????????");
//        }
//        JSONArray purchaseApplyDetails = reqJson.getJSONArray("purchaseApplyDetailVo");
//        List<PurchaseApplyDetailPo> purchaseApplyDetailPos = new ArrayList<>();
//        for (int detailIndex = 0; detailIndex < purchaseApplyDetails.size(); detailIndex++) {
//            JSONObject purchaseApplyDetail = purchaseApplyDetails.getJSONObject(detailIndex);
//            Assert.hasKeyAndValue(purchaseApplyDetail, "purchaseQuantity", "?????????????????????");
//            Assert.hasKeyAndValue(purchaseApplyDetail, "price", "?????????????????????");
//            Assert.hasKeyAndValue(purchaseApplyDetail, "id", "??????ID??????");
//            PurchaseApplyDetailPo purchaseApplyDetailPo = BeanConvertUtil.covertBean(purchaseApplyDetail, PurchaseApplyDetailPo.class);
//            purchaseApplyDetailPos.add(purchaseApplyDetailPo);
//        }
//        PurchaseApplyPo purchaseApplyPo = new PurchaseApplyPo();
//        purchaseApplyPo.setApplyOrderId(reqJson.getString("applyOrderId"));
//        purchaseApplyPo.setPurchaseApplyDetailPos(purchaseApplyDetailPos);
//        return resourceEnterBMOImpl.enter(purchaseApplyPo);
//    }

    /**
     * ??????????????????
     * <p>
     * {"resourceStores":[{"resId":"852020061636590016","resName":"?????????","resCode":"003","price":"100.00","stock":"0","description":"ada","quantity":"1"},
     * {"resId":"852020061729120031","resName":"?????????","resCode":"002","price":"33.00","stock":"0","description":"??????","quantity":"1"}],
     * "description":"123123","endUserName":"1","endUserTel":"17797173942","file":"","resOrderType":"10000","staffId":"","staffName":""}
     *
     * @param reqJson
     * @return
     */
    @RequestMapping(value = "/purchaseStorage", method = RequestMethod.POST)
    public ResponseEntity<String> purchaseStorage(@RequestBody JSONObject reqJson,
                                                  @RequestHeader(value = "user-id") String userId,

                                                  @RequestHeader(value = "store-id") String storeId) {
        Assert.hasKeyAndValue(reqJson, "resourceStores", "???????????????????????????????????????");
        Assert.hasKeyAndValue(reqJson, "description", "????????????????????????????????????");
        Assert.hasKeyAndValue(reqJson, "resOrderType", "??????????????????????????????");
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        Assert.listOnlyOne(userDtos,"???????????????");


        String userName  = userDtos.get(0).getName();
        PurchaseApplyPo purchaseApplyPo = new PurchaseApplyPo();
        purchaseApplyPo.setApplyOrderId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_applyOrderId));
        purchaseApplyPo.setDescription(reqJson.getString("description"));
        purchaseApplyPo.setUserId(userId);
        purchaseApplyPo.setUserName(userName);
        purchaseApplyPo.setEndUserName(reqJson.getString("endUserName"));
        purchaseApplyPo.setEndUserTel(reqJson.getString("endUserTel"));
        purchaseApplyPo.setStoreId(storeId);
        purchaseApplyPo.setResOrderType(PurchaseApplyDto.RES_ORDER_TYPE_ENTER);
        purchaseApplyPo.setState(PurchaseApplyDto.STATE_END);
        purchaseApplyPo.setCreateTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        purchaseApplyPo.setDescription("??????????????????");
        purchaseApplyPo.setCreateUserId(userId);
        purchaseApplyPo.setCreateUserName(userName);
        purchaseApplyPo.setWarehousingWay(PurchaseApplyDto.WAREHOUSING_TYPE_DIRECT);
        purchaseApplyPo.setCommunityId(reqJson.getString("communityId"));
        JSONArray resourceStores = reqJson.getJSONArray("resourceStores");
        List<PurchaseApplyDetailPo> purchaseApplyDetailPos = new ArrayList<>();
        for (int resourceStoreIndex = 0; resourceStoreIndex < resourceStores.size(); resourceStoreIndex++) {
            JSONObject resourceStore = resourceStores.getJSONObject(resourceStoreIndex);
            PurchaseApplyDetailPo purchaseApplyDetailPo = BeanConvertUtil.covertBean(resourceStore, PurchaseApplyDetailPo.class);
            purchaseApplyDetailPo.setId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_applyOrderId));
            purchaseApplyDetailPo.setRemark("??????????????????");
            purchaseApplyDetailPo.setOriginalStock(resourceStore.getString("stock"));
            purchaseApplyDetailPo.setQuantity(purchaseApplyDetailPo.getPurchaseQuantity());
            purchaseApplyDetailPos.add(purchaseApplyDetailPo);
            //????????????
            ResourceStorePo resourceStorePo = new ResourceStorePo();
            resourceStorePo.setPurchasePrice(purchaseApplyDetailPo.getPrice());
            resourceStorePo.setResId(purchaseApplyDetailPo.getResId());
            resourceStorePo.setStock(purchaseApplyDetailPo.getPurchaseQuantity());
            resourceStorePo.setResOrderType(PurchaseApplyDto.RES_ORDER_TYPE_ENTER);
            //??????????????????
            BigDecimal purchaseQuantity = new BigDecimal(purchaseApplyDetailPo.getPurchaseQuantity());
            //??????????????????????????????
            BigDecimal miniStock = new BigDecimal(resourceStore.getString("miniStock"));
            //????????????????????????
            BigDecimal newMiniStock = new BigDecimal(0);
            if (StringUtil.isEmpty(resourceStore.getString("miniUnitStock"))) {
                throw new IllegalArgumentException("???????????????????????????????????????");
            }
            BigDecimal miniUnitStock = new BigDecimal(resourceStore.getString("miniUnitStock"));
            //????????????????????????
            if (StringUtil.isEmpty(resourceStore.getString("miniStock"))) {
                newMiniStock = purchaseQuantity.multiply(miniUnitStock);
            } else {
                newMiniStock = (purchaseQuantity.multiply(miniUnitStock)).add(miniStock);
            }
            resourceStorePo.setMiniStock(String.valueOf(newMiniStock));
            resourceStoreInnerServiceSMOImpl.updateResourceStore(resourceStorePo);
        }
        purchaseApplyPo.setPurchaseApplyDetailPos(purchaseApplyDetailPos);
        return purchaseApplyBMOImpl.apply(purchaseApplyPo,reqJson);
    }
}
