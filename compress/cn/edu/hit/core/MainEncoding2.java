package cn.edu.hit.core;

import java.awt.geom.Line2D;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.PrimitiveIterator.OfDouble;
import java.util.concurrent.TimeUnit;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.text.AbstractDocument.LeafElement;
import javax.xml.stream.events.EndDocument;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.tools.ant.filters.FixCrLfFilter.AddAsisRemove;
import org.apache.tools.ant.taskdefs.Exit;
import org.apache.tools.ant.taskdefs.optional.clearcase.ClearCase;
import org.tukaani.xz.simple.PowerPC;
import org.apache.tools.ant.taskdefs.optional.clearcase.ClearCase;

import cn.edu.hit.model.AuxiliaryInfo;
import cn.edu.hit.model.CompressResult;
import cn.edu.hit.model.DeRead;
import cn.edu.hit.model.PBWTReadRe;
import cn.edu.hit.model.QualEnum;
import cn.edu.hit.model.READSYMBOL;
import cn.edu.hit.model.ReadElemEnum;
import cn.edu.hit.model.ReadInfo;
import cn.edu.hit.model.ReadPbwtResult;
import cn.edu.hit.model.ReadsHorizonModel;
import cn.edu.hit.model.ReadsPreProcessResult;
import cn.edu.hit.model.VerticalEncodeResult;
import cn.edu.hit.test.ReadStruct;
import cn.edu.hit.testdata.Test1;
import cn.edu.hit.testdata.Test2;
import cn.edu.hit.util.Huffman2;
import htsjdk.samtools.metrics.StringHeader;
import htsjdk.samtools.seekablestream.ISeekableStreamFactory;
import sun.security.krb5.internal.crypto.crc32;

public class MainEncoding2
{
	static CompressResult allRes = new CompressResult();
	
	static ArrayList<ArrayList<Integer>> tempIndexes = new ArrayList<>();

	static ArrayList<String> exBackUp = new ArrayList<String>();

	static int exListLength = 0;

	static int readsQualLength = 0;

	static ArrayList<ReadStruct> ReadsList; // 这里只是为了之后的检验使用，验证pbwt reads序列的解压缩正确

	public static void main(String[] args)
	{
		System. out .println( " 内存信息 :" + toMemoryInfo());
//		MainEncoding2 encoding = new MainEncoding2();

//		ReadPreProcess readPreProcess = new ReadPreProcess();
//		String filePath = "/home/yangli/Documents/compress/5X/NC_5X.fastq.sorted.bam";
//		List<List<ReadInfo>> readInfos = readPreProcess.splitBamFile(filePath);
//		ReadsPreProcessResult reads = readPreProcess.readsProc(readInfos.get(0));

		// reads 还需要经过一段处理，转化为Test1格式的数据
		 Test1 testOne = new Test1(4);
//		 GlobalCompression(testOne);
		 LocalCompresssion(testOne);
//		 CompressResult testCompress = encoding.EncodePbwt(testOne);
//		 ReadPbwtResult reCompressRes = encoding.DeCodePbwt(testCompress);
//		PrintInterval(reads);
		
//		long lstart1 = System.currentTimeMillis();
//		CompressResult CompressRes = encoding.EncodePbwt(reads);
//		System. out .println( " 内存信息 :" + toMemoryInfo());
//		long lend1 = System.currentTimeMillis();
//		long time = (lend1 - lstart1);
		// 解压缩
//		ReadPbwtResult reCompressRes = encoding.DeCodePbwt(CompressRes);
		System. out .println( " 内存信息 :" + toMemoryInfo());
		// 正确性验证
//		ProcessIsTrue(reads, reCompressRes);
//		System.out.println(time / 1000);
//		System.out.println("Using time：" + time / 1000 / 60 / 60 + " h:" + time / 1000 / 60 % 60 + " m:"
//				+ time / 1000 % 60 + " s");
		System.out.println("********************End********************");
		System. out .println( " 内存信息 :" + toMemoryInfo());
	}
	
	/**
	 * 局部压缩策略
	 * @param testOne
	 */
	private static void LocalCompresssion(Test1 testOne)
	{
		MainEncoding2 encoding = new MainEncoding2();
		CompressResult testCompress = encoding.EncodePbwt2(testOne);
		partDeCompresssion(3, 14, testCompress);
		ReadPbwtResult reCompressRes = encoding.DeCodePbwt2(testCompress);
		
	}
	/**
	 * 全局压缩策略
	 * @param testOne
	 */
	private static void GlobalCompression(Test1 testOne)
		{
			MainEncoding2 encoding = new MainEncoding2();
			CompressResult testCompress = encoding.EncodePbwt(testOne);
			ReadPbwtResult reCompressRes = encoding.DeCodePbwt(testCompress);
			
		}
	
	
	private static ArrayList<DeRead> partDeCompresssion(int start, int end, CompressResult testCompress)
	{
		int MinStart = 0;
		MinStart = deCompressLen(testCompress.getStartResult());
		if (end < MinStart)
		{
			return null;
		}
		
		ArrayList<DeRead> reads = new ArrayList<>();
		ReadPbwtResult reResult = new ReadPbwtResult();
		ArrayList<ArrayList<Integer>> starToEndReads = new ArrayList<>();
		ArrayList<ArrayList<String>> starToEndEx = new ArrayList<>();
		ArrayList<Character> starToEndExQual = new ArrayList<>();
		ArrayList<Character> starToEndQual = new ArrayList<>();
		
		AuxiliaryInfo count = new AuxiliaryInfo(start, end);
		
		// reads进行bit转为byte
		starToEndReads = DecodeTPBWT(count,MinStart,testCompress.getReadsResult());
		starToEndEx = deEncodeExceptionList(count,testCompress.getExceptionResult());
		starToEndQual = deEncodeQual(count, testCompress.getReadQuaReasult());
		starToEndExQual = deEncodeExQual(count, testCompress.getExceptionQuaResult());
		
		reResult.setMinStart(count.realstart);
		reResult.setListsPBWT(starToEndReads);
		reResult.setListsExcep(starToEndEx);
		reResult.setListsQual(starToEndQual);
		reResult.setListExQual2(starToEndExQual);
		
		reads = partPBWTAlgoRe(reResult);
		return reads;
	}
	

	/**
	 * 输出reads连续区域长度以及间隔长度
	 * @param reads
	 */
	private static void PrintInterval(ReadsPreProcessResult reads)
	{
		System.out.println("×*×*×*×*×*×*×*×*×*×*×*×*×*×*×*×*×*×*×*×*×*×*×*×*×*×*×*×*×*×*×");
		System.out.println("（horizon）start[0]："+reads.getReadsHorizon().get(0).getAlignmentStart());
		System.out.println("（readsinfo）start[0]："+reads.getReadsInfo().get(0).getAlignmentStart());
		System.out.println("（horizon）end[last]："+reads.getReadsHorizon().get(reads.getReadsHorizon().size()-1).getAlignmentEnd());
		System.out.println("（readsinfo）end[last]："+reads.getReadsInfo().get(reads.getReadsInfo().size()-1).getAlignmentEnd());
		
		int start = 0;
		int end = 0;
		int Sumdistance = 0;
		int Suminterval = 0;
//		for(int i =0; i<reads.getReadsHorizon().size()-1;i++)
//		{
//			if(reads.getReadsHorizon().get(i+1).getAlignmentStart())
//		}
	}

	/**
	 * 验证解压缩过程的正确性
	 * 
	 * @param testOne
	 * @param reCompressRes
	 */
	public static void ProcessIsTrue(Test1 testOne, ReadPbwtResult reCompressRes)
	{
		int[] deS = reCompressRes.getStartPos();
		deStartIsTrue(testOne.readStart, deS);

		ArrayList<ArrayList<Integer>> rePbwtList = reCompressRes.getListsPBWT();
		PbwtIsTrue(testOne.getListsPBWT(), rePbwtList);

		List<String> dexBy = reCompressRes.getListsExcep2();
		dexIsTrue2(exBackUp, dexBy);

		System.out.println("readsQual:");
		for (int i = 0; i < reCompressRes.getListsQual().size(); i++)
		{
			System.out.print(reCompressRes.getListsQual().get(i) + " ");
		}

		System.out.println("\n" + "exQual");
		for (int j = 0; j < reCompressRes.getListExQual2().size(); j++)
		{
			System.out.print(reCompressRes.getListExQual2().get(j) + " ");
		}
		System.out.println("\n" + "reCompressExLength " + reCompressRes.getListExQual2().size() + " \t"
				+ "reCompressQualLength: " + reCompressRes.getListsQual().size());

	}

	public static void ProcessIsTrue(ReadsPreProcessResult reads, ReadPbwtResult reCompressRes)
	{
		int[] deS = reCompressRes.getStartPos();
		deStartIsTrue(reads.getReadsHorizon(), deS);

		ArrayList<ArrayList<Integer>> rePbwtList = reCompressRes.getListsPBWT();
		PbwtIsTrue2(ReadsList, rePbwtList);

		List<String> dexBy = reCompressRes.getListsExcep2();
		dexIsTrue2(exBackUp, dexBy);

		System.out.println("readsQualLength:" + readsQualLength + "\t" + "reCompressRes.getListsQual().size():"
				+ reCompressRes.getListsQual().size());
//		System.out.println("readsQual:");
//		for (int i = 0; i < reCompressRes.getListsQual().size(); i++)
//		{
//			System.out.print(reCompressRes.getListsQual().get(i) + " ");
//		}
//
//		System.out.println("\n" + "exQual");
//		for (int j = 0; j < reCompressRes.getListExQual2().size(); j++)
//		{
//			System.out.print(reCompressRes.getListExQual2().get(j) + " ");
//		}
		System.out.println("\n" + "reCompressExLength " + reCompressRes.getListExQual2().size() + " \t"
				+ "reCompressQualLength: " + reCompressRes.getListsQual().size());

	}
	
	/**
	 * 全局压缩策略
	 * @param compressRes
	 * @return
	 */
	private ReadPbwtResult DeCodePbwt(CompressResult compressRes)
	{
		ReadPbwtResult reResult = new ReadPbwtResult();

//		reResult.setStartPos(deStartPos(compressRes.getStartResult()));
		reResult.setMinStart(deCompressLen(compressRes.getStartResult())); // 此处只是解压缩了minStart

		reResult.setListsPBWT(DecodeTPBWT(compressRes.getReadsResult()));

		reResult.setListsExcep2(Arrays.asList(deEncodeExceptionList(compressRes.getExceptionResult())));
//		reResult.setListsExcep(deEncodeExceptionList(compressRes.getExceptionResult()));
		
		reResult.setListsQual(deEncodeQual2(compressRes.getReadQuaReasult()));

//		reResult.setListExQual2(deEncodeExceptionQual2(compressRes.getExceptionQuaResult()));

		reResult.setListExQual2(deEncodeQual2(compressRes.getExceptionQuaResult()));
		
		// 添加一个函数将其整合为reads
		PBWTAlgoRe5(reResult);
		return reResult;
	}

	/**
	 * 局部压缩策略
	 * @param compressRes
	 * @return
	 */
	private ReadPbwtResult DeCodePbwt2(CompressResult compressRes)
	{
		ReadPbwtResult reResult = new ReadPbwtResult();

//		reResult.setStartPos(deStartPos(compressRes.getStartResult()));
		reResult.setMinStart(deCompressLen(compressRes.getStartResult())); // 此处只是解压缩了minStart

		reResult.setListsPBWT(DecodeTPBWT(compressRes.getReadsResult()));

//		reResult.setListsExcep2(Arrays.asList(deEncodeExceptionList(compressRes.getExceptionResult())));
		reResult.setListsExcep(deEncodeExceptionList2(compressRes.getExceptionResult()));
		
		reResult.setListsQual(deEncodeQual2(compressRes.getReadQuaReasult()));

//		reResult.setListExQual2(deEncodeExceptionQual2(compressRes.getExceptionQuaResult()));

		reResult.setListExQual2(deEncodeQual2(compressRes.getExceptionQuaResult()));
		
		// 添加一个函数将其整合为reads
		PBWTAlgoRe4(reResult);
		return reResult;
	}

	
	private ArrayList<Character> deEncodeQual2(byte[] pbwtQual)
	{
		ArrayList<Character> dexQual2 = new ArrayList<>();
		char[] deStr = new char[8];
		char[] deStr2 = new char[3];
		char[] deStr3 = new char[6];
		if (pbwtQual.length % 3 == 0)
		{
			for (int i = 0; i < pbwtQual.length; i += 3)
			{
				deStr[0] = (char) ((byte) ((((pbwtQual[i] + 128) & 0xff) >> 5) % 8) + 48);
				dexQual2.add(TranforOr(deStr[0]));
				deStr[1] = (char) ((byte) (((pbwtQual[i] & 0xff) >> 2) % 8) + 48);
				dexQual2.add(TranforOr(deStr[1]));
				deStr[2] = (char) ((byte) ((((pbwtQual[i] & 0xff) % 4) * 2)
						+ ((((pbwtQual[i + 1] + 128) & 0xff) >> 7) % 2)) + 48);
				dexQual2.add(TranforOr(deStr[2]));
				deStr[3] = (char) ((byte) (((pbwtQual[i + 1] & 0xff) >> 4) % 8) + 48);
				dexQual2.add(TranforOr(deStr[3]));
				deStr[4] = (char) ((byte) (((pbwtQual[i + 1] & 0xff) >> 1) % 8) + 48);
				dexQual2.add(TranforOr(deStr[4]));
				deStr[5] = (char) ((byte) ((((pbwtQual[i + 1] & 0xff) % 2) * 4)
						+ ((((pbwtQual[i + 2] + 128) & 0xff) >> 6) % 4)) + 48);
				dexQual2.add(TranforOr(deStr[5]));
				deStr[6] = (char) ((byte) (((pbwtQual[i + 2] & 0xff) >> 3) % 8) + 48);
				dexQual2.add(TranforOr(deStr[6]));
				deStr[7] = (char) ((byte) ((pbwtQual[i + 2] & 0xff) % 8) + 48);
				dexQual2.add(TranforOr(deStr[7]));
//				System.out.println();
			}
		}

		else
		{
			// 需要在循环终止条件上减去3，因为最后一个不一定正合适
			for (int i = 0; i < pbwtQual.length - 3; i += 3)
			{
				deStr[0] = (char) ((byte) ((((pbwtQual[i] + 128) & 0xff) >> 5) % 8) + 48);
				dexQual2.add(TranforOr(deStr[0]));
				deStr[1] = (char) ((byte) (((pbwtQual[i] & 0xff) >> 2) % 8) + 48);
				dexQual2.add(TranforOr(deStr[1]));
				deStr[2] = (char) ((byte) ((((pbwtQual[i] & 0xff) % 4) * 2)
						+ ((((pbwtQual[i + 1] + 128) & 0xff) >> 7) % 2)) + 48);
				dexQual2.add(TranforOr(deStr[2]));
				deStr[3] = (char) ((byte) (((pbwtQual[i + 1] & 0xff) >> 4) % 8) + 48);
				dexQual2.add(TranforOr(deStr[3]));
				deStr[4] = (char) ((byte) (((pbwtQual[i + 1] & 0xff) >> 1) % 8) + 48);
				dexQual2.add(TranforOr(deStr[4]));
				deStr[5] = (char) ((byte) ((((pbwtQual[i + 1] & 0xff) % 2) * 4)
						+ ((((pbwtQual[i + 2] + 128) & 0xff) >> 6) % 4)) + 48);
				dexQual2.add(TranforOr(deStr[5]));
				deStr[6] = (char) ((byte) (((pbwtQual[i + 2] & 0xff) >> 3) % 8) + 48);
				dexQual2.add(TranforOr(deStr[6]));
				deStr[7] = (char) ((byte) ((pbwtQual[i + 2] & 0xff) % 8) + 48);
				dexQual2.add(TranforOr(deStr[7]));
//				System.out.println();

			}
			// 多出一个字节的情况，策略就是全部还原，可能会多出来几个，但是最后拼接的时候就没了
			if (pbwtQual.length % 3 == 1)
			{
				deStr2[0] = (char) ((byte) ((((pbwtQual[pbwtQual.length - 1] + 128) & 0xff) >> 5) % 8) + 48);
				deStr2[1] = (char) ((byte) (((pbwtQual[pbwtQual.length - 1] & 0xff) >> 2) % 8) + 48);
				dexQual2.add(TranforOr(deStr2[0]));
				dexQual2.add(TranforOr(deStr2[1]));
				// System.out.print(" " + deStr2[0] + " " + deStr2[1] );
			}
			// 多出两个字节的情况，全部还原
			if (pbwtQual.length % 3 == 2)
			{
				deStr3[0] = (char) ((byte) ((((pbwtQual[pbwtQual.length - 2] + 128) & 0xff) >> 5) % 8) + 48);
				dexQual2.add(TranforOr(deStr3[0]));
				deStr3[1] = (char) ((byte) (((pbwtQual[pbwtQual.length - 2] & 0xff) >> 2) % 8) + 48);
				dexQual2.add(TranforOr(deStr3[1]));
				deStr3[2] = (char) ((byte) ((((pbwtQual[pbwtQual.length - 2] & 0xff) % 4) * 2)
						+ ((((pbwtQual[pbwtQual.length - 1] + 128) & 0xff) >> 7) % 2)) + 48);
				dexQual2.add(TranforOr(deStr3[2]));
				deStr3[3] = (char) ((byte) (((pbwtQual[pbwtQual.length - 1] & 0xff) >> 4) % 8) + 48);
				dexQual2.add(TranforOr(deStr3[3]));
				deStr3[4] = (char) ((byte) (((pbwtQual[pbwtQual.length - 1] & 0xff) >> 1) % 8) + 48);
				dexQual2.add(TranforOr(deStr3[4]));
			}
		}

		return dexQual2;
	}

	private ArrayList<String> deEncodeQual(byte[] pbwtQual)
	{
		ArrayList<String> dexQual = new ArrayList<>();
		ArrayList<String> dexQual2 = new ArrayList<>();
		String[] deStr = new String[8];
		String[] deStr2 = new String[3];
		String[] deStr3 = new String[6];

		if (pbwtQual.length % 3 == 0)
		{
			for (int i = 0; i < pbwtQual.length; i += 3)
			{
				deStr[0] = Byte.toString((byte) ((((pbwtQual[i] + 128) & 0xff) >> 5) % 8));
				deStr[1] = Byte.toString((byte) (((pbwtQual[i] & 0xff) >> 2) % 8));
				deStr[2] = Byte.toString(
						(byte) ((((pbwtQual[i] & 0xff) % 4) * 2) + ((((pbwtQual[i + 1] + 128) & 0xff) >> 7) % 2)));
				deStr[3] = Byte.toString((byte) (((pbwtQual[i + 1] & 0xff) >> 4) % 8));
				deStr[4] = Byte.toString((byte) (((pbwtQual[i + 1] & 0xff) >> 1) % 8));
				deStr[5] = Byte.toString(
						(byte) ((((pbwtQual[i + 1] & 0xff) % 2) * 4) + ((((pbwtQual[i + 2] + 128) & 0xff) >> 6) % 4)));
				deStr[6] = Byte.toString((byte) (((pbwtQual[i + 2] & 0xff) >> 3) % 8));
				deStr[7] = Byte.toString((byte) ((pbwtQual[i + 2] & 0xff) % 8));
//				System.out.println();

				for (int j = 0; j < deStr.length; j++)
				{
					dexQual.add(deStr[j]);
					dexQual2.add(TranforOr(deStr[j]));
					// System.out.print(" " + deStr[j]);
				}

			}
		}

		else
		{
			// 需要在循环终止条件上减去3，因为最后一个不一定正合适
			for (int i = 0; i < pbwtQual.length - 3; i += 3)
			{
				deStr[0] = Byte.toString((byte) ((((pbwtQual[i] + 128) & 0xff) >> 5) % 8));
				deStr[1] = Byte.toString((byte) (((pbwtQual[i] & 0xff) >> 2) % 8));
				deStr[2] = Byte.toString(
						(byte) ((((pbwtQual[i] & 0xff) % 4) * 2) + ((((pbwtQual[i + 1] + 128) & 0xff) >> 7) % 2)));
				deStr[3] = Byte.toString((byte) (((pbwtQual[i + 1] & 0xff) >> 4) % 8));
				deStr[4] = Byte.toString((byte) (((pbwtQual[i + 1] & 0xff) >> 1) % 8));
				deStr[5] = Byte.toString(
						(byte) ((((pbwtQual[i + 1] & 0xff) % 2) * 4) + ((((pbwtQual[i + 2] + 128) & 0xff) >> 6) % 4)));
				deStr[6] = Byte.toString((byte) (((pbwtQual[i + 2] & 0xff) >> 3) % 8));
				deStr[7] = Byte.toString((byte) ((pbwtQual[i + 2] & 0xff) % 8));

				for (int j = 0; j < deStr.length; j++)
				{
					dexQual.add(deStr[j]);
					dexQual2.add(TranforOr(deStr[j]));
					// System.out.print(" " + deStr[j]);
				}
//				System.out.println();

			}
			// 多出一个字节的情况，策略就是全部还原，可能会多出来几个，但是最后拼接的时候就没了
			if (pbwtQual.length % 3 == 1)
			{
				deStr2[0] = Byte.toString((byte) ((((pbwtQual[pbwtQual.length - 1] + 128) & 0xff) >> 5) % 8));
				deStr2[1] = Byte.toString((byte) (((pbwtQual[pbwtQual.length - 1] & 0xff) >> 2) % 8));
				dexQual.add(deStr2[0]);
				dexQual2.add(TranforOr(deStr2[0]));
				dexQual.add(deStr2[1]);
				dexQual2.add(TranforOr(deStr2[1]));
				// System.out.print(" " + deStr2[0] + " " + deStr2[1] );
			}
			// 多出两个字节的情况，全部还原
			if (pbwtQual.length % 3 == 2)
			{
				deStr3[0] = Byte.toString((byte) ((((pbwtQual[pbwtQual.length - 2] + 128) & 0xff) >> 5) % 8));
				deStr3[1] = Byte.toString((byte) (((pbwtQual[pbwtQual.length - 2] & 0xff) >> 2) % 8));
				deStr3[2] = Byte.toString((byte) ((((pbwtQual[pbwtQual.length - 2] & 0xff) % 4) * 2)
						+ ((((pbwtQual[pbwtQual.length - 1] + 128) & 0xff) >> 7) % 2)));
				deStr3[3] = Byte.toString((byte) (((pbwtQual[pbwtQual.length - 1] & 0xff) >> 4) % 8));
				deStr3[4] = Byte.toString((byte) (((pbwtQual[pbwtQual.length - 1] & 0xff) >> 1) % 8));
				for (int j = 0; j < 5; j++)
				{
					dexQual.add(deStr3[j]);
					dexQual2.add(TranforOr(deStr3[j]));
					// System.out.print(" " + deStr3[j]);
				}
			}
		}
		System.out.println("The decompression of pbwtQual is OK");
		return dexQual2;

	}

	// 只去解压异常值，不采用游程编码的方式
	private ArrayList<String> deEncodeExceptionQual(byte[] exQual)
	{
		// 解压策略，将exqual.size（）/3 大小的正常解压,余下的分成7种情况解压
		// ArrayList<String> dexQual = new ArrayList<>();
		ArrayList<String> dexQual2 = new ArrayList<>();
		String[] deStr = new String[8];
		String[] deStr2 = new String[3];
		String[] deStr3 = new String[6];
		// 如果压缩那边正好没有多余，那么就是3B还原成8B
		if (exQual.length % 3 == 0)
		{
			for (int i = 0; i < exQual.length; i += 3)
			{
				deStr[0] = Byte.toString((byte) ((((exQual[i] + 128) & 0xff) >> 5) % 8));
				deStr[1] = Byte.toString((byte) (((exQual[i] & 0xff) >> 2) % 8));
				deStr[2] = Byte.toString(
						(byte) ((((exQual[i] & 0xff) % 4) * 2) + ((((exQual[i + 1] + 128) & 0xff) >> 7) % 2)));
				deStr[3] = Byte.toString((byte) (((exQual[i + 1] & 0xff) >> 4) % 8));
				deStr[4] = Byte.toString((byte) (((exQual[i + 1] & 0xff) >> 1) % 8));
				deStr[5] = Byte.toString(
						(byte) ((((exQual[i + 1] & 0xff) % 2) * 4) + ((((exQual[i + 2] + 128) & 0xff) >> 6) % 4)));
				deStr[6] = Byte.toString((byte) (((exQual[i + 2] & 0xff) >> 3) % 8));
				deStr[7] = Byte.toString((byte) ((exQual[i + 2] & 0xff) % 8));
//				System.out.println();

				for (int j = 0; j < deStr.length; j++)
				{
					// dexQual.add(deStr[j]);
					dexQual2.add(TranforOr(deStr[j]));
					// System.out.print(" " + deStr[j]);
				}

			}
		} else
		{
			// 需要在循环终止条件上减去3，因为最后一个不一定正合适
			for (int i = 0; i < exQual.length - 3; i += 3)
			{
				deStr[0] = Byte.toString((byte) ((((exQual[i] + 128) & 0xff) >> 5) % 8));
				deStr[1] = Byte.toString((byte) (((exQual[i] & 0xff) >> 2) % 8));
				deStr[2] = Byte.toString(
						(byte) ((((exQual[i] & 0xff) % 4) * 2) + ((((exQual[i + 1] + 128) & 0xff) >> 7) % 2)));
				deStr[3] = Byte.toString((byte) (((exQual[i + 1] & 0xff) >> 4) % 8));
				deStr[4] = Byte.toString((byte) (((exQual[i + 1] & 0xff) >> 1) % 8));
				deStr[5] = Byte.toString(
						(byte) ((((exQual[i + 1] & 0xff) % 2) * 4) + ((((exQual[i + 2] + 128) & 0xff) >> 6) % 4)));
				deStr[6] = Byte.toString((byte) (((exQual[i + 2] & 0xff) >> 3) % 8));
				deStr[7] = Byte.toString((byte) ((exQual[i + 2] & 0xff) % 8));

				for (int j = 0; j < deStr.length; j++)
				{
					// dexQual.add(deStr[j]);
					dexQual2.add(TranforOr(deStr[j]));
					// System.out.print(" " + deStr[j]);
				}
//				System.out.println();

			}
			// 多出一个字节的情况，策略就是全部还原，可能会多出来几个，但是最后拼接的时候就没了
			if (exQual.length % 3 == 1)
			{
				deStr2[0] = Byte.toString((byte) ((((exQual[exQual.length - 1] + 128) & 0xff) >> 5) % 8));
				deStr2[1] = Byte.toString((byte) (((exQual[exQual.length - 1] & 0xff) >> 2) % 8));
				// dexQual.add(deStr2[0]);
				dexQual2.add(TranforOr(deStr2[0]));
				// dexQual.add(deStr2[1]);
				dexQual2.add(TranforOr(deStr2[1]));
				// System.out.print(" " + deStr2[0] + " " + deStr2[1] );
			}
			// 多出两个字节的情况，全部还原
			if (exQual.length % 3 == 2)
			{
				deStr3[0] = Byte.toString((byte) ((((exQual[exQual.length - 2] + 128) & 0xff) >> 5) % 8));
				deStr3[1] = Byte.toString((byte) (((exQual[exQual.length - 2] & 0xff) >> 2) % 8));
				deStr3[2] = Byte.toString((byte) ((((exQual[exQual.length - 2] & 0xff) % 4) * 2)
						+ ((((exQual[exQual.length - 1] + 128) & 0xff) >> 7) % 2)));
				deStr3[3] = Byte.toString((byte) (((exQual[exQual.length - 1] & 0xff) >> 4) % 8));
				deStr3[4] = Byte.toString((byte) (((exQual[exQual.length - 1] & 0xff) >> 1) % 8));
				for (int j = 0; j < 5; j++)
				{
					// dexQual.add(deStr3[j]);
					dexQual2.add(TranforOr(deStr3[j]));
					// System.out.print(" " + deStr3[j]);
				}
			}
		}
		System.out.println("The decompression of exQual is OK");
		return dexQual2;
	}

	/**
	 * 最大化压缩的解压
	 * 
	 * @param exQual
	 * @return
	 */
	private ArrayList<Character> deEncodeExceptionQual2(byte[] exQual)
	{
		// 解压策略，将exqual.size（）/3 大小的正常解压,余下的分成7种情况解压
		// ArrayList<String> dexQual = new ArrayList<>();
		ArrayList<Character> dexQual2 = new ArrayList<>();
		char[] deStr = new char[8];
		char[] deStr2 = new char[3];
		char[] deStr3 = new char[6];
		// 如果压缩那边正好没有多余，那么就是3B还原成8B
		if (exQual.length % 3 == 0)
		{
			for (int i = 0; i < exQual.length; i += 3)
			{
				deStr[0] = (char) ((byte) (((exQual[i] & 0xff) >> 5) % 8) + 48);
				dexQual2.add(TranforOr(deStr[0]));
				deStr[1] = (char) ((byte) (((exQual[i] & 0xff) >> 2) % 8) + 48);
				dexQual2.add(TranforOr(deStr[1]));
				deStr[2] = (char) ((byte) ((((exQual[i] & 0xff) % 4) * 2) + (((exQual[i + 1] & 0xff) >> 7) % 2)) + 48);
				dexQual2.add(TranforOr(deStr[2]));
				deStr[3] = (char) ((byte) (((exQual[i + 1] & 0xff) >> 4) % 8) + 48);
				dexQual2.add(TranforOr(deStr[3]));
				deStr[4] = (char) ((byte) (((exQual[i + 1] & 0xff) >> 1) % 8) + 48);
				dexQual2.add(TranforOr(deStr[4]));
				deStr[5] = (char) ((byte) ((((exQual[i + 1] & 0xff) % 2) * 4) + (((exQual[i + 2] & 0xff) >> 6) % 4))
						+ 48);
				dexQual2.add(TranforOr(deStr[5]));
				deStr[6] = (char) ((byte) (((exQual[i + 2] & 0xff) >> 3) % 8) + 48);
				dexQual2.add(TranforOr(deStr[6]));
				deStr[7] = (char) ((byte) ((exQual[i + 2] & 0xff) % 8) + 48);
				dexQual2.add(TranforOr(deStr[7]));
//				System.out.println();

			}
		} else
		{
			// 需要在循环终止条件上减去3，因为最后一个不一定正合适
			for (int i = 0; i < exQual.length - 3; i += 3)
			{
				deStr[0] = (char) ((byte) (((exQual[i] & 0xff) >> 5) % 8) + 48);
				dexQual2.add(TranforOr(deStr[0]));
				deStr[1] = (char) ((byte) (((exQual[i] & 0xff) >> 2) % 8) + 48);
				dexQual2.add(TranforOr(deStr[1]));
				deStr[2] = (char) ((byte) ((((exQual[i] & 0xff) % 4) * 2) + (((exQual[i + 1] & 0xff) >> 7) % 2)) + 48);
				dexQual2.add(TranforOr(deStr[2]));
				deStr[3] = (char) ((byte) (((exQual[i + 1] & 0xff) >> 4) % 8) + 48);
				dexQual2.add(TranforOr(deStr[3]));
				deStr[4] = (char) ((byte) (((exQual[i + 1] & 0xff) >> 1) % 8) + 48);
				dexQual2.add(TranforOr(deStr[4]));
				deStr[5] = (char) ((byte) ((((exQual[i + 1] & 0xff) % 2) * 4) + (((exQual[i + 2] & 0xff) >> 6) % 4))
						+ 48);
				dexQual2.add(TranforOr(deStr[5]));
				deStr[6] = (char) ((byte) (((exQual[i + 2] & 0xff) >> 3) % 8) + 48);
				dexQual2.add(TranforOr(deStr[6]));
				deStr[7] = (char) ((byte) ((exQual[i + 2] & 0xff) % 8) + 48);
				dexQual2.add(TranforOr(deStr[7]));
//				System.out.println();

			}
			// 多出一个字节的情况，策略就是全部还原，可能会多出来几个，但是最后拼接的时候就没了
			if (exQual.length % 3 == 1)
			{
				deStr2[0] = (char) ((byte) (((exQual[exQual.length - 1] & 0xff) >> 5) % 8) + 48);
				deStr2[1] = (char) ((byte) (((exQual[exQual.length - 1] & 0xff) >> 2) % 8) + 48);
				dexQual2.add(TranforOr(deStr2[0]));
				dexQual2.add(TranforOr(deStr2[1]));
			}
			// 多出两个字节的情况，全部还原
			if (exQual.length % 3 == 2)
			{
				deStr3[0] = (char) ((byte) (((exQual[exQual.length - 2] & 0xff) >> 5) % 8) + 48);
				dexQual2.add(TranforOr(deStr3[0]));
				deStr3[1] = (char) ((byte) (((exQual[exQual.length - 2] & 0xff) >> 2) % 8) + 48);
				dexQual2.add(TranforOr(deStr3[1]));
				deStr3[2] = (char) ((byte) ((((exQual[exQual.length - 2] & 0xff) % 4) * 2)
						+ (((exQual[exQual.length - 1] & 0xff) >> 7) % 2)) + 48);
				dexQual2.add(TranforOr(deStr3[2]));
				deStr3[3] = (char) ((byte) (((exQual[exQual.length - 1] & 0xff) >> 4) % 8) + 48);
				dexQual2.add(TranforOr(deStr3[3]));
				deStr3[4] = (char) ((byte) (((exQual[exQual.length - 1] & 0xff) >> 1) % 8) + 48);
				dexQual2.add(TranforOr(deStr3[4]));

			}
		}
		System.out.println("The decompression of exQual is OK");
		return dexQual2;
	}

