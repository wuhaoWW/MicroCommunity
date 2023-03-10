package com.java110.job.task.attendance;

import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.attendanceClasses.AttendanceClassesDto;
import com.java110.dto.attendanceClasses.AttendanceClassesTaskDetailDto;
import com.java110.dto.attendanceClasses.AttendanceClassesTaskDto;
import com.java110.dto.org.OrgStaffRelDto;
import com.java110.dto.scheduleClassesStaff.ScheduleClassesStaffDto;
import com.java110.dto.scheduleClassesTime.ScheduleClassesTimeDto;
import com.java110.dto.store.StoreDto;
import com.java110.dto.task.TaskDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.common.IAttendanceClassesInnerServiceSMO;
import com.java110.intf.common.IAttendanceClassesTaskDetailInnerServiceSMO;
import com.java110.intf.common.IAttendanceClassesTaskInnerServiceSMO;
import com.java110.intf.store.IOrgStaffRelV1InnerServiceSMO;
import com.java110.intf.store.IScheduleClassesStaffV1InnerServiceSMO;
import com.java110.intf.store.IStoreV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.job.quartz.TaskSystemQuartz;
import com.java110.po.attendanceClassesTask.AttendanceClassesTaskPo;
import com.java110.po.attendanceClassesTaskDetail.AttendanceClassesTaskDetailPo;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 考勤任务生成
 */
@Component
public class GeneratorAttendanceTaskTemplate extends TaskSystemQuartz {

    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMOImpl;

    @Autowired
    private IAttendanceClassesInnerServiceSMO attendanceClassesInnerServiceSMOImpl;

    @Autowired
    private IAttendanceClassesTaskInnerServiceSMO attendanceClassesTaskInnerServiceSMOImpl;

    @Autowired
    private IAttendanceClassesTaskDetailInnerServiceSMO attendanceClassesTaskDetailInnerServiceSMOImpl;

    @Autowired
    private IOrgStaffRelV1InnerServiceSMO orgStaffRelV1InnerServiceSMOImpl;

    @Autowired
    private IScheduleClassesStaffV1InnerServiceSMO scheduleClassesStaffV1InnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    @Override
    protected void process(TaskDto taskDto) throws Exception {
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreTypeCd(StoreDto.STORE_TYPE_PROPERTY);
        List<StoreDto> storeDtos = storeV1InnerServiceSMOImpl.queryStores(storeDto);

        if (storeDtos == null || storeDtos.size() < 1) {
            return;
        }

        for (StoreDto tmpStoreDto : storeDtos) {
            try {
                generatorAttendance(tmpStoreDto);
            } catch (Exception e) {
                logger.error("生成考勤任务失败", e);
            }
        }

    }

    /**
     * 考勤任务
     *
     * @param tmpStoreDto
     */
    private void generatorAttendance(StoreDto tmpStoreDto) {

        AttendanceClassesDto attendanceClassesDto = new AttendanceClassesDto();
        attendanceClassesDto.setStoreId(tmpStoreDto.getStoreId());
        List<AttendanceClassesDto> attendanceClassesDtos = attendanceClassesInnerServiceSMOImpl.queryAttendanceClassess(attendanceClassesDto);

        if (attendanceClassesDtos == null || attendanceClassesDtos.size() < 1) {
            return;
        }

        for (AttendanceClassesDto tmpAttendanceClassesDto : attendanceClassesDtos) {
            try {
                doGeneratorAttendance(tmpStoreDto, tmpAttendanceClassesDto);
            } catch (Exception e) {
                logger.error("生成考勤任务失败", e);
            }
        }

    }

    /**
     * 生成巡检任务
     *
     * @param tmpStoreDto
     * @param tmpAttendanceClassesDto
     */
    private void doGeneratorAttendance(StoreDto tmpStoreDto, AttendanceClassesDto tmpAttendanceClassesDto) {

        //查询组织 关联的员工

        OrgStaffRelDto orgStaffRelDto = new OrgStaffRelDto();
        orgStaffRelDto.setOrgId(tmpAttendanceClassesDto.getClassesObjId());
        orgStaffRelDto.setStoreId(tmpStoreDto.getStoreId());
        List<OrgStaffRelDto> orgStaffRelDtos = orgStaffRelV1InnerServiceSMOImpl.queryOrgStaffRels(orgStaffRelDto);
        if (orgStaffRelDtos == null || orgStaffRelDtos.size() < 1) {
            return;
        }

        for (OrgStaffRelDto tmpOrgStaffRelDto : orgStaffRelDtos) {
            try {
                doGeneratorStaffAttendance(tmpStoreDto, tmpAttendanceClassesDto, tmpOrgStaffRelDto);
            } catch (Exception e) {
                logger.error("员工生成考勤任务失败" + tmpOrgStaffRelDto.getStaffId(), e);
            }
        }
    }

