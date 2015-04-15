package com.yooiistudios.serialanimator.animator;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Pair;
import android.util.SparseArray;
import android.view.View;

import com.yooiistudios.serialanimator.AnimatorListenerImpl;
import com.yooiistudios.serialanimator.ViewTransientUtils;
import com.yooiistudios.serialanimator.property.ViewProperty;

import java.util.List;

/**
 * Created by Dongheyon Jeong in SequentialAnimationTest from Yooii Studios Co., LTD. on 15. 1. 29.
 *
 * SerialValueAnimator
 *  android.view.animation.Animation 객체를 사용하는 애니메이터
 */
public class SerialValueAnimator extends SerialAnimator<SerialValueAnimator.ValueAnimatorProperty,
        SerialValueAnimator.ValueTransitionListener> {
    private ValueAnimators mValueAnimators;

    public SerialValueAnimator() {
        mValueAnimators = new ValueAnimators();
    }

//    public SerialValueAnimator(Context context, ViewProperty.AnimationListener listener, int count) {
//        this();
//        initMockViewProperties(context, listener, count);
//    }

    public void applyMockViewProperties(Context context, ViewProperty.AnimationListener listener, int count) {
        View mockView = new View(context);
        for (int i = 0; i < count; i++) {
            ViewProperty mockViewProperty = new ViewProperty.Builder()
                    .setView(mockView)
                    .setAnimationListener(listener)
                    .setViewIndex(i)
                    .buildAsMock();
            if (getViewProperties().getViewPropertyByKey(i) == null) {
//                getViewProperties().putViewPropertyByKey(i, mockViewProperty);
                putViewPropertyIfRoom(mockViewProperty, i);
            }
        }
    }

    @Override
    protected void transitItemOnFlyAt(int idx) {
        if (getStartTimeInMilli() < 0) {
            return;
        }

        ViewProperty viewProperty = getViewProperties().getViewPropertyByKey(idx);
        long timePast = System.currentTimeMillis() - getStartTimeInMilli();
        ValueAnimatorProperty transitionProperty = getTransitionProperty();

        if (transitionProperty.inTimeToTransit(viewProperty, timePast)) {
            transitInTime(viewProperty, timePast);
        } else if (transitionProperty.shouldTransitInFuture(viewProperty, timePast)){
            transitInFuture(viewProperty, timePast);
        }
    }

    private void transitInTime(ViewProperty viewProperty, long timePast) {
        viewProperty.getTransitionInfo().index =
                getTransitionProperty().getTransitionIndexForProperty(viewProperty, timePast);
        viewProperty.getTransitionInfo().currentPlayTime =
                getTransitionProperty().getCurrentPlayTime(viewProperty, timePast);
        viewProperty.getTransitionInfo().ignorePreviousCallback = true;

        transitAndRequestNext(viewProperty);
    }

    private void transitInFuture(ViewProperty viewProperty, long timePast) {
        viewProperty.getTransitionInfo().index =
                getTransitionProperty().getTransitionIndexForProperty(viewProperty, timePast);

        requestTransitionWithDelayConsume(viewProperty, timePast);
    }

    @Override
    protected void onTransit(ViewProperty property, ValueTransitionListener transitionListener) {
        List<ValueAnimator> valueAnimators = getTransitionProperty().getTransitions(property.getView());
//        if (isLastTransition(property)) {
//            for (ValueAnimator animator : valueAnimators) {
//                animator.addListener(transitionListener);
//            }
//        }
        ValueAnimator valueAnimator = valueAnimators.get(property.getTransitionInfo().index);
        valueAnimator.addListener(transitionListener);
        valueAnimator.start();
        valueAnimator.setCurrentPlayTime(property.getTransitionInfo().currentPlayTime);

        mValueAnimators.put(valueAnimator, transitionListener, property.getViewIndex());
    }

    @Override
    protected void onCancelTransitionByViewProperty(ViewProperty viewProperty) {
        cancelValueAnimatorAt(viewProperty);
        resetViewStateAt(viewProperty.getViewIndex());
    }

    private void cancelValueAnimatorAt(ViewProperty viewProperty) {
        ValueTransitionListener listener = mValueAnimators.getListenerByIndex(viewProperty.getViewIndex());
        listener.setIgnoreCallback(viewProperty.getTransitionInfo().ignorePreviousCallback);
        viewProperty.getTransitionInfo().ignorePreviousCallback = false;

        ValueAnimator animator = mValueAnimators.getAnimatorByIndex(viewProperty.getViewIndex());
        animator.cancel();
    }

    private void resetViewStateAt(int index) {
        ViewProperty viewProperty = getViewProperties().getViewPropertyByIndex(index);
        List<ValueAnimator> valueAnimators = getTransitionProperty().getTransitions(viewProperty.getView());
        ValueAnimator valueAnimator = valueAnimators.get(0);
        valueAnimator.setCurrentPlayTime(0);
    }

    @Override
    protected ValueTransitionListener makeTransitionListener(ViewProperty property) {
        ValueTransitionListener listener = new ValueTransitionListener(property);
        // FIXME 아래 라인 copy & paste 임. super 로 빼야 할듯
        listener.setLastTransition(isLastTransition(property));

        return listener;
    }

    protected static class ValueTransitionListener extends AnimatorListenerImpl
            implements SerialAnimator.TransitionListener {
        private ViewProperty mViewProperty;
        private boolean mIsLastTransition;
        private boolean mIgnoreCallback;

        public ValueTransitionListener(ViewProperty viewProperty) {
            mViewProperty = viewProperty;
        }

        public ViewProperty getViewProperty() {
            return mViewProperty;
        }

        private void notifyOnAnimationEnd() {
            if (mIsLastTransition) {
                ViewTransientUtils.clearState(getViewProperty());
            }

            if (!mIgnoreCallback) {
                ViewProperty.AnimationListener callback =
                        getViewProperty().getAnimationListener();

                if (callback != null) {
                    callback.onAnimationEnd(getViewProperty());
                }
            }
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            notifyOnAnimationEnd();
        }

        public void setLastTransition(boolean isLastTransition) {
            mIsLastTransition = isLastTransition;
        }

        public void setIgnoreCallback(boolean ignoreCallback) {
            mIgnoreCallback = ignoreCallback;
        }
    }

    public static class ValueAnimatorProperty extends SerialAnimator.TransitionProperty<ValueAnimator> {
        public ValueAnimatorProperty(@NonNull TransitionSupplier<ValueAnimator> transitionSupplier,
                                     long initialDelayInMillisec, long intervalInMillisec) {
            super(transitionSupplier, initialDelayInMillisec, intervalInMillisec);
        }

        @Override
        protected long getDuration(ValueAnimator transition) {
            return transition.getDuration();
        }

        public long getCurrentPlayTime(ViewProperty property, long timePast) {
            long baseDelay = getDelayForInitialTransition(property);
            long delayBeforeTransition = 0;

            List<ValueAnimator> transitionList = getTransitions(null);
            for (int i = 0; i < property.getTransitionInfo().index; i++) {
                android.animation.ValueAnimator transition = transitionList.get(i);
                delayBeforeTransition += getDuration(transition);
            }

            return timePast - baseDelay - delayBeforeTransition;
        }
    }

    private static class ValueAnimators {
        private SparseArray<Pair<ValueAnimator, ValueTransitionListener>> mValueAnimators = new SparseArray<>();

        public void put(ValueAnimator animator, ValueTransitionListener animatorListener, int index) {
            mValueAnimators.put(index, new Pair<>(animator, animatorListener));
        }

        public ValueAnimator getAnimatorByIndex(int index) {
            ValueAnimator animator;
            try {
                animator = mValueAnimators.get(index).first;
                if (animator == null) {
                    animator = new NullValueAnimator();
                }
            } catch (IndexOutOfBoundsException | NullPointerException e) {
                animator = new NullValueAnimator();
            }

            return animator;
        }

        public ValueTransitionListener getListenerByIndex(int index) {
            ValueTransitionListener animatorListener;
            try {
                animatorListener = mValueAnimators.get(index).second;
                if (animatorListener == null) {
                    animatorListener = new NullValueTransitionListener();
                }
            } catch (IndexOutOfBoundsException | NullPointerException e) {
                animatorListener = new NullValueTransitionListener();
            }

            return animatorListener;
        }

        private static class NullValueAnimator extends ValueAnimator {
            @Override
            public void cancel() {
            }
        }

        private static class NullValueTransitionListener extends ValueTransitionListener {
            public NullValueTransitionListener() {
                super(null);
            }
        }
    }
}
