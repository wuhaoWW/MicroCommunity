package com.java110.store.api;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.contract.ContractDto;
import com.java110.dto.contractAttr.ContractAttrDto;
import com.java110.dto.contractChangePlan.ContractChangePlanDto;
import com.java110.dto.contractChangePlanDetail.ContractChangePlanDetailDto;
import com.java110.dto.contractChangePlanDetailAttr.ContractChangePlanDetailAttrDto;
import com.java110.dto.contractCollectionPlan.ContractCollectionPlanDto;
import com.java110.dto.contractRoom.ContractRoomDto;
import com.java110.dto.contractType.ContractTypeDto;
import com.java110.dto.contractTypeSpec.ContractTypeSpecDto;
import com.java110.dto.contractTypeTemplate.ContractTypeTemplateDto;
import com.java110.entity.audit.AuditUser;
import com.java110.po.contract.ContractPo;
import com.java110.po.contractAttr.ContractAttrPo;
import com.java110.po.contractChangePlan.ContractChangePlanPo;
import com.java110.po.contractChangePlanDetail.ContractChangePlanDetailPo;
import com.java110.po.contractChangePlanDetailAttr.ContractChangePlanDetailAttrPo;
import com.java110.po.contractChangePlanRoom.ContractChangePlanRoomPo;
import com.java110.po.contractCollectionPlan.ContractCollectionPlanPo;
import com.java110.po.contractFile.ContractFilePo;
import com.java110.po.contractRoom.ContractRoomPo;
import com.java110.po.contractType.ContractTypePo;
import com.java110.po.contractTypeSpec.ContractTypeSpecPo;
import com.java110.po.contractTypeTemplate.ContractTypeTemplatePo;
import com.java110.store.bmo.contract.IDeleteContractBMO;
import com.java110.store.bmo.contract.IGetContractBMO;
import com.java110.store.bmo.contract.ISaveContractBMO;
import com.java110.store.bmo.contract.IUpdateContractBMO;
import com.java110.store.bmo.contractAttr.IDeleteContractAttrBMO;
import com.java110.store.bmo.contractAttr.IGetContractAttrBMO;
import com.java110.store.bmo.contractAttr.ISaveContractAttrBMO;
import com.java110.store.bmo.contractAttr.IUpdateContractAttrBMO;
import com.java110.store.bmo.contractChangePlan.IDeleteContractChangePlanBMO;
import com.java110.store.bmo.contractChangePlan.IGetContractChangePlanBMO;
import com.java110.store.bmo.contractChangePlan.ISaveContractChangePlanBMO;
import com.java110.store.bmo.contractChangePlan.IUpdateContractChangePlanBMO;
import com.java110.store.bmo.contractChangePlanDetail.IDeleteContractChangePlanDetailBMO;
import com.java110.store.bmo.contractChangePlanDetail.IGetContractChangePlanDetailBMO;
import com.java110.store.bmo.contractChangePlanDetail.ISaveContractChangePlanDetailBMO;
import com.java110.store.bmo.contractChangePlanDetail.IUpdateContractChangePlanDetailBMO;
import com.java110.store.bmo.contractChangePlanDetailAttr.IDeleteContractChangePlanDetailAttrBMO;
import com.java110.store.bmo.contractChangePlanDetailAttr.IGetContractChangePlanDetailAttrBMO;
import com.java110.store.bmo.contractChangePlanDetailAttr.ISaveContractChangePlanDetailAttrBMO;
import com.java110.store.bmo.contractChangePlanDetailAttr.IUpdateContractChangePlanDetailAttrBMO;
import com.java110.store.bmo.contractCollectionPlan.IDeleteContractCollectionPlanBMO;
import com.java110.store.bmo.contractCollectionPlan.IGetContractCollectionPlanBMO;
import com.java110.store.bmo.contractCollectionPlan.ISaveContractCollectionPlanBMO;
import com.java110.store.bmo.contractCollectionPlan.IUpdateContractCollectionPlanBMO;
import com.java110.store.bmo.contractRoom.IDeleteContractRoomBMO;
import com.java110.store.bmo.contractRoom.IGetContractRoomBMO;
import com.java110.store.bmo.contractRoom.ISaveContractRoomBMO;
import com.java110.store.bmo.contractRoom.IUpdateContractRoomBMO;
import com.java110.store.bmo.contractType.IDeleteContractTypeBMO;
import com.java110.store.bmo.contractType.IGetContractTypeBMO;
import com.java110.store.bmo.contractType.ISaveContractTypeBMO;
import com.java110.store.bmo.contractType.IUpdateContractTypeBMO;
import com.java110.store.bmo.contractTypeSpec.IDeleteContractTypeSpecBMO;
import com.java110.store.bmo.contractTypeSpec.IGetContractTypeSpecBMO;
import com.java110.store.bmo.contractTypeSpec.ISaveContractTypeSpecBMO;
import com.java110.store.bmo.contractTypeSpec.IUpdateContractTypeSpecBMO;
import com.java110.store.bmo.contractTypeTemplate.*;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping(value = "/contract")
public class ContractApi {

    @Autowired
    private ISaveContractBMO saveContractBMOImpl;
    @Autowired
    private IUpdateContractBMO updateContractBMOImpl;
    @Autowired
    private IDeleteContractBMO deleteContractBMOImpl;

    @Autowired
    private IGetContractBMO getContractBMOImpl;

    @Autowired
    private ISaveContractTypeBMO saveContractTypeBMOImpl;
    @Autowired
    private IUpdateContractTypeBMO updateContractTypeBMOImpl;
    @Autowired
    private IDeleteContractTypeBMO deleteContractTypeBMOImpl;

    @Autowired
    private IGetContractTypeBMO getContractTypeBMOImpl;

    @Autowired
    private ISaveContractTypeSpecBMO saveContractTypeSpecBMOImpl;
    @Autowired
    private IUpdateContractTypeSpecBMO updateContractTypeSpecBMOImpl;
    @Autowired
    private IDeleteContractTypeSpecBMO deleteContractTypeSpecBMOImpl;

    @Autowired
    private IGetContractTypeSpecBMO getContractTypeSpecBMOImpl;

    @Autowired
    private ISaveContractAttrBMO saveContractAttrBMOImpl;
    @Autowired
    private IUpdateContractAttrBMO updateContractAttrBMOImpl;
    @Autowired
    private IDeleteContractAttrBMO deleteContractAttrBMOImpl;

    @Autowired
    private IGetContractAttrBMO getContractAttrBMOImpl;

    @Autowired
    private ISaveContractTypeTemplateBMO saveContractTypeTemplateBMOImpl;
    @Autowired
    private IUpdateContractTypeTemplateBMO updateContractTypeTemplateBMOImpl;
    @Autowired
    private IDeleteContractTypeTemplateBMO deleteContractTypeTemplateBMOImpl;

    @Autowired
    private IGetContractTypeTemplateBMO getContractTypeTemplateBMOImpl;


    @Autowired
    private ISaveContractChangePlanBMO saveContractChangePlanBMOImpl;
    @Autowired
    private IUpdateContractChangePlanBMO updateContractChangePlanBMOImpl;
    @Autowired
    private IDeleteContractChangePlanBMO deleteContractChangePlanBMOImpl;

    @Autowired
    private IGetContractChangePlanBMO getContractChangePlanBMOImpl;


    @Autowired
    private ISaveContractChangePlanDetailBMO saveContractChangePlanDetailBMOImpl;
    @Autowired
    private IUpdateContractChangePlanDetailBMO updateContractChangePlanDetailBMOImpl;
    @Autowired
    private IDeleteContractChangePlanDetailBMO deleteContractChangePlanDetailBMOImpl;

