package com.fiftytalk.carddraw.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "card-draw")
public class CardDrawProperties {
    private int dailyFreeQuota = 5;
    private int maxCourseResetPerDay = 10;
    private int maxPracticeExtraPerDay = 20;
    private int businessResetHour = 5;
    private String cdnBaseUrl = "https://cdn.51talkjr.com/";
    private String uploadDir = "uploads";

    public int getDailyFreeQuota() {
        return dailyFreeQuota;
    }

    public void setDailyFreeQuota(int dailyFreeQuota) {
        this.dailyFreeQuota = dailyFreeQuota;
    }

    public int getMaxCourseResetPerDay() {
        return maxCourseResetPerDay;
    }

    public void setMaxCourseResetPerDay(int maxCourseResetPerDay) {
        this.maxCourseResetPerDay = maxCourseResetPerDay;
    }

    public int getMaxPracticeExtraPerDay() {
        return maxPracticeExtraPerDay;
    }

    public void setMaxPracticeExtraPerDay(int maxPracticeExtraPerDay) {
        this.maxPracticeExtraPerDay = maxPracticeExtraPerDay;
    }

    public int getBusinessResetHour() {
        return businessResetHour;
    }

    public void setBusinessResetHour(int businessResetHour) {
        this.businessResetHour = businessResetHour;
    }

    public String getCdnBaseUrl() {
        return cdnBaseUrl;
    }

    public void setCdnBaseUrl(String cdnBaseUrl) {
        this.cdnBaseUrl = cdnBaseUrl;
    }

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }
}
