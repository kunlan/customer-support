package com.jint;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

@WebListener
public class SessionListener implements HttpSessionListener, HttpSessionIdListener {
	
	private SimpleDateFormat formatter =
			new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");

	@Override
	public void sessionIdChanged(HttpSessionEvent event, String oldSessionId) {
		System.out.println(this.date() + ": Session ID " + oldSessionId +
				" changed to " + event.getSession().getId());
		SessionRegistry.updateSession(event.getSession(), oldSessionId);
	}

	@Override
	public void sessionCreated(HttpSessionEvent se) {
		System.out.println(this.date() + ": Session " + se.getSession().getId() +
				" created.");
		SessionRegistry.addSession(se.getSession());
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		System.out.println(this.date() + ": Session " + se.getSession().getId() +
				" destoryed.");
		SessionRegistry.removeSession(se.getSession());
	}

	private String date() {
		return this.formatter.format(new Date());
	}
}
