package com.viaje.viaje.controller;

import com.viaje.viaje.model.*;
import com.viaje.viaje.repository.OrdersRepository;
import com.viaje.viaje.repository.PlanDetailRepository;
import com.viaje.viaje.repository.PointTransactionRepository;
import com.viaje.viaje.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    public UserService userService;

    @Autowired
    public CommentsController commentsController;

    @Autowired
    public TravelPlansService travelPlansService;
    private PointTransactionRepository transactionRepository;

    private OrdersRepository ordersRepository;

    private PlanDetailRepository planDetailRepository;
    private TagsService tagsService;

    @Autowired
    public BoardService boardService;

    public AdminController(PointTransactionRepository transactionRepository, OrdersRepository ordersRepository, PlanDetailRepository planDetailRepository, TagsService tagsService) {
        this.transactionRepository = transactionRepository;
        this.ordersRepository = ordersRepository;
        this.planDetailRepository = planDetailRepository;
        this.tagsService = tagsService;
    }

    @GetMapping("/admin")
    public String listPlans(HttpSession session, Model model) {
        Integer userNumber = userService.findUserNumber();
        model.addAttribute("userNumber", userNumber);

        Integer totalChargePoint = transactionRepository.sumAllChargeAmounts();
        model.addAttribute("chargeTotalPoints", totalChargePoint);

        Long userPoints = userService.totalUserPoint();
        model.addAttribute("userTotalPoint", userPoints);

        Integer totalOrders = ordersRepository.findAllByOrderStatus(Orders.OrderStatus.COMPLETED).size();
        model.addAttribute("totalOrders", totalOrders);

        String email = (String) session.getAttribute("user");
        Boolean isAdmin = adminService.isAdmin(email);
        if (email != null && adminService.isAdmin(email)) {
            session.setAttribute("isAdmin",isAdmin);
            return "/admin";
        } else {
            return "redirect:/";
        }
    }


    @GetMapping("/products/admin")
    public String PlansStatus(Model model) {
        List<Board> pendingPlans = adminService.findPendingTravelPlans();
        model.addAttribute("boardList", pendingPlans);
        return "board";
    }

    @GetMapping("/product_detail/admin/{id}")
    public String productDetailAdmin(@PathVariable("id")Long id, HttpSession session, Model model){
        Users user = userService.findByEmail((String) session.getAttribute("user"));
        TravelPlans selectedPlan = travelPlansService.findByPlanId(id);
        List<Comments> comments = commentsController.getComments(id);
        List<PlanDetail> planDetails = planDetailRepository.findAllByTravelPlanOrderByPlanDateAscPlanTimeAsc(selectedPlan);
        List<Tags> planTags=tagsService.findTags(selectedPlan);

        // 관리자 권한 확인
        if (user != null && user.isAdmin()) {
            session.setAttribute("selectedPlan", selectedPlan);
            model.addAttribute("selectedPlan", selectedPlan);
            model.addAttribute("user", user);
            model.addAttribute("comments", comments);
            model.addAttribute("planDetails", planDetails);
            model.addAttribute("tagsList", planTags);
            return "/productDetail";
        } else {
            // 관리자가 아닌 경우, 다른 페이지로 리다이렉트하거나 권한 없음 메시지를 보여줄 수 있음
            return "redirect:/"; // 예시로 홈 페이지로 리다이렉트하는 것으로 설정
        }

    }

    @PostMapping("/plan/updateStatus")
    public String updateStatus(@RequestParam("planId") Long planId, @RequestParam("status") String status) {
        adminService.updatePlanStatus(planId, status);
        return "redirect:/products/admin";
    }

}
