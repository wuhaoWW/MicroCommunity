package com.java110.community.cmd.ownerRepair;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.file.FileRelDto;
import com.java110.dto.repair.RepairDto;
import com.java110.dto.repair.RepairUserDto;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.community.IRepairInnerServiceSMO;
import com.java110.intf.community.IRepairUserInnerServiceSMO;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import com.java110.vo.api.junkRequirement.PhotoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Java110Cmd(serviceCode = "ownerRepair.listOwnerRepairs")
public class ListOwnerRepairsCmd extends Cmd {

    @Autowired
    private IRepairInnerServiceSMO repairInnerServiceSMOImpl;

    @Autowired
    private IRepairUserInnerServiceSMO repairUserInnerServiceSMOImpl;

    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        super.validatePageInfo(reqJson);
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        RepairDto ownerRepairDto = BeanConvertUtil.covertBean(reqJson, RepairDto.class);

        if (!StringUtil.isEmpty(ownerRepairDto.getRoomId()) && ownerRepairDto.getRoomId().contains(",")) {
            String[] roomIds = ownerRepairDto.getRoomId().split(",");
            ownerRepairDto.setRoomIds(roomIds);
            ownerRepairDto.setRoomId("");
        }
        //PC???????????????PC????????????H5?????????
        //??????????????????????????? ?????????????????????????????????
        if (!StringUtil.isEmpty(ownerRepairDto.getReqSource()) && ownerRepairDto.getReqSource().equals("mobile")) {
            ownerRepairDto.setState(RepairDto.STATE_WAIT);
        }
        //pc?????????????????? ?????????PC?????????????????????????????????????????????
        if (!StringUtil.isEmpty(ownerRepairDto.getReqSource()) && ownerRepairDto.getReqSource().equals("pc_mobile")) {
            String[] repair_channel = {RepairDto.REPAIR_CHANNEL_STAFF, RepairDto.REPAIR_CHANNEL_TEL};
            ownerRepairDto.setRepairChannels(Arrays.asList(repair_channel));
        }

        int count = repairInnerServiceSMOImpl.queryRepairsCount(ownerRepairDto);

