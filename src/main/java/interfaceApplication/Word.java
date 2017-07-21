package interfaceApplication;

import java.util.HashMap;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import JGrapeSystem.jGrapeFW_Message;
import apps.appsProxy;
import authority.privilige;
import database.db;
import json.JSONHelper;
import model.WordModel;
import nlogger.nlogger;
import rpc.execRequest;
import time.TimeHelper;

public class Word {
	private JSONObject _obj;
	private WordModel model;
	private HashMap<String, Object> map;

	public Word() {
		map = new HashMap<String, Object>();
		_obj = new JSONObject();
		model = new WordModel();
	}

	/**
	 * 新增热词 [若库中已存在该热词，则热词搜索次数加1，否则新增热词]
	 * 
	 * @project GrapeWord
	 * @package interfaceApplication
	 * @file Word.java
	 * 
	 * @param info
	 * @return
	 *
	 */
	public String AddWord(String info) {
		int code = 99;
		String _id;
		JSONObject object = model.check(info, def());
		if (object == null) {
			return resultMessage(1);
		}
		// 判断库中是否存在该热搜关键词
		try {
			String content = object.get("content").toString();
			JSONObject obj = findByContent(content);
			if (obj != null) {
				// 获取该热搜词的_id
				obj = (JSONObject) obj.get("_id");
				_id = obj.get("$oid").toString();
				obj = Count(content);
				if (obj != null) {
					code = model.getdb().eq("_id", new ObjectId(_id)).data(obj).update() != null ? 0 : 99;
				}
			} else {
				code = model.getdb().data(object).insertOnce() != null ? 0 : 99;
			}
		} catch (Exception e) {
			code = 99;
		}
		return resultMessage(code, "热词新增成功");
	}

	// 批量新增热词
	public String AddWords(String infos) {
		String result = resultMessage(99);
		String info;
		JSONArray array = JSONArray.toJSONArray(infos);
		if (array != null && array.size() != 0) {
			for (int i = 0; i < array.size(); i++) {
				info = array.get(i).toString();
				result = AddWord(info);
			}
		}
		return result;
	}

	/**
	 * 按搜索次数显示热词
	 * 
	 * @project GrapeWord
	 * @package interfaceApplication
	 * @file Word.java
	 * 
	 * @param no
	 *            显示的热词数量
	 * @param conds
	 *            条件
	 * @return
	 *
	 */
	public String ShowWord(int no, String conds) {
		JSONArray cond = JSONHelper.string2array(conds);
		db _db = model.getdb();
		_db = (cond == null) ? _db : _db.where(cond);
		JSONArray array = _db.desc("time").desc("count").limit(20).select();
		return resultMessage(array);
	}

	/**
	 * 删除热词，支持批量删除操作
	 * 
	 * @project GrapeWord
	 * @package interfaceApplication
	 * @file Word.java
	 * 
	 * @param id
	 * @return
	 *
	 */
	public String DeleteWord(String id) {
		int role = getRoleSign();
		int code = 99;
		if (role < 3) {
			return resultMessage(2);
		}
		try {
			String[] value = id.split(",");
			if (value.length == 1) {
				code = model.getdb().eq("_id", new ObjectId(id)).delete() != null ? 0 : 99;
			} else {
				db db = model.getdb();
				for (String _id : value) {
					db.eq("_id", new ObjectId(_id));
				}
				code = db.deleteAll() == value.length ? 0 : 99;
			}
		} catch (Exception e) {
			nlogger.logout(e);
			code = 99;
		}
		return resultMessage(code, "热词删除成功");
	}

	@SuppressWarnings("unchecked")
	public String Page(int ids, int pageSize) {
		JSONArray array = new JSONArray();
		JSONObject object = new JSONObject();
		int role = getRoleSign();
		try {
			if (role >= 3) {
				array = model.getdb().page(ids, pageSize);
				object.put("totalSize", (int) Math.ceil((double) model.getdb().count() / pageSize));
			} else {
				object.put("totalSize", 0);
			}
		} catch (Exception e) {
			nlogger.logout(e);
			object.put("totalSize", 0);
		}
		object.put("pageSize", pageSize);
		object.put("currentPage", ids);
		object.put("data", array);
		return resultMessage(object);
	}

	/**
	 * 根据时间查询热词
	 * 
	 * @project GrapeWord
	 * @package interfaceApplication
	 * @file Word.java
	 * 
	 *
	 */
	public String Search(String time, int no) {
		JSONArray array = null;
		try {
			array = model.getdb().like("time", time).desc("count").limit(no).select();
		} catch (Exception e) {
			array = null;
		}
		return resultMessage(array);
	}

	/**
	 * 根据角色plv，获取角色级别
	 * 
	 * @project GrapeSuggest
	 * @package interfaceApplication
	 * @file Suggest.java
	 * 
	 * @return
	 *
	 */
	private int getRoleSign() {
		int roleSign = 0; // 游客
		String sid = (String) execRequest.getChannelValue("sid");
		if (sid != null) {
			try {
				privilige privil = new privilige(sid);
				int roleplv = privil.getRolePV(appsProxy.appidString());
				if (roleplv >= 1000 && roleplv < 3000) {
					roleSign = 1; // 普通用户即企业员工
				}
				if (roleplv >= 3000 && roleplv < 5000) {
					roleSign = 2; // 栏目管理员
				}
				if (roleplv >= 5000 && roleplv < 8000) {
					roleSign = 3; // 企业管理员
				}
				if (roleplv >= 8000 && roleplv < 10000) {
					roleSign = 4; // 监督管理员
				}
				if (roleplv >= 10000) {
					roleSign = 5; // 总管理员
				}
			} catch (Exception e) {
				nlogger.logout(e);
				roleSign = 0;
			}
		}
		return roleSign;
	}

	/**
	 * 根据内容查询热搜关键词
	 * 
	 * @project GrapeWord
	 * @package interfaceApplication
	 * @file Word.java
	 * 
	 * @param content
	 * @return
	 *
	 */
	private JSONObject findByContent(String content) {
		JSONObject object = model.getdb().eq("content", content).find();
		return object;
	}

	@SuppressWarnings("unchecked")
	private JSONObject Count(String content) {
		JSONObject object = null;
		try {
			object = new JSONObject();
			object = model.getdb().eq("content", content).field("count").find();
			int count = Integer.parseInt(object.get("count").toString()) + 1;
			object.put("count", count);
		} catch (Exception e) {
			nlogger.logout(e);
			object = null;
		}
		return object;
	}

	private HashMap<String, Object> def() {
		map.put("content", "");
		map.put("count", 1);
		map.put("state", 1); // 待审核热词
		map.put("time", TimeHelper.nowMillis());
		return map;
	}

	private String resultMessage(int num) {
		return resultMessage(num, "");
	}

	@SuppressWarnings("unchecked")
	private String resultMessage(JSONObject object) {
		if (object == null) {
			object = new JSONObject();
		}
		_obj.put("records", object);
		return resultMessage(0, _obj.toString());
	}

	@SuppressWarnings("unchecked")
	private String resultMessage(JSONArray array) {
		if (array == null) {
			array = new JSONArray();
		}
		_obj.put("records", array);
		return resultMessage(0, _obj.toString());
	}

	private String resultMessage(int num, String message) {
		String msg = "";
		switch (num) {
		case 0:
			msg = message;
			break;
		case 1:
			msg = "必填字段没有填";
			break;
		case 2:
			msg = "没有操作权限";
			break;
		default:
			msg = "其他操作异常";
			break;
		}
		return jGrapeFW_Message.netMSG(num, msg);
	}
}