    @Autowired
    private IGetContractChangePlanDetailBMO getContractChangePlanDetailBMOImpl;

    @Autowired
    private ISaveContractChangePlanDetailAttrBMO saveContractChangePlanDetailAttrBMOImpl;
    @Autowired
    private IUpdateContractChangePlanDetailAttrBMO updateContractChangePlanDetailAttrBMOImpl;
    @Autowired
    private IDeleteContractChangePlanDetailAttrBMO deleteContractChangePlanDetailAttrBMOImpl;

    @Autowired
    private IGetContractChangePlanDetailAttrBMO getContractChangePlanDetailAttrBMOImpl;

    @Autowired
    private IPrintContractTemplateBMO printContractTemplateBMO;


    @Autowired
    private ISaveContractCollectionPlanBMO saveContractCollectionPlanBMOImpl;
    @Autowired
    private IUpdateContractCollectionPlanBMO updateContractCollectionPlanBMOImpl;
    @Autowired
    private IDeleteContractCollectionPlanBMO deleteContractCollectionPlanBMOImpl;

    @Autowired
    private IGetContractCollectionPlanBMO getContractCollectionPlanBMOImpl;

    @Autowired
    private ISaveContractRoomBMO saveContractRoomBMOImpl;
    @Autowired
    private IUpdateContractRoomBMO updateContractRoomBMOImpl;
    @Autowired
    private IDeleteContractRoomBMO deleteContractRoomBMOImpl;

    @Autowired
    private IGetContractRoomBMO getContractRoomBMOImpl;

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/saveContract
     * @path /app/contract/saveContract
     */
    @RequestMapping(value = "/saveContract", method = RequestMethod.POST)
    public ResponseEntity<String> saveContract(@RequestBody JSONObject reqJson,
                                               @RequestHeader(value = "store-id") String storeId,
                                               @RequestHeader(value = "user-id") String userId) {

        Assert.hasKeyAndValue(reqJson, "contractCode", "????????????????????????contractCode");
        Assert.hasKeyAndValue(reqJson, "contractName", "????????????????????????contractName");
        Assert.hasKeyAndValue(reqJson, "contractType", "????????????????????????contractType");
        Assert.hasKeyAndValue(reqJson, "partyA", "????????????????????????partyA");
        Assert.hasKeyAndValue(reqJson, "partyB", "????????????????????????partyB");
        Assert.hasKeyAndValue(reqJson, "aContacts", "????????????????????????aContacts");
        Assert.hasKeyAndValue(reqJson, "aLink", "????????????????????????aLink");
        Assert.hasKeyAndValue(reqJson, "bContacts", "????????????????????????bContacts");
        Assert.hasKeyAndValue(reqJson, "bLink", "????????????????????????bLink");
        Assert.hasKeyAndValue(reqJson, "operator", "????????????????????????operator");
        Assert.hasKeyAndValue(reqJson, "operatorLink", "????????????????????????operatorLink");
        Assert.hasKeyAndValue(reqJson, "startTime", "????????????????????????startTime");
        Assert.hasKeyAndValue(reqJson, "endTime", "????????????????????????endTime");
        Assert.hasKeyAndValue(reqJson, "signingTime", "????????????????????????signingTime");

        ContractPo contractPo = BeanConvertUtil.covertBean(reqJson, ContractPo.class);
        contractPo.setStoreId(storeId);
        contractPo.setStartUserId(userId);
        if (!reqJson.containsKey("contractParentId") || "-1".equals(reqJson.getString("contractParentId"))) {
            contractPo.setContractParentId("-1");
        }
        reqJson.put("userId", userId);


        if (reqJson.containsKey("contractFilePo")) {
            JSONArray contractFiles = reqJson.getJSONArray("contractFilePo");
            List<ContractFilePo> contractFilePos = new ArrayList<>();
            for (int conFileIndex = 0; conFileIndex < contractFiles.size(); conFileIndex++) {
                JSONObject resourceStore = contractFiles.getJSONObject(conFileIndex);
                ContractFilePo contractFilePo = BeanConvertUtil.covertBean(resourceStore, ContractFilePo.class);
                contractFilePo.setContractFileId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_contractFileId));
                contractFilePos.add(contractFilePo);
            }
            contractPo.setContractFilePo(contractFilePos);
        }


        return saveContractBMOImpl.save(contractPo, reqJson);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/updateContract
     * @path /app/contract/updateContract
     */
    @RequestMapping(value = "/updateContract", method = RequestMethod.POST)
    public ResponseEntity<String> updateContract(@RequestBody JSONObject reqJson) {

        Assert.hasKeyAndValue(reqJson, "contractCode", "????????????????????????contractCode");
        Assert.hasKeyAndValue(reqJson, "contractName", "????????????????????????contractName");
        Assert.hasKeyAndValue(reqJson, "contractType", "????????????????????????contractType");
        Assert.hasKeyAndValue(reqJson, "partyA", "????????????????????????partyA");
        Assert.hasKeyAndValue(reqJson, "partyB", "????????????????????????partyB");
        Assert.hasKeyAndValue(reqJson, "aContacts", "????????????????????????aContacts");
        Assert.hasKeyAndValue(reqJson, "aLink", "????????????????????????aLink");
        Assert.hasKeyAndValue(reqJson, "bContacts", "????????????????????????bContacts");
        Assert.hasKeyAndValue(reqJson, "bLink", "????????????????????????bLink");
        Assert.hasKeyAndValue(reqJson, "operator", "????????????????????????operator");
        Assert.hasKeyAndValue(reqJson, "operatorLink", "????????????????????????operatorLink");
        Assert.hasKeyAndValue(reqJson, "startTime", "????????????????????????startTime");
        Assert.hasKeyAndValue(reqJson, "endTime", "????????????????????????endTime");
        Assert.hasKeyAndValue(reqJson, "signingTime", "????????????????????????signingTime");
        Assert.hasKeyAndValue(reqJson, "contractId", "contractId????????????");


        ContractPo contractPo = BeanConvertUtil.covertBean(reqJson, ContractPo.class);

        JSONArray contractFiles = reqJson.getJSONArray("contractFilePo");
        List<ContractFilePo> contractFilePos = new ArrayList<>();
        for (int conFileIndex = 0; conFileIndex < contractFiles.size(); conFileIndex++) {
            JSONObject resourceStore = contractFiles.getJSONObject(conFileIndex);
            ContractFilePo contractFilePo = BeanConvertUtil.covertBean(resourceStore, ContractFilePo.class);
            contractFilePo.setContractFileId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_contractFileId));
            contractFilePos.add(contractFilePo);
        }
        contractPo.setContractFilePo(contractFilePos);
        return updateContractBMOImpl.update(contractPo, reqJson);
    }


    /**
     * ????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/stopContract
     * @path /app/contract/stopContract
     */
    @RequestMapping(value = "/stopContract", method = RequestMethod.POST)
    public ResponseEntity<String> stopContract(@RequestBody JSONObject reqJson) {

        Assert.hasKeyAndValue(reqJson, "contractId", "contractId????????????");

        ContractPo contractPo = BeanConvertUtil.covertBean(reqJson, ContractPo.class);
        contractPo.setState(ContractDto.STATE_COMPLAINT);
        return updateContractBMOImpl.update(contractPo, reqJson);
    }


    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/needAuditContract
     * @path /app/contract/needAuditContract
     */
    @RequestMapping(value = "/needAuditContract", method = RequestMethod.POST)
    public ResponseEntity<String> needAuditContract(
            @RequestHeader(value = "store-id") String storeId,
            @RequestHeader(value = "user-id") String userId,
            @RequestBody JSONObject reqJson) {
        ContractDto contractDto = new ContractDto();
        contractDto.setTaskId(reqJson.getString("taskId"));
        contractDto.setContractId(reqJson.getString("contractId"));
        contractDto.setStoreId(storeId);
        contractDto.setAuditCode(reqJson.getString("state"));
        contractDto.setAuditMessage(reqJson.getString("remark"));
        contractDto.setCurrentUserId(userId);

        return updateContractBMOImpl.needAuditContract(contractDto, reqJson);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/auditContract
     * @path /app/contract/auditContract
     */
    @RequestMapping(value = "/auditContract", method = RequestMethod.POST)
    public ResponseEntity<String> auditContract(@RequestBody JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "state", "??????????????????????????????");
        Assert.hasKeyAndValue(reqJson, "stateDesc", "??????????????????????????????");
        Assert.hasKeyAndValue(reqJson, "contractId", "contractId????????????");


        ContractPo contractPo = BeanConvertUtil.covertBean(reqJson, ContractPo.class);
        return updateContractBMOImpl.update(contractPo, reqJson);
    }


    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/deleteContract
     * @path /app/contract/deleteContract
     */
    @RequestMapping(value = "/deleteContract", method = RequestMethod.POST)
    public ResponseEntity<String> deleteContract(@RequestBody JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "communityId", "??????ID????????????");

        Assert.hasKeyAndValue(reqJson, "contractId", "contractId????????????");


        ContractPo contractPo = BeanConvertUtil.covertBean(reqJson, ContractPo.class);
        return deleteContractBMOImpl.delete(contractPo);
    }


