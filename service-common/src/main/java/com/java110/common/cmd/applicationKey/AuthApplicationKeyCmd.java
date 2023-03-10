package com.java110.common.cmd.applicationKey;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.DataFlowContext;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.RoomDto;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.machine.ApplicationKeyDto;
import com.java110.dto.machine.MachineDto;
import com.java110.dto.unit.FloorAndUnitDto;
import com.java110.intf.common.IApplicationKeyInnerServiceSMO;
import com.java110.intf.common.IMachineInnerServiceSMO;
import com.java110.intf.common.IMachineRecordV1InnerServiceSMO;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.community.IFloorInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.community.IUnitInnerServiceSMO;
import com.java110.po.machine.MachineRecordPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "applicationKey.authApplicationKeys")
public class AuthApplicationKeyCmd extends Cmd {

    @Autowired
    private IMachineInnerServiceSMO machineInnerServiceSMOImpl;

    @Autowired
    private IApplicationKeyInnerServiceSMO applicationKeyInnerServiceSMOImpl;

    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;


    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    @Autowired
    private IUnitInnerServiceSMO unitInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IMachineRecordV1InnerServiceSMO machineRecordV1InnerServiceSMOImpl;


    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        Assert.hasKeyAndValue(reqJson, "communityId", "????????????????????????");
        Assert.hasKeyAndValue(reqJson, "machineCode", "??????????????????????????????");
        Assert.hasKeyAndValue(reqJson, "pwd", "????????????????????????");
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        //1.0 ?????? ??????ID??? ????????????????????????ID

        MachineDto machineDto = new MachineDto();
        machineDto.setMachineCode(reqJson.getString("machineCode"));
        machineDto.setCommunityId(reqJson.getString("communityId"));
        List<MachineDto> machineDtos = machineInnerServiceSMOImpl.queryMachines(machineDto);

        Assert.listOnlyOne(machineDtos, "?????????????????????????????????Id ???????????????");

        ApplicationKeyDto applicationKeyDto = new ApplicationKeyDto();
        applicationKeyDto.setCommunityId(reqJson.getString("communityId"));
        applicationKeyDto.setMachineId(machineDtos.get(0).getMachineId());
        applicationKeyDto.setPwd(reqJson.getString("pwd"));

        int count = applicationKeyInnerServiceSMOImpl.queryApplicationKeysCount(applicationKeyDto);

        ResponseEntity<String> responseEntity = null;
        JSONObject reqParam = new JSONObject();
        reqParam.put("communityId", reqJson.getString("communityId"));
        reqParam.put("machineId", machineDtos.get(0).getMachineId());
        reqParam.put("machineCode", reqJson.getString("machineCode"));
        if (count > 0) {
            reqParam.put("recordTypeCd", "8888");
            responseEntity = new ResponseEntity<String>("??????", HttpStatus.OK);
        } else {
            reqParam.put("recordTypeCd", "6666");
            responseEntity = new ResponseEntity<String>("????????????", HttpStatus.UNAUTHORIZED);
        }
        context.setResponseEntity(responseEntity);

