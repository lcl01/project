package com.changgou.service;

import java.util.Map;

public interface WeixinPayService {
    /*****
     * 创建二维码
//     * @param out_trade_no : 客户端自定义订单编号
//     * @param total_fee    : 交易金额,单位：分
     * @param parameter
     * @return
     */
//    public Map createNative(String out_trade_no, String total_fee);
    public Map createNative(Map<String,String> parameter);
    /***
     * 查询订单状态
     * @param out_trade_no : 客户端自定义订单编号
     * @return
     */
    public Map queryPayStatus(String out_trade_no);
    /***
     * 关闭支付
     * @param orderId
     * @return
     */
    Map<String,String> closePay(Long orderId) throws Exception;
}
