package single.cjj.share.model;

import java.util.ArrayList;
import java.util.List;

public class CommentThread {
    private String id;
    private int total;
    private List<Comment> items = new ArrayList<>();

    public CommentThread() {
    }

    public CommentThread(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<Comment> getItems() {
        return items;
    }

    public void setItems(List<Comment> items) {
        this.items = items;
    }

    public void addComment(Comment comment) {
        this.items.add(comment);
        this.total = this.items.size();
    }
}
