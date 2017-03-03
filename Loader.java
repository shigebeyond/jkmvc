package com.test;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.ArrayList;

/**
 * 获得某个包下的类
 * @author shi
 *
 */
public class Loader {
	
	/**
	 * 从某个包中获得所有的类
	 * 
	 * @param pckgname
	 * @return
	 * @throws ClassNotFoundException
	 */
	public static ArrayList<Class> getClasses(String pckgname)
			throws ClassNotFoundException {
		// 获得指定包的目录
		File directory = getFiles(pckgname);
		
		// 获得包下的.class文件
		String[] files = directory.list(new FilenameFilter(){
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".class");
			}
		});
		
		// 获得类
		ArrayList<Class> classes = new ArrayList<Class>();
		for (int i = 0; i < files.length; i++) {
			// 去掉 .class 后缀 
			String file = files[i].substring(0, files[i].length() - 6);
			// 添加类
			classes.add(Class.forName(pckgname + '.' + file));
		}
		
		return classes;
	}
	
	/**
	 * 获得指定包下的目录
	 * 
	 * @param pckgname
	 * @return
	 * @throws ClassNotFoundException
	 */
	private static File getFiles(String pckgname) throws ClassNotFoundException {
		try {
			// 获得类加载器
			ClassLoader cld = Thread.currentThread().getContextClassLoader();
			if (cld == null)
				throw new ClassNotFoundException("Can't get class loader.");
			
			// 获得该路径的资源
			String path = pckgname.replace('.', '/');
			URL resource = cld.getResource(path);
			if (resource == null)
				throw new ClassNotFoundException("No resource for " + path);
			
			return new File(resource.getFile());
		} catch (NullPointerException x) {
			throw new ClassNotFoundException(pckgname + " 's directory not exists");
		}
	}
	
	public static void main(String[] args) throws ClassNotFoundException {
		for (Class clazz : getClasses("com.test")) {
			System.out.println(clazz);
		}
	}
}
