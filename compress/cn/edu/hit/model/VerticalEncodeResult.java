package cn.edu.hit.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 垂直编码的输出结果
 * 
 * @author rivers May 30, 2017 2:13:40 PM
 *
 */

public class VerticalEncodeResult {
	// 垂直pbwt编码的结果
	ArrayList<ArrayList<Integer>> listsPBWT;
	// 水平编码的结果
	List<ReadsHorizonModel> readsHorizon;
	// 异常信息编码结果
	List<String> exceptionInfo;

	public ArrayList<ArrayList<Integer>> getListsPBWT() {
		return listsPBWT;
	}

	public void setListsPBWT(ArrayList<ArrayList<Integer>> listsPBWT) {
		this.listsPBWT = listsPBWT;
	}

	public List<ReadsHorizonModel> getReadsHorizon() {
		return readsHorizon;
	}

	public void setReadsHorizon(List<ReadsHorizonModel> readsHorizon) {
		this.readsHorizon = readsHorizon;
	}

	public List<String> getExceptionInfo() {
		return exceptionInfo;
	}

	public void setExceptionInfo(List<String> exceptionInfo) {
		this.exceptionInfo = exceptionInfo;
	}

}
