package com.viaje.viaje.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pointTransactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    private Users user;

    @Column(nullable = false)
    private Integer chargeAmount;

    @Column(nullable = false)
    private Integer chargePoint;

    @Column(nullable = false)
    private LocalDateTime createdAt;  // 생성 일시

    @Column(nullable = false)
    private LocalDateTime updatedAt;  // 업데이트 일시

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus transactionStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    public enum TransactionStatus{
        PENDING, PROCESSING, COMPLETED, CANCELLED
    }
    public enum TransactionType{
        CREDIT, DEBIT
    }

    @PrePersist  // 엔티티가 저장되기 전에 실행될 메서드를 지정하는 JPA 어노테이션
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();  // 현재 시간을 생성 일시로 설정
        this.updatedAt = LocalDateTime.now();  // 현재 시간을 업데이트 일시로 설정
        if (transactionStatus== null) {
            transactionStatus= TransactionStatus.PENDING;
        }
        if (transactionType== null) {
            transactionType= TransactionType.CREDIT;
        }

    }
}
