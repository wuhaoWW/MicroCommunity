package com.java110.user.cmd.owner;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.file.FileDto;
import com.java110.dto.file.FileRelDto;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.intf.common.IFileInnerServiceSMO;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.user.IOwnerV1InnerServiceSMO;
import com.java110.intf.user.IOwnerAppUserInnerServiceSMO;
import com.java110.intf.user.IOwnerAppUserV1InnerServiceSMO;
import com.java110.intf.user.IOwnerAttrInnerServiceSMO;
import com.java110.intf.user.IOwnerInnerServiceSMO;
import com.java110.po.file.FileRelPo;
import com.java110.po.owner.OwnerAppUserPo;
import com.java110.po.owner.OwnerAttrPo;
import com.java110.po.owner.OwnerPo;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110Cmd(serviceCode = "owner.editOwner")
public class EditOwnerCmd extends Cmd {

    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    @Autowired
    private IOwnerAttrInnerServiceSMO ownerAttrInnerServiceSMOImpl;

    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerAppUserInnerServiceSMO ownerAppUserInnerServiceSMOImpl;

    @Autowired
    private IOwnerAppUserV1InnerServiceSMO ownerAppUserV1InnerServiceSMOImpl;

    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    @Autowired
    private IFileInnerServiceSMO fileInnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        Assert.jsonObjectHaveKey(reqJson, "memberId", "????????????????????????ownerId");
        Assert.jsonObjectHaveKey(reqJson, "name", "????????????????????????name");
//        Assert.jsonObjectHaveKey(reqJson, "age", "????????????????????????age");
        Assert.jsonObjectHaveKey(reqJson, "link", "????????????????????????link");
        Assert.jsonObjectHaveKey(reqJson, "sex", "????????????????????????sex");
        Assert.jsonObjectHaveKey(reqJson, "ownerTypeCd", "????????????????????????ownerTypeCd");
        Assert.jsonObjectHaveKey(reqJson, "communityId", "????????????????????????communityId");
        // Assert.jsonObjectHaveKey(paramIn, "idCard", "????????????????????????????????????");
        Assert.judgeAttrValue(reqJson);

        //???????????????(???????????????????????????)
        String link = reqJson.getString("link");
        if (!StringUtil.isEmpty(link) && link.contains("*")) {
            OwnerDto ownerDto = new OwnerDto();
            ownerDto.setOwnerId(reqJson.getString("ownerId"));
            //??????
            ownerDto.setOwnerTypeCd("1001");
            List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryOwners(ownerDto);
            Assert.listOnlyOne(ownerDtos, "???????????????????????????");
            link = ownerDtos.get(0).getLink();
            reqJson.put("link", link);
        }
        //??????????????????(??????????????????????????????)
        String idCard = reqJson.getString("idCard");

        if (!StringUtil.isEmpty(idCard) && idCard.contains("*")) {
            OwnerDto owner = new OwnerDto();
            owner.setOwnerId(reqJson.getString("ownerId"));
            //??????
            owner.setOwnerTypeCd("1001");
            List<OwnerDto> owners = ownerInnerServiceSMOImpl.queryOwners(owner);
            Assert.listOnlyOne(owners, "???????????????????????????");
            idCard = owners.get(0).getIdCard();
            reqJson.put("idCard", idCard);
        }

        String userValidate = MappingCache.getValue("USER_VALIDATE");

        if (!"ON".equals(userValidate)) {
            return;
        }

        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setLink(link);
        ownerDto.setCommunityId(reqJson.getString("communityId"));
        List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryAllOwners(ownerDto);
        if (ownerDtos != null && ownerDtos.size() > 1) {
            throw new IllegalArgumentException("?????????????????????????????????");
        } else if (ownerDtos != null && ownerDtos.size() == 1) {
            for (OwnerDto owner : ownerDtos) {
                if ((!StringUtil.isEmpty(reqJson.getString("ownerId")) && !owner.getOwnerId().equals(reqJson.getString("ownerId"))) || (!StringUtil.isEmpty(reqJson.getString("memberId")) && !owner.getMemberId().equals(reqJson.getString("memberId")))) {
                    throw new IllegalArgumentException("?????????????????????????????????");
                }
            }
        }
        if (!StringUtil.isEmpty(idCard)) {
            OwnerDto owner = new OwnerDto();
            owner.setIdCard(idCard);
            owner.setCommunityId(reqJson.getString("communityId"));
            List<OwnerDto> owners = ownerInnerServiceSMOImpl.queryAllOwners(owner);
            if (owners != null && owners.size() > 1) {
                throw new IllegalArgumentException("????????????????????????????????????");
            } else if (owners != null && owners.size() == 1) {
                for (OwnerDto ownerDto1 : owners) {
                    if ((!StringUtil.isEmpty(reqJson.getString("ownerId")) && !ownerDto1.getOwnerId().equals(reqJson.getString("ownerId"))) || (!StringUtil.isEmpty(reqJson.getString("memberId")) && !ownerDto1.getMemberId().equals(reqJson.getString("memberId")))) {
                        throw new IllegalArgumentException("????????????????????????????????????");
                    }
                }
            }
        }
    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        if (!reqJson.containsKey("ownerId") || OwnerDto.OWNER_TYPE_CD_OWNER.equals(reqJson.getString("ownerTypeCd"))) {
            reqJson.put("ownerId", reqJson.getString("memberId"));
        }

