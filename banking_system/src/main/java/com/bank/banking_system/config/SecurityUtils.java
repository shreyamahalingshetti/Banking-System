package com.bank.banking_system.config;

import com.bank.banking_system.entity.Account;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return "anonymous";
    }

    public static boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        }
        return false;
    }

    public static void checkAccountOwnership(Account account) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return;
        }
        if (isCurrentUserAdmin()) {
            return;
        }
        String currentEmail = authentication.getName();
        if (account != null && account.getCustomer() != null &&
                !account.getCustomer().getEmail().equalsIgnoreCase(currentEmail)) {
            throw new AccessDeniedException("Access denied: You do not have permission for this account");
        }
    }
}
