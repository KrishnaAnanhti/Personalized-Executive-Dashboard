
package demo.unisys.com.rekoginitionfinal.model;

import android.os.Parcel;
import android.os.Parcelable;

public class TextMessage implements Parcelable {
    private String message;
    private String from;
    private String mImagePath;
    private Boolean mRecognizationComplete = false;
    private Boolean mRecognizationSuccess = false;
    private Boolean mIsItPasswordField =false;
    public TextMessage() {
    }

    public TextMessage(final String message, final String from) {
        if(null!=message && message!="")
        this.message =  message.replace(".", "");
    else
        this.message =  message;
        this.from = from;
    }

    protected TextMessage(Parcel in) {
        message = in.readString();
        from = in.readString();
        mImagePath = in.readString();
        mRecognizationComplete = in.readByte() != 0;
        mRecognizationSuccess = in.readByte() != 0;
        mIsItPasswordField = in.readByte() != 0;
    }

    public static final Creator<TextMessage> CREATOR = new Creator<TextMessage>() {
        @Override
        public TextMessage createFromParcel(Parcel in) {
            return new TextMessage(in);
        }

        @Override
        public TextMessage[] newArray(int size) {
            return new TextMessage[size];
        }
    };

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {

        if(null!=message && message!="")
            this.message =  message.replace(".", "");
        else
            this.message =  message;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getImagePath() {
        return mImagePath;
    }

    public void setImagePath(String mImagePath) {
        this.mImagePath = mImagePath;
    }

    public Boolean getRecognizationComplete() {
        return mRecognizationComplete;
    }

    public void setRecognizationComplete(Boolean mRecognizationComplete) {
        this.mRecognizationComplete = mRecognizationComplete;
    }

    public Boolean getRecognizationSuccess() {
        return mRecognizationSuccess;
    }

    public void setRecognizationSuccess(Boolean mRecognizationSuccess) {
        this.mRecognizationSuccess = mRecognizationSuccess;
    }

    public Boolean getIsItPasswordField() {
        return mIsItPasswordField;
    }

    public void setIsItPasswordField(Boolean mIsItPasswordField) {
        this.mIsItPasswordField = mIsItPasswordField;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(message);
        parcel.writeString(from);
        parcel.writeString(mImagePath);
        parcel.writeByte((byte) (mRecognizationComplete ? 1 : 0));
        parcel.writeByte((byte) (mRecognizationSuccess ? 1 : 0));
        parcel.writeByte((byte) (mIsItPasswordField ? 1 : 0));
    }
}
