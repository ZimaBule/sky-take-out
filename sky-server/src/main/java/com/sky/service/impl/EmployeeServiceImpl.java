package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;

@Service
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        // TODO 后期需要进行md5加密，然后再进行比对
        //对前端传过来的明文密码进行md5加密处理
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    public void save(EmployeeDTO employeeDTO) {
        //创建员工实体类对象
        Employee employee=new Employee();

        //将DTO中的数据复制到实体类中
        BeanUtils.copyProperties(employeeDTO, employee);

        //员工实体类对象的属性比其DTO对象多，所以剩余的属性需要手动赋值
        employee.setStatus(StatusConstant.ENABLE);
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        //设置创建人和修改人的信息(当前登录用户的id),通过BaseContext获取当前登录用户的id
        employee.setCreateUser(BaseContext.getCurrentId());
        employee.setUpdateUser(BaseContext.getCurrentId());

        //将员工对象保存到数据库中
        employeeMapper.insert(employee);

    }

    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        log.info("分页查询员工,参数为：{}", employeePageQueryDTO);
        //设置分页查询参数
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());
        //执行分页查询
        Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO);
        //返回分页查询结果
        PageResult pageResult = new PageResult(page.getTotal(), page.getResult());
        return pageResult;
    }

    //修改员工状态
    public void updateStatus(Integer status, Long id){

        //创建员工实体类对象的2种方式
        /*
        方式1：
        Employee employee=new Employee();
        employee.setStatus(status);
        employee.setId(id);
        */
        //方式2：
        Employee employee=Employee.builder()
        .status(status)
        .id(id)
        .build();

        //使用通用修改来修改员工状态
        employeeMapper.update(employee);
    }

    //根据id查询员工
    public Employee getById(Long id){
        Employee employee = employeeMapper.getById(id);
        employee.setPassword("*****");
        return employee;
    }

    //修改员工信息
    public void update(EmployeeDTO employeeDTO){
        Employee employee=new Employee();
        BeanUtils.copyProperties(employeeDTO, employee);//将DTO中的数据复制到实体类中

        employee.setUpdateTime(LocalDateTime.now());
        employee.setUpdateUser(BaseContext.getCurrentId());
        //在登录那里拦截器那里已经将此次登录者id存入上下文变量BaseContext

        employeeMapper.update(employee);
    }

}
