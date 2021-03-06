/*
 * FAE, Feinno App Engine
 *  
 * Create by gaolei 2011-1-10
 * 
 * Copyright (c) 2011 北京新媒传信科技有限公司
 */
package com.feinno.serialization.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import com.feinno.serialization.Codec;
import com.feinno.serialization.Serializer;
import com.google.gson.Gson;

/**
 * 
 * <b>描述: </b>JSON序列化编解码器，用于将一个Java对象转换为JSON格式的数据流或字节数组，该类接口遵循{@link Codec}的标准
 * <p>
 * <b>功能: </b>JSON序列化编解码器，用于将一个Java对象转换为JSON格式的数据流或字节数组
 * <p>
 * <b>用法: </b>本类建议配合{@link Serializer}使用，当一个类想使用JSON格式进行序列化时，必须为其加入
 * {@link JsonContract}注解
 * <p>
 * 
 * @author 高磊 gaolei@feinno.com
 * 
 */
public class JsonCodec implements Codec {
	Class<?> clazz;
	Gson gson;

	/**
	 * 构造方法
	 * 
	 * @param clazz
	 */
	public JsonCodec(Class clazz) {
		this.gson = new Gson();
		this.clazz = clazz;
	}

	/*
	 * @see com.feinno.serialization.Codec#encode(java.lang.Object,
	 * java.io.OutputStream)
	 */
	@Override
	public void encode(Object obj, OutputStream stream) throws IOException {
		String s = gson.toJson(obj);
		byte[] buffer = s.getBytes(Charset.forName("UTF-8"));
		stream.write(buffer, 0, buffer.length);
	}

	/*
	 * @see com.feinno.serialization.Codec#encode(java.lang.Object)
	 */
	@Override
	public byte[] encode(Object obj) throws IOException {
		String s = gson.toJson(obj);
		byte[] buffer = s.getBytes(Charset.forName("UTF-8"));
		return buffer;
	}

	/*
	 * @see com.feinno.serialization.Codec#decode(java.io.InputStream)
	 */
	@Override
	public <E> E decode(InputStream stream, int len) throws IOException {
		byte[] buffer = new byte[len];
		stream.read(buffer, 0, len);
		// return decode(buffer); //ok in eclipse, but not javac (at least
		// broken in 1.6.0_23)
		return this.<E> decode(buffer);

	}

	/*
	 * @see com.feinno.serialization.Codec#decode(byte[])
	 */
	@Override
	public <E> E decode(byte[] buffer) throws IOException {
		String s = new String(buffer); // TODO UTF-8 Encoding
		return (E) gson.fromJson(s, clazz);
	}
}
