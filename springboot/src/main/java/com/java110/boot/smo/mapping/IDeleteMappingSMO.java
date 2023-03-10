package com.java110.boot.smo.mapping;

import com.java110.core.context.IPageData;
import org.springframework.http.ResponseEntity;

/**
 * 添加编码映射接口
 *
 * add by wuxw 2019-06-30
 */
public interface IDeleteMappingSMO {

    /**
     * 添加编码映射
     * @param pd 页面数据封装
     * @return ResponseEntity 对象
     */
    ResponseEntity<String> deleteMapping(IPageData pd);
}
