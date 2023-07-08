package com.zpy.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zpy.reggie.entity.Employee;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {
}
