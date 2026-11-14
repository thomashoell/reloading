package de.tho.reloading.web;

import de.tho.reloading.entity.AppUser;
import de.tho.reloading.user.CurrentUser;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Model attributes every page needs. Looking up the current user also creates the application user
 * on the first request after the first login.
 */
@ControllerAdvice(basePackageClasses = GlobalModel.class)
public class GlobalModel {

    private final CurrentUser currentUser;

    public GlobalModel(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @ModelAttribute("user")
    public AppUser user() {
        return currentUser.get();
    }
}
