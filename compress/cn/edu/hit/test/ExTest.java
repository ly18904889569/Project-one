package cn.edu.hit.test;

import java.nio.ByteBuffer;

import cn.edu.hit.core.MainEncoding2;
import cn.edu.hit.util.Huffman2;

public class ExTest
{

	public static void main(String[] args)
	{
		String orgin = "GCGGG";
		System.out.println(orgin);
		byte[] now = CompressEx(orgin);
		String[] strings = MainEncoding2.deEncodeExceptionList(now);
		for(int i=0; i<strings.length; i++)
		{
			System.out.println(strings[i]);
		}
	}

	/**
	 * @param orgin
	 */
	private static byte[] CompressEx(String orgin)
	{
		Huffman2 huffman = new Huffman2();
		String rateText = "AAACCCTTTGGG||DN";
		huffman.handleRate(rateText);
		StringBuilder rawText = new StringBuilder();
		rawText.append(orgin + "|");
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
		return array;
	}

}
