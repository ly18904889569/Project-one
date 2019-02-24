package cn.edu.hit.util;

import java.io.FileNotFoundException;
import java.util.PriorityQueue;
import java.util.TreeMap;

public class Huffman {

	static PriorityQueue<Node> nodes = new PriorityQueue<>((o1, o2) -> (o1.value < o2.value) ? -1 : 1);
	static TreeMap<Character, String> codes = new TreeMap<>();
	static String text = "TTAAAAACGGGTGGAAGCGGTTCCCGGGGGCAGGCTCGGACCGGCGAAAGCGCCGGCAGAGCGTGCCGCTTTATCCTTGCTTCCGCTTAATCTGCGCVVVVVVVVVV";
	static String encoded = "";
	static String decoded = "";
	static int ASCII[] = new int[128];

	public static void main(String[] args) throws FileNotFoundException {
		handleNewText();
	}

	private static boolean handleNewText() {
		ASCII = new int[128];
		nodes.clear();
		codes.clear();
		encoded = "";
		decoded = "";
		System.out.println("Text: " + text);
		calculateCharIntervals(nodes, true);
		buildTree(nodes);
		generateCodes(nodes.peek(), "");

		printCodes();
		System.out.println("-- Encoding/Decoding --");
		System.out.println(text.length());
		encodeText();
		decodeText();
		return false;
	}

	private static void decodeText() {
		
		decoded = "";
		Node node = nodes.peek();
		System.out.println("cncoded.length:\t"+encoded.length()+"\t"+encoded);
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
//					System.out.println(tmpNode.character);
//					System.out.println(tmpNode.value);
				}
					
				else
					System.out.println("Input not Valid");

		}
		System.out.println("Decoded Text: " + decoded);
	}

	private static void encodeText() {
		encoded = "";
		for (int i = 0; i < text.length(); i++)
			encoded += codes.get(text.charAt(i));
		System.out.println("Encoded Text: " + encoded);
	}

	private static void buildTree(PriorityQueue<Node> vector) {
		while (vector.size() > 1)
			vector.add(new Node(vector.poll(), vector.poll()));
	}

	private static void printCodes() {
		System.out.println("--- Printing Codes ---");
		codes.forEach((k, v) -> System.out.println("'" + k + "' : " + v));
	}

	private static void calculateCharIntervals(PriorityQueue<Node> vector, boolean printIntervals) {
		if (printIntervals)
			System.out.println("-- intervals --");

		for (int i = 0; i < text.length(); i++)
			ASCII[text.charAt(i)]++;

		for (int i = 0; i < ASCII.length; i++)
			if (ASCII[i] > 0) {
				vector.add(new Node(ASCII[i] / (text.length() * 1.0), ((char) i) + ""));
				if (printIntervals)
					System.out.println("'" + ((char) i) + "' : " + ASCII[i] / (text.length() * 1.0));
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

class Node {
	Node left, right;
	double value;
	String character;

	public Node(double value, String character) {
		this.value = value;
		this.character = character;
		left = null;
		right = null;
	}

	public Node(Node left, Node right) {
		this.value = left.value + right.value;
		character = left.character + right.character;
		if (left.value < right.value) {
			this.right = right;
			this.left = left;
		} else {
			this.right = left;
			this.left = right;
		}
	}
}
