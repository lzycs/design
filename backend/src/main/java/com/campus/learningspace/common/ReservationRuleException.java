package com.campus.learningspace.common;

/**
 * 预约规则校验失败（次数/时长等）
 */
public class ReservationRuleException extends RuntimeException {

    public ReservationRuleException(String message) {
        super(message);
    }
}
