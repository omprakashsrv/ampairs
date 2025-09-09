package com.ampairs.workspace.controller

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

/**
 * Controller for the admin panel home page
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("@superAdminAuth.isSuperAdmin(authentication)")
class AdminIndexController {
    
    /**
     * Display the admin panel home page
     */
    @GetMapping
    fun adminIndex(): String {
        return "admin/index"
    }
}