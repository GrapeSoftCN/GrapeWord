package model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.json.simple.JSONObject;

import apps.appsProxy;
import check.formHelper;
import check.formHelper.formdef;
import database.db;
import database.userDBHelper;
import json.JSONHelper;
import nlogger.nlogger;
import rpc.execRequest;

public class WordModel {
	private userDBHelper Word;
	private formHelper form;
	private String sid = null;

	public WordModel() {
		sid = (String) execRequest.getChannelValue("sid");
		Word = new userDBHelper("Words", sid);
		form = Word.getChecker();
		form.putRule("content", formdef.notNull);
	}

	/**
	 * 非空字段验证
	 * 
	 * @project GrapeWord
	 * @package model
	 * @file WordModel.java
	 * 
	 * @param info
	 * @param map
	 * @return  存在非空字段，返回null，否则返回JSONObject
	 *
	 */
	public JSONObject check(String info, HashMap<String, Object> map) {
		JSONObject object = AddMap(map, info);
		return !form.checkRuleEx(object) ? null : object;
	}

	public db getdb() {
		return Word.bind(String.valueOf(appsProxy.appid()));
	}

	/**
	 * 将map添加至JSONObject中
	 * 
	 * @param map
	 * @param object
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public JSONObject AddMap(HashMap<String, Object> map, String Info) {
		JSONObject object = JSONHelper.string2json(Info);
		if (object != null) {
			if (map.entrySet() != null) {
				Iterator<Entry<String, Object>> iterator = map.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<String, Object> entry = (Map.Entry<String, Object>) iterator.next();
					if (!object.containsKey(entry.getKey())) {
						object.put(entry.getKey(), entry.getValue());
					}
				}
			}
		}
		return object;
	}
}
