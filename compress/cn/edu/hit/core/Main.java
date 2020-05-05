package cn.edu.hit.core;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main
{

	public static void main(String[] args)
	{
		System. out .println( " 内存信息 :" + toMemoryInfo());
		int seeds = 40;
		String filePath = "/home/yangli/Documents/compress/60X/NC_60X.fastq.sorted.bam";
		String writePath = "/home/yangli/Documents/compress/60X";
		long lstart1 = System.currentTimeMillis();
		
		
//		VerticalEncode compressResult = new VerticalEncode(filePath,seeds);
		VerticalEncode compressResult2  = new VerticalEncode(filePath,writePath,seeds);
		
		System. out .println( " 内存信息 :" + toMemoryInfo());
		long lend1 = System.currentTimeMillis();
		long time = (lend1 - lstart1);
		
//		long lstart2 = System.currentTimeMillis();
//		VerticalDecode deCompressResult = new VerticalDecode(compressResult);
//		long lend2 = System.currentTimeMillis();
//		long time2 = (lend2 - lstart2);
		
		System.out.println("Compressing time：" + time / 1000 / 60 / 60 + " h:" + time / 1000 / 60 % 60 + " m:"
				+ time / 1000 % 60 + " s");
//		System.out.println("Decompressing time：" + time / 1000 / 60 / 60 + " h:" + time / 1000 / 60 % 60 + " m:"
//				+ time2 / 1000 % 60 + " s");
		
//		WriteBy(writePath,compressResult,0);
	}

	public static void WriteBy(String writePath, VerticalEncode compressResult)
	{
		String fileDestEx = writePath+"/exBy";
		String fileDestExQual =writePath+"/exQual";
		String fileDestPbwtQual = writePath+"/pbwtQual";
		String fileDestPbwt = writePath+"/pbwt";
		String fileDestStart = writePath+"/start";
		Path path = null;
		try {
              path = Paths.get(fileDestEx);
              Files.write(path, compressResult.getAllRes().getExceptionResult());
              path = Paths.get(fileDestExQual);
              Files.write(path, compressResult.getAllRes().getExceptionQuaResult());
              path = Paths.get(fileDestPbwtQual);
              Files.write(path, compressResult.getAllRes().getReadQuaReasult());
              path = Paths.get(fileDestPbwt);
              Files.write(path, compressResult.getAllRes().getReadsResult());
              path = Paths.get(fileDestStart);
              Files.write(path, compressResult.getAllRes().getStartResult());
            	
        } catch (IOException e) {
            e.printStackTrace();
        }
		
	}

	public static void WriteBy(String writePath, VerticalEncode compressResult, int seeds)
	{
		String fileDestEx = null;
		String fileDestExQual = null;
		String fileDestPbwtQual = null;
		String fileDestPbwt = null;
		String fileDestStart = null;
		Path path = null;
		fileDestEx = writePath+"/exBy"+seeds;
		fileDestExQual =writePath+"/exQual"+seeds;
		fileDestPbwtQual = writePath+"/pbwtQual"+seeds;
		fileDestPbwt = writePath+"/pbwt"+seeds;
		fileDestStart = writePath+"/start"+seeds;
		try {
              path = Paths.get(fileDestEx);
              Files.write(path, compressResult.getAllRes().getExceptionResult());
              path = Paths.get(fileDestExQual);
              Files.write(path, compressResult.getAllRes().getExceptionQuaResult());
              path = Paths.get(fileDestPbwtQual);
              Files.write(path, compressResult.getAllRes().getReadQuaReasult());
              path = Paths.get(fileDestPbwt);
              Files.write(path, compressResult.getAllRes().getReadsResult());
              path = Paths.get(fileDestStart);
              Files.write(path, compressResult.getAllRes().getStartResult());
            	
        } catch (IOException e) {
            e.printStackTrace();
        }
		
		
	}
	
	/**
	 * 检测内存
	 * @return
	 */
	 public static String toMemoryInfo()
	 {
		 
	       Runtime currRuntime = Runtime.getRuntime ();
	       int nFreeMemory = ( int ) (currRuntime.freeMemory() / 1024 / 1024);
	       int nTotalMemory = ( int ) (currRuntime.totalMemory() / 1024 / 1024);
	       return nFreeMemory + "M/" + nTotalMemory +"M(free/total)" ;
	    }

}
