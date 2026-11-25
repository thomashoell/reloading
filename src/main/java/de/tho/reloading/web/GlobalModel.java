package de.tho.reloading.web;

import de.tho.reloading.entity.AppUser;
import de.tho.reloading.user.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * Model attributes every page needs. Looking up the current user also creates the application user
 * on the first request after the first login.
 */
@ControllerAdvice(basePackageClasses = GlobalModel.class)
public class GlobalModel {

    private static final List<String> MASTER_DATA_PATHS =
            List.of("/manufacturers", "/calibers", "/casings", "/bullets", "/powders", "/primers");

    private final CurrentUser currentUser;

    public GlobalModel(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @ModelAttribute("user")
    public AppUser user() {
        return currentUser.get();
    }

    /** Path of the current page without the context path, for marking the current entry of the navigation. */
    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    /** Section of the navigation the current page belongs to: home, mydata, masterdata, settings or empty. */
    @ModelAttribute("navSection")
    public String navSection(HttpServletRequest request) {
        String path = currentPath(request);
        if (path.isEmpty() || path.equals("/")) {
            return "home";
        }
        if (path.startsWith("/settings")) {
            return "settings";
        }
        if (path.startsWith("/rifles") || path.contains("-lots") || path.startsWith("/processed-cases")
                || path.startsWith("/loads")) {
            return "mydata";
        }
        return MASTER_DATA_PATHS.stream().anyMatch(path::startsWith) ? "masterdata" : "";
    }
}
