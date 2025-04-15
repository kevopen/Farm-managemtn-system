package com.farmingmgt.system.Views;


import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Route("/marketplace-shop")
public class MarketplaceCustomer extends VerticalLayout {
    private final UI uiInstance;
    private final Grid<Marketplace> marketplaceGrid = new Grid<>(Marketplace.class, false);
    private final List<Marketplace> allItems = new ArrayList<>();
    private final TextField searchField = new TextField();

    public MarketplaceCustomer() {
        setPadding(true);
        setSpacing(true);
        this.uiInstance = UI.getCurrent();

        add(buildHeader());
        add(new H2("Welcome to the Marketplace"));

        configureGrid();
        add(searchField, marketplaceGrid);

        loadMarketplaceItems();
    }

    private HorizontalLayout buildHeader() {
        // Search field
        searchField.setPlaceholder("Search item...");
        searchField.setClearButtonVisible(true);
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.addValueChangeListener(e -> filterGridItems(e.getValue()));

        // Cart button
        Button cartButton = new Button("Cart", new Icon(VaadinIcon.CART));
        cartButton.addClickListener(e -> Notification.show("Cart clicked!"));

        // Logout button
        Button logoutButton = new Button("Logout", new Icon(VaadinIcon.SIGN_OUT));
        logoutButton.addClickListener(e -> {
            Notification.show("Logged out.");
            uiInstance.getPage().setLocation("/login"); // Adjust as per your route
        });

        // Header layout
        HorizontalLayout header = new HorizontalLayout(searchField, cartButton, logoutButton);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.expand(searchField); // Makes search field stretch

        return header;
    }

    private void configureGrid() {
        marketplaceGrid.setWidthFull();

        marketplaceGrid.addColumn(Marketplace::getName).setHeader("Item");
        marketplaceGrid.addColumn(Marketplace::getCategory).setHeader("Category");
        marketplaceGrid.addColumn(Marketplace::getItemType).setHeader("Type");
        marketplaceGrid.addColumn(Marketplace::getQuantity).setHeader("Available");
        marketplaceGrid.addColumn(item -> "KES " + item.getPrice()).setHeader("Price");
        marketplaceGrid.addColumn(item -> "KES " + item.getTotal()).setHeader("Total");

        marketplaceGrid.addComponentColumn(item -> {
            Button buyButton = new Button("Buy", new Icon(VaadinIcon.CART));
            buyButton.addClickListener(e -> showPurchaseDialog(item));
            return new HorizontalLayout(buyButton);
        }).setHeader("Actions");
    }

    private void loadMarketplaceItems() {
        MarketplaceService.fetchMarketplaceItems().thenAccept(items -> {
            uiInstance.access(() -> {
                allItems.clear();
                allItems.addAll(items);
                marketplaceGrid.setItems(allItems);
            });
        }).exceptionally(ex -> {
            ex.printStackTrace();
            uiInstance.access(() -> Notification.show("Failed to load items from marketplace."));
            return null;
        });
    }

    private void filterGridItems(String searchTerm) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            marketplaceGrid.setItems(allItems);
            return;
        }

        List<Marketplace> filtered = allItems.stream()
                .filter(item -> item.getName().toLowerCase().contains(searchTerm.toLowerCase()))
                .collect(Collectors.toList());

        marketplaceGrid.setItems(filtered);
    }

    private void showPurchaseDialog(Marketplace item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Buy Item");

        VerticalLayout layout = new VerticalLayout();
        layout.add(new Span("Item: " + item.getName()));
        layout.add(new Span("Category: " + item.getCategory()));
        layout.add(new Span("Price per unit: KES " + item.getPrice()));
        layout.add(new Span("Available quantity: " + item.getQuantity()));
        layout.setPadding(false);

        Button confirm = new Button("Confirm Purchase", event -> {
            Notification.show("Purchase confirmed for: " + item.getName());
            dialog.close();
        });

        Button cancel = new Button("Cancel", event -> dialog.close());
        cancel.getStyle().set("margin-right", "auto");

        dialog.getFooter().add(cancel, confirm);
        dialog.add(layout);
        dialog.open();
    }
}