        addMachineRecord(reqParam);

    }

    /**
     * ??????????????????
     *
     * @param paramInJson ???????????????????????????
     * @return ?????????????????????????????????
     */
    public void addMachineRecord(JSONObject paramInJson) {

        //paramInJson.put("fileTime", DateUtil.getFormatTimeString(new Date(), DateUtil.DATE_FORMATE_STRING_A));
        paramInJson.put("name", "??????");
        paramInJson.put("tel", "");
        paramInJson.put("idCard", "");
        paramInJson.put("openTypeCd", "2000");
        paramInJson.put("machineRecordId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_machineRecordId));
        MachineRecordPo machineRecordPo = BeanConvertUtil.covertBean(paramInJson, MachineRecordPo.class);
        int flag = machineRecordV1InnerServiceSMOImpl.saveMachineRecord(machineRecordPo);
        if (flag < 1) {
            throw new CmdException("????????????????????????");
        }
    }

    private void refreshMachines(List<ApplicationKeyDto> applicationKeyDtos) {

        //???????????? ??????
        refreshCommunitys(applicationKeyDtos);

        //????????????????????????
        refreshUnits(applicationKeyDtos);

        //???????????? ????????????
        refreshRooms(applicationKeyDtos);

    }

    /**
     * ??????????????????
     *
     * @param applicationKeyDtos ????????????
     * @return ??????userIds ??????
     */
    private void refreshCommunitys(List<ApplicationKeyDto> applicationKeyDtos) {
        List<String> communityIds = new ArrayList<String>();
        List<ApplicationKeyDto> tmpApplicationKeyDtos = new ArrayList<>();
        for (ApplicationKeyDto applicationKeyDto : applicationKeyDtos) {

            if (!"2000".equals(applicationKeyDto.getLocationTypeCd())
                    && !"3000".equals(applicationKeyDto.getLocationTypeCd())
                    ) {
                communityIds.add(applicationKeyDto.getLocationObjId());
                tmpApplicationKeyDtos.add(applicationKeyDto);
            }
        }

        if (communityIds.size() < 1) {
            return;
        }
        String[] tmpCommunityIds = communityIds.toArray(new String[communityIds.size()]);

        CommunityDto communityDto = new CommunityDto();
        communityDto.setCommunityIds(tmpCommunityIds);
        //?????? userId ??????????????????
        List<CommunityDto> communityDtos = communityInnerServiceSMOImpl.queryCommunitys(communityDto);

        for (ApplicationKeyDto applicationKeyDto : tmpApplicationKeyDtos) {
            for (CommunityDto tmpCommunityDto : communityDtos) {
                if (applicationKeyDto.getLocationObjId().equals(tmpCommunityDto.getCommunityId())) {
                    applicationKeyDto.setLocationObjName(tmpCommunityDto.getName() + " " + applicationKeyDto.getLocationTypeName());
                }
            }
        }
    }


    /**
     * ??????????????????
     *
     * @param applicationKeyDtos ????????????
     * @return ??????userIds ??????
     */
    private void refreshUnits(List<ApplicationKeyDto> applicationKeyDtos) {
        List<String> unitIds = new ArrayList<String>();
        List<ApplicationKeyDto> tmpApplicationKeyDtos = new ArrayList<>();
        for (ApplicationKeyDto applicationKeyDto : applicationKeyDtos) {

            if ("2000".equals(applicationKeyDto.getLocationTypeCd())) {
                unitIds.add(applicationKeyDto.getLocationObjId());
                tmpApplicationKeyDtos.add(applicationKeyDto);
            }
        }

        if (unitIds.size() < 1) {
            return;
        }
        String[] tmpUnitIds = unitIds.toArray(new String[unitIds.size()]);

        FloorAndUnitDto floorAndUnitDto = new FloorAndUnitDto();
        floorAndUnitDto.setUnitIds(tmpUnitIds);
        //?????? userId ??????????????????
        List<FloorAndUnitDto> unitDtos = unitInnerServiceSMOImpl.getFloorAndUnitInfo(floorAndUnitDto);

        for (ApplicationKeyDto applicationKeyDto : tmpApplicationKeyDtos) {
            for (FloorAndUnitDto tmpUnitDto : unitDtos) {
                if (applicationKeyDto.getLocationObjId().equals(tmpUnitDto.getUnitId())) {
                    applicationKeyDto.setLocationObjName(tmpUnitDto.getFloorNum() + "???" + tmpUnitDto.getUnitNum() + "??????");
                    BeanConvertUtil.covertBean(tmpUnitDto, applicationKeyDto);
                }
            }
        }
    }

    /**
     * ??????????????????
     *
     * @param applicationKeyDtos ????????????
     * @return ??????userIds ??????
     */
    private void refreshRooms(List<ApplicationKeyDto> applicationKeyDtos) {
        List<String> roomIds = new ArrayList<String>();
        List<ApplicationKeyDto> tmpApplicationKeyDtos = new ArrayList<>();
        for (ApplicationKeyDto applicationKeyDto : applicationKeyDtos) {

            if ("3000".equals(applicationKeyDto.getLocationTypeCd())) {
                roomIds.add(applicationKeyDto.getLocationObjId());
                tmpApplicationKeyDtos.add(applicationKeyDto);
            }
        }
        if (roomIds.size() < 1) {
            return;
        }
        String[] tmpRoomIds = roomIds.toArray(new String[roomIds.size()]);

        RoomDto roomDto = new RoomDto();
        roomDto.setRoomIds(tmpRoomIds);
        roomDto.setCommunityId(applicationKeyDtos.get(0).getCommunityId());
        //?????? userId ??????????????????
        List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);

        for (ApplicationKeyDto applicationKeyDto : tmpApplicationKeyDtos) {
            for (RoomDto tmpRoomDto : roomDtos) {
                if (applicationKeyDto.getLocationObjId().equals(tmpRoomDto.getRoomId())) {
                    applicationKeyDto.setLocationObjName(tmpRoomDto.getFloorNum() + "???" + tmpRoomDto.getUnitNum() + "??????" + tmpRoomDto.getRoomNum() + "???");
                    BeanConvertUtil.covertBean(tmpRoomDto, applicationKeyDto);
                }
            }
        }
    }
}
