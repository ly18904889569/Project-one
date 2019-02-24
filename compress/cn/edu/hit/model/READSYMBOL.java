package cn.edu.hit.model;

/**
 * 
 * @author rivers
 * Jun 11, 2017 3:46:56 PM
 * 部分值有冗余，但是不影响后面的编码工作
 */
public enum READSYMBOL {
	//equal
	E,
	// I 是在字符串序列处理的时候，指明此处处理了多个insert的元素
	I,
	//
	END,
	//x 变换为 A, C. G, T
	X,
	
	// * 情形
	STAR,
	//AI -S, CI -V, GI -H, TI -Y
	S, V, H, Y, 
	//D, N, P
	D, N, P,
	// S 的情形影响也很大，也摘除来，单独处理吧 
	SS
	
}
