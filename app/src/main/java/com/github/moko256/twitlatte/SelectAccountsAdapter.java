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
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.moko256.twitlatte.entity.AccessToken;
import com.github.moko256.twitlatte.text.TwitterStringUtils;

import java.util.ArrayList;
import java.util.List;

import twitter4j.User;

/**
 * Created by moko256 on 2017/10/26.
 *
 * @author moko256
 */

public class SelectAccountsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_IMAGE = 0;
    private static final int VIEW_TYPE_ADD = 1;
    private static final int VIEW_TYPE_REMOVE = 2;

    private final Context context;

    private final ArrayList<User> users = new ArrayList<>();
    private final ArrayList<AccessToken> accessTokens = new ArrayList<>();

    public OnImageClickListener onImageButtonClickListener;
    public View.OnClickListener onAddButtonClickListener;
    public View.OnClickListener onRemoveButtonClickListener;

    public SelectAccountsAdapter(Context context){
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        return (position < accessTokens.size())? VIEW_TYPE_IMAGE: ((position == accessTokens.size())? VIEW_TYPE_ADD: VIEW_TYPE_REMOVE);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_IMAGE){
            return new ImageChildViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_select_accounts_image_child, parent, false));
        } else {
            return new ResourceIconImageViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_select_accounts_resource_image, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int type = getItemViewType(position);
        switch (type) {
            case VIEW_TYPE_IMAGE: {
                ImageChildViewHolder viewHolder = (ImageChildViewHolder) holder;

                User user = users.get(position);
                AccessToken accessToken = accessTokens.get(position);

                if (user != null) {

                    Uri image = Uri.parse(user.get400x400ProfileImageURLHttps());
                    viewHolder.title.setText(TwitterStringUtils.plusAtMark(user.getScreenName(), accessToken.getUrl()));
                    GlideApp.with(context).load(image).circleCrop().into(viewHolder.image);
                    viewHolder.itemView.setOnClickListener(v -> {
                        if (onImageButtonClickListener != null) {
                            onImageButtonClickListener.onClick(accessToken);
                        }
                    });

                } else {

                    viewHolder.title.setText(TwitterStringUtils.plusAtMark(accessToken.getScreenName(), accessToken.getUrl()));
                    viewHolder.itemView.setOnClickListener(v -> {
                        if (onImageButtonClickListener != null) {
                            onImageButtonClickListener.onClick(accessToken);
                        }
                    });

                }
                break;
            }
            case VIEW_TYPE_ADD: {
                ResourceIconImageViewHolder viewHolder = (ResourceIconImageViewHolder) holder;
                viewHolder.image.setImageResource(R.drawable.ic_add_white_24dp);
                viewHolder.title.setText(R.string.add_account);
                viewHolder.itemView.setOnClickListener(onAddButtonClickListener);
                break;
            }
            case VIEW_TYPE_REMOVE: {
                ResourceIconImageViewHolder viewHolder = (ResourceIconImageViewHolder) holder;
                viewHolder.image.setImageResource(R.drawable.ic_remove_black_24dp);
                viewHolder.title.setText(R.string.do_logout);
                viewHolder.itemView.setOnClickListener(onRemoveButtonClickListener);
                break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return accessTokens.size() + 2;
    }

    public void clearImages() {
        users.clear();
        accessTokens.clear();
    }

    public void addAndUpdate(List<User> userList, List<AccessToken> accessTokenList){
        users.addAll(userList);
        accessTokens.addAll(accessTokenList);
        notifyDataSetChanged();
    }

    public void removeAccessTokensAndUpdate(AccessToken accessToken){
        int position = accessTokens.indexOf(accessToken);
        users.remove(position);
        accessTokens.remove(position);
        notifyItemRemoved(position);
    }

    private final static class ImageChildViewHolder extends RecyclerView.ViewHolder{

        ImageView image;
        TextView title;

        ImageChildViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.layout_images_adapter_image_child_image);
            title = itemView.findViewById(R.id.layout_images_adapter_image_child_title);
        }
    }

    private final static class ResourceIconImageViewHolder extends RecyclerView.ViewHolder{
        AppCompatImageView image;
        TextView title;

        ResourceIconImageViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.layout_images_adapter_resource_image_icon);
            title = itemView.findViewById(R.id.layout_images_adapter_resource_image_title);
        }
    }

    public interface OnImageClickListener {
        void onClick(AccessToken accessToken);
    }
}