	public static boolean dexIsTrue(ArrayList<ArrayList<String>> exceptionList, String[] dexBy)
	{
		int index = 0;
		for (ArrayList<String> exbase : exceptionList)
		{
			if (index >= dexBy.length)
			{
				System.out.println("The compression of ex is error!");
				return false;
			}
			for (int i = 0; i < exbase.size(); i++)
			{
				if (!exbase.get(i).equals(dexBy[index]))
				{
					System.out.println("The compression of ex is error!");
					return false;
				}
				index++;
			}
		}
		if (index != dexBy.length)
		{
			System.out.println("The compression of ex is error!");
			return false;
		}
		System.out.println("The compression of ex is right!");
		return true;
	}

	private static boolean dexIsTrue2(ArrayList<String> exBackUp2, List<String> dexBy)
	{
		int lengthExOne = exBackUp2.size();
		int lengthExTwo = dexBy.size();
//		if (lengthExOne != lengthExTwo)
//		{
//			System.out.println("########### The process of exList is Wrong（1） #############");
////			return false;
//		} else
		{
			for (int i = 0; i < lengthExTwo; i++)
			{
				if (!exBackUp.get(i).equals(dexBy.get(i)))
				{
					System.out.println("########### The process of exList is Wrong（2) #############"+i+" ");
					System.out.println("origin:"+exBackUp2.get(i)+"\t decompress:"+dexBy.get(i));
//					return false;
				}
			}
		}
		System.out.println("The process of exList is right!");
		return true;
	}

	public static void dexIsTrue(ArrayList<ArrayList<String>> exceptionList, List<String> dexBy)
	{
		int index = 0;
		for (ArrayList<String> exbase : exceptionList)
		{
			if (index >= dexBy.size())
			{
				System.out.println("The compression of ex is error!");
			}
			for (int i = 0; i < exbase.size(); i++)
			{
				if (!exbase.get(i).equals(dexBy.get(index)))
				{
					System.out.println("The compression of ex is error!");
				}
				index++;
			}
		}
		if (index != dexBy.size())
		{
			System.out.println("The compression of ex is error!");
		}
		System.out.println("The compression of ex is right!");
	}

	/**
	 * 针对索引压缩的
	 * 插入压缩
	 * @param array
	 * @return
	 */
	public static ArrayList<ArrayList<String>> deEncodeExceptionList2(byte[] array)
	{
		String byte2EncodeResult = "";
		String tempbyte2 = "";
		int num = 0;
		for (int i = 0; i < array.length - 2; i++)
		{
			if (array[i] == ' ')
			{
				break;
			}
			tempbyte2 = String.format("%8s", Integer.toBinaryString(array[i] & 0xFF)).replace(' ', '0');

			num++;
//			if (num % 2 == 1)
//			{
//				System.out.print(tempbyte2);
//			} else
//			{
//				System.out.println(tempbyte2);
//			}
			byte2EncodeResult += tempbyte2;
		}

		String strTail = String.format("%8s", Integer.toBinaryString(array[array.length - 2] & 0xFF)).replace(' ', '0')
				+ String.format("%8s", Integer.toBinaryString(array[array.length - 1] & 0xFF)).replace(' ', '0');
		System.out.println(strTail);
		byte2EncodeResult += strTail;
		Huffman2 huffman = new Huffman2();
		String rateText = "AAACCCTTTGGG||DN";
		huffman.handleRate(rateText);

		String decodedResult = huffman.decodeText(byte2EncodeResult); // bit还原为String
		String decodeResult2 = decodedResult.substring(0, decodedResult.lastIndexOf("|")); // 去掉最后的结尾
		// decodeResult2+="|"; // 这里可能会影响速度，之后再改

		ArrayList<ArrayList<String>> deResult = new ArrayList<ArrayList<String>>();
		String[] deTemp = decodeResult2.split("\\|",-1);
		// 点隔开这里明天继续,注意空的问题
		ArrayList<String> templist = new ArrayList<String>();
		for (String str : deTemp)
		{
			if (!str.isEmpty())
			{
				templist.add(str);
			}
			else
			{
				deResult.add(templist);
				templist = new ArrayList<String>();
			}
		}

		return deResult;
	}

	/**
	 * 针对最大化压缩的
	 * 
	 * @param array
	 * @return
	 */
	public static String[] deEncodeExceptionList(byte[] array)
	{
		// 实现解压缩
		ArrayList<String> dexRes = new ArrayList<String>();
		String byte2EncodeResult = "";
		StringBuffer byte2EncodeResult2 = new StringBuffer();
		String tempbyte2 = "";
		int num = 0;
		for (int i = 0; i < array.length - 2; i++)
		{
//			if (array[i] == ' ')
//			{
//				System.out.println("i of ex \t"+i);
//				break;
//			}
//			tempbyte2 = String.format("%8s", Integer.toBinaryString(array[i] & 0xFF)).replace(' ', '0');
			tempbyte2 = String.format("%8s", Integer.toBinaryString(array[i] & 0xFF));
			
			num++;
//			if (num % 2 == 1)
//			{
//				System.out.print(tempbyte2);
//			} else
//			{
//				System.out.println(tempbyte2);
//			}
//			byte2EncodeResult += tempbyte2;
			byte2EncodeResult2.append(tempbyte2);
		}
		// System.out.println();
		// System.out.println("num : "+num);
		// 针对最后一进行处理
		String strTail = String.format("%8s", Integer.toBinaryString(array[array.length - 2] & 0xFF))
				+ String.format("%8s", Integer.toBinaryString(array[array.length - 1] & 0xFF));
		System.out.println("strTail:"+strTail);
		// 这个地方问题严重，暂时不知道如何解决,这句话有什么用啊，
		// String[] res = strTail.split("000111");
		// byte2EncodeResult += res[0];
		// System.out.println("res[0]: "+res[0]);
//		byte2EncodeResult += strTail;
		byte2EncodeResult2.append(strTail);

		// decoding Text
		// 这部分需要优化一下，首先需要通过样本计算出每个碱基出现的概率，从而建立一个正确的huffman树，齐次解压缩和压缩只需要建立一次树就行。
		Huffman2 huffman = new Huffman2();
		String rateText = "AAACCCTTTGGG||DN";
		huffman.handleRate(rateText);

//		String decodedResult = huffman.decodeText(byte2EncodeResult);
		StringBuffer decodedRes = huffman.decodeText2(byte2EncodeResult2);
		String decodeRes2 = decodedRes.substring(0, decodedRes.lastIndexOf("|")) + '|';
		System.out.println(decodeRes2.charAt(decodeRes2.length()-1));
//		String decodeResult2 = decodedResult.substring(0, decodedResult.lastIndexOf("|"));

		// System.out.println(decodeResult2);

//		String[] dexResult = decodeResult2.split("\\|");
		String[] dexResult = decodeRes2.split("\\|");
//		char tempChar;
//		for(int i=0; i<decodeRes2.length();i++)
//		{
//			StringBuffer tempStr = new StringBuffer();
//			tempChar = decodeRes2.charAt(i);
//			if(tempChar != '|')
//			{
//				tempStr.append(tempChar);
//			}
//			else
//			{
//				dexRes.add(tempStr.toString());
//			}
//		}
		// System.out.print(decodedResult.replace("|", "\n"));
		System.out.println("testing the speed");
		return dexResult;
	}

	public static ArrayList<String> deEncodeException(byte[] array)
	{
		// 实现解压缩
		ArrayList<String> dexRes = new ArrayList<String>();
		String byte2EncodeResult = "";
		StringBuffer byte2EncodeResult2 = new StringBuffer();
		String tempbyte2 = "";
		for (int i = 0; i < array.length - 2; i++)
		{
			tempbyte2 = String.format("%8s", Integer.toBinaryString(array[i] & 0xFF));
			byte2EncodeResult2.append(tempbyte2);
		}

		// 针对最后一进行处理
		String strTail = String.format("%8s", Integer.toBinaryString(array[array.length - 2] & 0xFF))
				+ String.format("%8s", Integer.toBinaryString(array[array.length - 1] & 0xFF));
		System.out.println("strTail:"+strTail);
		byte2EncodeResult2.append(strTail);

		// decoding Text
		// 这部分需要优化一下，首先需要通过样本计算出每个碱基出现的概率，从而建立一个正确的huffman树，齐次解压缩和压缩只需要建立一次树就行。
		Huffman2 huffman = new Huffman2();
		String rateText = "AAACCCTTTGGG||DN";
		huffman.handleRate(rateText);

		StringBuffer decodedRes = huffman.decodeText2(byte2EncodeResult2);
		String decodeRes2 = decodedRes.substring(0, decodedRes.lastIndexOf("|")) + '|';
		char tempChar;
		for(int i=0; i<decodeRes2.length();i++)
		{
			StringBuffer tempStr = new StringBuffer();
			tempChar = decodeRes2.charAt(i);
			if(tempChar != '|')
			{
				tempStr.append(tempChar);
			}
			else
			{
				dexRes.add(tempStr.toString());
			}
		}
		// System.out.print(decodedResult.replace("|", "\n"));
		System.out.println("testing the speed");
		return dexRes;
	}
	
	// 传入的vE是一个0,1序列。第一步进行动态pbwt变化，第二部进行游程编码
	// 我们在进行reads编码的过程中同时也需要编码序列的起始位置（长度先看一下）
	// 我需要在这里重新写一下pbwt变化
	public CompressResult EncodePbwt(Test1 testOne)
	{
		// CompressResult allRes = new CompressResult();
		ReadPbwtResult pbwtres = new ReadPbwtResult();
		List<Integer> keys = new ArrayList<Integer>();
		List<Integer> values = new ArrayList<Integer>();
		Integer[] start = new Integer[testOne.readStart.length];
		Integer[] end = new Integer[testOne.readEnd.length];

		System.out.println("Original reads(Test.one.length):\t");
		// 这里是一次遍历 不知道之后这里是不是可以简化一下,在这里可以进行start的压缩过程。
		for (int i = 0; i < start.length; i++)
		{
			start[i] = testOne.readStart[i];
			end[i] = testOne.readEnd[i];
			PrintReads1(start[i], testOne.getListsPBWT().get(i));
			// System.out.println(testOne.getListsPBWT().get(i).toString());
		}
		// 在这里进行start的压缩，有什么用啊，把start信息在存放一遍，testOne中就有这个信息，需要在来一次吗
//		byte[] startPos = compressStartPos(testOne.readStart);
//		allRes.setStartResult(startPos);
		// 对每个read从头遍历到尾，把各个信息加入到struct中
		ArrayList<ReadStruct> readsList = new ArrayList<ReadStruct>();
		int index = 0; // 异常信息的索引
		for (int i = 0; i < testOne.listsPBWT.size(); i++)
		{
			ReadStruct struct = new ReadStruct();
			struct.setStartAlignment(start[i]);
			struct.setEndAlignment(end[i]);
			ArrayList<Integer> list = new ArrayList<Integer>();
			// ArrayList<String> qualist = new ArrayList<>();
			int fla = 0; // 变异监测位
			for (int j = 0; j < testOne.getListsPBWT().get(i).size(); j++)
			{
				Integer val = testOne.getListsPBWT().get(i).get(j);
				list.add(val);
				if (val == 1)
				{
					fla = 1;
				}
			}
			// 这里处理注意了readquality是全部的不单单是异常值，所以说在改写主函数的时候这里注意
			struct.setReads(list);
			struct.setReadQuality(testOne.getReadQual().get(i).toString());
			// struct.setReadQuality(testOne.getReadQual().get(i));
			if (fla == 1)
			{
				struct.setException(testOne.getExceptionList().get(index));
				struct.setExceptionQuality(testOne.getExceptionListQual().get(index));
				index++;
			} else
			{
				ArrayList<String> exnull = new ArrayList<>();
				ArrayList<String> exqnull = new ArrayList<>();
				struct.setException(exnull);
				struct.setExceptionQuality(exqnull);
			}
			readsList.add(struct);
		}
		// pbwt 变化
//		pbwtres = PBWTAlgo(readsList, start, end);
		// 这里改写为我们正确的排序算法
		
//		pbwtres = PBWTAlgo2(readsList, start, end);
		
//		局部解压缩算法压缩过程
		pbwtres = PBWTAlgo5(readsList, start, end);
		
		
		
//		ArrayList<ArrayList<Integer>> pbwtResult = pbwtres.getListsPBWT();

		// ArrayList<ArrayList<Integer>> repbwt = PBWTAlgoRe(pbwtResult, start,end); //
		// 之前需要其实和终止位置
		// ArrayList<ArrayList<Integer>> repbwt = PBWTAlgoRe(pbwtResult);

		// PbwtIsTrue(testOne.listsPBWT,repbwt);

		// 继续写二进制编码，这里有一个问题就是，还原的时候需要起始位置那需不需要长度。
		// 这里想法第一，用不用水平编码，我认为深度达到一定用垂直方向压缩效果才好，但是如果达不到效果肯定一般。
		// 第二点使用了pbwt变换之后数据更加集中了，然后是不是可以在此基础上在进行一步变换是数据更加几种呢
		// 最后的那个3，用处就是知道这里是结束了，但是问题是在这里必然停止了水平编码。我们是不是可以换一种方式就是结尾用7个1表示，因为如果达到一定数量不匹配就是
		// ss了，所以说用7个1表示，肯定是比之前好，不连接一样的，如果连上了效果更好。
		// 如果完全去了3，那么这样效果更好，但是再还原的时候就需要长度信息，不然就无法还原。
		// 起始位置信息也需要压缩，不然利用参考序列是无法进行还原的
//		byte[] value2by = binaryValues(values);

//		runLen(keys, values, pbwtResult);
		
		int minStart = testOne.readStart[0];
		byte[] startPos = new byte[4];
		compressLen(minStart, startPos);
		allRes.setStartResult(startPos);
		
		runLen(keys, values, pbwtres);
		byte[] kv = binaryKV(keys, values);
//		byte[] keyAndValue = (byte[]) ArrayUtils.addAll(StartPos, kv);
		// 首先是对value值进行压缩，范围在1B-3B，长度不超过128的为1B，超过128的为3B

		// 对于key值的压缩4个合成为一个
//		byte[] key2by = binaryKeys(keys);
		// 先key在value好一点，因为长度较短。这里有两个方式能够在解压缩的时候将keyAndValue分开。一个是记录key的长度，一个是加上间隔符号

//		byte[] keyAndValue = (byte[]) ArrayUtils.addAll(value2by, key2by);
//		System.out.println("keysAndValues size:\t" + keyAndValue.length);

//		int len = value2by.length;
//		byte[] len2 = new byte[4];
//		compressLen(len, len2);
		// int delen = deCompressLen(len2);
		// if(len == delen)
		// {
		// System.out.println("It's right(compress len)");
		// }
		// else
		// {
		// System.out.println("It's wrong(compress len)");
		// }
//		keyAndValue = (byte[]) ArrayUtils.addAll(len2, keyAndValue);

		// DecodePbwt(key2by, value2by);
		// 这里仅仅是2进制还原，并不带有pbwt还原。再去证明一下正确性
		// ArrayList<ArrayList<Integer>> rePBWT = DecodePBWT(keyAndValue);

		// PbwtIsTrue(pbwtResult,rePBWT);

		// 在这里进行余下的几个部分的压缩工作
		byte[] exBy = EncodeExceptionList2(pbwtres.getListsExcep2());
		// MainEncoding2.deEncodeExceptionList(exBy);
		// 需要改两处，一个是质量分数都全部认为是char，然后是正常质量分数一列是一个值
		byte[] exQual = EncodePbwtSingleQual(pbwtres.getListExQual2());
		byte[] pbwtQual = EncodePbwtSingleQual(pbwtres.getListsQual());
		// ArrayList<Character> qual = deEncodeQual2(pbwtQual);
		// byte[] pbwtQual = MainEncoding2.EncodePbwtQual(pbwtres.getListsQual());

		allRes.setExceptionResult(exBy);
		allRes.setExceptionQuaResult(exQual);
		allRes.setReadQuaReasult(pbwtQual);
		allRes.setReadsResult(kv);

		return allRes;
	}
	/**
	 * 局部压缩策略
	 * @param testOne
	 * @return
	 */
	public CompressResult EncodePbwt2(Test1 testOne)
	{
		// CompressResult allRes = new CompressResult();
		ReadPbwtResult pbwtres = new ReadPbwtResult();
		List<Integer> keys = new ArrayList<Integer>();
		List<Integer> values = new ArrayList<Integer>();
		Integer[] start = new Integer[testOne.readStart.length];
		Integer[] end = new Integer[testOne.readEnd.length];

		System.out.println("Original reads(Test.one.length):\t");
		// 这里是一次遍历 不知道之后这里是不是可以简化一下,在这里可以进行start的压缩过程。
		for (int i = 0; i < start.length; i++)
		{
			start[i] = testOne.readStart[i];
			end[i] = testOne.readEnd[i];
			PrintReads1(start[i], testOne.getListsPBWT().get(i));
			// System.out.println(testOne.getListsPBWT().get(i).toString());
		}
		// 在这里进行start的压缩，有什么用啊，把start信息在存放一遍，testOne中就有这个信息，需要在来一次吗
//		byte[] startPos = compressStartPos(testOne.readStart);
//		allRes.setStartResult(startPos);
		// 对每个read从头遍历到尾，把各个信息加入到struct中
		ArrayList<ReadStruct> readsList = new ArrayList<ReadStruct>();
		int index = 0; // 异常信息的索引
		for (int i = 0; i < testOne.listsPBWT.size(); i++)
		{
			ReadStruct struct = new ReadStruct();
			struct.setStartAlignment(start[i]);
			struct.setEndAlignment(end[i]);
			ArrayList<Integer> list = new ArrayList<Integer>();
			// ArrayList<String> qualist = new ArrayList<>();
			int fla = 0; // 变异监测位
			for (int j = 0; j < testOne.getListsPBWT().get(i).size(); j++)
			{
				Integer val = testOne.getListsPBWT().get(i).get(j);
				list.add(val);
				if (val == 1)
				{
					fla = 1;
				}
			}
			// 这里处理注意了readquality是全部的不单单是异常值，所以说在改写主函数的时候这里注意
			struct.setReads(list);
			struct.setReadQuality(testOne.getReadQual().get(i).toString());
			// struct.setReadQuality(testOne.getReadQual().get(i));
			if (fla == 1)
			{
				struct.setException(testOne.getExceptionList().get(index));
				struct.setExceptionQuality(testOne.getExceptionListQual().get(index));
				index++;
			} else
			{
				ArrayList<String> exnull = new ArrayList<>();
				ArrayList<String> exqnull = new ArrayList<>();
				struct.setException(exnull);
				struct.setExceptionQuality(exqnull);
			}
			readsList.add(struct);
		}
		// pbwt 变化
//		pbwtres = PBWTAlgo(readsList, start, end);
		// 这里改写为我们正确的排序算法
		
//		pbwtres = PBWTAlgo2(readsList, start, end);
		
//		局部解压缩算法压缩过程
		pbwtres = PBWTAlgo4(readsList, start, end);
			
		int minStart = testOne.readStart[0];
		byte[] startPos = new byte[4];
		compressLen(minStart, startPos);
		allRes.setStartResult(startPos);
		
		runLen(keys, values, pbwtres);
		byte[] kv = binaryKV(keys, values);

		// 在这里进行余下的几个部分的压缩工作
		byte[] exBy = EncodeExceptionList4(pbwtres.getListsExcep());
		// MainEncoding2.deEncodeExceptionList(exBy);
		// 需要改两处，一个是质量分数都全部认为是char，然后是正常质量分数一列是一个值
		byte[] exQual = EncodePbwtSingleQual(pbwtres.getListExQual2());
		byte[] pbwtQual = EncodePbwtSingleQual(pbwtres.getListsQual());
		// ArrayList<Character> qual = deEncodeQual2(pbwtQual);
		// byte[] pbwtQual = MainEncoding2.EncodePbwtQual(pbwtres.getListsQual());

		allRes.setExceptionResult(exBy);
		allRes.setExceptionQuaResult(exQual);
		allRes.setReadQuaReasult(pbwtQual);
		allRes.setReadsResult(kv);

		return allRes;
	}

	
	
	/**
	 * 针对真实数据的压缩算法
	 * 
	 * @param reads
	 * @return
	 */
	public CompressResult EncodePbwt(ReadsPreProcessResult reads)
	{
		// CompressResult allRes = new CompressResult();
		ReadPbwtResult pbwtres = new ReadPbwtResult();
		List<Integer> keys = new ArrayList<Integer>();
		List<Integer> values = new ArrayList<Integer>();
		int[] start = new int[reads.getReadsHorizon().size()];
		int[] end = new int[reads.getReadsHorizon().size()];

		System.out.println("Original reads(Test.one.length):\t");
		// 之后看一下这个循环是否可以并入下面的循环，我认为应该是可以
		for (int i = 0; i < reads.getReadsHorizon().size(); i++)
		{
			start[i] = reads.getReadsHorizon().get(i).getAlignmentStart();
			end[i] = reads.getReadsHorizon().get(i).getAlignmentEnd();
			// System.out.println(start[i]+"\t" + end[i]);
		}
		System.out.println("The difference is：" + (end[end.length-1]-start[0]));
		// 在这里进行start的压缩，有什么用啊，把start信息在存放一遍，testOne中就有这个信息，需要在来一次吗
		byte[] startPos = compressStartPos(start);
		allRes.setStartResult(startPos);
		// 之前用于检测解压缩是否正确
		// int[] reStart = deStartPos(startPos);
		// for(int i=0; i<reStart.length&&i<start.length;i++)
		// {
		// System.out.println(start[i]+","+reStart[i]);
		// }
		// 对每个read从头遍历到尾，把各个信息加入到struct中
		// 这里专门写一个适配的函数,来把过程改变一下,以便适配需要的结果
		ArrayList<ReadStruct> readsList = new ArrayList<ReadStruct>();
		// int index = 0; // 异常信息的索引
		for (int i = 0; i < reads.getReadsInfo().size(); i++)
		{
			ReadStruct struct = new ReadStruct();
			struct.setStartAlignment(start[i]);
			struct.setEndAlignment(end[i]);
			// start[i] = reads.getReadsHorizon().get(i).getAlignmentStart();
			// end[i] = reads.getReadsHorizon().get(i).getAlignmentEnd();
			ArrayList<Integer> list = new ArrayList<Integer>();
			// ArrayList<String> qualist = new ArrayList<>();
			int fla = 0; // 变异监测位
			for (int j = 0; j < reads.getReadsInfo().get(i).getReads().size(); j++)
			{
				READSYMBOL val = reads.getReadsInfo().get(i).getReads().get(j);
				int numVal = (val == READSYMBOL.E ? 0 : (val == READSYMBOL.END ? 3 : 1));
				list.add(numVal);// liyang:相同0，不同1，结束3
				if (numVal == 1)
				{
					fla = 1;
				}
			}

			// 这里处理注意了readquality是全部的不单单是异常值，所以说在改写主函数的时候这里注意
			struct.setReads(list);
			struct.setReadQuality(reads.getReadsInfo().get(i).getReadQuality());
			// struct.setReadQuality(testOne.getReadQual().get(i));
			if (fla == 1)
			{
				struct.setException(reads.getReadsInfo().get(i).getExceptionInfo());
				struct.setExceptionQuality(reads.getReadsInfo().get(i).getExceptionQuality());
				// index++;
			} else
			{
				ArrayList<String> exnull = new ArrayList<>();
				ArrayList<String> exqnull = new ArrayList<>();
				struct.setException(exnull);
				struct.setExceptionQuality(exqnull);
			}
			readsList.add(struct);
		}
		ReadsList = readsList;
		// byte[] startPos = compressStartPos(start);
		// allRes.setStartResult(startPos);
		// pbwt 变化
		
//		Dpbwt dpbwt = new Dpbwt();
//		pbwtres = dpbwt.PBWTAlgo(readsList, start, end);
		
		pbwtres = PBWTAlgo(readsList, start, end);
		ArrayList<ArrayList<Integer>> pbwtResult = pbwtres.getListsPBWT();

		// ArrayList<ArrayList<Integer>> repbwt = PBWTAlgoRe(pbwtResult, start,end); //
		// 之前需要其实和终止位置
		// ArrayList<ArrayList<Integer>> repbwt = PBWTAlgoRe(pbwtResult);

		// PbwtIsTrue(testOne.listsPBWT,repbwt);

		// 继续写二进制编码，这里有一个问题就是，还原的时候需要起始位置那需不需要长度。
		// 这里想法第一，用不用水平编码，我认为深度达到一定用垂直方向压缩效果才好，但是如果达不到效果肯定一般。
		// 第二点使用了pbwt变换之后数据更加集中了，然后是不是可以在此基础上在进行一步变换是数据更加几种呢
		// 最后的那个3，用处就是知道这里是结束了，但是问题是在这里必然停止了水平编码。我们是不是可以换一种方式就是结尾用7个1表示，因为如果达到一定数量不匹配就是
		// ss了，所以说用7个1表示，肯定是比之前好，不连接一样的，如果连上了效果更好。
		// 如果完全去了3，那么这样效果更好，但是再还原的时候就需要长度信息，不然就无法还原。
		// 起始位置信息也需要压缩，不然利用参考序列是无法进行还原的

		runLen(keys, values, pbwtResult);

		// 首先是对value值进行压缩，范围在1B-3B，长度不超过128的为1B，超过128的为3B
		byte[] value2by = binaryValues(values);

		// 对于key值的压缩4个合成为一个
		byte[] key2by = binaryKeys(keys);
		// 先key在value好一点，因为长度较短。这里有两个方式能够在解压缩的时候将keyAndValue分开。一个是记录key的长度，一个是加上间隔符号

		byte[] keyAndValue = (byte[]) ArrayUtils.addAll(value2by, key2by);
		System.out.println("keysAndValues size:\t" + keyAndValue.length);

		int len = value2by.length;
		byte[] len2 = new byte[4];
		compressLen(len, len2);
		// int delen = deCompressLen(len2);
		// if(len == delen)
		// {
		// System.out.println("It's right(compress len)");
		// }
		// else
		// {
		// System.out.println("It's wrong(compress len)");
		// }
		keyAndValue = (byte[]) ArrayUtils.addAll(len2, keyAndValue);

		// DecodePbwt(key2by, value2by);
		// 这里仅仅是2进制还原，并不带有pbwt还原。再去证明一下正确性
		// ArrayList<ArrayList<Integer>> rePBWT = DecodePBWT(keyAndValue);

		// PbwtIsTrue(pbwtResult,rePBWT);

		// 在这里进行余下的几个部分的压缩工作
		byte[] exBy = EncodeExceptionList(pbwtres.getListsExcep());
		// MainEncoding2.deEncodeExceptionList(exBy);
		// 需要改两处，一个是质量分数都全部认为是char，然后是正常质量分数一列是一个值
		byte[] exQual = EncodeExceptionQual(pbwtres.getListExQual());
		byte[] pbwtQual = EncodePbwtSingleQual(pbwtres.getListsQual());
		// ArrayList<Character> qual = deEncodeQual2(pbwtQual);
		// byte[] pbwtQual = MainEncoding2.EncodePbwtQual(pbwtres.getListsQual());

		allRes.setExceptionResult(exBy);
		allRes.setExceptionQuaResult(exQual);
		allRes.setReadQuaReasult(pbwtQual);
		allRes.setReadsResult(keyAndValue);

		return allRes;
	}

	private void PrintReads1(Integer integer, ArrayList<Integer> arrayList)
	{
		for (int i = 1; i < integer + arrayList.size(); i++)
		{
			if (i < integer)
			{
				System.out.print("  ");
			} else
			{
				System.out.print(arrayList.get(i - integer) + " ");
			}
		}
		System.out.println();

	}

	private byte[] binaryKeys(List<Integer> keys)
	{
		ArrayList<Byte> bKeys = new ArrayList<Byte>();
		keys.add(2);
		if (keys.size() % 4 != 0)
		{ // liyang:这里我们用4的原因是首先2bit存储一个key，然后是2个key整合为一个
			int num = (keys.size() / 4 + 1) * 4 - keys.size(); // liyang:这里所做的事情就是检测一下我到底需要加上几个2
			while (num-- > 0)
			{
				keys.add(2);
			}
		} // liyang:当走出这个循环的时候表明已经可以被4整除了
		for (int i = 0; i < keys.size() - 3; i += 4)
		{ // liyang:减三的原因是4个为一个整体，所以说只要检测到第一个就可以，后面的没有必要了
			int key = keys.get(i) * 64 + keys.get(i + 1) * 16 + keys.get(i + 2) * 4 + keys.get(i + 3) - 128;
			bKeys.add((byte) (key));
		}
		ByteBuffer keyBytes = ByteBuffer.allocate(bKeys.size());
		for (byte by : bKeys)
		{
			keyBytes.put(by);
		}
		byte[] key2by = keyBytes.array();
		System.out.println("keys size:\t" + key2by.length);
		return key2by;
	}

	private byte[] binaryValues(List<Integer> values)
	{
		ArrayList<Byte> bVals = new ArrayList<Byte>();
		for (int i = 0; i < values.size(); i++)
		{
			int val = values.get(i) - 128;
			if (val >= 127)
			{ // liyang:这里是处理超过128大小的长度的
				// 变成3部分呢，第一部分11111111作为指示，第二、第三byte则是真正的值拼接在一起
				bVals.add((byte) (-1));
				bVals.add((byte) ((val >> 8) & 0xff));
				bVals.add((byte) (val & 0xff));
			} else
			{
				bVals.add((byte) (val));
			}
		}
		ByteBuffer bytes = ByteBuffer.allocate(bVals.size());
		for (byte by : bVals)
		{
			bytes.put(by);
		}
		byte[] value2by = bytes.array();
		System.out.println("values size:\t" + value2by.length);
		return value2by;
	}
	/**
	 * 将keys和values进行合并压缩
	 * @param values
	 * @param keys
	 * @return
	 */
	private byte[] binaryKV(List<Integer> keys ,List<Integer> values)
	{
		List<Byte> keyValue = new ArrayList<>();
		int tempKey = -1;
		int tempValue = -1;
		int tempByte = -1;
		int lastkey = -1;
		for (int i = 0; i < values.size(); i++)
		{
			tempKey = keys.get(i);
			tempValue = values.get(i);
			// 当key等于2的时候处理比较繁琐，需要将函数再加上额外附加信息
			if (tempKey == 2)
			{
				tempByte = tempKey*64 + tempValue*16 - 128;
				// 接下来的所有操作都是关于插入信息的操作，前面占用了
				
//				if (tempValue == 1)
//				{
//					if ( lastkey == 2)
//					{
//						// 此时表明该序列为空，只有2
//						
//					}
//					else
//					{
//						// 此时表明该序列不为空，为0,1混合
//						
//					}
//				}
//				else if (tempValue == 2) {
//					
//				}
//				else
//				{
//					
//				}
				keyValue.add((byte)(tempByte & 0xff));
			}
			else
			{
				// 当value大于2^5-1，需要设置1并且额外增加一个byte
				if (tempValue > 31)
				{
					tempByte = tempKey*64 + 1*32 + tempValue/256 - 128;
					keyValue.add((byte) (tempByte & 0xff));
					tempByte = tempValue % 256 - 128;
					keyValue.add((byte) (tempByte & 0xff));
				}
				// 当value不大于31时，压缩为一个byte，并将标志位设置为0
				else 
				{
					// 这里先不去减128试试看，
					tempByte = tempKey*64 + 0*32 + tempValue - 128;
					keyValue.add((byte) (tempByte & 0xff));
				}
			}
			lastkey = tempKey;
		}
		ByteBuffer bytes = ByteBuffer.allocate(keyValue.size());
		for (byte by : keyValue)
		{
			bytes.put(by);
		}
		byte[] byteKV= bytes.array();
		return byteKV;
	}
	
	private void DebinaryKV(byte[] byteKV)
	{
		int tempByte = 0;
		int tempByte2 = 0;
		int value = -1;
		int key = -1;
		int lenFlag = -1;
		for(int i =0; i<byteKV.length; i++)
		{
			tempByte = byteKV[i] + 128;
			key = (tempByte >> 6)%4;
//			value = tempByte%32;
			if (key == 2)
			{
				value = (tempByte >> 4)%4;
//				lenFlag = (tempByte >> 3)%2;
//				if (lenFlag == 1)
//				{
//					i++;
//					tempByte2 = byteKV[i] + 128;
//				}
//				else
//				{
//					
//				}
			}
			// 当key为0,1,3时候都有可能长度超过31
			else
			{
				lenFlag = (tempByte >> 5)%2;
				if (lenFlag == 1)
				{
					i++;
					tempByte2 = byteKV[i] + 128;
					value = tempByte%32*256 + tempByte2; // 这里稍有问题，暂时这样
				}
				else 
				{
					value = tempByte%32;
				}
			}
		}
	}
	
