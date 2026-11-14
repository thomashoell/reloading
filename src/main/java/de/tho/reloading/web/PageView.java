package de.tho.reloading.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One page of an already filtered and sorted list, with what a template needs for the pagination.
 * Master data is small and immutable, so the lists are paged in memory instead of in the database.
 * Page numbers start at 1.
 */
public final class PageView<T> {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;
    // page links shown around the current page
    private static final int WINDOW = 2;

    private final List<T> items;
    private final int number;
    private final int size;
    private final int totalItems;
    private final int totalPages;
    private final String path;
    private final String filterQuery;

    private PageView(List<T> items, int number, int size, int totalItems, int totalPages, String path,
                     String filterQuery) {
        this.items = items;
        this.number = number;
        this.size = size;
        this.totalItems = totalItems;
        this.totalPages = totalPages;
        this.path = path;
        this.filterQuery = filterQuery;
    }

    /**
     * @param all     the filtered and sorted list
     * @param page    requested page, starting at 1; null or out of range is corrected
     * @param size    requested page size; null means {@link #DEFAULT_SIZE}, at most {@link #MAX_SIZE}
     * @param path    path of the list, for the links to other pages
     * @param filters active filters, kept in the links to other pages; null and empty values are left out
     */
    public static <T> PageView<T> of(List<T> all, Integer page, Integer size, String path, Map<String, ?> filters) {
        int pageSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        int totalItems = all.size();
        int totalPages = Math.max(1, (totalItems + pageSize - 1) / pageSize);
        int number = page == null ? 1 : Math.max(1, Math.min(page, totalPages));
        int from = Math.min((number - 1) * pageSize, totalItems);
        int to = Math.min(from + pageSize, totalItems);

        StringBuilder filterQuery = new StringBuilder();
        filters.forEach((name, value) -> {
            if (value != null && !value.toString().isBlank()) {
                filterQuery.append('&').append(encode(name)).append('=').append(encode(value.toString()));
            }
        });
        return new PageView<>(List.copyOf(all.subList(from, to)), number, pageSize, totalItems, totalPages, path,
                filterQuery.toString());
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** Link to another page with the same page size and filters. */
    public String url(int page) {
        int target = Math.max(1, Math.min(page, totalPages));
        StringBuilder url = new StringBuilder(path).append("?page=").append(target);
        if (size != DEFAULT_SIZE) {
            url.append("&size=").append(size);
        }
        return url.append(filterQuery).toString();
    }

    /** Page numbers to link: the first, the last and the ones around the current page; 0 stands for a gap. */
    public List<Integer> getPages() {
        List<Integer> pages = new ArrayList<>();
        int previous = 0;
        for (int page = 1; page <= totalPages; page++) {
            if (page == 1 || page == totalPages || Math.abs(page - number) <= WINDOW) {
                if (page - previous > 1) {
                    pages.add(0);
                }
                pages.add(page);
                previous = page;
            }
        }
        return pages;
    }

    public List<T> getItems() {
        return items;
    }

    public int getNumber() {
        return number;
    }

    public int getSize() {
        return size;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }

    /** Position of the first item of this page in the whole list, starting at 1; 0 if the list is empty. */
    public int getFirstItem() {
        return items.isEmpty() ? 0 : (number - 1) * size + 1;
    }

    public int getLastItem() {
        return (number - 1) * size + items.size();
    }

    public boolean isFirst() {
        return number == 1;
    }

    public boolean isLast() {
        return number == totalPages;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
