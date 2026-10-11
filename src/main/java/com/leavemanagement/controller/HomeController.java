package com.leavemanagement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Web Controller for serving HTML pages. Page access by role is set in SecurityConfig.
 */
@Controller
@RequestMapping("/")
public class HomeController {

    @GetMapping
    public String index() {
        return "index";
    }

    @GetMapping("login")
    public String login() {
        return "login";
    }

    @GetMapping("change-password")
    public String changePassword() {
        return "change-password";
    }

    @GetMapping("profile")
    public String profile() {
        return "profile";
    }

    @GetMapping("employees")
    public String employees() {
        return "employees";
    }

    @GetMapping("leave-requests")
    public String leaveRequests() {
        return "leave-requests";
    }

    @GetMapping("comp-off")
    public String compOff() {
        return "comp-off";
    }

    @GetMapping("leave-balance")
    public String leaveBalance() {
        return "leave-balance";
    }

    @GetMapping("dashboard")
    public String dashboard() {
        return "dashboard";
    }
}
