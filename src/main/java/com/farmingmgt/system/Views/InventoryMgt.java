package com.farmingmgt.system.Views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.HashMap;
import java.util.Map;

@Route(value = "inventory", layout = ManagerLayout.class)
@PageTitle("Inventory")
public class InventoryMgt extends VerticalLayout {
    private final UI uiInstance;


    public InventoryMgt() {
        this.uiInstance = UI.getCurrent();
        

        // Create cards with loading indicators
    }
}


