/*
 * FAE, Feinno App Engine
 *  
 * Create by lichunlei 2010-11-26
 * 
 * Copyright (c) 2010 北京新媒传信科技有限公司
 */
package com.feinno.configuration;

/**
 * 配置失败异常, 常见于配置更新失败场合
 * 
 * @author lichunlei
 */
public class ConfigurationFailedException extends ConfigurationException
{
	private static final long serialVersionUID = 8636671796268376690L;

	public ConfigurationFailedException(ConfigType type, String path, Exception e)
	{
		super(String.format("ConfigFailed<%1$s>, %2$s", type, path), e);
	}
}
