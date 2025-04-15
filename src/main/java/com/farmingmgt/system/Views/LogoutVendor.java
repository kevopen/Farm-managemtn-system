package com.farmingmgt.system.Views;

import com.farmingmgt.system.UserSession;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@Route(value = "logoutvendor", layout = VendorLayout.class)
@PageTitle("Logging out...")
public class LogoutVendor extends VerticalLayout {
    private final UI uiInstance;

    public LogoutVendor() {
        this.uiInstance = UI.getCurrent();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        add(new H3("Logging out..."));

        uiInstance.access(() -> {
            UserSession.clearSession();
            VaadinSession.getCurrent().close();
            uiInstance.getPage().setLocation("/");
        });
    }
}

