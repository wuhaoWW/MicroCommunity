package com.java110.common.listener.attendanceClasses;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.po.attendanceClasses.AttendanceClassesPo;
import com.java110.utils.constant.BusinessTypeConstant;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.constant.StatusConstant;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.core.annotation.Java110Listener;
import com.java110.core.context.DataFlowContext;
import com.java110.entity.center.Business;
import com.java110.common.dao.IAttendanceClassesServiceDao;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 删除考勤班次信息 侦听
 *
 * 处理节点
 * 1、businessAttendanceClasses:{} 考勤班次基本信息节点
 * 2、businessAttendanceClassesAttr:[{}] 考勤班次属性信息节点
 * 3、businessAttendanceClassesPhoto:[{}] 考勤班次照片信息节点
 * 4、businessAttendanceClassesCerdentials:[{}] 考勤班次证件信息节点
 * 协议地址 ：https://github.com/java110/MicroCommunity/wiki/%E5%88%A0%E9%99%A4%E5%95%86%E6%88%B7%E4%BF%A1%E6%81%AF-%E5%8D%8F%E8%AE%AE
 * Created by wuxw on 2018/5/18.
 */
@Java110Listener("deleteAttendanceClassesInfoListener")
@Transactional
public class DeleteAttendanceClassesInfoListener extends AbstractAttendanceClassesBusinessServiceDataFlowListener {

    private final static Logger logger = LoggerFactory.getLogger(DeleteAttendanceClassesInfoListener.class);
    @Autowired
    IAttendanceClassesServiceDao attendanceClassesServiceDaoImpl;

    @Override
    public int getOrder() {
        return 3;
    }

    @Override
    public String getBusinessTypeCd() {
        return BusinessTypeConstant.BUSINESS_TYPE_DELETE_ATTENDANCE_CLASSES;
    }

    /**
     * 根据删除信息 查出Instance表中数据 保存至business表 （状态写DEL） 方便撤单时直接更新回去
     * @param dataFlowContext 数据对象
     * @param business 当前业务对象
     */
    @Override
    protected void doSaveBusiness(DataFlowContext dataFlowContext, Business business) {
        JSONObject data = business.getDatas();

        Assert.notEmpty(data,"没有datas 节点，或没有子节点需要处理");

            //处理 businessAttendanceClasses 节点
            if(data.containsKey(AttendanceClassesPo.class.getSimpleName())){
                Object _obj = data.get(AttendanceClassesPo.class.getSimpleName());
                JSONArray businessAttendanceClassess = null;
                if(_obj instanceof JSONObject){
                    businessAttendanceClassess = new JSONArray();
                    businessAttendanceClassess.add(_obj);
                }else {
                    businessAttendanceClassess = (JSONArray)_obj;
                }
                //JSONObject businessAttendanceClasses = data.getJSONObject(AttendanceClassesPo.class.getSimpleName());
                for (int _attendanceClassesIndex = 0; _attendanceClassesIndex < businessAttendanceClassess.size();_attendanceClassesIndex++) {
                    JSONObject businessAttendanceClasses = businessAttendanceClassess.getJSONObject(_attendanceClassesIndex);
                    doBusinessAttendanceClasses(business, businessAttendanceClasses);
                    if(_obj instanceof JSONObject) {
                        dataFlowContext.addParamOut("classesId", businessAttendanceClasses.getString("classesId"));
                    }
                }

        }


    }

