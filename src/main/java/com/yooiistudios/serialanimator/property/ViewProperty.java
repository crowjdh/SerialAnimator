package com.yooiistudios.serialanimator.property;

import android.view.View;

/**
 * Created by Dongheyon Jeong in News-Android-L from Yooii Studios Co., LTD. on 15. 1. 27.
 *
 * ViewProperty
 *  애니메이션될 뷰의 속성
 */
public class ViewProperty implements Cloneable, AbstractViewProperty {
    public interface AnimationListener {
        public void onAnimationEnd(ViewProperty property);
    }
    private View mView;
    private AnimationListener mAnimationListener;
    private int mViewIndex;
    private TransitionInfo mTransitionInfo;

    public ViewProperty() {
        mTransitionInfo = TransitionInfo.makeDefault();
    }

    public void changeToNextTransitionState() {
        getTransitionInfo().index++;
        getTransitionInfo().currentPlayTime = TransitionInfo.DEFAULT_CURRENT_PLAY_TIME;
    }

    public void resetTransitionInfo() {
        TransitionInfo resetTransitionInfo = TransitionInfoManipulator.resetTransitionInfo(getTransitionInfo());
        setTransitionInfo(resetTransitionInfo);
    }

    public AbstractViewProperty makeShallowCloneWithDeepTransitionInfoCloneWhenPossible() {
        AbstractViewProperty abstractViewProperty;
        try {
            ViewProperty viewProperty = (ViewProperty)clone();
            TransitionInfo clonedTransitionInfo =
                    TransitionInfoManipulator.makeShallowCloneWhenPossible(viewProperty.getTransitionInfo());
            viewProperty.setTransitionInfo(clonedTransitionInfo);

            abstractViewProperty = viewProperty;
        } catch(CloneNotSupportedException e) {
            e.printStackTrace();
            abstractViewProperty = new NullViewProperty();
        }

        return abstractViewProperty;
    }

    public View getView() {
        return mView;
    }

    public void setView(View view) {
        mView = view;
    }

    // TODO rename to transition
    public AnimationListener getAnimationListener() {
        return mAnimationListener;
    }

    // TODO rename to transition
    public void setAnimationListener(AnimationListener animationListener) {
        mAnimationListener = animationListener;
    }

    public TransitionInfo getTransitionInfo() {
        return mTransitionInfo;
    }

    public void setTransitionInfo(TransitionInfo transitionInfo) {
        mTransitionInfo = transitionInfo;
    }

    public int getViewIndex() {
        return mViewIndex;
    }

    public void setViewIndex(int viewIndex) {
        mViewIndex = viewIndex;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("View index : ").append(mViewIndex)
                .append("\n")
                .append(mTransitionInfo.toString());
        return builder.toString();
    }

    public static class Builder {
        private View mView;
        private AnimationListener mAnimationListener;
        private int mViewIndex;
        private TransitionInfo mTransitionInfo = TransitionInfo.makeDefault();

        public Builder setView(View view) {
            mView = view;
            return this;
        }

        public Builder setAnimationListener(AnimationListener animationListener) {
            mAnimationListener = animationListener;
            return this;
        }

        public Builder setViewIndex(int viewIndex) {
            mViewIndex = viewIndex;
            return this;
        }

        public ViewProperty build() {
            ViewProperty property = new ViewProperty();
            return applyProperties(property);
        }

        public MockViewProperty buildAsMock() {
            MockViewProperty property = new MockViewProperty();
            return (MockViewProperty)applyProperties(property);
        }

        private ViewProperty applyProperties(ViewProperty property) {
            property.setView(mView);
            property.setViewIndex(mViewIndex);
            property.setTransitionInfo(mTransitionInfo);
            property.setAnimationListener(mAnimationListener);
            return property;
        }
    }

    public static class TransitionInfo implements Cloneable {
        public static final int DEFAULT_INDEX = 0;
        public static final long DEFAULT_CURRENT_PLAY_TIME = 0L;
        public int index;
        public long currentPlayTime;
        public boolean ignorePreviousCallback;
//        public boolean useNextTransition;

        public TransitionInfo(int index, long currentPlayTime) {
            this.index = index;
            this.currentPlayTime = currentPlayTime;
        }

        public static TransitionInfo makeDefault() {
            return new TransitionInfo(DEFAULT_INDEX, DEFAULT_CURRENT_PLAY_TIME);
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Transition index: ").append(index)
                    .append("\nCurrent play time: ").append(currentPlayTime);

            return builder.toString();
        }
    }

    private static final class TransitionInfoManipulator {
        private TransitionInfoManipulator() throws IllegalAccessException {
            throw new IllegalAccessException("You MUST NOT instantiate this class.");
        }

        private static TransitionInfo resetTransitionInfo(TransitionInfo transitionInfo) {
            transitionInfo.index = TransitionInfo.DEFAULT_INDEX;
            transitionInfo.currentPlayTime = TransitionInfo.DEFAULT_CURRENT_PLAY_TIME;

            return transitionInfo;
        }

        public static TransitionInfo makeShallowCloneWhenPossible(TransitionInfo info)
                throws CloneNotSupportedException {
            return (TransitionInfo)info.clone();
        }
    }
}
