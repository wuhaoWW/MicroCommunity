package com.java110.community.cmd.floor;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.FloorDto;
import com.java110.intf.community.IFloorInnerServiceSMO;
import com.java110.intf.community.IFloorV1InnerServiceSMO;
import com.java110.po.floor.FloorPo;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;


@Java110CmdDoc(title = "添加楼栋",
        description = "用于外系统添加楼栋信息功能",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/floor.saveFloor",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "floor.saveFloor"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "name", length = 64, remark = "名称"),
        @Java110ParamDoc(name = "floorArea", length = 64, remark = "面积"),
        @Java110ParamDoc(name = "floorNum", length = 64, remark = "编号"),
        @Java110ParamDoc(name = "seq",type = "int",length = 11, remark = "排序"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody="{\"floorId\":\"\",\"name\":\"2号楼\",\"floorName\":\"\",\"floorArea\":\"22\",\"floorNum\":\"2\",\"remark\":\"2\",\"errorInfo\":\"\",\"seq\":\"22\",\"communityId\":\"2022081539020475\"}",
        resBody="{'code':0,'msg':'成功'}"
)


@Java110Cmd(serviceCode = "floor.saveFloor")
public class SaveFloorCmd extends Cmd {
    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    @Autowired
    private IFloorV1InnerServiceSMO floorV1InnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        Assert.jsonObjectHaveKey(reqJson, "name", "请求报文中未包含name");
        Assert.jsonObjectHaveKey(reqJson, "userId", "请求报文中未包含userId");
        Assert.jsonObjectHaveKey(reqJson, "floorNum", "请求报文中未包含floorNum");
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求报文中未包含communityId");
        FloorDto floorDto = new FloorDto();
        floorDto.setFloorNum(reqJson.getString("floorNum"));
        floorDto.setCommunityId(reqJson.getString("communityId"));


        int floorCount = floorInnerServiceSMOImpl.queryFloorsCount(floorDto);

        if (floorCount > 0) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "楼栋编号已经存在");
        }
    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        String floorId = GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_floorId);
        JSONObject businessFloor = new JSONObject();
        businessFloor.put("floorId", floorId);
        businessFloor.put("name", reqJson.getString("name"));
        businessFloor.put("remark", reqJson.getString("remark"));
        businessFloor.put("userId", reqJson.getString("userId"));
        businessFloor.put("floorNum", reqJson.getString("floorNum"));
        businessFloor.put("communityId", reqJson.getString("communityId"));
        businessFloor.put("floorArea", reqJson.getString("floorArea"));
        businessFloor.put("seq",reqJson.getString("seq"));
        FloorPo floorPo = BeanConvertUtil.covertBean(businessFloor, FloorPo.class);
        int flag = floorV1InnerServiceSMOImpl.saveFloor(floorPo);

        if (flag < 1) {
            throw new CmdException("保存楼栋失败");
        }

        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}
