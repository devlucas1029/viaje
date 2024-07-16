package com.viaje.viaje.controller;

import com.viaje.viaje.model.*;
import com.viaje.viaje.repository.PlanDetailRepository;
import com.viaje.viaje.service.BoardService;
import com.viaje.viaje.service.TravelPlansService;
import com.viaje.viaje.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class BoardController {
    public final BoardService boardService;

    public final TravelPlansService travelPlansService;
    public final UserService userService;
    public final CommentsController commentsController;
    private final PlanDetailRepository planDetailRepository;

    public BoardController(BoardService boardService,
                           TravelPlansService travelPlansService,
                           UserService userService,
                           CommentsController commentsController,
                           PlanDetailRepository planDetailRepository) {
        this.boardService = boardService;
        this.travelPlansService = travelPlansService;
        this.userService = userService;
        this.commentsController = commentsController;
        this.planDetailRepository = planDetailRepository;
    }

    @GetMapping("/products")
    public String listPlans(HttpSession session, Model model) {
        List<Board> boardList = boardService.findAllBoardProduct();
        model.addAttribute("boardList", boardList);
        return "/board";
    }

    @GetMapping("/products/{type}")
    public String themePlans(@PathVariable("type") String type, HttpSession session,Model model ){
        List<Board> boardList = boardService.findProductByType(type);
        model.addAttribute("boardList",boardList);
        return "/board";
    }

    @GetMapping("/product_detail/{id}")
    public String productDetail(@PathVariable("id")Long id, HttpSession session, Model model){
        Users user = userService.findByEmail((String) session.getAttribute("user"));
        TravelPlans selectedPlan = travelPlansService.findByPlanId(id);
        List<Comments> comments = commentsController.getComments(id);
        List<PlanDetail> planDetails = planDetailRepository.findAllByTravelPlanOrderByPlanDateAscPlanTimeAsc(selectedPlan);
        session.setAttribute("selectedPlan",selectedPlan);
        model.addAttribute("selectedPlan", selectedPlan);
        model.addAttribute("user", user);
        model.addAttribute("comments",comments);
        model.addAttribute("planDetails", planDetails);
        return "/productDetail";
    }

    @GetMapping("/board/write")
    public String showWriteForm(Model model) {
        // 필요한 경우 모델에 데이터를 추가합니다.
        return "write";
    }
    @GetMapping("/qnaBoard")
    public String showQnaBoard(){
        return "qNa";
    }

}
