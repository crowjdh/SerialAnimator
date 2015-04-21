package com.yooiistudios.serialanimator.animator;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.view.View;

import com.yooiistudios.serialanimator.ViewTransientUtils;
import com.yooiistudios.serialanimator.property.AbstractViewProperty;
import com.yooiistudios.serialanimator.property.ViewProperties;
import com.yooiistudios.serialanimator.property.ViewProperty;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by Dongheyon Jeong in News-Android-L from Yooii Studios Co., LTD. on 15. 1. 27.
 *
 * SerialAnimator
 *  순차적으로 뷰들을 animating 하는 클래스
 */
public abstract class SerialAnimator<T extends SerialAnimator.TransitionProperty,
        S extends SerialAnimator.TransitionListener> {
    protected interface TransitionListener { }

    private static final int INVALID_START_TIME = -1;

    private final ViewProperties mViewProperties;
    private T mTransitionProperty;
    private TransitionHandler mTransitionHandler;
    private long mStartTimeInMilli;

    protected SerialAnimator() {
        mViewProperties = new ViewProperties();
        mTransitionHandler = new TransitionHandler(this);
    }

    public void animate() {
        if (isReadyForTransition()) {
            cancelAllTransitions();
            prepareForNewTransitionSequence();
            runSequentialTransition();
        }
    }

    public void cancelAllTransitions() {
        cancelAllTransitionsInternal(false);
    }

    public void cancelAndResetAllTransitions() {
        cancelAllTransitionsInternal(true);
    }

    private void cancelAllTransitionsInternal(boolean ignorePreviousCallback) {
        resetStartTime();

        mTransitionHandler.removeCallbacksAndMessages(null);
        int viewCount = mViewProperties.size();
        for (int i = 0; i < viewCount; i++) {
            // SparseArray 의 keyAt 메서드 특성상 아래와 같이 쿼리하면 key 의 ascending order 로 결과값이 나온다.

            // TODO: cancelAllTransitions 에서 이미 mTransitionHandler.removeCallbacksAndMessages 를
            // 불러주고 있기 때문에 cancelHandlerMessageAt 필요 없을지도
            cancelTransitionAtInternal(i, ignorePreviousCallback);
        }
    }

    private void cancelTransitionAt(int index) {
        cancelTransitionAtInternal(index, false);
    }

    private void cancelAndResetTransitionAt(int index) {
        cancelTransitionAtInternal(index, true);
    }

    private void cancelTransitionAtInternal(int index, boolean ignorePreviousCallback) {
        ViewProperty viewProperty = getViewProperties().getViewPropertyByIndex(index);
        cancelTransitionInternal(viewProperty, ignorePreviousCallback);
    }

    private void cancelTransitionByKeyInternal(int key, boolean ignorePreviousCallback) {
        ViewProperty viewProperty = getViewProperties().getViewPropertyByKey(key);
        cancelTransitionInternal(viewProperty, ignorePreviousCallback);
    }

    private void cancelTransitionInternal(ViewProperty viewProperty, boolean ignorePreviousCallback) {
//        ViewProperty property = getViewProperties().getViewPropertyByIndex(index);
        cancelHandlerMessageAt(viewProperty.getViewIndex());
        viewProperty.getTransitionInfo().ignorePreviousCallback = ignorePreviousCallback;
        onCancelTransitionByViewProperty(viewProperty);
    }

    private void resetStartTime() {
        mStartTimeInMilli = INVALID_START_TIME;
    }

    public void cancelHandlerMessageAt(int index) {
        mTransitionHandler.removeMessages(index);
    }

    protected abstract void onCancelTransitionByViewProperty(ViewProperty viewProperty);

    private void prepareForNewTransitionSequence() {
        prepareStartTime();

        int viewCount = mViewProperties.size();
        for (int i = 0; i < viewCount; i++) {
            // SparseArray 의 keyAt 메서드 특성상 아래와 같이 쿼리하면 key 의 ascending order 로 결과값이 나온다.
            ViewProperty viewProperty = mViewProperties.getViewPropertyByIndex(i);
//            int propertyIndex = mViewProperties.keyAt(i);
//            ViewProperty viewProperty = mViewProperties.get(propertyIndex);
            viewProperty.resetTransitionInfo();
        }
    }

    private void prepareStartTime() {
        mStartTimeInMilli = System.currentTimeMillis();
    }

    private void runSequentialTransition() {
        int viewCount = mViewProperties.size();
        for (int i = 0; i < viewCount; i++) {
            // SparseArray 의 keyAt 메서드 특성상 아래와 같이 쿼리하면 key 의 ascending order 로 결과값이 나온다.
            ViewProperty viewProperty = mViewProperties.getViewPropertyByIndex(i);
//            int propertyIndex = mViewProperties.keyAt(i);
//            ViewProperty property = mViewProperties.get(propertyIndex);
            requestTransition(viewProperty);
        }
    }

    protected void requestTransitionWithDelayConsume(ViewProperty viewProperty, long consume) {
        ViewTransientUtils.setState(viewProperty);
        long delay = getTransitionProperty().getDelay(viewProperty) - consume;
        Message message = Message.obtain();
        message.obj = viewProperty;
        // 이미 등록된 메시지를 취소하는 데에 쓰일 값
        int messageId = viewProperty.getViewIndex();
        message.what = messageId;

        cancelHandlerMessageAt(messageId);
        mTransitionHandler.sendMessageDelayed(message, delay);
    }

    protected void requestTransition(ViewProperty viewProperty) {
        requestTransitionWithDelayConsume(viewProperty, 0);
    }

    protected void requestNextTransition(ViewProperty previousViewProperty) {
        if (!isLastTransition(previousViewProperty)) {
            AbstractViewProperty clonedViewProperty =
                    previousViewProperty.makeShallowCloneWithDeepTransitionInfoCloneWhenPossible();
            if (clonedViewProperty instanceof ViewProperty) {
                ViewProperty legitViewProperty = (ViewProperty)clonedViewProperty;
                legitViewProperty.changeToNextTransitionState();

                long consume = previousViewProperty.getTransitionInfo().currentPlayTime;
                requestTransitionWithDelayConsume(legitViewProperty, consume);
            }
        }
    }

    protected void transitAndRequestNext(ViewProperty property) {
        transit(property);
        requestNextTransition(property);
    }

    private void transit(ViewProperty property) {
        S listener = makeTransitionListener(property);
        onCancelTransitionByViewProperty(property);
        onTransit(property, listener);
    }

    protected abstract void onTransit(ViewProperty property, S transitionListener);

    public void putViewPropertyIfRoom(ViewProperty requestedViewProperty, int key) {
        // 재사용된 뷰를 사용하는 ViewProperty 가 들어올 경우 해당 뷰가 속한 ViewProperty 의 트랜지션을 취소하고 제거한다
        cancelAndRemoveRecycledViewProperty(requestedViewProperty);

        if (mViewProperties.isContainingKey(key)) {
            cancelAndResetTransitionAt(key);
        }
        putViewProperty(requestedViewProperty, key);
        transitItemOnFlyAt(key);
    }

    private void cancelAndRemoveRecycledViewProperty(ViewProperty requestedViewProperty) {
        View requestedView = requestedViewProperty.getView();
        ViewProperty recycledViewProperty = mViewProperties.getViewPropertyByView(requestedView);
        if (recycledViewProperty != null) {
            int recycledKey = recycledViewProperty.getViewIndex();
            cancelTransitionInternal(recycledViewProperty, true);
            mViewProperties.removeViewPropertyByKey(recycledKey);
        }
    }

    protected abstract void transitItemOnFlyAt(int index);

    private void putViewProperty(ViewProperty requestedViewProperty, int key) {
        mViewProperties.putViewPropertyByKey(key, requestedViewProperty);
    }

    public void removeViewPropertyByKey(int key) {
        cancelAndResetTransitionAt(key);
        mViewProperties.removeViewPropertyByKey(key);
    }

    private void updateViewProperty(ViewProperty requestedViewProperty, int idx) {
        ViewProperty viewProperty = mViewProperties.getViewPropertyByKey(idx);
        viewProperty.setView(requestedViewProperty.getView());
        viewProperty.setAnimationListener(requestedViewProperty.getAnimationListener());
    }

    public void setTransitionProperty(T transitionProperty) {
        mTransitionProperty = transitionProperty;
    }

    protected boolean isLastTransition(ViewProperty property) {
        List transitions = getTransitionProperty().getDummyTransitions();
        return property.getTransitionInfo().index == transitions.size() - 1;
    }

    protected boolean isReadyForTransition() {
        return mViewProperties.size() > 0 && mTransitionProperty != null;
    }

    protected boolean isCancelled() {
        return mStartTimeInMilli == INVALID_START_TIME;
    }

    protected long getStartTimeInMilli() {
        return mStartTimeInMilli;
    }

    protected T getTransitionProperty() {
        return mTransitionProperty;
    }

    protected abstract S makeTransitionListener(ViewProperty property);

    protected ViewProperties getViewProperties() {
        return mViewProperties;
    }

    private static class TransitionHandler extends Handler {
        private WeakReference<SerialAnimator> mAnimatorWeakReference;

        public TransitionHandler(SerialAnimator animator) {
            mAnimatorWeakReference = new WeakReference<>(animator);
        }

        @Override
        public void handleMessage(Message message) {
            super.handleMessage(message);

            validateMessage(message);

            final ViewProperty property = getProperty(message);
            boolean animate = property.getView() != null;

            if (animate) {
                SerialAnimator animator = mAnimatorWeakReference.get();

                animator.transitAndRequestNext(property);
            }
        }

        private ViewProperty getProperty(Message message) {
            return (ViewProperty)message.obj;
        }

        private void validateMessage(Message message) {
            if (!(message.obj instanceof ViewProperty)) {
                throw new IllegalStateException("msg.obj MUST BE an instance of subclass of "
                        + ViewProperty.class.getSimpleName());
            }
        }
    }

    public static abstract class TransitionProperty<T> {
        public interface TransitionSupplier<T> {
            public @NonNull List<T> onSupplyTransitionList(View targetView);
        }

        private TransitionSupplier<T> mTransitionSupplier;
        private long mInitialDelayInMillisec;
        private long mIntervalInMillisec;

        // DOUBT factory 로 만들어야 하나?
        public TransitionProperty(TransitionSupplier<T> transitionSupplier,
                                  long initialDelayInMillisec, long intervalInMillisec) {
            throwIfParametersAreInvalid(initialDelayInMillisec, intervalInMillisec);

            mTransitionSupplier = transitionSupplier;
            mInitialDelayInMillisec = initialDelayInMillisec;
            mIntervalInMillisec = intervalInMillisec;
        }

        private void throwIfParametersAreInvalid(long initialDelayInMillisec,
                                                 long intervalInMillisec) {
            if (initialDelayInMillisec < 0 || intervalInMillisec < 0) {
                throw new IllegalArgumentException();
            }
        }

        protected TransitionSupplier<T> getTransitionSupplier() {
            return mTransitionSupplier;
        }

        public long getInitialDelayInMillisec() {
            return mInitialDelayInMillisec;
        }

        public long getIntervalInMillisec() {
            return mIntervalInMillisec;
        }

        public List<T> getTransitions(View targetView) {
            return mTransitionSupplier.onSupplyTransitionList(targetView);
        }

        protected List<T> getDummyTransitions() {
            return getTransitions(null);
        }

        protected final long getDelay(ViewProperty property) {
//            long delay = getInitialDelayInMillisec() + getPreviousTransitionDuration(property);
            long delay = getPreviousTransitionDuration(property);

            if (property.getTransitionInfo().index == 0) {
                delay += getInitialDelayInMillisec();
                delay += getDelayForInitialTransition(property);
            }

            return delay;
        }

        protected final long getDelayForInitialTransition(ViewProperty property) {
            return getIntervalInMillisec() * property.getViewIndex();
        }

        protected long getTotalTransitionDuration() {
            long totalDuration = 0;//getDelay(viewProperty);
            List<T> transitions = getDummyTransitions();
            for (int i = 0; i < transitions.size(); i++) {
                T transition = transitions.get(i);
                totalDuration += getDuration(transition);
            }

            return totalDuration;
        }

        /**
         * 해당 뷰의 트랜지션 시작 시간을 기준으로 현재 트랜지션이 언제 시작되야 하는지의 정보
         * eg, 2번째 뷰의 3번째 트랜지션의 경우 3번째 트랜지션 시작 전까지의 시간
         * @param property
         * @return
         */
        protected long getPreviousTransitionDuration(ViewProperty property) {
            long delayBeforeTransitions;
//            View targetView = property.getView();
            if (property.getTransitionInfo().index == 0) {
                delayBeforeTransitions = 0;
            } else {
                int previousTransitionIndex = property.getTransitionInfo().index - 1;
                T previousTransition = getTransitions(null).get(previousTransitionIndex);
                delayBeforeTransitions = getDuration(previousTransition);
            }

            return delayBeforeTransitions;
        }

        protected long getDelaySinceBase(ViewProperty property) {
            long delay = 0;
            List<T> transitions = getDummyTransitions();
            for (int i = 0; i < property.getTransitionInfo().index; i++) {
                T transition = transitions.get(i);
                delay += getDuration(transition);
            }

            return delay;
        }

        protected boolean inTimeToTransit(ViewProperty property, long timePast) {
//            long delay = getDelay(property);
            long startTime = getDelayForInitialTransition(property);
            long endTime = startTime + getTotalTransitionDuration();

            return timePast > startTime && timePast < endTime;
        }

        protected boolean shouldTransitInFuture(ViewProperty property, long timePast) {
            long delayForInitialTransition = getDelayForInitialTransition(property);

            return  delayForInitialTransition > timePast;
        }

        public int getTransitionIndexForProperty(ViewProperty property, long timePast) {
            List<T> transitions = getDummyTransitions();
            long transitionStartTime = getDelayForInitialTransition(property);
//            long playTime = property.getTransitionInfo().currentPlayTime;
            int transitionIndex = 0;
            for (int i = 0; i < transitions.size(); i++) {
                T transition = transitions.get(i);
                long transitionEndTime = transitionStartTime + getDuration(transition);

                if (timePast > transitionStartTime && timePast < transitionEndTime) {
                    transitionIndex = i;
                    break;
                } else {
                    transitionStartTime = transitionEndTime;
                }
            }

            return transitionIndex;
        }

        protected abstract long getDuration(T transition);
    }
}
