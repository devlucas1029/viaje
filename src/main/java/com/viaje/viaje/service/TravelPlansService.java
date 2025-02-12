package com.viaje.viaje.service;

import com.viaje.viaje.dto.PlanCertificationDTO;
import com.viaje.viaje.dto.PlanDetailDTO;
import com.viaje.viaje.dto.TravelPlansDTO;
import com.viaje.viaje.model.PlanCertification;
import com.viaje.viaje.model.PlanDetail;
import com.viaje.viaje.model.TravelPlans;
import com.viaje.viaje.model.Users;
import com.viaje.viaje.repository.PlanCertificationRepository;
import com.viaje.viaje.repository.PlanDetailRepository;
import com.viaje.viaje.repository.TravelPlansRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TravelPlansService {


    private final FileUploadUtil fileUploadUtil;
    private final TravelPlansRepository travelPlansRepository;

    private final BoardService boardService;
    private final UserService userService;
    private final TagsService tagsService;
    private final PlanCertificationRepository planCertificationRepository;
    private final PlanDetailRepository planDetailRepository;
    public TravelPlansService(FileUploadUtil fileUploadUtil, TravelPlansRepository travelPlansRepository, BoardService boardService, UserService userService, TagsService tagsService, PlanCertificationRepository planCertificationRepository, PlanDetailRepository planDetailRepository) {
        this.travelPlansRepository = travelPlansRepository;
        this.boardService = boardService;
        this.userService = userService;
        this.tagsService = tagsService;
        this.planCertificationRepository = planCertificationRepository;
        this.fileUploadUtil=fileUploadUtil;
        this.planDetailRepository = planDetailRepository;
    }
    @Transactional
    public TravelPlans createPlan(HttpSession session, TravelPlansDTO tpDTO, PlanCertificationDTO pcDTO,List<PlanDetailDTO> planDetails) throws IOException {
        String user_email = (String) session.getAttribute("user");
        Users user = userService.findByEmail(user_email);
        if (user == null) {
            throw new IllegalArgumentException("User is required to create a travel plan.");
        }
//        plan이미지저장
        List<String> planFilePaths = new ArrayList<>();
        for (MultipartFile planImage : tpDTO.getPlanImages()) {
            String planFileName = fileUploadUtil.saveFile(planImage, true);
            String planFilePath = fileUploadUtil.getPlanUploadDir() + "/" + planFileName;
            planFilePaths.add(planFileName);
        }
//        인증 이미지 저장
        List<String> certFilePaths = new ArrayList<>();
        for (MultipartFile certImage : pcDTO.getCertImages()) {
            String certFileName = fileUploadUtil.saveFile(certImage, false);
            String certFilePath = fileUploadUtil.getCertUploadDir() + "/" + certFileName;
            certFilePaths.add(certFileName);
        }

        Map<String,Number> durations = calculateDurationAndPrice(tpDTO);
//        travelPlan저장
        TravelPlans travelPlans = TravelPlans.builder()
                .nights((Long) durations.get("nights"))
                .days((Long) durations.get("days"))
                .price((Integer) durations.get("price"))
                .startDate(tpDTO.getStartDate())
                .endDate(tpDTO.getEndDate())
                .nation(tpDTO.getNation())
                .title(tpDTO.getTitle())
                .detail(tpDTO.getDetail())
                .imagePaths(planFilePaths)
                .totalBudget(tpDTO.getTotalBudget())
                .user(user)
                .build();

        TravelPlans savedPlan = travelPlansRepository.save(travelPlans);
//        plan인증 저장
        PlanCertification planCertification = PlanCertification.builder()
                .travelPlans(savedPlan)
                .certImagePaths(certFilePaths)
                .build();
        planCertificationRepository.save(planCertification);
// 여러 개의 계획 내용 저장
        for (PlanDetailDTO pdDTO : planDetails) {
            PlanDetail planDetail = PlanDetail.builder()
                    .travelPlan(savedPlan)
                    .planDate(pdDTO.getPlanDate())
                    .planTime(pdDTO.getPlanTime())
                    .activity(pdDTO.getActivity())
                    .description(pdDTO.getDescription())
                    .build();
            planDetailRepository.save(planDetail);
        }
        tagsService.insertPlanTag(session, travelPlans);
        boardService.postPlan(user, travelPlans);
        return travelPlans;
    }

    @Transactional
    public TravelPlans updateplan(Users user, Long updatePlanId, TravelPlansDTO tpDTO, List<PlanDetailDTO> planDetails) throws IOException {
        TravelPlans updatePlan = travelPlansRepository.findById(updatePlanId).orElseThrow();

        if (user == null) {
            throw new IllegalArgumentException("User is required to update a travel plan.");
        }
        Map<String,Number> durations = calculateDurationAndPrice(tpDTO);
//        travelPlan저장

        updatePlan = TravelPlans.builder()
                .planId(updatePlanId)
                .nights((Long) durations.get("nights"))
                .days((Long) durations.get("days"))
                .price((Integer) durations.get("price"))
                .startDate(tpDTO.getStartDate())
                .endDate(tpDTO.getEndDate())
                .nation(tpDTO.getNation())
                .title(tpDTO.getTitle())
                .detail(tpDTO.getDetail())
                .sold(updatePlan.getSold())
                .imagePaths(updatePlan.getImagePaths())
                .status(TravelPlans.PlanStatus.PENDING)
                .totalBudget(tpDTO.getTotalBudget())
                .user(user)
                .build();

        TravelPlans updatedPlan = travelPlansRepository.save(updatePlan);

        planDetailRepository.deleteAllByTravelPlan(updatedPlan);
// 여러 개의 계획 내용 저장
        for (PlanDetailDTO pdDTO : planDetails) {
            PlanDetail planDetail = PlanDetail.builder()
                    .travelPlan(updatedPlan)
                    .planDate(pdDTO.getPlanDate())
                    .planTime(pdDTO.getPlanTime())
                    .activity(pdDTO.getActivity())
                    .description(pdDTO.getDescription())
                    .build();
            planDetailRepository.save(planDetail);
        }

        return updatedPlan;
    }

    public TravelPlans findByPlanId(Long planId){
        TravelPlans plan = travelPlansRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("TravelPlan not found"));

        return plan;
    }

    public String deletePlan(HttpSession session, Long planId) {
        TravelPlans plan = travelPlansRepository.findById(planId)
                .orElseThrow();
        return "plan deleted";
    }


    private Map<String, Number> calculateDurationAndPrice(TravelPlansDTO tpDTO) {
        Map<String, Number> durations = new HashMap<>();
        if (tpDTO.getStartDate() != null && tpDTO.getEndDate() != null) {
            long nights = ChronoUnit.DAYS.between(tpDTO.getStartDate(), tpDTO.getEndDate());
            long days = nights + 1;
            int price;
            if (nights > 10) {
                price = 5000;
            } else if (nights > 7) {
                price = 5000;
            } else if (nights > 5) {
                price = 4000;
            } else if (nights > 3) {
                price = 3000;
            } else {
                price = 1000;
            }

            durations.put("nights", nights);
            durations.put("days", days);
            durations.put("price", price);

            return durations;
        }

        return null; // 또는 예외 처리
    }

    public List<TravelPlans> findPendingTravelPlans() {
        return travelPlansRepository.findByStatus(TravelPlans.PlanStatus.PENDING);
    }

    public void approvePlan(Long id) {
        TravelPlans plan = travelPlansRepository.findById(id).orElse(null);
        if (plan != null && plan.getStatus() == TravelPlans.PlanStatus.PENDING) {
            plan.setStatus(TravelPlans.PlanStatus.APPROVED);
            travelPlansRepository.save(plan);
        }

    }

    public List<PlanDetail> findPlanDetailByPlan(TravelPlans updatePlan) {
        return planDetailRepository.findAllByTravelPlanOrderByPlanDateAscPlanTimeAsc(updatePlan);
    }

    public TravelPlans getPlanById(Long planId) {
        return travelPlansRepository.findById(planId).orElseThrow();
    }
}
