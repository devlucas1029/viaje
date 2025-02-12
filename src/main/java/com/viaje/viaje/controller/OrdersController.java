package com.viaje.viaje.controller;

import com.viaje.viaje.model.OrderItems;
import com.viaje.viaje.model.Orders;
import com.viaje.viaje.model.PointTransaction;
import com.viaje.viaje.repository.OrdersItemRepository;
import com.viaje.viaje.repository.OrdersRepository;
import com.viaje.viaje.service.OrdersService;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class OrdersController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final OrdersService ordersService;
    private final OrdersRepository ordersRepository;
    private final OrdersItemRepository ordersItemRepository;
    public OrdersController(OrdersService ordersService, OrdersRepository ordersRepository, OrdersItemRepository ordersItemRepository) {
        this.ordersService = ordersService;
        this.ordersRepository = ordersRepository;
        this.ordersItemRepository = ordersItemRepository;
    }

    @PostMapping("/order")
    public String orderPage (HttpSession session, Model model){
//        if (session.getAttribute("failOrderId") != null) {
//            List<OrderItems> failOrderItems = ordersItemRepository.findAllByOrders_OrderId((Long) session.getAttribute("failOrderId"));
//            model.addAttribute("orderItemList", failOrderItems);
//
//        }else {
            List<OrderItems> newOrderList = ordersService.createOrderItems(session, model);
            model.addAttribute("orderItemList", newOrderList);
//        }
        return "/orderPage";
    }

    @PostMapping("/order/create")
    public String createOrders(@RequestParam Long orderId, HttpSession session, Model model, RedirectAttributes redirectAttributes){
        ordersService.payorder(orderId,session,model);
        Orders ordered = ordersRepository.findById(orderId).orElseThrow();
        if (ordered.getOrderStatus().equals(Orders.OrderStatus.COMPLETED)) {
            model.addAttribute("order", ordered);
            if (session.getAttribute("failOrderId") != null){
                session.removeAttribute("failOrderId");
            }
            return "order_success";
        } else if (ordered.getOrderStatus().equals(Orders.OrderStatus.CANCELLED)) {
            model.addAttribute("error","포인트가 부족합니다.");
            session.setAttribute("failOrderId",ordered.getOrderId());
            return "orderFail";
        }else{
            return "cart";
        }

    }

    @GetMapping("/adminOrderHistory")
    public String adminOrderHistory(Model model) {
        List<OrderItems> ordersItemList = ordersItemRepository.findAll();
        model.addAttribute("ordersItemList", ordersItemList);
        return "orderHistory";
    }

    @GetMapping("/adminSales")
    public String adminSalesList(Model model) {
        List<OrderItems> salesList = ordersItemRepository.findAll();


        // 총 판매액 계산
        int totalSalesAmount = salesList.stream()
                .mapToInt(sale -> sale.getOrders().getTotal_amount() * sale.getQuantity())
                .sum();


        // 총 주문 수 계산
        int totalOrdersCount = (int) salesList.stream()
                .map(OrderItems::getOrders)
                .distinct()
                .count();

        // 모델에 데이터 추가
        model.addAttribute("salesList", salesList);
        model.addAttribute("totalSalesAmount", totalSalesAmount);
        model.addAttribute("totalOrdersCount", totalOrdersCount);

        return "sales";
    }


}