	private void runLen(List<Integer> keys, List<Integer> values, ArrayList<ArrayList<Integer>> pbwtResult)
	{
		int countZero = 0, countOne = 0, countThree = 0;
		int preVal = -1;
		int curVal = -1;

		for (ArrayList<Integer> listPbwt : pbwtResult)
		{
			// 当一列并无元素时候，直接加入占位符号2就行
			if (listPbwt.isEmpty())
			{
				keys.add(2);
				values.add(1);
			}else {
				countZero = 0;
				countOne = 0;
				countThree = 0;
				for (int i = 0; i < listPbwt.size(); i++)
				{ // liyang:纵向进行
					curVal = listPbwt.get(i);
					if (i != 0 && curVal != preVal)
					{ // 把i为0的起始条件排除掉，因为其实位置之前没有preVal
						if (preVal == 0)
						{
							keys.add(0);
							values.add(countZero);
						} else if (preVal == 1)
						{
							keys.add(1);
							values.add(countOne);
						} else
						{
							keys.add(3);
							values.add(countThree);
						}
						// 计数器清零
						countZero = 0;
						countOne = 0;
						countThree = 0;

					}

					preVal = curVal; // liyang:因为是起始位置，这样就会默认计数为1
					if (curVal == 0)
					{
						countZero++;
					} else if (curVal == 1)
					{
						countOne++;
					}else if (curVal == 3)
					{
						countThree++;
					} else
					{
						System.out.println("readsRL count Error....");
					}
					if (i == listPbwt.size() - 1)
					{ // liyang：处理最后一个元素，同时也是退出这里垂直编码
						// 处理到最后一个元素了
						if (curVal == 0)
						{
							keys.add(0);
							values.add(countZero);
						} else if (curVal == 1)
						{
							keys.add(1);
							values.add(countOne);
						} else
						{
							keys.add(3);
							values.add(countThree);
						}
						// 同时加入结束的分隔符
						// 暂作为间隔符使用,在这里不会有2出现，用2来表示单条listpbwt的终止
						// 需要加上这个2吗，2作为一条listpbwt的终止
						keys.add(2);
						values.add(1);
					}
				}
				
			}

		}
	}

	/**
	 * 局部策略
	 * @param keys
	 * @param values
	 * @param pbwtres
	 */
	private void runLen(List<Integer> keys, List<Integer> values, ReadPbwtResult pbwtres)
	{
		int countZero = 0, countOne = 0, countThree = 0;
		int preVal = -1;
		int curVal = -1;
		boolean oneFlag = false;
		boolean zeroFlag = false;

		for (ArrayList<Integer> listPbwt : pbwtres.getListsPBWT())
		{
			// 当一列并无元素时候，直接加入占位符号2就行
			if (listPbwt.isEmpty())
			{
				keys.add(2);
				values.add(1);
			}
			else 
			{
				countZero = 0;
				countOne = 0;
				countThree = 0;
				for (int i = 0; i < listPbwt.size(); i++)
				{ // liyang:纵向进行
					curVal = listPbwt.get(i);
					if (i != 0 && curVal != preVal)
					{ // 把i为0的起始条件排除掉，因为其实位置之前没有preVal
						if (preVal == 0)
						{
							keys.add(0);
							values.add(countZero);
						} else if (preVal == 1)
						{
							keys.add(1);
							values.add(countOne);
						} else
						{
							keys.add(3);
							values.add(countThree);
						}
						// 计数器清零
						countZero = 0;
						countOne = 0;
						countThree = 0;

					}

					preVal = curVal; // liyang:因为是起始位置，这样就会默认计数为1
					if (curVal == 0)
					{
						zeroFlag = true;
						countZero++;
					} else if (curVal == 1)
					{
						oneFlag = true;
						countOne++;
					}else if (curVal == 3)
					{
						countThree++;
					} else
					{
						System.out.println("readsRL count Error....");
					}
					if (i == listPbwt.size() - 1)
					{ // liyang：处理最后一个元素，同时也是退出这里垂直编码
						// 处理到最后一个元素了
						if (curVal == 0)
						{
							keys.add(0);
							values.add(countZero);
						} else if (curVal == 1)
						{
							keys.add(1);
							values.add(countOne);
						} else
						{
							keys.add(3);
							values.add(countThree);
						}
						// 同时加入结束的分隔符
						// 暂作为间隔符使用,在这里不会有2出现，用2来表示单条listpbwt的终止
						// 需要加上这个2吗，2作为一条listpbwt的终止
						if (!zeroFlag&&oneFlag)
						{
							keys.add(2);
							values.add(3);
						}
						else if (zeroFlag&&!oneFlag) 
						{
							keys.add(2);
							values.add(2);
						}
						else 
						{	// 包含了全是3和0,1混合的情况
							keys.add(2);
							values.add(1);
						}
					}
				}
			}
			oneFlag = false;
			zeroFlag = false;
		}
	}

	
	private static void compressLen(int len, byte[] len2)
	{
		if (len < Math.pow(2, 8))
		{
			len2[0] = (byte) 0;
			len2[1] = (byte) 0;
			len2[2] = (byte) 0;
			len2[3] = (byte) (len & 0xff);
		} else if (len < Math.pow(2, 16))
		{
			len2[0] = (byte) 0;
			len2[1] = (byte) 0;
			len2[2] = (byte) ((len >> 8) & 0xff);
			len2[3] = (byte) (len & 0xff);
		} else if (len < Math.pow(2, 24))
		{
			len2[0] = (byte) 0;
			len2[1] = (byte) ((len >> 16) & 0xff);
			len2[2] = (byte) ((len >> 8) & 0xff);
			len2[3] = (byte) (len & 0xff);
		} else
		{
			len2[0] = (byte) ((len >> 24) & 0xff);
			len2[1] = (byte) ((len >> 16) & 0xff);
			len2[2] = (byte) ((len >> 8) & 0xff);
			len2[3] = (byte) (len & 0xff);
		}
	}

	private static int deCompressLen(byte[] len2)
	{
		boolean flag = true;
		int num = 0;
		int i = 0;
		while (i < len2.length && flag)
		{
			if (len2[i] == (byte) (0) && flag)
			{
				i++;
				continue;
			}
			flag = false;
			if (i == 3 && len2[3] == 0)
			{
				num = 0;
				break;
			}
			switch (i)
			{
			case 0:
				num = (len2[0] & 0xff) * 16777216 + (len2[1] & 0xff) * 65536 + (len2[2] & 0xff) * 256
						+ (len2[3] & 0xff);
				break;
			case 1:
				num = (len2[1] & 0xff) * 65536 + (len2[2] & 0xff) * 256 + (len2[3] & 0xff);
				break;
			case 2:
				num = (len2[2] & 0xff) * 256 + (len2[3] & 0xff);
				break;
			case 3:
				num = len2[3] & 0xff;
				break;
			default:
				System.out.println("the process of compression of len is error!");
			}
		}
		return num;
	}

	// 这里还原需要把所有步骤解决，首先是二进制转换为10进制，然后在pbwt还原
	public static ArrayList<ArrayList<Integer>> DecodePBWT(byte[] temp)
	{
		// 首先截取前四位为长度，然后4+长度为value。顺次解压完成之后再去解压key，在解压key的同时完成pbwt的还原工作
		ArrayList<Integer> bValsFromByte = new ArrayList<Integer>();
		ArrayList<Byte> bKeysFromByte = new ArrayList<Byte>();
		ArrayList<ArrayList<Integer>> reList = new ArrayList<ArrayList<Integer>>();
		ArrayList<ArrayList<Integer>> result = new ArrayList<ArrayList<Integer>>();
		int key = 0;
		byte[] len2 = new byte[4];
		int len = 0;
		for (int i = 0; i < temp.length; i++)
		{
			if (i < 4)
			{
				len2[i] = temp[i];
				if (i == 3)
				{
					len = deCompressLen(len2);
				}
				continue;
			}
			if (i < 4 + len)
			{
				if (temp[i] != 127)
				{
					bValsFromByte.add(temp[i] + 128);
				} else
				{
					bValsFromByte.add(temp[i + 1] * 256 + temp[i + 2]);
					i += 2;
				}
			} else
			{
				key = temp[i] + 128;
				bKeysFromByte.add((byte) ((key >> 6) % 4));
				bKeysFromByte.add((byte) ((key >> 4) % 4));
				bKeysFromByte.add((byte) ((key >> 2) % 4));
				bKeysFromByte.add((byte) ((key >> 0) % 4));
			}
		}

		reList = reListPbwt(bKeysFromByte, bValsFromByte);

		// result 这里需要pbwt还原
		result = PBWTAlgoRe2(reList);

		return result;

	}

	/**
	 * 局部解压缩策略
	 * @param byteKV
	 * @return
	 */
	public static ArrayList<ArrayList<Integer>> DecodeTPBWT(byte[] byteKV)
	{
		// 首先截取前四位为长度，然后4+长度为value。顺次解压完成之后再去解压key，在解压key的同时完成pbwt的还原工作
//		ArrayList<Integer> bKeys = new ArrayList<Integer>();
//		ArrayList<Integer> bVals = new ArrayList<Integer>();

//		ArrayList<ArrayList<Integer>> reList = new ArrayList<ArrayList<Integer>>();
		ArrayList<ArrayList<Integer>> result = new ArrayList<ArrayList<Integer>>();
		
//		DebitKV(byteKV, bKeys, bVals);  // 解压缩为key 和 value
		
//		reList = reListPbwt2(bKeys, bVals); // 解压缩为短读序列
		
		// result 这里需要pbwt还原
//		result = PBWTAlgoRe2(reList); // 逆向pbwt还原
		result = DebitKV(byteKV);
		return result;

	}
	/**key和value合起来的解压缩算法
	 * @param byteKV
	 * @param bKeys
	 * @param bVals
	 */
	private static void DebitKV(byte[] byteKV, ArrayList<Integer> bKeys, ArrayList<Integer> bVals)
	{
		int tempByte = 0;
		int tempByte2 = 0;
		int value = -1;
		int key = -1;
		int lenFlag = -1;
		for(int i =0; i<byteKV.length; i++)
		{
			tempByte = byteKV[i] + 128;
			key = (tempByte >> 6)%4;
			if (key == 2)
			{
				value = (tempByte >> 4)%4;
			}
			// 当key为0,1,3时候都有可能长度超过31
			else
			{
				lenFlag = (tempByte >> 5)%2;
				if (lenFlag == 1)
				{
					i++;
					tempByte2 = byteKV[i] + 128;
					value = tempByte%32*256 + tempByte2; // 这里稍有问题，暂时这样
				}
				else 
				{
					value = tempByte%32;
				}
			}
			bKeys.add(key);
			bVals.add(value);
		}
	}

	/**
	 * 将bit转换为key和value，然后转换为RL整合到一起
	 * 将空列位置保留了下来
	 * REF参数没有传出去，且3和2认为一种情况
	 * @param byteKV
	 * @return reListsPBWT
	 */
	private static ArrayList<ArrayList<Integer>> DebitKV(byte[] byteKV)
	{
		int tempByte = 0;
		int tempByte2 = 0;
		int value = -1;
		int key = -1;
		int lenFlag = -1;
		int lastvalue = -1;
		boolean oneFlag = false;
		boolean zeroFlag =false;
		ArrayList<ArrayList<Integer>> reListsPBWT = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> pbwt = new ArrayList<Integer>();
		ArrayList<Integer> REF = new ArrayList<>();
		
		for(int i =0; i<byteKV.length; i++)
		{
			tempByte = byteKV[i] + 128;
			key = (tempByte >> 6)%4;
			if (key == 2)
			{
				value = (tempByte >> 4)%4;
				if (value == 1) // key 为1，0,1混合或者空或者全是3
				{
					if (zeroFlag && oneFlag)
					{
						REF.add(10);
					}
					else if (lastvalue!=2)
					{
						// 上一个value不是2说明该列不为空
						// 暂时认为情况一样
						REF.add(2);
					}
					else
					{
						// 上述条件一个都不满足
						REF.add(2);
					}
				}
				else if (value == 2)	// value 为2 全是0情况
				{
					REF.add(0);
				}
				else	// value为3，全是1情况
				{
					REF.add(1);
				}
				zeroFlag = false;
				oneFlag = false;
				reListsPBWT.add(pbwt);
				pbwt = new ArrayList<>();
			}
			// 当key为0,1,3时候都有可能长度超过31
			else
			{
				if (!zeroFlag && key == 0)
				{
					zeroFlag = true;
				}
				else if (!oneFlag && key == 1)
				{
					oneFlag = true;
				}
				else {
					// 为三的情况
				}
				lenFlag = (tempByte >> 5)%2;
				if (lenFlag == 1)
				{
					i++;
					tempByte2 = byteKV[i] + 128;
					value = tempByte%32*256 + tempByte2; // 这里稍有问题，暂时这样
				}
				else 
				{
					value = tempByte%32;
				}

				// 0,1,3情况转换为RL
				while(value--> 0) 
				{
					pbwt.add(key);
				}
			}
			lastvalue = value;
		}
		return reListsPBWT;
	}

	
	private void DecodePbwt(byte[] keys, byte[] values)
	{
		// 对于key值进行解压,还原之后的key数量可能会多些，因为有补位
		ArrayList<Byte> bKeysFromByte = new ArrayList<Byte>();
		int key = 0;
		for (int i = 0; i < keys.length; i++)
		{
			// 移位+取余 处理最方便
			key = keys[i] + 128;
			bKeysFromByte.add((byte) ((key >> 6) % 4));
			bKeysFromByte.add((byte) ((key >> 4) % 4));
			bKeysFromByte.add((byte) ((key >> 2) % 4));
			bKeysFromByte.add((byte) ((key >> 0) % 4));
		}
		System.out.println("bKeysFromByte.size:\t" + bKeysFromByte.size());
		// 对于value值进行解压，还原之后的value数量可能会多些，因为有补位
		ArrayList<Integer> bValsFromByte = new ArrayList<Integer>();
		for (int i = 0; i < values.length; i++)
		{
			if (values[i] != 127)
			{
				bValsFromByte.add(values[i] + 128);
			} else
			{
				bValsFromByte.add(values[i + 1] * 256 + values[i + 2]);
				i += 2;
			}
		}
		System.out.println("bValsFromByte.size:\t" + bValsFromByte.size());

		reListPbwt(bKeysFromByte, bValsFromByte);

	}

	private static ArrayList<ArrayList<Integer>> reListPbwt(ArrayList<Byte> bKeysFromByte,
			ArrayList<Integer> bValsFromByte)
	{
		ArrayList<ArrayList<Integer>> reListsPBWT = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> pbwt = new ArrayList<Integer>();
		int num = 0;
		int keyTemp = 0;
		for (int i = 0; i < bValsFromByte.size(); i++)
		{
			if (bKeysFromByte.get(i) == 2)
			{
				reListsPBWT.add(pbwt);
				pbwt = new ArrayList<Integer>();
			} else
			{
				num = bValsFromByte.get(i);
				keyTemp = bKeysFromByte.get(i);
				while (num-- > 0)
				{
					pbwt.add(keyTemp);
				}
			}
		}

//		for (ArrayList<Integer> list : reListsPBWT)
//		{
//			System.out.println(list.toString());
//		}
		return reListsPBWT;
	}

	private static ArrayList<ArrayList<Integer>> reListPbwt2(ArrayList<Integer> bKeysFromByte,
			ArrayList<Integer> bValsFromByte)
	{
		ArrayList<ArrayList<Integer>> reListsPBWT = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> pbwt = new ArrayList<Integer>();
		int num = 0;
		int keyTemp = 0;
		for (int i = 0; i < bValsFromByte.size(); i++)
		{
			if (bKeysFromByte.get(i) == 2)
			{
				reListsPBWT.add(pbwt);
				pbwt = new ArrayList<Integer>();
			} else
			{
				num = bValsFromByte.get(i);
				keyTemp = bKeysFromByte.get(i);
				while (num-- > 0)
				{
					pbwt.add(keyTemp);
				}
			}
		}

//		for (ArrayList<Integer> list : reListsPBWT)
//		{
//			System.out.println(list.toString());
//		}
		return reListsPBWT;
	}

	
	public static boolean PbwtIsTrue(ArrayList<ArrayList<Integer>> pbwtResult, ArrayList<ArrayList<Integer>> repbwt)
	{
		for (int i = 0; i < pbwtResult.size() && i < repbwt.size(); i++)
		{
			for (int j = 0; j < pbwtResult.get(i).size() && j < repbwt.get(i).size(); j++)
			{
				if (pbwtResult.get(i).get(j) != repbwt.get(i).get(j))
				{
					System.out.println("Wrong!：i=" + i + "  j=" + j);
					return false;
				}
			}
		}
		System.out.println("the Process Of pbwt is right!");
		return true;
	}

	public static boolean PbwtIsTrue2(ArrayList<ReadStruct> readsList, ArrayList<ArrayList<Integer>> repbwt)
	{
		for (int i = 0; i < readsList.size() && i < repbwt.size(); i++)
		{
			for (int j = 0; j < readsList.get(i).getReads().size() && j < repbwt.get(i).size(); j++)
			{
				if (readsList.get(i).getReads().get(j) != repbwt.get(i).get(j))
				{
					System.out.println("Wrong!：i=" + i + "  j=" + j);
					return false;
				}
			}
		}
		System.out.println("the Process Of pbwt is right!");
		return true;
	}

	/**
	 * 进行pbwt逆变换，从第一列开始如果是0的话，就把下一列的从前面的一直对应，如果是1的话，那就是后面的与之对应
	 * 
	 * @param pbwtResult
	 * @return
	 */
	private static ArrayList<ArrayList<Integer>> PBWTAlgoRe(ArrayList<ArrayList<Integer>> pbwtResult)
	{
		PBWTReadRe pbwtReadRe = new PBWTReadRe();
		ArrayList<ArrayList<Integer>> readsResult = new ArrayList<ArrayList<Integer>>();
		// ArrayList<ArrayList<String>> exceptionResult = new
		// ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<Integer>> readsCurrListTemp = new ArrayList<ArrayList<Integer>>();
		// ArrayList<ArrayList<String>> exceptionResultTemp = new
		// ArrayList<ArrayList<String>>();

		pbwtReadRe.setReadsResult(readsResult);
		// ArrayList<Integer> startAlignment = new ArrayList<Integer>();
		// ArrayList<Integer> endAlignment = new ArrayList<Integer>();
		// pbwtReadRe.setStartAlignment(startAlignment);
		// pbwtReadRe.setEndAlignment(endAlignment);

		// ArrayList<ArrayList<Character>> qualReadsResult = new
		// ArrayList<ArrayList<Character>>();
		// ArrayList<ArrayList<String>> qualExceptionResult = new
		// ArrayList<ArrayList<String>>();
		// ArrayList<ArrayList<Character>> qualReadsCurrListTemp = new
		// ArrayList<ArrayList<Character>>();
		// ArrayList<ArrayList<String>> qualExceptionResultTemp = new
		// ArrayList<ArrayList<String>>();

		ArrayList<Integer> d = new ArrayList<Integer>();
		ArrayList<Integer> e = new ArrayList<Integer>();

		int preVal = 0, preSize = 0; // 定义之前的碱基值和之前列的大小
		int currVal = 0, currSize = 0, currAddSize = 0; // 定义当前的
		ArrayList<Integer> removeIndexRe = new ArrayList<Integer>();
		Boolean removeFlag = false;
		int exceptionIndex = 0;	// exceptionList 的唯一index
		int endIndex = 0;
		int pre = 0; // 索引空间大小
		int cur = 0; // currentlist当前被删除元素之前序列数量
		LinkedList<Integer> posIndex = new LinkedList<Integer>(); // 位置索引表，表示插入到输出list中位置
		// 比对完之后从左往右
		for (int col = 0; col < pbwtResult.size(); col++)
		{
			preSize = readsCurrListTemp.size();
			currSize = pbwtResult.get(col).size(); // liyang：没有currsize的大小是1,这就说明了这里只是拿出了第一个容器
			currAddSize = currSize > preSize ? (currSize - preSize) : 0; // crazy：这里的目的是为了后面构造还原空间的
			// 只有对第一列处理比较特殊，其他都是正常，前一个大小就是前一列的大小
			// 构造还原kongjian
			for (int i = 0; i < currAddSize; i++)
			{
				ArrayList<Integer> listTemp = new ArrayList<Integer>();
				readsCurrListTemp.add(listTemp);
				// ArrayList<String> listExceptionTemp = new ArrayList<String>();
				// exceptionResultTemp.add(listExceptionTemp);

				// ArrayList<Character> listTemp2 = new ArrayList<Character>();
				// qualReadsCurrListTemp.add(listTemp2);
				// ArrayList<String> listExceptionTemp2 = new ArrayList<String>();
				// qualExceptionResultTemp.add(listExceptionTemp2);
			}
			if (col == 0)
			{
				// 针对第一列的情形，做一下特殊化的处理，全部加入到临时temp中
				for (int i = 0; i < readsCurrListTemp.size(); i++)
				{
					readsCurrListTemp.get(i).add(pbwtResult.get(col).get(i));
					// 这是质量数部分的处理
					// qualReadsCurrListTemp.get(i).add(listsPbwtQual.get(col).get(i));
				}
				continue;
			}
			// 还原的时候是按照存放之前位置的索引，来从前往后进行还原
			// 比对完之后从上往下
			for (int row = 0; row < preSize; row++)
			{
				preVal = readsCurrListTemp.get(row).get(readsCurrListTemp.get(row).size() - 1);
				currVal = pbwtResult.get(col).get(row);
				if (currVal == ReadElemEnum.END.ordinal())
				{
					removeFlag = true;
				}
				if (preVal == 0)
				{
					d.add(row);
				} else if (preVal == 1)
				{
					e.add(row);
				} else
				{
					System.out.println("Invalid ELSE");
				}

				// 单独写一下对当前位点的处理，把异常信息值写入进来
				if (preVal == 1)
				{
					// 这里这么处理，是由于exceptionList的数据特点来决定的，这部分可以仔细想一下
					// qualExceptionResultTemp.get(row).addAll(exceptionListQual.get(exceptionIndex));
					// exceptionResultTemp.get(row).addAll(exceptionList.get(exceptionIndex++));
				}
			}

			Integer[] f = new Integer[currSize];
			// 这里加入对质量数的处理, 异常的质量数，稍后再加进去吧，现在有点搞不明白了
			// Character[] qualF = new Character[currSize];
			int k = 0;
			for (Integer ins : d)
			{
				// qualF[ins] = listsPbwtQual.get(col).get(k);
				f[ins] = pbwtResult.get(col).get(k++);

			}
			for (Integer ins : e)
			{
				// qualF[ins] = listsPbwtQual.get(col).get(k);
				f[ins] = pbwtResult.get(col).get(k++);
			}
			for (; k < currSize; k++)
			{
				// qualF[k] = listsPbwtQual.get(col).get(k);
				f[k] = pbwtResult.get(col).get(k);
			}
			for (int m = 0; m < currSize; m++)
			{
				readsCurrListTemp.get(m).add(f[m]);
				// qualReadsCurrListTemp.get(m).add(qualF[m]);
			}
			// 这里是将要删除的索引位置加入到removeIndeRe中，这个位置是currentlist中的位置
			if (removeFlag)
			{
				for (int i = 0; i < readsCurrListTemp.size(); i++)
				{
					if (readsCurrListTemp.get(i).get(readsCurrListTemp.get(i).size() - 1) == ReadElemEnum.END.ordinal())
					{
						removeIndexRe.add(i);
					}
				}
			}
			// 这里是需要修改的地方
			int offset = 0; // 因为有偏移，所以需要offset
			for (Integer ins : removeIndexRe)
			{
				pre = posIndex.size();
				cur = ins - offset;
				if (pre == cur)
				{
					// 加在末尾
					readsResult.add(readsCurrListTemp.get(ins - offset));
//					System.out.println(readsCurrListTemp.get(ins - offset));
					readsCurrListTemp.remove(ins - offset);
				} else if (pre < cur)
				{
					// 在末尾加上站位符号，并将删除的序列加入到最后
					for (int i = 0; i < (cur - pre); i++)
					{
						ArrayList<Integer> empty = new ArrayList<Integer>();
						readsResult.add(empty);
						posIndex.add(readsResult.size() - 1);
					}
					readsResult.add(readsCurrListTemp.get(ins - offset));
//					System.out.println(readsCurrListTemp.get(ins - offset));
					readsCurrListTemp.remove(ins - offset);
				}
				// 此时表明之前没有出去的序列要出去
				else
				{
					int pos = posIndex.get(cur);
					posIndex.remove(cur);
					readsResult.set(pos, readsCurrListTemp.get(ins - offset));
//					System.out.println(readsCurrListTemp.get(ins - offset));
					readsCurrListTemp.remove(ins - offset);
				}
				// readsResult.add(readsCurrListTemp.get(ins - offset));
				// System.out.println(readsCurrListTemp.get(ins - offset));
				// readsCurrListTemp.remove(ins - offset);
				// exceptionResult.add(exceptionResultTemp.get(ins - offset));
				// exceptionResultTemp.remove(ins - offset);
				// startAlignment 和 endAlignment都处理一下
				// startAlignment.add(startAndEndList.get(endIndex).getAlignmentStart());
				// startAlignment.add(start[endIndex]);
				// endAlignment.add(startAndEndList.get(endIndex++).getAlignmentEnd());
				// endAlignment.add(end[endIndex++]);
				// 这里把质量数加入进来
				// qualReadsResult.add(qualReadsCurrListTemp.get(ins - offset));
				// qualReadsCurrListTemp.remove(ins - offset);
				// qualExceptionResult.add(qualExceptionResultTemp.get(ins - offset));
				// qualExceptionResultTemp.remove(ins - offset);
				offset++;
			}
			removeIndexRe.clear();
			d.clear();
			e.clear();
			removeFlag = false;
		}
		System.out.println("PBWT Convert Re.(readResult.length):\t");
//		for (int i = 0; i < readsResult.size(); i++)
//		{
//			System.out.println(readsResult.get(i).toString());
//		}

		return readsResult;
	}

	// 这里还需要参数ak，改一下函数传进来这个参数。暂时用静态变量进行处理
	/**
	 * 适用于全局和局部按照行解压缩
	 * @param pbwtResult
	 * @return
	 */
	private static ArrayList<ArrayList<Integer>> PBWTAlgoRe2(ArrayList<ArrayList<Integer>> pbwtResult)
	{
		PBWTReadRe pbwtReadRe = new PBWTReadRe();
		ArrayList<ArrayList<Integer>> readsResult = new ArrayList<ArrayList<Integer>>();
		ArrayList<ArrayList<Integer>> readsCurrListTemp = new ArrayList<ArrayList<Integer>>();

		pbwtReadRe.setReadsResult(readsResult);
		
		int indexpos = -1;
		int posValue = -1;

		int preSize = 0; // 定义之前的碱基值和之前列的大小
		int currSize = 0, currAddSize = 0; // 定义当前的
		ArrayList<Integer> removeIndexRe = new ArrayList<Integer>();
		Boolean removeFlag = false;
		int pre = 0; // 索引空间大小
		int cur = 0; // currentlist当前被删除元素之前序列数量
		LinkedList<Integer> posIndex = new LinkedList<Integer>(); // 位置索引表，表示插入到输出list中位置
		// 比对完之后从左往右
		for (int col = 0; col < pbwtResult.size(); col++)
		{
			preSize = readsCurrListTemp.size();
			currSize = pbwtResult.get(col).size(); // liyang：没有currsize的大小是1,这就说明了这里只是拿出了第一个容器
			currAddSize = currSize > preSize ? (currSize - preSize) : 0; // crazy：这里的目的是为了后面构造还原空间的
			// 只有对第一列处理比较特殊，其他都是正常，前一个大小就是前一列的大小
			// 构造还原空间
			for (int i = 0; i < currAddSize; i++)
			{
				ArrayList<Integer> listTemp = new ArrayList<Integer>();
				readsCurrListTemp.add(listTemp);
			}
			if (!readsCurrListTemp.isEmpty())
			{
				if (col == 0 || preSize == 0)
				{
					// 针对第一列的情形，做一下特殊化的处理，全部加入到临时temp中
					for (int i = 0; i < readsCurrListTemp.size(); i++)
					{
						readsCurrListTemp.get(i).add(pbwtResult.get(col).get(i));
						// 这是质量数部分的处理
						// qualReadsCurrListTemp.get(i).add(listsPbwtQual.get(col).get(i));
					}
					continue;
				}
				// 还原的时候是按照存放之前位置的索引，来从前往后进行还原
				// 比对完之后从上往下
				for (int row = 0; row < currSize; row++)
				{
					indexpos = tempIndexes.get(col).get(row);
					posValue = pbwtResult.get(col).get(row);
					
					// posvalue是3的情况，需要记录indexpos方便之后删除
					if (posValue ==ReadElemEnum.END.ordinal() )
					{
						readsCurrListTemp.get(indexpos).add(posValue);
						removeIndexRe.add(indexpos);
						removeFlag = true;
					}
					// 按照row找到ak数组中对应的indexpos，然后找到将这个值放到indexpos位置
					else 
					{
						readsCurrListTemp.get(indexpos).add(posValue);
					}
				}

				int offset = 0; // 因为有偏移，所以需要offset
				Collections.sort(removeIndexRe);
				for (Integer ins : removeIndexRe)
				{
				 	pre = posIndex.size();
					cur = ins - offset;
					if (pre == cur)
					{
						// 加在末尾
						readsResult.add(readsCurrListTemp.get(ins - offset));
//						System.out.println(readsCurrListTemp.get(ins - offset));
						readsCurrListTemp.remove(ins - offset);
					} 
					else if (pre < cur)
					{
						// 在末尾加上站位符号，并将删除的序列加入到最后
						for (int i = 0; i < (cur - pre); i++)
						{
							ArrayList<Integer> empty = new ArrayList<Integer>();
							readsResult.add(empty);
							posIndex.add(readsResult.size() - 1);
						}
						readsResult.add(readsCurrListTemp.get(ins - offset));
//						System.out.println(readsCurrListTemp.get(ins - offset));
						readsCurrListTemp.remove(ins - offset);
					}
					// 此时表明之前没有出去的序列要出去
					else
					{
						int pos = posIndex.get(cur);
						posIndex.remove(cur);
						readsResult.set(pos, readsCurrListTemp.get(ins - offset));
//						System.out.println(readsCurrListTemp.get(ins - offset));
						readsCurrListTemp.remove(ins - offset);
					}
					offset++;
				}
				removeIndexRe.clear();
				removeFlag = false;
			}
			else {
				
			}
			
		}
		System.out.println("PBWT Convert Re.(readResult.length):\t");
//		for (int i = 0; i < readsResult.size(); i++)
//		{
//			System.out.println(readsResult.get(i).toString());
//		}

		return readsResult;
	}

	/**
	 * 用于插入压缩问题，垂直方向解压缩，方便再次进行压缩
	 * @param pbwtResult
	 * @return
	 */
	private static ArrayList<ArrayList<Integer>> PBWTAlgoRe3(ArrayList<ArrayList<Integer>> pbwtResult)
	{
		PBWTReadRe pbwtReadRe = new PBWTReadRe();
		ArrayList<ArrayList<Integer>> readsResult = new ArrayList<ArrayList<Integer>>();
		ArrayList<ArrayList<Integer>> readsCurrListTemp = new ArrayList<ArrayList<Integer>>();
		pbwtReadRe.setReadsResult(readsResult);

		int curIndex = -1;
		int curValue = -1;
		int nextemp = -1;
		int j = 0;
		for (int i = 0; i < pbwtResult.size(); i++)
		{
			j = 0;
			curIndex = tempIndexes.get(i).get(0);
			curValue = pbwtResult.get(i).get(0);
			while (j < pbwtResult.get(i).size())
			{
				nextemp = pbwtResult.get(i).get(curIndex);
				pbwtResult.get(i).set(curIndex, curValue);
				curValue = nextemp;
				curIndex = tempIndexes.get(i).get(curIndex);
				j++;
			}

			// pbwtResult.get(i).
		}

		System.out.println("PBWT Convert Re.(readResult.length):\t");

		return readsResult;
	}

