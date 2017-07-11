
package demo.unisys.com.rekoginitionfinal.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import demo.unisys.com.rekoginitionfinal.R;
import demo.unisys.com.rekoginitionfinal.model.TextMessage;
import demo.unisys.com.rekoginitionfinal.utils.PasswordTransformationUtil;

public class MessagesListAdapter extends BaseAdapter {
    private static String TAG = "DN";
    private Context context;
    private int count;
    private static LayoutInflater layoutInflater;
    private ArrayList<TextMessage> messageArrayList;
    public Boolean mDownloadCompleted = false;
    public Boolean mRecognizationSuccess = false;

    public MessagesListAdapter(Context context, ArrayList<TextMessage> messageArrayList) {
        this.context = context;
        this.messageArrayList = messageArrayList;
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setLastRecognizationStatus(boolean recognizationComplete, Boolean recognizationSuccess) {
        for (int i = messageArrayList.size() - 1; i >= 0; i--)
        {
            TextMessage textMessage = messageArrayList.get(i);
            if (textMessage.getFrom().equals("imageType"))
            {
                if (!textMessage.getRecognizationComplete())
                {
                    textMessage.setRecognizationSuccess(recognizationSuccess);
                    textMessage.setRecognizationComplete(recognizationComplete);
                }
                break;
            }
        }

    }

    @Override
    public int getCount() {
        return messageArrayList != null ? messageArrayList.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        TextMessage item = messageArrayList.get(position);
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.man_conversation, null);
            holder = new Holder();
            holder.txtBotData = (TextView) convertView.findViewById(R.id.txtChatBot);
            holder.dotImage = (ImageView) convertView.findViewById(R.id.dot_indictaor);
            holder.txtHumanData = (TextView) convertView.findViewById(R.id.txtHumanText);
            holder.userImage = (ImageView) convertView.findViewById(R.id.imgUserImage);
            holder.mRowlyt = (LinearLayout) convertView.findViewById(R.id.lytRoot);
            holder.mLytUserImage = (RelativeLayout) convertView.findViewById(R.id.lytUserImage);
            holder.mTxtPhotoRecog = (TextView) convertView.findViewById(R.id.txtPhotoRecProcess);
            holder.mCameraIcon = (ImageView) convertView.findViewById(R.id.cam_icon);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }
        if (item.getFrom().equals("Bot")) {
            holder.txtBotData.setVisibility(View.VISIBLE);
            holder.txtBotData.setText(item.getMessage());
            holder.txtHumanData.setVisibility(View.INVISIBLE);
            holder.dotImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.red_dot));
        } else if (item.getFrom().equalsIgnoreCase("user") && !TextUtils.isEmpty(item.getMessage())) {
            holder.txtHumanData.setVisibility(View.VISIBLE);
            holder.txtHumanData.setText(item.getMessage());
            holder.txtBotData.setVisibility(View.INVISIBLE);
            holder.dotImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.green_dot));
        } else if (!TextUtils.isEmpty(item.getImagePath())) {
            if (holder.mLytUserImage != null) {
                holder.mLytUserImage.setVisibility(View.VISIBLE);
                Bitmap bitmap = BitmapFactory.decodeFile(item.getImagePath());
                holder.userImage.setImageBitmap(bitmap);
                holder.txtHumanData.setVisibility(View.GONE);
                holder.dotImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.green_dot));
                if (item.getRecognizationComplete()) {
                    holder.mTxtPhotoRecog.setText(context.getString(R.string.photoRecogDone));
                    if (item.getRecognizationSuccess()) {
                        holder.mCameraIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.success));
                    } else {
                        holder.mCameraIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.failure));
                    }
                } else {
                    holder.mCameraIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.camera_icon));
                    holder.mTxtPhotoRecog.setText(context.getString(R.string.imageRecognizationProgress));
                }

            }
        }
        holder.mRowlyt.setVisibility(View.VISIBLE);
        if (TextUtils.isEmpty(item.getMessage()) && TextUtils.isEmpty(item.getImagePath())) {
            // holder.dotImage.setVisibility(View.GONE);
            holder.txtBotData.setVisibility(View.GONE);
            holder.txtHumanData.setVisibility(View.GONE);
            holder.mLytUserImage.setVisibility(View.GONE);
            holder.mRowlyt.setVisibility(View.GONE);
        } else if (!TextUtils.isEmpty(item.getImagePath())) {
            holder.mRowlyt.setVisibility(View.VISIBLE);
            holder.mLytUserImage.setVisibility(View.VISIBLE);
            holder.dotImage.setVisibility(View.VISIBLE);
            holder.txtHumanData.setVisibility(View.GONE);
            holder.txtBotData.setVisibility(View.INVISIBLE);
        }
        if(item.getIsItPasswordField()){
            holder.txtHumanData.setTransformationMethod(new PasswordTransformationUtil());
        }
        Typeface roboto = Typeface.createFromAsset(context.getAssets(),
                "font/Roboto-Regular.ttf"); //use this.getAssets if you are calling from an Activity
        holder.txtBotData.setTypeface(roboto);
        holder.txtHumanData.setTypeface(roboto);
        return convertView;
    }

    // Helper class to recycle View's
    static class Holder {
        TextView txtBotData;
        TextView txtHumanData;
        ImageView dotImage;
        ImageView userImage;
        LinearLayout mRowlyt;
        RelativeLayout mLytUserImage;
        TextView mTxtPhotoRecog;
        ImageView mCameraIcon;
    }

    // Add new items
    public void refreshList(ArrayList<TextMessage> list) {
        this.messageArrayList.clear();
        this.messageArrayList.addAll(list);
        notifyDataSetChanged();
    }

}