//    /**
//     * ????????????????????????
//     *
//     * @param storeId    ??????ID
//     * @param expiration ?????????????????? 1 ????????????
//     * @return
//     * @serviceCode /contract/queryContract
//     * @path /app/contract/queryContract
//     */
//    @RequestMapping(value = "/queryContract", method = RequestMethod.GET)
//    public ResponseEntity<String> queryContract(@RequestHeader(value = "store-id") String storeId,
//                                                @RequestParam(value = "state", required = false) String state,
//                                                @RequestParam(value = "expiration", required = false) String expiration,
//                                                @RequestParam(value = "objId", required = false) String objId,
//                                                @RequestParam(value = "contractId", required = false) String contractId,
//                                                @RequestParam(value = "contractNameLike", required = false) String contractNameLike,
//                                                @RequestParam(value = "contractCode", required = false) String contractCode,
//                                                @RequestParam(value = "page") int page,
//                                                @RequestParam(value = "row") int row) {
//        ContractDto contractDto = new ContractDto();
//        contractDto.setPage(page);
//        contractDto.setRow(row);
//        contractDto.setStoreId(storeId);
//        contractDto.setState(state);
//        contractDto.setObjId(objId);
//        contractDto.setContractId(contractId);
//        contractDto.setContractCode(contractCode);
//        contractDto.setContractNameLike(contractNameLike);
//        //?????????????????????
//        if ("1".equals(expiration)) {
//            contractDto.setNoStates(new String[]{ContractDto.STATE_COMPLAINT, ContractDto.STATE_FAIL});
//            contractDto.setEndTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
//        }
//        return getContractBMOImpl.get(contractDto);
//    }

    /**
     * ??????????????????
     *
     * @param storeId ??????ID
     * @return
     * @serviceCode /contract/queryContractTask
     * @path /app/contract/queryContractTask
     */
    @RequestMapping(value = "/queryContractTask", method = RequestMethod.GET)
    public ResponseEntity<String> queryContractTask(@RequestHeader(value = "store-id") String storeId,
                                                    @RequestHeader(value = "user-id") String userId,
                                                    @RequestParam(value = "page") int page,
                                                    @RequestParam(value = "row") int row) {
        AuditUser auditUser = new AuditUser();
        auditUser.setUserId(userId);
        auditUser.setPage(page);
        auditUser.setRow(row);
        auditUser.setStoreId(storeId);

        return getContractBMOImpl.queryContractTask(auditUser);
    }

    /**
     * ??????????????????
     *
     * @param storeId ??????ID
     * @return
     * @serviceCode /contract/queryContractHistoryTask
     * @path /app/contract/queryContractHistoryTask
     */
    @RequestMapping(value = "/queryContractHistoryTask", method = RequestMethod.GET)
    public ResponseEntity<String> queryContractHistoryTask(@RequestHeader(value = "store-id") String storeId,
                                                           @RequestHeader(value = "user-id") String userId,
                                                           @RequestParam(value = "page") int page,
                                                           @RequestParam(value = "row") int row) {


        AuditUser auditUser = new AuditUser();
        auditUser.setUserId(userId);
        auditUser.setPage(page);
        auditUser.setRow(row);
        auditUser.setStoreId(storeId);

        return getContractBMOImpl.queryContractHistoryTask(auditUser);
    }

    /**
     * ??????????????????
     *
     * @param storeId ??????ID
     * @return
     * @serviceCode /contract/queryContractChangeTask
     * @path /app/contract/queryContractChangeTask
     */
    @RequestMapping(value = "/queryContractChangeTask", method = RequestMethod.GET)
    public ResponseEntity<String> queryContractChangeTask(@RequestHeader(value = "store-id") String storeId,
                                                          @RequestHeader(value = "user-id") String userId,
                                                          @RequestParam(value = "page") int page,
                                                          @RequestParam(value = "row") int row) {
        AuditUser auditUser = new AuditUser();
        auditUser.setUserId(userId);
        auditUser.setPage(page);
        auditUser.setRow(row);
        auditUser.setStoreId(storeId);

        return getContractBMOImpl.queryContractChangeTask(auditUser);
    }

    /**
     * ??????????????????
     *
     * @param storeId ??????ID
     * @return
     * @serviceCode /contract/queryContractChangeHistoryTask
     * @path /app/contract/queryContractChangeHistoryTask
     */
    @RequestMapping(value = "/queryContractChangeHistoryTask", method = RequestMethod.GET)
    public ResponseEntity<String> queryContractChangeHistoryTask(@RequestHeader(value = "store-id") String storeId,
                                                                 @RequestHeader(value = "user-id") String userId,
                                                                 @RequestParam(value = "page") int page,
                                                                 @RequestParam(value = "row") int row) {


        AuditUser auditUser = new AuditUser();
        auditUser.setUserId(userId);
        auditUser.setPage(page);
        auditUser.setRow(row);
        auditUser.setStoreId(storeId);
        return getContractBMOImpl.queryContractChangeHistoryTask(auditUser);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/saveContractType
     * @path /app/contract/saveContractType
     */
    @RequestMapping(value = "/saveContractType", method = RequestMethod.POST)
    public ResponseEntity<String> saveContractType(@RequestBody JSONObject reqJson, @RequestHeader(value = "store-id", required = false) String storeId) {

        Assert.hasKeyAndValue(reqJson, "typeName", "????????????????????????typeName");
        Assert.hasKeyAndValue(reqJson, "audit", "????????????????????????audit");


        ContractTypePo contractTypePo = BeanConvertUtil.covertBean(reqJson, ContractTypePo.class);
        contractTypePo.setStoreId(storeId);
        return saveContractTypeBMOImpl.save(contractTypePo);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/updateContractType
     * @path /app/contract/updateContractType
     */
    @RequestMapping(value = "/updateContractType", method = RequestMethod.POST)
    public ResponseEntity<String> updateContractType(@RequestBody JSONObject reqJson) {

        Assert.hasKeyAndValue(reqJson, "typeName", "????????????????????????typeName");
        Assert.hasKeyAndValue(reqJson, "audit", "????????????????????????audit");
        Assert.hasKeyAndValue(reqJson, "contractTypeId", "contractTypeId????????????");


        ContractTypePo contractTypePo = BeanConvertUtil.covertBean(reqJson, ContractTypePo.class);
        return updateContractTypeBMOImpl.update(contractTypePo);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/deleteContractType
     * @path /app/contract/deleteContractType
     */
    @RequestMapping(value = "/deleteContractType", method = RequestMethod.POST)
    public ResponseEntity<String> deleteContractType(@RequestBody JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "communityId", "??????ID????????????");

        Assert.hasKeyAndValue(reqJson, "contractTypeId", "contractTypeId????????????");


        ContractTypePo contractTypePo = BeanConvertUtil.covertBean(reqJson, ContractTypePo.class);
        return deleteContractTypeBMOImpl.delete(contractTypePo);
    }

    /**
     * ????????????????????????
     *
     * @param storeId ??????ID
     * @return
     * @serviceCode /contract/queryContractType
     * @path /app/contract/queryContractType
     */
    @RequestMapping(value = "/queryContractType", method = RequestMethod.GET)
    public ResponseEntity<String> queryContractType(@RequestHeader(value = "store-id") String storeId,
                                                    @RequestParam(value = "audit", required = false) String audit,
                                                    @RequestParam(value = "typeName", required = false) String typeName,
                                                    @RequestParam(value = "contractTypeId", required = false) String contractTypeId,
                                                    @RequestParam(value = "page") int page,
                                                    @RequestParam(value = "row") int row) {
        ContractTypeDto contractTypeDto = new ContractTypeDto();
        contractTypeDto.setPage(page);
        contractTypeDto.setRow(row);
        contractTypeDto.setAudit(audit);
        contractTypeDto.setTypeName(typeName);
        contractTypeDto.setContractTypeId(contractTypeId);
        contractTypeDto.setStoreId(storeId);
        return getContractTypeBMOImpl.get(contractTypeDto);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/saveContractTypeSpec
     * @path /app/contract/saveContractTypeSpec
     */
    @RequestMapping(value = "/saveContractTypeSpec", method = RequestMethod.POST)
    public ResponseEntity<String> saveContractTypeSpec(@RequestBody JSONObject reqJson, @RequestHeader(value = "store-id") String storeId) {

        // Assert.hasKeyAndValue(reqJson, "specCd", "????????????????????????specCd");
        Assert.hasKeyAndValue(reqJson, "contractTypeId", "????????????????????????contractTypeId");
        Assert.hasKeyAndValue(reqJson, "specName", "????????????????????????specName");
        Assert.hasKeyAndValue(reqJson, "specHoldplace", "????????????????????????specHoldplace");
        Assert.hasKeyAndValue(reqJson, "required", "????????????????????????required");
        Assert.hasKeyAndValue(reqJson, "specShow", "????????????????????????specShow");
        Assert.hasKeyAndValue(reqJson, "specValueType", "????????????????????????specValueType");
        Assert.hasKeyAndValue(reqJson, "specType", "????????????????????????specType");
        Assert.hasKeyAndValue(reqJson, "listShow", "????????????????????????listShow");


        ContractTypeSpecPo contractTypeSpecPo = BeanConvertUtil.covertBean(reqJson, ContractTypeSpecPo.class);
        contractTypeSpecPo.setStoreId(storeId);
        return saveContractTypeSpecBMOImpl.save(contractTypeSpecPo);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/updateContractTypeSpec
     * @path /app/contract/updateContractTypeSpec
     */
    @RequestMapping(value = "/updateContractTypeSpec", method = RequestMethod.POST)
    public ResponseEntity<String> updateContractTypeSpec(@RequestBody JSONObject reqJson) {

        Assert.hasKeyAndValue(reqJson, "specCd", "????????????????????????specCd");
        Assert.hasKeyAndValue(reqJson, "contractTypeId", "????????????????????????contractTypeId");
        Assert.hasKeyAndValue(reqJson, "specName", "????????????????????????specName");
        Assert.hasKeyAndValue(reqJson, "specHoldplace", "????????????????????????specHoldplace");
        Assert.hasKeyAndValue(reqJson, "required", "????????????????????????required");
        Assert.hasKeyAndValue(reqJson, "specShow", "????????????????????????specShow");
        Assert.hasKeyAndValue(reqJson, "specValueType", "????????????????????????specValueType");
        Assert.hasKeyAndValue(reqJson, "specType", "????????????????????????specType");
        Assert.hasKeyAndValue(reqJson, "listShow", "????????????????????????listShow");
        Assert.hasKeyAndValue(reqJson, "specCd", "specCd????????????");


        ContractTypeSpecPo contractTypeSpecPo = BeanConvertUtil.covertBean(reqJson, ContractTypeSpecPo.class);
        return updateContractTypeSpecBMOImpl.update(contractTypeSpecPo);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/deleteContractTypeSpec
     * @path /app/contract/deleteContractTypeSpec
     */
    @RequestMapping(value = "/deleteContractTypeSpec", method = RequestMethod.POST)
    public ResponseEntity<String> deleteContractTypeSpec(@RequestBody JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "communityId", "??????ID????????????");

        Assert.hasKeyAndValue(reqJson, "specCd", "specCd????????????");


        ContractTypeSpecPo contractTypeSpecPo = BeanConvertUtil.covertBean(reqJson, ContractTypeSpecPo.class);
        return deleteContractTypeSpecBMOImpl.delete(contractTypeSpecPo);
    }

    /**
     * ????????????????????????
     *
     * @param storeId ??????ID
     * @return
     * @serviceCode /contract/queryContractTypeSpec
     * @path /app/contract/queryContractTypeSpec
     */
    @RequestMapping(value = "/queryContractTypeSpec", method = RequestMethod.GET)
    public ResponseEntity<String> queryContractTypeSpec(@RequestParam(value = "specName", required = false) String specName,
                                                        @RequestParam(value = "specShow", required = false) String specShow,
                                                        @RequestParam(value = "specCd", required = false) String specCd,
                                                        @RequestHeader(value = "store-id") String storeId,
                                                        @RequestParam(value = "page") int page,
                                                        @RequestParam(value = "row") int row,
                                                        @RequestParam(value = "contractTypeId") String contractTypeId) {
        ContractTypeSpecDto contractTypeSpecDto = new ContractTypeSpecDto();
        contractTypeSpecDto.setPage(page);
        contractTypeSpecDto.setRow(row);
        contractTypeSpecDto.setStoreId(storeId);
        contractTypeSpecDto.setContractTypeId(contractTypeId);
        contractTypeSpecDto.setSpecName(specName);
        contractTypeSpecDto.setSpecShow(specShow);
        contractTypeSpecDto.setSpecCd(specCd);
        return getContractTypeSpecBMOImpl.get(contractTypeSpecDto);
    }


    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/saveContractAttr
     * @path /app/contract/saveContractAttr
     */
    @RequestMapping(value = "/saveContractAttr", method = RequestMethod.POST)
    public ResponseEntity<String> saveContractAttr(@RequestBody JSONObject reqJson) {

        Assert.hasKeyAndValue(reqJson, "contractId", "????????????????????????contractId");
        Assert.hasKeyAndValue(reqJson, "storeId", "????????????????????????storeId");
        Assert.hasKeyAndValue(reqJson, "specCd", "????????????????????????specCd");


        ContractAttrPo contractAttrPo = BeanConvertUtil.covertBean(reqJson, ContractAttrPo.class);
        return saveContractAttrBMOImpl.save(contractAttrPo);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/updateContractAttr
     * @path /app/contract/updateContractAttr
     */
    @RequestMapping(value = "/updateContractAttr", method = RequestMethod.POST)
    public ResponseEntity<String> updateContractAttr(@RequestBody JSONObject reqJson) {

        Assert.hasKeyAndValue(reqJson, "contractId", "????????????????????????contractId");
        Assert.hasKeyAndValue(reqJson, "storeId", "????????????????????????storeId");
        Assert.hasKeyAndValue(reqJson, "specCd", "????????????????????????specCd");
        Assert.hasKeyAndValue(reqJson, "attrId", "attrId????????????");


        ContractAttrPo contractAttrPo = BeanConvertUtil.covertBean(reqJson, ContractAttrPo.class);
        return updateContractAttrBMOImpl.update(contractAttrPo);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/deleteContractAttr
     * @path /app/contract/deleteContractAttr
     */
    @RequestMapping(value = "/deleteContractAttr", method = RequestMethod.POST)
    public ResponseEntity<String> deleteContractAttr(@RequestBody JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "communityId", "??????ID????????????");

        Assert.hasKeyAndValue(reqJson, "attrId", "attrId????????????");


        ContractAttrPo contractAttrPo = BeanConvertUtil.covertBean(reqJson, ContractAttrPo.class);
        return deleteContractAttrBMOImpl.delete(contractAttrPo);
    }

    /**
     * ????????????????????????
     *
     * @param storeId ??????ID
     * @return
     * @serviceCode /contract/queryContractAttr
     * @path /app/contract/queryContractAttr
     */
    @RequestMapping(value = "/queryContractAttr", method = RequestMethod.GET)
    public ResponseEntity<String> queryContractAttr(@RequestHeader(value = "store-id") String storeId,
                                                    @RequestParam(value = "page") int page,
                                                    @RequestParam(value = "row") int row) {
        ContractAttrDto contractAttrDto = new ContractAttrDto();
        contractAttrDto.setPage(page);
        contractAttrDto.setRow(row);
        contractAttrDto.setStoreId(storeId);
        return getContractAttrBMOImpl.get(contractAttrDto);
    }


    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/saveContractTypeTemplate
     * @path /app/contract/saveContractTypeTemplate
     */
    @RequestMapping(value = "/saveContractTypeTemplate", method = RequestMethod.POST)
    public ResponseEntity<String> saveContractTypeTemplate(@RequestBody JSONObject reqJson, @RequestHeader(value = "store-id") String storeId) {

        Assert.hasKeyAndValue(reqJson, "contractTypeId", "????????????????????????contractTypeId");
        Assert.hasKeyAndValue(reqJson, "context", "????????????????????????context");


        ContractTypeTemplatePo contractTypeTemplatePo = BeanConvertUtil.covertBean(reqJson, ContractTypeTemplatePo.class);
        contractTypeTemplatePo.setStoreId(storeId);
        return saveContractTypeTemplateBMOImpl.save(contractTypeTemplatePo);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/updateContractTypeTemplate
     * @path /app/contract/updateContractTypeTemplate
     */
    @RequestMapping(value = "/updateContractTypeTemplate", method = RequestMethod.POST)
    public ResponseEntity<String> updateContractTypeTemplate(@RequestBody JSONObject reqJson) {

        Assert.hasKeyAndValue(reqJson, "contractTypeId", "????????????????????????contractTypeId");
        Assert.hasKeyAndValue(reqJson, "context", "????????????????????????context");
        Assert.hasKeyAndValue(reqJson, "templateId", "templateId????????????");


        ContractTypeTemplatePo contractTypeTemplatePo = BeanConvertUtil.covertBean(reqJson, ContractTypeTemplatePo.class);
        return updateContractTypeTemplateBMOImpl.update(contractTypeTemplatePo);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/deleteContractTypeTemplate
     * @path /app/contract/deleteContractTypeTemplate
     */
    @RequestMapping(value = "/deleteContractTypeTemplate", method = RequestMethod.POST)
    public ResponseEntity<String> deleteContractTypeTemplate(@RequestBody JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "communityId", "??????ID????????????");

        Assert.hasKeyAndValue(reqJson, "templateId", "templateId????????????");


        ContractTypeTemplatePo contractTypeTemplatePo = BeanConvertUtil.covertBean(reqJson, ContractTypeTemplatePo.class);
        return deleteContractTypeTemplateBMOImpl.delete(contractTypeTemplatePo);
    }

    /**
     * ????????????????????????
     *
     * @param storeId ??????ID
     * @return
     * @serviceCode /contract/queryContractTypeTemplate
     * @path /app/contract/queryContractTypeTemplate
     */
    @RequestMapping(value = "/queryContractTypeTemplate", method = RequestMethod.GET)
    public ResponseEntity<String> queryContractTypeTemplate(@RequestHeader(value = "store-id") String storeId,
                                                            @RequestParam(value = "contractTypeId", required = false) String contractTypeId,
                                                            @RequestParam(value = "page") int page,
                                                            @RequestParam(value = "row") int row) {
        ContractTypeTemplateDto contractTypeTemplateDto = new ContractTypeTemplateDto();
        contractTypeTemplateDto.setPage(page);
        contractTypeTemplateDto.setRow(row);
        contractTypeTemplateDto.setStoreId(storeId);
        contractTypeTemplateDto.setContractTypeId(contractTypeId);
        return getContractTypeTemplateBMOImpl.get(contractTypeTemplateDto);
    }


    /**
     * ????????????????????????
     * {"index":0,"contractName":"????????????","contractCode":"1","contractType":"812021030474360091","partyA":"?????????","partyB":"?????????","aContacts":"?????????",
     * "bContacts":"?????????11","aLink":"18909711443","bLink":"18909711443","operator":"1","operatorLink":"13789876589","amount":"100.00",
     * "startTime":"2021-03-10 00:00:50","endTime":"2021-03-03 01:05:50","signingTime":"2021-03-02 00:00:50","param":"contractChangeMainBody",
     * "planType":"1001","changeRemark":"??????"}
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/saveContractChangePlan
     * @path /app/contract/saveContractChangePlan
     */
    @RequestMapping(value = "/saveContractChangePlan", method = RequestMethod.POST)
    public ResponseEntity<String> saveContractChangePlan(@RequestHeader(value = "store-id") String storeId,
                                                         @RequestHeader(value = "user-id") String userId,
                                                         @RequestBody JSONObject reqJson) {

        Assert.hasKeyAndValue(reqJson, "planType", "????????????????????????planType");

        ContractChangePlanPo contractChangePlanPo = BeanConvertUtil.covertBean(reqJson, ContractChangePlanPo.class);
        contractChangePlanPo.setStoreId(storeId);
        contractChangePlanPo.setChangePerson(userId);

        contractChangePlanPo.setState(ContractChangePlanDto.STATE_W);
        contractChangePlanPo.setRemark(reqJson.getString("changeRemark"));

        List<ContractChangePlanRoomPo> contractChangePlanRoomPos = new ArrayList<>();
        ContractChangePlanRoomPo contractChangePlanRoomPo = null;
        JSONObject roomInfo = null;
        if (reqJson.containsKey("rooms")) {
            JSONArray rooms = reqJson.getJSONArray("rooms");
            if (rooms != null && rooms.size() > 0) {
                for (int roomIndex = 0; roomIndex < rooms.size(); roomIndex++) {
//                    contractChangePlanRoomPos.add(BeanConvertUtil.covertBean(rooms.getJSONObject(roomIndex), ContractChangePlanRoomPo.class));
                    roomInfo = rooms.getJSONObject(roomIndex);
                    contractChangePlanRoomPo = BeanConvertUtil.covertBean(roomInfo, ContractChangePlanRoomPo.class);
                    contractChangePlanRoomPo.setRoomName(roomInfo.getString("floorNum")
                            + "-" + roomInfo.getString("unitNum")
                            + "-" + roomInfo.getString("roomNum"));
                    contractChangePlanRoomPos.add(contractChangePlanRoomPo);
                }
            }
        }

        ContractChangePlanDetailPo contractChangePlanDetailPo = BeanConvertUtil.covertBean(reqJson, ContractChangePlanDetailPo.class);
        contractChangePlanDetailPo.setStoreId(storeId);
        return saveContractChangePlanBMOImpl.save(contractChangePlanPo, contractChangePlanDetailPo, contractChangePlanRoomPos, reqJson);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/updateContractChangePlan
     * @path /app/contract/updateContractChangePlan
     */
    @RequestMapping(value = "/updateContractChangePlan", method = RequestMethod.POST)
    public ResponseEntity<String> updateContractChangePlan(@RequestBody JSONObject reqJson) {

        Assert.hasKeyAndValue(reqJson, "planType", "????????????????????????planType");
        Assert.hasKeyAndValue(reqJson, "changePerson", "????????????????????????changePerson");
        Assert.hasKeyAndValue(reqJson, "state", "????????????????????????state");
        Assert.hasKeyAndValue(reqJson, "storeId", "????????????????????????storeId");
        Assert.hasKeyAndValue(reqJson, "planId", "planId????????????");


        ContractChangePlanPo contractChangePlanPo = BeanConvertUtil.covertBean(reqJson, ContractChangePlanPo.class);
        return updateContractChangePlanBMOImpl.update(contractChangePlanPo);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/deleteContractChangePlan
     * @path /app/contract/deleteContractChangePlan
     */
    @RequestMapping(value = "/deleteContractChangePlan", method = RequestMethod.POST)
    public ResponseEntity<String> deleteContractChangePlan(@RequestHeader(value = "store-id") String storeId,
                                                           @RequestBody JSONObject reqJson) {

        Assert.hasKeyAndValue(reqJson, "planId", "planId????????????");

        ContractChangePlanPo contractChangePlanPo = BeanConvertUtil.covertBean(reqJson, ContractChangePlanPo.class);
        contractChangePlanPo.setStoreId(storeId);
        return deleteContractChangePlanBMOImpl.delete(contractChangePlanPo);
    }

    /**
     * ????????????????????????
     *
     * @param storeId ??????ID
     * @return
     * @serviceCode /contract/queryContractChangePlan
     * @path /app/contract/queryContractChangePlan
     */
    @RequestMapping(value = "/queryContractChangePlan", method = RequestMethod.GET)
    public ResponseEntity<String> queryContractChangePlan(@RequestHeader(value = "store-id") String storeId,
                                                          @RequestParam(value = "page") int page,
                                                          @RequestParam(value = "row") int row,
                                                          @RequestParam(value = "contractId", required = false) String contractId,
                                                          @RequestParam(value = "contractName", required = false) String contractName,
                                                          @RequestParam(value = "contractCode", required = false) String contractCode,
                                                          @RequestParam(value = "contractType", required = false) String contractType,
                                                          @RequestParam(value = "planId", required = false) String planId
    ) {
        ContractChangePlanDto contractChangePlanDto = new ContractChangePlanDto();
        contractChangePlanDto.setPage(page);
        contractChangePlanDto.setRow(row);
        contractChangePlanDto.setStoreId(storeId);
        contractChangePlanDto.setContractId(contractId);
        contractChangePlanDto.setContractName(contractName);
        contractChangePlanDto.setPlanId(planId);
        contractChangePlanDto.setContractCode(contractCode);
        contractChangePlanDto.setContractType(contractType);
        return getContractChangePlanBMOImpl.get(contractChangePlanDto);
    }


    /**
     * ??????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/needAuditContractPlan
     * @path /app/contract/needAuditContractPlan
     */
    @RequestMapping(value = "/needAuditContractPlan", method = RequestMethod.POST)
    public ResponseEntity<String> needAuditContractPlan(
            @RequestHeader(value = "store-id") String storeId,
            @RequestHeader(value = "user-id") String userId,
            @RequestBody JSONObject reqJson) {
        ContractChangePlanDto contractChangePlanDto = new ContractChangePlanDto();
        contractChangePlanDto.setTaskId(reqJson.getString("taskId"));
        contractChangePlanDto.setPlanId(reqJson.getString("planId"));
        contractChangePlanDto.setContractId(reqJson.getString("contractId"));
        contractChangePlanDto.setStoreId(storeId);
        contractChangePlanDto.setAuditCode(reqJson.getString("state"));
        contractChangePlanDto.setAuditMessage(reqJson.getString("remark"));
        contractChangePlanDto.setCurrentUserId(userId);

        return updateContractBMOImpl.needAuditContractPlan(contractChangePlanDto, reqJson);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/saveContractChangePlanDetail
     * @path /app/contract/saveContractChangePlanDetail
     */
    @RequestMapping(value = "/saveContractChangePlanDetail", method = RequestMethod.POST)
    public ResponseEntity<String> saveContractChangePlanDetail(@RequestHeader(value = "store-id") String storeId,
                                                               @RequestBody JSONObject reqJson) {

        Assert.hasKeyAndValue(reqJson, "contractCode", "????????????????????????contractCode");
        Assert.hasKeyAndValue(reqJson, "contractName", "????????????????????????contractName");
        Assert.hasKeyAndValue(reqJson, "contractType", "????????????????????????contractType");
        Assert.hasKeyAndValue(reqJson, "partyA", "????????????????????????partyA");
        Assert.hasKeyAndValue(reqJson, "partyB", "????????????????????????partyB");
        Assert.hasKeyAndValue(reqJson, "aContacts", "????????????????????????aContacts");
        Assert.hasKeyAndValue(reqJson, "aLink", "????????????????????????aLink");
        Assert.hasKeyAndValue(reqJson, "bContacts", "????????????????????????bContacts");
        Assert.hasKeyAndValue(reqJson, "bLink", "????????????????????????bLink");
        Assert.hasKeyAndValue(reqJson, "operator", "????????????????????????operator");
        Assert.hasKeyAndValue(reqJson, "operatorLink", "????????????????????????operatorLink");
        Assert.hasKeyAndValue(reqJson, "startTime", "????????????????????????startTime");
        Assert.hasKeyAndValue(reqJson, "endTime", "????????????????????????endTime");
        Assert.hasKeyAndValue(reqJson, "signingTime", "????????????????????????signingTime");


        ContractChangePlanDetailPo contractChangePlanDetailPo = BeanConvertUtil.covertBean(reqJson, ContractChangePlanDetailPo.class);
        contractChangePlanDetailPo.setStoreId(storeId);
        return saveContractChangePlanDetailBMOImpl.save(contractChangePlanDetailPo);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/updateContractChangePlanDetail
     * @path /app/contract/updateContractChangePlanDetail
     */
    @RequestMapping(value = "/updateContractChangePlanDetail", method = RequestMethod.POST)
    public ResponseEntity<String> updateContractChangePlanDetail(@RequestBody JSONObject reqJson) {

        Assert.hasKeyAndValue(reqJson, "contractCode", "????????????????????????contractCode");
        Assert.hasKeyAndValue(reqJson, "contractName", "????????????????????????contractName");
        Assert.hasKeyAndValue(reqJson, "contractType", "????????????????????????contractType");
        Assert.hasKeyAndValue(reqJson, "partyA", "????????????????????????partyA");
        Assert.hasKeyAndValue(reqJson, "partyB", "????????????????????????partyB");
        Assert.hasKeyAndValue(reqJson, "aContacts", "????????????????????????aContacts");
        Assert.hasKeyAndValue(reqJson, "aLink", "????????????????????????aLink");
        Assert.hasKeyAndValue(reqJson, "bContacts", "????????????????????????bContacts");
        Assert.hasKeyAndValue(reqJson, "bLink", "????????????????????????bLink");
        Assert.hasKeyAndValue(reqJson, "operator", "????????????????????????operator");
        Assert.hasKeyAndValue(reqJson, "operatorLink", "????????????????????????operatorLink");
        Assert.hasKeyAndValue(reqJson, "startTime", "????????????????????????startTime");
        Assert.hasKeyAndValue(reqJson, "endTime", "????????????????????????endTime");
        Assert.hasKeyAndValue(reqJson, "signingTime", "????????????????????????signingTime");
        Assert.hasKeyAndValue(reqJson, "detailId", "detailId????????????");


        ContractChangePlanDetailPo contractChangePlanDetailPo = BeanConvertUtil.covertBean(reqJson, ContractChangePlanDetailPo.class);
        return updateContractChangePlanDetailBMOImpl.update(contractChangePlanDetailPo);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/deleteContractChangePlanDetail
     * @path /app/contract/deleteContractChangePlanDetail
     */
    @RequestMapping(value = "/deleteContractChangePlanDetail", method = RequestMethod.POST)
    public ResponseEntity<String> deleteContractChangePlanDetail(@RequestBody JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "communityId", "??????ID????????????");

        Assert.hasKeyAndValue(reqJson, "detailId", "detailId????????????");


        ContractChangePlanDetailPo contractChangePlanDetailPo = BeanConvertUtil.covertBean(reqJson, ContractChangePlanDetailPo.class);
        return deleteContractChangePlanDetailBMOImpl.delete(contractChangePlanDetailPo);
    }

    /**
     * ????????????????????????
     *
     * @param storeId ??????ID
     * @return
     * @serviceCode /contract/queryContractChangePlanDetail
     * @path /app/contract/queryContractChangePlanDetail
     */
    @RequestMapping(value = "/queryContractChangePlanDetail", method = RequestMethod.GET)
    public ResponseEntity<String> queryContractChangePlanDetail(@RequestHeader(value = "store-id") String storeId,
                                                                @RequestParam(value = "planId", required = false) String planId,
                                                                @RequestParam(value = "contractId", required = false) String contractId,
                                                                @RequestParam(value = "page") int page,
                                                                @RequestParam(value = "row") int row) {
        ContractChangePlanDetailDto contractChangePlanDetailDto = new ContractChangePlanDetailDto();
        contractChangePlanDetailDto.setPage(page);
        contractChangePlanDetailDto.setRow(row);
        contractChangePlanDetailDto.setStoreId(storeId);
        contractChangePlanDetailDto.setPlanId(planId);
        contractChangePlanDetailDto.setContractId(contractId);
        return getContractChangePlanDetailBMOImpl.get(contractChangePlanDetailDto);
    }


    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/saveContractChangePlanDetailAttr
     * @path /app/contract/saveContractChangePlanDetailAttr
     */
    @RequestMapping(value = "/saveContractChangePlanDetailAttr", method = RequestMethod.POST)
    public ResponseEntity<String> saveContractChangePlanDetailAttr(@RequestBody JSONObject reqJson) {

        Assert.hasKeyAndValue(reqJson, "contractId", "????????????????????????contractId");
        Assert.hasKeyAndValue(reqJson, "detailId", "????????????????????????detailId");
        Assert.hasKeyAndValue(reqJson, "storeId", "????????????????????????storeId");
        Assert.hasKeyAndValue(reqJson, "specCd", "????????????????????????specCd");


        ContractChangePlanDetailAttrPo contractChangePlanDetailAttrPo = BeanConvertUtil.covertBean(reqJson, ContractChangePlanDetailAttrPo.class);
        return saveContractChangePlanDetailAttrBMOImpl.save(contractChangePlanDetailAttrPo);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/updateContractChangePlanDetailAttr
     * @path /app/contract/updateContractChangePlanDetailAttr
     */
    @RequestMapping(value = "/updateContractChangePlanDetailAttr", method = RequestMethod.POST)
    public ResponseEntity<String> updateContractChangePlanDetailAttr(@RequestBody JSONObject reqJson) {

        Assert.hasKeyAndValue(reqJson, "contractId", "????????????????????????contractId");
        Assert.hasKeyAndValue(reqJson, "detailId", "????????????????????????detailId");
        Assert.hasKeyAndValue(reqJson, "storeId", "????????????????????????storeId");
        Assert.hasKeyAndValue(reqJson, "specCd", "????????????????????????specCd");
        Assert.hasKeyAndValue(reqJson, "attrId", "attrId????????????");

        ContractChangePlanDetailAttrPo contractChangePlanDetailAttrPo = BeanConvertUtil.covertBean(reqJson, ContractChangePlanDetailAttrPo.class);
        return updateContractChangePlanDetailAttrBMOImpl.update(contractChangePlanDetailAttrPo);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/deleteContractChangePlanDetailAttr
     * @path /app/contract/deleteContractChangePlanDetailAttr
     */
    @RequestMapping(value = "/deleteContractChangePlanDetailAttr", method = RequestMethod.POST)
    public ResponseEntity<String> deleteContractChangePlanDetailAttr(@RequestBody JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "communityId", "??????ID????????????");

        Assert.hasKeyAndValue(reqJson, "attrId", "attrId????????????");


        ContractChangePlanDetailAttrPo contractChangePlanDetailAttrPo = BeanConvertUtil.covertBean(reqJson, ContractChangePlanDetailAttrPo.class);
        return deleteContractChangePlanDetailAttrBMOImpl.delete(contractChangePlanDetailAttrPo);
    }

    /**
     * ????????????????????????
     *
     * @param storeId ??????ID
     * @return
     * @serviceCode /contract/queryContractChangePlanDetailAttr
     * @path /app/contract/queryContractChangePlanDetailAttr
     */
    @RequestMapping(value = "/queryContractChangePlanDetailAttr", method = RequestMethod.GET)
    public ResponseEntity<String> queryContractChangePlanDetailAttr(@RequestHeader(value = "store-id") String storeId,
                                                                    @RequestParam(value = "page") int page,
                                                                    @RequestParam(value = "row") int row) {
        ContractChangePlanDetailAttrDto contractChangePlanDetailAttrDto = new ContractChangePlanDetailAttrDto();
        contractChangePlanDetailAttrDto.setPage(page);
        contractChangePlanDetailAttrDto.setRow(row);
        contractChangePlanDetailAttrDto.setStoreId(storeId);
        return getContractChangePlanDetailAttrBMOImpl.get(contractChangePlanDetailAttrDto);
    }

    /**
     * ????????????
     *
     * @param storeId ??????ID
     * @return
     * @serviceCode /contract/printContractTemplate
     * @path /app/contract/printContractTemplate
     */
    @RequestMapping(value = "/printContractTemplate", method = RequestMethod.GET)
    public ResponseEntity<String> printContractTemplate(@RequestHeader(value = "store-id") String storeId,
                                                        @RequestParam(value = "contractTypeId", required = false) String contractTypeId,
                                                        @RequestParam(value = "contractId", required = false) String contractId,
                                                        @RequestParam(value = "page") int page,
                                                        @RequestParam(value = "row") int row) {
        ContractTypeTemplateDto contractTypeTemplateDto = new ContractTypeTemplateDto();
        contractTypeTemplateDto.setPage(page);
        contractTypeTemplateDto.setRow(row);
        contractTypeTemplateDto.setStoreId(storeId);
        contractTypeTemplateDto.setContractTypeId(contractTypeId);

        ContractDto contractDto = new ContractDto();
        contractDto.setPage(page);
        contractDto.setRow(row);
        contractDto.setStoreId(storeId);
        contractDto.setContractId(contractId);

        ContractTypeSpecDto contractTypeSpecDto = new ContractTypeSpecDto();
        contractTypeSpecDto.setPage(page);
        contractTypeSpecDto.setRow(100);
        contractTypeSpecDto.setStoreId(storeId);
        contractTypeSpecDto.setContractTypeId(contractTypeId);

        return printContractTemplateBMO.get(contractTypeTemplateDto, contractDto, contractTypeSpecDto);
    }


    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/saveContractCollectionPlan
     * @path /app/contract/saveContractCollectionPlan
     */
    @RequestMapping(value = "/saveContractCollectionPlan", method = RequestMethod.POST)
    public ResponseEntity<String> saveContractCollectionPlan(@RequestHeader(value = "store-id") String storeId,
                                                             @RequestBody JSONObject reqJson) {

        Assert.hasKeyAndValue(reqJson, "contractId", "????????????????????????contractId");
        Assert.hasKeyAndValue(reqJson, "feeId", "????????????????????????feeId");
        Assert.hasKeyAndValue(reqJson, "storeId", "????????????????????????storeId");
        Assert.hasKeyAndValue(reqJson, "planName", "????????????????????????planName");


        ContractCollectionPlanPo contractCollectionPlanPo = BeanConvertUtil.covertBean(reqJson, ContractCollectionPlanPo.class);
        contractCollectionPlanPo.setStoreId(storeId);
        return saveContractCollectionPlanBMOImpl.save(contractCollectionPlanPo);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/updateContractCollectionPlan
     * @path /app/contract/updateContractCollectionPlan
     */
    @RequestMapping(value = "/updateContractCollectionPlan", method = RequestMethod.POST)
    public ResponseEntity<String> updateContractCollectionPlan(@RequestHeader(value = "store-id") String storeId,
                                                               @RequestBody JSONObject reqJson) {

        Assert.hasKeyAndValue(reqJson, "contractId", "????????????????????????contractId");
        Assert.hasKeyAndValue(reqJson, "feeId", "????????????????????????feeId");
        Assert.hasKeyAndValue(reqJson, "storeId", "????????????????????????storeId");
        Assert.hasKeyAndValue(reqJson, "planName", "????????????????????????planName");
        Assert.hasKeyAndValue(reqJson, "planId", "planId????????????");


        ContractCollectionPlanPo contractCollectionPlanPo = BeanConvertUtil.covertBean(reqJson, ContractCollectionPlanPo.class);
        contractCollectionPlanPo.setStoreId(storeId);
        return updateContractCollectionPlanBMOImpl.update(contractCollectionPlanPo);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/deleteContractCollectionPlan
     * @path /app/contract/deleteContractCollectionPlan
     */
    @RequestMapping(value = "/deleteContractCollectionPlan", method = RequestMethod.POST)
    public ResponseEntity<String> deleteContractCollectionPlan(@RequestHeader(value = "store-id") String storeId,
                                                               @RequestBody JSONObject reqJson) {

        Assert.hasKeyAndValue(reqJson, "planId", "planId????????????");


        ContractCollectionPlanPo contractCollectionPlanPo = BeanConvertUtil.covertBean(reqJson, ContractCollectionPlanPo.class);
        contractCollectionPlanPo.setStoreId(storeId);
        return deleteContractCollectionPlanBMOImpl.delete(contractCollectionPlanPo);
    }

    /**
     * ????????????????????????
     *
     * @param storeId ??????ID
     * @return
     * @serviceCode /contract/queryContractCollectionPlan
     * @path /app/contract/queryContractCollectionPlan
     */
    @RequestMapping(value = "/queryContractCollectionPlan", method = RequestMethod.GET)
    public ResponseEntity<String> queryContractCollectionPlan(@RequestHeader(value = "store-id") String storeId,
                                                              @RequestParam(value = "communityId") String communityId,
                                                              @RequestParam(value = "page") int page,
                                                              @RequestParam(value = "row") int row) {
        ContractCollectionPlanDto contractCollectionPlanDto = new ContractCollectionPlanDto();
        contractCollectionPlanDto.setPage(page);
        contractCollectionPlanDto.setRow(row);
        contractCollectionPlanDto.setStoreId(storeId);
        return getContractCollectionPlanBMOImpl.get(contractCollectionPlanDto);
    }


    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/saveContractRoom
     * @path /app/contract/saveContractRoom
     */
    @RequestMapping(value = "/saveContractRoom", method = RequestMethod.POST)
    public ResponseEntity<String> saveContractRoom(@RequestBody JSONObject reqJson) {

        Assert.hasKeyAndValue(reqJson, "contractId", "????????????????????????contractId");
        Assert.hasKeyAndValue(reqJson, "roomId", "????????????????????????roomId");


        ContractRoomPo contractRoomPo = BeanConvertUtil.covertBean(reqJson, ContractRoomPo.class);
        return saveContractRoomBMOImpl.save(contractRoomPo);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/updateContractRoom
     * @path /app/contract/updateContractRoom
     */
    @RequestMapping(value = "/updateContractRoom", method = RequestMethod.POST)
    public ResponseEntity<String> updateContractRoom(@RequestBody JSONObject reqJson) {

        Assert.hasKeyAndValue(reqJson, "contractId", "????????????????????????contractId");
        Assert.hasKeyAndValue(reqJson, "roomId", "????????????????????????roomId");
        Assert.hasKeyAndValue(reqJson, "crId", "crId????????????");


        ContractRoomPo contractRoomPo = BeanConvertUtil.covertBean(reqJson, ContractRoomPo.class);
        return updateContractRoomBMOImpl.update(contractRoomPo);
    }

    /**
     * ????????????????????????
     *
     * @param reqJson
     * @return
     * @serviceCode /contract/deleteContractRoom
     * @path /app/contract/deleteContractRoom
     */
    @RequestMapping(value = "/deleteContractRoom", method = RequestMethod.POST)
    public ResponseEntity<String> deleteContractRoom(@RequestBody JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "communityId", "??????ID????????????");

        Assert.hasKeyAndValue(reqJson, "crId", "crId????????????");


        ContractRoomPo contractRoomPo = BeanConvertUtil.covertBean(reqJson, ContractRoomPo.class);
        return deleteContractRoomBMOImpl.delete(contractRoomPo);
    }

    /**
     * ????????????????????????
     *
     * @param storeId ??????ID
     * @return
     * @serviceCode /contract/queryContractRoom
     * @path /app/contract/queryContractRoom
     */
    @RequestMapping(value = "/queryContractRoom", method = RequestMethod.GET)
    public ResponseEntity<String> queryContractRoom(@RequestHeader(value = "store-id") String storeId,
                                                    @RequestParam(value = "contractId", required = false) String contractId,
                                                    @RequestParam(value = "page") int page,
                                                    @RequestParam(value = "row") int row) {
        ContractRoomDto contractRoomDto = new ContractRoomDto();
        contractRoomDto.setPage(page);
        contractRoomDto.setRow(row);
        contractRoomDto.setStoreId(storeId);
        contractRoomDto.setContractId(contractId);
        return getContractRoomBMOImpl.get(contractRoomDto);
    }

}
