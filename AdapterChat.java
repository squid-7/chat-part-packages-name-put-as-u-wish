package com.yourpackagename.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.yourpackagename.R;
import com.yourpackagename.models.ModelChat;
import com.yourpackagename.Utils;

import java.util.ArrayList;

public class AdapterChat extends RecyclerView.Adapter<AdapterChat.HolderChat> {
    private Context context;
    private ArrayList<ModelChat> chatArrayList;

    private static final int MSG_TYPE_LEFT = 0;
    private static final int MSG_TYPE_RIGHT = 1;

    private FirebaseUser firebaseUser;

    public AdapterChat(Context context, ArrayList<ModelChat> chatArrayList) {
        this.context = context;
        this.chatArrayList = chatArrayList;
        this.firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @Override
    public int getItemViewType(int position) {
        // Distinguish sent (own) vs received messages
        if (chatArrayList.get(position).getFromUid().equals(firebaseUser.getUid())) {
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }

    @NonNull
    @Override
    public HolderChat onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MSG_TYPE_RIGHT) {
            View view = LayoutInflater.from(context).inflate(R.layout.row_chat_right, parent, false);
            return new HolderChat(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.row_chat_left, parent, false);
            return new HolderChat(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull HolderChat holder, int position) {
        ModelChat modelChat = chatArrayList.get(position);
        String messageType = modelChat.getMessageType();
        String message = modelChat.getMessage();
        long timestamp = modelChat.getTimestamp();

        // Format time for display
        String formattedDate = Utils.formatTimestampDate(timestamp);

        try {
            if (messageType.equals(Utils.MESSAGE_TYPE_TEXT)) {
                holder.messageTv.setVisibility(View.VISIBLE);
                holder.imageIv.setVisibility(View.GONE);
                holder.messageTv.setText(message);
            } else if (messageType.equals(Utils.MESSAGE_TYPE_IMAGE)) {
                holder.messageTv.setVisibility(View.GONE);
                holder.imageIv.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(message)
                        .placeholder(R.drawable.ic_image_grey)
                        .error(R.drawable.ic_image_broken_grey)
                        .into(holder.imageIv);
            }
            holder.timeTv.setText(formattedDate);
        } catch (Exception e) {
            holder.messageTv.setText("Error displaying message.");
        }
    }

    @Override
    public int getItemCount() {
        return chatArrayList.size();
    }

    // Inner ViewHolder class for both types
    class HolderChat extends RecyclerView.ViewHolder {
        TextView messageTv, timeTv;
        ImageView imageIv;

        public HolderChat(@NonNull View itemView) {
            super(itemView);
            messageTv = itemView.findViewById(R.id.messageTv);
            imageIv = itemView.findViewById(R.id.imageIv);
            timeTv = itemView.findViewById(R.id.timeTv);
        }
    }
}
