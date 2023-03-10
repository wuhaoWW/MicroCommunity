package com.java110.store.cmd.store;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.account.AccountDto;
import com.java110.dto.store.StoreDto;
import com.java110.dto.workflow.WorkflowDto;
import com.java110.intf.acct.IAccountInnerServiceSMO;
import com.java110.intf.common.IWorkflowV1InnerServiceSMO;
import com.java110.intf.community.ICommunityMemberV1InnerServiceSMO;
import com.java110.intf.community.ICommunityV1InnerServiceSMO;
import com.java110.intf.store.IOrgStaffRelV1InnerServiceSMO;
import com.java110.intf.store.IStoreAttrV1InnerServiceSMO;
import com.java110.intf.store.IStoreUserV1InnerServiceSMO;
import com.java110.intf.store.IStoreV1InnerServiceSMO;
import com.java110.intf.user.*;
import com.java110.po.account.AccountPo;
import com.java110.po.org.OrgPo;
import com.java110.po.org.OrgStaffRelPo;
import com.java110.po.privilegeUser.PrivilegeUserPo;
import com.java110.po.store.StoreAttrPo;
import com.java110.po.store.StorePo;
import com.java110.po.store.StoreUserPo;
import com.java110.po.workflow.WorkflowPo;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.constant.StoreTypeConstant;
import com.java110.utils.constant.StoreUserRelConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;

@Java110Cmd(serviceCode = "save.store.info")
public class SaveStoreInfoCmd extends Cmd {

    public static final String CODE_PREFIX_ID = "10";

    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMOImpl;

    @Autowired
    private IStoreAttrV1InnerServiceSMO storeAttrV1InnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    @Autowired
    private IStoreUserV1InnerServiceSMO storeUserV1InnerServiceSMOImpl;

    @Autowired
    private IOrgV1InnerServiceSMO orgV1InnerServiceSMOImpl;

    @Autowired
    private IOrgStaffRelV1InnerServiceSMO orgStaffRelV1InnerServiceSMOImpl;

    @Autowired
    private IWorkflowV1InnerServiceSMO workflowV1InnerServiceSMOImpl;

    @Autowired
    private IPrivilegeUserV1InnerServiceSMO privilegeUserV1InnerServiceSMOImpl;

    @Autowired
    private ICommunityMemberV1InnerServiceSMO communityMemberV1InnerServiceSMOImpl;

    @Autowired
    private ICommunityV1InnerServiceSMO communityV1InnerServiceSMOImpl;

    @Autowired
    private IMenuGroupV1InnerServiceSMO menuGroupV1InnerServiceSMOImpl;

    @Autowired
    private IMenuGroupCommunityV1InnerServiceSMO menuGroupCommunityV1InnerServiceSMOImpl;

    @Autowired
    private IAccountInnerServiceSMO accountInnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.jsonObjectHaveKey(reqJson, StorePo.class.getSimpleName(), "????????????????????????businessStore ??????????????????");
    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        addStore(reqJson);
        //????????????
        addStaff(reqJson);
        //?????????????????????
        addOrg(reqJson);
        //????????????
        addStaffOrg(reqJson);
        addPurchase(reqJson);
        //businesses.add(storeBMOImpl.addCollection(paramObj)); //???????????? ?????????????????????????????????  ???????????????
        contractApply(reqJson);
        contractChange(reqJson);
        //??????????????????
        allocationStorehouse(reqJson);

