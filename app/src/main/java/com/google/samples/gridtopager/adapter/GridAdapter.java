/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.gridtopager.adapter;

import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static com.google.samples.gridtopager.adapter.ImageData.IMAGE_DRAWABLES;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.transition.TransitionSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.samples.gridtopager.MainActivity;
import com.google.samples.gridtopager.R;
import com.google.samples.gridtopager.adapter.GridAdapter.ImageViewHolder;
import com.google.samples.gridtopager.fragment.ImagePagerFragment;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 显示图片网格列表对应的Adapter
 */
public class GridAdapter extends RecyclerView.Adapter<ImageViewHolder> {

  /**
   * 用来处理图片获取完成和点击事件的监听器
   */
  private interface ViewHolderListener {

    void onLoadCompleted(ImageView view, int adapterPosition);

    void onItemClicked(View view, int adapterPosition);
  }

  private final RequestManager requestManager;
  private final ViewHolderListener viewHolderListener;

  /**
   * 提供给Fragment的构造函数
   */
  public GridAdapter(Fragment fragment) {
    this.requestManager = Glide.with(fragment);
    this.viewHolderListener = new ViewHolderListenerImpl(fragment);
  }

  @Override
  public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.image_card, parent, false);
    return new ImageViewHolder(view, requestManager, viewHolderListener);
  }

  @Override
  public void onBindViewHolder(ImageViewHolder holder, int position) {
    holder.onBind();
  }

  @Override
  public int getItemCount() {
    return IMAGE_DRAWABLES.length;
  }


  /**
   * 默认的 {@link ViewHolderListener}接口实现
   */
  private static class ViewHolderListenerImpl implements ViewHolderListener {

    private Fragment fragment;
    private AtomicBoolean enterTransitionStarted;

    ViewHolderListenerImpl(Fragment fragment) {
      this.fragment = fragment;
      this.enterTransitionStarted = new AtomicBoolean();
    }

    @Override
    public void onLoadCompleted(ImageView view, int position) {
      // 只有所选图片加载完成时才会调用startPostponedEnterTransition()方法
      if (MainActivity.currentPosition != position) {
        return;
      }
      if (enterTransitionStarted.getAndSet(true)) {
        return;
      }
      fragment.startPostponedEnterTransition();
    }

    /**
     * 处理单个View的点击事件：启动一个显示当前图片的{@link  ImagePagerFragment}，并将图片的当前位置传递给Fragment
     *
     * @param view 所点击的{@link ImageView} 共享元素将会在GridFragment中的SharedElementCallback重新映射
     * @param position 所选中View的位置
     */
    @Override
    public void onItemClicked(View view, int position) {
      // 更新位置
      MainActivity.currentPosition = position;

      // 从已经存在的过渡动画列表中排除所点击的View。（举例）Exclude the clicked card from the exit transition (e.g. the card will disappear immediately
      // instead of fading out with the rest to prevent an overlapping animation of fade and move).
      if (Build.VERSION.SDK_INT >= LOLLIPOP) {
        ((TransitionSet) fragment.getExitTransition()).excludeTarget(view, true);
      } else {
        ((android.support.transition.TransitionSet) fragment.getExitTransition()).excludeTarget(view, true);
      }

      ImageView transitioningView = view.findViewById(R.id.card_image);
      String transitionName;
      if(Build.VERSION.SDK_INT >= LOLLIPOP){
        transitionName = transitioningView.getTransitionName();
      } else{
        transitionName = ViewCompat.getTransitionName(transitioningView);
      }

      fragment.getFragmentManager()
          .beginTransaction()
          .setReorderingAllowed(true) // Optimize for shared element transition
          .addSharedElement(transitioningView, transitionName)
          .replace(R.id.fragment_container, new ImagePagerFragment(), ImagePagerFragment.class
              .getSimpleName())
          .addToBackStack(null)
          .commit();
    }
  }

  /**
   * ViewHolder for the grid's images.
   */
  static class ImageViewHolder extends RecyclerView.ViewHolder implements
      View.OnClickListener {

    private final ImageView image;
    private final RequestManager requestManager;
    private final ViewHolderListener viewHolderListener;

    ImageViewHolder(View itemView, RequestManager requestManager,
        ViewHolderListener viewHolderListener) {
      super(itemView);
      this.image = itemView.findViewById(R.id.card_image);
      this.requestManager = requestManager;
      this.viewHolderListener = viewHolderListener;
      itemView.findViewById(R.id.card_view).setOnClickListener(this);
    }

    /**
     * Binds this view holder to the given adapter position.
     *
     * The binding will load the image into the image view, as well as set its transition name for
     * later.
     */
    void onBind() {
      int adapterPosition = getAdapterPosition();
      setImage(adapterPosition);
      // Set the string value of the image resource as the unique transition name for the view.
      if(Build.VERSION.SDK_INT >= LOLLIPOP){
        image.setTransitionName(String.valueOf(IMAGE_DRAWABLES[adapterPosition]));
      } else {
        ViewCompat.setTransitionName(image, String.valueOf(IMAGE_DRAWABLES[adapterPosition]));
      }
    }

    void setImage(final int adapterPosition) {
      // Load the image with Glide to prevent OOM error when the image drawables are very large.
      requestManager
          .load(IMAGE_DRAWABLES[adapterPosition])
          .listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                Target<Drawable> target, boolean isFirstResource) {
              viewHolderListener.onLoadCompleted(image, adapterPosition);
              return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable>
                target, DataSource dataSource, boolean isFirstResource) {
              viewHolderListener.onLoadCompleted(image, adapterPosition);
              return false;
            }
          })
          .into(image);
    }

    @Override
    public void onClick(View view) {
      // Let the listener start the ImagePagerFragment.
      viewHolderListener.onItemClicked(view, getAdapterPosition());
    }
  }

}