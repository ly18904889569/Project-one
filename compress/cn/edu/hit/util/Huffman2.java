package cn.edu.hit.util;

import java.io.FileNotFoundException;
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
		System.out.println("-- Encoding/Decoding --");
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
			encoded += codes.get(rawText.charAt(i));
		}	
//		System.out.println("Encoded Text: " + encoded);
		return encoded;
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