    /**
     * 生成员工 考勤
     *
     * @param tmpStoreDto
     * @param tmpAttendanceClassesDto
     * @param tmpOrgStaffRelDto
     */
    private void doGeneratorStaffAttendance(StoreDto tmpStoreDto, AttendanceClassesDto tmpAttendanceClassesDto, OrgStaffRelDto tmpOrgStaffRelDto) {

        ScheduleClassesStaffDto scheduleClassesStaffDto = new ScheduleClassesStaffDto();
        scheduleClassesStaffDto.setStaffId(tmpOrgStaffRelDto.getStaffId());
        scheduleClassesStaffDto.setToday(new Date());
        scheduleClassesStaffDto = scheduleClassesStaffV1InnerServiceSMOImpl.staffIsWork(scheduleClassesStaffDto);

        List<ScheduleClassesTimeDto> times = scheduleClassesStaffDto.getTimes();

        if (times == null || times.size() < 1) {
            return;
        }


        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        AttendanceClassesTaskDto attendanceClassesTaskDto = new AttendanceClassesTaskDto();
        attendanceClassesTaskDto.setStaffId(tmpOrgStaffRelDto.getStaffId());
        attendanceClassesTaskDto.setClassId(tmpAttendanceClassesDto.getClassesId());
        attendanceClassesTaskDto.setTaskYear(year + "");
        attendanceClassesTaskDto.setTaskMonth(month + "");
        attendanceClassesTaskDto.setTaskDay(day + "");
        List<AttendanceClassesTaskDto> attendanceClassesTaskDtos = attendanceClassesTaskInnerServiceSMOImpl.queryAttendanceClassesTasks(attendanceClassesTaskDto);

        //已经生成过考勤任务
        if (attendanceClassesTaskDtos != null && attendanceClassesTaskDtos.size() > 0) {
            return;
        }

        UserDto userDto = new UserDto();
        userDto.setUserId(tmpOrgStaffRelDto.getStaffId());
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        Assert.listOnlyOne(userDtos,"员工不存在");


        AttendanceClassesTaskPo attendanceClassesTaskPo = new AttendanceClassesTaskPo();
        attendanceClassesTaskPo.setStaffId(tmpOrgStaffRelDto.getStaffId());
        attendanceClassesTaskPo.setClassId(tmpAttendanceClassesDto.getClassesId());
        attendanceClassesTaskPo.setTaskYear(year + "");
        attendanceClassesTaskPo.setTaskMonth(month + "");
        attendanceClassesTaskPo.setTaskDay(day + "");
        attendanceClassesTaskPo.setState(AttendanceClassesTaskDto.STATE_WAIT);
        attendanceClassesTaskPo.setStaffName(userDtos.get(0).getName());
        attendanceClassesTaskPo.setStoreId(tmpStoreDto.getStoreId());
        attendanceClassesTaskPo.setTaskId(GenerateCodeFactory.getGeneratorId("11"));

        int flag = attendanceClassesTaskInnerServiceSMOImpl.saveAttendanceClassesTask(attendanceClassesTaskPo);

        if (flag < 1) {
            throw new IllegalArgumentException("保存考勤任务失败");
        }

        for (ScheduleClassesTimeDto tmpScheduleClassesTimeDto : times) {
            doGeneratorStaffAttendanceTime(tmpStoreDto, tmpAttendanceClassesDto, tmpOrgStaffRelDto, tmpScheduleClassesTimeDto, attendanceClassesTaskPo);
        }

    }

    /**
     * 生成员工考勤任务
     *
     * @param tmpStoreDto
     * @param tmpAttendanceClassesDto
     * @param tmpOrgStaffRelDto
     * @param tmpScheduleClassesTimeDto
     */
    private void doGeneratorStaffAttendanceTime(StoreDto tmpStoreDto, AttendanceClassesDto tmpAttendanceClassesDto, OrgStaffRelDto tmpOrgStaffRelDto,
                                                ScheduleClassesTimeDto tmpScheduleClassesTimeDto, AttendanceClassesTaskPo attendanceClassesTaskPo) {

        String curDate = DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_B);

