package com.yooiistudios.serialanimator.property;

import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.view.View;

import com.google.common.collect.HashBiMap;

/**
 * Created by Dongheyon Jeong in SequentialAnimationTest from Yooii Studios Co., LTD. on 15. 2. 10.
 *
 * ViewProperties
 *  ViewProperty 의 SparseArray 를 용도에 맞게 래핑한 클래스
 */
public class ViewProperties {
    private final SparseArray<ViewProperty> mViewProperties;
//    private final HashSet<View> mViewHashSet;
    private final HashBiMap<View, Integer> mViewMap;

    public ViewProperties() {
        mViewProperties = new SparseArray<>();
//        mViewHashSet = new HashSet<>();
        mViewMap = HashBiMap.create();
    }

    public int size() {
        return mViewProperties.size();
    }

    public ViewProperty getViewPropertyByIndex(int index) {
        int key = mViewProperties.keyAt(index);
        return getViewPropertyByKey(key);
    }

    public ViewProperty getViewPropertyByKey(int key) {
        return mViewProperties.get(key);
    }

    @Nullable
    public ViewProperty getViewPropertyByView(View view) {
        Integer key = mViewMap.get(view);
        return key != null ? getViewPropertyByKey(key) : null;
    }

    public void putViewPropertyByKey(int key, ViewProperty viewProperty) {
        View view = viewProperty.getView();
        if (isContainingView(view)) {
            Integer previousKey = getKeyOfView(view);
            removeViewPropertyByKey(previousKey);
        }
        mViewProperties.put(key, viewProperty);
        mViewMap.forcePut(view, key);
    }

    public boolean isContainingKey(int key) {
        return getViewPropertyByKey(key) != null;
    }

    public boolean isContainingView(View view) {
        return getKeyOfView(view) != null;
    }

    private Integer getKeyOfView(View view) {
        return mViewMap.get(view);
    }

    public void removeViewPropertyByKey(int key) {
        ViewProperty viewProperty = getViewPropertyByKey(key);
        mViewProperties.remove(key);
        mViewMap.remove(viewProperty.getView());
    }

    public void removeViewPropertyByView(View view) {
        if (isContainingView(view)) {
            Integer previousKey = getKeyOfView(view);
            removeViewPropertyByKey(previousKey);
        }
    }
}