        //???????????? ?????? ????????????????????? ??????11???
//        if (link.length() != 11) {
//            throw new IllegalArgumentException("???????????????????????????");
//        }
        if (reqJson.containsKey("ownerPhoto") && !StringUtils.isEmpty(reqJson.getString("ownerPhoto"))) {
            editOwnerPhoto(reqJson);
        }
        editOwner(reqJson);
        JSONArray attrs = reqJson.getJSONArray("attrs");
        if (attrs.size() < 1) {
            return;
        }
        JSONObject attr = null;
        int flag = 0;
        for (int attrIndex = 0; attrIndex < attrs.size(); attrIndex++) {
            attr = attrs.getJSONObject(attrIndex);
            attr.put("memberId", reqJson.getString("memberId"));
            attr.put("communityId", reqJson.getString("communityId"));
            if (!attr.containsKey("attrId") || attr.getString("attrId").startsWith("-") || StringUtil.isEmpty(attr.getString("attrId"))) {
                attr.put("attrId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
                OwnerAttrPo ownerAttrPo = BeanConvertUtil.covertBean(attr, OwnerAttrPo.class);
                flag = ownerAttrInnerServiceSMOImpl.saveOwnerAttr(ownerAttrPo);
                if (flag < 1) {
                    throw new CmdException("????????????????????????");
                }
                continue;
            }
            OwnerAttrPo ownerAttrPo = BeanConvertUtil.covertBean(attr, OwnerAttrPo.class);
            flag = ownerAttrInnerServiceSMOImpl.updateOwnerAttrInfoInstance(ownerAttrPo);
            if (flag < 1) {
                throw new CmdException("????????????????????????");
            }
        }
    }

    public void editOwner(JSONObject paramInJson) {

        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setMemberId(paramInJson.getString("memberId"));
        List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryOwnerMembers(ownerDto);

        Assert.listOnlyOne(ownerDtos, "??????????????????????????????????????????");

        JSONObject businessOwner = new JSONObject();
        businessOwner.putAll(BeanConvertUtil.beanCovertMap(ownerDtos.get(0)));
        businessOwner.putAll(paramInJson);

        if (paramInJson.containsKey("wxPhoto")) {
            businessOwner.put("link", paramInJson.getString("wxPhoto"));
        }
        businessOwner.put("state", ownerDtos.get(0).getState());
        OwnerPo ownerPo = BeanConvertUtil.covertBean(businessOwner, OwnerPo.class);
        if (StringUtil.isEmpty(ownerPo.getIdCard())) {
            ownerPo.setAge(null);
        }
        int flag = ownerV1InnerServiceSMOImpl.updateOwner(ownerPo);
        if (flag < 1) {
            throw new CmdException("??????????????????");
        }
        OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setMemberId(paramInJson.getString("ownerId"));

        //??????app?????????
        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserInnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);
        if (ownerAppUserDtos != null && ownerAppUserDtos.size() > 0) {
            for (OwnerAppUserDto ownerAppUser : ownerAppUserDtos) {
                OwnerAppUserPo ownerAppUserPo = BeanConvertUtil.covertBean(ownerAppUser, OwnerAppUserPo.class);
                ownerAppUserPo.setLink(paramInJson.getString("link"));
                ownerAppUserPo.setIdCard(paramInJson.getString("idCard"));
                flag = ownerAppUserV1InnerServiceSMOImpl.updateOwnerAppUser(ownerAppUserPo);
                if (flag < 1) {
                    throw new CmdException("??????????????????");
                }
            }
        }
    }

    public void editOwnerPhoto(JSONObject paramInJson) {

        String _photo = paramInJson.getString("ownerPhoto");
        if(_photo.length()> 512){
            FileDto fileDto = new FileDto();
            fileDto.setFileId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_file_id));
            fileDto.setFileName(fileDto.getFileId());
            fileDto.setContext(_photo);
            fileDto.setSuffix("jpeg");
            fileDto.setCommunityId(paramInJson.getString("communityId"));
            _photo = fileInnerServiceSMOImpl.saveFile(fileDto);
        }

        FileRelDto fileRelDto = new FileRelDto();
        fileRelDto.setRelTypeCd("10000");
        fileRelDto.setObjId(paramInJson.getString("memberId"));
        int flag = 0;
        List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
        if (fileRelDtos == null || fileRelDtos.size() == 0) {
            JSONObject businessUnit = new JSONObject();
            businessUnit.put("fileRelId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_fileRelId));
            businessUnit.put("relTypeCd", "10000");
            businessUnit.put("saveWay", "table");
            businessUnit.put("objId", paramInJson.getString("memberId"));
            businessUnit.put("fileRealName", _photo);
            businessUnit.put("fileSaveName", _photo);
            FileRelPo fileRelPo = BeanConvertUtil.covertBean(businessUnit, FileRelPo.class);
            flag = fileRelInnerServiceSMOImpl.saveFileRel(fileRelPo);
            if (flag < 1) {
                throw new CmdException("??????????????????");
            }
            return;
        }

        JSONObject businessUnit = new JSONObject();
        businessUnit.putAll(BeanConvertUtil.beanCovertMap(fileRelDtos.get(0)));
        businessUnit.put("fileRealName", _photo);
        businessUnit.put("fileSaveName", _photo);
        FileRelPo fileRelPo = BeanConvertUtil.covertBean(businessUnit, FileRelPo.class);
        flag = fileRelInnerServiceSMOImpl.updateFileRel(fileRelPo);
        if (flag < 1) {
            throw new CmdException("??????????????????");
        }
    }
}
