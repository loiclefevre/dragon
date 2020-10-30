package com.oracle.dragon.model;

public class ORDSSQLServiceResponse {
    private ORDSSQLServiceResponseItems[] items;

    public ORDSSQLServiceResponse() {
    }

    public ORDSSQLServiceResponseItems[] getItems() {
        return items;
    }

    public void setItems(ORDSSQLServiceResponseItems[] items) {
        this.items = items;
    }
}
