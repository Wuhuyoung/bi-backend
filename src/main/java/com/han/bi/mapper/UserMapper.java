package com.han.bi.mapper;

import com.han.bi.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
* @author 86183
* @description 针对表【user(用户表)】的数据库操作Mapper
* @createDate 2023-08-02 23:42:41
* @Entity com.han.bi.model.entity.User
*/
@Mapper
public interface UserMapper extends BaseMapper<User> {
    /**
     * 查询用户剩余使用次数
     * @param userAccount
     * @return
     */
    int selectLeftCount(@RequestParam("userAccount") String userAccount);

    /**
     * 查询用户上次签到时间
     * @param userAccount
     * @return
     */
    LocalDate getSignInTime(@RequestParam("userAccount") String userAccount);
}