        List<RepairDto> ownerRepairs = new ArrayList<>();
        if (count > 0) {
            List<RepairDto> repairDtos = repairInnerServiceSMOImpl.queryRepairs(ownerRepairDto);
            for (RepairDto repairDto : repairDtos) {
                //????????????????????????
                String appraiseScoreNumber = repairDto.getAppraiseScore();
                Double appraiseScoreNum = 0.0;
                if (!StringUtil.isEmpty(appraiseScoreNumber)) {
                    appraiseScoreNum = Double.parseDouble(appraiseScoreNumber);
                }
                int appraiseScore = (int) Math.ceil(appraiseScoreNum);
                //????????????????????????
                String doorSpeedScoreNumber = repairDto.getDoorSpeedScore();
                Double doorSpeedScoreNum = 0.0;
                if (!StringUtil.isEmpty(doorSpeedScoreNumber)) {
                    doorSpeedScoreNum = Double.parseDouble(doorSpeedScoreNumber);
                }
                int doorSpeedScore = (int) Math.ceil(doorSpeedScoreNum);
                //???????????????????????????
                String repairmanServiceScoreNumber = repairDto.getRepairmanServiceScore();
                Double repairmanServiceScoreNum = 0.0;
                if (!StringUtil.isEmpty(repairmanServiceScoreNumber)) {
                    repairmanServiceScoreNum = Double.parseDouble(repairmanServiceScoreNumber);
                }
                int repairmanServiceScore = (int) Math.ceil(repairmanServiceScoreNum);
                //???????????????
                double averageNumber = (appraiseScoreNum + doorSpeedScoreNum + repairmanServiceScoreNum) / 3.0;
                BigDecimal averageNum = new BigDecimal(averageNumber);
                Double average = averageNum.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                repairDto.setAppraiseScore(String.valueOf(appraiseScore));
                repairDto.setDoorSpeedScore(String.valueOf(doorSpeedScore));
                repairDto.setRepairmanServiceScore(String.valueOf(repairmanServiceScore));
                repairDto.setAverage(String.valueOf(average));
                ownerRepairs.add(repairDto);
            }
            refreshRepair(ownerRepairs);
        } else {
            ownerRepairs = new ArrayList<>();
        }
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, ownerRepairs);
        context.setResponseEntity(responseEntity);
    }

    private void refreshRepair(List<RepairDto> ownerRepairs) {

        List<String> repairIds = new ArrayList<>();
        for (RepairDto apiOwnerRepairDataVo : ownerRepairs) {
            repairIds.add(apiOwnerRepairDataVo.getRepairId());
        }

        if (repairIds.size() < 1) {
            return;
        }
        RepairUserDto repairUserDto = new RepairUserDto();
        repairUserDto.setRepairIds(repairIds.toArray(new String[repairIds.size()]));
        List<RepairUserDto> repairUserDtos = repairUserInnerServiceSMOImpl.queryRepairUsers(repairUserDto);

        for (RepairUserDto tmpRepairUserDto : repairUserDtos) {
            for (RepairDto apiOwnerRepairDataVo : ownerRepairs) {
                if (tmpRepairUserDto.getRepairId().equals(apiOwnerRepairDataVo.getRepairId())) {
                    apiOwnerRepairDataVo.setStaffId(tmpRepairUserDto.getUserId());
                    //apiOwnerRepairDataVo.setStatmpRepairUserDto.getUserName());
                }
            }
        }

        //??????????????????
        List<PhotoVo> photoVos = null;
        List<PhotoVo> repairPhotos = null;  //????????????????????????
        List<PhotoVo> beforePhotos = null;  //???????????????
        List<PhotoVo> afterPhotos = null;  //???????????????
        PhotoVo photoVo = null;
        String imgUrl = MappingCache.getValue("IMG_PATH");

        for (RepairDto repairDto : ownerRepairs) {
            FileRelDto fileRelDto = new FileRelDto();
            fileRelDto.setObjId(repairDto.getRepairId());
            List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
            photoVos = new ArrayList<>();
            repairPhotos = new ArrayList<>();
            beforePhotos = new ArrayList<>();
            afterPhotos = new ArrayList<>();
            for (FileRelDto tmpFileRelDto : fileRelDtos) {
                photoVo = new PhotoVo();
                photoVo.setUrl(tmpFileRelDto.getFileRealName());
                photoVo.setRelTypeCd(tmpFileRelDto.getRelTypeCd());
                photoVos.add(photoVo);
                if (tmpFileRelDto.getRelTypeCd().equals(FileRelDto.REL_TYPE_CD_REPAIR)) {  //????????????
                    photoVo = new PhotoVo();
                    if(!tmpFileRelDto.getFileRealName().startsWith("http")){
                        photoVo.setUrl(imgUrl+tmpFileRelDto.getFileRealName());
                    }else{
                        photoVo.setUrl(tmpFileRelDto.getFileRealName());
                    }
                    photoVo.setRelTypeCd(tmpFileRelDto.getRelTypeCd());
                    repairPhotos.add(photoVo);  //????????????
                } else if (tmpFileRelDto.getRelTypeCd().equals(FileRelDto.BEFORE_REPAIR_PHOTOS)) {  //???????????????
                    photoVo = new PhotoVo();
                    if(!tmpFileRelDto.getFileRealName().startsWith("http")){
                        photoVo.setUrl(imgUrl+tmpFileRelDto.getFileRealName());
                    }else{
                        photoVo.setUrl(tmpFileRelDto.getFileRealName());
                    }                    photoVo.setRelTypeCd(tmpFileRelDto.getRelTypeCd());
                    beforePhotos.add(photoVo);  //???????????????
                } else if (tmpFileRelDto.getRelTypeCd().equals(FileRelDto.AFTER_REPAIR_PHOTOS)) {  //???????????????
                    photoVo = new PhotoVo();
                    if(!tmpFileRelDto.getFileRealName().startsWith("http")){
                        photoVo.setUrl(imgUrl+tmpFileRelDto.getFileRealName());
                    }else{
                        photoVo.setUrl(tmpFileRelDto.getFileRealName());
                    }                    photoVo.setRelTypeCd(tmpFileRelDto.getRelTypeCd());
                    afterPhotos.add(photoVo);
                }
            }
            repairDto.setPhotos(photoVos);
            repairDto.setRepairPhotos(repairPhotos);
            repairDto.setBeforePhotos(beforePhotos);
            repairDto.setAfterPhotos(afterPhotos);
        }
    }
}