	/**
	 * 改写输入参数，按照每一条reads存储,局部压缩
	 * @param pbwtResult
	 * @return
	 */
	private static ArrayList<DeRead> PBWTAlgoRe4(ReadPbwtResult reResult)
	{
		ArrayList<ArrayList<Integer>> pbwtResult = new ArrayList<>();
		
		ArrayList<DeRead> readsCurTemp = new ArrayList<>();
		ArrayList<DeRead> result = new ArrayList<>();

		int MinStart = reResult.getMinStart();
		pbwtResult = reResult.getListsPBWT();
		int curValue = -1;
		char curReadQual;
		char curExQual;
		String curEx = null;
		
		int qualIndex = 0;
		int exQualIndex = 0;
		int exIndex = 0;
		
		ArrayList<Integer> reIndex;	// 构建当前列索引
		ArrayList<ArrayList<Integer>> Index = new ArrayList<>(); // 所有索引信息
	
		int indexpos = -1;
		int posValue = -1;

		ArrayList<Integer> c;
		ArrayList<Integer> d;
		
		boolean oneFlag = false;
		boolean zeroFlag = false;
		
		int preSize = 0; // 定义之前的碱基值和之前列的大小
		int currSize = 0, currAddSize = 0; // 定义当前的
		ArrayList<Integer> removeIndexRe = new ArrayList<Integer>();
		Boolean removeFlag = false;
		int pre = 0; // 索引空间大小
		int cur = 0; // currentlist当前被删除元素之前序列数量
		LinkedList<Integer> posIndex = new LinkedList<Integer>(); // 位置索引表，表示插入到输出list中位置
		// 比对完之后从左往右
		for(int col = 0; col < pbwtResult.size(); col++)
		{
			preSize = readsCurTemp.size(); // 前一列大小用currlist大小表示
			currSize = pbwtResult.get(col).size(); // 用取到pbwt当前列表示
			currAddSize = currSize > preSize ? (currSize - preSize) : 0; // 新加入序列大小
			// 构造还原空间
			for (int i = 0; i < currAddSize; i++)
			{
				DeRead listTemp = new DeRead();
				listTemp.setStart(MinStart+col); // 在创建序列的时候设置起始位置信息
				readsCurTemp.add(listTemp);
				if (col != 0)
				{
					int length = Index.size() - 1;
					int length2= Index.get(length).size(); // 会不断更新
					Index.get(length).add(length2);
				}
			}
			if (!readsCurTemp.isEmpty())
			{
				c = new ArrayList<>();
				d = new ArrayList<>();
				// 第一列或者是前一列为空列
				if (col == 0)
				{
					reIndex = new ArrayList<>();
					int preIndex = 0;
					
					for (int i = 0; i < readsCurTemp.size(); i++)
					{
						curValue = pbwtResult.get(col).get(i);
						preIndex = i;
						readsCurTemp.get(i).getRead().add(curValue);
						if (curValue == 0)
						{
							// 加入质量分数,一列都是一个值
							curReadQual = reResult.getListsQual().get(qualIndex);
							readsCurTemp.get(i).getReadQual().add(curReadQual);
							readsCurTemp.get(i).getQual().add(curReadQual);
							c.add(preIndex);
							zeroFlag = true;
						}
						else if (curValue == 1) {
							// 添加异常信息，一列都是一个质量分数
							curExQual = reResult.getListsQual().get(exQualIndex);
							curEx = reResult.getListsExcep().get(exIndex).get(0);
							readsCurTemp.get(i).getExQual().add(curExQual);
							readsCurTemp.get(i).getQual().add(curExQual);
							readsCurTemp.get(i).getExBase().add(curEx);
							reResult.getListsExcep().get(exIndex).remove(0);
							d.add(preIndex);
							oneFlag = true;
						}
						else {
							// 当前为三，一般不可能
						}
						reIndex.add(i);
					}
					if (oneFlag)
					{
						exQualIndex++;
						exIndex++;
						oneFlag = false;
					}
					if (zeroFlag)
					{
						qualIndex++;
						zeroFlag = false;
					}
					c.addAll(d);
					Index.add(reIndex);
					Index.add(c);
					continue;
				}
				// 对于RL纵向序列采用逆TPBWT还原
				for (int row = 0; row < currSize; row++)
				{
					indexpos = Index.get(col).get(row);
					posValue = pbwtResult.get(col).get(row);
					if (posValue == 0){
						c.add(indexpos);
						readsCurTemp.get(indexpos).getRead().add(posValue);
						curReadQual = reResult.getListsQual().get(qualIndex);
						readsCurTemp.get(indexpos).getReadQual().add(curReadQual);
						readsCurTemp.get(indexpos).getQual().add(curReadQual); // 不要忘记在最后对计数器改变值
						zeroFlag = true;
						
					}
					else if (posValue == 1) {
						d.add(indexpos);
						readsCurTemp.get(indexpos).getRead().add(posValue);
						curExQual = reResult.getListsQual().get(exQualIndex);
						curEx = reResult.getListsExcep().get(exIndex).get(0);
						readsCurTemp.get(indexpos).getExQual().add(curExQual);
						readsCurTemp.get(indexpos).getExBase().add(curEx);
						reResult.getListsExcep().get(exIndex).remove(0);
						oneFlag = true;
					}
					else if (posValue == ReadElemEnum.END.ordinal()) {
						readsCurTemp.get(indexpos).getRead().add(posValue);
						removeIndexRe.add(indexpos);
						removeFlag = true;
					}
					else {
						// 这个位置留给情况2
						// 索引的处理放在后面，
					}
				}
				// 在这里构建下一列的索引
				// 并且该自增1的在这里判断
				if (oneFlag)
				{
					exQualIndex++;
					exIndex++;
					oneFlag = false;
				}
				if (zeroFlag)
				{
					qualIndex++;
					zeroFlag = false;
				}
				
				// 在这里构建下一列的索引
				// 改一下，放在后面需要进行一步删除操作
				c.addAll(d);
				
				if (removeFlag == true)
				{
					// 删除结尾为3的序列并且同时改变索引表
					int offset = 0;
					Collections.sort(removeIndexRe);
					for (Integer ins : removeIndexRe)
					{
						pre = posIndex.size();
						cur = ins - offset;
						// 删除序列操作
						if (pre == cur)
						{
							// 加在末尾
							result.add(readsCurTemp.get(ins - offset));
							readsCurTemp.remove(ins - offset);
							
						}
						else if (pre<cur) {
							for (int i = 0; i < cur-pre; i++)
							{
								DeRead empty = new DeRead();
								result.add(empty);
								posIndex.add(result.size() - 1);
							}
							result.add(readsCurTemp.get(ins - offset));
							readsCurTemp.remove(ins - offset);
						}
						else {
							// 此时表明之前没有出去的序列要出去
							int pos = posIndex.get(cur);
							posIndex.remove(cur);
							result.set(pos, readsCurTemp.get(ins - offset));
							readsCurTemp.remove(ins - offset);
						}
						// 改变索引表
						int tempIndex = -1;
						for (int i = 0; i < c.size(); i++)
						{
							if ((ins - offset)<=c.get(i))
							{
								tempIndex = c.get(i)-1;
								c.set(i, tempIndex);
							}
//							else if ((ins - offset) == c.get(i))
//							{
//								c.remove(i);
//								i--;
//							}
//							else {
//								
//							}
						}
						
						offset++;
					}
					removeIndexRe.clear();
					removeFlag = false;
				}
				
				// 在这里构建下一列的索引
				// 改一下，放在后面需要进行一步删除操作
				Index.add(c);
				
			}
			else
			{
				// 这里是针对该列为空的情况，单独设计
				ArrayList<Integer> emptyIndex = new ArrayList<>();
				Index.add(emptyIndex);
			}
			
		}
	return result;	
	}
	
	/**
	 * 继承于4，适用于全局
	 * @param reResult
	 * @return
	 */
	private static ArrayList<DeRead> PBWTAlgoRe5(ReadPbwtResult reResult)
	{
		ArrayList<ArrayList<Integer>> pbwtResult = new ArrayList<>();
		
		ArrayList<DeRead> readsCurTemp = new ArrayList<>();
		ArrayList<DeRead> result = new ArrayList<>();

		int MinStart = reResult.getMinStart();
		pbwtResult = reResult.getListsPBWT();
		int curValue = -1;
		char curReadQual;
		char curExQual;
		String curEx = null;
		
		int qualIndex = 0;
		int exQualIndex = 0;
		int exIndex = 0;
		
		ArrayList<Integer> reIndex;	// 构建当前列索引
		ArrayList<ArrayList<Integer>> Index = new ArrayList<>(); // 所有索引信息
	
		int indexpos = -1;
		int posValue = -1;

		ArrayList<Integer> c;
		ArrayList<Integer> d;
		
		boolean oneFlag = false;
		boolean zeroFlag = false;
		
		int preSize = 0; // 定义之前的碱基值和之前列的大小
		int currSize = 0, currAddSize = 0; // 定义当前的
		ArrayList<Integer> removeIndexRe = new ArrayList<Integer>();
		Boolean removeFlag = false;
		int pre = 0; // 索引空间大小
		int cur = 0; // currentlist当前被删除元素之前序列数量
		LinkedList<Integer> posIndex = new LinkedList<Integer>(); // 位置索引表，表示插入到输出list中位置
		// 比对完之后从左往右
		for(int col = 0; col < pbwtResult.size(); col++)
		{
			preSize = readsCurTemp.size(); // 前一列大小用currlist大小表示
			currSize = pbwtResult.get(col).size(); // 用取到pbwt当前列表示
			currAddSize = currSize > preSize ? (currSize - preSize) : 0; // 新加入序列大小
			// 构造还原空间
			for (int i = 0; i < currAddSize; i++)
			{
				DeRead listTemp = new DeRead();
				listTemp.setStart(MinStart+col); // 在创建序列的时候设置起始位置信息
				readsCurTemp.add(listTemp);
				if (col != 0)
				{
					int length = Index.size() - 1;
					int length2= Index.get(length).size(); // 会不断更新
					Index.get(length).add(length2);
				}
			}
			if (!readsCurTemp.isEmpty())
			{
				c = new ArrayList<>();
				d = new ArrayList<>();
				// 第一列或者是前一列为空列
				if (col == 0)
				{
					reIndex = new ArrayList<>();
					int preIndex = 0;
					
					for (int i = 0; i < readsCurTemp.size(); i++)
					{
						curValue = pbwtResult.get(col).get(i);
						preIndex = i;
						readsCurTemp.get(i).getRead().add(curValue);
						if (curValue == 0)
						{
							// 加入质量分数,一列都是一个值
							curReadQual = reResult.getListsQual().get(qualIndex);
							readsCurTemp.get(i).getReadQual().add(curReadQual);
							readsCurTemp.get(i).getQual().add(curReadQual);
							c.add(preIndex);
							zeroFlag = true;
						}
						else if (curValue == 1) {
							// 添加异常信息，一列都是一个质量分数
							curExQual = reResult.getListsQual().get(exQualIndex);
							curEx = reResult.getListsExcep2().get(exIndex);
							readsCurTemp.get(i).getExQual().add(curExQual);
							readsCurTemp.get(i).getQual().add(curExQual);
							readsCurTemp.get(i).getExBase().add(curEx);
							exIndex++;
							d.add(preIndex);
							oneFlag = true;
						}
						else {
							// 当前为三，一般不可能
						}
						reIndex.add(i);
					}
					if (oneFlag)
					{
						exQualIndex++;
						oneFlag = false;
					}
					if (zeroFlag)
					{
						qualIndex++;
						zeroFlag = false;
					}
					c.addAll(d);
					Index.add(reIndex);
					Index.add(c);
					continue;
				}
				// 对于RL纵向序列采用逆TPBWT还原
				for (int row = 0; row < currSize; row++)
				{
					indexpos = Index.get(col).get(row);
					posValue = pbwtResult.get(col).get(row);
					if (posValue == 0){
						c.add(indexpos);
						readsCurTemp.get(indexpos).getRead().add(posValue);
						curReadQual = reResult.getListsQual().get(qualIndex);
						readsCurTemp.get(indexpos).getReadQual().add(curReadQual);
						readsCurTemp.get(indexpos).getQual().add(curReadQual); // 不要忘记在最后对计数器改变值
						zeroFlag = true;
						
					}
					else if (posValue == 1) {
						d.add(indexpos);
						readsCurTemp.get(indexpos).getRead().add(posValue);
						curExQual = reResult.getListsQual().get(exQualIndex);
						curEx = reResult.getListsExcep2().get(exIndex);
						readsCurTemp.get(indexpos).getExQual().add(curExQual);
						readsCurTemp.get(indexpos).getExBase().add(curEx);
						exIndex++;
						oneFlag = true;
					}
					else if (posValue == ReadElemEnum.END.ordinal()) {
						readsCurTemp.get(indexpos).getRead().add(posValue);
						removeIndexRe.add(indexpos);
						removeFlag = true;
					}
					else {
						// 这个位置留给情况2
						// 索引的处理放在后面，
					}
				}
				// 在这里构建下一列的索引
				// 并且该自增1的在这里判断
				if (oneFlag)
				{
					exQualIndex++;
					oneFlag = false;
				}
				if (zeroFlag)
				{
					qualIndex++;
					zeroFlag = false;
				}
				
				// 在这里构建下一列的索引
				// 改一下，放在后面需要进行一步删除操作
				c.addAll(d);
				
				if (removeFlag == true)
				{
					// 删除结尾为3的序列并且同时改变索引表
					int offset = 0;
					Collections.sort(removeIndexRe);
					for (Integer ins : removeIndexRe)
					{
						pre = posIndex.size();
						cur = ins - offset;
						// 删除序列操作
						if (pre == cur)
						{
							// 加在末尾
							result.add(readsCurTemp.get(ins - offset));
							readsCurTemp.remove(ins - offset);
							
						}
						else if (pre<cur) {
							for (int i = 0; i < cur-pre; i++)
							{
								DeRead empty = new DeRead();
								result.add(empty);
								posIndex.add(result.size() - 1);
							}
							result.add(readsCurTemp.get(ins - offset));
							readsCurTemp.remove(ins - offset);
						}
						else {
							// 此时表明之前没有出去的序列要出去
							int pos = posIndex.get(cur);
							posIndex.remove(cur);
							result.set(pos, readsCurTemp.get(ins - offset));
							readsCurTemp.remove(ins - offset);
						}
						// 改变索引表
						int tempIndex = -1;
						for (int i = 0; i < c.size(); i++)
						{
							if ((ins - offset)<=c.get(i))
							{
								tempIndex = c.get(i)-1;
								c.set(i, tempIndex);
							}
//							else if ((ins - offset) == c.get(i))
//							{
//								c.remove(i);
//								i--;
//							}
//							else {
//								
//							}
						}
						
						offset++;
					}
					removeIndexRe.clear();
					removeFlag = false;
				}
				
				// 在这里构建下一列的索引
				// 改一下，放在后面需要进行一步删除操作
				Index.add(c);
				
			}
			else
			{
				// 这里是针对该列为空的情况，单独设计
				ArrayList<Integer> emptyIndex = new ArrayList<>();
				Index.add(emptyIndex);
			}
			
		}
	return result;	
	}

	
	
	// 这里处理有一个问题就是，我把pbwt的变化也加入其中了。但是之前进行质量分数的压缩是没哟经过pbwt变化的质量分数的压缩。
	// 最后我们需要看看到底考没考虑最特殊的情况就是完全匹配上去的情况
	private ReadPbwtResult PBWTAlgo(ArrayList<ReadStruct> readsList, Integer[] start, Integer[] end)
	{
		ArrayList<Integer> a = new ArrayList<Integer>(); // 存储为0的值
		ArrayList<Integer> b = new ArrayList<Integer>(); // 存储为1的值
		ArrayList<Integer> c = new ArrayList<Integer>(); // 存储新加入的值，在这里，就是ReadElemEnum.START部分 liyang:应该是存储3

		ArrayList<Character> qualA = new ArrayList<Character>();
		// ArrayList<Character> qualB = new ArrayList<Character>();
		// ArrayList<Character> qualC = new ArrayList<Character>();
		ArrayList<Integer> removeIndex = new ArrayList<Integer>();

		ArrayList<ArrayList<Integer>> listsPBWT = new ArrayList<ArrayList<Integer>>();
		ArrayList<Character> listsPbwtQual = new ArrayList<Character>();
		ArrayList<ArrayList<String>> exceptionList = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<Character>> exceptionListQual = new ArrayList<ArrayList<Character>>();

		ArrayList<ReadStruct> readsCurrList = new ArrayList<ReadStruct>();
		ReadPbwtResult result = new ReadPbwtResult();

		// 检测一下是否正常排序
		for (int i = 0; i < start.length; i++)
		{
			if (i > 1 && start[i] < start[i - 1])
			{
				System.out.println("It doesn't sort by start");
				System.exit(0);
			}
		}

		int enter = 0, currPosDist = 0;
		int readsIndex = 0;
		int endindex = end.length - 1;
		int tmp = -1;
		// 这是为了防止，最后一个reads由于不等长的原因导致其并非是最后的坐标，但是这样遍历一遍是不可行的（我认为）
		for (int i = end.length - 1; i >= 0; i--)
		{
			if (end[i] >= tmp)
			{
				tmp = end[i];
				endindex = i;
			}
		}

		// 真正的pbwt处理过程，位置信息是从1开始的，所以说不可能是0
		for (int pos = start[0]; pos <= end[endindex] + 1; pos++)
		{
			// 这了不明白，为什么不能够等于0呢，为什么需要这么处理呢
			if (pos == 0)
			{
				pos = start[++enter] - 1;
				continue;
			}

			if (currPosDist == 0) // liyang：这里是每条新的序列才能够进行的，只有currposdist减到0,才能加入下一条序列
			{
				while (enter < end.length && start[enter] == pos) // 有新元素进队列的情形 liyang：这里这个设计非常的完美pos一直加，而currposdist一直减
				{
					readsCurrList.add(readsList.get(readsIndex++)); // liyang:序列从横向的排列变成了纵向的排列，把所有的开头位置一样的先存储一下
					enter++;
				}
				if (enter < end.length)
				{
					currPosDist = start[enter] - pos;
				}
			}
			currPosDist--;
			if (!readsCurrList.isEmpty())
			{
				ArrayList<Integer> listPBWT = new ArrayList<Integer>();
				ArrayList<String> exception = new ArrayList<String>();
				ArrayList<Character> exceptionQual = new ArrayList<Character>();
				// ArrayList<Character> listPbwtQual = new ArrayList<Character>();
				// ArrayList<ArrayList<String>> exceptionList = new
				// ArrayList<ArrayList<String>>();
				// ArrayList<ArrayList<String>> exceptionListQual = new
				// ArrayList<ArrayList<String>>();
				for (int i = 0; i < readsCurrList.size(); i++)
				{
					// 一条read上的相对位置，从0开始到最后length-1
					int curVal = readsCurrList.get(i).getReads().get(pos - readsCurrList.get(i).getStartAlignment());
					char curValQual = 0; // 这里初值设置为0非常重要，应对是3的情况
					if (curVal == 1 || curVal == 0)
					{
						curValQual = readsCurrList.get(i).getReadQuality()
								.charAt(pos - readsCurrList.get(i).getStartAlignment());
					}
					// 处理第一列，就是之前并没有碱基的情况
					if (readsCurrList.get(i).getStartAlignment() == pos)
					{
						if (curVal == 0)
						{
							qualA.add(curValQual);
						} else
						{
							exception.add(readsCurrList.get(i).getException().get(0));
							exBackUp.add(readsCurrList.get(i).getException().get(0));
							readsCurrList.get(i).getException().remove(0);
							// qualB.add(curValQual);
							exceptionQual.add(curValQual);
							exListLength++;
						}
						c.add(curVal);
						// qualC.add(curValQual);
					} else
					{
						int preVal = readsCurrList.get(i).getReads()
								.get(pos - readsCurrList.get(i).getStartAlignment() - 1);
						if (preVal == 0)
						{
							// 这样的话0,1,3三种情况都有了
							// 如果是3的话没有质量分数，说明横着看read已经结束了，所以做法就是删除该read
							// 如果是0的话将质量分数加入到A中，
							// 如果是1的话需要将异常值和异常信息放到一个特殊的容器中，这样这两个信息就相当于提前了
							// 暂时不要这部分，采用另一种策略详见12.16
							if (curVal == ReadElemEnum.END.ordinal())
							{
								removeIndex.add(i);
							} else if (curVal == 0)
							{
								qualA.add(curValQual);
							} else
							{
								// ArrayList<String> exception = new ArrayList<String>();
								exception.add(readsCurrList.get(i).getException().get(0));
								exBackUp.add(readsCurrList.get(i).getException().get(0));
								readsCurrList.get(i).getException().remove(0);
								// qualB.add(curValQual);
								exceptionQual.add(curValQual);
								exListLength++;

							}
							a.add(curVal); // crazy:这里注意一下当为结束符号3的时候同样也会加入到其中
							// qualA.add(curValQual);

						} else if (preVal == 1)
						{
							if (curVal == ReadElemEnum.END.ordinal())
							{
								removeIndex.add(i);
							} else if (curVal == 0)
							{
								qualA.add(curValQual);
							} else
							{
								// ArrayList<String> exception = new ArrayList<String>();
								exception.add(readsCurrList.get(i).getException().get(0));
								exBackUp.add(readsCurrList.get(i).getException().get(0));
								readsCurrList.get(i).getException().remove(0);
								// qualB.add(curValQual);
								exceptionQual.add(curValQual);
								exListLength++;
							}
							b.add(curVal);
							// qualB.add(curValQual);
							// 这里加入异常信息,一个位点的位置,就算是一个异常信息了
							// ArrayList<String> exception = new ArrayList<String>();
							// //liyang：当前点位是0,前一个点位是1,但是却要把当前点位的东西加入
							// ArrayList<ArrayList<String>> exceptionList = new
							// ArrayList<ArrayList<String>>();
							// ArrayList<ArrayList<String>> exceptionListQual = new
							// ArrayList<ArrayList<String>>();
							// System.out.println("startAlignment\t"+readsCurrList.get(i).getStartAlignment()+"\tException
							// size:\t"+readsCurrList.get(i).getException().size());
							// 如果是空就会报错
							// exception.add(readsCurrList.get(i).getException().get(0));
							// exceptionList.add(exception);
							// 这里也把质量数也加入进去
							// ArrayList<String> exceptionQual = new ArrayList<String>();
							// exceptionQual.add(readsCurrList.get(i).getExceptionQuality().get(0));
							// exceptionListQual.add(exceptionQual);
							// readsCurrList.get(i).getException().remove(0);
							// readsCurrList.get(i).getExceptionQuality().remove(0);
						} else
						{
							System.out.println("Invalid ELSE readsCurrList process..");
						}
					}
				}
				// 异常值加入
				exceptionList.add(exception);
				// read序列加入
				for (Integer ins : a)
				{
					listPBWT.add(ins);
				}
				for (Integer ins : b)
				{
					listPBWT.add(ins);
				}
				for (Integer ins : c)
				{
					listPBWT.add(ins);
				}
				listsPBWT.add(listPBWT);

				// 这里应该先是稀疏化处理，然后才是均质化处理，
				// 这里丢给质量分数处理函数，处理。应该是单步在这里进行完成的
				int avg = 0;
				for (Character chr : qualA)
				{
					avg += chr;
				}
				// 这里注意，如果是大小为0的话，也需要加上一个0，站住这个位置
				if (qualA.size() == 0)
				{
					listsPbwtQual.add(null);
				} else
				{
					listsPbwtQual.add((char) (avg / qualA.size()));
				}
				// for(Character chr : qualA){
				// if(qualA.size() == 0){
				// listPbwtQual.add(chr);
				// }else{
				// char chT= (char)(avg/qualA.size()); //liyang：将数字转换为了字符
				// listPbwtQual.add(chT);
				// }
				// }
				// 异常质量分数
				exceptionListQual.add(exceptionQual);
				// for(Character chr : qualB){
				// listPbwtQual.add(chr);
				// }
				// for(Character chr : qualC){
				// listPbwtQual.add(chr);
				// }
				a.clear();
				b.clear();
				c.clear();
				qualA.clear();
				// qualB.clear();
				// qualC.clear();

				// 这里是一个移除操作，因为每次都要删除第一个，所以需要不断的偏移求相对位置
				int offset = 0;
				for (Integer ins : removeIndex)
				{
					readsCurrList.remove(ins - offset);
					offset++;
				}
				removeIndex.clear();
			}
			// 这里需要补全代码
			result.setListsPBWT(listsPBWT);
			result.setListsExcep(exceptionList);
			result.setListExQual(exceptionListQual);
			result.setListsQual(listsPbwtQual);

		}
		System.out.println();
		System.out.println("Original PBWT(pbwtResult.length):\t");
		for (int i = 0; i < listsPBWT.size(); i++)
		{
			System.out.println(listsPBWT.get(i).toString());
		}

		return result;
	}


	private ReadPbwtResult PBWTAlgo2(ArrayList<ReadStruct> readsList, Integer[] start, Integer[] end)
	{
		ArrayList<Integer> a = new ArrayList<Integer>(); // 存储为0的值
		ArrayList<Integer> b = new ArrayList<Integer>(); // 存储为1的值
		ArrayList<Integer> c = new ArrayList<Integer>(); // 存储新加入的值，在这里，就是ReadElemEnum.START部分 liyang:应该是存储3
		
		// 用于构建ak数组
		ArrayList<Integer> A = new ArrayList<Integer>();
		ArrayList<Integer> B = new ArrayList<Integer>();
		ArrayList<Integer> C = new ArrayList<Integer>();
		ArrayList<Character> qualA = new ArrayList<Character>();

		ArrayList<ArrayList<Integer>> listsPBWT = new ArrayList<ArrayList<Integer>>();
		ArrayList<Character> listsPbwtQual = new ArrayList<Character>();
		ArrayList<ArrayList<String>> exceptionList = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<Character>> exceptionListQual = new ArrayList<ArrayList<Character>>();

		ArrayList<ArrayList<Integer>> indexes = new ArrayList<>();	// 用于存储所有的ak
		
		ArrayList<ReadStruct> readsCurrList = new ArrayList<ReadStruct>();
		ReadPbwtResult result = new ReadPbwtResult();

		// 检测一下是否正常排序
		for (int i = 0; i < start.length; i++)
		{
			if (i > 1 && start[i] < start[i - 1])
			{
				System.out.println("It doesn't sort by start");
				System.exit(0);
			}
		}

		int enter = 0, currPosDist = 0;
		int readsIndex = 0;
		int endindex = end.length - 1;
		int tmp = -1;
		// 这是为了防止，最后一个reads由于不等长的原因导致其并非是最后的坐标，但是这样遍历一遍是不可行的（我认为）
		for (int i = end.length - 1; i >= 0; i--)
		{
			if (end[i] >= tmp)
			{
				tmp = end[i];
				endindex = i;
			}
		}

		// 真正的pbwt处理过程，位置信息是从1开始的，所以说不可能是0
		int pos = 0;
		boolean emptyFlag = true;	// 用于监测collection是非为空
		for (pos = start[0]; pos <= end[endindex] + 1; pos++)
		{
			// 这了不明白，为什么不能够等于0呢，为什么需要这么处理呢
			if (pos == 0)
			{
				pos = start[++enter] - 1;
				continue;
			}

//			这里的目的就是为了初始化ak数组，这个顺序不能改
			if(readsCurrList.isEmpty())
			{
				emptyFlag = true;
			}
			if (currPosDist == 0) // liyang：这里是每条新的序列才能够进行的，只有currposdist减到0,才能加入下一条序列
			{
				while (enter < end.length && start[enter] == pos) // 有新元素进队列的情形 liyang：这里这个设计非常的完美pos一直加，而currposdist一直减
				{
					readsCurrList.add(readsList.get(readsIndex++)); // liyang:序列从横向的排列变成了纵向的排列，把所有的开头位置一样的先存储一下
					enter++;
				}					
				if (enter < end.length)
				{
					currPosDist = start[enter] - pos;
				}
			}
			currPosDist--;

//			如果不为空，进行下面操作。容器中有序列
			if(!readsCurrList.isEmpty())
			{
				ArrayList<Integer> listPbwt = new ArrayList<Integer>();
				ArrayList<String> exception = new ArrayList<String>();
				ArrayList<Character> exceptionQual = new ArrayList<Character>();
				ArrayList<Integer> index = new ArrayList<>();
				
				// 初始化操作，可能会出现多次初始化的情况
				if(emptyFlag == true)
				{
					// 在这里进行初值化操作我认为比较合适
					// 读取此时容器长度，然后依次遍历，将a0赋值
					ArrayList<Integer> a0 = new ArrayList<>();
					for(int i=0 ;i <readsCurrList.size(); i++)
					{
						a0.add(i);
					}
					indexes.add(a0);
				}
				// 应对插入更新问题
				// 如果进行之前的初始化操作，这个if不会进入。
				int maxSize = indexes.size()-1;	
				int maxValue = indexes.get(maxSize).size()-1;
				int difference = readsCurrList.size()-indexes.get(maxSize).size();
				if (emptyFlag == false && difference > 0)
				{
					// 更新ak数组
					for(int i=0; i<difference; i++)
					{
						maxValue+=1;
						indexes.get(maxSize).add(maxValue);
					}
					
				}
				
				emptyFlag = false;	// 只有第一次为空进这个循环才会执行这个操作，其他时候不进入
				char curValQual = ' ';
				int curValue = -1;
				int curIndex = -1;
				// 构造ak+1和k位点的dpbwt
				for (int i = 0; i < readsCurrList.size(); i++)
				{	
					// 一条read上的相对位置，从0开始到最后length-1
//					curVal = readsCurrList.get(i).getReads().get(pos - readsCurrList.get(i).getStartAlignment());
					curValQual = 0; // 这里初值设置为0非常重要，应对是3的情况
					
					curIndex = indexes.get(indexes.size() - 1).get(i);
					curValue = readsCurrList.get(curIndex).getReads()
							.get(pos - readsCurrList.get(curIndex).getStartAlignment());
					
					if (curValue == 1 || curValue == 0)
					{
						curValQual = readsCurrList.get(curIndex).getReadQuality()
								.charAt(pos - readsCurrList.get(curIndex).getStartAlignment());
					}
					
					if (curValue == 0)
					{
						A.add(curIndex);
						qualA.add(curValQual);
						
						listPbwt.add(curValue);
					}
					else if (curValue == 1)
					{
						B.add(curIndex);
						listPbwt.add(curValue);
//						System.out.println(pos);
						exception.add(readsCurrList.get(curIndex).getException().get(0));
						exBackUp.add(readsCurrList.get(curIndex).getException().get(0));
						readsCurrList.get(curIndex).getException().remove(0);
						exceptionQual.add(curValQual);
						exListLength++;
					}
					else
					{
						C.add(curIndex);
						listPbwt.add(ReadElemEnum.END.ordinal());
					}
					
				}

				for (Integer ins : A)
				{
					index.add(ins);
				}
				for (Integer ins : B)
				{
					index.add(ins);
				}
				
				// 异常值加入
				exceptionList.add(exception);
				// read序列加入
				listsPBWT.add(listPbwt);
				// 正常质量分数加入
				// 这里应该先是稀疏化处理，然后才是均质化处理，
				// 这里丢给质量分数处理函数，处理。应该是单步在这里进行完成的
				int avg = 0;
				for (Character chr : qualA)
				{
					avg += chr;
				}
				// 这里注意，如果是大小为0的话，也需要加上一个0，站住这个位置
				if (qualA.size() == 0)
				{
					listsPbwtQual.add(null);
				} else
				{
					listsPbwtQual.add((char) (avg / qualA.size()));
						
				}
				// 异常质量分数加入
				exceptionListQual.add(exceptionQual);

				a.clear();
				b.clear();
				c.clear();
				A.clear();
				B.clear();
//				C.clear();
				qualA.clear();
				// 这里是一个移除操作，因为每次都要删除第一个，所以需要不断的偏移求相对位置
				// 这里改一下，删除更新，更新ak+1这个数组
				int offset = 0;
				int tempIndex = -1;
				Collections.sort(C);
				for (Integer ins : C)
				{
					readsCurrList.remove(ins - offset); // 删除序列
					for (int i = 0; i < index.size(); i++)
					{
						if ((ins - offset) <= index.get(i))
						{
							tempIndex = index.get(i) - 1;
							index.set(i, tempIndex);
						}
						// else if(ins == index.get(i))
						// {
						// index.remove(i);
						// i--;
						// }
						else
						{
							// 什么也不做
						}
					}
					offset++;
				}
				C.clear();
				indexes.add(index);
			}
			// 这里需要补全代码
			result.setListsPBWT(listsPBWT);
			result.setListsExcep(exceptionList);
			result.setListExQual(exceptionListQual);
			result.setListsQual(listsPbwtQual);
			
			readsQualLength++;

		}
		tempIndexes = indexes;	// 先这样最简单化处理
		result.setIndexes(indexes); // indexes应该单独加入，因为之后修改不会全部保存
//		System.out.println("listsPBWT size:"+result.getListsPBWT().size());
		System.out.println("\n"+"pos:"+(pos-start[0]));
		System.out.println();
		System.out.println("Original PBWT(pbwtResult.length):\t");
		// for(int i=0; i<listsPBWT.size(); i++)
		// {
		// System.out.println(listsPBWT.get(i).toString());
		// }

		return result;
	}
	
