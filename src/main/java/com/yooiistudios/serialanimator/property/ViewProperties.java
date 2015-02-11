package com.yooiistudios.serialanimator.property;

import android.util.SparseArray;

/**
 * Created by Dongheyon Jeong in SequentialAnimationTest from Yooii Studios Co., LTD. on 15. 2. 10.
 *
 * ViewProperties
 *  ViewProperty 의 SparseArray 를 용도에 맞게 래핑한 클래스
 */
public class ViewProperties {
    private final SparseArray<ViewProperty> mViewProperties;

    public ViewProperties() {
        mViewProperties = new SparseArray<>();
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

    public void putViewPropertyByKey(int key, ViewProperty viewProperty) {
        mViewProperties.put(key, viewProperty);
    }

//    public void put
}
