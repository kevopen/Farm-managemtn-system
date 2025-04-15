package com.farmingmgt.system.Views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.HashMap;
import java.util.Map;

@Route(value = "vendors", layout = HomeLayout.class)
@PageTitle("Vendors")
public class Vendors extends VerticalLayout {
    private final UI uiInstance;

    public Vendors() {
        this.uiInstance = UI.getCurrent();
        

        // Create cards with loading indicators
    }
}


