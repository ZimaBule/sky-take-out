package com.sky.controller.admin;

import com.github.pagehelper.PageHelper;
import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import com.sky.utils.JwtUtil;
import com.sky.vo.EmployeeLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

//表现层11
/**
 * 员工管理
 */
@RestController
@RequestMapping("/admin/employee")
//@RequestMapping("/employee")
@Slf4j
@Api(tags = "员工相关接口")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @PostMapping("/login")
    @ApiOperation("员工登录")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) { //JSON格式的请求数据要用实体类(一般命名DTO)来接收
        log.info("员工登录：{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .build();

        return Result.success(employeeLoginVO);
    }

    /**
     * 退出
     *
     * @return
     */
    @PostMapping("/logout")
    @ApiOperation("员工退出")
    public Result<String> logout() {
        return Result.success();
    }

    @PostMapping
    @ApiOperation("新增员工")
    public Result save(@RequestBody EmployeeDTO employeeDTO){
        log.info("新增员工：{}", employeeDTO);
        employeeService.save(employeeDTO);
        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("分页查询员工")
    public Result<PageResult> pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        //前端参数是Query类型且用实体类接时，不要用@RequestParam注解，框架会自己去和DTO类里属性匹配赋值
        //因为要返回给前端：即要给Result的data属性赋值，所以把泛型<T>确定为<PageResult>
        log.info("分页查询员工,参数为：{}", employeePageQueryDTO);
        //调用service层的分页查询方法
        PageResult pageResult = employeeService.pageQuery(employeePageQueryDTO);
        return Result.success(pageResult);
    }

    //2种请求参数：路径参数、查询参数
    @PostMapping("/status/{status}")
    @ApiOperation("修改员工状态(启用/禁用)")
    public Result<String> updateStatus(@PathVariable Integer status, @RequestParam Long id) {
        //页面上虽然没显示 id 这一列（只显示姓名/账号/手机号/状态/时间/操作），
        //但前端 JS 拿到的每一行数据对象里是有 id 字段的，只是没画到界面上而已。
        //点击禁用按钮时，会发送 POST/admin/employee/status/0?id=3
        //0 走的是路径 @PathVariable Integer status；
        //id=3 走的是查询参数 @RequestParam Long id。

        log.info("修改员工状态：{},{}", status, id);
        employeeService.updateStatus(status, id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("根据id查询员工信息")
    public Result<Employee> getById(@PathVariable Long id){
        Employee employee = employeeService.getById(id);
        return Result.success(employee);
    }

    @PutMapping
    @ApiOperation("修改员工信息")
    public Result update(@RequestBody EmployeeDTO employeeDTO){
        /*
          前端看到的这个"修改员工信息"界面，只是用 Element UI组件画出来的表单样式。
          点"保存"时，它不是走浏览器原生 <form> 提交，
          而是前端 JS（Vue + axios）把各字段收集成一个对象、以 JSON 格式放进请求体发给后端。
          所以后端必须用 @RequestBody 来接。
        */
        log.info("修改员工信息：{}", employeeDTO);
        employeeService.update(employeeDTO);
        return Result.success();
    }
}
