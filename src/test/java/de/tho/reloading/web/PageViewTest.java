package de.tho.reloading.web;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class PageViewTest {

    private static final Map<String, Object> NO_FILTERS = Map.of();

    private static List<Integer> numbers(int count) {
        return IntStream.rangeClosed(1, count).boxed().toList();
    }

    @Test
    void shouldShowTwentyItemsByDefault() {
        PageView<Integer> page = PageView.of(numbers(45), null, null, "/list", NO_FILTERS);

        assertThat(page.getItems()).hasSize(20).startsWith(1).endsWith(20);
        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(20);
        assertThat(page.getTotalItems()).isEqualTo(45);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getFirstItem()).isEqualTo(1);
        assertThat(page.getLastItem()).isEqualTo(20);
        assertThat(page.isFirst()).isTrue();
        assertThat(page.isLast()).isFalse();
    }

    @Test
    void shouldShowTheRestOnTheLastPage() {
        PageView<Integer> page = PageView.of(numbers(45), 3, null, "/list", NO_FILTERS);

        assertThat(page.getItems()).containsExactly(41, 42, 43, 44, 45);
        assertThat(page.getFirstItem()).isEqualTo(41);
        assertThat(page.getLastItem()).isEqualTo(45);
        assertThat(page.isLast()).isTrue();
    }

    @Test
    void shouldCorrectPageAndSizeOutOfRange() {
        assertThat(PageView.of(numbers(45), 99, null, "/list", NO_FILTERS).getNumber()).isEqualTo(3);
        assertThat(PageView.of(numbers(45), 0, null, "/list", NO_FILTERS).getNumber()).isEqualTo(1);
        assertThat(PageView.of(numbers(45), -5, 0, "/list", NO_FILTERS).getSize()).isEqualTo(20);
        assertThat(PageView.of(numbers(500), 1, 1000, "/list", NO_FILTERS).getSize()).isEqualTo(100);
    }

    @Test
    void shouldHandleAnEmptyList() {
        PageView<Integer> page = PageView.of(List.of(), 2, null, "/list", NO_FILTERS);

        assertThat(page.isEmpty()).isTrue();
        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getTotalPages()).isEqualTo(1);
        assertThat(page.getFirstItem()).isZero();
        assertThat(page.getLastItem()).isZero();
        assertThat(page.isFirst()).isTrue();
        assertThat(page.isLast()).isTrue();
    }

    @Test
    void shouldHaveOnePageForExactlyOnePageOfItems() {
        PageView<Integer> page = PageView.of(numbers(20), 1, null, "/list", NO_FILTERS);

        assertThat(page.getTotalPages()).isEqualTo(1);
        assertThat(page.getItems()).hasSize(20);
    }

    @Test
    void shouldKeepSizeAndFiltersInLinks() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("manufacturer", "abc");
        filters.put("caliber", null);
        filters.put("q", "6.5 x 55");

        PageView<Integer> page = PageView.of(numbers(45), 1, 5, "/list", filters);

        assertThat(page.url(2)).isEqualTo("/list?page=2&size=5&manufacturer=abc&q=6.5+x+55");
        assertThat(page.url(99)).isEqualTo("/list?page=9&size=5&manufacturer=abc&q=6.5+x+55");
        assertThat(PageView.of(numbers(45), 1, null, "/list", NO_FILTERS).url(2)).isEqualTo("/list?page=2");
    }

    @Test
    void shouldLinkFirstLastAndNeighbouringPages() {
        assertThat(PageView.of(numbers(45), 1, null, "/list", NO_FILTERS).getPages()).containsExactly(1, 2, 3);
        // 0 stands for a gap
        assertThat(PageView.of(numbers(400), 10, null, "/list", NO_FILTERS).getPages())
                .containsExactly(1, 0, 8, 9, 10, 11, 12, 0, 20);
        assertThat(PageView.of(numbers(400), 1, null, "/list", NO_FILTERS).getPages()).containsExactly(1, 2, 3, 0, 20);
    }
}
