package cn.edu.hit.core;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import cn.edu.hit.model.CompressResult;
import cn.edu.hit.model.QualEnum;
import cn.edu.hit.model.READSYMBOL;
import cn.edu.hit.model.ReadInfo;
import cn.edu.hit.model.ReadPbwtResult;
import cn.edu.hit.model.ReadsPreProcessResult;
import cn.edu.hit.test.ReadStruct;
import cn.edu.hit.util.Huffman2;

public class VerticalEncode
{
	private CompressResult allRes;
	
	public VerticalEncode(String filePath, int seeds)
	{
//		分线程+重构数据类型
		allRes = new CompressResult();
		ReadsPreProcessResult reads = new ReadsPreProcessResult();
		ReadPreProcess readPreProcess = new ReadPreProcess();
		List<List<ReadInfo>> readInfos = readPreProcess.splitBamFile2(filePath,seeds);
		reads = readPreProcess.readsProc(readInfos.get(0));
		
		this.Init(reads);
//		Init(readPreProcess.readsProc(readInfos.get(1)));
//		Init(readPreProcess.readsProc(readInfos.get(2)));
	}
	
	public VerticalEncode(String filePath, String writePath, int seeds)
	{
		allRes = new CompressResult();
		ReadsPreProcessResult reads = new ReadsPreProcessResult();
		ReadPreProcess readPreProcess = new ReadPreProcess();
		List<List<ReadInfo>> readInfos = readPreProcess.splitBamFile2(filePath,seeds);
		for(int i=0; i<seeds; i++)
		{
			reads = readPreProcess.readsProc(readInfos.get(i));
			this.Init(reads);
			Main.WriteBy(writePath, this, i);
		}
		
	}
	
	private void Init(ReadsPreProcessResult reads)
	{
		ArrayList<ReadStruct> ReadsList = new ArrayList<ReadStruct>();
		ReadPbwtResult pbwtres = new ReadPbwtResult();
		Dpbwt dpbwt = new Dpbwt();
		int[] start = new int[reads.getReadsHorizon().size()];
		int[] end = new int[reads.getReadsHorizon().size()];
		
		System.out.println("Original reads(Test.one.length):\t");
		for (int i = 0; i < reads.getReadsHorizon().size(); i++)
		{
			start[i] = reads.getReadsHorizon().get(i).getAlignmentStart();
			end[i] = reads.getReadsHorizon().get(i).getAlignmentEnd();
		}
//		起始坐标的压缩
		byte[] startPos = compressStartPos(start);
		allRes.setStartResult(startPos);
//		一会修改startpost压缩函数，将输入参数改一下
		
//		需要先搞一个数据结构的转化，先不着急去压缩其他信息
		ReadsList = transferData(reads);
		pbwtres = dpbwt.PBWTAlgo(ReadsList, start, end);
		
//		进行游程编码
		byte[] keyValue = VerRL(pbwtres);
		// 在这里进行余下的几个部分的压缩工作
		byte[] exBy = EncodeExceptionList(pbwtres.getListsExcep());
		System.out.println("Test×*×*×*×*×*×*×*×*×*×*×*×*×*");
		byte[] exQual = EncodeExceptionQual(pbwtres.getListExQual());
		byte[] pbwtQual = EncodePbwtSingleQual(pbwtres.getListsQual());
		
		allRes.setExceptionResult(exBy);
		allRes.setExceptionQuaResult(exQual);
		allRes.setReadQuaReasult(pbwtQual);
		allRes.setReadsResult(keyValue);
		
	}

	private void Compress(ReadsPreProcessResult reads)
	{
		
	}
	/**
	 * @param pbwtres
	 */
	private byte[] VerRL(ReadPbwtResult pbwtres)
	{
		ArrayList<ArrayList<Integer>> pbwtResult = pbwtres.getListsPBWT();
		List<Integer> keys = new ArrayList<Integer>();
		List<Integer> values = new ArrayList<Integer>();
		runLen(keys, values, pbwtResult);
		byte[] value2by = binaryValues(values);
		byte[] key2by = binaryKeys(keys);
		byte[] keyAndValue = (byte[]) ArrayUtils.addAll(value2by, key2by);
		System.out.println("keysAndValues size:\t" + keyAndValue.length);
		int len = value2by.length;
		byte[] len2 = new byte[4];
		compressLen(len, len2);
		keyAndValue = (byte[]) ArrayUtils.addAll(len2, keyAndValue);
		return keyAndValue;
	}

	/**将原始输入数据处理为0,1序列
	 * @param reads
	 */
	private ArrayList<ReadStruct> transferData(ReadsPreProcessResult reads)
	{
		ArrayList<ReadStruct> readsList = new ArrayList<ReadStruct>();
		for (int i = 0; i < reads.getReadsInfo().size(); i++)
		{
			ReadStruct struct = new ReadStruct();
			struct.setStartAlignment(reads.getReadsHorizon().get(i).getAlignmentStart());
			struct.setEndAlignment(reads.getReadsHorizon().get(i).getAlignmentEnd());
			ArrayList<Integer> list = new ArrayList<Integer>();
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
			
			struct.setReads(list);
			struct.setReadQuality(reads.getReadsInfo().get(i).getReadQuality());
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
		return readsList;
	}
	
	private byte[] compressStartPos(int[] start)
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
		return startPos;
	}

	private void compressLen(int len, byte[] len2)
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
	
	private void runLen(List<Integer> keys, List<Integer> values, ArrayList<ArrayList<Integer>> pbwtResult)
	{
		int countZero = 0, countOne = 0, countThree = 0;
		int preVal = -1;
		int curVal = -1;

		for (ArrayList<Integer> listPbwt : pbwtResult)
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
					countZero++;
				} else if (curVal == 1)
				{
					countOne++;
				} else if (curVal == 3)
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

	private byte[] EncodeExceptionList(ArrayList<ArrayList<String>> vE)
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

	private byte[] EncodeExceptionQual(ArrayList<ArrayList<Character>> vE)
	{
		ArrayList<ArrayList<Character>> exceptionQual = vE;
		ArrayList<ArrayList<Integer>> exceptionQualProc = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> keys = new ArrayList<Integer>();

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

		// System.out.println("exception length:"+eQual2By.length);
		System.out.println("the compression of exQual has completed");
		return eQual2By;
	}

	private byte[] EncodePbwtSingleQual(ArrayList<Character> vE)
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


	public CompressResult getAllRes()
	{
		return allRes;
	}


	public void setAllRes(CompressResult allRes)
	{
		this.allRes = allRes;
	}

}
