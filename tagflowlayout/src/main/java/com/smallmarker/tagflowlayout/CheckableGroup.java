package com.smallmarker.tagflowlayout;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author zl
 * @Date 2022/7/27
 **/
public class CheckableGroup<T extends MaterialCheckable<T>> {
    private final Map<Integer, T> checkables = new HashMap<>();
    private final Set<Integer> checkedIds = new HashSet<>();

    private CheckableGroup.OnCheckedStateChangeListener onCheckedStateChangeListener;
    private boolean singleSelection;
    private boolean selectionRequired;

    public void setSingleSelection(boolean singleSelection) {
        if (this.singleSelection != singleSelection) {
            this.singleSelection = singleSelection;
            clearCheck();
        }
    }

    public boolean isSingleSelection() {
        return singleSelection;
    }

    public void setSelectionRequired(boolean selectionRequired) {
        this.selectionRequired = selectionRequired;
    }

    public boolean isSelectionRequired() {
        return selectionRequired;
    }

    public void setOnCheckedStateChangeListener(@Nullable CheckableGroup.OnCheckedStateChangeListener listener) {
        this.onCheckedStateChangeListener = listener;
    }

    public void addCheckable(T checkable) {
        checkables.put(checkable.getId(), checkable);
        if (checkable.isChecked()) {
            checkInternal(checkable);
        }
        checkable.setInternalOnCheckedChangeListener(new MaterialCheckable.OnCheckedChangeListener<T>() {
            @Override
            public void onCheckedChanged(T checkable, boolean isChecked) {
                if (isChecked ? checkInternal(checkable) : uncheckInternal(checkable, selectionRequired)) {
                    onCheckedStateChanged();
                }
            }
        });
    }

    public void removeCheckable(T checkable) {
        checkable.setInternalOnCheckedChangeListener(null);
        checkables.remove(checkable.getId());
        checkedIds.remove(checkable.getId());
    }

    public void check(@IdRes int id) {
        MaterialCheckable<T> checkable = checkables.get(id);
        if (checkable == null) {
            return;
        }
        if (checkInternal(checkable)) {
            onCheckedStateChanged();
        }
    }

    public void uncheck(@IdRes int id) {
        MaterialCheckable<T> checkable = checkables.get(id);
        if (checkable == null) {
            return;
        }
        if (uncheckInternal(checkable, selectionRequired)) {
            onCheckedStateChanged();
        }
    }

    public void clearCheck() {
        boolean checkedStateChanged = !checkedIds.isEmpty();
        for (MaterialCheckable<T> checkable : checkables.values()) {
            uncheckInternal(checkable, false);
        }
        if (checkedStateChanged) {
            onCheckedStateChanged();
        }
    }

    @IdRes
    public int getSingleCheckedId() {
        return singleSelection && !checkedIds.isEmpty() ? checkedIds.iterator().next() : View.NO_ID;
    }

    @NonNull
    public Set<Integer> getCheckedIds() {
        return new HashSet<>(checkedIds);
    }

    @NonNull
    public List<Integer> getCheckedIdsSortedByChildOrder(@NonNull ViewGroup parent) {
        Set<Integer> checkedIds = getCheckedIds();
        List<Integer> sortedCheckedIds = new ArrayList<>();
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof MaterialCheckable<?> && checkedIds.contains(child.getId())) {
                sortedCheckedIds.add(child.getId());
            }
        }
        return sortedCheckedIds;
    }

    private boolean checkInternal(@NonNull MaterialCheckable<T> checkable) {
        int id = checkable.getId();
        if (checkedIds.contains(id)) {
            return false;
        }
        MaterialCheckable<T> singleCheckedItem = checkables.get(getSingleCheckedId());
        if (singleCheckedItem != null) {
            uncheckInternal(singleCheckedItem, false);
        }
        boolean checkedStateChanged = checkedIds.add(id);
        if (!checkable.isChecked()) {
            checkable.setChecked(true);
        }
        return checkedStateChanged;
    }

    private boolean uncheckInternal(
            @NonNull MaterialCheckable<T> checkable, boolean selectionRequired) {
        int id = checkable.getId();
        if (!checkedIds.contains(id)) {
            return false;
        }
        if (selectionRequired && checkedIds.size() == 1 && checkedIds.contains(id)) {
            // It's the only checked item, cannot be unchecked if selection is required
            checkable.setChecked(true);
            return false;
        }
        boolean checkedStateChanged = checkedIds.remove(id);
        if (checkable.isChecked()) {
            checkable.setChecked(false);
        }
        return checkedStateChanged;
    }

    private void onCheckedStateChanged() {
        if (onCheckedStateChangeListener != null) {
            onCheckedStateChangeListener.onCheckedStateChanged(getCheckedIds());
        }
    }

    /**
     * A listener interface for checked state changes.
     */
    public interface OnCheckedStateChangeListener {
        void onCheckedStateChanged(@NonNull Set<Integer> checkedIds);
    }
}
