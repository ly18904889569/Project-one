package cn.edu.hit.util;

import java.io.PrintWriter;

/**
 * 写文件的工具类
 * @author rivers
 * May 29, 2017 9:06:12 PM
 *
 */
public class FileWriterHelper {
	
	public static void writeFile(String fileSavePath,String content){
		// write into file
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(fileSavePath, "UTF-8");
		} catch (Exception e) {
		}

		writer.println(content);
		writer.flush();
	}
}
