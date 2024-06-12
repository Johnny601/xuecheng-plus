
package com.xuecheng.base.exception;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

/**
 * @description 通用错误信息
 * @author Mr.M
 * @date 2022/9/6 11:29
 * @version 1.0
 */
@Getter
@AllArgsConstructor
public enum CommonError {
	UNKNOWN_ERROR("执行过程异常，请重试。"),
	PARAMS_ERROR("非法参数"),
	OBJECT_NULL("对象为空"),
	QUERY_NULL("查询结果为空"),
	REQUEST_NULL("请求参数为空");

	private final String errMessage;
}
