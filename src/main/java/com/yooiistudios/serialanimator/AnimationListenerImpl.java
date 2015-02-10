package com.yooiistudios.serialanimator;

import android.view.animation.Animation;

/**
 * Created by Dongheyon Jeong in News-Android-L from Yooii Studios Co., LTD. on 15. 1. 28.
 *
 * AnimationListenerImpl
 *  코드를 깔끔하게 하기 위해 미리 모든 이벤트 핸들러를 구현해 놓은 구현 클래스
 */
public class AnimationListenerImpl implements Animation.AnimationListener {
    @Override public void onAnimationStart(Animation animation) { }
    @Override public void onAnimationEnd(Animation animation) { }
    @Override public void onAnimationRepeat(Animation animation) { }
}
