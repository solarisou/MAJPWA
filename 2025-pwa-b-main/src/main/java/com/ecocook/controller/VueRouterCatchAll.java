package com.ecocook.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class VueRouterCatchAll {

    // Servir les assets (JS, CSS, images) tels quels
    @RequestMapping("/vue/assets/**")
    public String vueAssets(HttpServletRequest req) {
        return "forward:" + req.getRequestURI().replace("/vue/", "/app-vue/");
    }

    // Pour toutes les autres routes Vue, toujours donner index.html
    // Cela permet au router Vue de gerer les URLs
    @RequestMapping("/vue/**")
    public String vueApp() {
        return "forward:/app-vue/index.html";
    }

    // Redirection pour aller vers l'app Vue apres login
    @RequestMapping("/GO-VUE")
    public String goToVue() {
        return "redirect:/vue/";
    }
}