        //???????????? ??????????????????????????????
        JSONObject businessStoreObj = reqJson.getJSONObject(StorePo.class.getSimpleName());
        if (StoreTypeConstant.STORE_TYPE_MALL.equals(businessStoreObj.getString("storeTypeCd"))) {
            //??????????????????
            addAccount(reqJson, AccountDto.ACCT_TYPE_CASH);
            addAccount(reqJson, AccountDto.ACCT_TYPE_INTEGRAL);
//            businesses.add(storeBMOImpl.addAccount(paramObj,AccountDto.ACCT_TYPE_GOLD));
        }
        //??????
        privilegeUserDefault(reqJson);
    }

    public void addAccount(JSONObject paramInJson, String acctType) {
        JSONObject businessStoreObj = paramInJson.getJSONObject(StorePo.class.getSimpleName());

        AccountPo accountPo = new AccountPo();
        accountPo.setAcctId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_acctId));

        accountPo.setAcctName(businessStoreObj.getString("name"));
        accountPo.setAcctType(acctType);
        accountPo.setAmount("0");
        accountPo.setObjId(paramInJson.getString("storeId"));
        accountPo.setObjType(AccountDto.OBJ_TYPE_STORE);
        int flag = accountInnerServiceSMOImpl.saveAccount(accountPo);
        if (flag < 1) {
            throw new CmdException("????????????");
        }
    }


    /**
     * ????????????
     *
     * @param paramInJson
     * @return
     */
    public void allocationStorehouse(JSONObject paramInJson) {

        WorkflowPo workflowPo = new WorkflowPo();
        workflowPo.setCommunityId("9999"); //????????????
        workflowPo.setFlowId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        workflowPo.setFlowName("????????????");
        workflowPo.setFlowType(WorkflowDto.FLOW_TYPE_ALLOCATION_STOREHOUSE);
        workflowPo.setSkipLevel(WorkflowDto.DEFAULT_SKIP_LEVEL);
        workflowPo.setStoreId(paramInJson.getString("storeId"));
        int flag = workflowV1InnerServiceSMOImpl.saveWorkflow(workflowPo);
        if (flag < 1) {
            throw new CmdException("????????????");
        }
    }

    /**
     * ????????????
     *
     * @param paramInJson
     * @return
     */
    public void contractChange(JSONObject paramInJson) {


        WorkflowPo workflowPo = new WorkflowPo();
        workflowPo.setCommunityId("9999"); //????????????
        workflowPo.setFlowId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        workflowPo.setFlowName("????????????");
        workflowPo.setFlowType(WorkflowDto.FLOW_TYPE_CONTRACT_CHANGE);
        workflowPo.setSkipLevel(WorkflowDto.DEFAULT_SKIP_LEVEL);
        workflowPo.setStoreId(paramInJson.getString("storeId"));
        int flag = workflowV1InnerServiceSMOImpl.saveWorkflow(workflowPo);
        if (flag < 1) {
            throw new CmdException("????????????");
        }
    }

    /**
     * ??????????????????
     *
     * @param paramInJson
     * @return
     */
    public void contractApply(JSONObject paramInJson) {

        WorkflowPo workflowPo = new WorkflowPo();
        workflowPo.setCommunityId("9999"); //????????????
        workflowPo.setFlowId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        workflowPo.setFlowName("??????????????????");
        workflowPo.setFlowType(WorkflowDto.FLOW_TYPE_CONTRACT_APPLY);
        workflowPo.setSkipLevel(WorkflowDto.DEFAULT_SKIP_LEVEL);
        workflowPo.setStoreId(paramInJson.getString("storeId"));
        int flag = workflowV1InnerServiceSMOImpl.saveWorkflow(workflowPo);
        if (flag < 1) {
            throw new CmdException("????????????");
        }
    }

    public void addPurchase(JSONObject paramInJson) {

        WorkflowPo workflowPo = new WorkflowPo();
        workflowPo.setCommunityId("9999"); //????????????
        workflowPo.setFlowId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        workflowPo.setFlowName("????????????");
        workflowPo.setFlowType(WorkflowDto.FLOW_TYPE_PURCHASE);
        workflowPo.setSkipLevel(WorkflowDto.DEFAULT_SKIP_LEVEL);
        workflowPo.setStoreId(paramInJson.getString("storeId"));
        int flag = workflowV1InnerServiceSMOImpl.saveWorkflow(workflowPo);
        if (flag < 1) {
            throw new CmdException("????????????");
        }
    }

    public void addStaffOrg(JSONObject paramInJson) {


        JSONObject businessOrgStaffRel = new JSONObject();
        businessOrgStaffRel.put("relId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_relId));
        businessOrgStaffRel.put("storeId", paramInJson.getString("storeId"));
        businessOrgStaffRel.put("staffId", paramInJson.getJSONObject(StorePo.class.getSimpleName()).getString("userId"));
        businessOrgStaffRel.put("orgId", paramInJson.getString("levelOneOrgId"));
        businessOrgStaffRel.put("relCd", StoreUserRelConstant.REL_ADMIN);
        //?????? ????????????
        OrgStaffRelPo orgStaffRelPo = BeanConvertUtil.covertBean(businessOrgStaffRel, OrgStaffRelPo.class);

        int flag = orgStaffRelV1InnerServiceSMOImpl.saveOrgStaffRel(orgStaffRelPo);
        if (flag < 1) {
            throw new CmdException("????????????");
        }
    }

    /**
     * ????????????????????????
     *
     * @param paramInJson ???????????????????????????
     * @return ?????????????????????????????????
     */
    public void addOrg(JSONObject paramInJson) {

        String orgId = GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_orgId);
        paramInJson.put("levelOneOrgId", orgId);

        JSONObject businessOrg = new JSONObject();
        businessOrg.put("orgName", paramInJson.getJSONObject(StorePo.class.getSimpleName()).getString("name"));
        businessOrg.put("orgLevel", "1");
        businessOrg.put("parentOrgId", orgId);
        businessOrg.put("belongCommunityId", "9999");
        businessOrg.put("orgId", orgId);
        businessOrg.put("allowOperation", "F");
        businessOrg.put("storeId", paramInJson.getString("storeId"));

        //?????? ????????????
        OrgPo orgPo = BeanConvertUtil.covertBean(businessOrg, OrgPo.class);

        int flag = orgV1InnerServiceSMOImpl.saveOrg(orgPo);
        if (flag < 1) {
            throw new CmdException("????????????");
        }
    }

    /**
     * ????????????
     *
     * @param paramInJson
     * @return
     */
    public void addStore(JSONObject paramInJson) {
        int flag = 0;
        String storeId = GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_storeId);
        paramInJson.put("storeId", storeId);
        if (paramInJson.containsKey(StorePo.class.getSimpleName())) {
            JSONObject businessStoreObj = paramInJson.getJSONObject(StorePo.class.getSimpleName());
            businessStoreObj.put("storeId", storeId);
            if (!businessStoreObj.containsKey("password")) {
                String staffDefaultPassword = MappingCache.getValue(MappingConstant.KEY_STAFF_DEFAULT_PASSWORD);
                Assert.hasLength(staffDefaultPassword, "???????????????????????????????????????????????????" + MappingConstant.KEY_STAFF_DEFAULT_PASSWORD);
                businessStoreObj.put("password", staffDefaultPassword);
            }

            if (!businessStoreObj.containsKey("mapX")) {
                businessStoreObj.put("mapX", "");
            }

            if (!businessStoreObj.containsKey("mapY")) {
                businessStoreObj.put("mapY", "");
            }
            StorePo storePo = BeanConvertUtil.covertBean(businessStoreObj, StorePo.class);
            storePo.setState(StoreDto.STATE_NORMAL);
            flag = storeV1InnerServiceSMOImpl.saveStore(storePo);

            if (flag < 1) {
                throw new CmdException("??????????????????");
            }
        }

        if (paramInJson.containsKey(StoreAttrPo.class.getSimpleName())) {
            JSONArray attrs = paramInJson.getJSONArray(StoreAttrPo.class.getSimpleName());

            for (int businessStoreAttrIndex = 0; businessStoreAttrIndex < attrs.size(); businessStoreAttrIndex++) {
                JSONObject attr = attrs.getJSONObject(businessStoreAttrIndex);
                attr.put("storeId", storeId);
                attr.put("attrId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
                StoreAttrPo storeAttrPo = BeanConvertUtil.covertBean(attr, StoreAttrPo.class);
                flag = storeAttrV1InnerServiceSMOImpl.saveStoreAttr(storeAttrPo);
                if (flag < 1) {
                    throw new CmdException("??????????????????");
                }
            }
        }
    }

    /**
     * ????????????
     *
     * @param paramInJson
     * @return
     */
    public void addStaff(JSONObject paramInJson) {

        JSONObject businessStoreUser = new JSONObject();
        businessStoreUser.put("storeId", paramInJson.getString("storeId"));
        businessStoreUser.put("storeUserId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_storeUserId));
        businessStoreUser.put("userId", paramInJson.getJSONObject(StorePo.class.getSimpleName()).getString("userId"));
        businessStoreUser.put("relCd", StoreUserRelConstant.REL_ADMIN);
        StoreUserPo storeUserPo = BeanConvertUtil.covertBean(businessStoreUser, StoreUserPo.class);
        int flag = storeUserV1InnerServiceSMOImpl.saveStoreUser(storeUserPo);
        if (flag < 1) {
            throw new CmdException("????????????");
        }

    }

    /**
     * ????????????
     *
     * @return
     */
    private void privilegeUserDefault(JSONObject paramObj) {

        String defaultPrivilege = MappingCache.getValue(MappingConstant.DOMAIN_DEFAULT_PRIVILEGE_ADMIN, paramObj.getJSONObject(StorePo.class.getSimpleName()).getString("storeTypeCd"));

        Assert.hasLength(defaultPrivilege, "?????????????????????");
        PrivilegeUserPo privilegeUserPo = new PrivilegeUserPo();
        privilegeUserPo.setPrivilegeFlag("1");
        privilegeUserPo.setStoreId(paramObj.getString("storeId"));
        privilegeUserPo.setUserId(paramObj.getJSONObject(StorePo.class.getSimpleName()).getString("userId"));
        privilegeUserPo.setpId(defaultPrivilege);
        privilegeUserPo.setPuId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));

        int flag = privilegeUserV1InnerServiceSMOImpl.savePrivilegeUser(privilegeUserPo);
        if (flag < 1) {
            throw new CmdException("????????????");
        }
    }
}
