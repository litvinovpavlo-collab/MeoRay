package dev.meoray.client.notify;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NotifyManager {
    private final List<Notify> notifies = new ArrayList<>();
    private boolean canAdd = true;
    private static NotifyManager INSTANCE;

    public static NotifyManager get() {
        if (INSTANCE == null) INSTANCE = new NotifyManager();
        return INSTANCE;
    }

    public void add(String text, String bind, Status status) {
        if (!canAdd) return;
        Notify n = new Notify(text, bind, status);
        notifies.add(n);
        if (notifies.size() > 6) {
            notifies.remove(0);
        }
    }

    public void add(String text) {
        add(text, "", Status.INFO);
    }

    public void addSuccess(String text, String bind) {
        add(text, bind, Status.SUCCESS);
    }

    public void addWarning(String text, String bind) {
        add(text, bind, Status.WARNING);
    }

    public void addError(String text, String bind) {
        add(text, bind, Status.ERROR);
    }

    public List<Notify> getNotifies() {
        return notifies;
    }

    public void update() {
        Iterator<Notify> it = notifies.iterator();
        while (it.hasNext()) {
            Notify n = it.next();
            n.getFadeIn();
            n.getFadeOut();
            if (n.isExpired()) {
                it.remove();
            }
        }
    }

    public void clear() {
        notifies.clear();
    }

    public void setCanAdd(boolean b) {
        canAdd = b;
    }
}