    /**
     * 删除 instance数据
     * @param dataFlowContext 数据对象
     * @param business 当前业务对象
     */
    @Override
    protected void doBusinessToInstance(DataFlowContext dataFlowContext, Business business) {
        String bId = business.getbId();
        //Assert.hasLength(bId,"请求报文中没有包含 bId");

        //考勤班次信息
        Map info = new HashMap();
        info.put("bId",business.getbId());
        info.put("operate",StatusConstant.OPERATE_DEL);

        //考勤班次信息
        List<Map> businessAttendanceClassesInfos = attendanceClassesServiceDaoImpl.getBusinessAttendanceClassesInfo(info);
        if( businessAttendanceClassesInfos != null && businessAttendanceClassesInfos.size() >0) {
            for (int _attendanceClassesIndex = 0; _attendanceClassesIndex < businessAttendanceClassesInfos.size();_attendanceClassesIndex++) {
                Map businessAttendanceClassesInfo = businessAttendanceClassesInfos.get(_attendanceClassesIndex);
                flushBusinessAttendanceClassesInfo(businessAttendanceClassesInfo,StatusConstant.STATUS_CD_INVALID);
                attendanceClassesServiceDaoImpl.updateAttendanceClassesInfoInstance(businessAttendanceClassesInfo);
                dataFlowContext.addParamOut("classesId",businessAttendanceClassesInfo.get("classes_id"));
            }
        }

    }

    /**
     * 撤单
     * 从business表中查询到DEL的数据 将instance中的数据更新回来
     * @param dataFlowContext 数据对象
     * @param business 当前业务对象
     */
    @Override
    protected void doRecover(DataFlowContext dataFlowContext, Business business) {
        String bId = business.getbId();
        //Assert.hasLength(bId,"请求报文中没有包含 bId");
        Map info = new HashMap();
        info.put("bId",bId);
        info.put("statusCd",StatusConstant.STATUS_CD_INVALID);

        Map delInfo = new HashMap();
        delInfo.put("bId",business.getbId());
        delInfo.put("operate",StatusConstant.OPERATE_DEL);
        //考勤班次信息
        List<Map> attendanceClassesInfo = attendanceClassesServiceDaoImpl.getAttendanceClassesInfo(info);
        if(attendanceClassesInfo != null && attendanceClassesInfo.size() > 0){

            //考勤班次信息
            List<Map> businessAttendanceClassesInfos = attendanceClassesServiceDaoImpl.getBusinessAttendanceClassesInfo(delInfo);
            //除非程序出错了，这里不会为空
            if(businessAttendanceClassesInfos == null ||  businessAttendanceClassesInfos.size() == 0){
                throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_INNER_ERROR,"撤单失败（attendanceClasses），程序内部异常,请检查！ "+delInfo);
            }
            for (int _attendanceClassesIndex = 0; _attendanceClassesIndex < businessAttendanceClassesInfos.size();_attendanceClassesIndex++) {
                Map businessAttendanceClassesInfo = businessAttendanceClassesInfos.get(_attendanceClassesIndex);
                flushBusinessAttendanceClassesInfo(businessAttendanceClassesInfo,StatusConstant.STATUS_CD_VALID);
                attendanceClassesServiceDaoImpl.updateAttendanceClassesInfoInstance(businessAttendanceClassesInfo);
            }
        }
    }



    /**
     * 处理 businessAttendanceClasses 节点
     * @param business 总的数据节点
     * @param businessAttendanceClasses 考勤班次节点
     */
    private void doBusinessAttendanceClasses(Business business,JSONObject businessAttendanceClasses){

        Assert.jsonObjectHaveKey(businessAttendanceClasses,"classesId","businessAttendanceClasses 节点下没有包含 classesId 节点");

        if(businessAttendanceClasses.getString("classesId").startsWith("-")){
            throw new ListenerExecuteException(ResponseConstant.RESULT_PARAM_ERROR,"classesId 错误，不能自动生成（必须已经存在的classesId）"+businessAttendanceClasses);
        }
        //自动插入DEL
        autoSaveDelBusinessAttendanceClasses(business,businessAttendanceClasses);
    }
    @Override
    public IAttendanceClassesServiceDao getAttendanceClassesServiceDaoImpl() {
        return attendanceClassesServiceDaoImpl;
    }

    public void setAttendanceClassesServiceDaoImpl(IAttendanceClassesServiceDao attendanceClassesServiceDaoImpl) {
        this.attendanceClassesServiceDaoImpl = attendanceClassesServiceDaoImpl;
    }
}