	/**
	 * 用于局部解压缩，引入gap为2的情况
	 * @param readsList
	 * @param start
	 * @param end
	 * @return
	 */
	private ReadPbwtResult PBWTAlgo3(ArrayList<ReadStruct> readsList, Integer[] start, Integer[] end)
	{
		ArrayList<Integer> a = new ArrayList<Integer>(); // 存储为0的值
		ArrayList<Integer> b = new ArrayList<Integer>(); // 存储为1的值
		ArrayList<Integer> c = new ArrayList<Integer>(); // 存储新加入的值，在这里，就是ReadElemEnum.START部分 liyang:应该是存储3
		
		// 用于构建ak数组
		ArrayList<Integer> A = new ArrayList<Integer>();
		ArrayList<Integer> B = new ArrayList<Integer>();
		ArrayList<Integer> C = new ArrayList<Integer>();
		ArrayList<Character> qualA = new ArrayList<Character>();

		ArrayList<ArrayList<Integer>> listsPBWT = new ArrayList<ArrayList<Integer>>();
		ArrayList<Character> listsPbwtQual = new ArrayList<Character>();
		ArrayList<ArrayList<String>> exceptionList = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<Character>> exceptionListQual = new ArrayList<ArrayList<Character>>();

		ArrayList<ArrayList<Integer>> indexes = new ArrayList<>();	// 用于存储所有的ak
		
		ArrayList<ReadStruct> readsCurrList = new ArrayList<ReadStruct>();
		ReadPbwtResult result = new ReadPbwtResult();

		// 检测一下是否正常排序
		int endindex = 0;
		for (int i = 0; i < start.length; i++)
		{
			if (i > 1 && start[i] < start[i - 1])
			{
				System.out.println("It doesn't sort by start");
				System.exit(0);
			}
			if (end[i] > end[endindex])
			{
				endindex = i;
			}
		}

		int enter = 0, currPosDist = 0;
		int readsIndex = 0;
//		int endindex = end.length - 1;
//		int tmp = -1;
		// 这是为了防止，最后一个reads由于不等长的原因导致其并非是最后的坐标，但是这样遍历一遍是不可行的（我认为）
		// 这里可以并入检查是否对其，我们同时记录最大结束位置就行
//		for (int i = end.length - 1; i >= 0; i--)
//		{
//			if (end[i] >= tmp)
//			{
//				tmp = end[i];
//				endindex = i;
//			}
//		}

		// 真正的pbwt处理过程，位置信息是从1开始的，所以说不可能是0
		int pos = 0;
		boolean emptyFlag = true;	// 用于监测collection是非为空
		for (pos = start[0]; pos <= end[endindex]; pos++)
		{
			// 这了不明白，为什么不能够等于0呢，为什么需要这么处理呢
			if (pos == 0)
			{
				pos = start[++enter] - 1;
				continue;
			}

//			这里的目的就是为了初始化ak数组，这个顺序不能改
			if(readsCurrList.isEmpty())
			{
				emptyFlag = true;
			}
			if (currPosDist == 0) // liyang：这里是每条新的序列才能够进行的，只有currposdist减到0,才能加入下一条序列
			{
				while (enter < end.length && start[enter] == pos) // 有新元素进队列的情形 liyang：这里这个设计非常的完美pos一直加，而currposdist一直减
				{
					readsCurrList.add(readsList.get(readsIndex++)); // liyang:序列从横向的排列变成了纵向的排列，把所有的开头位置一样的先存储一下
					enter++;
				}					
				if (enter < end.length)
				{
					currPosDist = start[enter] - pos;
				}
			}
			currPosDist--;

//			如果不为空，进行下面操作。容器中有序列
			ArrayList<Integer> listPbwt = new ArrayList<Integer>();
			ArrayList<String> exception = new ArrayList<String>();
			ArrayList<Character> exceptionQual = new ArrayList<Character>();
			ArrayList<Integer> index = new ArrayList<>();
			if(!readsCurrList.isEmpty())
			{				
				// 初始化操作，可能会出现多次初始化的情况
				if(emptyFlag == true)
				{
					// 在这里进行初值化操作我认为比较合适
					// 读取此时容器长度，然后依次遍历，将a0赋值
					ArrayList<Integer> a0 = new ArrayList<>();
					for(int i=0 ;i <readsCurrList.size(); i++)
					{
						a0.add(i);
					}
					indexes.add(a0);
				}
				// 应对插入更新问题
				// 如果进行之前的初始化操作，这个if不会进入。
				int maxSize = indexes.size()-1;	
				int maxValue = indexes.get(maxSize).size()-1;
				int difference = readsCurrList.size()-indexes.get(maxSize).size();
				if (emptyFlag == false && difference > 0)
				{
					// 更新ak数组
					for(int i=0; i<difference; i++)
					{
						maxValue+=1;
						indexes.get(maxSize).add(maxValue);
					}
					
				}
				
				emptyFlag = false;	// 只有第一次为空进这个循环才会执行这个操作，其他时候不进入
				char curValQual = ' ';
				int curValue = -1;
				int curIndex = -1;
				// 构造ak+1和k位点的dpbwt
				for (int i = 0; i < readsCurrList.size(); i++)
				{	
					// 一条read上的相对位置，从0开始到最后length-1
//					curVal = readsCurrList.get(i).getReads().get(pos - readsCurrList.get(i).getStartAlignment());
					curValQual = 0; // 这里初值设置为0非常重要，应对是3的情况
					
					curIndex = indexes.get(indexes.size() - 1).get(i);
					curValue = readsCurrList.get(curIndex).getReads()
							.get(pos - readsCurrList.get(curIndex).getStartAlignment());
					
					if (curValue == 1 || curValue == 0)
					{
						curValQual = readsCurrList.get(curIndex).getReadQuality()
								.charAt(pos - readsCurrList.get(curIndex).getStartAlignment());
					}
					
					if (curValue == 0)
					{
						A.add(curIndex);
						qualA.add(curValQual);
						
						listPbwt.add(curValue);
					}
					else if (curValue == 1)
					{
						B.add(curIndex);
						listPbwt.add(curValue);
//						System.out.println(pos);
						exception.add(readsCurrList.get(curIndex).getException().get(0));
						exBackUp.add(readsCurrList.get(curIndex).getException().get(0));
						readsCurrList.get(curIndex).getException().remove(0);
						exceptionQual.add(curValQual);
						exListLength++;
					}
					else
					{
						C.add(curIndex);
						listPbwt.add(ReadElemEnum.END.ordinal());
					}
					
				}

				for (Integer ins : A)
				{
					index.add(ins);
				}
				for (Integer ins : B)
				{
					index.add(ins);
				}
				
				// 异常值加入
				exceptionList.add(exception);
				// read序列加入
				listsPBWT.add(listPbwt);
				// 正常质量分数加入
				// 这里应该先是稀疏化处理，然后才是均质化处理，
				// 这里丢给质量分数处理函数，处理。应该是单步在这里进行完成的
				int avg = 0;
				for (Character chr : qualA)
				{
					avg += chr;
				}
				// 这里注意，如果是大小为0的话，也需要加上一个0，站住这个位置
				if (qualA.size() == 0)
				{
					listsPbwtQual.add(null); // 这里设置null
				} else
				{
					listsPbwtQual.add((char) (avg / qualA.size()));
						
				}
				// 异常质量分数加入
				exceptionListQual.add(exceptionQual);

				a.clear();
				b.clear();
				c.clear();
				A.clear();
				B.clear();
//				C.clear();
				qualA.clear();
				// 这里是一个移除操作，因为每次都要删除第一个，所以需要不断的偏移求相对位置
				// 这里改一下，删除更新，更新ak+1这个数组
				int offset = 0;
				int tempIndex = -1;
				Collections.sort(C);
				for (Integer ins : C)
				{
					readsCurrList.remove(ins - offset); // 删除序列
					for (int i = 0; i < index.size(); i++)
					{
						if ((ins - offset) <= index.get(i))
						{
							tempIndex = index.get(i) - 1;
							index.set(i, tempIndex);
						}
						// else if(ins == index.get(i))
						// {
						// index.remove(i);
						// i--;
						// }
						else
						{
							// 什么也不做
						}
					}
					offset++;
				}
				C.clear();
				if(!index.isEmpty())
				{
					indexes.add(index);
				}
				
			}
			// 这里添加出现gap的情况,异常值，异常分数都是空，但是正常质量分数这里需要设置为null
			else
			{
//				listPbwt.add(2);
				indexes.add(index);
				listsPBWT.add(listPbwt);
				listsPbwtQual.add(null);
				exceptionListQual.add(exceptionQual);
				exceptionList.add(exception);
				
			}
			// 这里需要补全代码
			result.setListsPBWT(listsPBWT);
			result.setListsExcep(exceptionList);
			result.setListExQual(exceptionListQual);
			result.setListsQual(listsPbwtQual);
			
			readsQualLength++;

		}
		tempIndexes = indexes;	// 先这样最简单化处理
		result.setIndexes(indexes); // indexes应该单独加入，因为之后修改不会全部保存
//		System.out.println("listsPBWT size:"+result.getListsPBWT().size());
		System.out.println("\n"+"pos:"+(pos-start[0]));
		System.out.println();
		System.out.println("Original PBWT(pbwtResult.length):\t");
		// for(int i=0; i<listsPBWT.size(); i++)
		// {
		// System.out.println(listsPBWT.get(i).toString());
		// }

		return result;
	}

	/**
	 * 局部解压缩，引入gap为2同时异常值变换位置
	 * @param readsList
	 * @param start
	 * @param end
	 * @return
	 */
	private ReadPbwtResult PBWTAlgo4(ArrayList<ReadStruct> readsList, Integer[] start, Integer[] end)
	{
		ArrayList<Integer> a = new ArrayList<Integer>(); // 存储为0的值
		ArrayList<Integer> b = new ArrayList<Integer>(); // 存储为1的值
		ArrayList<Integer> c = new ArrayList<Integer>(); // 存储新加入的值，在这里，就是ReadElemEnum.START部分 liyang:应该是存储3
		
		// 用于构建ak数组
		ArrayList<Integer> A = new ArrayList<Integer>();
		ArrayList<Integer> B = new ArrayList<Integer>();
		ArrayList<Integer> C = new ArrayList<Integer>();
		ArrayList<Character> qualA = new ArrayList<Character>();
		ArrayList<Character> qualB = new ArrayList<Character>();

		ArrayList<ArrayList<Integer>> listsPBWT = new ArrayList<ArrayList<Integer>>();
		ArrayList<Character> listsPbwtQual = new ArrayList<Character>();
		ArrayList<ArrayList<String>> exceptionList = new ArrayList<ArrayList<String>>();
//		ArrayList<ArrayList<Character>> exceptionListQual = new ArrayList<ArrayList<Character>>();
		ArrayList<Character> exceptionQual = new ArrayList<Character>();
		ArrayList<ArrayList<Integer>> indexes = new ArrayList<>();	// 用于存储所有的ak
		
		ArrayList<ReadStruct> readsCurrList = new ArrayList<ReadStruct>();
		ReadPbwtResult result = new ReadPbwtResult();

		// 检测一下是否正常排序
		int endindex = 0;
		for (int i = 0; i < start.length; i++)
		{
			if (i > 1 && start[i] < start[i - 1])
			{
				System.out.println("It doesn't sort by start");
				System.exit(0);
			}
			if (end[i] > end[endindex])
			{
				endindex = i;
			}
		}

		int enter = 0, currPosDist = 0;
		int readsIndex = 0;
//		int endindex = end.length - 1;
//		int tmp = -1;
		// 这是为了防止，最后一个reads由于不等长的原因导致其并非是最后的坐标，但是这样遍历一遍是不可行的（我认为）
		// 这里可以并入检查是否对其，我们同时记录最大结束位置就行
//		for (int i = end.length - 1; i >= 0; i--)
//		{
//			if (end[i] >= tmp)
//			{
//				tmp = end[i];
//				endindex = i;
//			}
//		}

		// 真正的pbwt处理过程，位置信息是从1开始的，所以说不可能是0
		int pos = 0;
		boolean emptyFlag = true;	// 用于监测collection是非为空
		for (pos = start[0]; pos <= end[endindex]; pos++)
		{
			// 这了不明白，为什么不能够等于0呢，为什么需要这么处理呢
			if (pos == 0)
			{
				pos = start[++enter] - 1;
				continue;
			}

//			这里的目的就是为了初始化ak数组，这个顺序不能改
			if(readsCurrList.isEmpty())
			{
				emptyFlag = true;
			}
			if (currPosDist == 0) // liyang：这里是每条新的序列才能够进行的，只有currposdist减到0,才能加入下一条序列
			{
				while (enter < end.length && start[enter] == pos) // 有新元素进队列的情形 liyang：这里这个设计非常的完美pos一直加，而currposdist一直减
				{
					readsCurrList.add(readsList.get(readsIndex++)); // liyang:序列从横向的排列变成了纵向的排列，把所有的开头位置一样的先存储一下
					enter++;
				}					
				if (enter < end.length)
				{
					currPosDist = start[enter] - pos;
				}
			}
			currPosDist--;

//			如果不为空，进行下面操作。容器中有序列
			ArrayList<Integer> listPbwt = new ArrayList<Integer>();
			ArrayList<String> exception = new ArrayList<String>();
			ArrayList<Integer> index = new ArrayList<>();
			if(!readsCurrList.isEmpty())
			{				
				// 初始化操作，可能会出现多次初始化的情况
				if(emptyFlag == true)
				{
					// 在这里进行初值化操作我认为比较合适
					// 读取此时容器长度，然后依次遍历，将a0赋值
					ArrayList<Integer> a0 = new ArrayList<>();
					for(int i=0 ;i <readsCurrList.size(); i++)
					{
						a0.add(i);
					}
					indexes.add(a0);
				}
				// 应对插入更新问题
				// 如果进行之前的初始化操作，这个if不会进入。
				int maxSize = indexes.size()-1;	
				int maxValue = indexes.get(maxSize).size()-1;
				int difference = readsCurrList.size()-indexes.get(maxSize).size();
				if (emptyFlag == false && difference > 0)
				{
					// 更新ak数组
					for(int i=0; i<difference; i++)
					{
						maxValue+=1;
						indexes.get(maxSize).add(maxValue);
					}
					
				}
				
				emptyFlag = false;	// 只有第一次为空进这个循环才会执行这个操作，其他时候不进入
				char curValQual = ' ';
				int curValue = -1;
				int curIndex = -1;
				// 构造ak+1和k位点的dpbwt
				for (int i = 0; i < readsCurrList.size(); i++)
				{	
					// 一条read上的相对位置，从0开始到最后length-1
//					curVal = readsCurrList.get(i).getReads().get(pos - readsCurrList.get(i).getStartAlignment());
					curValQual = 0; // 这里初值设置为0非常重要，应对是3的情况
					
					curIndex = indexes.get(indexes.size() - 1).get(i);
					curValue = readsCurrList.get(curIndex).getReads()
							.get(pos - readsCurrList.get(curIndex).getStartAlignment());
					
					if (curValue == 1 || curValue == 0)
					{
						curValQual = readsCurrList.get(curIndex).getReadQuality()
								.charAt(pos - readsCurrList.get(curIndex).getStartAlignment());
					}
					
					if (curValue == 0)
					{
						A.add(curIndex);
						qualA.add(curValQual);
						listPbwt.add(curValue);
					}
					else if (curValue == 1)
					{
						B.add(curIndex);
						listPbwt.add(curValue);
//						System.out.println(pos);
						exception.add(readsCurrList.get(curIndex).getException().get(0));
						exBackUp.add(readsCurrList.get(curIndex).getException().get(0));
						readsCurrList.get(curIndex).getException().remove(0);
//						exceptionQual.add(curValQual);
						qualB.add(curValQual);
						exListLength++;
					}
					else
					{
						C.add(curIndex);
						listPbwt.add(ReadElemEnum.END.ordinal());
					}
					
				}

				for (Integer ins : A)
				{
					index.add(ins);
				}
				for (Integer ins : B)
				{
					index.add(ins);
				}
				
				// 异常值加入
				if (!exception.isEmpty())
				{
					exceptionList.add(exception);
				}
				// read序列加入
				listsPBWT.add(listPbwt);
				// 正常质量分数加入
				// 这里应该先是稀疏化处理，然后才是均质化处理，
				// 这里丢给质量分数处理函数，处理。应该是单步在这里进行完成的
				int avg = 0;
				int Exavg = 0;
				for (Character chr : qualA)
				{
					avg += chr;
				}
				// 这里注意，如果是大小为0的话，也需要加上一个0，站住这个位置
				if (qualA.size() != 0)
				{
					listsPbwtQual.add((char) (avg / qualA.size()));
				} 
				for (Character chr : qualB)
				{
					Exavg += chr;
				}
				if (qualB.size() != 0)
				{
					exceptionQual.add((char) (Exavg / qualB.size()));
				}

				a.clear();
				b.clear();
				c.clear();
				A.clear();
				B.clear();
//				C.clear();
				qualA.clear();
				qualB.clear();
				// 这里是一个移除操作，因为每次都要删除第一个，所以需要不断的偏移求相对位置
				// 这里改一下，删除更新，更新ak+1这个数组
				int offset = 0;
				int tempIndex = -1;
				Collections.sort(C);
				for (Integer ins : C)
				{
					readsCurrList.remove(ins - offset); // 删除序列
					for (int i = 0; i < index.size(); i++)
					{
						if ((ins - offset) <= index.get(i))
						{
							tempIndex = index.get(i) - 1;
							index.set(i, tempIndex);
						}
						// else if(ins == index.get(i))
						// {
						// index.remove(i);
						// i--;
						// }
						else
						{
							// 什么也不做
						}
					}
					offset++;
				}
				C.clear();
				if(!index.isEmpty())
				{
					indexes.add(index);
				}
				
			}
			// 这里添加出现gap的情况,异常值，异常分数都是空，但是正常质量分数这里需要设置为null
			else
			{
//				listPbwt.add(2);
				indexes.add(index);
				listsPBWT.add(listPbwt);
//				listsPbwtQual.add(null);
//				exceptionListQual.add(exceptionQual);
//				exceptionList.add(exception);
				
			}
			readsQualLength++;

		}
		tempIndexes = indexes;	// 先这样最简单化处理
		result.setListsPBWT(listsPBWT);
		result.setListsQual(listsPbwtQual);
		result.setListExQual2(exceptionQual);
		result.setListsExcep(exceptionList);
		result.setIndexes(indexes); // indexes应该单独加入，因为之后修改不会全部保存
//		System.out.println("listsPBWT size:"+result.getListsPBWT().size());
		System.out.println("\n"+"pos:"+(pos-start[0]));
		System.out.println();
		System.out.println("Original PBWT(pbwtResult.length):\t");
		// for(int i=0; i<listsPBWT.size(); i++)
		// {
		// System.out.println(listsPBWT.get(i).toString());
		// }

		return result;
	}

	/**
	 * 全局压缩最终，异常值处理方式按照一维存储
	 * @param readsList
	 * @param start
	 * @param end
	 * @return
	 */
	private ReadPbwtResult PBWTAlgo5(ArrayList<ReadStruct> readsList, Integer[] start, Integer[] end)
	{
		ArrayList<Integer> a = new ArrayList<Integer>(); // 存储为0的值
		ArrayList<Integer> b = new ArrayList<Integer>(); // 存储为1的值
		ArrayList<Integer> c = new ArrayList<Integer>(); // 存储新加入的值，在这里，就是ReadElemEnum.START部分 liyang:应该是存储3
		
		// 用于构建ak数组
		ArrayList<Integer> A = new ArrayList<Integer>();
		ArrayList<Integer> B = new ArrayList<Integer>();
		ArrayList<Integer> C = new ArrayList<Integer>();
		ArrayList<Character> qualA = new ArrayList<Character>();
		ArrayList<Character> qualB = new ArrayList<Character>();

		ArrayList<ArrayList<Integer>> listsPBWT = new ArrayList<ArrayList<Integer>>();
		ArrayList<Character> listsPbwtQual = new ArrayList<Character>();
		ArrayList<ArrayList<String>> exceptionList = new ArrayList<ArrayList<String>>();
//		ArrayList<ArrayList<Character>> exceptionListQual = new ArrayList<ArrayList<Character>>();
		ArrayList<Character> exceptionQual = new ArrayList<Character>();
		ArrayList<String> exception2 = new ArrayList<>();
		ArrayList<ArrayList<Integer>> indexes = new ArrayList<>();	// 用于存储所有的ak
		
		ArrayList<ReadStruct> readsCurrList = new ArrayList<ReadStruct>();
		ReadPbwtResult result = new ReadPbwtResult();

		// 检测一下是否正常排序
		int endindex = 0;
		for (int i = 0; i < start.length; i++)
		{
			if (i > 1 && start[i] < start[i - 1])
			{
				System.out.println("It doesn't sort by start");
				System.exit(0);
			}
			if (end[i] > end[endindex])
			{
				endindex = i;
			}
		}

		int enter = 0, currPosDist = 0;
		int readsIndex = 0;

		// 真正的pbwt处理过程，位置信息是从1开始的，所以说不可能是0
		int pos = 0;
		boolean emptyFlag = true;	// 用于监测collection是非为空
		for (pos = start[0]; pos <= end[endindex]; pos++)
		{
			// 这了不明白，为什么不能够等于0呢，为什么需要这么处理呢
			if (pos == 0)
			{
				pos = start[++enter] - 1;
				continue;
			}

//			这里的目的就是为了初始化ak数组，这个顺序不能改
			if(readsCurrList.isEmpty())
			{
				emptyFlag = true;
			}
			if (currPosDist == 0) // liyang：这里是每条新的序列才能够进行的，只有currposdist减到0,才能加入下一条序列
			{
				while (enter < end.length && start[enter] == pos) // 有新元素进队列的情形 liyang：这里这个设计非常的完美pos一直加，而currposdist一直减
				{
					readsCurrList.add(readsList.get(readsIndex++)); // liyang:序列从横向的排列变成了纵向的排列，把所有的开头位置一样的先存储一下
					enter++;
				}					
				if (enter < end.length)
				{
					currPosDist = start[enter] - pos;
				}
			}
			currPosDist--;

//			如果不为空，进行下面操作。容器中有序列
			ArrayList<Integer> listPbwt = new ArrayList<Integer>();
			ArrayList<String> exception = new ArrayList<String>();
			ArrayList<Integer> index = new ArrayList<>();
			if(!readsCurrList.isEmpty())
			{				
				// 初始化操作，可能会出现多次初始化的情况
				if(emptyFlag == true)
				{
					// 在这里进行初值化操作我认为比较合适
					// 读取此时容器长度，然后依次遍历，将a0赋值
					ArrayList<Integer> a0 = new ArrayList<>();
					for(int i=0 ;i <readsCurrList.size(); i++)
					{
						a0.add(i);
					}
					indexes.add(a0);
				}
				// 应对插入更新问题
				// 如果进行之前的初始化操作，这个if不会进入。
				int maxSize = indexes.size()-1;	
				int maxValue = indexes.get(maxSize).size()-1;
				int difference = readsCurrList.size()-indexes.get(maxSize).size();
				if (emptyFlag == false && difference > 0)
				{
					// 更新ak数组
					for(int i=0; i<difference; i++)
					{
						maxValue+=1;
						indexes.get(maxSize).add(maxValue);
					}
					
				}
				
				emptyFlag = false;	// 只有第一次为空进这个循环才会执行这个操作，其他时候不进入
				char curValQual = ' ';
				int curValue = -1;
				int curIndex = -1;
				// 构造ak+1和k位点的dpbwt
				for (int i = 0; i < readsCurrList.size(); i++)
				{	
					// 一条read上的相对位置，从0开始到最后length-1
//					curVal = readsCurrList.get(i).getReads().get(pos - readsCurrList.get(i).getStartAlignment());
					curValQual = 0; // 这里初值设置为0非常重要，应对是3的情况
					
					curIndex = indexes.get(indexes.size() - 1).get(i);
					curValue = readsCurrList.get(curIndex).getReads()
							.get(pos - readsCurrList.get(curIndex).getStartAlignment());
					
					if (curValue == 1 || curValue == 0)
					{
						curValQual = readsCurrList.get(curIndex).getReadQuality()
								.charAt(pos - readsCurrList.get(curIndex).getStartAlignment());
					}
					
					if (curValue == 0)
					{
						A.add(curIndex);
						qualA.add(curValQual);
						listPbwt.add(curValue);
					}
					else if (curValue == 1)
					{
						B.add(curIndex);
						listPbwt.add(curValue);
//						System.out.println(pos);
//						exception.add(readsCurrList.get(curIndex).getException().get(0));
						exception2.add(readsCurrList.get(curIndex).getException().get(0));
//						exBackUp.add(readsCurrList.get(curIndex).getException().get(0));
						readsCurrList.get(curIndex).getException().remove(0);
//						exceptionQual.add(curValQual);
						qualB.add(curValQual);
//						exListLength++;
					}
					else
					{
						C.add(curIndex);
						listPbwt.add(ReadElemEnum.END.ordinal());
					}
					
				}

				for (Integer ins : A)
				{
					index.add(ins);
				}
				for (Integer ins : B)
				{
					index.add(ins);
				}
				
				// 异常值加入
//				if (!exception.isEmpty())
//				{
//					exceptionList.add(exception);
//				}
				// read序列加入
				listsPBWT.add(listPbwt);
				// 正常质量分数加入
				// 这里应该先是稀疏化处理，然后才是均质化处理，
				// 这里丢给质量分数处理函数，处理。应该是单步在这里进行完成的
				int avg = 0;
				int Exavg = 0;
				for (Character chr : qualA)
				{
					avg += chr;
				}
				// 这里注意，如果是大小为0的话，也需要加上一个0，站住这个位置
				if (qualA.size() != 0)
				{
					listsPbwtQual.add((char) (avg / qualA.size()));
				} 
				for (Character chr : qualB)
				{
					Exavg += chr;
				}
				if (qualB.size() != 0)
				{
					exceptionQual.add((char) (Exavg / qualB.size()));
				}

				a.clear();
				b.clear();
				c.clear();
				A.clear();
				B.clear();
//				C.clear();
				qualA.clear();
				qualB.clear();
				// 这里是一个移除操作，因为每次都要删除第一个，所以需要不断的偏移求相对位置
				// 这里改一下，删除更新，更新ak+1这个数组
				int offset = 0;
				int tempIndex = -1;
				Collections.sort(C);
				for (Integer ins : C)
				{
					readsCurrList.remove(ins - offset); // 删除序列
					for (int i = 0; i < index.size(); i++)
					{
						if ((ins - offset) <= index.get(i))
						{
							tempIndex = index.get(i) - 1;
							index.set(i, tempIndex);
						}
						// else if(ins == index.get(i))
						// {
						// index.remove(i);
						// i--;
						// }
						else
						{
							// 什么也不做
						}
					}
					offset++;
				}
				C.clear();
				if(!index.isEmpty())
				{
					indexes.add(index);
				}
				
			}
			// 这里添加出现gap的情况,异常值，异常分数都是空，但是正常质量分数这里需要设置为null
			else
			{
//				listPbwt.add(2);
				indexes.add(index);
				listsPBWT.add(listPbwt);
//				listsPbwtQual.add(null);
//				exceptionListQual.add(exceptionQual);
//				exceptionList.add(exception);
				
			}
			readsQualLength++;

		}
		tempIndexes = indexes;	// 先这样最简单化处理
		result.setListsPBWT(listsPBWT);
		result.setListsQual(listsPbwtQual);
		result.setListExQual2(exceptionQual);
//		result.setListsExcep(exceptionList);
		result.setListsExcep2(exception2);
		result.setIndexes(indexes); // indexes应该单独加入，因为之后修改不会全部保存
//		System.out.println("listsPBWT size:"+result.getListsPBWT().size());
		System.out.println("\n"+"pos:"+(pos-start[0]));
		System.out.println();
		System.out.println("Original PBWT(pbwtResult.length):\t");
		// for(int i=0; i<listsPBWT.size(); i++)
		// {
		// System.out.println(listsPBWT.get(i).toString());
		// }

		return result;
	}

	
	private ReadPbwtResult PBWTAlgo(ArrayList<ReadStruct> readsList, int[] start, int[] end)
	{
		ArrayList<Integer> a = new ArrayList<Integer>(); // 存储为0的值
		ArrayList<Integer> b = new ArrayList<Integer>(); // 存储为1的值
		ArrayList<Integer> c = new ArrayList<Integer>(); // 存储新加入的值，在这里，就是ReadElemEnum.START部分 liyang:应该是存储3
		
		// 用于构建ak数组
		ArrayList<Integer> A = new ArrayList<Integer>();
		ArrayList<Integer> B = new ArrayList<Integer>();
		ArrayList<Integer> C = new ArrayList<Integer>();
		ArrayList<Character> qualA = new ArrayList<Character>();

		ArrayList<ArrayList<Integer>> listsPBWT = new ArrayList<ArrayList<Integer>>();
		ArrayList<Character> listsPbwtQual = new ArrayList<Character>();
		ArrayList<ArrayList<String>> exceptionList = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<Character>> exceptionListQual = new ArrayList<ArrayList<Character>>();

		ArrayList<ArrayList<Integer>> indexes = new ArrayList<>();	// 用于存储所有的ak
		
		ArrayList<ReadStruct> readsCurrList = new ArrayList<ReadStruct>();
		ReadPbwtResult result = new ReadPbwtResult();

		// 检测一下是否正常排序
		for (int i = 0; i < start.length; i++)
		{
			if (i > 1 && start[i] < start[i - 1])
			{
				System.out.println("It doesn't sort by start");
				System.exit(0);
			}
		}

		int enter = 0, currPosDist = 0;
		int readsIndex = 0;
		int endindex = end.length - 1;
		int tmp = -1;
		// 这是为了防止，最后一个reads由于不等长的原因导致其并非是最后的坐标，但是这样遍历一遍是不可行的（我认为）
		for (int i = end.length - 1; i >= 0; i--)
		{
			if (end[i] >= tmp)
			{
				tmp = end[i];
				endindex = i;
			}
		}

		// 真正的pbwt处理过程，位置信息是从1开始的，所以说不可能是0
		int pos = 0;
		boolean emptyFlag = true;	// 用于监测collection是非为空
		for (pos = start[0]; pos <= end[endindex] + 1; pos++)
		{
			// 这了不明白，为什么不能够等于0呢，为什么需要这么处理呢
			if (pos == 0)
			{
				pos = start[++enter] - 1;
				continue;
			}

//			这里的目的就是为了初始化ak数组，这个顺序不能改
			if(readsCurrList.isEmpty())
			{
				emptyFlag = true;
			}
			if (currPosDist == 0) // liyang：这里是每条新的序列才能够进行的，只有currposdist减到0,才能加入下一条序列
			{
				while (enter < end.length && start[enter] == pos) // 有新元素进队列的情形 liyang：这里这个设计非常的完美pos一直加，而currposdist一直减
				{
					readsCurrList.add(readsList.get(readsIndex++)); // liyang:序列从横向的排列变成了纵向的排列，把所有的开头位置一样的先存储一下
					enter++;
				}					
				if (enter < end.length)
				{
					currPosDist = start[enter] - pos;
				}
			}
			currPosDist--;

//			如果不为空，进行下面操作。容器中有序列
			if(!readsCurrList.isEmpty())
			{
				ArrayList<Integer> listPbwt = new ArrayList<Integer>();
				ArrayList<String> exception = new ArrayList<String>();
				ArrayList<Character> exceptionQual = new ArrayList<Character>();
				ArrayList<Integer> index = new ArrayList<>();
				
				// 初始化操作，可能会出现多次初始化的情况
				if(emptyFlag == true)
				{
					// 在这里进行初值化操作我认为比较合适
					// 读取此时容器长度，然后依次遍历，将a0赋值
					ArrayList<Integer> a0 = new ArrayList<>();
					for(int i=0 ;i <readsCurrList.size(); i++)
					{
						a0.add(i);
					}
					indexes.add(a0);
				}
				// 应对插入更新问题
				// 如果进行之前的初始化操作，这个if不会进入。
				int maxSize = indexes.size()-1;	
				int maxValue = indexes.get(maxSize).size()-1;
				int difference = readsCurrList.size()-indexes.get(maxSize).size();
				if (emptyFlag == false && difference > 0)
				{
					// 更新ak数组
					for(int i=0; i<difference; i++)
					{
						maxValue+=1;
						indexes.get(maxSize).add(maxValue);
					}
					
				}
				
				emptyFlag = false;	// 只有第一次为空进这个循环才会执行这个操作，其他时候不进入
				int curVal = -1;
				char curValQual = ' ';
				int curValue = -1;
				int curIndex = -1;
				// 构造ak+1和k位点的dpbwt
				for (int i = 0; i < readsCurrList.size(); i++)
				{	
					// 一条read上的相对位置，从0开始到最后length-1
//					curVal = readsCurrList.get(i).getReads().get(pos - readsCurrList.get(i).getStartAlignment());
					curValQual = 0; // 这里初值设置为0非常重要，应对是3的情况
					
					curIndex = indexes.get(indexes.size() - 1).get(i);
					curValue = readsCurrList.get(curIndex).getReads()
							.get(pos - readsCurrList.get(i).getStartAlignment());
					
					if (curValue == 1 || curVal == 0)
					{
						curValQual = readsCurrList.get(curIndex).getReadQuality()
								.charAt(pos - readsCurrList.get(i).getStartAlignment());
					}
					
					if (curValue == 0)
					{
						A.add(curIndex);
						qualA.add(curValQual);
						
						listPbwt.add(curValue);
					}
					else if (curValue == 1)
					{
						B.add(curIndex);
						listPbwt.add(curValue);
						
						exception.add(readsCurrList.get(curIndex).getException().get(0));
						exBackUp.add(readsCurrList.get(curIndex).getException().get(0));
						readsCurrList.get(curIndex).getException().remove(0);
						exceptionQual.add(curValQual);
						exListLength++;
					}
					else
					{
						C.add(curIndex);
						listPbwt.add(ReadElemEnum.END.ordinal());
					}
					
					for (Integer ins : A)
					{
						index.add(ins);
					}
					for (Integer ins : B)
					{
						index.add(ins);
					}
				}

				// 异常值加入
				exceptionList.add(exception);
				// read序列加入
				listsPBWT.add(listPbwt);
				// 正常质量分数加入
				// 这里应该先是稀疏化处理，然后才是均质化处理，
				// 这里丢给质量分数处理函数，处理。应该是单步在这里进行完成的
				int avg = 0;
				for (Character chr : qualA)
				{
					avg += chr;
				}
				// 这里注意，如果是大小为0的话，也需要加上一个0，站住这个位置
				if (qualA.size() == 0)
				{
					listsPbwtQual.add(null);
				} else
				{
					listsPbwtQual.add((char) (avg / qualA.size()));
						
				}
				// 异常质量分数加入
				exceptionListQual.add(exceptionQual);

				a.clear();
				b.clear();
				c.clear();
				A.clear();
				B.clear();
				C.clear();
				qualA.clear();
				// 这里是一个移除操作，因为每次都要删除第一个，所以需要不断的偏移求相对位置
				// 这里改一下，删除更新，更新ak+1这个数组
				int offset = 0;
				int tempIndex = -1;
				Collections.sort(C);
				for (Integer ins : C)
				{
					readsCurrList.remove(ins - offset);	// 删除序列
					for(int i=0 ;i<index.size() ;i++)
					{
						if (ins < index.get(i))
						{
							tempIndex = index.get(i) - 1;
							index.set(i, tempIndex);
						}
						else if(ins == index.get(i))
						{
							index.remove(i);
							i--;
						}
						else
						{
							// 什么也不做
						}
					}
					offset++;
				}
				C.clear();
				indexes.add(index);
			}
			// 这里需要补全代码
			result.setListsPBWT(listsPBWT);
			result.setListsExcep(exceptionList);
			result.setListExQual(exceptionListQual);
			result.setListsQual(listsPbwtQual);
			readsQualLength++;

		}
//		System.out.println("listsPBWT size:"+result.getListsPBWT().size());
		System.out.println("\n"+"pos:"+(pos-start[0]));
		System.out.println();
		System.out.println("Original PBWT(pbwtResult.length):\t");
		// for(int i=0; i<listsPBWT.size(); i++)
		// {
		// System.out.println(listsPBWT.get(i).toString());
		// }

		return result;
	}

