package com.java110.common.cmd.attendanceClasses;

import com.alibaba.fastjson.JSONObject;
import com.java110.common.smo.impl.AttendanceClassesTaskDetailInnerServiceSMOImpl;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.client.FileUploadTemplate;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.attendanceClasses.AttendanceClassesDto;
import com.java110.dto.attendanceClasses.AttendanceClassesTaskDetailDto;
import com.java110.dto.attendanceClasses.AttendanceClassesTaskDto;
import com.java110.dto.file.FileDto;
import com.java110.dto.org.OrgStaffRelDto;
import com.java110.dto.store.StoreUserDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.common.*;
import com.java110.intf.store.IOrgStaffRelV1InnerServiceSMO;
import com.java110.intf.store.IStoreInnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.attendanceClasses.AttendanceClassesPo;
import com.java110.po.attendanceClassesTask.AttendanceClassesTaskPo;
import com.java110.po.attendanceClassesTaskDetail.AttendanceClassesTaskDetailPo;
import com.java110.po.attendanceLog.AttendanceLogPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Java110CmdDoc(title = "考勤打卡",
        description = "外系统打卡",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/attendanceClasses.checkIn",
        resource = "commonDoc",
        author = "吴学文",
        serviceCode = "attendanceClasses.checkIn"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "staffId", length = 30, remark = "打卡员工"),
        @Java110ParamDoc(name = "checkTime", type = "String", length = 30, remark = "考勤时间 YYYY-MM-DD hh24:mi:ss"),
        @Java110ParamDoc(name = "photo", type = "String", length = 2048, remark = "考勤图片"),

})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody = "{\"classesId\":\"\",\"staffId\":\"xxx\",\"checkTime\":\"2022-01-01 09:09:09\",\"photo\":\"...\"}",
        resBody = "{'code':0,'msg':'成功'}"
)
/**
 * 考勤 打卡
 */
@Java110Cmd(serviceCode = "attendanceClasses.checkIn")
public class CheckInCmd extends Cmd {

    @Autowired
    private IStoreInnerServiceSMO storeInnerServiceSMOImpl;

    @Autowired
    private IAttendanceLogInnerServiceSMO attendanceLogInnerServiceSMOImpl;

    @Autowired
    private IAttendanceClassesTaskDetailInnerServiceSMO attendanceClassesTaskDetailInnerServiceSMOImpl;

    @Autowired
    private IAttendanceClassesV1InnerServiceSMO attendanceClassesV1InnerServiceSMOImpl;

    @Autowired
    private IAttendanceClassesTaskInnerServiceSMO attendanceClassesTaskInnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    @Autowired
    private IFileInnerServiceSMO fileInnerServiceSMOImpl;

    @Autowired
    private IOrgStaffRelV1InnerServiceSMO orgStaffRelV1InnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        Assert.hasKeyAndValue(reqJson, "staffId", "未包含员工");
        Assert.hasKeyAndValue(reqJson, "clockTime", "未包含考勤时间");
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        StoreUserDto storeUserDto = new StoreUserDto();
        storeUserDto.setUserId(reqJson.getString("staffId"));
        List<StoreUserDto> storeUserDtos = storeInnerServiceSMOImpl.getStoreUserInfo(storeUserDto);

        Assert.listOnlyOne(storeUserDtos, "未找到商户信息");

        UserDto userDto = new UserDto();
        userDto.setUserId(reqJson.getString("staffId"));
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        Assert.listOnlyOne(storeUserDtos, "员工不存在");

        OrgStaffRelDto orgStaffRelDto = new OrgStaffRelDto();
        orgStaffRelDto.setStoreId(storeUserDtos.get(0).getStoreId());
        orgStaffRelDto.setStaffId(reqJson.getString("staffId"));
        List<OrgStaffRelDto> orgStaffRelDtos = orgStaffRelV1InnerServiceSMOImpl.queryOrgStaffRels(orgStaffRelDto);

        if(orgStaffRelDtos == null || orgStaffRelDtos.size() < 1){
            throw new CmdException("员工没有考勤任务");
        }

        List<String> orgIds = new ArrayList<>();
        for(OrgStaffRelDto orgStaffRelDto1: orgStaffRelDtos){
            orgIds.add(orgStaffRelDto1.getOrgId());
        }

        // 考勤班次是否存在
        AttendanceClassesDto attendanceClassesDto = new AttendanceClassesDto();
        attendanceClassesDto.setStoreId(storeUserDtos.get(0).getStoreId());
        attendanceClassesDto.setClassesObjIds(orgIds.toArray(new String[orgIds.size()]));
        List<AttendanceClassesDto> attendanceClassesDtos = attendanceClassesV1InnerServiceSMOImpl.queryAttendanceClassess(attendanceClassesDto);

        Assert.listOnlyOne(attendanceClassesDtos, "班次不存在");


