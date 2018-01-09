package com.jint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

/**
 * 维护活跃会话列表
 * @author Lion
 *
 * 2018年1月9日 上午8:34:50
 */
public final class SessionRegistry {

	private static final Map<String, HttpSession> SESSIONS = new HashMap<>();
	
	private SessionRegistry() {}
	
	public static void addSession(HttpSession session) {
		SESSIONS.put(session.getId(), session);
	}
	
	public static void updateSession(HttpSession session, String oldSessionId) {
		synchronized (SESSIONS) {
			SESSIONS.remove(oldSessionId);
			addSession(session);
		}
	}
	
	public static void removeSession(HttpSession session) {
		SESSIONS.remove(session.getId());
	}
	
	public static List<HttpSession> getAllSession() {
		return new ArrayList<>(SESSIONS.values());
	}
	
	public static int getNumberOfSession() {
		return SESSIONS.size();
	}
}
