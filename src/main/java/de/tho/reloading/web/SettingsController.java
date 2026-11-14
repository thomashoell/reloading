package de.tho.reloading.web;

import de.tho.reloading.entity.AppUser;
import de.tho.reloading.user.CurrentUser;
import de.tho.reloading.user.UserService;
import de.tho.reloading.utility.LengthUnit;
import de.tho.reloading.utility.WeightUnit;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Settings of the current user: the units measures are entered and displayed in. */
@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final UserService userService;
    private final CurrentUser currentUser;

    public SettingsController(UserService userService, CurrentUser currentUser) {
        this.userService = userService;
        this.currentUser = currentUser;
    }

    @ModelAttribute
    public void units(Model model) {
        model.addAttribute("weightUnits", WeightUnit.values());
        model.addAttribute("lengthUnits", LengthUnit.values());
    }

    @GetMapping
    public String show(Model model) {
        AppUser user = currentUser.get();
        SettingsForm form = new SettingsForm();
        form.setWeightUnit(user.getWeightUnit());
        form.setLengthUnit(user.getLengthUnit());
        model.addAttribute("form", form);
        return "settings";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") SettingsForm form, BindingResult result,
                       RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return "settings";
        }
        // the user comes from the login, never from the request
        userService.updateUnits(currentUser.id(), form.getWeightUnit(), form.getLengthUnit());
        redirect.addFlashAttribute("success", "Die Einstellungen wurden gespeichert.");
        return "redirect:/settings";
    }
}
