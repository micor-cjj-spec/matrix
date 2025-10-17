package single.cjj.share.model;

public class Pagination {
    private int page;
    private int pageSize;
    private long total;

    public Pagination() {
    }

    public Pagination(int page, int pageSize, long total) {
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
