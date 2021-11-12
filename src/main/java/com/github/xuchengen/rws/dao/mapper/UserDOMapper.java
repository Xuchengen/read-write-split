package com.github.xuchengen.rws.dao.mapper;

import com.github.xuchengen.rws.dao.model.UserDO;
import org.springframework.stereotype.Repository;
import tk.mybatis.mapper.common.Mapper;

@Repository(value = "userDOMapper")
public interface UserDOMapper extends Mapper<UserDO> {

    int insert(UserDO userDO);

}