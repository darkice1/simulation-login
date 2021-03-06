package org.simulationlogin.login.impl;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.simulationlogin.login.AbstractLogin;
import org.simulationlogin.util.Constants;

/**
 * 模拟jd登录
 * 
 * @author ksfzhaohui
 * 
 */
public class JDLogin extends AbstractLogin {

	private static Log logger = LogFactory.getLog(JDLogin.class);

	public JDLogin(String userName, String password) {
		super(userName, password);
	}

	@Override
	protected void readyLogin() throws Exception {
		HttpGet readyGet = null;
		try {
			String readyUrl = getReadyLoginUrl();
			readyGet = new HttpGet(readyUrl);
			HttpResponse response = getUserClient().execute(readyGet);
			HttpEntity entity = response.getEntity();
			String info = EntityUtils.toString(entity);
			Document doc = Jsoup.parseBodyFragment(info);
			Elements es = doc.select("#formlogin > input");
			if (es != null && es.size() > 0) {
				for (int i = 0; i < es.size(); i++) {
					readyParams.put(es.get(i).attr("name"),
							es.get(i).attr("value"));
				}
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (readyGet != null) {
				readyGet.releaseConnection();
			}
		}
	}

	@Override
	protected int executeLogin() throws Exception {
		HttpPost loginPost = null;
		try {
			String loginUrl = getLoginUrl();
			logger.info("请求login-->" + loginUrl);
			loginPost = new HttpPost(loginUrl);

			List<NameValuePair> loginNameValues = new ArrayList<NameValuePair>();
			loginNameValues.add(new BasicNameValuePair("loginname",
					getUserName()));
			loginNameValues.add(new BasicNameValuePair("nloginpwd",
					getPassword()));
			loginNameValues.add(new BasicNameValuePair("loginpwd",
					getPassword()));
			loginNameValues.add(new BasicNameValuePair("authcode",
					getAuthCode()));
			Iterator<String> iterator = readyParams.keySet().iterator();
			while (iterator.hasNext()) {
				String key = iterator.next().toString();
				String value = readyParams.get(key).toString();
				loginNameValues.add(new BasicNameValuePair(key, value));
			}

			loginPost.setEntity(new UrlEncodedFormEntity(loginNameValues,
					Consts.UTF_8));

			HttpResponse response = getUserClient().execute(loginPost);
			String loginRespInfoStr = EntityUtils.toString(
					response.getEntity(), Charset.forName("utf-8"));
			logger.info(loginRespInfoStr);
			if (loginRespInfoStr.contains("success")) {
				return Constants.SUCCESS;
			}
			return Constants.FAIL;
		} catch (Exception e) {
			throw e;
		} finally {
			loginPost.releaseConnection();
		}
	}

	@Override
	protected void testLogin() throws Exception {

	}

	@Override
	protected String getReadyLoginUrl() {
		return "https://passport.jd.com/uc/login";
	}

	@Override
	protected String getAuthCodeImageUrl() {
		String uuid = readyParams.get("uuid");
		return "https://authcode.jd.com/verify/image?acid=" + uuid + "&uid="
				+ uuid;
	}

	@Override
	protected String getLoginUrl() {
		String uuid = readyParams.get("uuid");
		return "https://passport.jd.com/uc/loginService?uuid=" + uuid;
	}

}
