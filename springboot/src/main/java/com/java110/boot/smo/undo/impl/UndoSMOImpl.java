package com.java110.boot.smo.undo.impl;

import com.alibaba.fastjson.JSONObject;
import com.java110.boot.smo.DefaultAbstractComponentSMO;
import com.java110.boot.smo.undo.IUndoSMO;
import com.java110.core.context.IPageData;
import com.java110.entity.component.ComponentValidateResult;
import com.java110.utils.exception.SMOException;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 查询carInout服务类
 */
@Service("undoSMOImpl")
public class UndoSMOImpl extends DefaultAbstractComponentSMO implements IUndoSMO {

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public ResponseEntity<String> listUndos(IPageData pd) throws SMOException {
        return businessProcess(pd);
    }

    @Override
    protected void validate(IPageData pd, JSONObject paramIn) {
        //Assert.hasKeyAndValue(paramIn, "communityId", "必填，请填写小区信息");
        super.validatePageInfo(pd);

        //super.checkUserHasPrivilege(pd, restTemplate, PrivilegeCodeConstant.AGENT_HAS_LIST_CARINOUT);
    }

    @Override
    protected ResponseEntity<String> doBusinessProcess(IPageData pd, JSONObject paramIn) {
        ComponentValidateResult result = super.validateStoreStaffCommunityRelationship(pd, restTemplate);


        Map<String, Object> headers = pd.getHeaders();
        headers.put("user-id", result.getUserId());
        headers.put("store-id", result.getStoreId());
        paramIn.put("storeId", result.getStoreId());
        paramIn.put("userId", result.getUserId());

        JSONObject doing = new JSONObject();
        //查询 报修待办
        String apiUrl = "ownerRepair.listStaffRepairs" + mapToUrlParam(paramIn);
        ResponseEntity<String> responseEntity = this.callCenterService(restTemplate, pd, "",
                apiUrl,
                HttpMethod.GET);

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
            doing.put("repair", paramOut.getString("total"));
        } else {
            doing.put("repair", "0");
        }
        paramIn.put("staffId", result.getUserId());


        apiUrl = "auditUser.listAuditComplaints" + mapToUrlParam(paramIn);
        responseEntity = this.callCenterService(restTemplate, pd, "",
                apiUrl,
                HttpMethod.GET);
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
            doing.put("complaint", paramOut.getString("total"));
        } else {
            doing.put("complaint", "0");
        }
        //采购待办
        apiUrl = "auditUser.listAuditOrders" + mapToUrlParam(paramIn);
        responseEntity = this.callCenterService(restTemplate, pd, "",
                apiUrl,
                HttpMethod.GET);
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
            doing.put("purchase", paramOut.getString("total"));
        } else {
            doing.put("purchase", "0");
        }
        //物品领用待办
        apiUrl = "/collection/getCollectionAuditOrder" + mapToUrlParam(paramIn);
        responseEntity = this.callCenterService(restTemplate, pd, "",
                apiUrl,
                HttpMethod.GET);
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
            doing.put("collection", paramOut.getString("total"));
        } else {
            doing.put("collection", "0");
        }

        //contract/queryContractTask
        //合同起草待办
        apiUrl = "/contract/queryContractTask" + mapToUrlParam(paramIn);
        responseEntity = this.callCenterService(restTemplate, pd, "",
                apiUrl,
                HttpMethod.GET);
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
            doing.put("contractApply", paramOut.getString("total"));
        } else {
            doing.put("contractApply", "0");
        }
        //contract/queryContractTask
        //合同变更
        apiUrl = "/contract/queryContractChangeTask" + mapToUrlParam(paramIn);
        responseEntity = this.callCenterService(restTemplate, pd, "",
                apiUrl,
                HttpMethod.GET);
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
            doing.put("contractChange", paramOut.getString("total"));
        } else {
            doing.put("contractChange", "0");
        }

        //合同变更
        apiUrl = "resourceStore.listAllocationStoreAuditOrders" + mapToUrlParam(paramIn);
        responseEntity = this.callCenterService(restTemplate, pd, "",
                apiUrl,
                HttpMethod.GET);
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
            doing.put("allocation", paramOut.getString("total"));
        } else {
            doing.put("allocation", "0");
        }

        return ResultVo.createResponseEntity(doing);
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
}
