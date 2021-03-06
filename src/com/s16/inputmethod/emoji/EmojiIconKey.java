package com.s16.inputmethod.emoji;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

public class EmojiIconKey {

	static final String JSON_KEY = "codesArraySpec";
	
	CharSequence codesArraySpec;
	public int[] codes;
	public CharSequence label;
	public CharSequence outputText;
	
	public EmojiIconKey() {
		
	}
	
	static EmojiIconKey from(Context context, String codesArraySpec) {
		EmojiIconKey key = new EmojiIconKey();
		if (codesArraySpec != null) {
			key.codesArraySpec = codesArraySpec; 
			if (codesArraySpec.matches("^[0-9a-fA-F]+\\|[0-9a-fA-F]+\\,[0-9a-fA-F]+\\|[0-9]+$") 
					|| codesArraySpec.matches("^[0-9a-fA-F]+\\|\\|[0-9]+$")) {
				
				int supportedMinSdkVersion = CodesArrayParser.getMinSupportSdkVersion(codesArraySpec);
				if (Build.VERSION.SDK_INT < supportedMinSdkVersion) {
					return key;
				}
				
				int codePoint = CodesArrayParser.parseCode(codesArraySpec);
				key.label = CodesArrayParser.parseLabel(codesArraySpec);
				key.codes = new int[] { codePoint };
				key.outputText = CodesArrayParser.parseOutputText(codesArraySpec);
				
			} else if (codesArraySpec.matches("^[0-9a-fA-F]+$")) {
				int codePoint = CodesArrayParser.parseCode(codesArraySpec);
				key.label = CodesArrayParser.parseLabel(codesArraySpec);
				key.codes = new int[] { codePoint };
				key.outputText = CodesArrayParser.parseOutputText(codesArraySpec);
				
			} else {
				key.label = codesArraySpec;
				key.codes = new int[] { CodesArrayParser.CODE_OUTPUT_TEXT };
				key.outputText = codesArraySpec;
			}
			
		}
		return key;
	}
	
	static EmojiIconKey from(Context context, JSONObject jsonObject) {
		if (jsonObject != null) {
			String codesArraySpec = getJSONString(jsonObject, JSON_KEY);
			if (!TextUtils.isEmpty(codesArraySpec)) {
				return EmojiIconKey.from(context, codesArraySpec);
			}
		}
		return null;
	}
	
	boolean isEmpty() {
		return codes == null;
	}
	
	JSONObject toJSONObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put(JSON_KEY, codesArraySpec == null ? null : codesArraySpec.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return obj;
	}
	
	static String getJSONString(JSONObject jsonObject, String key) {
		String result = null;
		if (jsonObject != null && key != null && jsonObject.has(key)) {
			try {
				result = jsonObject.getString(key);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
}
