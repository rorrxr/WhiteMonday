package com.minju.order.entity;

import com.minju.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

//@Component
//@RequiredArgsConstructor
//@EnableScheduling
//public class OrderScheduler {
//
//    private final OrderService orderService;
//
//    @Scheduled(cron = "0 0 * * * ?") // 매 시간 정각에 실행
//    public void updateOrderStatus() {
//        orderService.updateOrderStatusScheduler();
//    }
//
//    @Scheduled(cron = "0 30 * * * ?") // 매 시간 30분마다 실행
//    public void processReturns() {
//        orderService.processReturns();
//    }
//}