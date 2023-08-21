package com.han.bi.utils;

import org.springframework.web.bind.annotation.GetMapping;

import java.util.concurrent.ThreadLocalRandom;

public class AvatarUtils {
    public static String[] urlSuffix = {"36596612490399385558",
            "20898456034192565741",
            "27974633210073964537",
            "73095409641895908377",
            "99727306186400866012",
            "73790421003939446586",
            "83499288035664791116",
            "32086986739249645345",
            "96455050349564197880",
            "48089585147609261093",
            "09658215458942420398",
            "57295525334935408554",
            "88927136393374940163",
            "19610456687463436508",
            "57945903941764201047",
            "81399296442863826981",
            "79669195228046538434",
            "71753925004545889864",
            "89925450129897794970",
            "72319303607780133022"};

    // 随机函数
    public static ThreadLocalRandom getRandom(){
        return ThreadLocalRandom.current();
    }

    /**
     * 返回随机头像的url
     * @return
     */
    public static String getAvatar() {
        String url = "https://avatar-1314662469.cos.ap-shanghai.myqcloud.com/avatar/";
        int num = getRandom().nextInt(21);
        url += urlSuffix[num] + ".jpeg";
        return url;
    }
}