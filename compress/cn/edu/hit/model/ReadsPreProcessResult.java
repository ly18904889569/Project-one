package cn.edu.hit.model;

import java.util.List;

/**
 * 预处理结果后返回的结果
 * 
 * @author rivers May 29, 2017 10:33:40 PM
 *
 */

public class ReadsPreProcessResult {
	// reads的信息，主要供垂直编码时使用
	List<ReadMatchResult> readsInfo;
	// 水平编码信息，start、end都可以从此处获得
	List<ReadsHorizonModel> readsHorizon;
	// 异常信息,到垂编码的时候，再对异常信息重新编排顺序吧
	//+ Exception的信息可以从readsInfo获得
	
	public List<ReadsHorizonModel> getReadsHorizon() {
		return readsHorizon;
	}

	public List<ReadMatchResult> getReadsInfo() {
		return readsInfo;
	}

	public void setReadsInfo(List<ReadMatchResult> readsInfo) {
		this.readsInfo = readsInfo;
	}

	public void setReadsHorizon(List<ReadsHorizonModel> readsHorizon) {
		this.readsHorizon = readsHorizon;
	}

}
