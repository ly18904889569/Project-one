package cn.edu.hit.core;

public class Main
{

	public static void main(String[] args)
	{
		System. out .println( " 内存信息 :" + toMemoryInfo());
		int seeds = 3;
		String filePath = "/home/yangli/Documents/compress/5X/NC_5X.fastq.sorted.bam";
		long lstart1 = System.currentTimeMillis();
		
		
		VerticalEncode compressResult = new VerticalEncode(filePath,seeds);
		
		System. out .println( " 内存信息 :" + toMemoryInfo());
		long lend1 = System.currentTimeMillis();
		long time = (lend1 - lstart1);
		
		long lstart2 = System.currentTimeMillis();
		VerticalDecode deCompressResult = new VerticalDecode(compressResult);
		long lend2 = System.currentTimeMillis();
		long time2 = (lend2 - lstart2);
		
		System.out.println("Compressing time：" + time / 1000 / 60 / 60 + " h:" + time / 1000 / 60 % 60 + " m:"
				+ time / 1000 % 60 + " s");
		System.out.println("Decompressing time：" + time / 1000 / 60 / 60 + " h:" + time / 1000 / 60 % 60 + " m:"
				+ time2 / 1000 % 60 + " s");
		
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
