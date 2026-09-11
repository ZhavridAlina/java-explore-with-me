package ru.practicum.util;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class OffsetPageRequest implements Pageable {

    private final int offset;
    private final int limit;
    private final Sort sort;

    private OffsetPageRequest(int offset, int limit, Sort sort) {
        this.offset = offset;
        this.limit = limit;
        this.sort = sort;
    }

    public static OffsetPageRequest of(int from, int size) {
        return of(from, size, Sort.unsorted());
    }

    public static OffsetPageRequest of(int from, int size, Sort sort) {
        return new OffsetPageRequest(from, size, sort);
    }

    @Override
    public int getPageNumber() {
        return offset / limit;
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetPageRequest(offset + limit, limit, sort);
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? new OffsetPageRequest(offset - limit, limit, sort) : first();
    }

    @Override
    public Pageable first() {
        return new OffsetPageRequest(0, limit, sort);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetPageRequest(pageNumber * limit, limit, sort);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }
}
