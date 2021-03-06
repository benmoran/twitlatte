/*
 * Copyright 2015-2018 The twitlatte authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.moko256.twitlatte;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.moko256.twitlatte.entity.Repeat;
import com.github.moko256.twitlatte.entity.Status;
import com.github.moko256.twitlatte.entity.StatusObject;
import com.github.moko256.twitlatte.entity.StatusObjectKt;
import com.github.moko256.twitlatte.glide.GlideApp;
import com.github.moko256.twitlatte.glide.GlideRequests;
import com.github.moko256.twitlatte.repository.PreferenceRepository;
import com.github.moko256.twitlatte.widget.TweetImageTableView;

import java.util.List;

import twitter4j.User;

/**
 * Created by moko256 on 2016/02/11.
 *
 * @author moko256
 */
class StatusesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Long> data;
    private final Context context;
    private OnLoadMoreClickListener onLoadMoreClick;
    private boolean shouldShowMediaOnly = false;

    StatusesAdapter(Context context, List<Long> data) {
        this.context = context;
        this.data = data;

        setHasStableIds(true);
    }

    public void setOnLoadMoreClick(OnLoadMoreClickListener onLoadMoreClick) {
        this.onLoadMoreClick = onLoadMoreClick;
    }

    public OnLoadMoreClickListener getOnLoadMoreClick() {
        return onLoadMoreClick;
    }

    public boolean shouldShowMediaOnly() {
        return shouldShowMediaOnly;
    }

    public void setShouldShowMediaOnly(boolean shouldShowMediaOnly) {
        this.shouldShowMediaOnly = shouldShowMediaOnly;
    }

    @Override
    public long getItemId(int position) {
        return data.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        if (data.get(position) == -1L ){
            return R.layout.layout_list_load_more_text;
        }
        StatusObject status = GlobalApplication.statusCache.get(data.get(position));
        if (status == null){
            return R.layout.layout_list_load_more_text;
        }
        Status item = ((Status) (status instanceof Repeat ? GlobalApplication.statusCache.get(((Repeat) status).getRepeatedStatusId()) : status));
        if (item == null){
            return R.layout.layout_list_load_more_text;
        }

        User user = GlobalApplication.userCache.get(StatusObjectKt.getId(status));

        PreferenceRepository conf = GlobalApplication.preferenceRepository;
        if((conf.getBoolean(GlobalApplication.KEY_IS_PATTERN_TWEET_MUTE, false) && conf.getPattern(GlobalApplication.KEY_TWEET_MUTE_PATTERN).matcher(item.getText()).find()) ||
                (conf.getBoolean(GlobalApplication.KEY_IS_PATTERN_USER_SCREEN_NAME_MUTE, false) && conf.getPattern(GlobalApplication.KEY_USER_SCREEN_NAME_MUTE_PATTERN).matcher(user.getScreenName()).find()) ||
                (conf.getBoolean(GlobalApplication.KEY_IS_PATTERN_USER_NAME_MUTE, false) && conf.getPattern(GlobalApplication.KEY_USER_NAME_MUTE_PATTERN).matcher(user.getName()).find()) ||
                (conf.getBoolean(GlobalApplication.KEY_IS_PATTERN_TWEET_SOURCE_MUTE, false) && conf.getPattern(GlobalApplication.KEY_TWEET_SOURCE_MUTE_PATTERN).matcher((item.getSourceName() != null)?item.getSourceName():"").find())
                ){
            return R.layout.layout_list_muted_text;
        }
        if (shouldShowMediaOnly || (conf.getBoolean(GlobalApplication.KEY_IS_PATTERN_TWEET_MUTE_SHOW_ONLY_IMAGE, false)
                && item.getMedias() != null
                && conf.getPattern(GlobalApplication.KEY_TWEET_MUTE_SHOW_ONLY_IMAGE_PATTERN).matcher(item.getText()).find())) {
            return R.layout.layout_list_tweet_only_image;
        }
        if (status instanceof Repeat) {
            return R.layout.layout_retweeted_post_card;
        } else {
            return R.layout.layout_post_card;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        ViewGroup child = (ViewGroup) LayoutInflater
                .from(context)
                .inflate(
                        i,
                        viewGroup,
                        false
                );
        switch (i) {
            case R.layout.layout_list_load_more_text:
                return new MoreLoadViewHolder(child);
            case R.layout.layout_list_muted_text:
                return new MutedTweetViewHolder(child);
            case R.layout.layout_list_tweet_only_image:
                return new ImagesOnlyTweetViewHolder(child);
            case R.layout.layout_retweeted_post_card:
                return new RepeatedStatusViewHolder(GlideApp.with(context), child);
            case R.layout.layout_post_card:
                return new StatusViewHolder(GlideApp.with(context), child);
            default:
                throw new RuntimeException("Invalid id");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, final int i) {
        if (viewHolder instanceof StatusViewHolder) {
            Status status = (Status) GlobalApplication.statusCache.get(data.get(i));
            User user = GlobalApplication.userCache.get(status.getUserId());
            Status quotedStatus = status.getQuotedStatusId() != -1?
                    (Status) GlobalApplication.statusCache.get(status.getQuotedStatusId())
                    :null;
            User quotedStatusUser = quotedStatus != null? GlobalApplication.userCache.get(quotedStatus.getUserId()): null;

            ((StatusViewHolder) viewHolder).setStatus(user, status, quotedStatusUser, quotedStatus);
        } else if (viewHolder instanceof RepeatedStatusViewHolder){
            StatusObject object = GlobalApplication.statusCache.get(data.get(i));
            User repeatedUser = GlobalApplication.userCache.get(((Repeat) object).getUserId());
            Status status = ((Status) GlobalApplication.statusCache.get(StatusObjectKt.getId(object)));
            User user = GlobalApplication.userCache.get(status.getUserId());
            Status quotedStatus = status.getQuotedStatusId() != -1?
                    (Status) GlobalApplication.statusCache.get(status.getQuotedStatusId())
                    :null;
            User quotedStatusUser = quotedStatus != null? GlobalApplication.userCache.get(quotedStatus.getUserId()): null;

            ((RepeatedStatusViewHolder) viewHolder).setStatus(repeatedUser, user, status, quotedStatusUser, quotedStatus);
        } else if (viewHolder instanceof ImagesOnlyTweetViewHolder){
            Status status = (Status) GlobalApplication.statusCache.get(data.get(i));

            ((ImagesOnlyTweetViewHolder) viewHolder).setStatus(status);
        } else if (viewHolder instanceof MoreLoadViewHolder) {
            ((MoreLoadViewHolder) viewHolder).setIsLoading(false);
            viewHolder.itemView.setOnClickListener(v -> {
                ((MoreLoadViewHolder) viewHolder).setIsLoading(true);
                onLoadMoreClick.onClick(i);
            });
            ViewGroup.LayoutParams layoutParams = viewHolder.itemView.getLayoutParams();
            if (layoutParams instanceof StaggeredGridLayoutManager.LayoutParams) {
                ((StaggeredGridLayoutManager.LayoutParams) layoutParams).setFullSpan(true);
            }
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof StatusViewHolder){
            ((StatusViewHolder) holder).clear();
        } else if (holder instanceof RepeatedStatusViewHolder){
            ((RepeatedStatusViewHolder) holder).clear();
        } else if (holder instanceof ImagesOnlyTweetViewHolder){
            ((ImagesOnlyTweetViewHolder) holder).setStatus(null);
        }
    }

    @Override
    public int getItemCount() {
        if (data != null) {
            return data.size();
        } else {
            return 0;
        }
    }

    private class StatusViewHolder extends RecyclerView.ViewHolder {
        final StatusViewBinder statusViewBinder;

        StatusViewHolder(GlideRequests glideRequests, ViewGroup itemView) {
            super(itemView);
            statusViewBinder = new StatusViewBinder(glideRequests, itemView);
        }

        void setStatus(User user, Status status, User quotedStatusUser, Status quotedStatus) {
            statusViewBinder.setStatus(user, status, quotedStatusUser, quotedStatus);
        }

        void clear() {
            statusViewBinder.clear();
        }
    }

    private class RepeatedStatusViewHolder extends RecyclerView.ViewHolder {
        final StatusViewBinder statusViewBinder;

        RepeatedStatusViewHolder(GlideRequests glideRequests, ViewGroup itemView) {
            super(itemView);
            statusViewBinder = new StatusViewBinder(glideRequests, itemView);
        }

        void setStatus(User repeatedUser, User user, Status status, User quotedStatusUser, Status quotedStatus) {
            statusViewBinder.setStatus(user, status, quotedStatusUser, quotedStatus);
        }

        void clear() {
            statusViewBinder.clear();
        }
    }

    private class MoreLoadViewHolder extends RecyclerView.ViewHolder {
        final TextView text;
        final ProgressBar progressBar;

        private boolean isLoading = false;

        MoreLoadViewHolder(ViewGroup viewGroup) {
            super(viewGroup);
            text = itemView.findViewById(R.id.layout_list_load_more_text_view);
            progressBar = itemView.findViewById(R.id.layout_list_load_more_text_progress);
        }

        void setIsLoading(boolean isLoading){
            this.isLoading = isLoading;
            itemView.setClickable(!isLoading);
            text.setVisibility(isLoading? View.INVISIBLE: View.VISIBLE);
            progressBar.setVisibility(isLoading? View.VISIBLE: View.INVISIBLE);
        }

        boolean getIsLoading(){
            return isLoading;
        }
    }

    private class MutedTweetViewHolder extends RecyclerView.ViewHolder {
        MutedTweetViewHolder(ViewGroup viewGroup) {
            super(LayoutInflater.from(context).inflate(R.layout.layout_list_muted_text, viewGroup, false));
        }
    }

    private class ImagesOnlyTweetViewHolder extends RecyclerView.ViewHolder {
        final TweetImageTableView tweetImageTableView;

        ImagesOnlyTweetViewHolder(ViewGroup viewGroup) {
            super(viewGroup);
            tweetImageTableView = itemView.findViewById(R.id.list_tweet_image_container);
        }

        void setStatus(Status status) {
            if (status != null) {
                tweetImageTableView.setMediaEntities(status.getMedias(), status.isSensitive());
                tweetImageTableView.setOnLongClickListener(v -> {
                    context.startActivity(ShowTweetActivity.getIntent(context, status.getId()));
                    return true;
                });
            } else {
                tweetImageTableView.clearImages();
            }
        }
    }

    interface OnLoadMoreClickListener {
        void onClick(int position);
    }
}