	// 暂时按照一列一个值处理，之后可能需要进行检测，按照几个值进行存储比较准确
	// 这里对于质量分数的压缩有三种策略
	// 第一种：按照之前的压缩方式不同就加上0,
	// 第二种：只有在每一列的最后才加上0，还原的时候通过数0可以定位
	// 第三种：一列直接均质化处理变为一个值，还原的时候通过起始位置和长度找到位点
	private static byte[] EncodePbwtQual(ArrayList<ArrayList<Character>> vE)
	{
		ArrayList<ArrayList<Character>> reQual = vE;
		ArrayList<ArrayList<Integer>> pbwtQualProc = new ArrayList<ArrayList<Integer>>();
		byte[] pbwtQual2By = null;
		ByteBuffer pbwtQual = null;

		for (ArrayList<Character> qualList : reQual)
		{
			ArrayList<Integer> qualProc = new ArrayList<Integer>();
			for (Character ch : qualList)
			{
				if (ch - 33 < 10)
				{
					qualProc.add(QualEnum.six.ordinal()); // 1
				} else if (ch - 33 < 20)
				{
					qualProc.add(QualEnum.fifteen.ordinal()); // 2
				} else if (ch - 33 < 25)
				{
					qualProc.add(QualEnum.twenty.ordinal()); // 3
				} else if (ch - 33 < 30)
				{
					qualProc.add(QualEnum.twenty_seven.ordinal()); // 4
				} else if (ch - 33 < 35)
				{
					qualProc.add(QualEnum.thirty_three.ordinal()); // 5
				} else if (ch - 33 < 40)
				{
					qualProc.add(QualEnum.thirty_seven.ordinal()); // 6
				} else
				{
					qualProc.add(QualEnum.forty.ordinal()); // 7
				}
			}
			pbwtQualProc.add(qualProc);
		}

		ArrayList<Integer> keys = new ArrayList<Integer>();
		ArrayList<Integer> values = new ArrayList<Integer>();
		int cas = 3;
		// 分三种情况压缩key值
		switch (cas)
		{
		case 1:
			for (ArrayList<Integer> list : pbwtQualProc)
			{
				int pre = -1;
				int cur = -1;
				int count = 0;
				for (int i = 0; i < list.size(); i++)
				{
					cur = list.get(i);
					if (i != 0 && cur != pre)
					{
						keys.add(pre);
						values.add(count);
						count = 0;
					}
					if (i == list.size() - 1)
					{
						// 处理到最后一个元素了
						keys.add(cur);
						values.add(++count);
						// 结尾增加一个标识符
						keys.add(QualEnum.separator.ordinal());
						values.add(0);
					}

					count++;
					pre = cur;
				}
			}
			// by2 和 by3 可能有问题 ×128 和 ×64 可能是反的. 8*3+8可能是错的，应该是+3
			pbwtQual = ByteBuffer.allocate(keys.size() / 8 * 3 + 3);
			for (int i = 0; i < keys.size() - 8; i += 8)
			{
				byte by1 = (byte) (keys.get(i) * 32 + keys.get(i + 1) * 4 + keys.get(i + 2) / 2 - 128);
				byte by2 = (byte) (keys.get(i + 2) % 2 * 128 + keys.get(i + 3) * 16 + keys.get(i + 4) * 2
						+ keys.get(i + 5) / 4 - 128);
				byte by3 = (byte) (keys.get(i + 5) % 4 * 64 + keys.get(i + 6) * 8 + keys.get(i + 7) - 128);
				pbwtQual.put(by1);
				pbwtQual.put(by2);
				pbwtQual.put(by3);
			}
			break;

		case 2:

			break;

		case 3:
			for (ArrayList<Integer> list : pbwtQualProc)
			{
				int cur = -1;
				int sum = 0;
				for (int i = 0; i < list.size() - 1; i++)
				{
					cur = list.get(i);
					sum += cur;
				}
				sum += list.get(list.size() - 1);
				keys.add(sum / list.size());
				// keys.add(QualEnum.separator.ordinal());
			}
			if (keys.size() % 8 == 0)
			{
				pbwtQual = ByteBuffer.allocate(keys.size() / 8 * 3);
				for (int i = 0; i < keys.size(); i += 8)
				{
					byte by1 = (byte) (keys.get(i) * 32 + keys.get(i + 1) * 4 + keys.get(i + 2) / 2 - 128);
					byte by2 = (byte) (keys.get(i + 2) % 2 * 128 + keys.get(i + 3) * 16 + keys.get(i + 4) * 2
							+ keys.get(i + 5) / 4 - 128);
					byte by3 = (byte) (keys.get(i + 5) % 4 * 64 + keys.get(i + 6) * 8 + keys.get(i + 7) - 128);
					pbwtQual.put(by1);
					pbwtQual.put(by2);
					pbwtQual.put(by3);
				}
			}

			else
			{
				pbwtQual = ByteBuffer.allocate(keys.size() / 8 * 3 + 3);
				for (int i = 0; i < keys.size() - 8; i += 8)
				{
					byte by1 = (byte) (keys.get(i) * 32 + keys.get(i + 1) * 4 + keys.get(i + 2) / 2 - 128);
					byte by2 = (byte) (keys.get(i + 2) % 2 * 128 + keys.get(i + 3) * 16 + keys.get(i + 4) * 2
							+ keys.get(i + 5) / 4 - 128);
					byte by3 = (byte) (keys.get(i + 5) % 4 * 64 + keys.get(i + 6) * 8 + keys.get(i + 7) - 128);
					pbwtQual.put(by1);
					pbwtQual.put(by2);
					pbwtQual.put(by3);
				}

				int j = keys.size() % 8;
				int left = keys.size() - keys.size() % 8;
				byte lastby;
				byte lastby2;
				byte lastby3;
				if (j == 1)
				{
					lastby = (byte) (keys.get(left) * 32 - 128);
					pbwtQual.put(lastby);
				} else if (j == 2)
				{
					lastby = (byte) (keys.get(left) * 32 + keys.get(left + 1) * 4 - 128);
					pbwtQual.put(lastby);
				} else if (j == 3)
				{
					lastby = (byte) (keys.get(left) * 32 + keys.get(left + 1) * 4 + keys.get(left + 2) / 2 - 128);
					lastby2 = (byte) (keys.get(left + 2) % 2 * 128 - 128);
					pbwtQual.put(lastby);
					pbwtQual.put(lastby2);
				} else if (j == 4)
				{
					lastby = (byte) (keys.get(left) * 32 + keys.get(left + 1) * 4 + keys.get(left + 2) / 2 - 128);
					lastby2 = (byte) (keys.get(left + 2) % 2 * 128 + keys.get(left + 3) * 16 - 128);
					pbwtQual.put(lastby);
					pbwtQual.put(lastby2);
				} else if (j == 5)
				{
					lastby = (byte) (keys.get(left) * 32 + keys.get(left + 1) * 4 + keys.get(left + 2) / 2 - 128);
					lastby2 = (byte) (keys.get(left + 2) % 2 * 128 + keys.get(left + 3) * 16 + keys.get(left + 4) * 2
							- 128);
					pbwtQual.put(lastby);
					pbwtQual.put(lastby2);
				} else if (j == 6)
				{
					lastby = (byte) (keys.get(left) * 32 + keys.get(left + 1) * 4 + keys.get(left + 2) / 2 - 128);
					lastby2 = (byte) (keys.get(left + 2) % 2 * 128 + keys.get(left + 3) * 16 + keys.get(left + 4) * 2
							+ keys.get(left + 5) / 4 - 128);
					lastby3 = (byte) (keys.get(left + 5) % 4 * 64 - 128);
					pbwtQual.put(lastby);
					pbwtQual.put(lastby2);
					pbwtQual.put(lastby3);
				} else if (j == 7)
				{
					lastby = (byte) (keys.get(left) * 32 + keys.get(left + 1) * 4 + keys.get(left + 2) / 2 - 128);
					lastby2 = (byte) (keys.get(left + 2) % 2 * 128 + keys.get(left + 3) * 16 + keys.get(left + 4) * 2
							+ keys.get(left + 5) / 4 - 128);
					lastby3 = (byte) (keys.get(left + 5) % 4 * 64 + keys.get(left + 6) * 8 - 128);
					pbwtQual.put(lastby);
					pbwtQual.put(lastby2);
					pbwtQual.put(lastby3);
				} else
				{
					System.out.println("PbwtQual transfor byte is error !");
				}

			}
			break;
		default:
			System.out.println("PbwtQual transfor byte is error !");
			break;
		}

		pbwtQual2By = pbwtQual.array();
		System.out.println("the compression of pbwtQual has completed");
		return pbwtQual2By;
	}

	// 这个函数是EncodePbwtSingleQual的一种特殊情况，就是pbwtQual每列只有一个
	// 这样只需要压缩value值就行了，不要需要压缩key了
	// 我认为并不需要在进行了游程编码了，因为横向并不是具备生物特性，强行使用效果并不一定会非常好
	// 之前的预处理我么可知，每一列必然有一个qual值，所以说不可能出现空的情况
	/**
	 * 既适用于有局部又适用于全局
	 * @param vE
	 * @return
	 */
	private static byte[] EncodePbwtSingleQual(ArrayList<Character> vE)
	{
		ArrayList<Character> reQual = vE;
		ArrayList<Integer> pbwtQualProc = new ArrayList<Integer>();
		byte[] pbwtQual2By = null;
		ByteBuffer pbwtQual = null;

		for (Character ch : reQual)
		{
			if (ch != null)
			{
				if (ch - 33 < 10)
				{
					pbwtQualProc.add(QualEnum.six.ordinal()); // 1
				} else if (ch - 33 < 20)
				{
					pbwtQualProc.add(QualEnum.fifteen.ordinal()); // 2
				} else if (ch - 33 < 25)
				{
					pbwtQualProc.add(QualEnum.twenty.ordinal()); // 3
				} else if (ch - 33 < 30)
				{
					pbwtQualProc.add(QualEnum.twenty_seven.ordinal()); // 4
				} else if (ch - 33 < 35)
				{
					pbwtQualProc.add(QualEnum.thirty_three.ordinal()); // 5
				} else if (ch - 33 < 40)
				{
					pbwtQualProc.add(QualEnum.thirty_seven.ordinal()); // 6
				} else
				{
					pbwtQualProc.add(QualEnum.forty.ordinal()); // 7
				}
			} else
			{
				pbwtQualProc.add(0);
			}

		}

		int left = 0;
		if (pbwtQualProc.size() % 8 == 0)
		{
			// ××××××××××××××××××这里注意相对与原代码，循环终止条件不要-8××××××××××××××××××××
			pbwtQual = ByteBuffer.allocate(pbwtQualProc.size() / 8 * 3);
			for (; left < pbwtQualProc.size(); left += 8)
			{
				byte by1 = (byte) (pbwtQualProc.get(left) * 32 + pbwtQualProc.get(left + 1) * 4
						+ pbwtQualProc.get(left + 2) / 2 - 128);
				byte by2 = (byte) (pbwtQualProc.get(left + 2) % 2 * 128 + pbwtQualProc.get(left + 3) * 16
						+ pbwtQualProc.get(left + 4) * 2 + pbwtQualProc.get(left + 5) / 4 - 128);
				byte by3 = (byte) (pbwtQualProc.get(left + 5) % 4 * 64 + pbwtQualProc.get(left + 6) * 8
						+ pbwtQualProc.get(left + 7) - 128);
				pbwtQual.put(by1);
				pbwtQual.put(by2);
				pbwtQual.put(by3);
			}
		}

		// 多出 7,6,5,4,3,2,1情况
		else
		{
			pbwtQual = ByteBuffer.allocate(pbwtQualProc.size() / 8 * 3 + 3);
			for (; left < pbwtQualProc.size() - 8; left += 8)
			{
				byte by1 = (byte) (pbwtQualProc.get(left) * 32 + pbwtQualProc.get(left + 1) * 4
						+ pbwtQualProc.get(left + 2) / 2 - 128);
				byte by2 = (byte) (pbwtQualProc.get(left + 2) % 2 * 128 + pbwtQualProc.get(left + 3) * 16
						+ pbwtQualProc.get(left + 4) * 2 + pbwtQualProc.get(left + 5) / 4 - 128);
				byte by3 = (byte) (pbwtQualProc.get(left + 5) % 4 * 64 + pbwtQualProc.get(left + 6) * 8
						+ pbwtQualProc.get(left + 7) - 128);
				pbwtQual.put(by1);
				pbwtQual.put(by2);
				pbwtQual.put(by3);
			}
			int j = pbwtQualProc.size() - left;
			byte lastby;
			byte lastby2;
			byte lastby3;
			switch (j)
			{
			case 1:
				lastby = (byte) (pbwtQualProc.get(left) * 32 - 128);
				pbwtQual.put(lastby);
				break;
			case 2:
				lastby = (byte) (pbwtQualProc.get(left) * 32 + pbwtQualProc.get(left + 1) * 4 - 128);
				pbwtQual.put(lastby);
				break;
			case 3:
				lastby = (byte) (pbwtQualProc.get(left) * 32 + pbwtQualProc.get(left + 1) * 4
						+ pbwtQualProc.get(left + 2) / 2 - 128);
				lastby2 = (byte) (pbwtQualProc.get(left + 2) % 2 * 128 - 128);
				pbwtQual.put(lastby);
				pbwtQual.put(lastby2);
				break;
			case 4:
				lastby = (byte) (pbwtQualProc.get(left) * 32 + pbwtQualProc.get(left + 1) * 4
						+ pbwtQualProc.get(left + 2) / 2 - 128);
				lastby2 = (byte) (pbwtQualProc.get(left + 2) % 2 * 128 + pbwtQualProc.get(left + 3) * 16 - 128);
				pbwtQual.put(lastby);
				pbwtQual.put(lastby2);
				break;
			case 5:
				lastby = (byte) (pbwtQualProc.get(left) * 32 + pbwtQualProc.get(left + 1) * 4
						+ pbwtQualProc.get(left + 2) / 2 - 128);
				lastby2 = (byte) (pbwtQualProc.get(left + 2) % 2 * 128 + pbwtQualProc.get(left + 3) * 16
						+ pbwtQualProc.get(left + 4) * 2 - 128);
				pbwtQual.put(lastby);
				pbwtQual.put(lastby2);
				break;
			case 6:
				lastby = (byte) (pbwtQualProc.get(left) * 32 + pbwtQualProc.get(left + 1) * 4
						+ pbwtQualProc.get(left + 2) / 2 - 128);
				lastby2 = (byte) (pbwtQualProc.get(left + 2) % 2 * 128 + pbwtQualProc.get(left + 3) * 16
						+ pbwtQualProc.get(left + 4) * 2 + pbwtQualProc.get(left + 5) / 4 - 128);
				lastby3 = (byte) (pbwtQualProc.get(left + 5) % 4 * 64 - 128);
				pbwtQual.put(lastby);
				pbwtQual.put(lastby2);
				pbwtQual.put(lastby3);
				break;
			case 7:
				lastby = (byte) (pbwtQualProc.get(left) * 32 + pbwtQualProc.get(left + 1) * 4
						+ pbwtQualProc.get(left + 2) / 2 - 128);
				lastby2 = (byte) (pbwtQualProc.get(left + 2) % 2 * 128 + pbwtQualProc.get(left + 3) * 16
						+ pbwtQualProc.get(left + 4) * 2 + pbwtQualProc.get(left + 5) / 4 - 128);
				lastby3 = (byte) (pbwtQualProc.get(left + 5) % 4 * 64 + pbwtQualProc.get(left + 6) * 8 - 128);
				pbwtQual.put(lastby);
				pbwtQual.put(lastby2);
				pbwtQual.put(lastby3);
				break;
			default:
				System.out.println("exceptionQual transfor byte is error !");
				break;
			}
		}
		pbwtQual2By = pbwtQual.array();

		return pbwtQual2By;
	}

	// 这个函数需要修改，首先是把0的区分坐标删除，还原的时遇到1就从列表中取第一个就行。
	// value的值存在大量的重复，很多都是1，我们可以在进行一次游程编码，或者说设计一个框架。提前预测一下
	// 先不用游程编码，之后的思路就是设计检测1的含量，达到多少压缩效果最好。
	// 策略，只保留key值不要value，然后每个都占3位1-7。还原的时候比较麻烦，就是3个一取，最后可能正好，也可能不够，不够就不要了，因为是补上去的。
	// 策略一：按照之前，能够实现指定区间的加压缩。 策略二：不能够实现指定未知的解压缩，但是压缩更小。先去把策略二实现了
	// 对于异常质量分数的压缩策略，最终是不能够按照索引位置进行加压缩的
	private static byte[] EncodeExceptionQual(ArrayList<ArrayList<Character>> vE)
	{
		System. out .println( " 内存信息 :" + toMemoryInfo());
		ArrayList<ArrayList<Character>> exceptionQual = vE;
		ArrayList<ArrayList<Integer>> exceptionQualProc = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> keys = new ArrayList<Integer>();
//		System.out.println("the length of VE"+vE.size());
		System. out .println( " 内存信息 :" + toMemoryInfo());
		for (ArrayList<Character> qualList : exceptionQual)
		{
			ArrayList<Integer> qualProc = new ArrayList<Integer>();
			if (qualList.isEmpty())
			{
				qualProc.add(0);
				// keys.add(0);
			} else
			{
				for (char qual : qualList)
				{
					// char[] quals = qual.toCharArray();
					if (qual - 33 < 10)
					{
						qualProc.add(QualEnum.six.ordinal());
						keys.add(QualEnum.six.ordinal());
					} else if (qual - 33 < 20)
					{
						qualProc.add(QualEnum.fifteen.ordinal());
						keys.add(QualEnum.fifteen.ordinal());
					} else if (qual - 33 < 25)
					{
						qualProc.add(QualEnum.twenty.ordinal());
						keys.add(QualEnum.twenty.ordinal());
					} else if (qual - 33 < 30)
					{
						qualProc.add(QualEnum.twenty_seven.ordinal());
						keys.add(QualEnum.twenty_seven.ordinal());
					} else if (qual - 33 < 35)
					{
						qualProc.add(QualEnum.thirty_three.ordinal());
						keys.add(QualEnum.thirty_three.ordinal());
					} else if (qual - 33 < 40)
					{
						qualProc.add(QualEnum.thirty_seven.ordinal());
						keys.add(QualEnum.thirty_seven.ordinal());
					} else
					{
						qualProc.add(QualEnum.forty.ordinal());
						keys.add(QualEnum.forty.ordinal());
					}
				}
			}
			exceptionQualProc.add(qualProc);
		}

		// ArrayList<Integer> keys = new ArrayList<Integer>();
		// ArrayList<Integer> values = new ArrayList<Integer>();
		// for(ArrayList<Integer> list: exceptionQualProc){
		// int pre = -1;
		// int cur = -1;
		// int count = 0;
		// for(int i = 0; i < list.size(); i++){
		// cur = list.get(i);
		// if(i !=0 && cur != pre){
		// keys.add(pre);
		// values.add(count);
		// count = 0;
		// }
		// if(i == list.size() - 1){
		// // 处理到最后一个元素了
		// keys.add(cur);
		// values.add(++count);
		// //结尾增加一个标识符
		// // 不需要，解压缩的时候按照顺序找就行
		//// keys.add(QualEnum.separator.ordinal());
		//// values.add(0);
		// }
		//
		// count ++;
		// pre = cur;
		// }
		// }
		// System.out.println();
		// System.out.println("values :"+values);
		// System.out.println("keys :"+keys);
		// System.out.println();
		// ByteBuffer eQual = ByteBuffer.allocate(keys.size());
		// for(int i = 0; i < keys.size(); i++){
		// eQual.put((byte)(keys.get(i)*32 + values.get(i) - 128));
		// }
		System. out .println( " 内存信息 :" + toMemoryInfo());
		int left = 0;
		ByteBuffer eQual2;
		// 刚好不多的情况
		if (keys.size() % 8 == 0)
		{
			// ××××××××××××××××××这里注意相对与原代码，循环终止条件不要-8××××××××××××××××××××
			eQual2 = ByteBuffer.allocate(keys.size() / 8 * 3);
			for (; left < keys.size(); left += 8)
			{
				byte by1 = (byte) (keys.get(left) * 32 + keys.get(left + 1) * 4 + keys.get(left + 2) / 2);
				byte by2 = (byte) (keys.get(left + 2) % 2 * 128 + keys.get(left + 3) * 16 + keys.get(left + 4) * 2
						+ keys.get(left + 5) / 4);
				byte by3 = (byte) (keys.get(left + 5) % 4 * 64 + keys.get(left + 6) * 8 + keys.get(left + 7));
				eQual2.put(by1);
				eQual2.put(by2);
				eQual2.put(by3);
			}
		}
		// 多出 7,6,5,4,3,2,1情况
		else
		{
			eQual2 = ByteBuffer.allocate(keys.size() / 8 * 3 + 3);
			for (; left < keys.size() - 8; left += 8)
			{
				byte by1 = (byte) (keys.get(left) * 32 + keys.get(left + 1) * 4 + keys.get(left + 2) / 2);
				byte by2 = (byte) (keys.get(left + 2) % 2 * 128 + keys.get(left + 3) * 16 + keys.get(left + 4) * 2
						+ keys.get(left + 5) / 4);
				byte by3 = (byte) (keys.get(left + 5) % 4 * 64 + keys.get(left + 6) * 8 + keys.get(left + 7));
				eQual2.put(by1);
				eQual2.put(by2);
				eQual2.put(by3);
			}
			int j = keys.size() - left;
			byte lastby;
			byte lastby2;
			byte lastby3;
			switch (j)
			{
			case 1:
				lastby = (byte) (keys.get(left) * 32);
				eQual2.put(lastby);
				break;
			case 2:
				lastby = (byte) (keys.get(left) * 32 + keys.get(left + 1) * 4);
				eQual2.put(lastby);
				break;
			case 3:
				lastby = (byte) (keys.get(left) * 32 + keys.get(left + 1) * 4 + keys.get(left + 2) / 2);
				lastby2 = (byte) (keys.get(left + 2) % 2 * 128);
				eQual2.put(lastby);
				eQual2.put(lastby2);
				break;
			case 4:
				lastby = (byte) (keys.get(left) * 32 + keys.get(left + 1) * 4 + keys.get(left + 2) / 2);
				lastby2 = (byte) (keys.get(left + 2) % 2 * 128 + keys.get(left + 3) * 16);
				eQual2.put(lastby);
				eQual2.put(lastby2);
				break;
			case 5:
				lastby = (byte) (keys.get(left) * 32 + keys.get(left + 1) * 4 + keys.get(left + 2) / 2);
				lastby2 = (byte) (keys.get(left + 2) % 2 * 128 + keys.get(left + 3) * 16 + keys.get(left + 4) * 2);
				eQual2.put(lastby);
				eQual2.put(lastby2);
				break;
			case 6:
				lastby = (byte) (keys.get(left) * 32 + keys.get(left + 1) * 4 + keys.get(left + 2) / 2);
				lastby2 = (byte) (keys.get(left + 2) % 2 * 128 + keys.get(left + 3) * 16 + keys.get(left + 4) * 2
						+ keys.get(left + 5) / 4);
				lastby3 = (byte) (keys.get(left + 5) % 4 * 64);
				eQual2.put(lastby);
				eQual2.put(lastby2);
				eQual2.put(lastby3);
				break;
			case 7:
				lastby = (byte) (keys.get(left) * 32 + keys.get(left + 1) * 4 + keys.get(left + 2) / 2);
				lastby2 = (byte) (keys.get(left + 2) % 2 * 128 + keys.get(left + 3) * 16 + keys.get(left + 4) * 2
						+ keys.get(left + 5) / 4);
				lastby3 = (byte) (keys.get(left + 5) % 4 * 64 + keys.get(left + 6) * 8);
				eQual2.put(lastby);
				eQual2.put(lastby2);
				eQual2.put(lastby3);
				break;
			default:
				System.out.println("exceptionQual transfor byte is error !");
				break;
			}
		}

		byte[] eQual2By = eQual2.array();
		System. out .println( " 内存信息 :" + toMemoryInfo());
		// System.out.println("exception length:"+eQual2By.length);
		System.out.println("the compression of exQual has completed");
		return eQual2By;
	}

	/**
	 * 针对按照序列解压缩的异常值解压缩
	 * 最大化压缩
	 * @param vE
	 * @return
	 */
	public static byte[] EncodeExceptionList(ArrayList<ArrayList<String>> vE)
	{
		ArrayList<ArrayList<String>> exceptionList = vE;
		Huffman2 huffman = new Huffman2();
		String rateText = "AAACCCTTTGGG||DN";
		huffman.handleRate(rateText);
		StringBuilder rawText = new StringBuilder();
		for (ArrayList<String> str : exceptionList)
		{
			for (int i = 0; i < str.size(); i++)
			{
				rawText.append(str.get(i) + "|");
			}
		}
		String[] str = Huffman2.encodeText2(rawText.toString());

		int length = 0; // crazy:用于记录str真正的长度；
		for (int i = 0; i < str.length; i++)
		{
//			System.out.println(str[i]);
			if (str[i] == "")
			{
				length = i;
				break;
			}
			length = i + 1;
		}
//		System.out.println("huffman length \t" + length);
		// System.out.println();
		// System.out.println("length : "+length);
//		for (ArrayList<String> ar : exceptionList)
//		{
//			for (String str1 : ar)
//			{
//				System.out.print(str1 + "  ");
//			}
//			System.out.println();
//		}
//		System.out.println();
		// System.out.println(length);

		ByteBuffer bytes = ByteBuffer.allocate(length * 2); // liyang:开辟双倍内存，short占用两个字节
		for (int i = 0; i < length - 1; i++)
		{
			// System.out.println("bytes.postiton:\t"+bytes.position());
			bytes.putShort((short) (Integer.parseInt(str[i], 2)));
		}
		// 最后一个位置再拼凑一下然后放进去
		// System.out.println(str[length-1].length());
		// ××××××××××××××××××××××××××××××××加上1没问题，但是你要怎么还原回去呢××××××××××××××××××××××××××××××××××××××××××
		String strTemp = str[length - 1];
		if (strTemp.length() < 16)
		{
			for (int num = 16 - strTemp.length(); num > 0; num--)
			{
				strTemp += "1";
			}
		} else
		{

		}
		bytes.putShort((short) (Integer.parseInt(strTemp, 2)));
		byte[] array = bytes.array();
		System.out.println("the compression of ex has completed ");
		return array;
	}

	/**
	 * 全局压缩，异常值为一维
	 * @param vE
	 * @return
	 */
	public static byte[] EncodeExceptionList2(List<String> vE)
	{
		List<String> exceptionList = vE;
		Huffman2 huffman = new Huffman2();
		String rateText = "AAACCCTTTGGG||DN";
		huffman.handleRate(rateText);
		StringBuilder rawText = new StringBuilder();
		for (String str : exceptionList)
		{
			rawText.append(str + "|");
		}
		String[] str = Huffman2.encodeText2(rawText.toString());

		int length = 0; // crazy:用于记录str真正的长度；
		for (int i = 0; i < str.length; i++)
		{
//			System.out.println(str[i]);
			if (str[i] == "")
			{
				length = i;
				break;
			}
			length = i + 1;
		}

		ByteBuffer bytes = ByteBuffer.allocate(length * 2); // liyang:开辟双倍内存，short占用两个字节
		for (int i = 0; i < length - 1; i++)
		{
			// System.out.println("bytes.postiton:\t"+bytes.position());
			bytes.putShort((short) (Integer.parseInt(str[i], 2)));
		}
		// 最后一个位置再拼凑一下然后放进去
		// System.out.println(str[length-1].length());
		// ××××××××××××××××××××××××××××××××加上1没问题，但是你要怎么还原回去呢××××××××××××××××××××××××××××××××××××××××××
		String strTemp = str[length - 1];
		if (strTemp.length() < 16)
		{
			for (int num = 16 - strTemp.length(); num > 0; num--)
			{
				strTemp += "1";
			}
		} else
		{

		}
		bytes.putShort((short) (Integer.parseInt(strTemp, 2)));
		byte[] array = bytes.array();
		System.out.println("the compression of ex has completed ");
		return array;
	}

	/**
	 * 适用于插入压缩，引入符号‘，’进行内部区分，空的位置加上‘|’
	 * @param vE
	 * @return
	 */
	public static byte[] EncodeExceptionList3(ArrayList<ArrayList<String>> vE)
	{
		ArrayList<ArrayList<String>> exceptionList = vE;
		Huffman2 huffman = new Huffman2();
		String rateText = "AAACCCTTTGGG||,,DN";
		huffman.handleRate(rateText);
		StringBuilder rawText = new StringBuilder();
		for (ArrayList<String> str : exceptionList)
		{
			if (str.isEmpty())
			{
				rawText.append("|");
			} else if (str.size() == 1)
			{
				rawText.append(str.get(0) + "|");
			} else
			{
				int i = 0;
				while (i < str.size() - 1)
				{
					rawText.append(str.get(i) + ",");
					i++;
				}
				rawText.append(str.get(i) + "|");
			}
		}
		String[] str = Huffman2.encodeText2(rawText.toString());

		int length = 0; // crazy:用于记录str真正的长度；
		for (int i = 0; i < str.length; i++)
		{
			System.out.println(str[i]);
			if (str[i] == "")
			{
				length = i;
				break;
			}
			length = i + 1;
		}

		ByteBuffer bytes = ByteBuffer.allocate(length * 2); // liyang:开辟双倍内存，short占用两个字节
		for (int i = 0; i < length - 1; i++)
		{
			// System.out.println("bytes.postiton:\t"+bytes.position());
			bytes.putShort((short) (Integer.parseInt(str[i], 2)));
		}
		// 最后一个位置再拼凑一下然后放进去
		// System.out.println(str[length-1].length());
		// ××××××××××××××××××××××××××××××××加上1没问题，但是你要怎么还原回去呢××××××××××××××××××××××××××××××××××××××××××
		String strTemp = str[length - 1];
		if (strTemp.length() < 16)
		{
			for (int num = 16 - strTemp.length(); num > 0; num--)
			{
				strTemp += "1";
			}
		} else
		{

		}
		bytes.putShort((short) (Integer.parseInt(strTemp, 2)));
		byte[] array = bytes.array();
		System.out.println("the compression of ex has completed ");
		return array;
	}

