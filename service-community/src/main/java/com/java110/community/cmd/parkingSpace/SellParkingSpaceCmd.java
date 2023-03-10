package com.java110.community.cmd.parkingSpace;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.DataFlowContext;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.community.IParkingSpaceV1InnerServiceSMO;
import com.java110.intf.fee.IFeeConfigInnerServiceSMO;
import com.java110.intf.user.IOwnerCarAttrInnerServiceSMO;
import com.java110.intf.user.IOwnerCarV1InnerServiceSMO;
import com.java110.po.car.OwnerCarPo;
import com.java110.po.ownerCarAttr.OwnerCarAttrPo;
import com.java110.po.parking.ParkingSpacePo;
import com.java110.utils.constant.BusinessTypeConstant;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

@Java110Cmd(serviceCode = "parkingSpace.sellParkingSpace")
public class SellParkingSpaceCmd extends Cmd {


    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;
    @Autowired
    private IParkingSpaceV1InnerServiceSMO parkingSpaceV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerCarV1InnerServiceSMO ownerCarV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerCarAttrInnerServiceSMO ownerCarAttrInnerServiceSMOImpl;


    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.jsonObjectHaveKey(reqJson, "communityId", "???????????????ID");
        Assert.jsonObjectHaveKey(reqJson, "ownerId", "????????????????????????ownerId");
        Assert.jsonObjectHaveKey(reqJson, "carNum", "????????????????????????carNum");
        Assert.jsonObjectHaveKey(reqJson, "carBrand", "????????????????????????carBrand");
        Assert.jsonObjectHaveKey(reqJson, "carType", "????????????????????????carType");
        Assert.jsonObjectHaveKey(reqJson, "carColor", "?????????carColor");
        Assert.jsonObjectHaveKey(reqJson, "psId", "?????????psId");
        Assert.jsonObjectHaveKey(reqJson, "storeId", "?????????storeId");
        //Assert.jsonObjectHaveKey(reqJson, "receivedAmount", "?????????receivedAmount");
        Assert.jsonObjectHaveKey(reqJson, "sellOrHire", "?????????sellOrHire");

        Assert.hasLength(reqJson.getString("communityId"), "??????ID????????????");
        Assert.hasLength(reqJson.getString("ownerId"), "ownerId????????????");
        Assert.hasLength(reqJson.getString("psId"), "psId????????????");
        //Assert.isMoney(reqJson.getString("receivedAmount"), "???????????????????????????");

        if (!"H".equals(reqJson.getString("sellOrHire"))
                && !"S".equals(reqJson.getString("sellOrHire"))) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "???????????????sellOrFire????????? ????????????S ?????????H");
        }
    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        String feeId = GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_feeId);
        reqJson.put("feeId", feeId);

        //???????????????
        sellParkingSpace(reqJson);


        reqJson.put("carNumType",reqJson.getString("sellOrHire"));

        modifySellParkingSpaceState(reqJson);
    }



    /**
     * ???????????????????????????
     *
     * @param paramInJson ???????????????????????????
     * @return ?????????????????????????????????
     */
    public void modifySellParkingSpaceState(JSONObject paramInJson) {

        ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
        parkingSpaceDto.setCommunityId(paramInJson.getString("communityId"));
        parkingSpaceDto.setPsId(paramInJson.getString("psId"));
        List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);

        if (parkingSpaceDtos == null || parkingSpaceDtos.size() != 1) {
            //throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "???????????????????????????" + JSONObject.toJSONString(parkingSpaceDto));
            return;
        }

        parkingSpaceDto = parkingSpaceDtos.get(0);

        JSONObject businessParkingSpace = new JSONObject();

        businessParkingSpace.putAll(BeanConvertUtil.beanCovertMap(parkingSpaceDto));
        businessParkingSpace.put("state", paramInJson.getString("carNumType"));
        ParkingSpacePo parkingSpacePo = BeanConvertUtil.covertBean(businessParkingSpace, ParkingSpacePo.class);
        int flag = parkingSpaceV1InnerServiceSMOImpl.updateParkingSpace(parkingSpacePo);
        if (flag < 1) {
            throw new CmdException("??????????????????");
        }
    }

    /**
     * ?????????????????????
     * <p>
     * * name:'',
     * *                 age:'',
     * *                 link:'',
     * *                 sex:'',
     * *                 remark:''
     *
     * @param paramInJson ???????????????????????????
     * @return ?????????????????????????????????
     */
    public void sellParkingSpace(JSONObject paramInJson) {

        JSONObject businessOwnerCar = new JSONObject();
        businessOwnerCar.putAll(paramInJson);
        businessOwnerCar.put("memberId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_carId));
        if (!paramInJson.containsKey("carId") || paramInJson.getString("carId").startsWith("-")) {
            businessOwnerCar.put("carId", businessOwnerCar.getString("memberId"));
        }
        OwnerCarPo ownerCarPo = BeanConvertUtil.covertBean(businessOwnerCar, OwnerCarPo.class);
        ownerCarPo.setState(OwnerCarDto.STATE_NORMAL);

        //??????????????????????????????
        if (!paramInJson.containsKey("carTypeCd") || StringUtil.isEmpty(paramInJson.getString("carTypeCd"))) {
            ownerCarPo.setCarTypeCd(OwnerCarDto.CAR_TYPE_PRIMARY);
        }
        //??????????????????
        dealOwnerCarAttr(paramInJson, ownerCarPo);
        int flag =  ownerCarV1InnerServiceSMOImpl.saveOwnerCar(ownerCarPo);
        if (flag < 1) {
            throw new CmdException("??????????????????");
        }
    }

    private void dealOwnerCarAttr(JSONObject paramInJson, OwnerCarPo ownerCarPo) {

        if (!paramInJson.containsKey("attrs")) {
            return;
        }

        JSONArray attrs = paramInJson.getJSONArray("attrs");
        if (attrs.size() < 1) {
            return;
        }
        JSONObject attr = null;
        int flag = 0;
        for (int attrIndex = 0; attrIndex < attrs.size(); attrIndex++) {
            attr = attrs.getJSONObject(attrIndex);
            OwnerCarAttrPo ownerCarAttrPo = new OwnerCarAttrPo();
            ownerCarAttrPo.setAttrId(GenerateCodeFactory.getAttrId());
            ownerCarAttrPo.setCommunityId(ownerCarPo.getCommunityId());
            ownerCarAttrPo.setCarId(ownerCarPo.getCarId());
            ownerCarAttrPo.setSpecCd(attr.getString("specCd"));
            ownerCarAttrPo.setValue(attr.getString("value"));
            flag = ownerCarAttrInnerServiceSMOImpl.saveOwnerCarAttr(ownerCarAttrPo);
            if (flag < 1) {
                throw new CmdException("????????????????????????");
            }
        }

    }
}
