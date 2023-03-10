package com.java110.user.bmo.rentingConfig;
import com.java110.po.rentingConfig.RentingConfigPo;
import org.springframework.http.ResponseEntity;

public interface IUpdateRentingConfigBMO {


    /**
     * 修改房屋出租配置
     * add by wuxw
     * @param rentingConfigPo
     * @return
     */
    ResponseEntity<String> update(RentingConfigPo rentingConfigPo);


}
