package com.java110.acct.coupon;

import com.java110.dto.couponPropertyUser.CouponPropertyUserDto;
import com.java110.dto.couponPropertyUser.CouponQrCodeDto;

public interface ICouponAdapt {

    public static final String COUPON_PRE= "couponProperty";

    /**
     * 生成 优惠券核销二维码
     * @param couponPropertyUserDto
     * @return
     */
    CouponQrCodeDto generatorQrcode(CouponPropertyUserDto couponPropertyUserDto);
}
