package com.java110.boot.smo.assetExport;

import com.java110.core.context.IPageData;
import org.springframework.http.ResponseEntity;

/**
 * @ClassName IAssetImportSMO
 * @Description TODO
 * @Author wuxw
 * @Date 2019/9/23 23:13
 * @Version 1.0
 * add by wuxw 2019/9/23
 **/
public interface IExportMeterWaterSMO {

    /**
     * 导入excel数据
     * @param pd 前台数据封装
     * @return ResponseEntity
     */
    public ResponseEntity<Object> exportExcelData(IPageData pd) throws Exception;
    /**
     * 导入excel数据
     * @param pd 前台数据封装
     * @return ResponseEntity
     */
    public ResponseEntity<Object> exportExcelData2(IPageData pd) throws Exception;
}