        AttendanceLogPo attendanceLogPo = new AttendanceLogPo();
        attendanceLogPo.setLogId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_logId));
        attendanceLogPo.setStoreId(storeUserDtos.get(0).getStoreId());
        attendanceLogPo.setStaffId(reqJson.getString("staffId"));
        attendanceLogPo.setClockTime(reqJson.getString("clockTime"));
        attendanceLogPo.setDepartmentId(attendanceClassesDtos.get(0).getClassesObjId());
        attendanceLogPo.setDepartmentName(attendanceClassesDtos.get(0).getClassesObjName());
        attendanceLogPo.setStaffName(userDtos.get(0).getName());

        int flag = attendanceLogInnerServiceSMOImpl.saveAttendanceLog(attendanceLogPo);

        if (flag < 1) {
            throw new CmdException("考勤失败");
        }

        // 查询是否为上班

        AttendanceClassesTaskDetailDto attendanceClassesTaskDetailDto = new AttendanceClassesTaskDetailDto();
        attendanceClassesTaskDetailDto.setNowCheckTime(reqJson.getString("clockTime"));
        attendanceClassesTaskDetailDto.setClassId(attendanceClassesDtos.get(0).getClassesId());
        attendanceClassesTaskDetailDto.setStaffId(reqJson.getString("staffId"));
        List<AttendanceClassesTaskDetailDto> attendanceClassesTaskDetailDtos = attendanceClassesTaskDetailInnerServiceSMOImpl.queryAttendanceClassesTaskDetails(attendanceClassesTaskDetailDto);

        if (attendanceClassesTaskDetailDtos == null || attendanceClassesTaskDetailDtos.size() < 1) {
            context.setResponseEntity(ResultVo.error("不是考勤范围内"));
            return;
        }

        String photo = "";
        if (reqJson.containsKey("photo") && !StringUtil.isEmpty(reqJson.getString("photo"))) {
            FileDto fileDto = new FileDto();
            fileDto.setFileId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_file_id));
            fileDto.setFileName(fileDto.getFileId());
            fileDto.setContext(reqJson.getString("photo"));
            fileDto.setSuffix("jpeg");
            fileDto.setCommunityId("-1");
            photo = fileInnerServiceSMOImpl.saveFile(fileDto);
        }

        //当前考勤的 记录
        AttendanceClassesTaskDetailDto nowAttendanceClassesTaskDetailDto = attendanceClassesTaskDetailDtos.get(0);

        AttendanceClassesTaskDetailPo attendanceClassesTaskDetailPo = new AttendanceClassesTaskDetailPo();
        attendanceClassesTaskDetailPo.setDetailId(nowAttendanceClassesTaskDetailDto.getDetailId());
        attendanceClassesTaskDetailPo.setCheckTime(reqJson.getString("clockTime"));
        attendanceClassesTaskDetailPo.setState(getState(nowAttendanceClassesTaskDetailDto, DateUtil.getDateFromStringA(reqJson.getString("clockTime"))));
        attendanceClassesTaskDetailPo.setFacePath(photo);
        flag = attendanceClassesTaskDetailInnerServiceSMOImpl.updateAttendanceClassesTaskDetail(attendanceClassesTaskDetailPo);

        if (flag < 1) {
            throw new CmdException("考勤失败");
        }


        attendanceClassesTaskDetailDto = new AttendanceClassesTaskDetailDto();
        attendanceClassesTaskDetailDto.setTaskId(nowAttendanceClassesTaskDetailDto.getTaskId());
        attendanceClassesTaskDetailDto.setState(AttendanceClassesTaskDetailDto.STATE_WAIT);
        int count = attendanceClassesTaskDetailInnerServiceSMOImpl.queryAttendanceClassesTaskDetailsCount(attendanceClassesTaskDetailDto);
        AttendanceClassesTaskPo attendanceClassesTaskPo = new AttendanceClassesTaskPo();
        attendanceClassesTaskPo.setTaskId(nowAttendanceClassesTaskDetailDto.getTaskId());
        if (count > 0) {
            attendanceClassesTaskPo.setState(AttendanceClassesTaskDto.STATE_DOING);
        } else {
            attendanceClassesTaskPo.setState(AttendanceClassesTaskDto.STATE_FINISH);
        }

        flag = attendanceClassesTaskInnerServiceSMOImpl.updateAttendanceClassesTask(attendanceClassesTaskPo);

        if (flag < 1) {
            throw new CmdException("考勤失败");
        }
    }

    /**
     * 考勤状态计算
     *
     * @param nowAttendanceClassesTaskDetailDto
     * @param clockTime
     * @return
     */
    private String getState(AttendanceClassesTaskDetailDto nowAttendanceClassesTaskDetailDto, Date clockTime) {

        Date value = DateUtil.getDateFromStringA(nowAttendanceClassesTaskDetailDto.getValue());

        if (AttendanceClassesTaskDetailDto.SPEC_CD_START.equals(nowAttendanceClassesTaskDetailDto.getSpecCd())) {

            if (clockTime.after(value)) {
                return AttendanceClassesTaskDetailDto.STATE_LATE;
            }
            return AttendanceClassesTaskDetailDto.STATE_NORMAL;
        }


        if (clockTime.before(value)) {
            return AttendanceClassesTaskDetailDto.STATE_LEAVE;
        }
        return AttendanceClassesTaskDetailDto.STATE_NORMAL;
    }
}
