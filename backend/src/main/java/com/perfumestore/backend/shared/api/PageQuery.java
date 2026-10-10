package com.perfumestore.backend.shared.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class PageQuery {

    @Min(value = 0, message = "A página não pode ser negativa.")
    private int page = 0;

    @Min(value = 1, message = "O tamanho deve ser maior que zero.")
    @Max(value = 100, message = "O tamanho máximo é 100.")
    private int size = 20;

    private String sort = "id,asc";

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }
}
