package com.yooiistudios.serialanimator.animator;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
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
                    .build();
            if (getViewProperties().getViewPropertyByKey(i) == null) {
//                getViewProperties().putViewPropertyByKey(i, mockViewProperty);
                putViewPropertyIfRoom(mockViewProperty, i);
            }
        }
    }

    @Override
    public void putViewPropertyIfRoom(ViewProperty requestedViewProperty, int idx) {
        super.putViewPropertyIfRoom(requestedViewProperty, idx);

        transitItemOnFlyAt(idx);
    }

    private void transitItemOnFlyAt(int idx) {
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

        mValueAnimators.put(valueAnimator, property.getViewIndex());
    }

    @Override
    protected void onCancelTransitionAt(int index) {
        cancelValueAnimatorAt(index);
        resetViewStateAt(index);
    }

    private void cancelValueAnimatorAt(int index) {
        ValueAnimator animator = mValueAnimators.getByIndex(index);
        animator.cancel();

        String message;
        if (animator instanceof NullValueAnimator) {
            message = NullValueAnimator.class.getSimpleName();
        } else {
            message = ValueAnimator.class.getSimpleName();
        }
        Log.i("getByIndex(index)", "instance : " + message);
    }

    private void resetViewStateAt(int index) {
        ViewProperty viewProperty = getViewProperties().getViewPropertyByIndex(index);
        List<ValueAnimator> valueAnimators = getTransitionProperty().getTransitions(viewProperty.getView());
        ValueAnimator valueAnimator = valueAnimators.get(0);
        valueAnimator.setCurrentPlayTime(0);
    }

    @Override
    protected ValueTransitionListener makeTransitionListener(ViewProperty property) {
//        ValueTransitionListener listener = new ValueTransitionListener(property);
        // FIXME 아래 라인 copy & paste 임. super 로 빼야 할듯
//        listener.setIsLastTransition(isLastTransition(property));

        return new ValueTransitionListener(property);
    }

    protected static class ValueTransitionListener extends AnimatorListenerImpl
            implements SerialAnimator.TransitionListener {
        private ViewProperty mViewProperty;

        public ValueTransitionListener(ViewProperty viewProperty) {
            mViewProperty = viewProperty;
        }

        public ViewProperty getViewProperty() {
            return mViewProperty;
        }

        private void notifyOnAnimationEnd() {
            ViewProperty.AnimationListener callback =
                    getViewProperty().getAnimationListener();

            ViewTransientUtils.clearState(getViewProperty());

            if (callback != null) {
                callback.onAnimationEnd(getViewProperty());
            }
        }


        @Override
        public void onAnimationEnd(Animator animation) {
            notifyOnAnimationEnd();
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            ViewTransientUtils.clearState(getViewProperty());
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
        private SparseArray<ValueAnimator> mValueAnimators = new SparseArray<>();

        public void put(ValueAnimator animator, int index) {
            mValueAnimators.put(index, animator);
        }

        public ValueAnimator getByIndex(int index) {
            ValueAnimator animator;
            try {
//                int propertyIndex = mValueAnimators.keyAt(key);
                animator = mValueAnimators.get(index);
                if (animator == null) {
                    animator = new NullValueAnimator();
                }
            } catch (IndexOutOfBoundsException | NullPointerException e) {
                animator = new NullValueAnimator();
            }

            return animator;
        }
    }

    private static class NullValueAnimator extends ValueAnimator {
        @Override
        public void cancel() {
        }
    }
}
