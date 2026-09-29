package com.twekl.dashboard.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_followup_checks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderFollowupCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "preset_id")
    private Long presetId;

    @Column(name = "preset_name", nullable = false)
    private String presetName;

    @Column(name = "duration_value")
    private Integer durationValue;

    @Column(name = "duration_unit")
    private String durationUnit;

    @Builder.Default
    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @Column(name = "note", length = 2000)
    private String note;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "checked_by")
    private String checkedBy;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
