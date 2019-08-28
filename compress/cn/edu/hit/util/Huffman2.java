package cn.edu.hit.util;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.TreeMap;

public class Huffman2 {

	static PriorityQueue<Node> nodes = new PriorityQueue<>((o1, o2) -> (o1.value < o2.value) ? -1 : 1);
	static TreeMap<Character, String> codes = new TreeMap<>();
	static String encoded = "";
	static String decoded = "";
	static int ASCII[] = new int[128];

	public static void main(String[] args) throws FileNotFoundException {
		String rateText = "ACGTAV";
		String rawText = "AACACAGT";
		handleNewText(rateText, rawText);
	}

	//全是私有的方法,要对外开放一些方法
	public void handleRate(String rateText){
		ASCII = new int[128];
		nodes.clear();
		codes.clear();
		encoded = "";
		decoded = "";
		calculateCharIntervals2(nodes, rateText);
		buildTree(nodes);
		generateCodes(nodes.peek(), "");
		
		printCodes();
	}
	
	private static boolean handleNewText(String rateText,String rawText) {
		String str;
		ASCII = new int[128];
		nodes.clear();
		codes.clear();
		encoded = "";
		decoded = "";
		calculateCharIntervals2(nodes, rateText);
		buildTree(nodes);
		generateCodes(nodes.peek(), "");

		printCodes();
		System.out.println("-- Encoding/Decoding --");		//crazy:暂且注释掉，这个是因为改为字符型数组冲突注释掉
		String result = encodeText(rawText);
		str=decodeText(result);
		System.out.println("Decoding result:"+str);
		return false;
	}

	public  static String decodeText(String encoded) {
		
		decoded = "";
		Node node = nodes.peek();
		System.out.println("cncoded.length:\t"+encoded.length()+"\t" + encoded);
		for (int i = 0; i < encoded.length();) {
			Node tmpNode = node;
			while (tmpNode.left != null && tmpNode.right != null && i < encoded.length()) {
				if (encoded.charAt(i) == '1')
					tmpNode = tmpNode.right;
				else
					tmpNode = tmpNode.left;
				i++;
			}
			
			if (tmpNode != null)
				if (tmpNode.character.length() == 1){
					decoded += tmpNode.character;
				}else
					System.out.println("Input not Valid");	//压缩后的序列反解回来出错

		}
//		System.out.println("Decoded Text: " + decoded);
		return decoded;
	}

	public static String encodeText(String rawText) {
		for (int i = 0; i < rawText.length(); i++)
		{
			
			encoded += codes.get(rawText.charAt(i));	//crazy：这里就是时间主要耗费的地方，我可不可以不拼成一串
			codes.get(rawText.charAt(i));				//crazy:之前的想法是变成一串，现阶段的想法是用数组进行存放。达到多长之后重新开一个，我这里只需要定义一个16位数长的的字符型数组就行
		}
//		System.out.println("Encoded Text: " + encoded);
		return encoded;
	}
	
	public static String[] encodeText2(String rawText) {
		String str1[] = new String[rawText.length()*4/16];	//crazy:这里的大小并不确定，只能按照尽量多的去存放
		Arrays.fill(str1, "");
		String strTmp="";	//crazy:用于存放huffman转换之后的字符串
		int k=0;	//crazy:拼接字符串的索引
		for (int i = 0; i < rawText.length(); i++)
		{
			
//			encoded += codes.get(rawText.charAt(i));	//crazy：这里就是时间主要耗费的地方，我可不可以不拼成一串
			strTmp=codes.get(rawText.charAt(i));				//crazy:之前的想法是变成一串，现阶段的想法是用数组进行存放。达到多长之后重新开一个，我这里只需要定义一个16位数长的的字符型数组就行
			int strTmpSize = strTmp.length();	//crazy：Huffman转换之后字符串的长度
			int strSixteenSize = 16 - str1[k].length();	//crazy:16长度字符串未填满的长度
			//crazy:下面进行拼接操作
			if(strTmpSize<strSixteenSize)
			{
				str1[k] = str1[k] + strTmp;
			}
			else if(strTmpSize == strSixteenSize)
			{
				str1[k] = str1[k]+strTmp;
				k++;
			}
			else
			{
				String str3 = strTmp.substring(0, strSixteenSize);	//截取能够加入的大小
				String str4 = strTmp.substring(strSixteenSize, strTmpSize);	//将剩下的截取保存
				str1[k] = str1[k] + str3;
				str1[++k]+=str4;
			}
			strTmp = "";
		}
//		System.out.println("Encoded Text: " + encoded);
//		return encoded;
		return str1;
	}
	
//	public static String[] encodeTex(String[] rawText)
//	{
//	
//		for (int i = 0 ; i < rawText.length ; i++)
//		{
//			rawText[i] = codes.get(rawText[i].charAt(i));
//		}
//		return rawText;
//	}
	

	private static void buildTree(PriorityQueue<Node> vector) {
		while (vector.size() > 1)
			vector.add(new Node(vector.poll(), vector.poll()));
	}

	private static void printCodes() {
		System.out.println("--- Printing Codes ---");
		codes.forEach((k, v) -> System.out.println("'" + k + "' : " + v));
	}

	
	
	private static void calculateCharIntervals2(PriorityQueue<Node> vector, String text) {
		//可以这样自己预设比例，然后进行计算
		// 可以把这里改造一下,变成传递自己约定的参数比例
		for (int i = 0; i < text.length(); i++)
			ASCII[text.charAt(i)]++;

		for (int i = 0; i < ASCII.length; i++)
			if (ASCII[i] > 0) {
				vector.add(new Node(ASCII[i] / (text.length() * 1.0), ((char) i) + ""));
//				System.out.println("'" + ((char) i) + "' : " + ASCII[i] / (text.length() * 1.0));
			}
	}

	private static void generateCodes(Node node, String s) {
		if (node != null) {
			if (node.right != null)
				generateCodes(node.right, s + "1");

			if (node.left != null)
				generateCodes(node.left, s + "0");

			if (node.left == null && node.right == null)
				codes.put(node.character.charAt(0), s);
		}
	}

}