	/**
	 * 适用于局部解压缩，因为列与列之间加上了双杠
	 * @param vE
	 * @return
	 */
	public static byte[] EncodeExceptionList4(ArrayList<ArrayList<String>> vE)
	{
		ArrayList<ArrayList<String>> exceptionList = vE;
		Huffman2 huffman = new Huffman2();
		String rateText = "AAACCCTTTGGG||DN";
		huffman.handleRate(rateText);
		StringBuilder rawText = new StringBuilder();
		for (ArrayList<String> str : exceptionList)
		{
			if (str.isEmpty())
			{
				rawText.append("||");
			} else if (str.size() == 1)
			{
				rawText.append(str.get(0) + "||");
			} else
			{
				int i = 0;
				while (i < str.size() - 1)
				{
					rawText.append(str.get(i) + "|");
					i++;
				}
				rawText.append(str.get(i) + "||");
			}
		}
		String[] str = Huffman2.encodeText2(rawText.toString());

		int length = 0; // crazy:用于记录str真正的长度；
		for (int i = 0; i < str.length; i++)
		{
			System.out.println(str[i]);
			if (str[i] == "")
			{
				length = i;
				break;
			}
			length = i + 1;
		}

		ByteBuffer bytes = ByteBuffer.allocate(length * 2); // liyang:开辟双倍内存，short占用两个字节
		for (int i = 0; i < length - 1; i++)
		{
			// System.out.println("bytes.postiton:\t"+bytes.position());
			bytes.putShort((short) (Integer.parseInt(str[i], 2)));
		}
		// 最后一个位置再拼凑一下然后放进去
		// System.out.println(str[length-1].length());
		// ××××××××××××××××××××××××××××××××加上1没问题，但是你要怎么还原回去呢××××××××××××××××××××××××××××××××××××××××××
		String strTemp = str[length - 1];
		if (strTemp.length() < 16)
		{
			for (int num = 16 - strTemp.length(); num > 0; num--)
			{
				strTemp += "1";
			}
		} else
		{

		}
		bytes.putShort((short) (Integer.parseInt(strTemp, 2)));
		byte[] array = bytes.array();
		System.out.println("the compression of ex has completed ");
		return array;
	}

	
	
	private String TranforOr(String string)
	{
		// 分箱过程的还原过程,写在大循环中可以减少一次遍历
		Random random = new Random();
		String str = "";
		if (string.equals("1"))
		{
			str = String.valueOf(33 + random.nextInt(10));
		} else if (string.equals("2"))
		{
			str = String.valueOf(43 + random.nextInt(10));
		} else if (string.equals("3"))
		{
			str = String.valueOf(54 + random.nextInt(5));
		} else if (string.equals("4"))
		{
			str = String.valueOf(59 + random.nextInt(5));
		} else if (string.equals("5"))
		{
			str = String.valueOf(64 + random.nextInt(5));
		} else if (string.equals("6"))
		{
			str = String.valueOf(69 + random.nextInt(5));
		} else if (string.equals("7"))
		{
			str = String.valueOf(74 + random.nextInt(10));
		} else
		{
			str = String.valueOf(0); // 包含0和4的情况，0不用说了不会出现，4是压缩过程中故意舍掉的具体为啥不知道
		}
		return str;
	}

	private static char TranforOr(char ch)
	{
		Random random = new Random();
		char temp;
		if (ch == '1')
		{
			temp = Character.valueOf((char) (34 + random.nextInt(10)));
		} else if (ch == '2')
		{
			temp = Character.valueOf((char) (44 + random.nextInt(10)));
		} else if (ch == '3')
		{
			temp = Character.valueOf((char) (54 + random.nextInt(5)));
		} else if (ch == '4')
		{
			temp = Character.valueOf((char) (59 + random.nextInt(5)));
		} else if (ch == '5')
		{
			temp = Character.valueOf((char) (64 + random.nextInt(5)));
		} else if (ch == '6')
		{
			temp = Character.valueOf((char) (69 + random.nextInt(5)));
		} else if (ch == '7')
		{
			temp = Character.valueOf((char) (74 + random.nextInt(10)));
		} else
		{
			temp = '!';
			// 包含0和4的情况，0不用说了不会出现，4是压缩过程中故意舍掉的具体为啥不知道
		}
		return temp;

	}

	/**
	 * 用于压缩全部的起始位点信息,同时也可以压缩MinStart
	 * @param start
	 * @return
	 */
	private static byte[] compressStartPos(int[] start)
	{
		// 写一个求相对位置的函数
		// 在求解相对位置的时候就应该把压缩进行
		// 这里有个问题，相对位置用多少位进行存储呢
		// 我们先用2字节进行存储，深度必须达到一定程度才可以压缩，不然无法进行
		// 第一个信息我们用4字节进行存储。
		int[] RelativePostion = new int[start.length];
		byte[] len2 = new byte[2 * start.length + 2];
		int flag = 0;
		for (int i = 0; i < start.length; i++)
		{
			if (i == 0)
			{
				RelativePostion[i] = start[i];
				// 这里对于起始位置采用4个字节进行存储。不差这点，这样就可以覆盖整个长度
				compressLen(RelativePostion[i], len2);
			} else
			{
				RelativePostion[i] = start[i] - start[i - 1];
				if (RelativePostion[i] <= Math.pow(2, 8))
					flag = 1 > flag ? 1 : flag;
				else if (RelativePostion[i] <= Math.pow(2, 16))
					flag = 2 > flag ? 2 : flag;
				else if (RelativePostion[i] <= Math.pow(2, 24))
					flag = 3 > flag ? 3 : flag;
				else
					flag = 4 > flag ? 4 : flag;
				// 这里还需要另一个函数就是压缩相对位置，两个字节一存放
			}

		}
//		print(RelativePostion);
		// 这里写一个压缩位置的函数，2字节存储
		// 起始位置的处理起始和对于长度的处理是类似的
		// 对于后续的处理也都是按照
		byte startPos[] = new byte[4 + (RelativePostion.length - 1) * flag + 1];
		int j = 0;
		for (int i = 0; i < RelativePostion.length; i++)
		{
			if (i == 0)
			{
				compressLen(RelativePostion[i], startPos);
				j = 4;
			} else
			{
				switch (flag)
				{
				case 1:
					startPos[j++] = (byte) (RelativePostion[i] & 0xff);
					break;
				case 2:
					startPos[j++] = (byte) ((RelativePostion[i] >> 8) & 0xff);
					startPos[j++] = (byte) (RelativePostion[i] & 0xff);
					break;
				case 3:
					startPos[j++] = (byte) ((RelativePostion[i] >> 16) & 0xff);
					startPos[j++] = (byte) ((RelativePostion[i] >> 8) & 0xff);
					startPos[j++] = (byte) (RelativePostion[i] & 0xff);
					;
					break;
				case 4:
					startPos[j++] = (byte) ((RelativePostion[i] >> 24) & 0xff);
					startPos[j++] = (byte) ((RelativePostion[i] >> 16) & 0xff);
					startPos[j++] = (byte) ((RelativePostion[i] >> 8) & 0xff);
					startPos[j++] = (byte) (RelativePostion[i] & 0xff);
					break;
				default:
					System.out.println("The compression of startPos exclused the fisrt one is false");
				}

			}
		}
		startPos[j] = (byte) (flag & 0xff);
		// flag也需要压缩一下，不然怎么知道到底是多少个字节一个长度呢。把flag放末尾，这样好找
//		for (int i = 0; i < startPos.length; i++)
//			System.out.print(startPos[i] + " ");
		return startPos;
	}

	/**
	 * 适用于全局压缩
	 * @param startPos
	 * @return
	 */
	private static int[] deStartPos(byte[] startPos)
	{
		int deStart = deCompressLen(Arrays.copyOf(startPos, 4));
		int deFlag = startPos[startPos.length - 1] & 0xff;
		int len = (startPos.length - 5) / deFlag + 1;
		int deS[] = new int[len];
		deS[0] = deStart;
		System.out.println("\n" + " deStart: " + deStart + " deFlag: " + deFlag);
		int index = 1;
		if (deFlag == 1)
		{
			for (int i = 4; i < startPos.length - 1; i++)
			{
				deS[index] = (startPos[i] & 0xff) + deS[index - 1];
				// System.out.print(deS[index-1]+" "+deS[index]+" "+(startPos[i] & 0xff)+"\t");
				index++;
			}
		} else if (deFlag == 2)
		{
			for (int i = 4; i < startPos.length - 1; i = i + 2)
			{
				deS[index] = ((startPos[i] & 0xff) * 256 + (startPos[i + 1] & 0xff)) + deS[index - 1];
				// System.out.print(deS[index-1]+" "+deS[index]+" "+((startPos[i]&0xff)*256 +
				// (startPos[i+1]&0xff))+"\t");
				index++;
			}
		} else if (deFlag == 3)
		{
			for (int i = 4; i < startPos.length - 1; i = i + 3)
			{
				deS[index] = ((startPos[i] & 0xff) * 65536 + (startPos[i + 1] & 0xff) * 256 + (startPos[i + 2] & 0xff))
						+ deS[index - 1];
				index++;
			}
		} else
		{
			for (int i = 4; i < startPos.length - 1; i = i + 4)
			{
				deS[index] = ((startPos[i] & 0xff) * 16777216 + (startPos[i + 1] & 0xff) * 65536
						+ (startPos[i + 3] & 0xff) * 256 + (startPos[i + 4] & 0xff)) + deS[index - 1];
				index++;
			}
		}
		return deS;
	}

	/**
	 * 适用于局部压缩MinStart
	 * @param startPos
	 * @return
	 */
	private static int[] deStartPos2(byte[] startPos)
	{
		int deStart = deCompressLen(Arrays.copyOf(startPos, 4));
		int deFlag = startPos[startPos.length - 1] & 0xff;
		int len = (startPos.length - 5) / deFlag + 1;
		int deS[] = new int[len];
		deS[0] = deStart;
		System.out.println("\n" + " deStart: " + deStart + " deFlag: " + deFlag);
		int index = 1;
		if (deFlag == 1)
		{
			for (int i = 4; i < startPos.length - 1; i++)
			{
				deS[index] = (startPos[i] & 0xff) + deS[index - 1];
				// System.out.print(deS[index-1]+" "+deS[index]+" "+(startPos[i] & 0xff)+"\t");
				index++;
			}
		} else if (deFlag == 2)
		{
			for (int i = 4; i < startPos.length - 1; i = i + 2)
			{
				deS[index] = ((startPos[i] & 0xff) * 256 + (startPos[i + 1] & 0xff)) + deS[index - 1];
				// System.out.print(deS[index-1]+" "+deS[index]+" "+((startPos[i]&0xff)*256 +
				// (startPos[i+1]&0xff))+"\t");
				index++;
			}
		} else if (deFlag == 3)
		{
			for (int i = 4; i < startPos.length - 1; i = i + 3)
			{
				deS[index] = ((startPos[i] & 0xff) * 65536 + (startPos[i + 1] & 0xff) * 256 + (startPos[i + 2] & 0xff))
						+ deS[index - 1];
				index++;
			}
		} else
		{
			for (int i = 4; i < startPos.length - 1; i = i + 4)
			{
				deS[index] = ((startPos[i] & 0xff) * 16777216 + (startPos[i + 1] & 0xff) * 65536
						+ (startPos[i + 3] & 0xff) * 256 + (startPos[i + 4] & 0xff)) + deS[index - 1];
				index++;
			}
		}
		return deS;
	}

	private static void print(int[] start)
	{
		for (int j = 0; j < start.length; j++)
		{
			System.out.print(start[j] + " ");
		}
		System.out.println();
	}

	private static void deStartIsTrue(int[] start, int[] deS)
	{
		System.out.println();
		for (int i = 0; i < deS.length && i < start.length; i++)
		{
			if (deS[i] != start[i])
			{
				System.out.println("Error!!!!	" + i);
				break;
			}
		}
		System.out.println("The process of start is right!");
	}

	private static void deStartIsTrue(List<ReadsHorizonModel> readsHorizon, int[] deS)
	{
		System.out.println();
		for (int i = 0; i < deS.length && i < readsHorizon.size(); i++)
		{
			if (deS[i] != readsHorizon.get(i).getAlignmentStart())
			{
				System.out.println("Error!!!!	" + i);
				break;
			}
		}
		System.out.println("The process of start is right!");

	}

	private char[] getChars(byte[] bytes)
	{
		Charset cs = Charset.forName("UTF-8");
		ByteBuffer bb = ByteBuffer.allocate(bytes.length);
		bb.put(bytes);
		bb.flip();
		CharBuffer cb = cs.decode(bb);
		return cb.array();
	}

	public static char byteToChar(byte[] b)
	{
		char c = (char) (((b[0] & 0xFF) << 8) | (b[1] & 0xFF));
		return c;
	}
	
	 public static String toMemoryInfo() {
		 
	       Runtime currRuntime = Runtime.getRuntime ();
	       int nFreeMemory = ( int ) (currRuntime.freeMemory() / 1024 / 1024);
	       int nTotalMemory = ( int ) (currRuntime.totalMemory() / 1024 / 1024);
	       int nMaxMemory = ( int ) (currRuntime.maxMemory() / 1024 / 1024);
	       return nFreeMemory + "M/" + nTotalMemory + "M/" + nMaxMemory + "M(free/total/Max)" ;
	    }

	 /**
	  * 局部解压缩，显示操作
	  * @param start
	  * @param minStart
	  * @param readsResult
	  */
	private static ArrayList<ArrayList<Integer>> DecodeTPBWT(AuxiliaryInfo count, int minStart, byte[] byteKV)
	{
		ArrayList<ArrayList<Integer>> reListsPBWT = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> pbwt = new ArrayList<Integer>();
		
		int curPos = 0;
		int difference = 0;
		int KVlen = 0;
		int tempByte = 0;
		int tempByte2 = 0;
		int value = -1;
		int key = -1;
		int lenFlag = -1;
		int lastkey = -1;
		boolean oneFlag = false;
		boolean zeroFlag =false;
		if (count.start <= minStart)
		{
			count.realstart = minStart;
			// 这种情况起始的count都是0，特殊写一下起始没有必要
//			count.rowReadsCount = 0;
			count.exCount = 0;
			count.QualCount = 0;
			count.exQualCount = 0;
			int i = 0;
			KVlen = byteKV.length;
			curPos = minStart;
			difference = count.end - curPos + 1;
			while (difference > 0 && KVlen > 0)
			{
				tempByte = byteKV[i] + 128;
				key = (tempByte >> 6)%4;
				if (key == 2)
				{
					value = (tempByte >> 4)%4;
					// 暂时还没确定2的存储格式
//					lenFlag = (tempByte >> 5)%2;
//					if (lenFlag == 1)
//					{
//						i++;
//						KVlen--;
//					}
					if (value == 1)
					{
						// 0,1混合或者空或者全是3
						if (zeroFlag && oneFlag)
						{
							// 0,1混合情况
							count.QualCount++;
							count.exCount++;
							count.exQualCount++;
						}
						else if (lastkey!=2)
						{
							// 上一个value不是2说明该列不为空
							// 暂时认为情况一样
							// 为3情况
							// 所有计数器不变
						}
						else
						{
							// 上述条件一个都不满足
							// 为空情况
							// 所有计数器不变
						}
						
					}
					else if (value == 2)
					{
						// 全是0的情况
						count.QualCount++;
					}
					else
					{
						// 全是1的情况
						count.exCount++;
						count.exQualCount++;
					}
//					count.rowReadsCount++; // 遇到2就自增1
					difference--;
					reListsPBWT.add(pbwt);
					pbwt = new ArrayList<>();
					zeroFlag = false;
					oneFlag = false;
				}
				else
				{
					if (!zeroFlag && key == 0)
					{
						zeroFlag = true;
					}
					else if (!oneFlag && key == 1)
					{
						oneFlag = true;
					}
					else {
						// 为三的情况
					}
					lenFlag = (tempByte >> 5)%2;
					if (lenFlag == 1)
					{
						i++;
						KVlen--;
						tempByte2 = byteKV[i] + 128;
						value = tempByte%32*256 + tempByte2; // 这里稍有问题，暂时这样
					}
					else 
					{
						value = tempByte%32;
					}
					// 0,1,3情况转换为RL
					while(value--> 0) 
					{
						pbwt.add(key);
					}
				}
				lastkey = key;
				KVlen--;
				i++;
			}
		}
		else
		{
			count.realstart = count.start;
			curPos = count.start;
			difference = count.start - minStart;
			int i = 0;
//			int readsCount = 0;
			KVlen = byteKV.length;
			// 在curpos之前的不存储，只是简单的存储起始的count
			while(KVlen>0)
			{
				if (difference>0)
				{
					tempByte = byteKV[i] + 128;
					key = (tempByte >> 6)%4;
					if (key == 2)
					{
						value = (tempByte >> 4)%4;
//						lenFlag = (tempByte >> 5)%2;
//						if (lenFlag == 1)
//						{
//							i++;
//							KVlen--;
//						}
						if (value == 1)
						{
							// 0,1混合或者空或者全是3
							if (zeroFlag && oneFlag)
							{
								// 0,1混合情况
								count.QualCount++;
								count.exCount++;
								count.exQualCount++;
							}
							else if (lastkey!=2)
							{
								// 上一个value不是2说明该列不为空
								// 暂时认为情况一样
								// 为3情况
								// 所有计数器不变
							}
							else
							{
								// 上述条件一个都不满足
								// 为空情况
								// 所有计数器不变
							}
							
						}
						else if (value == 2)
						{
							// 全是0的情况
							count.QualCount++;
						}
						else
						{
							// 全是1的情况
							count.exCount++;
							count.exQualCount++;
						}
//						readsCount++;
						difference--;
						zeroFlag = false;
						oneFlag = false;
					}
					else
					{
						if (!zeroFlag && key == 0)
						{
							zeroFlag = true;
						}
						else if (!oneFlag && key == 1)
						{
							oneFlag = true;
						}
						else {
							// 为三的情况
						}
						lenFlag = (tempByte >> 5)%2;
						if (lenFlag == 1)
						{
							i++;
							KVlen--;
							tempByte2 = byteKV[i] + 128;
							value = tempByte%32*256 + tempByte2; // 这里稍有问题，暂时这样
						}
						else 
						{
							value = tempByte%32;
						}
						// 0,1,3情况转换为RL
//						while(value--> 0) 
//						{
//							pbwt.add(key);
//						}
					}
					lastkey = key;
					KVlen--;
					i++;
				}
				else
				{
					// 针对difference小于0的情况，也就是进入了需要显示的区域内
//					count.endRowReadsCount = count.rowReadsCount;
					count.endExCount = count.exCount;
					count.endQualCount = count.QualCount;
					count.endExQualCount = count.endExQualCount;
					while(count.end-curPos+difference>=0&&KVlen>0)
					{
						tempByte = byteKV[i] + 128;
						key = (tempByte >> 6)%4;
						if (key == 2)
						{
							value = (tempByte >> 4)%4;
//							lenFlag = (tempByte >> 5)%2;
//							if (lenFlag == 1)
//							{
//								i++;
//								KVlen--;
//							}
							if (value == 1)
							{
								// 0,1混合或者空或者全是3
								if (zeroFlag && oneFlag)
								{
									// 0,1混合情况
									count.endQualCount++;
									count.endExCount++;
									count.endExQualCount++;
//									count.QualCount++;
//									count.exCount++;
//									count.exQualCount++;
								}
								else if (lastkey!=2)
								{
									// 上一个value不是2说明该列不为空
									// 暂时认为情况一样
									// 为3情况
									// 所有计数器不变
									
								}
								else
								{
									// 上述条件一个都不满足
									// 为空情况
									// 所有计数器不变
									
								}
								
							}
							else if (value == 2)
							{
								// 全是0的情况
//								count.QualCount++;
								count.endQualCount++;
							}
							else
							{
								// 全是1的情况
//								count.exCount++;
//								count.exQualCount++;
								count.endExCount++;
								count.endExQualCount++;
							}
//							count.endRowReadsCount++;
							difference--;
							reListsPBWT.add(pbwt);
							pbwt = new ArrayList<>();
							zeroFlag = false;
							oneFlag = false;
						}
						else
						{
							if (!zeroFlag && key == 0)
							{
								zeroFlag = true;
							}
							else if (!oneFlag && key == 1)
							{
								oneFlag = true;
							}
							else {
								// 为三的情况
							}
							lenFlag = (tempByte >> 5)%2;
							if (lenFlag == 1)
							{
								i++;
								KVlen--;
								tempByte2 = byteKV[i] + 128;
								value = tempByte%32*256 + tempByte2; // 这里稍有问题，暂时这样
							}
							else 
							{
								value = tempByte%32;
							}
							// 0,1,3情况转换为RL
							while(value--> 0) 
							{
								pbwt.add(key);
							}
						}
						lastkey = key;
						KVlen--;
						i++;
					}
					break;
				}
			}
			// 当kvlen小于等于0说明start小于整个区间的maxend，输出为空
		}
	
		return reListsPBWT;
	}
	
	/**
	 * 用于局部显示
	 * @param count
	 * @param exceptionResult
	 * @return
	 */
	private static ArrayList<ArrayList<String>> deEncodeExceptionList(AuxiliaryInfo count, byte[] exceptionResult)
	{
		String byte2EncodeResult = "";
		String tempbyte2 = "";
		int num = 0;
		for (int i = 0; i < exceptionResult.length - 2; i++)
		{
			if (exceptionResult[i] == ' ')
			{
				break;
			}
			tempbyte2 = String.format("%8s", Integer.toBinaryString(exceptionResult[i] & 0xFF)).replace(' ', '0');

//			num++;
//			if (num % 2 == 1)
//			{
//				System.out.print(tempbyte2);
//			} else
//			{
//				System.out.println(tempbyte2);
//			}
			byte2EncodeResult += tempbyte2;
		}

		String strTail = String.format("%8s", Integer.toBinaryString(exceptionResult[exceptionResult.length - 2] & 0xFF)).replace(' ', '0')
				+ String.format("%8s", Integer.toBinaryString(exceptionResult[exceptionResult.length - 1] & 0xFF)).replace(' ', '0');
		System.out.println(strTail);
		byte2EncodeResult += strTail;
		Huffman2 huffman = new Huffman2();
		String rateText = "AAACCCTTTGGG||DN";
		huffman.handleRate(rateText);

		String decodedResult = huffman.decodeText(byte2EncodeResult); // bit还原为String
		String decodeResult2 = decodedResult.substring(0, decodedResult.lastIndexOf("|")); // 去掉最后的结尾
		// decodeResult2+="|"; // 这里可能会影响速度，之后再改

		ArrayList<ArrayList<String>> deResult = new ArrayList<ArrayList<String>>();
		String[] deTemp = decodeResult2.split("\\|",-1);
		// 点隔开这里明天继续,注意空的问题
		ArrayList<String> templist = new ArrayList<String>();
		for (String str : deTemp)
		{
			if (!str.isEmpty())
			{
				templist.add(str);
			}
			else
			{
				if (num>=count.exCount&&num<=count.endExCount)
				{
					deResult.add(templist);
				}
				templist = new ArrayList<String>();
				num++;
			}
		}

		return deResult;
	}
	
