package com.bank.banking_system.service;

import com.bank.banking_system.entity.AuditLog;
import com.bank.banking_system.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog log(String action, String performedBy, String targetType, String targetId, String details) {
        String actor = (performedBy != null && !performedBy.isBlank()) ? performedBy : "anonymous";
        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .performedBy(actor)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .build();
        return auditLogRepository.save(auditLog);
    }
}
