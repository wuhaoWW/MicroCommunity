package com.java110.store.cmd.shop;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.roleCommunity.RoleCommunityDto;
import com.java110.dto.shop.ShopDto;
import com.java110.dto.store.StoreDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.mall.IShopInnerServiceSMO;
import com.java110.intf.store.IStoreV1InnerServiceSMO;
import com.java110.intf.user.IOrgInnerServiceSMO;
import com.java110.intf.user.IOrgStaffRelInnerServiceSMO;
import com.java110.intf.user.IRoleCommunityV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.utils.constant.StateConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import com.java110.vo.api.community.ApiCommunityDataVo;
import com.java110.vo.api.community.ApiCommunityVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "shop.listMyEnteredShops")
public class ListMyEnteredShopsCmd extends Cmd {

    @Autowired(required = false)
    private IShopInnerServiceSMO shopInnerServiceSMOImpl;

    @Autowired
    private IOrgStaffRelInnerServiceSMO orgStaffRelInnerServiceSMOImpl;

    @Autowired
    private IRoleCommunityV1InnerServiceSMO roleCommunityV1InnerServiceSMOImpl;

    @Autowired
    private IOrgInnerServiceSMO orgInnerServiceSMOImpl;

    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {

        if (!reqJson.containsKey("storeId") || StringUtil.isEmpty(reqJson.getString("storeId"))) {
            reqJson.put("storeId", cmdDataFlowContext.getReqHeaders().get("store-id"));
        }

        if (!reqJson.containsKey("userId") || StringUtil.isEmpty(reqJson.getString("userId"))) {
            reqJson.put("userId", cmdDataFlowContext.getReqHeaders().get("user-id"));
        }

        Assert.hasKeyAndValue(reqJson, "storeId", "????????????????????????????????????");
        Assert.hasKeyAndValue(reqJson, "userId", "????????????????????????????????????");
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {


        // ?????????????????????????????????????????? ?????? ???????????????
        UserDto userDto = new UserDto();
        userDto.setUserId(reqJson.getString("userId"));
        userDto.setPage(1);
        userDto.setRow(1);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        Assert.listOnlyOne(userDtos, "???????????????");

        //????????????????????????;
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreId(reqJson.getString("storeId"));
        List<StoreDto> storeDtos = storeV1InnerServiceSMOImpl.queryStores(storeDto);

        Assert.listOnlyOne(storeDtos, "???????????????");

        int count = 0;
        List<ShopDto> shops = null;
        ShopDto shopDto = null;
        if(UserDto.LEVEL_CD_ADMIN.equals(userDtos.get(0).getLevelCd())){
            shopDto = BeanConvertUtil.covertBean(reqJson, ShopDto.class);
            shopDto.setStoreId(reqJson.getString("storeId"));
            shopDto.setState(ShopDto.STATE_Y);
            if (reqJson.containsKey("shopName")) {
                shopDto.setShopName(reqJson.getString("shopName"));
            }
            count = shopInnerServiceSMOImpl.queryShopsCount(shopDto);
            if (count > 0) {
                shops = BeanConvertUtil.covertBeanList(shopInnerServiceSMOImpl.queryShops(shopDto), ShopDto.class);
            } else {
                shops = new ArrayList<>();
            }
        }else{
            RoleCommunityDto orgCommunityDto = BeanConvertUtil.covertBean(reqJson, RoleCommunityDto.class);
            orgCommunityDto.setStaffId(userDtos.get(0).getUserId());
            count = roleCommunityV1InnerServiceSMOImpl.queryRoleCommunitysNameCount(orgCommunityDto);
            if (count > 0) {
                List<RoleCommunityDto> roleCommunityDtos = roleCommunityV1InnerServiceSMOImpl.queryRoleCommunitysName(orgCommunityDto);
                shops = new ArrayList<>();
                for (RoleCommunityDto tmpOrgCommunityDto : roleCommunityDtos) {
                    shopDto = new ShopDto();
                    shopDto.setShopId(tmpOrgCommunityDto.getCommunityId());
                    shopDto.setShopName(tmpOrgCommunityDto.getCommunityName());
                    shops.add(shopDto);
                }
            } else {
                shops = new ArrayList<>();
            }

        }
        //?????? ??????????????????????????????
        if (shops.size() < 1 && (StoreDto.STORE_TYPE_ADMIN.equals(storeDtos.get(0).getStoreTypeCd()) || StoreDto.STORE_TYPE_DEV.equals(storeDtos.get(0).getStoreTypeCd()))) {
             shopDto = new ShopDto();
            shopDto.setShopId("-1");
            shopDto.setShopName("????????????");
            shopDto.setStoreTel("18909711234");
            shopDto.setState("1100");
            shops.add(shopDto);
        }

        context.setResponseEntity(ResultVo.createResponseEntity((int) Math.ceil((double) count / (double) reqJson.getInteger("row")),count,shops));
    }
}
