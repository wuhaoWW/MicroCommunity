package com.java110.community.cmd.parkingSpace;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.DataFlowContext;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.community.IParkingSpaceV1InnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.fee.IPayFeeV1InnerServiceSMO;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.intf.user.IOwnerCarV1InnerServiceSMO;
import com.java110.po.car.OwnerCarPo;
import com.java110.po.fee.PayFeePo;
import com.java110.po.parking.ParkingSpacePo;
import com.java110.utils.constant.BusinessTypeConstant;
import com.java110.utils.constant.FeeTypeConstant;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

@Java110Cmd(serviceCode = "parkingSpace.exitParkingSpace")
public class ExitParkingSpaceCmd extends Cmd {

    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;
    @Autowired
    private IParkingSpaceV1InnerServiceSMO parkingSpaceV1InnerServiceSMOImpl;

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IOwnerCarV1InnerServiceSMO ownerCarV1InnerServiceSMOImpl;

    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;


    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.jsonObjectHaveKey(reqJson, "communityId", "????????????????????????communityId??????");
        Assert.jsonObjectHaveKey(reqJson, "ownerId", "????????????????????????ownerId??????");
        Assert.jsonObjectHaveKey(reqJson, "psId", "????????????????????????psId??????");
        Assert.jsonObjectHaveKey(reqJson, "storeId", "????????????????????????storeId??????");


        Assert.hasLength(reqJson.getString("communityId"), "??????ID????????????");
        Assert.hasLength(reqJson.getString("ownerId"), "ownerId????????????");
        Assert.hasLength(reqJson.getString("psId"), "psId????????????");
        Assert.hasLength(reqJson.getString("storeId"), "storeId????????????");
        //

    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        //?????? ??????????????????????????????
        exitParkingSpace(reqJson);

        //?????????????????????????????????
        modifyParkingSpaceState(reqJson);


        //??????????????????
        exitParkingSpaceFee(reqJson);
    }

    /**
     * ??????????????????
     *
     * @param paramInJson     ???????????????????????????
     * @return ?????????????????????????????????
     */
    public void exitParkingSpace(JSONObject paramInJson) {


        OwnerCarDto ownerCarDto = (OwnerCarDto) paramInJson.get("ownerCarDto");

        JSONObject businessOwnerCar = new JSONObject();
        //businessUnit.putAll(paramInJson);
        businessOwnerCar.put("carId", ownerCarDto.getCarId());
        //businessUnit.put("userId", dataFlowContext.getRequestCurrentHeaders().get(CommonConstant.HTTP_USER_ID));
        OwnerCarPo ownerCarPo = BeanConvertUtil.covertBean(businessOwnerCar, OwnerCarPo.class);

        int flag = ownerCarV1InnerServiceSMOImpl.deleteOwnerCar(ownerCarPo);
        if(flag < 1){
            throw new CmdException("??????????????????");
        }
    }

    /**
     * ???????????????????????????
     *
     * @param paramInJson ???????????????????????????
     * @return ?????????????????????????????????
     */
    public void modifyParkingSpaceState(JSONObject paramInJson) {

        ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
        parkingSpaceDto.setCommunityId(paramInJson.getString("communityId"));
        parkingSpaceDto.setPsId(paramInJson.getString("psId"));
        List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);

        if (parkingSpaceDtos == null || parkingSpaceDtos.size() != 1) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "???????????????????????????" + JSONObject.toJSONString(parkingSpaceDto));
        }

        parkingSpaceDto = parkingSpaceDtos.get(0);

        JSONObject businessParkingSpace = new JSONObject();

        businessParkingSpace.putAll(BeanConvertUtil.beanCovertMap(parkingSpaceDto));
        businessParkingSpace.put("state", "F");
        ParkingSpacePo parkingSpacePo = BeanConvertUtil.covertBean(businessParkingSpace, ParkingSpacePo.class);

       int flag = parkingSpaceV1InnerServiceSMOImpl.updateParkingSpace(parkingSpacePo);

        if(flag < 1){
            throw new CmdException("??????????????????");
        }
    }

    public void exitParkingSpaceFee(JSONObject paramInJson) {


        ParkingSpaceDto parkingSpaceDto = (ParkingSpaceDto) paramInJson.get("parkingSpaceDto");
        //?????????????????????????????????
        FeeDto feeDto = new FeeDto();
        feeDto.setCommunityId(paramInJson.getString("communityId"));
        feeDto.setIncomeObjId(paramInJson.getString("storeId"));
        feeDto.setPayerObjId(paramInJson.getString("psId"));
        feeDto.setFeeTypeCd("1001".equals(parkingSpaceDto.getTypeCd())
                ? ("H".equals(parkingSpaceDto.getState())
                ? FeeTypeConstant.FEE_TYPE_HIRE_UP_PARKING_SPACE
                : FeeTypeConstant.FEE_TYPE_SELL_UP_PARKING_SPACE)
                : ("H".equals(parkingSpaceDto.getState())
                ? FeeTypeConstant.FEE_TYPE_HIRE_DOWN_PARKING_SPACE
                : FeeTypeConstant.FEE_TYPE_SELL_DOWN_PARKING_SPACE));
        List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);

        if (feeDtos == null || feeDtos.size() != 1) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "??????????????????????????????????????????????????????");
        }


        JSONObject businessFee = new JSONObject();
        //businessUnit.putAll(paramInJson);
        businessFee.put("feeId", feeDtos.get(0).getFeeId());
        //businessUnit.put("userId", dataFlowContext.getRequestCurrentHeaders().get(CommonConstant.HTTP_USER_ID));
        PayFeePo payFeePo = BeanConvertUtil.covertBean(businessFee, PayFeePo.class);

       int flag = payFeeV1InnerServiceSMOImpl.deletePayFee(payFeePo);

        if(flag < 1){
            throw new CmdException("??????????????????");
        }
    }

}
