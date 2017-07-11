package demo.unisys.com.rekoginitionfinal.model;

import android.os.Parcel;
import android.os.Parcelable;


public class ResponseData  implements Parcelable{
    private String ageRange;
    private String personName;
    private String personFirstName;
    private String role;
    private String gender;
    private String smile;
    private String eyeglasses;
    private String emotions;
    private String confidence;
    private String type;
    private String downloadImagePath;
    private String city;
    public ResponseData() {
    }

    protected ResponseData(Parcel in) {
        ageRange = in.readString();
        personName = in.readString();
        personFirstName = in.readString();
        role = in.readString();
        gender = in.readString();
        smile = in.readString();
        eyeglasses = in.readString();
        emotions = in.readString();
        confidence = in.readString();
        type = in.readString();
        downloadImagePath=in.readString();
        city=in.readString();
    }

    public static final Creator<ResponseData> CREATOR = new Creator<ResponseData>() {
        @Override
        public ResponseData createFromParcel(Parcel in) {
            return new ResponseData(in);
        }

        @Override
        public ResponseData[] newArray(int size) {
            return new ResponseData[size];
        }
    };

    public String getAgeRange() {
        return ageRange;
    }

    public void setAgeRange(String ageRange) {
        this.ageRange = ageRange;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public String getPersonFirstName() {
        return personFirstName;
    }

    public void setPersonFirstName(String personFirstName) {
        this.personFirstName = personFirstName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getSmile() {
        return smile;
    }

    public void setSmile(String smile) {
        this.smile = smile;
    }

    public String getEyeglasses() {
        return eyeglasses;
    }

    public void setEyeglasses(String eyeglasses) {
        this.eyeglasses = eyeglasses;
    }

    public String getEmotions() {
        return emotions;
    }

    public void setEmotions(String emotions) {
        this.emotions = emotions;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getDownloadImagePath() {
        return downloadImagePath;
    }

    public void setDownloadImagePath(String downloadImagePath) {
        this.downloadImagePath = downloadImagePath;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(ageRange);
        parcel.writeString(personName);
        parcel.writeString(personFirstName);
        parcel.writeString(role);
        parcel.writeString(gender);
        parcel.writeString(smile);
        parcel.writeString(eyeglasses);
        parcel.writeString(emotions);
        parcel.writeString(confidence);
        parcel.writeString(type);
        parcel.writeString(downloadImagePath);
        parcel.writeString(city);
    }
}
/*
{"ageRange":{"high":43,"low":26},"matchedImage":"Ananthi.jpg","emotions":[{"confidence":56.484676,"type":"HAPPY"},{"confidence":34.861034,"type":"CONFUSED"},{"confidence":5.4180803,"type":"CALM"}],"role":"CEO","gender":"Female","city":"Bangalore","confidence":99.99059,"smile":{"confidence":95.12993,"value":true},"personName":"Krishna Ananthi","statusMsg":"Matched","eyeglasses":{"confidence":94.3503,"value":false}}*/
