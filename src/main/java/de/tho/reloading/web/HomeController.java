package de.tho.reloading.web;

import de.tho.reloading.user.CurrentUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final CurrentUser currentUser;

    public HomeController(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @GetMapping("/")
    String home(Model model) {
        // creates the application user on the first request after the first login
        model.addAttribute("user", currentUser.get());
        return "index";
    }
}