        String startTimeStr = tmpScheduleClassesTimeDto.getStartTime();
        String endTimeStr = tmpScheduleClassesTimeDto.getEndTime();


        //
        //alter table attendance_classes_task_detail add COLUMN leave_value varchar(12) not null comment '正常或者早退时间，spec_cd 1001 是正常考勤 2002 是早退时间';
        //
        //alter table attendance_classes_task_detail add COLUMN late_value varchar(12) not null comment '正常或者早退时间，spec_cd 1001 是迟到 2002 是正常时间';

        String value = curDate + " " + startTimeStr + ":00";
        Date valueDate = DateUtil.getDateFromStringA(value);

        int timeOffset = Integer.parseInt(tmpAttendanceClassesDto.getTimeOffset());

        AttendanceClassesTaskDetailPo attendanceClassesTaskDetailPo = new AttendanceClassesTaskDetailPo();
        attendanceClassesTaskDetailPo.setDetailId(GenerateCodeFactory.getGeneratorId("12"));
        attendanceClassesTaskDetailPo.setTaskId(attendanceClassesTaskPo.getTaskId());
        attendanceClassesTaskDetailPo.setSpecCd(AttendanceClassesTaskDetailDto.SPEC_CD_START);
        attendanceClassesTaskDetailPo.setValue(curDate + " " + startTimeStr + ":00");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(valueDate);
        calendar.add(Calendar.MINUTE, timeOffset * -1);
        attendanceClassesTaskDetailPo.setLeaveValue(DateUtil.getFormatTimeString(calendar.getTime(), DateUtil.DATE_FORMATE_STRING_A));

        int lateOffset = Integer.parseInt(tmpAttendanceClassesDto.getLateOffset());
        calendar = Calendar.getInstance();
        calendar.setTime(valueDate);
        calendar.add(Calendar.MINUTE, lateOffset);
        attendanceClassesTaskDetailPo.setLateValue(DateUtil.getFormatTimeString(calendar.getTime(), DateUtil.DATE_FORMATE_STRING_A));
        attendanceClassesTaskDetailPo.setState(AttendanceClassesTaskDetailDto.STATE_WAIT);
        attendanceClassesTaskDetailPo.setStoreId(tmpStoreDto.getStoreId());

        int flag = attendanceClassesTaskDetailInnerServiceSMOImpl.saveAttendanceClassesTaskDetail(attendanceClassesTaskDetailPo);

        if (flag < 1) {
            throw new IllegalArgumentException("保存考勤任务失败");
        }

        value = curDate + " " + endTimeStr + ":00";
        valueDate = DateUtil.getDateFromStringA(value);

        int leaveOffset = Integer.parseInt(tmpAttendanceClassesDto.getLeaveOffset());

        attendanceClassesTaskDetailPo = new AttendanceClassesTaskDetailPo();
        attendanceClassesTaskDetailPo.setDetailId(GenerateCodeFactory.getGeneratorId("12"));
        attendanceClassesTaskDetailPo.setTaskId(attendanceClassesTaskPo.getTaskId());
        attendanceClassesTaskDetailPo.setSpecCd(AttendanceClassesTaskDetailDto.SPEC_CD_END);
        attendanceClassesTaskDetailPo.setValue(curDate + " " + endTimeStr + ":00");

        calendar = Calendar.getInstance();
        calendar.setTime(valueDate);
        calendar.add(Calendar.MINUTE, leaveOffset * -1);
        attendanceClassesTaskDetailPo.setLeaveValue(DateUtil.getFormatTimeString(calendar.getTime(), DateUtil.DATE_FORMATE_STRING_A));
        calendar = Calendar.getInstance();
        calendar.setTime(valueDate);
        calendar.add(Calendar.MINUTE, timeOffset);
        attendanceClassesTaskDetailPo.setLateValue(DateUtil.getFormatTimeString(calendar.getTime(), DateUtil.DATE_FORMATE_STRING_A));
        attendanceClassesTaskDetailPo.setState(AttendanceClassesTaskDetailDto.STATE_WAIT);
        attendanceClassesTaskDetailPo.setStoreId(tmpStoreDto.getStoreId());

        flag = attendanceClassesTaskDetailInnerServiceSMOImpl.saveAttendanceClassesTaskDetail(attendanceClassesTaskDetailPo);

        if (flag < 1) {
            throw new IllegalArgumentException("保存考勤任务失败");
        }


    }
}
