package me.ykrank.s1next.view.adapter.delegate;

import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.text.method.LinkMovementMethod;
import android.view.ViewGroup;

import com.github.ykrank.androidlifecycle.AndroidLifeCycle;

import java.util.List;

import javax.inject.Inject;

import me.ykrank.s1next.App;
import me.ykrank.s1next.R;
import me.ykrank.s1next.data.User;
import me.ykrank.s1next.data.api.model.Post;
import me.ykrank.s1next.data.api.model.Thread;
import me.ykrank.s1next.data.api.model.Vote;
import me.ykrank.s1next.data.pref.GeneralPreferencesManager;
import me.ykrank.s1next.databinding.ItemPostBinding;
import me.ykrank.s1next.viewmodel.PostViewModel;
import me.ykrank.s1next.widget.RxBus;
import me.ykrank.s1next.widget.span.PostMovementMethod;

public final class PostAdapterDelegate extends BaseAdapterDelegate<Post, PostAdapterDelegate.ItemViewBindingHolder> {

    @Inject
    RxBus mRxBus;
    @Inject
    User mUser;
    @Inject
    GeneralPreferencesManager mGeneralPreferencesManager;

    private final Fragment fragment;
    @Nullable
    private Thread threadInfo;
    @Nullable
    private Vote voteInfo;

    public PostAdapterDelegate(Fragment fragment) {
        super(fragment.getContext());

        this.fragment = fragment;
        App.getAppComponent().inject(this);
    }

    private static void setTextSelectable(ItemPostBinding binding, boolean selectable) {
        binding.authorName.setTextIsSelectable(selectable);
        binding.tvFloor.setTextIsSelectable(selectable);
        binding.tvReply.setTextIsSelectable(selectable);
        binding.authorName.setMovementMethod(LinkMovementMethod.getInstance());
        binding.tvFloor.setMovementMethod(LinkMovementMethod.getInstance());
        binding.tvReply.setMovementMethod(PostMovementMethod.getInstance());
        binding.tvFloor.setLongClickable(false);
    }

    @NonNull
    @Override
    protected Class<Post> getTClass() {
        return Post.class;
    }

    @Override
    public boolean isForViewType(@NonNull List<Object> items, int position) {
        return super.isForViewType(items, position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent) {
        ItemPostBinding binding = DataBindingUtil.inflate(mLayoutInflater,
                R.layout.item_post, parent, false);
        binding.setPostViewModel(new PostViewModel(mRxBus, mUser));

        //Bind textview lifecycle to fragment
        AndroidLifeCycle.bindFragment(binding.tvReply, fragment);

        //If setTextIsSelectable, then should reset movement
        boolean selectable = mGeneralPreferencesManager.isPostSelectable();
        setTextSelectable(binding, selectable);

        return new ItemViewBindingHolder(binding);
    }

    @Override
    public void onBindViewHolderData(Post post, int position, @NonNull ItemViewBindingHolder holder, @NonNull List<Object> payloads) {
        ItemPostBinding binding = holder.itemPostBinding;

        boolean selectable = mGeneralPreferencesManager.isPostSelectable();
        if (selectable != binding.tvReply.isTextSelectable()) {
            setTextSelectable(binding, selectable);
        }

        binding.getPostViewModel().thread.set(threadInfo);
        binding.getPostViewModel().post.set(post);

        if ("1".equals(post.getCount())) {
            binding.getPostViewModel().vote.set(voteInfo);
        } else {
            binding.getPostViewModel().vote.set(null);
        }

        binding.executePendingBindings();
    }

    // Bug workaround for losing text selection ability, see:
    // https://code.google.com/p/android/issues/detail?id=208169
    @Override
    protected void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (mGeneralPreferencesManager.isPostSelectable()) {
            ItemPostBinding binding = ((ItemViewBindingHolder) holder).itemPostBinding;
            binding.authorName.setEnabled(false);
            binding.tvFloor.setEnabled(false);
            binding.tvReply.setEnabled(false);
            binding.authorName.setEnabled(true);
            binding.tvFloor.setEnabled(true);
            binding.tvReply.setEnabled(true);
        }
    }

    public void setThreadInfo(@NonNull Thread threadInfo) {
        this.threadInfo = threadInfo;
    }

    public void setVoteInfo(@Nullable Vote voteInfo) {
        this.voteInfo = voteInfo;
    }

    static final class ItemViewBindingHolder extends RecyclerView.ViewHolder {

        private final ItemPostBinding itemPostBinding;

        public ItemViewBindingHolder(ItemPostBinding itemPostBinding) {
            super(itemPostBinding.getRoot());

            this.itemPostBinding = itemPostBinding;
        }
    }
}
