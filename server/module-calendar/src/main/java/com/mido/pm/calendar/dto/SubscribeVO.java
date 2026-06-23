package com.mido.pm.calendar.dto;

/** 日历订阅信息：token 与匿名 ics 订阅地址（相对路径，前端拼当前域名）。 */
public record SubscribeVO(
        String token,
        String icsUrl) {
}
