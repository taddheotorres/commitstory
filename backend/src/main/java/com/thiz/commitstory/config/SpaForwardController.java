package com.thiz.commitstory.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaForwardController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, HttpServletResponse response) {
        Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        if (statusCode != null && statusCode == HttpStatus.NOT_FOUND.value()) {
            String requestUri = (String) request.getAttribute("jakarta.servlet.error.request_uri");
            if (requestUri != null && requestUri.startsWith("/api/")) {
                return null;
            }
            response.setStatus(HttpStatus.OK.value());
            return "forward:/index.html";
        }
        return null;
    }
}
