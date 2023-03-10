/*
 * Copyright 2017-2020 吴学文 and java110 team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.java110.common.cmd.attendanceClasses;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.intf.common.IAttendanceClassesV1InnerServiceSMO;
import com.java110.po.attendanceClasses.AttendanceClassesPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


@Java110CmdDoc(title = "删除考勤规则",
        description = "外系统删除考勤规则",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/attendanceClasses.deleteAttendanceClasses",
        resource = "commonDoc",
        author = "吴学文",
        serviceCode = "attendanceClasses.deleteAttendanceClasses"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "classesId", length = 30, remark = "班级ID"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody="{\"classesId\":\"123123\"}",
        resBody="{'code':0,'msg':'成功'}"
)
/**
 * 类表述：删除
 * 服务编码：attendanceClasses.deleteAttendanceClasses
 * 请求路劲：/app/attendanceClasses.DeleteAttendanceClasses
 * add by 吴学文 at 2022-07-16 17:50:14 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "attendanceClasses.deleteAttendanceClasses")
public class DeleteAttendanceClassesCmd extends Cmd {
    private static Logger logger = LoggerFactory.getLogger(DeleteAttendanceClassesCmd.class);

    @Autowired
    private IAttendanceClassesV1InnerServiceSMO attendanceClassesV1InnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "classesId", "classesId不能为空");

    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        String storeId = cmdDataFlowContext.getReqHeaders().get("store-id");

        AttendanceClassesPo attendanceClassesPo = BeanConvertUtil.covertBean(reqJson, AttendanceClassesPo.class);
        attendanceClassesPo.setStoreId(storeId);
        int flag = attendanceClassesV1InnerServiceSMOImpl.deleteAttendanceClasses(attendanceClassesPo);

        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}
