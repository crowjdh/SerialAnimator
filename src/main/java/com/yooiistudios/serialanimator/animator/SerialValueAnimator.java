package com.yooiistudios.serialanimator.animator;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.support.annotation.NonNull;

import com.yooiistudios.serialanimator.ViewTransientUtils;
import com.yooiistudios.serialanimator.property.ViewProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Dongheyon Jeong in SequentialAnimationTest from Yooii Studios Co., LTD. on 15. 1. 29.
 *
 * SerialValueAnimator
 *  android.view.animation.Animation 객체를 사용하는 애니메이터
 */
public class SerialValueAnimator extends SerialAnimator<SerialValueAnimator.ValueAnimatorProperty,
        SerialValueAnimator.ValueTransitionListener> {
    private Map<ViewProperty, ValueAnimator> mValueAnimators;

    public SerialValueAnimator() {
        mValueAnimators = new HashMap<>();
    }

    @Override
    public void putViewPropertyIfRoom(ViewProperty requestedViewProperty, int idx) {
        super.putViewPropertyIfRoom(requestedViewProperty, idx);

        transitItemOnFlyAt(idx);
    }

    private void transitItemOnFlyAt(int idx) {
        ViewProperty viewProperty = getViewProperties().get(idx);
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
        if (isLastTransition(property)) {
            for (ValueAnimator animator : valueAnimators) {
                animator.addListener(transitionListener);
            }
        }
        ValueAnimator valueAnimator = valueAnimators.get(property.getTransitionInfo().index);
        valueAnimator.start();
        valueAnimator.setCurrentPlayTime(property.getTransitionInfo().currentPlayTime);

        mValueAnimators.put(property, valueAnimator);
    }

    @Override
    public void cancelAllHandlerMessages() {
        for (Map.Entry<ViewProperty, ValueAnimator> entry : mValueAnimators.entrySet()) {
            ViewProperty viewProperty = entry.getKey();
            ValueAnimator animator = entry.getValue();
            animator.cancel();

            List<ValueAnimator> valueAnimators = getTransitionProperty().getTransitions(viewProperty.getView());
            ValueAnimator valueAnimator = valueAnimators.get(0);
            valueAnimator.setCurrentPlayTime(0);
        }
    }

    @Override
    protected ValueTransitionListener makeTransitionListener(ViewProperty property) {
        ValueTransitionListener listener = new ValueTransitionListener(property);
        // FIXME 아래 라인 copy & paste 임. super 로 빼야 할듯
        listener.setIsLastTransition(isLastTransition(property));

        return listener;
    }

    protected static class ValueTransitionListener
            implements SerialAnimator.TransitionListener, ValueAnimator.AnimatorListener {
        private ViewProperty mViewProperty;
        private boolean mIsLastTransition;

        public ValueTransitionListener(ViewProperty viewProperty) {
            mViewProperty = viewProperty;
        }

        public ViewProperty getViewProperty() {
            return mViewProperty;
        }

        public boolean isLastTransition() {
            return mIsLastTransition;
        }

        public void setIsLastTransition(boolean isLastTransition) {
            mIsLastTransition = isLastTransition;
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
            if (isLastTransition()) {
                notifyOnAnimationEnd();
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            ViewTransientUtils.clearState(getViewProperty());
        }

        @Override public void onAnimationStart(Animator animation) { }
        @Override public void onAnimationRepeat(Animator animation) { }
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
                ValueAnimator transition = transitionList.get(i);
                delayBeforeTransition += getDuration(transition);
            }

            return timePast - baseDelay - delayBeforeTransition;
        }
    }
}
