package me.ykrank.s1next.viewmodel;

import android.databinding.ObservableField;
import android.net.Uri;
import android.view.View;

import me.ykrank.s1next.data.api.model.HomeReply;
import me.ykrank.s1next.view.activity.PostListGatewayActivity;

/**
 * Created by ykrank on 2017/2/4.
 */

public final class HomeReplyItemViewModel {
    public final ObservableField<HomeReply> reply = new ObservableField<>();

    public final void onClick(View v) {
        PostListGatewayActivity.start(v.getContext(), Uri.parse(reply.get().getUrl()));
    }
}
