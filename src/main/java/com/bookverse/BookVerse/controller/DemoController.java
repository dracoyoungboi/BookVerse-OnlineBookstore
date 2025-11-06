package com.bookverse.BookVerse.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("demo")
public class DemoController {
    @GetMapping("/user")
    public String user() {
        return "user/index-7";
    }
    @GetMapping("/admin")
    public String admin() {
        return "admin/invoice-list";
    }
}


