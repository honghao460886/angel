/*
 * FAE, Feinno App Engine
 *  
 * Create by windcraft 2011-2-1
 * 
 * Copyright (c) 2011 北京新媒传信科技有限公司
 */
package com.feinno.appengine.resource;

/**
 * FAE能够使用的资源抽象定义，每一个资源由以下三个数据定义
 * 	类型
 * 	名称
 * 	序号
 * 
 * @author 高磊 gaolei@feinno.com
 */
public interface Resource
{
	/**
	 * 返回资源类型
	 * @return
	 */
	ResourceType type();
	/**
	 * 
	 * 返回一个唯一的资源名称
	 * @return
	 */
	String name();
	
	/**
	 * 
	 * 返回资源的序号
	 * @return
	 */
	int index();
}
