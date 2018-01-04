package com.jint;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

@WebServlet(
		name = "ticketServlet",
		urlPatterns = {"/tickets"},
		loadOnStartup = 1)
@MultipartConfig(
		fileSizeThreshold = 5_242_880,//5MB
		maxFileSize = 20_971_520L,//20MB
		maxRequestSize = 41_943_040L)//40MB
public class TicketServlet extends HttpServlet {

}