	/**
	 * 适用于正常质量分数局部显示
	 * @param count
	 * @param readQuaReasult
	 * @return
	 */
	private static ArrayList<Character> deEncodeQual(AuxiliaryInfo count, byte[] readQuaReasult)
	{
		ArrayList<Character> dexQual2 = new ArrayList<>();
		char[] deStr = new char[8];
		int startNum = 0;
		int startNumRe = 0;
		int endNum = 0;
		int endNumRe = 0;
		
		startNum = (count.QualCount+1)/8;
		startNumRe = (count.QualCount+1)%8;
		endNum = count.endQualCount/8;
		endNumRe = count.endQualCount%8;
		
		for (int i = startNum; i <= endNum; i++)
		{
			if (i == startNum)
			{
				if (startNum == endNum)
				{
					for (int j = startNumRe; j <=endNumRe; j++)
					{
						switch (j)
						{
						case 0:
							deStr[0] = (char) ((byte) ((((readQuaReasult[i*3] + 128) & 0xff) >> 5) % 8) + 48);
							dexQual2.add(TranforOr(deStr[0]));
							break;
						case 1:
							deStr[1] = (char) ((byte) (((readQuaReasult[i*3] & 0xff) >> 2) % 8) + 48);
							dexQual2.add(TranforOr(deStr[1]));
							break;
						case 2:
							deStr[2] = (char) ((byte) ((((readQuaReasult[i*3] & 0xff) % 4) * 2)
									+ ((((readQuaReasult[i*3 + 1] + 128) & 0xff) >> 7) % 2)) + 48);
							dexQual2.add(TranforOr(deStr[2]));
							break;
						case 3:
							deStr[3] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 4) % 8) + 48);
							dexQual2.add(TranforOr(deStr[3]));
							break;
						case 4:
							deStr[4] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 1) % 8) + 48);
							dexQual2.add(TranforOr(deStr[4]));
							break;
						case 5:
							deStr[5] = (char) ((byte) ((((readQuaReasult[i*3 + 1] & 0xff) % 2) * 4)
									+ ((((readQuaReasult[i*3 + 2] + 128) & 0xff) >> 6) % 4)) + 48);
							dexQual2.add(TranforOr(deStr[5]));
							break;
						case 6:
							deStr[6] = (char) ((byte) (((readQuaReasult[i*3 + 2] & 0xff) >> 3) % 8) + 48);
							dexQual2.add(TranforOr(deStr[6]));
							break;
						case 7:
							deStr[7] = (char) ((byte) ((readQuaReasult[i*3 + 2] & 0xff) % 8) + 48);
							dexQual2.add(TranforOr(deStr[7]));
							break;
						default:
							break;
						}
					}
				}
				else 
				{
					// 这里需要根据 startNumRe来判断到底存几个，用switch
					switch (startNumRe)
					{
					case 0:
						// 并非是一个没有，而是第一个全部加入
						deStr[0] = (char) ((byte) ((((readQuaReasult[i*3] + 128) & 0xff) >> 5) % 8) + 48);
						dexQual2.add(TranforOr(deStr[0]));
						deStr[1] = (char) ((byte) (((readQuaReasult[i*3] & 0xff) >> 2) % 8) + 48);
						dexQual2.add(TranforOr(deStr[1]));
						deStr[2] = (char) ((byte) ((((readQuaReasult[i*3] & 0xff) % 4) * 2)
								+ ((((readQuaReasult[i*3 + 1] + 128) & 0xff) >> 7) % 2)) + 48);
						dexQual2.add(TranforOr(deStr[2]));
						deStr[3] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 4) % 8) + 48);
						dexQual2.add(TranforOr(deStr[3]));
						deStr[4] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 1) % 8) + 48);
						dexQual2.add(TranforOr(deStr[4]));
						deStr[5] = (char) ((byte) ((((readQuaReasult[i*3 + 1] & 0xff) % 2) * 4)
								+ ((((readQuaReasult[i*3 + 2] + 128) & 0xff) >> 6) % 4)) + 48);
						dexQual2.add(TranforOr(deStr[5]));
						deStr[6] = (char) ((byte) (((readQuaReasult[i*3 + 2] & 0xff) >> 3) % 8) + 48);
						dexQual2.add(TranforOr(deStr[6]));
						deStr[7] = (char) ((byte) ((readQuaReasult[i*3 + 2] & 0xff) % 8) + 48);
						dexQual2.add(TranforOr(deStr[7]));
						break;
					case 1:
						deStr[1] = (char) ((byte) (((readQuaReasult[i*3] & 0xff) >> 2) % 8) + 48);
						dexQual2.add(TranforOr(deStr[1]));
						deStr[2] = (char) ((byte) ((((readQuaReasult[i*3] & 0xff) % 4) * 2)
								+ ((((readQuaReasult[i*3 + 1] + 128) & 0xff) >> 7) % 2)) + 48);
						dexQual2.add(TranforOr(deStr[2]));
						deStr[3] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 4) % 8) + 48);
						dexQual2.add(TranforOr(deStr[3]));
						deStr[4] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 1) % 8) + 48);
						dexQual2.add(TranforOr(deStr[4]));
						deStr[5] = (char) ((byte) ((((readQuaReasult[i*3 + 1] & 0xff) % 2) * 4)
								+ ((((readQuaReasult[i*3 + 2] + 128) & 0xff) >> 6) % 4)) + 48);
						dexQual2.add(TranforOr(deStr[5]));
						deStr[6] = (char) ((byte) (((readQuaReasult[i*3 + 2] & 0xff) >> 3) % 8) + 48);
						dexQual2.add(TranforOr(deStr[6]));
						deStr[7] = (char) ((byte) ((readQuaReasult[i*3 + 2] & 0xff) % 8) + 48);
						dexQual2.add(TranforOr(deStr[7]));
						break;
					case 2:
						deStr[2] = (char) ((byte) ((((readQuaReasult[i*3] & 0xff) % 4) * 2)
								+ ((((readQuaReasult[i*3 + 1] + 128) & 0xff) >> 7) % 2)) + 48);
						dexQual2.add(TranforOr(deStr[2]));
						deStr[3] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 4) % 8) + 48);
						dexQual2.add(TranforOr(deStr[3]));
						deStr[4] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 1) % 8) + 48);
						dexQual2.add(TranforOr(deStr[4]));
						deStr[5] = (char) ((byte) ((((readQuaReasult[i*3 + 1] & 0xff) % 2) * 4)
								+ ((((readQuaReasult[i*3 + 2] + 128) & 0xff) >> 6) % 4)) + 48);
						dexQual2.add(TranforOr(deStr[5]));
						deStr[6] = (char) ((byte) (((readQuaReasult[i*3 + 2] & 0xff) >> 3) % 8) + 48);
						dexQual2.add(TranforOr(deStr[6]));
						deStr[7] = (char) ((byte) ((readQuaReasult[i*3 + 2] & 0xff) % 8) + 48);
						dexQual2.add(TranforOr(deStr[7]));
						break;
					case 3:
						deStr[3] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 4) % 8) + 48);
						dexQual2.add(TranforOr(deStr[3]));
						deStr[4] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 1) % 8) + 48);
						dexQual2.add(TranforOr(deStr[4]));
						deStr[5] = (char) ((byte) ((((readQuaReasult[i*3 + 1] & 0xff) % 2) * 4)
								+ ((((readQuaReasult[i*3 + 2] + 128) & 0xff) >> 6) % 4)) + 48);
						dexQual2.add(TranforOr(deStr[5]));
						deStr[6] = (char) ((byte) (((readQuaReasult[i*3 + 2] & 0xff) >> 3) % 8) + 48);
						dexQual2.add(TranforOr(deStr[6]));
						deStr[7] = (char) ((byte) ((readQuaReasult[i*3 + 2] & 0xff) % 8) + 48);
						dexQual2.add(TranforOr(deStr[7]));
						break;
					case 4:
						deStr[4] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 1) % 8) + 48);
						dexQual2.add(TranforOr(deStr[4]));
						deStr[5] = (char) ((byte) ((((readQuaReasult[i*3 + 1] & 0xff) % 2) * 4)
								+ ((((readQuaReasult[i*3 + 2] + 128) & 0xff) >> 6) % 4)) + 48);
						dexQual2.add(TranforOr(deStr[5]));
						deStr[6] = (char) ((byte) (((readQuaReasult[i*3 + 2] & 0xff) >> 3) % 8) + 48);
						dexQual2.add(TranforOr(deStr[6]));
						deStr[7] = (char) ((byte) ((readQuaReasult[i*3 + 2] & 0xff) % 8) + 48);
						dexQual2.add(TranforOr(deStr[7]));
						break;
					case 5:
						deStr[5] = (char) ((byte) ((((readQuaReasult[i*3 + 1] & 0xff) % 2) * 4)
								+ ((((readQuaReasult[i*3 + 2] + 128) & 0xff) >> 6) % 4)) + 48);
						dexQual2.add(TranforOr(deStr[5]));
						deStr[6] = (char) ((byte) (((readQuaReasult[i*3 + 2] & 0xff) >> 3) % 8) + 48);
						dexQual2.add(TranforOr(deStr[6]));
						deStr[7] = (char) ((byte) ((readQuaReasult[i*3 + 2] & 0xff) % 8) + 48);
						dexQual2.add(TranforOr(deStr[7]));
						break;
					case 6:
						deStr[6] = (char) ((byte) (((readQuaReasult[i*3 + 2] & 0xff) >> 3) % 8) + 48);
						dexQual2.add(TranforOr(deStr[6]));
						deStr[7] = (char) ((byte) ((readQuaReasult[i*3 + 2] & 0xff) % 8) + 48);
						dexQual2.add(TranforOr(deStr[7]));
						break;
					case 7:
						deStr[7] = (char) ((byte) ((readQuaReasult[i*3 + 2] & 0xff) % 8) + 48);
						dexQual2.add(TranforOr(deStr[7]));
						break;
					default:
						break;
					}
				}
				
			}
			else if (i == endNum)
			{
				switch (endNumRe)
				{
				case 0:
					// 剩余0个，对于最大范围不会超出，因为qualcount最大表示也是byte的范围
					break;
				case 1:
					deStr[0] = (char) ((byte) ((((readQuaReasult[i*3] + 128) & 0xff) >> 5) % 8) + 48);
					dexQual2.add(TranforOr(deStr[0]));
					break;
				case 2:
					deStr[0] = (char) ((byte) ((((readQuaReasult[i*3] + 128) & 0xff) >> 5) % 8) + 48);
					dexQual2.add(TranforOr(deStr[0]));
					deStr[1] = (char) ((byte) (((readQuaReasult[i*3] & 0xff) >> 2) % 8) + 48);
					dexQual2.add(TranforOr(deStr[1]));
					break;
				case 3:
					deStr[0] = (char) ((byte) ((((readQuaReasult[i*3] + 128) & 0xff) >> 5) % 8) + 48);
					dexQual2.add(TranforOr(deStr[0]));
					deStr[1] = (char) ((byte) (((readQuaReasult[i*3] & 0xff) >> 2) % 8) + 48);
					dexQual2.add(TranforOr(deStr[1]));
					deStr[2] = (char) ((byte) ((((readQuaReasult[i*3] & 0xff) % 4) * 2)
							+ ((((readQuaReasult[i*3 + 1] + 128) & 0xff) >> 7) % 2)) + 48);
					dexQual2.add(TranforOr(deStr[2]));
					break;
				case 4:
					deStr[0] = (char) ((byte) ((((readQuaReasult[i*3] + 128) & 0xff) >> 5) % 8) + 48);
					dexQual2.add(TranforOr(deStr[0]));
					deStr[1] = (char) ((byte) (((readQuaReasult[i*3] & 0xff) >> 2) % 8) + 48);
					dexQual2.add(TranforOr(deStr[1]));
					deStr[2] = (char) ((byte) ((((readQuaReasult[i*3] & 0xff) % 4) * 2)
							+ ((((readQuaReasult[i*3 + 1] + 128) & 0xff) >> 7) % 2)) + 48);
					dexQual2.add(TranforOr(deStr[2]));
					deStr[3] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 4) % 8) + 48);
					dexQual2.add(TranforOr(deStr[3]));
					break;
				case 5:
					deStr[0] = (char) ((byte) ((((readQuaReasult[i*3] + 128) & 0xff) >> 5) % 8) + 48);
					dexQual2.add(TranforOr(deStr[0]));
					deStr[1] = (char) ((byte) (((readQuaReasult[i*3] & 0xff) >> 2) % 8) + 48);
					dexQual2.add(TranforOr(deStr[1]));
					deStr[2] = (char) ((byte) ((((readQuaReasult[i*3] & 0xff) % 4) * 2)
							+ ((((readQuaReasult[i*3 + 1] + 128) & 0xff) >> 7) % 2)) + 48);
					dexQual2.add(TranforOr(deStr[2]));
					deStr[3] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 4) % 8) + 48);
					dexQual2.add(TranforOr(deStr[3]));
					deStr[4] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 1) % 8) + 48);
					dexQual2.add(TranforOr(deStr[4]));
					break;
				case 6:
					deStr[0] = (char) ((byte) ((((readQuaReasult[i*3] + 128) & 0xff) >> 5) % 8) + 48);
					dexQual2.add(TranforOr(deStr[0]));
					deStr[1] = (char) ((byte) (((readQuaReasult[i*3] & 0xff) >> 2) % 8) + 48);
					dexQual2.add(TranforOr(deStr[1]));
					deStr[2] = (char) ((byte) ((((readQuaReasult[i*3] & 0xff) % 4) * 2)
							+ ((((readQuaReasult[i*3 + 1] + 128) & 0xff) >> 7) % 2)) + 48);
					dexQual2.add(TranforOr(deStr[2]));
					deStr[3] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 4) % 8) + 48);
					dexQual2.add(TranforOr(deStr[3]));
					deStr[4] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 1) % 8) + 48);
					dexQual2.add(TranforOr(deStr[4]));
					deStr[5] = (char) ((byte) ((((readQuaReasult[i*3 + 1] & 0xff) % 2) * 4)
							+ ((((readQuaReasult[i*3 + 2] + 128) & 0xff) >> 6) % 4)) + 48);
					dexQual2.add(TranforOr(deStr[5]));
					break;
				case 7:
					deStr[0] = (char) ((byte) ((((readQuaReasult[i*3] + 128) & 0xff) >> 5) % 8) + 48);
					dexQual2.add(TranforOr(deStr[0]));
					deStr[1] = (char) ((byte) (((readQuaReasult[i*3] & 0xff) >> 2) % 8) + 48);
					dexQual2.add(TranforOr(deStr[1]));
					deStr[2] = (char) ((byte) ((((readQuaReasult[i*3] & 0xff) % 4) * 2)
							+ ((((readQuaReasult[i*3 + 1] + 128) & 0xff) >> 7) % 2)) + 48);
					dexQual2.add(TranforOr(deStr[2]));
					deStr[3] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 4) % 8) + 48);
					dexQual2.add(TranforOr(deStr[3]));
					deStr[4] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 1) % 8) + 48);
					dexQual2.add(TranforOr(deStr[4]));
					deStr[5] = (char) ((byte) ((((readQuaReasult[i*3 + 1] & 0xff) % 2) * 4)
							+ ((((readQuaReasult[i*3 + 2] + 128) & 0xff) >> 6) % 4)) + 48);
					dexQual2.add(TranforOr(deStr[5]));
					deStr[6] = (char) ((byte) (((readQuaReasult[i*3 + 2] & 0xff) >> 3) % 8) + 48);
					dexQual2.add(TranforOr(deStr[6]));
					break;
				default:
					break;
				}
			}
			else
			{
				// 永远不可能是最后，这是中间过程
				deStr[0] = (char) ((byte) ((((readQuaReasult[i*3] + 128) & 0xff) >> 5) % 8) + 48);
				dexQual2.add(TranforOr(deStr[0]));
				deStr[1] = (char) ((byte) (((readQuaReasult[i*3] & 0xff) >> 2) % 8) + 48);
				dexQual2.add(TranforOr(deStr[1]));
				deStr[2] = (char) ((byte) ((((readQuaReasult[i*3] & 0xff) % 4) * 2)
						+ ((((readQuaReasult[i*3 + 1] + 128) & 0xff) >> 7) % 2)) + 48);
				dexQual2.add(TranforOr(deStr[2]));
				deStr[3] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 4) % 8) + 48);
				dexQual2.add(TranforOr(deStr[3]));
				deStr[4] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 1) % 8) + 48);
				dexQual2.add(TranforOr(deStr[4]));
				deStr[5] = (char) ((byte) ((((readQuaReasult[i*3 + 1] & 0xff) % 2) * 4)
						+ ((((readQuaReasult[i*3 + 2] + 128) & 0xff) >> 6) % 4)) + 48);
				dexQual2.add(TranforOr(deStr[5]));
				deStr[6] = (char) ((byte) (((readQuaReasult[i*3 + 2] & 0xff) >> 3) % 8) + 48);
				dexQual2.add(TranforOr(deStr[6]));
				deStr[7] = (char) ((byte) ((readQuaReasult[i*3 + 2] & 0xff) % 8) + 48);
				dexQual2.add(TranforOr(deStr[7]));
			}
		}
		
		return dexQual2;

	}
	
	/**
	 * 适用于异常质量分数局部显示
	 * @param count
	 * @param readQuaReasult
	 * @return
	 */
	private static ArrayList<Character> deEncodeExQual(AuxiliaryInfo count, byte[] readQuaReasult)
	{
		ArrayList<Character> dexQual2 = new ArrayList<>();
		char[] deStr = new char[8];
		int startNum = 0;
		int startNumRe = 0;
		int endNum = 0;
		int endNumRe = 0;
		
		startNum = (count.exQualCount+1)/8;
		startNumRe = (count.exQualCount+1)%8;
		endNum = count.endExQualCount/8;
		endNumRe = count.endExQualCount%8;
		
		for (int i = startNum; i <= endNum; i++)
		{
			if (i == startNum)
			{
				if (startNum == endNum)
				{
					for (int j = startNumRe; j <=endNumRe; j++)
					{
						switch (j)
						{
						case 0:
							deStr[0] = (char) ((byte) ((((readQuaReasult[i*3] + 128) & 0xff) >> 5) % 8) + 48);
							dexQual2.add(TranforOr(deStr[0]));
							break;
						case 1:
							deStr[1] = (char) ((byte) (((readQuaReasult[i*3] & 0xff) >> 2) % 8) + 48);
							dexQual2.add(TranforOr(deStr[1]));
							break;
						case 2:
							deStr[2] = (char) ((byte) ((((readQuaReasult[i*3] & 0xff) % 4) * 2)
									+ ((((readQuaReasult[i*3 + 1] + 128) & 0xff) >> 7) % 2)) + 48);
							dexQual2.add(TranforOr(deStr[2]));
							break;
						case 3:
							deStr[3] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 4) % 8) + 48);
							dexQual2.add(TranforOr(deStr[3]));
							break;
						case 4:
							deStr[4] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 1) % 8) + 48);
							dexQual2.add(TranforOr(deStr[4]));
							break;
						case 5:
							deStr[5] = (char) ((byte) ((((readQuaReasult[i*3 + 1] & 0xff) % 2) * 4)
									+ ((((readQuaReasult[i*3 + 2] + 128) & 0xff) >> 6) % 4)) + 48);
							dexQual2.add(TranforOr(deStr[5]));
							break;
						case 6:
							deStr[6] = (char) ((byte) (((readQuaReasult[i*3 + 2] & 0xff) >> 3) % 8) + 48);
							dexQual2.add(TranforOr(deStr[6]));
							break;
						case 7:
							deStr[7] = (char) ((byte) ((readQuaReasult[i*3 + 2] & 0xff) % 8) + 48);
							dexQual2.add(TranforOr(deStr[7]));
							break;
						default:
							break;
						}
					}
				}
				else
				{
					// 这里需要根据 startNumRe来判断到底存几个，用switch
					switch (startNumRe)
					{
					case 0:
						// 并非是一个没有，而是第一个全部加入
						deStr[0] = (char) ((byte) ((((readQuaReasult[i*3] + 128) & 0xff) >> 5) % 8) + 48);
						dexQual2.add(TranforOr(deStr[0]));
						deStr[1] = (char) ((byte) (((readQuaReasult[i*3] & 0xff) >> 2) % 8) + 48);
						dexQual2.add(TranforOr(deStr[1]));
						deStr[2] = (char) ((byte) ((((readQuaReasult[i*3] & 0xff) % 4) * 2)
								+ ((((readQuaReasult[i*3 + 1] + 128) & 0xff) >> 7) % 2)) + 48);
						dexQual2.add(TranforOr(deStr[2]));
						deStr[3] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 4) % 8) + 48);
						dexQual2.add(TranforOr(deStr[3]));
						deStr[4] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 1) % 8) + 48);
						dexQual2.add(TranforOr(deStr[4]));
						deStr[5] = (char) ((byte) ((((readQuaReasult[i*3 + 1] & 0xff) % 2) * 4)
								+ ((((readQuaReasult[i*3 + 2] + 128) & 0xff) >> 6) % 4)) + 48);
						dexQual2.add(TranforOr(deStr[5]));
						deStr[6] = (char) ((byte) (((readQuaReasult[i*3 + 2] & 0xff) >> 3) % 8) + 48);
						dexQual2.add(TranforOr(deStr[6]));
						deStr[7] = (char) ((byte) ((readQuaReasult[i*3 + 2] & 0xff) % 8) + 48);
						dexQual2.add(TranforOr(deStr[7]));
						break;
					case 1:
						deStr[1] = (char) ((byte) (((readQuaReasult[i*3] & 0xff) >> 2) % 8) + 48);
						dexQual2.add(TranforOr(deStr[1]));
						deStr[2] = (char) ((byte) ((((readQuaReasult[i*3] & 0xff) % 4) * 2)
								+ ((((readQuaReasult[i*3 + 1] + 128) & 0xff) >> 7) % 2)) + 48);
						dexQual2.add(TranforOr(deStr[2]));
						deStr[3] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 4) % 8) + 48);
						dexQual2.add(TranforOr(deStr[3]));
						deStr[4] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 1) % 8) + 48);
						dexQual2.add(TranforOr(deStr[4]));
						deStr[5] = (char) ((byte) ((((readQuaReasult[i*3 + 1] & 0xff) % 2) * 4)
								+ ((((readQuaReasult[i*3 + 2] + 128) & 0xff) >> 6) % 4)) + 48);
						dexQual2.add(TranforOr(deStr[5]));
						deStr[6] = (char) ((byte) (((readQuaReasult[i*3 + 2] & 0xff) >> 3) % 8) + 48);
						dexQual2.add(TranforOr(deStr[6]));
						deStr[7] = (char) ((byte) ((readQuaReasult[i*3 + 2] & 0xff) % 8) + 48);
						dexQual2.add(TranforOr(deStr[7]));
						break;
					case 2:
						deStr[2] = (char) ((byte) ((((readQuaReasult[i*3] & 0xff) % 4) * 2)
								+ ((((readQuaReasult[i*3 + 1] + 128) & 0xff) >> 7) % 2)) + 48);
						dexQual2.add(TranforOr(deStr[2]));
						deStr[3] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 4) % 8) + 48);
						dexQual2.add(TranforOr(deStr[3]));
						deStr[4] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 1) % 8) + 48);
						dexQual2.add(TranforOr(deStr[4]));
						deStr[5] = (char) ((byte) ((((readQuaReasult[i*3 + 1] & 0xff) % 2) * 4)
								+ ((((readQuaReasult[i*3 + 2] + 128) & 0xff) >> 6) % 4)) + 48);
						dexQual2.add(TranforOr(deStr[5]));
						deStr[6] = (char) ((byte) (((readQuaReasult[i*3 + 2] & 0xff) >> 3) % 8) + 48);
						dexQual2.add(TranforOr(deStr[6]));
						deStr[7] = (char) ((byte) ((readQuaReasult[i*3 + 2] & 0xff) % 8) + 48);
						dexQual2.add(TranforOr(deStr[7]));
						break;
					case 3:
						deStr[3] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 4) % 8) + 48);
						dexQual2.add(TranforOr(deStr[3]));
						deStr[4] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 1) % 8) + 48);
						dexQual2.add(TranforOr(deStr[4]));
						deStr[5] = (char) ((byte) ((((readQuaReasult[i*3 + 1] & 0xff) % 2) * 4)
								+ ((((readQuaReasult[i*3 + 2] + 128) & 0xff) >> 6) % 4)) + 48);
						dexQual2.add(TranforOr(deStr[5]));
						deStr[6] = (char) ((byte) (((readQuaReasult[i*3 + 2] & 0xff) >> 3) % 8) + 48);
						dexQual2.add(TranforOr(deStr[6]));
						deStr[7] = (char) ((byte) ((readQuaReasult[i*3 + 2] & 0xff) % 8) + 48);
						dexQual2.add(TranforOr(deStr[7]));
						break;
					case 4:
						deStr[4] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 1) % 8) + 48);
						dexQual2.add(TranforOr(deStr[4]));
						deStr[5] = (char) ((byte) ((((readQuaReasult[i*3 + 1] & 0xff) % 2) * 4)
								+ ((((readQuaReasult[i*3 + 2] + 128) & 0xff) >> 6) % 4)) + 48);
						dexQual2.add(TranforOr(deStr[5]));
						deStr[6] = (char) ((byte) (((readQuaReasult[i*3 + 2] & 0xff) >> 3) % 8) + 48);
						dexQual2.add(TranforOr(deStr[6]));
						deStr[7] = (char) ((byte) ((readQuaReasult[i*3 + 2] & 0xff) % 8) + 48);
						dexQual2.add(TranforOr(deStr[7]));
						break;
					case 5:
						deStr[5] = (char) ((byte) ((((readQuaReasult[i*3 + 1] & 0xff) % 2) * 4)
								+ ((((readQuaReasult[i*3 + 2] + 128) & 0xff) >> 6) % 4)) + 48);
						dexQual2.add(TranforOr(deStr[5]));
						deStr[6] = (char) ((byte) (((readQuaReasult[i*3 + 2] & 0xff) >> 3) % 8) + 48);
						dexQual2.add(TranforOr(deStr[6]));
						deStr[7] = (char) ((byte) ((readQuaReasult[i*3 + 2] & 0xff) % 8) + 48);
						dexQual2.add(TranforOr(deStr[7]));
						break;
					case 6:
						deStr[6] = (char) ((byte) (((readQuaReasult[i*3 + 2] & 0xff) >> 3) % 8) + 48);
						dexQual2.add(TranforOr(deStr[6]));
						deStr[7] = (char) ((byte) ((readQuaReasult[i*3 + 2] & 0xff) % 8) + 48);
						dexQual2.add(TranforOr(deStr[7]));
						break;
					case 7:
						deStr[7] = (char) ((byte) ((readQuaReasult[i*3 + 2] & 0xff) % 8) + 48);
						dexQual2.add(TranforOr(deStr[7]));
						break;
					default:
						break;
					}
				}
			}
			else if (i == endNum)
			{
				switch (endNumRe)
				{
				case 0:
					// 剩余0个，对于最大范围不会超出，因为qualcount最大表示也是byte的范围
					break;
				case 1:
					deStr[0] = (char) ((byte) ((((readQuaReasult[i*3] + 128) & 0xff) >> 5) % 8) + 48);
					dexQual2.add(TranforOr(deStr[0]));
					break;
				case 2:
					deStr[0] = (char) ((byte) ((((readQuaReasult[i*3] + 128) & 0xff) >> 5) % 8) + 48);
					dexQual2.add(TranforOr(deStr[0]));
					deStr[1] = (char) ((byte) (((readQuaReasult[i*3] & 0xff) >> 2) % 8) + 48);
					dexQual2.add(TranforOr(deStr[1]));
					break;
				case 3:
					deStr[0] = (char) ((byte) ((((readQuaReasult[i*3] + 128) & 0xff) >> 5) % 8) + 48);
					dexQual2.add(TranforOr(deStr[0]));
					deStr[1] = (char) ((byte) (((readQuaReasult[i*3] & 0xff) >> 2) % 8) + 48);
					dexQual2.add(TranforOr(deStr[1]));
					deStr[2] = (char) ((byte) ((((readQuaReasult[i*3] & 0xff) % 4) * 2)
							+ ((((readQuaReasult[i*3 + 1] + 128) & 0xff) >> 7) % 2)) + 48);
					dexQual2.add(TranforOr(deStr[2]));
					break;
				case 4:
					deStr[0] = (char) ((byte) ((((readQuaReasult[i*3] + 128) & 0xff) >> 5) % 8) + 48);
					dexQual2.add(TranforOr(deStr[0]));
					deStr[1] = (char) ((byte) (((readQuaReasult[i*3] & 0xff) >> 2) % 8) + 48);
					dexQual2.add(TranforOr(deStr[1]));
					deStr[2] = (char) ((byte) ((((readQuaReasult[i*3] & 0xff) % 4) * 2)
							+ ((((readQuaReasult[i*3 + 1] + 128) & 0xff) >> 7) % 2)) + 48);
					dexQual2.add(TranforOr(deStr[2]));
					deStr[3] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 4) % 8) + 48);
					dexQual2.add(TranforOr(deStr[3]));
					break;
				case 5:
					deStr[0] = (char) ((byte) ((((readQuaReasult[i*3] + 128) & 0xff) >> 5) % 8) + 48);
					dexQual2.add(TranforOr(deStr[0]));
					deStr[1] = (char) ((byte) (((readQuaReasult[i*3] & 0xff) >> 2) % 8) + 48);
					dexQual2.add(TranforOr(deStr[1]));
					deStr[2] = (char) ((byte) ((((readQuaReasult[i*3] & 0xff) % 4) * 2)
							+ ((((readQuaReasult[i*3 + 1] + 128) & 0xff) >> 7) % 2)) + 48);
					dexQual2.add(TranforOr(deStr[2]));
					deStr[3] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 4) % 8) + 48);
					dexQual2.add(TranforOr(deStr[3]));
					deStr[4] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 1) % 8) + 48);
					dexQual2.add(TranforOr(deStr[4]));
					break;
				case 6:
					deStr[0] = (char) ((byte) ((((readQuaReasult[i*3] + 128) & 0xff) >> 5) % 8) + 48);
					dexQual2.add(TranforOr(deStr[0]));
					deStr[1] = (char) ((byte) (((readQuaReasult[i*3] & 0xff) >> 2) % 8) + 48);
					dexQual2.add(TranforOr(deStr[1]));
					deStr[2] = (char) ((byte) ((((readQuaReasult[i*3] & 0xff) % 4) * 2)
							+ ((((readQuaReasult[i*3 + 1] + 128) & 0xff) >> 7) % 2)) + 48);
					dexQual2.add(TranforOr(deStr[2]));
					deStr[3] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 4) % 8) + 48);
					dexQual2.add(TranforOr(deStr[3]));
					deStr[4] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 1) % 8) + 48);
					dexQual2.add(TranforOr(deStr[4]));
					deStr[5] = (char) ((byte) ((((readQuaReasult[i*3 + 1] & 0xff) % 2) * 4)
							+ ((((readQuaReasult[i*3 + 2] + 128) & 0xff) >> 6) % 4)) + 48);
					dexQual2.add(TranforOr(deStr[5]));
					break;
				case 7:
					deStr[0] = (char) ((byte) ((((readQuaReasult[i*3] + 128) & 0xff) >> 5) % 8) + 48);
					dexQual2.add(TranforOr(deStr[0]));
					deStr[1] = (char) ((byte) (((readQuaReasult[i*3] & 0xff) >> 2) % 8) + 48);
					dexQual2.add(TranforOr(deStr[1]));
					deStr[2] = (char) ((byte) ((((readQuaReasult[i*3] & 0xff) % 4) * 2)
							+ ((((readQuaReasult[i*3 + 1] + 128) & 0xff) >> 7) % 2)) + 48);
					dexQual2.add(TranforOr(deStr[2]));
					deStr[3] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 4) % 8) + 48);
					dexQual2.add(TranforOr(deStr[3]));
					deStr[4] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 1) % 8) + 48);
					dexQual2.add(TranforOr(deStr[4]));
					deStr[5] = (char) ((byte) ((((readQuaReasult[i*3 + 1] & 0xff) % 2) * 4)
							+ ((((readQuaReasult[i*3 + 2] + 128) & 0xff) >> 6) % 4)) + 48);
					dexQual2.add(TranforOr(deStr[5]));
					deStr[6] = (char) ((byte) (((readQuaReasult[i*3 + 2] & 0xff) >> 3) % 8) + 48);
					dexQual2.add(TranforOr(deStr[6]));
					break;
				default:
					break;
				}
			}
			else
			{
				// 永远不可能是最后，这是中间过程
				deStr[0] = (char) ((byte) ((((readQuaReasult[i*3] + 128) & 0xff) >> 5) % 8) + 48);
				dexQual2.add(TranforOr(deStr[0]));
				deStr[1] = (char) ((byte) (((readQuaReasult[i*3] & 0xff) >> 2) % 8) + 48);
				dexQual2.add(TranforOr(deStr[1]));
				deStr[2] = (char) ((byte) ((((readQuaReasult[i*3] & 0xff) % 4) * 2)
						+ ((((readQuaReasult[i*3 + 1] + 128) & 0xff) >> 7) % 2)) + 48);
				dexQual2.add(TranforOr(deStr[2]));
				deStr[3] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 4) % 8) + 48);
				dexQual2.add(TranforOr(deStr[3]));
				deStr[4] = (char) ((byte) (((readQuaReasult[i*3 + 1] & 0xff) >> 1) % 8) + 48);
				dexQual2.add(TranforOr(deStr[4]));
				deStr[5] = (char) ((byte) ((((readQuaReasult[i*3 + 1] & 0xff) % 2) * 4)
						+ ((((readQuaReasult[i*3 + 2] + 128) & 0xff) >> 6) % 4)) + 48);
				dexQual2.add(TranforOr(deStr[5]));
				deStr[6] = (char) ((byte) (((readQuaReasult[i*3 + 2] & 0xff) >> 3) % 8) + 48);
				dexQual2.add(TranforOr(deStr[6]));
				deStr[7] = (char) ((byte) ((readQuaReasult[i*3 + 2] & 0xff) % 8) + 48);
				dexQual2.add(TranforOr(deStr[7]));
			}
		}
		
		return dexQual2;

	}
	
	/**
	 * 局部显示解压缩
	 * @param reResult
	 * @return
	 */
	private static ArrayList<DeRead> partPBWTAlgoRe(ReadPbwtResult reResult)
	{
		ArrayList<ArrayList<Integer>> pbwtResult = new ArrayList<>();
		
		ArrayList<DeRead> readsCurTemp = new ArrayList<>();
		ArrayList<DeRead> result = new ArrayList<>();

		int MinStart = reResult.getMinStart();
		pbwtResult = reResult.getListsPBWT();
		int curValue = -1;
		char curReadQual;
		char curExQual;
		String curEx = null;
		
		int qualIndex = 0;
		int exQualIndex = 0;
		int exIndex = 0;
		
		ArrayList<Integer> reIndex;	// 构建当前列索引
		ArrayList<ArrayList<Integer>> Index = new ArrayList<>(); // 所有索引信息
	
		int indexpos = -1;
		int posValue = -1;

		ArrayList<Integer> c;
		ArrayList<Integer> d;
		
		boolean oneFlag = false;
		boolean zeroFlag = false;
		
		int preSize = 0; // 定义之前的碱基值和之前列的大小
		int currSize = 0, currAddSize = 0; // 定义当前的
		ArrayList<Integer> removeIndexRe = new ArrayList<Integer>();
		Boolean removeFlag = false;
		int pre = 0; // 索引空间大小
		int cur = 0; // currentlist当前被删除元素之前序列数量
		LinkedList<Integer> posIndex = new LinkedList<Integer>(); // 位置索引表，表示插入到输出list中位置
		// 比对完之后从左往右
		for(int col = 0; col < pbwtResult.size(); col++)
		{
			preSize = readsCurTemp.size(); // 前一列大小用currlist大小表示
			currSize = pbwtResult.get(col).size(); // 用取到pbwt当前列表示
			currAddSize = currSize > preSize ? (currSize - preSize) : 0; // 新加入序列大小
			// 构造还原空间
			for (int i = 0; i < currAddSize; i++)
			{
				DeRead listTemp = new DeRead();
				listTemp.setStart(MinStart+col); // 在创建序列的时候设置起始位置信息
				readsCurTemp.add(listTemp);
				if (col != 0)
				{
					int length = Index.size() - 1;
					int length2= Index.get(length).size(); // 会不断更新
					Index.get(length).add(length2);
				}
			}
			if (!readsCurTemp.isEmpty())
			{
				c = new ArrayList<>();
				d = new ArrayList<>();
				// 第一列或者是前一列为空列
				if (col == 0)
				{
					reIndex = new ArrayList<>();
					int preIndex = 0;
					
					for (int i = 0; i < readsCurTemp.size(); i++)
					{
						curValue = pbwtResult.get(col).get(i);
						preIndex = i;
						readsCurTemp.get(i).getRead().add(curValue);
						if (curValue == 0)
						{
							// 加入质量分数,一列都是一个值
							curReadQual = reResult.getListsQual().get(qualIndex);
							readsCurTemp.get(i).getReadQual().add(curReadQual);
							readsCurTemp.get(i).getQual().add(curReadQual);
							c.add(preIndex);
							zeroFlag = true;
						}
						else if (curValue == 1) {
							// 添加异常信息，一列都是一个质量分数
							curExQual = reResult.getListsQual().get(exQualIndex);
							curEx = reResult.getListsExcep().get(exIndex).get(0);
							readsCurTemp.get(i).getExQual().add(curExQual);
							readsCurTemp.get(i).getQual().add(curExQual);
							readsCurTemp.get(i).getExBase().add(curEx);
							reResult.getListsExcep().get(exIndex).remove(0);
							d.add(preIndex);
							oneFlag = true;
						}
						else {
							// 当前为三，一般不可能
						}
						reIndex.add(i);
					}
					if (oneFlag)
					{
						exQualIndex++;
						exIndex++;
						oneFlag = false;
					}
					if (zeroFlag)
					{
						qualIndex++;
						zeroFlag = false;
					}
					c.addAll(d);
					Index.add(reIndex);
					Index.add(c);
					continue;
				}
				// 对于RL纵向序列采用逆TPBWT还原
				for (int row = 0; row < currSize; row++)
				{
					indexpos = Index.get(col).get(row);
					posValue = pbwtResult.get(col).get(row);
					if (posValue == 0){
						c.add(indexpos);
						readsCurTemp.get(indexpos).getRead().add(posValue);
						curReadQual = reResult.getListsQual().get(qualIndex);
						readsCurTemp.get(indexpos).getReadQual().add(curReadQual);
						readsCurTemp.get(indexpos).getQual().add(curReadQual); // 不要忘记在最后对计数器改变值
						zeroFlag = true;
						if (col == pbwtResult.size())
						{
							removeIndexRe.add(indexpos);
							removeFlag = true;
						}
						
					}
					else if (posValue == 1) {
						d.add(indexpos);
						readsCurTemp.get(indexpos).getRead().add(posValue);
						curExQual = reResult.getListsQual().get(exQualIndex);
						curEx = reResult.getListsExcep().get(exIndex).get(0);
						readsCurTemp.get(indexpos).getExQual().add(curExQual);
						readsCurTemp.get(indexpos).getExBase().add(curEx);
						reResult.getListsExcep().get(exIndex).remove(0);
						oneFlag = true;
						if (col == pbwtResult.size())
						{
							removeIndexRe.add(indexpos);
							removeFlag = true;
						}
					}
					else if (posValue == ReadElemEnum.END.ordinal()) {
						readsCurTemp.get(indexpos).getRead().add(posValue);
						removeIndexRe.add(indexpos);
						removeFlag = true;
					}
					else {
						// 这个位置留给情况2
						// 索引的处理放在后面，
					}
				}
				// 在这里构建下一列的索引
				// 并且该自增1的在这里判断
				if (oneFlag)
				{
					exQualIndex++;
					exIndex++;
					oneFlag = false;
				}
				if (zeroFlag)
				{
					qualIndex++;
					zeroFlag = false;
				}
				
				// 在这里构建下一列的索引
				// 改一下，放在后面需要进行一步删除操作
				c.addAll(d);
				
				if (removeFlag == true)
				{
					// 删除结尾为3的序列并且同时改变索引表
					int offset = 0;
					Collections.sort(removeIndexRe);
					for (Integer ins : removeIndexRe)
					{
						pre = posIndex.size();
						cur = ins - offset;
						// 删除序列操作
						if (pre == cur)
						{
							// 加在末尾
							result.add(readsCurTemp.get(ins - offset));
							readsCurTemp.remove(ins - offset);
							
						}
						else if (pre<cur) {
							for (int i = 0; i < cur-pre; i++)
							{
								DeRead empty = new DeRead();
								result.add(empty);
								posIndex.add(result.size() - 1);
							}
							result.add(readsCurTemp.get(ins - offset));
							readsCurTemp.remove(ins - offset);
						}
						else {
							// 此时表明之前没有出去的序列要出去
							int pos = posIndex.get(cur);
							posIndex.remove(cur);
							result.set(pos, readsCurTemp.get(ins - offset));
							readsCurTemp.remove(ins - offset);
						}
						// 改变索引表
						int tempIndex = -1;
						for (int i = 0; i < c.size(); i++)
						{
							if ((ins - offset)<=c.get(i))
							{
								tempIndex = c.get(i)-1;
								c.set(i, tempIndex);
							}
//							else if ((ins - offset) == c.get(i))
//							{
//								c.remove(i);
//								i--;
//							}
//							else {
//								
//							}
						}
						
						offset++;
					}
					removeIndexRe.clear();
					removeFlag = false;
				}
				
				// 在这里构建下一列的索引
				// 改一下，放在后面需要进行一步删除操作
				Index.add(c);
				
			}
			else
			{
				// 这里是针对该列为空的情况，单独设计
				ArrayList<Integer> emptyIndex = new ArrayList<>();
				Index.add(emptyIndex);
			}
			
		}
	return result;	
	}
	
	
}
