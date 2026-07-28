package br.com.systemcommerce.shared.pagination;

import br.com.systemcommerce.shared.web.CorrelationIdContext;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;

public class PageResponse<T> {

    private final List<T> data;
    private final PageInfo page;
    private final Instant timestamp;
    private final String correlationId;

    private PageResponse(List<T> data, PageInfo page, Instant timestamp, String correlationId) {
        this.data = data;
        this.page = page;
        this.timestamp = timestamp;
        this.correlationId = correlationId;
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                new PageInfo(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages()),
                Instant.now(),
                CorrelationIdContext.current());
    }

    public List<T> getData() {
        return data;
    }

    public PageInfo getPage() {
        return page;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public static class PageInfo {
        private final int number;
        private final int size;
        private final long totalElements;
        private final int totalPages;

        public PageInfo(int number, int size, long totalElements, int totalPages) {
            this.number = number;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
        }

        public int getNumber() {
            return number;
        }

        public int getSize() {
            return size;
        }

        public long getTotalElements() {
            return totalElements;
        }

        public int getTotalPages() {
            return totalPages;
        }
    }
}
