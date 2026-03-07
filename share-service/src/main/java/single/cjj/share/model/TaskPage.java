package single.cjj.share.model;

import java.util.List;

public class TaskPage {
    private List<Task> data;
    private Pagination pagination;

    public TaskPage() {
    }

    public TaskPage(List<Task> data, Pagination pagination) {
        this.data = data;
        this.pagination = pagination;
    }

    public List<Task> getData() {
        return data;
    }

    public void setData(List<Task> data) {
        this.data = data;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }
}
