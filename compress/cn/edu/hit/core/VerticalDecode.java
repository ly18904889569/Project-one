package cn.edu.hit.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import cn.edu.hit.model.CompressResult;
import cn.edu.hit.model.ReadPbwtResult;
import cn.edu.hit.util.Huffman2;

public class VerticalDecode
{
	private ReadPbwtResult reCompressRes;
	
	public VerticalDecode(VerticalEncode compressResult)
	{
		reCompressRes = new ReadPbwtResult();
		reCompressRes = DeCodePbwt(compressResult.getAllRes());
	}

	public VerticalDecode(String byPaht)
	{
		reCompressRes = new ReadPbwtResult();
	}

	private ReadPbwtResult DeCodePbwt(CompressResult compressRes)
	{
		reCompressRes.setStartPos(deStartPos(compressRes.getStartResult()));
		reCompressRes.setListsPBWT(DecodePBWT(compressRes.getReadsResult()));
		reCompressRes.setListsExcep2(Arrays.asList(deEncodeExceptionList(compressRes.getExceptionResult())));
		reCompressRes.setListsQual(deEncodeQual(compressRes.getReadQuaReasult()));
		reCompressRes.setListExQual2(deEncodeExceptionQual(compressRes.getExceptionQuaResult()));
		return null;
	}
	
	private int[] deStartPos(byte[] startPos)
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

	private ArrayList<ArrayList<Integer>> DecodePBWT(byte[] temp)
	{
		Dpbwt depwbt = new Dpbwt();
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
		result = depwbt.PBWTAlgoRe(reList);

		return result;

	}

	private String[] deEncodeExceptionList(byte[] array)
	{
		// 实现解压缩
		StringBuffer byte2EncodeResult2 = new StringBuffer();
		String tempbyte2 = "";
		for (int i = 0; i < array.length - 2; i++)
		{
			tempbyte2 = String.format("%8s", Integer.toBinaryString(array[i] & 0xFF));
			byte2EncodeResult2.append(tempbyte2);
		}
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
		System.out.println(decodeRes2.charAt(decodeRes2.length()-1));

		String[] dexResult = decodeRes2.split("\\|");
		System.out.println("testing the speed");
		return dexResult;
	}

	private ArrayList<Character> deEncodeQual(byte[] pbwtQual)
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

	private ArrayList<Character> deEncodeExceptionQual(byte[] exQual)
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

	private char TranforOr(char ch)
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

	private int deCompressLen(byte[] len2)
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

	private ArrayList<ArrayList<Integer>> reListPbwt(ArrayList<Byte> bKeysFromByte,
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

}
