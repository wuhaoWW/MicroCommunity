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
package com.java110.job.adapt.hcIot.staff;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.dto.attendanceClasses.AttendanceClassesDto;
import com.java110.dto.file.FileDto;
import com.java110.dto.file.FileRelDto;
import com.java110.dto.machine.MachineDto;
import com.java110.dto.org.OrgStaffRelDto;
import com.java110.entity.order.Business;
import com.java110.intf.common.IAttendanceClassesInnerServiceSMO;
import com.java110.intf.common.IFileInnerServiceSMO;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.common.IMachineV1InnerServiceSMO;
import com.java110.intf.user.IOrgStaffRelInnerServiceSMO;
import com.java110.job.adapt.DatabusAdaptImpl;
import com.java110.job.adapt.hcIot.asyn.IIotSendAsyn;
import com.java110.po.store.StoreUserPo;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * HC iot 车辆同步适配器
 * <p>
 * 接口协议地址： https://gitee.com/java110/MicroCommunityThings/blob/master/back/docs/api.md
 *
 * @desc add by 吴学文 18:58
 */
@Component(value = "addStaffToIotAdapt")
public class AddStaffToIotAdapt extends DatabusAdaptImpl {

    @Autowired
    private IIotSendAsyn hcStoreUserAsynImpl;

    @Autowired
    private IOrgStaffRelInnerServiceSMO orgStaffRelInnerServiceSMOImpl;

    @Autowired
    private IAttendanceClassesInnerServiceSMO attendanceClassesInnerServiceSMOImpl;

    @Autowired
    private IMachineV1InnerServiceSMO machineV1InnerServiceSMOImpl;


    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    @Autowired
    private IFileInnerServiceSMO fileInnerServiceSMOImpl;


    /**
     * accessToken={access_token}
     * &extCommunityUuid=01000
     * &extCommunityId=1
     * &devSn=111111111
     * &name=设备名称
     * &positionType=0
     * &positionUuid=1
     *
     * @param business   当前处理业务
     * @param businesses 所有业务信息
     */
    @Override
    public void execute(Business business, List<Business> businesses) {
        JSONObject data = business.getData();
        JSONArray businessStoreUsers = new JSONArray();
        if (data.containsKey(StoreUserPo.class.getSimpleName())) {
            Object bObj = data.get(StoreUserPo.class.getSimpleName());
            if (bObj instanceof JSONObject) {

                businessStoreUsers.add(bObj);
            } else if (bObj instanceof List) {
                businessStoreUsers = JSONArray.parseArray(JSONObject.toJSONString(bObj));
            } else {
                businessStoreUsers = (JSONArray) bObj;
            }
        } else {
            if (data instanceof JSONObject) {
                businessStoreUsers.add(data);
            }
        }
        //JSONObject businessStoreUser = data.getJSONObject("businessStoreUser");
        for (int bStoreUserIndex = 0; bStoreUserIndex < businessStoreUsers.size(); bStoreUserIndex++) {
            JSONObject businessStoreUser = businessStoreUsers.getJSONObject(bStoreUserIndex);
            doSendStoreUser(business, businessStoreUser);
        }
    }

    private void doSendStoreUser(Business business, JSONObject businessStoreUser) {

        StoreUserPo storeUserPo = BeanConvertUtil.covertBean(businessStoreUser, StoreUserPo.class);

        //查询员工部门
        //查询员工部门
        OrgStaffRelDto orgStaffRelDto = new OrgStaffRelDto();
        orgStaffRelDto.setStaffId(storeUserPo.getUserId());
        orgStaffRelDto.setStoreId(storeUserPo.getStoreId());
        List<OrgStaffRelDto> orgStaffRelDtos = orgStaffRelInnerServiceSMOImpl.queryOrgStaffRels(orgStaffRelDto);


        if(orgStaffRelDtos == null || orgStaffRelDtos.size()<1){
            throw new IllegalArgumentException("员工未包含组织");
        }

        for (OrgStaffRelDto tmpOrgStaffRelDto : orgStaffRelDtos) {

            //查询员工部门是否参与考勤
            AttendanceClassesDto attendanceClassesDto = new AttendanceClassesDto();
            attendanceClassesDto.setClassesObjType(AttendanceClassesDto.CLASSES_OBJ_TYPE_PARTMENT);
            attendanceClassesDto.setClassesObjId(tmpOrgStaffRelDto.getOrgId());
            List<AttendanceClassesDto> attendanceClassesDtos = attendanceClassesInnerServiceSMOImpl.queryAttendanceClassess(attendanceClassesDto);

            //员工部门没有考勤，不用处理
            if (attendanceClassesDtos == null || attendanceClassesDtos.size() < 1) {
                continue;
            }

            MachineDto machineDto = new MachineDto();
            machineDto.setLocationObjId(tmpOrgStaffRelDto.getOrgId());
            machineDto.setMachineTypeCd(MachineDto.MACHINE_TYPE_ATTENDANCE);
            List<MachineDto> machineDtos = machineV1InnerServiceSMOImpl.queryMachines(machineDto);

            String img = getStaffPhoto(orgStaffRelDtos.get(0));

            JSONObject storeUserObj = null;
            List<JSONObject> storeUserObjs = new ArrayList<>();
            for (AttendanceClassesDto tmpAttendanceClassesDto : attendanceClassesDtos) {

                storeUserObj = new JSONObject();
                storeUserObj.put("extClassesId", tmpAttendanceClassesDto.getClassesId());
                storeUserObj.put("extStaffId", tmpOrgStaffRelDto.getStaffId());
                storeUserObj.put("staffName", tmpOrgStaffRelDto.getStaffName());
                storeUserObj.put("departmentId", tmpOrgStaffRelDto.getOrgId());
                storeUserObj.put("departmentName", tmpOrgStaffRelDto.getOrgName());
                if (machineDtos != null && machineDtos.size() < 1) {
                    storeUserObj.put("machineCode", machineDtos.get(0).getMachineCode());
                    storeUserObj.put("extMachineId", machineDtos.get(0).getMachineId());
                    storeUserObj.put("extCommunityId", machineDtos.get(0).getCommunityId());
                }
                storeUserObj.put("faceBase64", img);
                storeUserObjs.add(storeUserObj);
            }
            JSONObject postParameters = new JSONObject();
            postParameters.put("classesName", attendanceClassesDtos.get(0).getClassesName());
            postParameters.put("extClassesId", attendanceClassesDtos.get(0).getClassesId());
            postParameters.put("extCommunityId", "-1");
            hcStoreUserAsynImpl.addAttendanceStaff(postParameters, storeUserObjs);
        }
    }

    private String getStaffPhoto(OrgStaffRelDto orgStaffRelDto) {

        FileRelDto fileRelDto = new FileRelDto();
        fileRelDto.setObjId(orgStaffRelDto.getStaffId());
        fileRelDto.setRelTypeCd("12000");
        List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
        if (fileRelDtos == null || fileRelDtos.size() != 1) {
            return "";
        }
        FileDto fileDto = new FileDto();
        fileDto.setFileId(fileRelDtos.get(0).getFileSaveName());
        fileDto.setFileSaveName(fileRelDtos.get(0).getFileSaveName());
        List<FileDto> fileDtos = fileInnerServiceSMOImpl.queryFiles(fileDto);
        if (fileDtos == null || fileDtos.size() != 1) {
            return "";
        }

        return fileDtos.get(0).getContext();
    }
}
